package us.dit.ueba.openc2consumer.profiles.th.catalog;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.oasis.openc2.lycan.profiles.th.targets.HuntflowSpecifiers;
import us.dit.ueba.openc2consumer.profiles.th.model.HuntflowInfo;

/**
 * Selecciona los huntflows del catálogo que cumplen los filtros recibidos.
 * Puede filtrar por ruta, nombres y tipos de argumentos, etiquetas y resultados esperados.
 * Un huntflow debe cumplir todos los filtros indicados para aparecer en la lista.
 */
public final class HuntflowFilters {
    private HuntflowFilters() {
    }

    /** Comprueba los filtros y devuelve los huntflows que los cumplen, en el orden del catálogo. */
    public static List<HuntflowInfo> select(List<HuntflowInfo> catalog, HuntflowSpecifiers filters) {
        validateFilters(filters);
        String path = normalizePath(filters.getPath());
        List<HuntflowInfo> selected = new ArrayList<>();
        for (HuntflowInfo huntflow : catalog) {
            if (matchesFilters(huntflow, filters, path)) {
                selected.add(huntflow);
            }
        }
        return selected;
    }

    /** Rechaza filtros con rutas, listas o formatos incompletos. */
    private static void validateFilters(HuntflowSpecifiers filters) {
        // Comprobar que la ruta sea absoluta y que los filtros tengan valores válidos.
        String path = filters.getPath();
        if (path != null && (path.isBlank() || !path.startsWith("/"))) {
            throw new IllegalArgumentException("Path filter must be absolute and must not be blank");
        }
        validateList(filters.getTags());
        var format = filters.getFormatTypes();
        if (format != null && (format.getVarName() == null || format.getVarName().isBlank()
                || format.getVarType() == null || format.getVarType().isBlank())) {
            throw new IllegalArgumentException("format_types requires var_name and var_type");
        }
        validateList(filters.getArgNames());
        validateList(filters.getArgTypes());

    }

    /** Quita las barras finales, conservando / como ruta raíz. */
    private static String normalizePath(String path) {
        // Quitar la barra final evita crear un prefijo con dos barras.
        if (path != null) {
            while (path.length() > 1 && path.endsWith("/")) {
                path = path.substring(0, path.length() - 1);
            }
        }
        return path;
    }

    /** Comprueba que un huntflow cumpla todos los filtros indicados. */
    private static boolean matchesFilters(HuntflowInfo huntflow, HuntflowSpecifiers filters, String path) {
        if (path != null && !matchesPath(huntflow.path(), path)) {
            return false;
        }

        Map<String, String> required = huntflow.argsRequired();
        if (required == null) {
            required = Map.of();
        }
        if (filters.getArgNames() != null && !required.keySet().containsAll(filters.getArgNames())) {
            return false;
        }
        if (filters.getArgTypes() != null && !required.values().containsAll(filters.getArgTypes())) {
            return false;
        }

        List<String> tags = huntflow.tags();
        if (tags == null) {
            tags = List.of();
        }
        if (filters.getTags() != null && !tags.containsAll(filters.getTags())) {
            return false;
        }

        // El nombre y el tipo deben corresponder al mismo resultado esperado.
        var format = filters.getFormatTypes();
        if (format != null) {
            Map<String, String> expectedReturns = huntflow.expectedReturns();
            if (expectedReturns == null
                    || !format.getVarType().equals(expectedReturns.get(format.getVarName()))) {
                return false;
            }
        }
        return true;
    }

    /** Acepta la ruta exacta o una ruta situada debajo de ella; / acepta todas las rutas absolutas. */
    private static boolean matchesPath(String candidate, String path) {
        if ("/".equals(path)) {
            return candidate.startsWith("/");
        }
        return candidate.equals(path) || candidate.startsWith(path + "/");
    }

    /** Permite listas ausentes o vacías, pero rechaza elementos null o textos en blanco. */
    private static void validateList(List<String> values) {
        if (values == null) {
            return;
        }
        for (String value : values) {
            if (value == null || value.isBlank()) {
                throw new IllegalArgumentException("Filter values must not be blank");
            }
        }
    }
}