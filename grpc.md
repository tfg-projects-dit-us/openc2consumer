# Cliente gRPC de Velociraptor

Este proyecto se comunica con la API gRPC de Velociraptor. Las clases del cliente se **generan** a partir del contrato `src/main/proto/api.proto` con el plugin `io.github.ascopes:protobuf-maven-plugin`. El canal y los stubs se crean a mano en `VelociraptorConfig`.

---

## 1. Requisitos

- **JDK 21** (o 17). Lombok 1.18.42, la versión que gestiona Spring Boot 3.5.x, **no funciona con JDK 27**: la compilación falla con `ExceptionInInitializerError: com.sun.tools.javac.tree.EndPosTable`.
  En VS Code puedes fijar el JDK solo para este proyecto en `.vscode/settings.json` (`java.configuration.runtimes`, `java.jdt.ls.java.home`, `maven.terminal.customEnv` y `terminal.integrated.env.windows`).
- **Maven** instalado. El wrapper `./mvnw` no funciona porque falta `.mvn/wrapper/`; usa `mvn`.
- No hace falta instalar `protoc` ni el plugin de gRPC: Maven los descarga.

---

## 2. El contrato: `src/main/proto/api.proto`

Los ficheros `.proto` **deben estar en `src/main/proto/`**. Esa es la carpeta configurada en el `pom.xml`. Si los pones dentro de `src/main/java`, no se generará nada.

```proto
syntax = "proto3";

import "google/protobuf/empty.proto";

package proto;   // nombre del servicio en la red: debe coincidir con el servidor

option java_package = "us.dit.ueba.openc2consumer.integrations.velociraptor.proto";

message VQLCollectorArgs { ... }
message VQLResponse { ... }
...

service API {
   rpc Query(VQLCollectorArgs) returns (stream VQLResponse) {}
   rpc VFSGetBuffer(VFSFileBuffer) returns (VFSFileBuffer) {}
   rpc PushEvents(PushEventRequest) returns (google.protobuf.Empty) {}
}
```

Puntos importantes:

- **`package proto;` y `service API`** deben coincidir con el `api.proto` de Velociraptor. El cliente llama a `/proto.API/Query`. Si cambias alguno de los dos nombres, el servidor responderá `UNIMPLEMENTED`.
- **`option java_package`** solo decide en qué paquete Java se escriben las clases generadas. No afecta a la red.
- **Los números de campo** (`= 1`, `= 2`…) deben ser los mismos que en el servidor. Los nombres de campo pueden cambiar, los números no.
- Solo hace falta declarar los mensajes y RPC que usa el proyecto. Es un subconjunto del `api.proto` original de Velociraptor.

---

## 3. Configuración de la generación en `pom.xml`

```xml
<properties>
    <grpc.version>1.77.1</grpc.version>
    <protobuf-java.version>4.33.4</protobuf-java.version>
    <spring-grpc.version>1.0.2</spring-grpc.version>
</properties>
...
<build>
    <extensions>
        <!-- Detecta el SO para descargar los binarios nativos correctos -->
        <extension>
            <groupId>kr.motd.maven</groupId>
            <artifactId>os-maven-plugin</artifactId>
            <version>1.7.1</version>
        </extension>
    </extensions>
    <plugins>
        <plugin>
            <groupId>io.github.ascopes</groupId>
            <artifactId>protobuf-maven-plugin</artifactId>
            <version>4.0.3</version>
            <configuration>
                <!-- Versión de protoc: alineada con protobuf-java -->
                <protoc>${protobuf-java.version}</protoc>
                <!-- Carpeta donde están los .proto -->
                <sourceDirectories>
                    <sourceDirectory>${project.basedir}/src/main/proto</sourceDirectory>
                </sourceDirectories>
                <!-- Plugin de protoc que genera los stubs gRPC -->
                <binaryMavenPlugins>
                    <binaryMavenPlugin>
                        <groupId>io.grpc</groupId>
                        <artifactId>protoc-gen-grpc-java</artifactId>
                        <version>${grpc.version}</version>
                        <!-- Sin @javax.annotation.Generated (no existe en Java 17+) -->
                        <options>@generated=omit</options>
                    </binaryMavenPlugin>
                </binaryMavenPlugins>
            </configuration>
            <executions>
                <execution>
                    <id>generate</id>
                    <goals><goal>generate</goal></goals>
                </execution>
            </executions>
        </plugin>
    </plugins>
</build>
```

| Herramienta | Qué genera |
|---|---|
| `protoc` | Clases de los **mensajes** (`Api.java`) |
| `protoc-gen-grpc-java` | **Stubs** del servicio (`APIGrpc.java`) |

Las librerías que necesitan las clases generadas en ejecución (`protobuf-java`, `grpc-stub`, `grpc-protobuf`, `grpc-netty`) llegan a través de `spring-grpc-client-spring-boot-starter` y `grpc-services`. Sus versiones las fija el BOM `spring-grpc-dependencies`.

---

## 4. Generar las clases

```bash
mvn generate-sources    # solo genera las clases
mvn clean compile       # genera y compila todo
```

En la fase `generate-sources`, el plugin descarga `protoc` y `protoc-gen-grpc-java` para tu sistema operativo, compila los `.proto` y deja el resultado en:

```
target/generated-sources/protobuf/us/dit/ueba/openc2consumer/integrations/velociraptor/proto/
├── Api.java       ← mensajes (nombre derivado de api.proto)
└── APIGrpc.java   ← stubs (nombre derivado de service API)
```

Esa carpeta se añade automáticamente como código fuente. **No edites el código generado** ni lo subas a git: cambia el `.proto` y vuelve a compilar.

> Si VS Code no reconoce las clases generadas después de compilar, ejecuta *Java: Clean Java Language Server Workspace*.

### Clases generadas

En `Api` (importar como `...proto.Api.VQLCollectorArgs`, etc.):
- `VQLCollectorArgs`, `VQLRequest`, `VQLEnv`, `VQLResponse`, `VQLTypeMap`, `VFSFileBuffer`, `PushEventRequest`. Son inmutables y se construyen con `newBuilder()`.

En `APIGrpc`:

| Clase | Uso |
|---|---|
| `APIGrpc.APIBlockingStub` | Llamadas síncronas. `query(args)` devuelve `Iterator<VQLResponse>` (server streaming). **Es la que usa `VqlService`.** |
| `APIGrpc.APIStub` | Llamadas asíncronas con `StreamObserver`. Se crea como bean, pero no se usa. |
| `APIGrpc.APIFutureStub` | `ListenableFuture`; solo para RPC sin `stream`. |
| `APIGrpc.APIImplBase` | Base para implementar un servidor. Los tests la usan para montar un servidor falso. |

---

## 5. Conexión: `VelociraptorConfig`

El canal **no** lo crea Spring gRPC por configuración, sino un `@Configuration` propio, activo cuando el perfil no es `test`:

```java
@Configuration
@Profile("!test")
public class VelociraptorConfig {

    @Value("${velociraptor.host:localhost}")
    private String host;

    @Value("${velociraptor.port:8001}")
    private int port;

    @Bean
    public ManagedChannel velociraptorChannel() {
        return ManagedChannelBuilder.forAddress(host, port)
                .usePlaintext()
                .build();
    }

    @Bean
    public APIGrpc.APIBlockingStub velociraptorBlockingStub(ManagedChannel channel) {
        return APIGrpc.newBlockingStub(channel);
    }

    @Bean
    public APIGrpc.APIStub velociraptorAsyncStub(ManagedChannel channel) {
        return APIGrpc.newStub(channel);
    }
}
```

Propiedades en `application.properties`:

```properties
velociraptor.host=localhost
velociraptor.port=8000
```

> **Limitaciones actuales**
> - El canal es **sin cifrar** (`usePlaintext()`). La API de un Velociraptor real exige **TLS mutuo** con los certificados del fichero que genera `velociraptor config api_client`. Mientras no se configure, solo funciona contra servidores sin TLS, como el servidor falso de los tests.
> - La API gRPC de Velociraptor suele escuchar en el **8001**. El 8000 suele ser el frontend de clientes.
> - `velociraptor.ssl`, `velociraptor.username` y `velociraptor.password` están en `application.properties`, pero ningún código los lee todavía.

---

## 6. Uso del stub: `VqlService`

`VqlService` recibe el `APIBlockingStub` por constructor, monta la petición con `ArgsBuilder` y recorre el stream de respuestas:

```java
VQLCollectorArgs args = new ArgsBuilder(query)
        .setName(name)
        .setVariable("ArtifactName", artifactName)
        .buildArgs();

Iterator<VQLResponse> responses = blockingStub.query(args);   // /proto.API/Query
while (responses.hasNext()) {
    VQLResponse response = responses.next();
    // response.getLog(), response.getResponse() (JSON con las filas), ...
}
```

### Manejo de errores

Las llamadas lanzan `io.grpc.StatusRuntimeException`, tanto al invocar como al recorrer el iterador:

```java
try {
    ...
} catch (StatusRuntimeException e) {
    Status.Code code = e.getStatus().getCode();   // UNAVAILABLE, UNIMPLEMENTED, UNAUTHENTICATED...
}
```

Diagnóstico rápido:
- `UNAVAILABLE`: no hay conexión (host o puerto incorrectos, servidor caído, o TLS requerido).
- `UNIMPLEMENTED`: el nombre del servicio o del método no coincide con el servidor. Revisa `package proto;` y `service API`.

---

## 7. Tests

Con el perfil `test` no se carga `VelociraptorConfig`. En su lugar, `TestVelociraptorConfig` (en `src/test`) levanta un servidor gRPC **en memoria** que extiende `APIGrpc.APIImplBase` y crea los stubs contra él. `VqlServiceTest` usa además un mock de `APIBlockingStub`.

---

## 8. Añadir un nuevo RPC o mensaje

1. Copia el mensaje o RPC del `api.proto` de Velociraptor a `src/main/proto/api.proto`, **manteniendo los números de campo**.
2. Ejecuta `mvn generate-sources`.
3. Usa el nuevo método desde `APIBlockingStub` (o desde `APIStub` si es streaming de cliente o bidireccional).

| Tipo de RPC | Stub |
|---|---|
| Unary (`rpc M(Req) returns (Res)`) | `APIBlockingStub` → `Res` |
| Server streaming (`returns (stream Res)`) | `APIBlockingStub` → `Iterator<Res>` |
| Client / bidi streaming | `APIStub` con `StreamObserver` |

---

## 9. Referencias

- [protobuf-maven-plugin (ascopes)](https://ascopes.github.io/protobuf-maven-plugin/)
- [gRPC Java — Basics tutorial](https://grpc.io/docs/languages/java/basics/)
- [Velociraptor — API](https://docs.velociraptor.app/docs/server_automation/server_api/)
- [api.proto de Velociraptor](https://github.com/Velocidex/velociraptor/blob/master/api/proto/api.proto)
