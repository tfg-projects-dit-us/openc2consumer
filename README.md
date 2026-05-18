# OpenC2 Consumer 

Esta aplicación es un agente que por un lado recibe comandos openC2 desde el motor de procesos por otro ejecuta VQL en el servidor velociraptor que se configure

## Cliente Velociraptor
Ha sido encesario crear el Stub gRpc, que se ha hecho a partir de api.proto de velociraptor
Se ha usado mvn clean compile, y se crean las clases en target, a partir del fichero api.proto
El stub cliente se llama VqlApiGrpc
Para los tests se ha usado un mock del servidor velociraptor, sólo está comprobado que las invocaciones son correctas

## Operaciones para una evidencia
Los tipos de evidencias disponibles están declarados en  EvidenceType (Interfaz VqlInterface)
Para cada evidencia se definen las siguientes operaciones:
Carga del artefacto (consulta llamada: evidencetype_openc2_soar_new_artefact)
Inicio de la monitorización de esa evidencia (consulta llamada: evidencetype_openc2_soar_start_monitoring )
Añadir un usuario a la tabla de usuarios para los que se recoge esa evidencia (consulta llamada: evidencetype_openc2_soar_add_user)
Eliminar un usuario de la tabla de usuarios para los que se recoge esa evidencia (consutla llamada: evidencetype_openc2_soar_delete_user)


## Arquitectura de la solución

```
src/
├── main/
│   ├── java/us/dit/ueba/openc2consumer/
│   │   ├── Openc2consumerApplication.java        # Punto de entrada Spring Boot
│   │   ├── config/
│   │   │   └── VelociraptorConfig.java           # Beans gRPC: ManagedChannel, stubs síncrono y asíncrono
│   │   └── services/
│   │       ├── VqlInterface.java                 # Interfaz principal con el enum EvidenceType
│   │       ├── VqlService.java                   # Servicio Spring que envía consultas VQL a Velociraptor vía gRPC
│   │       ├── ArgsBuilder.java                  # Construye VQLCollectorArgs leyendo la query VQL desde fichero .artifact
│   │       ├── QuerySolver.java                  # Interfaz que define getQuery(): cada implementación devuelve la VQL adecuada
│   │       ├── StartMonitoringQuerySolver.java   # QuerySolver para iniciar monitorización (lee fichero .monitoring)
│   │       ├── NewArtifactQuerySolver.java       # QuerySolver para registrar un nuevo artefacto en Velociraptor
│   │       ├── AddUserQuerySolver.java           # QuerySolver para añadir un usuario a la monitorización
│   │       └── DeleteUserQuerySolver.java        # QuerySolver para eliminar un usuario de la monitorización
│   ├── proto/
│   │   └── api.proto                             # Definición Protobuf del API gRPC de Velociraptor
│   └── resources/
│       ├── application.properties                # Configuración: dirección del servidor y ruta de artefactos
│       └── logon.artifact                        # Consulta VQL de ejemplo para monitorización de logons
├── test/
│   ├── java/us/dit/ueba/openc2consumer/
│   │   ├── config/
│   │   │   └── TestVelociraptorConfig.java       # Configuración de test: servidor gRPC in-process mock
│   │   └── Openc2consumerApplicationTests.java   # Tests de integración Spring Boot
│   └── resources/
│       └── application.properties                # Propiedades para tests: perfil test, ruta artefactos, log level
└── target/generated-sources/protobuf/            # Clases Java generadas por el compilador protobuf
    └── us/dit/ueba/openc2consumer/proto/
        ├── Api.java                              # Mensajes Protobuf (VQLCollectorArgs, VQLResponse…)
        └── VqlApiGrpc.java                       # Stubs gRPC generados (blocking, async, future)
```