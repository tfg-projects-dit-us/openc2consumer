package us.dit.ueba.openc2consumer.profiles.th.catalog;

import us.dit.ueba.openc2consumer.profiles.th.ThreatHuntingService;
import us.dit.ueba.openc2consumer.profiles.th.model.HuntflowInfo;
import us.dit.ueba.openc2consumer.profiles.th.model.DatasourceInfo;

import java.io.IOException;
import us.dit.ueba.openc2consumer.profiles.th.InvestigationException;
import us.dit.ueba.openc2consumer.profiles.th.model.InvestigationRequest;
import us.dit.ueba.openc2consumer.profiles.th.model.InvestigationResult;
import java.util.HashSet;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Carga los catálogos de huntflows y fuentes de datos desde archivos JSON al crear el servicio.
 * Guarda las listas en memoria para responder a las consultas del perfil TH.
 * Rechaza rutas de huntflows o nombres de fuentes de datos repetidos.
 * El método investigate lanza una excepción porque este servicio solo proporciona catálogos.
 */
@Service
public class JsonThreatHuntingService implements ThreatHuntingService {
    private final List<HuntflowInfo> huntflows;
    private final List<DatasourceInfo> datasources;

    /** Utiliza los dos catálogos incluidos en resources/profiles/th. */
    public JsonThreatHuntingService(ObjectMapper mapper) {
        this(mapper, new ClassPathResource("profiles/th/huntflows.json"), new ClassPathResource("profiles/th/datasources.json"));
    }

    /** Utiliza el catálogo de huntflows recibido y el de fuentes de datos incluido en resources. */
    public JsonThreatHuntingService(ObjectMapper mapper, Resource catalog) {
        this(mapper, catalog, new ClassPathResource("profiles/th/datasources.json"));
    }

    /** Spring utiliza este constructor con las ubicaciones configuradas o las predeterminadas. */
    @Autowired
    public JsonThreatHuntingService(ObjectMapper mapper,
            @Value("${openc2.th.huntflows-location:classpath:profiles/th/huntflows.json}") Resource catalog,
            @Value("${openc2.th.datasources-location:classpath:profiles/th/datasources.json}") Resource datasourceCatalog) {
        this.huntflows = loadHuntflows(mapper, catalog);
        this.datasources = loadDatasources(mapper, datasourceCatalog);
    }

    @Override
    public List<HuntflowInfo> listHuntflows() {
        return huntflows;
    }

    @Override
    public List<DatasourceInfo> listDatasources(String name) {
        if (name.isEmpty()) {
            return datasources;
        }
        return datasources.stream().filter(source -> source.name().equals(name)).toList();
    }

    /** Lee los huntflows y comprueba que sus rutas no se repitan; un error impide crear el servicio. */
    private List<HuntflowInfo> loadHuntflows(ObjectMapper mapper, Resource catalog) {
        // Cerrar el archivo al terminar y leer el JSON como una lista de HuntflowInfo.
        try (var input = catalog.getInputStream()) {
            List<HuntflowInfo> loadedHuntflows = List.copyOf(mapper.readValue(input, new TypeReference<List<HuntflowInfo>>() {}));
            var paths = new HashSet<String>();
            for (HuntflowInfo huntflow : loadedHuntflows) {
                // El conjunto devuelve false cuando la ruta ya ha aparecido.
                if (!paths.add(huntflow.path())) {
                    throw new IllegalArgumentException("Duplicate huntflow path: " + huntflow.path());
                }
            }
            return loadedHuntflows;
        } catch (IOException | IllegalArgumentException e) {
            throw new IllegalStateException("Cannot load huntflow catalog: " + catalog.getDescription(), e);
        }
    }

    /** Lee las fuentes de datos y comprueba que sus nombres no se repitan. */
    private List<DatasourceInfo> loadDatasources(ObjectMapper mapper, Resource datasourceCatalog) {
        try (var input = datasourceCatalog.getInputStream()) {
            List<DatasourceInfo> loadedDatasources = List.copyOf(mapper.readValue(input, new TypeReference<List<DatasourceInfo>>() {}));
            var names = new HashSet<String>();
            for (DatasourceInfo datasource : loadedDatasources) {
                if (!names.add(datasource.name())) {
                    throw new IllegalArgumentException("Duplicate datasource name: " + datasource.name());
                }
            }
            return loadedDatasources;
        } catch (IOException | IllegalArgumentException e) {
            throw new IllegalStateException("Cannot load datasource catalog: " + datasourceCatalog.getDescription(), e);
        }
    }

    /** Rechaza la investigación con el motivo UNSUPPORTED. */
    @Override
    public InvestigationResult investigate(InvestigationRequest request) {
        throw new InvestigationException(InvestigationException.Reason.UNSUPPORTED,
                "This service only provides catalogs; investigation is not implemented");
    }
}