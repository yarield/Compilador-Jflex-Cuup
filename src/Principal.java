import java.io.*;
import java.nio.file.*;
import java_cup.runtime.Symbol;
import java.util.HashMap;
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
            System.out.println("Generando parser sintáctico...");
            java_cup.Main.main(rutaS);
            System.out.println("✅ Parser generado exitosamente");
        } catch (Exception e) {
            System.err.println("❌ Error al generar parser: " + e.getMessage());
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
     * SECCIÓN 2: FUNCIONES PARA GUARDAR TOKENS EN ARCHIVO
     ************************************************************/
    
    // Mapa para nombres de tokens legibles
    private static final Map<Integer, String> NOMBRES_TOKENS = new HashMap<>();
    
    static {
        // Inicializar el mapa con nombres genéricos
        // Estos se completarán dinámicamente
        NOMBRES_TOKENS.put(-1, "EOF");
        NOMBRES_TOKENS.put(0, "ERROR");
    }
    
    /************************************************************
     * FUNCIÓN PARA ESCRIBIR TOKENS A ARCHIVO (VERSIÓN SEGURA)
     * Esta versión NO depende de campos específicos en sym
     ************************************************************/
    public static void escribirTokensArchivo(String texto, String rutaSalida) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(rutaSalida))) {
            // Crear lexer para análisis
            LexerCup lexer = new LexerCup(new StringReader(texto));
            
            // Encabezado del archivo
            writer.println("=== ANÁLISIS LÉXICO - TOKENS ENCONTRADOS ===");
            writer.println("Archivo analizado: input.txt");
            writer.println("Fecha: " + new java.util.Date());
            writer.println("=============================================");
            writer.println();
            writer.println("LÍNEA\tCOLUMNA\tTOKEN\t\t\tLEXEMA");
            writer.println("------\t-------\t----------------\t------");
            
            int contadorTokens = 0;
            int erroresLexicos = 0;
            
            while (true) {
                Symbol token = lexer.next_token();
                int tipoToken = token.sym;
                
                // Verificar si llegamos al final
                if (tipoToken == sym.EOF) {
                    writer.println("\n=== FIN DE ARCHIVO ===");
                    break;
                }
                
                // Obtener información del token
                int linea = token.left + 1;
                int columna = token.right + 1;
                String lexema = (token.value != null) ? token.value.toString() : "";
                
                // Obtener nombre del token de forma segura
                String nombreToken = obtenerNombreTokenSeguro(tipoToken);
                
                // Contar errores léxicos
                if (nombreToken.contains("ERROR") || tipoToken == sym.error) {
                    erroresLexicos++;
                }
                
                // Formatear salida
                writer.printf("%d\t%d\t%-20s\t%s%n", 
                            linea, columna, nombreToken, lexema);
                
                contadorTokens++;
            }
            
            // Estadísticas finales
            writer.println("\n=============================================");
            writer.println("ESTADÍSTICAS:");
            writer.println("• Total de tokens: " + contadorTokens);
            writer.println("• Tokens válidos: " + (contadorTokens - erroresLexicos));
            writer.println("• Errores léxicos: " + erroresLexicos);
            writer.println("=============================================");
            
            System.out.println("✅ Tokens guardados en: " + rutaSalida);
            System.out.println("📊 Total tokens encontrados: " + contadorTokens);
            if (erroresLexicos > 0) {
                System.out.println("⚠️  Errores léxicos encontrados: " + erroresLexicos);
            }
            
        } catch (Exception e) {
            System.err.println("❌ Error al escribir archivo de tokens: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /************************************************************
     * FUNCIÓN SEGURA: Obtener nombre del token sin depender de campos específicos
     ************************************************************/
    private static String obtenerNombreTokenSeguro(int tipoToken) {
        // Primero, intentar obtener el nombre desde el mapa
        if (NOMBRES_TOKENS.containsKey(tipoToken)) {
            return NOMBRES_TOKENS.get(tipoToken);
        }
        
        // Si no está en el mapa, intentar obtenerlo dinámicamente
        try {
            // Intentar usar reflexión para obtener todos los campos de sym
            java.lang.reflect.Field[] campos = sym.class.getDeclaredFields();
            
            for (java.lang.reflect.Field campo : campos) {
                campo.setAccessible(true);
                if (campo.getType() == int.class) {
                    int valorCampo = campo.getInt(null);
                    if (valorCampo == tipoToken) {
                        String nombre = campo.getName();
                        NOMBRES_TOKENS.put(tipoToken, nombre); // Guardar en cache
                        return nombre;
                    }
                }
            }
        } catch (Exception e) {
            // Si falla la reflexión, continuamos con nombres genéricos
        }
        
        // Si no se encontró, crear un nombre genérico
        String nombreGenerico = "TOKEN_" + tipoToken;
        NOMBRES_TOKENS.put(tipoToken, nombreGenerico);
        return nombreGenerico;
    }
    
    /************************************************************
     * FUNCIÓN ALTERNATIVA MÁS SIMPLE
     * Si la anterior no funciona, usa esta
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
     * SECCIÓN 3: FUNCIONES PARA ANÁLISIS SINTÁCTICO
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
                return "✓ " + mensaje;
            } else {
                return "✗ " + mensaje;
            }
        }
    }
    
    public static ResultadoSintactico analizarSintactico(String texto) {
        try {
            Sintax parser = new Sintax(new LexerCup(new StringReader(texto)));
            parser.parse();
            return new ResultadoSintactico(true, "Análisis sintáctico correcto", -1, -1, null);
            
        } catch (Exception ex) {
            String mensaje = "Error de sintaxis";
            int linea = -1;
            int columna = -1;
            String valor = null;
            
            // Intentar extraer información del error
            if (ex.getMessage() != null) {
                mensaje = ex.getMessage();
                if (ex.getMessage().contains("Couldn't repair")) {
                    mensaje = "Error irrecuperable en el análisis sintáctico";
                }
            }
            
            return new ResultadoSintactico(false, mensaje, linea, columna, valor);
        }
    }
    
    /************************************************************
     * SECCIÓN 4: FUNCIÓN MAIN
     ************************************************************/
    
    public static void main(String[] args) throws Exception {
        System.out.println("=== SISTEMA DE ANÁLISIS LÉXICO Y SINTÁCTICO ===\n");
        
        // Rutas de los archivos
        String rutaLexer = "src/Lexer.flex";
        String rutaLexerCup = "src/LexerCup.flex";
        String[] rutaSintax = {"-parser", "Sintax", "src/Sintax.cup"};
        String archivoEntrada = "./archivos/input.txt";
        String archivoTokens = "./archivos/output_tokens.txt";
        
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
            
            // Contenido de ejemplo que debería funcionar
            contenidoArchivo = 
                "// Programa ejemplo\n" +
                "int main() {\n" +
                "    int x = 10;\n" +
                "    int y = 20;\n" +
                "    return x + y;\n" +
                "}";
            
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
        
        // Usar la versión SEGURA para guardar tokens
        escribirTokensArchivo(contenidoArchivo, archivoTokens);
        
        // Si falla la versión segura, usar la simple
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
            while ((linea = br.readLine()) != null && contador < 20) {
                System.out.println(linea);
                contador++;
            }
            if (contador == 20) {
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
            if (resultado.getLinea() > 0) {
                System.out.println("Línea: " + resultado.getLinea());
                System.out.println("Columna: " + resultado.getColumna());
            }
            if (resultado.getTextoError() != null) {
                System.out.println("Token: '" + resultado.getTextoError() + "'");
            }
        }
        
        System.out.println("\n=== PROCESO COMPLETADO ===");
        System.out.println("✅ Análisis léxico guardado en: " + archivoTokens);
        System.out.println("📁 Directorio de trabajo: " + System.getProperty("user.dir"));
    }
}