import java.util.*;

public class TablaSimbolos {

    private final Map<String, List<Scope>> tablasPorFuncion = new LinkedHashMap<>();

    private Scope scopeActual;
    private String funcionActual = "global";

    private final List<String> errores = new ArrayList<>();

    /* =========================
       INICIALIZACIÓN
       ========================= */

    public void iniciarGlobal() {
        funcionActual = "global";
        scopeActual = new Scope("global", null);
        tablasPorFuncion.put("global", new ArrayList<>(List.of(scopeActual)));
    }

    public void resetearScopeAGlobal() {
        List<Scope> g = tablasPorFuncion.get("global");
        if (g != null && !g.isEmpty()) {
            scopeActual = g.get(0);
            funcionActual = "global";
        }
    }

    /* =========================
       MANEJO DE SCOPES
       ========================= */

    public void entrarFuncion(String nombreFuncion) {
        if (scopeActual == null) iniciarGlobal();

        funcionActual = nombreFuncion;

        if (tablasPorFuncion.containsKey(nombreFuncion)) {
            scopeActual = tablasPorFuncion.get(nombreFuncion).get(0);
            return;
        }

        Scope global = tablasPorFuncion.get("global").get(0);
        scopeActual = new Scope(nombreFuncion, global);

        List<Scope> lista = new ArrayList<>();
        lista.add(scopeActual);
        tablasPorFuncion.put(nombreFuncion, lista);
    }

    public void salirFuncion() {
        resetearScopeAGlobal();
    }

    public void entrarBloque(String nombreBloque) {
        if (scopeActual == null) iniciarGlobal();

        Scope nuevo = new Scope(nombreBloque, scopeActual);
        scopeActual = nuevo;
        tablasPorFuncion.get(funcionActual).add(nuevo);
    }

    public void salirBloque() {
        if (scopeActual != null && scopeActual.padre != null) {
            scopeActual = scopeActual.padre;
        }
    }

    /* =========================
       DECLARACIÓN Y USO
       ========================= */

    public void declarar(Simbolo s) {
        if (scopeActual == null) iniciarGlobal();

        if (scopeActual.contieneEnEsteScope(s.nombre)) {
            errores.add("ERROR SEMÁNTICO: '" + s.nombre +
                    "' ya fue declarado en este ámbito (" +
                    scopeActual.nombre + "). [L:" + s.linea + ", C:" + s.columna + "]");
            return;
        }
        scopeActual.insertar(s);
    }

    public void usarIdentificador(String id, int linea, int columna) {
        if (scopeActual == null) iniciarGlobal();

        System.out.println("[DEBUG] USO de identificador: " + id +
                " en scope: " + scopeActual.nombre);

        Simbolo s = scopeActual.buscar(id);

        if (s == null) {
            errores.add("ERROR SEMÁNTICO: Uso de '" + id +
                    "' sin declarar. [L:" + linea + ", C:" + columna + "]");
        } else {
            System.out.println("[DEBUG] ✔ Encontrado: " + s);
        }
    }

    public Simbolo buscarSimbolo(String nombre) {
        if (scopeActual == null) return null;
        return scopeActual.buscar(nombre);
    }

    /* =========================
       SALIDA
       ========================= */

    public List<String> getErrores() {
        return errores;
    }

    public void imprimir() {
        for (String key : tablasPorFuncion.keySet()) {
            System.out.println("\nTabla: " + key);
            for (Scope sc : tablasPorFuncion.get(key)) {
                System.out.println("  Scope: " + sc.nombre);
                if (sc.getTabla().isEmpty()) {
                    System.out.println("    (vacío)");
                } else {
                    for (Simbolo s : sc.getTabla().values()) {
                        System.out.println("    " + s);
                    }
                }
            }
        }
    }
}
