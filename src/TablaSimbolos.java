import java.util.*;
//hello

public class TablaSimbolos {

    private final Map<String, List<Scope>> tablasPorFuncion = new LinkedHashMap<>();

    private Scope scopeActual;
    private String funcionActual = "global";

    private final List<String> errores = new ArrayList<>();

    public void iniciarGlobal() {
        funcionActual = "global";
        scopeActual = new Scope("global", null);
        tablasPorFuncion.put("global", new ArrayList<>(List.of(scopeActual)));
    }

    public void entrarFuncion(String nombreFuncion) {
        if (scopeActual == null) iniciarGlobal();

        nombreFuncion = (nombreFuncion == null) ? "" : nombreFuncion.trim();
        funcionActual = nombreFuncion;

        // Si ya existe, se reutiliza y se posiciona en su scope base
        if (tablasPorFuncion.containsKey(nombreFuncion)) {
            List<Scope> scopes = tablasPorFuncion.get(nombreFuncion);
            if (!scopes.isEmpty()) scopeActual = scopes.get(0);
            return;
        }

        Scope global = tablasPorFuncion.get("global").get(0);
        scopeActual = new Scope(nombreFuncion, global);

        List<Scope> lista = new ArrayList<>();
        lista.add(scopeActual);
        tablasPorFuncion.put(nombreFuncion, lista);
    }

    public void salirFuncion() {
        if (scopeActual == null) return;
        funcionActual = "global";
        // volver al scope global base
        List<Scope> g = tablasPorFuncion.get("global");
        if (g != null && !g.isEmpty()) scopeActual = g.get(0);
    }

    public void entrarBloque(String nombreBloque) {
        if (scopeActual == null) iniciarGlobal();

        nombreBloque = (nombreBloque == null) ? "" : nombreBloque.trim();

        // Asegurar que la lista exista para la función actual
        if (!tablasPorFuncion.containsKey(funcionActual)) {
            // por seguridad, crear tabla si alguien entró a bloque sin tabla creada
            entrarFuncion(funcionActual);
        }

        Scope nuevo = new Scope(nombreBloque, scopeActual);
        scopeActual = nuevo;
        tablasPorFuncion.get(funcionActual).add(nuevo);
    }

    public void salirBloque() {
        if (scopeActual != null && scopeActual.padre != null) {
            scopeActual = scopeActual.padre;
        }
    }

    public void declarar(Simbolo s) {
        if (scopeActual == null) iniciarGlobal();

        if (scopeActual.contieneEnEsteScope(s.nombre)) {
            errores.add("ERROR SEMÁNTICO: '" + s.nombre + "' ya fue declarado en este ámbito (" +
                    scopeActual.nombre + "). [L:" + s.linea + ", C:" + s.columna + "]");
            return;
        }
        scopeActual.insertar(s);
    }

    public void usarIdentificador(String id, int linea, int columna) {
        if (scopeActual == null) iniciarGlobal();
        Simbolo s = scopeActual.buscar(id);
        if (s == null) {
            errores.add("ERROR SEMÁNTICO: Uso de '" + id + "' sin declarar. [L:" + linea + ", C:" + columna + "]");
        }
    }

    public List<String> getErrores() {
        return errores;
    }

    public void imprimir() {
        for (String key : tablasPorFuncion.keySet()) {
            System.out.println("\nTabla: " + key);
            List<Scope> scopes = tablasPorFuncion.get(key);

            for (Scope sc : scopes) {
                System.out.println("  Scope: " + sc.nombre);
                if (sc.getTabla().isEmpty()) {
                    System.out.println("    (vacío)");
                } else {
                    for (Simbolo sym : sc.getTabla().values()) {
                        System.out.println("    " + sym);
                    }
                }
            }
        }
    }
}
