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
    
    System.out.println("\nDEBUG TacEmitter:");
    System.out.println("Globales en lista: " + globals.size());
    for (int i = 0; i < globals.size(); i++) {
        System.out.println("  Global[" + i + "]: \"" + globals.get(i) + "\"");
    }
    
    System.out.println("\nCódigo en lista: " + code.size());
    for (int i = 0; i < Math.min(code.size(), 10); i++) {
        System.out.println("  Code[" + i + "]: \"" + code.get(i) + "\"");
    }
    if (code.size() > 10) {
        System.out.println("  ... y " + (code.size() - 10) + " más");
    }
    
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
    System.out.println("\nDEBUG: Resultado final (primeros 200 chars):");
    System.out.println(finalResult.substring(0, Math.min(finalResult.length(), 200)));
    if (finalResult.length() > 200) {
        System.out.println("...");
    }
    
    return finalResult;
}
}