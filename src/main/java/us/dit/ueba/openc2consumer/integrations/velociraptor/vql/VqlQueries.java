package us.dit.ueba.openc2consumer.integrations.velociraptor.vql;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Locale;

/**
 * Reúne los textos VQL de las operaciones de Velociraptor.
 * La consulta de registro se lee del archivo .artifact; las demás están definidas aquí.
 * Estos métodos devuelven texto y no envían peticiones.
 */
public final class VqlQueries {
    private VqlQueries() {
    }
    /** Devuelve la consulta para añadir un usuario con su nivel de vigilancia. */
    public static String addUser() {
        String query = """     
    SELECT add_server_table(
        name = TableName,
        row = dict(
            Username = TargetUser,
            VigilanceLevel = get(item = scope(), field = 'TargetLevel', default = 'STANDARD')
        )
    ) FROM scope()
    """;

        return query;
    }

    /** Devuelve la consulta para retirar un usuario de la tabla de monitorización. */
    public static String deleteUser() {
        return "SELECT remove_server_table(name=TableName,condition=\"Username = '\" + TargetUser + \"'\") FROM scope()";
    }

    /** Devuelve la consulta para iniciar la monitorización de un artefacto. */
    public static String startMonitoring() {
        return "SELECT upsert_client_monitoring(artifact_name=ArtifactName) FROM scope()";

    }

    /** Lee la consulta de registro desde el archivo .artifact del tipo de evidencia. */
    public static String newArtifact(String evidenceType, String artifactsPath) {
        // Construir la ruta, por ejemplo: carpeta/userlogon.artifact.
        String filename = artifactsPath + File.separator + evidenceType.toLowerCase(Locale.ROOT) + ".artifact";
        File file = new File(filename);
        String query = null;
        if (!file.exists()) {
            throw new IllegalArgumentException("Archivo de consulta no encontrado: " + filename);
        }
        try {
            query = Files.readString(file.toPath());
        } catch (IOException e) {
            throw new IllegalArgumentException("Error reading query file: " + filename, e);
        }
        return query;
    }

}
