package us.dit.ueba.openc2consumer.core.actuators;

import us.dit.ueba.openc2consumer.core.actuators.Actuator;

import java.util.List;
import java.util.Map;

/**
 * Clase base que guarda el nombre y las operaciones admitidas por un perfil.
 * Cada perfil hereda estos campos para no repetir el mismo código.
 * El perfil concreto define cómo reconocer y resolver sus comandos.
 */

public abstract class ActuatorCommons implements Actuator {

    // Identificador del perfil, por ejemplo "th". No cambia tras la construcción.
    protected final String profileName;

    // Cada acción se asocia con la lista de targets que admite el actuador.
    protected Map<String, List<String>> supportedPairs;

    public ActuatorCommons(String profileName) {
        this.profileName = profileName;
    }

    /** Permite definir un actuador sin un perfil específico. */
    public ActuatorCommons() {
        this.profileName = null;
    }

    @Override
    public String getProfileName() {
        return profileName;
    }

    @Override
    public Map<String, List<String>> getSupportedPairs() {
        return supportedPairs;
    }
}