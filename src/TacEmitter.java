import java.util.ArrayList;
import java.util.List;

public class TacEmitter {
    private final List<String> globals = new ArrayList<>();
    private final List<String> code = new ArrayList<>();
    

    
    public void emitGlobal(String line) {
        if (line == null) return;
        globals.add(line);
    }
    
    public void emit(String line) {
        if (line == null) return;
        code.add(line);
    }

    public void label(String name) {
        if (name == null) return;
        code.add(name + ":");
    }

    public void blank() {
        code.add("");
    }
    
    public void blankGlobal() {
        globals.add("");
    }

   public String build() {
    StringBuilder sb = new StringBuilder();
    

    // Sección de globales
    boolean hasGlobals = false;
    for (int i = 0; i < globals.size(); i++) {
        String line = globals.get(i);
        if (!line.trim().isEmpty()) {
            sb.append(line);
            if (i < globals.size() - 1) sb.append("\n");
            hasGlobals = true;
        }
    }
    
    // Separador entre globales y código si hay ambos
    if (hasGlobals && !code.isEmpty()) {
        sb.append("\n");
    }
    
    // Sección de código (funciones)
    for (int i = 0; i < code.size(); i++) {
        sb.append(code.get(i));
        if (i < code.size() - 1) sb.append("\n");
    }
    
    String finalResult = sb.toString();
    return finalResult;
}
}