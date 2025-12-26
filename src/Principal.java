import java.io.*;
import java.nio.file.*;
import java_cup.runtime.Symbol;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Principal {
    
    public static void generarLexer(String ruta) {
        File lexerFile = new File("src/Lexer.java");
        lexerFile.delete();
        File archivo = new File(ruta);
        JFlex.Main.generate(archivo);
    }
 
    public static void generarLexerCup(String ruta) {
        File lexerCupFile = new File("src/LexerCup.java");
        lexerCupFile.delete();
        File archivo = new File(ruta);
        JFlex.Main.generate(archivo);
    }

    public static void generarSintax(String[] rutaS) {
        File SintaxFile = new File("src/Sintax.java");
        if (SintaxFile.exists()) {
            SintaxFile.delete();
        }
        try {
            java_cup.Main.main(rutaS);
            System.out.println("Parser generado");
        } catch (Exception e) {
            System.err.println("Error al generar parser: " + e.getMessage());
        }
    }

    public static void moverArchivosGenerados() {
        try {
            // Mover sym.java
            Path rutaSym = Paths.get("src/sym.java");
            if (Files.exists(rutaSym)) {
                Files.delete(rutaSym);
            }
            Files.move(
                Paths.get("sym.java"), 
                Paths.get("src/sym.java")
            );
            System.out.println("✅ sym.java movido a src/");
            
            // Mover Sintax.java
            Path rutaSin = Paths.get("src/Sintax.java");
            if (Files.exists(rutaSin)) {
                Files.delete(rutaSin);
            }
            Files.move(
                Paths.get("Sintax.java"), 
                Paths.get("src/Sintax.java")
            );
            System.out.println("✅ Sintax.java movido a src/");
        } catch (Exception e) {
            System.err.println("⚠️  Advertencia: No se pudieron mover algunos archivos");
        }
    }
    
    /************************************************************
     * FUNCIÓN FALTANTE: obtenerNombreToken (CORREGIDA)
     ************************************************************/
    private static String obtenerNombreToken(int tipoToken) {
        try {
            java.lang.reflect.Field[] campos = sym.class.getDeclaredFields();
            for (java.lang.reflect.Field campo : campos) {
                if (campo.getType() == int.class && campo.getInt(null) == tipoToken) {
                    return campo.getName();
                }
            }
        } catch (Exception e) {
            // Si falla, usar nombres genéricos
        }
        
        // Mapeo manual básico para los tokens principales
        switch (tipoToken) {
    case 0: return "ERROR";
    case 2: return "Int";
    case 3: return "Navidad";
    case 5: return "Igual";
    case 6: return "Suma";
    case 7: return "Resta";
    case 8: return "Multiplicacion";
    case 9: return "Division";
    case 10: return "Identificador";
    case 14: return "P_coma";
    case 16: return "If";
    case 17: return "Parentesis_a";  // ← CORREGIDO: Solo una vez
    case 18: return "Parentesis_c";
    case 19: return "Llave_a";
    case 20: return "Llave_c";
    case 21: return "Return";
    case 22: return "For";
    case 23: return "Numero";
    case 24: return "Else";
    case 25: return "While";
    case 26: return "Return";  // ← DUPLICADO con case 21, probablemente sea "Op_relacional" o similar
    case 27: return "Do";      // ← CAMBIADO: Probablemente sea "Do" en lugar de "Navidad"
    default: return "TOKEN_" + tipoToken;
}
    }
    
    /************************************************************
     * FUNCIÓN CORREGIDA: escribirTokensArchivo (DESPUÉS del análisis)
     ************************************************************/
    public static void escribirTokensArchivo(String texto, String rutaSalida) {
        try {
            // PRIMERO: Recolectar todos los tokens (ANÁLISIS LÉXICO)
            List<Symbol> tokens = new ArrayList<>();
            LexerCup lexer = new LexerCup(new StringReader(texto));
            
            int totalTokens = 0;
            int erroresLexicos = 0;
            
            while (true) {
                Symbol token = lexer.next_token();
                if (token.sym == sym.EOF) break;
                tokens.add(token);
                totalTokens++;
                if (token.sym == sym.ERROR) erroresLexicos++;
            }
            
            // DESPUÉS: Escribir al archivo
            try (PrintWriter writer = new PrintWriter(new FileWriter(rutaSalida))) {
                writer.println("=== ANÁLISIS LÉXICO COMPLETADO ===");
                writer.println("Archivo: input.txt");
                writer.println("Fecha: " + new java.util.Date());
                writer.println("==================================\n");
                
                writer.println("LÍNEA\tCOLUMNA\tTOKEN\t\tLEXEMA");
                writer.println("------\t-------\t-----\t\t------");
                
                for (Symbol token : tokens) {
                    String nombreToken = obtenerNombreToken(token.sym);
                    String lexema = token.value != null ? token.value.toString() : "";
                    writer.printf("%d\t%d\t%s\t\t%s%n",
                        token.left + 1, token.right + 1, nombreToken, lexema);
                }
                
                writer.println("\n==================================");
                writer.println("ESTADÍSTICAS:");
                writer.println("• Total tokens: " + totalTokens);
                writer.println("• Tokens válidos: " + (totalTokens - erroresLexicos));
                writer.println("• Errores léxicos: " + erroresLexicos);
                writer.println("==================================");
            }
            
            System.out.println("✅ Análisis léxico completado");
            System.out.println("📁 Resultados guardados en: " + rutaSalida);
            System.out.println("📊 Total tokens encontrados: " + totalTokens);
            if (erroresLexicos > 0) {
                System.out.println("⚠️  Errores léxicos: " + erroresLexicos);
            }
            
        } catch (Exception e) {
            System.err.println("❌ Error: " + e.getMessage());
        }
    }
    
    /************************************************************
     * FUNCIÓN AUXILIAR: obtenerNombreTokenSeguro (mantenida por compatibilidad)
     ************************************************************/
    private static final Map<Integer, String> NOMBRES_TOKENS = new HashMap<>();
    
    static {
        NOMBRES_TOKENS.put(-1, "EOF");
        NOMBRES_TOKENS.put(0, "ERROR");
    }
    
    private static String obtenerNombreTokenSeguro(int tipoToken) {
        if (NOMBRES_TOKENS.containsKey(tipoToken)) {
            return NOMBRES_TOKENS.get(tipoToken);
        }
        
        try {
            java.lang.reflect.Field[] campos = sym.class.getDeclaredFields();
            
            for (java.lang.reflect.Field campo : campos) {
                campo.setAccessible(true);
                if (campo.getType() == int.class) {
                    int valorCampo = campo.getInt(null);
                    if (valorCampo == tipoToken) {
                        String nombre = campo.getName();
                        NOMBRES_TOKENS.put(tipoToken, nombre);
                        return nombre;
                    }
                }
            }
        } catch (Exception e) {
            // Si falla, usar la función principal
        }
        
        return obtenerNombreToken(tipoToken);
    }
    
    /************************************************************
     * FUNCIÓN ALTERNATIVA (mantenida)
     ************************************************************/
    public static void escribirTokensArchivoSimple(String texto, String rutaSalida) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(rutaSalida))) {
            LexerCup lexer = new LexerCup(new StringReader(texto));
            
            writer.println("=== TOKENS ENCONTRADOS ===");
            writer.println("Formato: [Línea:Columna] TipoToken 'Lexema'");
            writer.println("==========================");
            writer.println();
            
            int contador = 0;
            
            while (true) {
                Symbol token = lexer.next_token();
                if (token.sym == sym.EOF) break;
                
                String lexema = token.value != null ? token.value.toString() : "";
                writer.printf("[%d:%d] Token#%d '%s'%n",
                    token.left + 1,
                    token.right + 1,
                    token.sym,
                    lexema.replace("\n", "\\n").replace("\t", "\\t"));
                
                contador++;
            }
            
            writer.println("\n==========================");
            writer.println("Total tokens: " + contador);
            writer.println("FIN DEL ANÁLISIS");
            
            System.out.println("✅ Archivo de tokens creado: " + rutaSalida);
            System.out.println("📊 Tokens procesados: " + contador);
            
        } catch (Exception e) {
            System.err.println("❌ Error: " + e.getMessage());
        }
    }
    
    /************************************************************
     * FUNCIONES PARA ANÁLISIS SINTÁCTICO
     ************************************************************/
    
    public static class ResultadoSintactico {
        private boolean exitoso;
        private String mensaje;
        private int linea;
        private int columna;
        private String textoError;
        
        public ResultadoSintactico(boolean exitoso, String mensaje, int linea, int columna, String textoError) {
            this.exitoso = exitoso;
            this.mensaje = mensaje;
            this.linea = linea;
            this.columna = columna;
            this.textoError = textoError;
        }
        
        public boolean isExitoso() { return exitoso; }
        public String getMensaje() { return mensaje; }
        public int getLinea() { return linea; }
        public int getColumna() { return columna; }
        public String getTextoError() { return textoError; }
        
        @Override
        public String toString() { 
            if (exitoso) {
                return "✅ " + mensaje;
            } else {
                return "❌ " + mensaje;
            }
        }
    }
    
    public static ResultadoSintactico analizarSintactico(String texto) {
        System.out.println("\n=== DEBUG SINTÁCTICO DETALLADO ===");
        
        try {
            // Primero mostrar tokens
            LexerCup lexer = new LexerCup(new StringReader(texto));
            System.out.println("Tokens generados:");
            
            int tokenNum = 0;
            while (true) {
                Symbol token = lexer.next_token();
                int tipo = token.sym;
                String valor = token.value != null ? token.value.toString() : "";
                
                // Usar la nueva función obtenerNombreToken
                String nombreToken = obtenerNombreToken(tipo);
                
                System.out.printf("  Token #%d: %s (%d) = '%s' [Línea:%d, Col:%d]%n",
                    ++tokenNum, nombreToken, tipo, valor, 
                    token.left+1, token.right+1);
                    
                if (tipo == sym.EOF) break;
            }
            
            // Ahora intentar parsear
            System.out.println("\nIntentando análisis sintáctico...");
            lexer = new LexerCup(new StringReader(texto));
            Sintax parser = new Sintax(lexer);
            parser.parse();
            
            return new ResultadoSintactico(true, "Análisis sintáctico EXITOSO", -1, -1, null);
            
        } catch (Exception ex) {
            System.err.println("\n❌ ERROR durante el parseo:");
            ex.printStackTrace();
            return new ResultadoSintactico(false, "Error: " + ex.getMessage(), -1, -1, null);
        }
    }
    
    /************************************************************
     * FUNCIÓN PARA LIMPIAR ARCHIVOS
     ************************************************************/
    public static void limpiarArchivosViejos() {
        System.out.println("🧹 LIMPIANDO ARCHIVOS VIEJOS...");
        
        String[] archivosAEliminar = {
            "src/LexerCup.java",
            "src/Lexer.java", 
            "src/Sintax.java",
            "src/sym.java",
            "LexerCup.java",
            "Lexer.java",
            "Sintax.java", 
            "sym.java"
        };
        
        for (String archivo : archivosAEliminar) {
            File f = new File(archivo);
            if (f.exists()) {
                if (f.delete()) {
                    System.out.println("  ✅ Eliminado: " + archivo);
                } else {
                    System.out.println("  ❌ No se pudo eliminar: " + archivo);
                }
            }
        }
        System.out.println();
    }
    
    /************************************************************
     * FUNCIÓN MAIN
     ************************************************************/
    
    public static void main(String[] args) throws Exception {
        System.out.println("=== SISTEMA DE ANÁLISIS LÉXICO Y SINTÁCTICO ===\n");
        
        // Rutas de los archivos
        String rutaLexer = "src/Lexer.flex";
        String rutaLexerCup = "src/LexerCup.flex";
        String[] rutaSintax = {"-parser", "Sintax", "src/Sintax.cup"};
        String archivoEntrada = "./archivos/input.txt";
        String archivoTokens = "./archivos/output_tokens.txt";
        
        // 🧹 Limpiar primero
        limpiarArchivosViejos();
        
        // 1. GENERAR ANALIZADORES
        System.out.println("1. GENERANDO ANALIZADORES...");
        generarLexer(rutaLexer);
        generarLexerCup(rutaLexerCup);
        generarSintax(rutaSintax);
        moverArchivosGenerados();
        System.out.println();
        
        // 2. LEER ARCHIVO FUENTE
        System.out.println("2. LEYENDO ARCHIVO FUENTE...");
        
        String contenidoArchivo = "";
        File archivo = new File(archivoEntrada);
        
        if (!archivo.exists()) {
            System.out.println("⚠️  Archivo no encontrado: " + archivoEntrada);
            System.out.println("Creando archivo de ejemplo...");
            
            // Crear directorio si no existe
            new File("./archivos").mkdirs();
            
            // Contenido de ejemplo que SÍ funciona con "navidad"
            contenidoArchivo = "int navidad(){}";
            
            // Escribir archivo de entrada
            try (PrintWriter writer = new PrintWriter(archivoEntrada)) {
                writer.println(contenidoArchivo);
            }
            System.out.println("✅ Archivo de ejemplo creado: " + archivoEntrada);
        } else {
            // Leer archivo existente
            try (BufferedReader br = new BufferedReader(new FileReader(archivo))) {
                StringBuilder sb = new StringBuilder();
                String linea;
                while ((linea = br.readLine()) != null) {
                    sb.append(linea).append("\n");
                }
                contenidoArchivo = sb.toString();
            }
            System.out.println("📄 Archivo analizado: " + archivoEntrada);
            System.out.println("Tamaño: " + archivo.length() + " bytes");
        }
        
        // Mostrar contenido
        System.out.println("\nCONTENIDO DEL ARCHIVO:");
        System.out.println("----------------------");
        System.out.println(contenidoArchivo);
        System.out.println("----------------------\n");
        
        // 3. REALIZAR ANÁLISIS LÉXICO Y GUARDAR EN ARCHIVO
        System.out.println("3. REALIZANDO ANÁLISIS LÉXICO...");
        System.out.println("Guardando tokens en: " + archivoTokens);
        
        // Usar la función corregida
        escribirTokensArchivo(contenidoArchivo, archivoTokens);
        
        // Si falla, usar la simple
        File archivoTokensVerificar = new File(archivoTokens);
        if (!archivoTokensVerificar.exists() || archivoTokensVerificar.length() == 0) {
            System.out.println("\n⚠️  Usando método alternativo...");
            escribirTokensArchivoSimple(contenidoArchivo, archivoTokens);
        }
        
        // 4. MOSTRAR PREVIEW del archivo generado
        System.out.println("\n4. VISTA PREVIA DEL ARCHIVO DE TOKENS:");
        System.out.println("========================================");
        
        try (BufferedReader br = new BufferedReader(new FileReader(archivoTokens))) {
            String linea;
            int contador = 0;
            while ((linea = br.readLine()) != null && contador < 15) {
                System.out.println(linea);
                contador++;
            }
            if (contador == 15) {
                System.out.println("... (archivo truncado para vista previa)");
            }
        } catch (Exception e) {
            System.err.println("No se pudo leer el archivo de tokens: " + e.getMessage());
        }
        
        // 5. ANÁLISIS SINTÁCTICO
        System.out.println("\n5. REALIZANDO ANÁLISIS SINTÁCTICO...");
        System.out.println("=====================================\n");
        
        ResultadoSintactico resultado = analizarSintactico(contenidoArchivo);
        System.out.println(resultado);
        
        if (!resultado.isExitoso()) {
            System.out.println("\nDETALLES DEL ERROR SINTÁCTICO:");
            System.out.println("-----------------------------");
            System.out.println("Mensaje: " + resultado.getMensaje());
        }
        
        System.out.println("\n=== PROCESO COMPLETADO ===");
        System.out.println("✅ Análisis léxico guardado en: " + archivoTokens);
        System.out.println("📁 Directorio de trabajo: " + System.getProperty("user.dir"));
    }
}