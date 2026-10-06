package us.dit.ueba.openc2consumer.profiles.th.arguments;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.oasis.openc2.lycan.profiles.th.args.HuntArgs;

import us.dit.ueba.openc2consumer.profiles.th.InvestigationException;
import us.dit.ueba.openc2consumer.profiles.th.InvestigationException.Reason;
import us.dit.ueba.openc2consumer.profiles.th.model.HuntflowInfo;

/**
 * Comprueba los argumentos recibidos para un huntflow y relaciona cada valor con su nombre.
 * Admite como máximo un argumento string y uno integer.
 * Devuelve un mapa como {username: "ana", count: 5}, conservando el tipo de los valores.
 */
public final class InvestigationArguments {
    private InvestigationArguments() {
    }

    /** Valida los tipos, la cantidad y los valores antes de construir el mapa de argumentos. */
    public static Map<String, Object> validate(HuntflowInfo huntflow, HuntArgs arguments) {
        Map<String, String> requiredArguments = huntflow.argsRequired();
        if (requiredArguments == null) {
            requiredArguments = Map.of();
        }

        // 1. Separar los nombres requeridos según su tipo.
        List<String> stringNames = new ArrayList<>();
        List<String> integerNames = new ArrayList<>();
        for (Map.Entry<String, String> argument : requiredArguments.entrySet()) {
            String name = argument.getKey();
            String type = argument.getValue();
            if ("string".equals(type)) {
                stringNames.add(name);
            } else if ("integer".equals(type)) {
                integerNames.add(name);
            } else {
                throw new InvestigationException(Reason.UNSUPPORTED,
                        "This huntflow requires argument types that are not implemented");
            }
        }
        rejectUnsupportedArguments(arguments);

        // 2. Obtener los valores recibidos. Una lista ausente equivale a una lista vacía.
        List<String> stringValues = List.of();
        List<Long> integerValues = List.of();
        if (arguments != null) {
            if (arguments.getStringArgs() != null) {
                stringValues = arguments.getStringArgs();
            }
            if (arguments.getIntegerArgs() != null) {
                integerValues = arguments.getIntegerArgs();
            }
        }

        // 3. Comprobar que no falten ni sobren valores de ninguno de los dos tipos.
        if (stringValues.size() != stringNames.size() || integerValues.size() != integerNames.size()) {
            throw new InvestigationException(Reason.INVALID_ARGUMENTS,
                    "Provide exactly one value for each required argument");
        }

        // Con varios nombres del mismo tipo no sabemos qué valor corresponde a cada nombre.
        if (stringNames.size() > 1 || integerNames.size() > 1) {
            throw new InvestigationException(Reason.UNSUPPORTED,
                    "Multiple arguments of the same type require an explicit mapping agreement");
        }

        // 4. Asociar cada nombre con su valor, comprobando que el valor sea válido.
        Map<String, Object> validatedArguments = new LinkedHashMap<>();
        if (!stringNames.isEmpty()) {
            String value = stringValues.get(0);
            if (value == null || value.isBlank()) {
                throw new InvestigationException(Reason.INVALID_ARGUMENTS,
                        "Argument values must not be blank");
            }
            validatedArguments.put(stringNames.get(0), value);
        }
        if (!integerNames.isEmpty()) {
            Long value = integerValues.get(0);
            if (value == null) {
                throw new InvestigationException(Reason.INVALID_ARGUMENTS,
                        "Integer arguments must not be null");
            }
            validatedArguments.put(integerNames.get(0), value);
        }
        return validatedArguments;
    }

    /** Rechaza las formas de argumentos que todavía no puede procesar esta clase. */
    private static void rejectUnsupportedArguments(HuntArgs arguments) {
        if (arguments == null) {
            return;
        }
        boolean hasUnsupportedArguments = arguments.getTypedArgs() != null
                || arguments.getNativeOc2() != null
                || arguments.getStix() != null
                || arguments.getStixExtensions() != null
                || arguments.getTimeranges() != null
                || arguments.getDatasources() != null;
        if (hasUnsupportedArguments) {
            throw new InvestigationException(Reason.UNSUPPORTED,
                    "Only string_args and integer_args are implemented");
        }
    }
}