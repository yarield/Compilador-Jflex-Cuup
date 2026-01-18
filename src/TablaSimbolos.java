import java.util.*;

public class TablaSimbolos {

    // Tablas por función/main (lo que el profe mostró)
    // key = "main" o "func:nombre"
    private final Map<String, List<Scope>> tablasPorFuncion = new LinkedHashMap<>();

    // Manejo del scope actual
    private Scope scopeActual;
    private String funcionActual = "global";

    // Lista de errores semánticos (por ahora solo duplicados y usos sin declarar)
    private final List<String> errores = new ArrayList<>();

    public void iniciarGlobal() {
        funcionActual = "global";
        scopeActual = new Scope("global", null);
        tablasPorFuncion.put("global", new ArrayList<>(List.of(scopeActual)));
    }

    public void entrarFuncion(String nombreFuncion) {
        funcionActual = nombreFuncion;
        scopeActual = new Scope(nombreFuncion, tablasPorFuncion.get("global").get(0)); // padre = global
        tablasPorFuncion.put(nombreFuncion, new ArrayList<>(List.of(scopeActual)));
    }

    public void entrarBloque(String nombreBloque) {
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

        // duplicado en el MISMO scope = error
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
