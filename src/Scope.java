import java.util.LinkedHashMap;
import java.util.Map;

public class Scope {
    public final String nombre; // ej: "global", "main", "func:foo", "bloque#3"
    public final Scope padre;

    private final Map<String, Simbolo> tabla = new LinkedHashMap<>();

    public Scope(String nombre, Scope padre) {
        this.nombre = (nombre == null) ? "" : nombre.trim();
        this.padre = padre;
    }

    private String norm(String id) {
        return (id == null) ? "" : id.trim();
    }

    public boolean contieneEnEsteScope(String id) {
        String k = norm(id);
        if (k.isEmpty()) return false;
        return tabla.containsKey(k);
    }

    public boolean insertar(Simbolo s) {
        if (s == null) return false;
        String k = norm(s.nombre);
        if (k.isEmpty()) return false;

        if (tabla.containsKey(k)) return false;
        tabla.put(k, s);
        return true;
    }

    public Simbolo buscar(String id) {
        String k = norm(id);
        if (k.isEmpty()) return null;

        Simbolo local = tabla.get(k);
        if (local != null) return local;

        if (padre != null) return padre.buscar(k);
        return null;
    }

    public Map<String, Simbolo> getTabla() {
        return tabla;
    }
}
