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
        System.out.println("[DEBUG] Iniciando tabla global");
        funcionActual = "global";
        scopeActual = new Scope("global", null);
        tablasPorFuncion.put("global", new ArrayList<>(List.of(scopeActual)));
        System.out.println("[DEBUG] Global iniciado, scope actual: " + scopeActual.nombre);
    }

    // ===============================
    // FUNCIONES - DECLARACIÓN Y MANEJO
    // ===============================

    public void entrarFuncion(String nombreFuncion) {
        System.out.println("[DEBUG entrarFuncion] Llamado con: " + nombreFuncion);
        
        if (scopeActual == null) {
            System.out.println("  -> Iniciando global...");
            iniciarGlobal();
        }
        
        // Si es "main", no procesar como función normal
        if (nombreFuncion.equals("main")) {
            funcionActual = "main";
            
            if (!tablasPorFuncion.containsKey("main")) {
                System.out.println("  -> Creando scope main");
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
            System.out.println("  -> Función ya existe, usando scope existente");
            scopeActual = tablasPorFuncion.get(nombreFuncion).get(0);
            return;
        }
        
        System.out.println("  -> Creando nuevo scope para función");
        Scope padre = tablasPorFuncion.get("global").get(0);
        Scope nuevo = new Scope(nombreFuncion, padre);
        
        tablasPorFuncion.put(nombreFuncion, new ArrayList<>(List.of(nuevo)));
        scopeActual = nuevo;
        
        System.out.println("  -> Scope creado: " + nuevo.nombre);
    }

    public void salirFuncion() {
        System.out.println("[DEBUG salirFuncion] Saliendo de función: " + funcionActual);
        resetearScopeAGlobal();
    }

    // ===============================
    // DECLARACIÓN DE FUNCIONES
    // ===============================

    public void declararFuncion(String nombre, String tipoRetorno, 
                                List<String> parametros, int linea, int columna) {
        System.out.println("[DEBUG DECLARAR FUNCION] " + nombre + 
                          " -> " + tipoRetorno + " " + parametros);
        
        // Verificar si ya existe en el ámbito global
        Scope globalScope = tablasPorFuncion.get("global").get(0);
        if (globalScope.contieneEnEsteScope(nombre)) {
            String error = "ERROR SEMÁNTICO: Función '" + nombre + 
                          "' ya declarada globalmente";
            System.out.println("  -> " + error);
            errores.add(error);
            return;
        }
        
        // Crear símbolo de función
        Simbolo funcion = Simbolo.crearFuncion(nombre, tipoRetorno, parametros, 
                                              "global", linea, columna);
        
        boolean insertado = globalScope.insertar(funcion);
        if (insertado) {
            System.out.println("  -> ✓ Función declarada: " + nombre);
        } else {
            System.out.println("  -> ✗ Error al declarar función");
            errores.add("ERROR SEMÁNTICO: No se pudo declarar función '" + nombre + "'");
        }
    }

    // ===============================
    // VERIFICACIÓN DE LLAMADAS A FUNCIONES
    // ===============================

    public void verificarLlamadaFuncion(String nombreFuncion, 
                                        List<String> tiposArgumentos,
                                        int linea, int columna) {
        System.out.println("[DEBUG LLAMADA] Verificando llamada a: " + nombreFuncion + 
                          " con argumentos: " + tiposArgumentos);
        
        // Buscar la función en el ámbito global
        Scope globalScope = tablasPorFuncion.get("global").get(0);
        Simbolo funcion = globalScope.buscar(nombreFuncion);
        
        if (funcion == null || !funcion.esFuncion()) {
            String error = "ERROR SEMÁNTICO: Función '" + nombreFuncion + 
                          "' no declarada. [L:" + linea + ", C:" + columna + "]";
            System.out.println("  -> ✗ " + error);
            errores.add(error);
            return;
        }
        
        // Verificar número de parámetros
        List<String> tiposParametros = funcion.getParametros();
        if (tiposArgumentos.size() != tiposParametros.size()) {
            String error = "ERROR SEMÁNTICO: Número incorrecto de argumentos para función '" + 
                          nombreFuncion + "'. Esperados: " + tiposParametros.size() + 
                          ", Recibidos: " + tiposArgumentos.size() + 
                          " [L:" + linea + ", C:" + columna + "]";
            System.out.println("  -> ✗ " + error);
            errores.add(error);
            return;
        }
        
        // Verificar tipos de parámetros
        for (int i = 0; i < tiposArgumentos.size(); i++) {
            String tipoEsperado = tiposParametros.get(i);
            String tipoRecibido = tiposArgumentos.get(i);
            
            if (!tiposCompatibles(tipoEsperado, tipoRecibido)) {
                String error = "ERROR SEMÁNTICO: Tipo incorrecto en argumento " + (i+1) + 
                              " de función '" + nombreFuncion + "'. " +
                              "Esperado: " + tipoEsperado + ", Recibido: " + tipoRecibido +
                              " [L:" + linea + ", C:" + columna + "]";
                System.out.println("  -> ✗ " + error);
                errores.add(error);
            }
        }
        
        System.out.println("  -> ✓ Llamada a función válida: " + nombreFuncion);
    }

    private boolean tiposCompatibles(String tipo1, String tipo2) {
        // Si alguno es desconocido, no podemos verificar (dejar pasar)
        if (tipo1.equals("desconocido") || tipo2.equals("desconocido")) {
            return true;
        }
        
        // Tipos exactamente iguales
        if (tipo1.equals(tipo2)) return true;
        
        // Reglas de compatibilidad (ajustar según tu lenguaje)
        if (tipo1.equals("float") && tipo2.equals("int")) return true;
        if (tipo1.equals("double") && tipo2.equals("float")) return true;
        if (tipo1.equals("double") && tipo2.equals("int")) return true;
        
        return false;
    }

    // ===============================
    // BÚSQUEDA DE FUNCIONES
    // ===============================

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
        System.out.println("[DEBUG entrarBloque] Creando bloque: " + nombre);
        
        if (scopeActual == null) {
            System.out.println("  -> Scope actual null, iniciando global");
            iniciarGlobal();
        }
        
        Scope nuevo = new Scope(nombre, scopeActual);
        
        // Asegurar que existe la lista para esta función
        if (!tablasPorFuncion.containsKey(funcionActual)) {
            tablasPorFuncion.put(funcionActual, new ArrayList<>());
        }
        
        tablasPorFuncion.get(funcionActual).add(nuevo);
        scopeActual = nuevo;
        System.out.println("  -> Nuevo scope actual: " + scopeActual.nombre);
    }

    public void salirBloque() {
        System.out.println("[DEBUG salirBloque] Saliendo de bloque: " + 
                          (scopeActual != null ? scopeActual.nombre : "null"));
        
        if (scopeActual != null && scopeActual.padre != null) {
            scopeActual = scopeActual.padre;
            System.out.println("  -> Scope actual ahora: " + scopeActual.nombre);
        }
    }

    // ===============================
    // DECLARACIÓN Y USO DE VARIABLES
    // ===============================

    public void declarar(Simbolo s) {
        System.out.println("[DEBUG DECLARAR] Intento declarar: " + s.nombre + 
                          " en scope: " + scopeActual.nombre);
        
        // Verificar si ya existe en este scope exacto
        if (scopeActual.contieneEnEsteScope(s.nombre)) {
            String error = "ERROR SEMÁNTICO: '" + s.nombre +
                    "' ya fue declarado en este ámbito (" +
                    scopeActual.nombre + ")";
            System.out.println("  -> " + error);
            errores.add(error);
            return;
        }
        
        // Verificar si existe en algún scope padre (solo advertencia)
        Simbolo existente = scopeActual.buscar(s.nombre);
        if (existente != null) {
            System.out.println("[DEBUG] Advertencia: '" + s.nombre + 
                              "' oculta declaración en scope padre: " + existente);
        }
        
        boolean insertado = scopeActual.insertar(s);
        if (insertado) {
            System.out.println("  -> ✓ Símbolo declarado: " + s.nombre + " tipo: " + s.tipo);
        } else {
            System.out.println("  -> ✗ Error al insertar símbolo");
        }
    }

    public void usarIdentificador(String id, int linea, int columna) {
        System.out.println("=== [DEBUG USO] ===");
        System.out.println("Buscando: '" + id + "'");
        System.out.println("Scope actual: " + scopeActual.nombre);
        System.out.println("Función actual: " + funcionActual);
        
        // Mostrar símbolos en este scope
        System.out.println("Símbolos en scope actual:");
        Map<String, Simbolo> tablaActual = scopeActual.getTabla();
        if (tablaActual.isEmpty()) {
            System.out.println("  (vacío)");
        } else {
            for (Map.Entry<String, Simbolo> entry : tablaActual.entrySet()) {
                System.out.println("  - " + entry.getKey() + " : " + entry.getValue().tipo);
            }
        }
        
        Simbolo s = scopeActual.buscar(id);
        
        if (s == null) {
            String error = "ERROR SEMÁNTICO: Uso de '" + id +
                    "' sin declarar. [L:" + linea + ", C:" + columna + "] " +
                    "(scope: " + scopeActual.nombre + ", función: " + funcionActual + ")";
            System.out.println("  -> ✗ " + error);
            errores.add(error);
        } else {
            System.out.println("  -> ✓ Encontrado: " + s);
        }
        System.out.println("===================");
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
            System.out.println("[DEBUG] SCOPE RESETEADO A GLOBAL");
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

        // Mismo tipo → OK
        if (tipoVar.equals(tipoExpr)) return;

        // int → float permitido
        if (tipoVar.equals("float") && tipoExpr.equals("int")) return;

        // ❌ TODO LO DEMÁS ES ERROR
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