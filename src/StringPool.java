import java.util.LinkedHashMap;
import java.util.Map;

public final class StringPool {
    private final Map<String, String> litToLabel = new LinkedHashMap<>();
    private int counter = 0;

    public String labelFor(String rawWithQuotes) {
        String existing = litToLabel.get(rawWithQuotes);
        if (existing != null) return existing;

        String lbl = "str_" + counter++;
        litToLabel.put(rawWithQuotes, lbl);
        return lbl;
    }

    public Map<String, String> entries() {
        return litToLabel;
    }

    public static String unescapeTacString(String rawWithQuotes) {
        String s = rawWithQuotes;
        if (s.length() >= 2 && s.startsWith("\"") && s.endsWith("\"")) {
            s = s.substring(1, s.length() - 1);
        }
        s = s.replace("\\n", "\n");
        s = s.replace("\\t", "\t");
        s = s.replace("\\\"", "\"");
        s = s.replace("\\\\", "\\");
        return s;
    }

    public static String toAsciizLiteral(String s) {
        String out = s.replace("\\", "\\\\")
                      .replace("\"", "\\\"")
                      .replace("\n", "\\n")
                      .replace("\t", "\\t");
        return "\"" + out + "\"";
    }
}
