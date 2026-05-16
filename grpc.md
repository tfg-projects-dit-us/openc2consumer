# Instrucciones para crear un cliente gRPC

Este proyecto usa **Spring gRPC** (`spring-grpc-client-spring-boot-starter`) junto con el plugin `protobuf-maven-plugin` para generar los stubs a partir de ficheros `.proto`.

---

## 1. Definir el servicio en el fichero `.proto`

En `src/main/proto/` crea o modifica un fichero `.proto` que incluya la definición del servicio:

```proto
syntax = "proto3";

package proto;

option java_package = "us.dit.ueba.openc2consumer.grpc";
option java_outer_classname = "MiServicioProto";

message MiRequest {
    string campo = 1;
}

message MiResponse {
    string resultado = 1;
}

service MiServicio {
    rpc MiMetodo(MiRequest) returns (MiResponse);
}
```

> Si el servicio ya está definido (p. ej. `API` en `api.proto`), este paso no es necesario.

---

## 2. Generar los stubs

Ejecuta:

```bash
./mvnw generate-sources
```

El plugin `protobuf-maven-plugin` (configurado en `pom.xml`) invocará a `protoc` y a `protoc-gen-grpc-java` y dejará las clases generadas en:

```
target/generated-sources/protobuf/
```

Las clases relevantes para el cliente son:
- `MiServicioGrpc` — clase principal del stub
- `MiServicioGrpc.MiServicioBlockingStub` — llamadas síncronas (unary)
- `MiServicioGrpc.MiServicioStub` — llamadas asíncronas
- `MiServicioGrpc.MiServicioFutureStub` — llamadas con `ListenableFuture`

---

## 3. Configurar la conexión en `application.properties`

```properties
# Canal gRPC al servidor destino (Spring gRPC lo inyecta por nombre de bean)
spring.grpc.client.channels.miservicio.address=static://localhost:9090
spring.grpc.client.channels.miservicio.negotiation-type=plaintext
```

Para TLS:

```properties
spring.grpc.client.channels.miservicio.address=static://servidor:443
spring.grpc.client.channels.miservicio.negotiation-type=tls
```

---

## 4. Inyectar el stub en un componente Spring

Spring gRPC proporciona la anotación `@GrpcClient` para inyectar el canal y crear el stub:

```java
import io.grpc.ManagedChannel;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;
import proto.MiServicioGrpc;
import proto.MiServicioGrpc.MiServicioBlockingStub;
import proto.MiServicioProto.MiRequest;
import proto.MiServicioProto.MiResponse;

@Service
public class MiServicioClienteService {

    @GrpcClient("miservicio")   // coincide con el nombre del canal en application.properties
    private MiServicioBlockingStub stub;

    public String llamar(String valor) {
        MiRequest request = MiRequest.newBuilder()
                .setCampo(valor)
                .build();
        MiResponse response = stub.miMetodo(request);
        return response.getResultado();
    }
}
```

> Alternativa con `ManagedChannel` inyectado manualmente:
>
> ```java
> @GrpcClient("miservicio")
> private ManagedChannel channel;
>
> // Crear el stub a mano:
> MiServicioBlockingStub stub = MiServicioGrpc.newBlockingStub(channel);
> ```

---

## 5. Llamadas con streaming

| Tipo | Stub a usar | Descripción |
|------|-------------|-------------|
| Unary | `BlockingStub` | 1 request → 1 response |
| Server streaming | `BlockingStub` | 1 request → `Iterator<Response>` |
| Client streaming | `Stub` (async) | `StreamObserver<Request>` → 1 response |
| Bidi streaming | `Stub` (async) | `StreamObserver<Request>` ↔ `StreamObserver<Response>` |

Ejemplo de server streaming:

```java
Iterator<MiResponse> it = stub.miStreamingMetodo(request);
while (it.hasNext()) {
    MiResponse r = it.next();
    // procesar r
}
```

---

## 6. Manejo de errores

Las llamadas gRPC lanzan `StatusRuntimeException` cuando el servidor devuelve un estado de error:

```java
try {
    MiResponse response = stub.miMetodo(request);
} catch (StatusRuntimeException e) {
    Status.Code code = e.getStatus().getCode();
    String description = e.getStatus().getDescription();
    // tratar según code: NOT_FOUND, UNAVAILABLE, UNAUTHENTICATED, etc.
}
```

---

## 7. Resumen de dependencias en `pom.xml`

Ya están presentes en el proyecto:

```xml
<!-- Cliente Spring gRPC -->
<dependency>
    <groupId>org.springframework.grpc</groupId>
    <artifactId>spring-grpc-client-spring-boot-starter</artifactId>
</dependency>

<!-- Servicios gRPC (health check, reflection…) -->
<dependency>
    <groupId>io.grpc</groupId>
    <artifactId>grpc-services</artifactId>
</dependency>
```

El BOM de Spring gRPC gestiona las versiones de `io.grpc` automáticamente.

---

## 8. Referencias

- [Spring gRPC — Client](https://docs.spring.io/spring-grpc/reference/client.html)
- [gRPC Java — Basics tutorial](https://grpc.io/docs/languages/java/basics/)
- [protobuf-maven-plugin](https://github.com/ascopes/protobuf-maven-plugin)
