import java.util.LinkedHashMap;
import java.util.Map;

public final class FloatPool {
    private final Map<String, String> litToLabel = new LinkedHashMap<>();
    private int counter = 0;

    public String labelFor(String rawFloatToken) {
        String existing = litToLabel.get(rawFloatToken);
        if (existing != null) return existing;

        String lbl = "flt_" + counter++;
        litToLabel.put(rawFloatToken, lbl);
        return lbl;
    }

    public Map<String, String> entries() {
        return litToLabel;
    }
}
