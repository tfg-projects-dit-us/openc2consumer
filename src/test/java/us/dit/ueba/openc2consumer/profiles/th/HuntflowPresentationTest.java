package us.dit.ueba.openc2consumer.profiles.th;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.oasis.openc2.lycan.profiles.th.targets.HuntflowSection;
import us.dit.ueba.openc2consumer.profiles.th.catalog.HuntflowPresentation;
import us.dit.ueba.openc2consumer.profiles.th.model.HuntflowInfo;
import static org.junit.jupiter.api.Assertions.*;

class HuntflowPresentationTest {
    private final HuntflowInfo huntflow = new HuntflowInfo("/auth/login", Map.of("username", "string"),
            1L, "1", Map.of("events", "string"), "example script");

    @Test
    void omittedAndEmptyFormatsReturnAllAvailableFields() {
        var expected = Map.of("path", "/auth/login", "args_required", Map.of("username", "string"),
                "uniqueId", 1L, "version", "1", "expected_returns", Map.of("events", "string"), "script", "example script");
        assertEquals(List.of(expected), HuntflowPresentation.selectFields(List.of(huntflow), null));
        assertEquals(List.of(expected), HuntflowPresentation.selectFields(List.of(huntflow), List.of()));
    }

    @Test
    void returnsOnlyRequestedFields() {
        assertEquals(List.of(Map.of("args_required", Map.of("username", "string"))),
                HuntflowPresentation.selectFields(List.of(huntflow), List.of(HuntflowSection.ARGS_REQUIRED)));
    }

    @Test
    void doesNotReturnEmptyRecordWhenRequestedDataIsMissing() {
        assertThrows(IllegalArgumentException.class, () -> HuntflowPresentation.selectFields(
                List.of(new HuntflowInfo("userlogon", null)), List.of(HuntflowSection.SCRIPT)));
    }
}