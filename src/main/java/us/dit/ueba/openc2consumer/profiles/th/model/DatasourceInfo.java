package us.dit.ueba.openc2consumer.profiles.th.model;

import java.util.List;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Guarda el nombre y las etiquetas de una fuente de datos del catálogo TH.
 * En JSON utiliza los campos ds_name y ds_tags, y omite los valores null.
 * Exige un nombre con texto y guarda una copia de las etiquetas que no admite cambios.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record DatasourceInfo(@JsonProperty("ds_name") String name,
        @JsonProperty("ds_tags") List<String> tags) {
    /** Comprueba el nombre y copia la lista de etiquetas al crear el objeto. */
    public DatasourceInfo {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Datasource name must not be blank");
        }
        if (tags != null) {
            tags = List.copyOf(tags);
        }
    }
}
