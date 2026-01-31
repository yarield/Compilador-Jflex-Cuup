import java.util.*;

public class TablaSimbolos {

    private final Map<String, List<Scope>> tablasPorFuncion = new LinkedHashMap<>();
    private Scope scopeActual;
    private String funcionActual = "global";
    private final List<String> errores = new ArrayList<>();

    // ===============================
    // INICIALIZACIÓN
    // ===============================

    public void iniciarGlobal() {
        funcionActual = "global";
        scopeActual = new Scope("global", null);
        tablasPorFuncion.put("global", new ArrayList<>(List.of(scopeActual)));
    }

    // ===============================
    // FUNCIONES
    // ===============================

    public void entrarFuncion(String nombreFuncion) {
        if (scopeActual == null) {
            iniciarGlobal();
        }
        
        if (nombreFuncion.equals("main")) {
            funcionActual = "main";
            
            if (!tablasPorFuncion.containsKey("main")) {
                Scope nuevo = new Scope("main", tablasPorFuncion.get("global").get(0));
                tablasPorFuncion.put("main", new ArrayList<>(List.of(nuevo)));
                scopeActual = nuevo;
            } else {
                scopeActual = tablasPorFuncion.get("main").get(0);
            }
            return;
        }
        
        funcionActual = nombreFuncion;
        
        if (tablasPorFuncion.containsKey(nombreFuncion)) {
            scopeActual = tablasPorFuncion.get(nombreFuncion).get(0);
            return;
        }
        
        Scope padre = tablasPorFuncion.get("global").get(0);
        Scope nuevo = new Scope(nombreFuncion, padre);
        
        tablasPorFuncion.put(nombreFuncion, new ArrayList<>(List.of(nuevo)));
        scopeActual = nuevo;
    }

    public void salirFuncion() {
        resetearScopeAGlobal();
    }

    public void declararFuncion(String nombre, String tipoRetorno, 
                                List<String> parametros, int linea, int columna) {
        // Verificar si ya existe
        Scope globalScope = tablasPorFuncion.get("global").get(0);
        if (globalScope.contieneEnEsteScope(nombre)) {
            String error = "ERROR SEMÁNTICO: Función '" + nombre + "' ya declarada";
            errores.add(error);
            return;
        }
        
        Simbolo funcion = Simbolo.crearFuncion(nombre, tipoRetorno, parametros, 
                                              "global", linea, columna);
        
        boolean insertado = globalScope.insertar(funcion);
        if (!insertado) {
            errores.add("ERROR SEMÁNTICO: No se pudo declarar función '" + nombre + "'");
        }
    }

    public void verificarLlamadaFuncion(String nombreFuncion, 
                                        List<String> tiposArgumentos,
                                        int linea, int columna) {
        Scope globalScope = tablasPorFuncion.get("global").get(0);
        Simbolo funcion = globalScope.buscar(nombreFuncion);
        
        if (funcion == null || !funcion.esFuncion()) {
            String error = "ERROR SEMÁNTICO: Función '" + nombreFuncion + 
                          "' no declarada. [L:" + linea + ", C:" + columna + "]";
            errores.add(error);
            return;
        }
        
        List<String> tiposParametros = funcion.getParametros();
        if (tiposArgumentos.size() != tiposParametros.size()) {
            String error = "ERROR SEMÁNTICO: Número incorrecto de argumentos para función '" + 
                          nombreFuncion + "'. Esperados: " + tiposParametros.size() + 
                          ", Recibidos: " + tiposArgumentos.size() + 
                          " [L:" + linea + ", C:" + columna + "]";
            errores.add(error);
            return;
        }
        
        for (int i = 0; i < tiposArgumentos.size(); i++) {
            String tipoEsperado = tiposParametros.get(i);
            String tipoRecibido = tiposArgumentos.get(i);
            
            if (!tiposCompatibles(tipoEsperado, tipoRecibido)) {
                String error = "ERROR SEMÁNTICO: Tipo incorrecto en argumento " + (i+1) + 
                              " de función '" + nombreFuncion + "'. " +
                              "Esperado: " + tipoEsperado + ", Recibido: " + tipoRecibido +
                              " [L:" + linea + ", C:" + columna + "]";
                errores.add(error);
            }
        }
    }

    private boolean tiposCompatibles(String tipo1, String tipo2) {
        if (tipo1.equals("desconocido") || tipo2.equals("desconocido")) {
            return true;
        }
        
        if (tipo1.equals(tipo2)) return true;
        
        if (tipo1.equals("float") && tipo2.equals("int")) return true;
        if (tipo1.equals("double") && tipo2.equals("float")) return true;
        if (tipo1.equals("double") && tipo2.equals("int")) return true;
        
        return false;
    }

    public Simbolo buscarFuncion(String nombre) {
        Scope globalScope = tablasPorFuncion.get("global").get(0);
        Simbolo simbolo = globalScope.buscar(nombre);
        
        if (simbolo != null && simbolo.esFuncion()) {
            return simbolo;
        }
        
        return null;
    }

    // ===============================
    // BLOQUES
    // ===============================

    public void entrarBloque(String nombre) {
        if (scopeActual == null) {
            iniciarGlobal();
        }
        
        Scope nuevo = new Scope(nombre, scopeActual);
        
        if (!tablasPorFuncion.containsKey(funcionActual)) {
            tablasPorFuncion.put(funcionActual, new ArrayList<>());
        }
        
        tablasPorFuncion.get(funcionActual).add(nuevo);
        scopeActual = nuevo;
    }

    public void salirBloque() {
        if (scopeActual != null && scopeActual.padre != null) {
            scopeActual = scopeActual.padre;
        }
    }

    // ===============================
    // DECLARACIÓN Y USO DE VARIABLES
    // ===============================

    public void declarar(Simbolo s) {
        if (scopeActual.contieneEnEsteScope(s.nombre)) {
            String error = "ERROR SEMÁNTICO: '" + s.nombre +
                    "' ya fue declarado en este ámbito (" +
                    scopeActual.nombre + ")";
            errores.add(error);
            return;
        }
        
        scopeActual.insertar(s);
    }

    public void usarIdentificador(String id, int linea, int columna) {
        Simbolo s = scopeActual.buscar(id);
        
        if (s == null) {
            String error = "ERROR SEMÁNTICO: Uso de '" + id +
                    "' sin declarar. [L:" + linea + ", C:" + columna + "] " +
                    "(scope: " + scopeActual.nombre + ", función: " + funcionActual + ")";
            errores.add(error);
        }
    }

    // ===============================
    // BÚSQUEDA
    // ===============================

    public Simbolo buscarSimbolo(String nombre) {
        return scopeActual.buscar(nombre);
    }

    public Scope getScopeActual() {
        return scopeActual;
    }

    // ===============================
    // RESET
    // ===============================

    public void resetearScopeAGlobal() {
        if (tablasPorFuncion.containsKey("global") && !tablasPorFuncion.get("global").isEmpty()) {
            scopeActual = tablasPorFuncion.get("global").get(0);
            funcionActual = "global";
        }
    }

    // ===============================
    // UTILIDADES
    // ===============================

    public List<String> getErrores() {
        return new ArrayList<>(errores);
    }

    public void imprimir() {
        System.out.println("\n------ TABLA DE SIMBOLOS --------");
        for (String key : tablasPorFuncion.keySet()) {
            System.out.println("\nTabla: " + key);
            for (Scope sc : tablasPorFuncion.get(key)) {
                System.out.println("  Scope: " + sc.nombre);
                Map<String, Simbolo> tabla = sc.getTabla();
                if (tabla.isEmpty()) {
                    System.out.println("    (vacío)");
                } else {
                    for (Simbolo s : tabla.values()) {
                        System.out.println("    " + s);
                    }
                }
            }
        }
        System.out.println("---------------------------------\n");
    }
    
    public void imprimirErrores() {
        System.out.println("\n------ ERRORES SEMÁNTICOS --------");
        if (errores.isEmpty()) {
            System.out.println("No hay errores semánticos.");
        } else {
            for (String error : errores) {
                System.out.println(error);
            }
        }
        System.out.println("----------------------------------\n");
    }
    
    // ===============================
    // VERIFICACIÓN DE TIPOS
    // ===============================
    
    public void verificarTipos(String tipoVar, String tipoExpr, int linea, int columna) {
        if (tipoExpr.equals("desconocido")) return;

        if (tipoVar.equals(tipoExpr)) return;

        if (tipoVar.equals("float") && tipoExpr.equals("int")) return;

        errores.add(
            "ERROR SEMÁNTICO: No se puede asignar " +
            tipoExpr + " a " + tipoVar
        );
    }

    public void verificarParametrosFuncion(Simbolo f, List<String> args) {
        List<String> params = f.getParametros();

        if (params.size() != args.size()) {
            errores.add(
                "ERROR SEMÁNTICO: La función " + f.nombre +
                " espera " + params.size() +
                " parámetros y recibió " + args.size()
            );
            return;
        }

        for (int i = 0; i < args.size(); i++) {
            String esperado = params.get(i);
            String recibido = args.get(i);

            if (!tiposCompatibles(esperado, recibido)) {
                errores.add(
                    "ERROR SEMÁNTICO: Parámetro " + (i + 1) +
                    " de " + f.nombre +
                    " esperaba " + esperado +
                    " y recibió " + recibido
                );
            }
        }
    }
}