import java.util.ArrayList;
import java.util.List;

public class TacEmitter {
    private final List<String> lines = new ArrayList<>();

    public void emit(String line) {
        if (line == null) return;
        lines.add(line);
    }

    public void comment(String comment) {
        if (comment == null || comment.isEmpty()) return;
        
        String commentLine = comment.startsWith("#") ? comment : "# " + comment;
        lines.add(commentLine);  
    }


    public void label(String name) {
        if (name == null) return;
        lines.add(name + ":");
    }

    public void blank() {
        lines.add("");
    }

    public String build() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < lines.size(); i++) {
            sb.append(lines.get(i));
            if (i < lines.size() - 1) sb.append("\n");
        }
        return sb.toString();
    }
}
