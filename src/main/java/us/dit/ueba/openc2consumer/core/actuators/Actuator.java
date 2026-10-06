package us.dit.ueba.openc2consumer.core.actuators;

import java.util.List;
import java.util.Map;
import org.oasis.openc2.lycan.OpenC2Message;
import org.oasis.openc2.lycan.OpenC2Response;

/**
 * Interfaz comun que deben implementar los actuadores de los perfiles OpenC2.
 * El Consumer la utiliza para conocer sus capacidades, elegir quien atiende
 * un comando y obtener su respuesta. Cada perfil implementa su propia logica.
 */
public interface Actuator {
    /** Identificador del perfil, por ejemplo "th" para Threat Hunting. */
    String getProfileName();

    /** Indica si reconoce la accion y el target; la validacion completa se realiza en solve. */
    boolean supports(OpenC2Message message);

    /** Procesa el comando y devuelve su estado y, cuando corresponda, sus resultados. */
    OpenC2Response solve(OpenC2Message message);

    /** Capacidades anunciadas: cada accion se asocia a la lista de targets que admite. */
    Map<String, List<String>> getSupportedPairs();
}