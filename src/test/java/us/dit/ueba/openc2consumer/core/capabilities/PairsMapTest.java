package us.dit.ueba.openc2consumer.core.capabilities;

import us.dit.ueba.openc2consumer.core.capabilities.PairsMap;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import static org.junit.jupiter.api.Assertions.*;

/** Comprueba validacion, ausencia de duplicados y copias inmutables de los pares declarados. */
class PairsMapTest {
    @Test
    void accumulatesTargetsAndIgnoresDuplicates() {
        var pairs = PairsMap.builder().addPair("query", "features")
                .addPair("query", "th.huntflows").addPair("query", "features")
                .addPair("query", "th.datasources").addPair("investigate", "th.hunt").build();
        assertEquals(List.of("features", "th.huntflows", "th.datasources"), pairs.getTargetsFor("query"));
        assertTrue(pairs.supports("investigate", "th.hunt"));
        assertTrue(pairs.supportsAction("query"));
        assertFalse(pairs.supports("query", "th.hunt"));
    }

    @Test
    void builtValuesAreImmutableAndIndependentFromBuilder() {
        var builder = PairsMap.builder().addPair("query", "features");
        var pairs = builder.build();
        builder.addPair("query", "th.huntflows").addPair("investigate", "th.hunt");
        assertEquals(List.of("features"), pairs.getTargetsFor("query"));
        assertFalse(pairs.supportsAction("investigate"));
        assertThrows(UnsupportedOperationException.class, () -> pairs.asMap().clear());
        assertThrows(UnsupportedOperationException.class, () -> pairs.asMap().get("query").add("file"));
        assertThrows(UnsupportedOperationException.class, () -> pairs.getTargetsFor("query").clear());
    }

    @Test
    void emptyMapSupportsNothing() {
        var pairs = PairsMap.builder().build();
        assertTrue(pairs.asMap().isEmpty());
        assertEquals(List.of(), pairs.getTargetsFor("query"));
        assertFalse(pairs.supportsAction("query"));
        assertFalse(pairs.supports("query", "features"));
    }

    @ParameterizedTest
    @CsvSource({"investigate,features", "investigate,th.huntflows", "investigate,th.datasources",
            "query,th.hunt", "unknown,file", "query,unknown", "QUERY,features"})
    void rejectsInvalidPairsWithoutChangingBuilder(String action, String target) {
        var builder = PairsMap.builder().addPair("query", "features");
        assertThrows(IllegalArgumentException.class, () -> builder.addPair(action, target));
        assertEquals(List.of("features"), builder.build().getTargetsFor("query"));
        assertEquals(1, builder.build().asMap().size());
    }

    @Test
    void rejectsNullInputsExplicitly() {
        assertThrows(IllegalArgumentException.class, () -> PairsMap.builder().addPair(null, "file"));
        assertThrows(IllegalArgumentException.class, () -> PairsMap.builder().addPair("query", null));
    }

    @Test
    void preservesExistingCommonPairs() {
        var pairs = PairsMap.builder().addPair("scan", "file").addPair("scan", "device").build();
        assertEquals(List.of("file", "device"), pairs.getTargetsFor("scan"));
    }
    @Test
    void acceptsActionsDefinedByLycan() {
        var pairs = PairsMap.builder()
                .addPairAction(org.oasis.openc2.lycan.types.ActionType.START, "device")
                .addPair("stop", "device")
                .build();
        assertTrue(pairs.supports("start", "device"));
        assertTrue(pairs.supports("stop", "device"));
        assertThrows(IllegalArgumentException.class,
                () -> PairsMap.builder().addPairAction(null, "device"));
    }
}