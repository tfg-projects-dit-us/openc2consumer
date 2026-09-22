package us.dit.ueba.openc2consumer.actuators;

import org.oasis.openc2.lycan.OpenC2Message;
import org.oasis.openc2.lycan.OpenC2Response;

public interface Actuator {
  /**
   * Identificador del perfil (ej: "th", "slpf", "er")
   */
  String getProfileName();

  /**
   * Evalúa si este actuador sabe/puede procesar la combinación de Action y Target
   */
  boolean supports(OpenC2Message message);

  /**
   * Ejecuta la lógica del comando y devuelve la respuesta individual
   */
  OpenC2Response solve(OpenC2Message message);

}
