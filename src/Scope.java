import java.util.LinkedHashMap;
import java.util.Map;

public class Scope {
    public final String nombre; // ej: "global", "main", "func:foo", "bloque#3"
    public final Scope padre;

    private final Map<String, Simbolo> tabla = new LinkedHashMap<>();

    public Scope(String nombre, Scope padre) {
        this.nombre = nombre;
        this.padre = padre;
    }

    public boolean contieneEnEsteScope(String id) {
        return tabla.containsKey(id);
    }

    public boolean insertar(Simbolo s) {
        if (tabla.containsKey(s.nombre)) return false;
        tabla.put(s.nombre, s);
        return true;
    }

    public Simbolo buscar(String id) {
        if (tabla.containsKey(id)) return tabla.get(id);
        if (padre != null) return padre.buscar(id);
        return null;
    }

    public Map<String, Simbolo> getTabla() {
        return tabla;
    }
}
