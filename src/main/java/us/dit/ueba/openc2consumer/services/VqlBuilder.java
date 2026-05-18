package us.dit.ueba.openc2consumer.services;

import org.springframework.stereotype.Component;

/**
 * El objetivo de esta clase es construir las consultas VQL que se enviarán a
 * Velociraptor.
 */
@Component("vqlBuilder")
public class VqlBuilder {

    private String someParameter;

    public VqlBuilder setSomeParameter(String someParameter) {
        this.someParameter = someParameter;
        return this;
    }

    public String buildVql() {
        String vql = someParameter != null ? "SELECT * FROM some_table WHERE param = '" + someParameter + "'" : "SELECT * FROM some_table";
        return vql;
    }
}
