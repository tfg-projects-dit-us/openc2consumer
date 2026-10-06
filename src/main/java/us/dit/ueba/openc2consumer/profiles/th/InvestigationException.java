package us.dit.ueba.openc2consumer.profiles.th;

import java.util.Objects;

/**
 * Comunica un error al validar o ejecutar una investigación.
 * Guarda el motivo, un mensaje y, si existe, la excepción que originó el fallo.
 * ThreatHuntingProfile utiliza el motivo para elegir el estado de la respuesta OpenC2.
 */
public class InvestigationException extends RuntimeException {

    /** Identifica el tipo de fallo para que el perfil pueda tratarlo. */
    public enum Reason {
        UNSUPPORTED,       // La operación, opción o tipo de argumento no está implementado.
        UNKNOWN_HUNTFLOW,  // No existe el huntflow solicitado.
        INVALID_ARGUMENTS, // Faltan argumentos, sobran o sus valores no son válidos.
        UNAVAILABLE,       // El servicio de ejecución no está disponible.
        EXECUTION_FAILED   // La investigación ha fallado durante su ejecución.
    }

    private final Reason reason;

    /** Crea el error con un motivo y un mensaje, sin una excepción de origen. */
    public InvestigationException(Reason reason, String message) {
        this(reason, message, null);
    }

    /** Conserva la excepción original y exige que el motivo no sea null. */
    public InvestigationException(Reason reason, String message, Throwable cause) {
        super(message, cause);
        this.reason = Objects.requireNonNull(reason);
    }

    /** Devuelve el motivo que utilizará el perfil para clasificar el error. */
    public Reason reason() {
        return reason;
    }
}