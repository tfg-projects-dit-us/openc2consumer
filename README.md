# OpenC2 Consumer

Componente de [SOAR4FUEBA](https://github.com/tfg-projects-dit-us/SOAR4FUEBA) que recibe comandos OpenC2 por HTTP y los delega en los actuadores de cada perfil. Desarrollado en el marco del proyecto FUEBA+ (PID2024-155581OB-C21).

## Estado actual

Se utiliza OpenC2 1.0 con la librería Lycan. El perfil Threat Hunting (TH) permite:

- Consultar capacidades con `query features`: versión, perfiles y pares acción–objetivo disponibles.
- Consultar el catálogo de huntflows, aplicar filtros y seleccionar campos con `return_format`.
- Consultar las fuentes de datos por nombre o listar todas.

La lógica de `investigate` está preparada y se prueba con Mockito, pero el servicio actual usa catálogos JSON y no ejecuta investigaciones ni anuncia esa capacidad. La conexión de TH con la ejecución real en Velociraptor queda pendiente; no se implementa el perfil TH completo.

## Organización

```text
src/main/java/us/dit/ueba/openc2consumer/
  core/                       # HTTP, validación, capacidades y selección de actuadores
  profiles/th/                # Perfil TH, catálogos y contrato de investigación
  integrations/velociraptor/  # Configuración gRPC, cliente y consultas VQL

src/main/resources/
  profiles/th/                       # huntflows.json y datasources.json
  integrations/velociraptor/artifacts/
  application.properties
```

Las pruebas siguen la misma organización en `src/test/java`. `VqlQueries` reúne las consultas para cargar artefactos, iniciar monitorización y añadir o eliminar usuarios.

## Preparación y ejecución

Se necesitan un JDK compatible con las dependencias y Maven. El `pom.xml` configura Java 17 como nivel de compilación.

Instalar primero [openc2-lycan-java](https://github.com/tfg-projects-dit-us/openc2-lycan-java) en el repositorio Maven local, ejecutando en su proyecto:

```shell
mvn clean install
```

Después, desde este repositorio:

```shell
mvn clean compile
mvn test
mvn spring-boot:run
```

La compilación genera el cliente gRPC a partir de `src/main/proto/api.proto`. Las pruebas unitarias usan Mockito y un servidor gRPC simulado, sin necesitar un Velociraptor real.

## Configuración

Los catálogos de TH se cargan al arrancar desde `src/main/resources/profiles/th/`. El de huntflows incluye `userlogon`; el de fuentes de datos comienza vacío. Para añadir entradas, editar los JSON y reiniciar. También se pueden usar archivos externos:

```properties
openc2.th.huntflows-location=file:C:/ruta/huntflows.json
openc2.th.datasources-location=file:C:/ruta/datasources.json
```

En `application.properties` se configura Velociraptor mediante `velociraptor.*`, y el cliente VQL mediante `ueba.evidences`, `ueba.user.vigilancelevels` y `artifacts.path`. Cada evidencia requiere un archivo `<evidencia>.artifact`, con artefacto `UEBA.SOAR.<evidencia>` y tabla de usuarios `Service.<evidencia>` cuando corresponda. Esta configuración no añade huntflows al catálogo TH.

## Comandos OpenC2

Enviar peticiones a `POST /openc2/command` con `Content-Type: application/openc2+json;version=1.0`.

```json
{"action":"query","target":{"features":["versions","profiles","pairs"]}}
```

```json
{"action":"query","target":{"th":{"huntflows":{}}}}
```

```json
{"action":"query","target":{"th":{"datasources":""}}}
```

Los filtros de huntflows admiten `path` absoluto, `arg_names`, `arg_types`, `tags` y `format_types`. Se combinan con AND. `return_format` selecciona `path`, `uniqueId`, `version`, `args_required`, `expected_returns` o `script`. Las consultas sin coincidencias devuelven una lista vacía con estado 200.

La validación de investigaciones contempla huntflows sin argumentos o con hasta un `string` y un `integer`, según `args_required`. Varios argumentos del mismo tipo, tipos complejos, opciones temporales y ejecución asíncrona quedan pendientes. Actualmente `investigate` devuelve 501 porque el servicio JSON no ejecuta investigaciones.

## Licencia

Apache 2.0.
