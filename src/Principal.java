import java.io.*;
import java.nio.file.*;
import java_cup.runtime.Symbol;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Principal {
    
    public static void generarLexer(String ruta) {
        File archivo = new File(ruta);
        JFlex.Main.generate(archivo);
    }
 
    public static void generarLexerCup(String ruta) {
        File archivo = new File(ruta);
        JFlex.Main.generate(archivo);
    }

    public static void generarSintax(String[] rutaS) {
        try {
            java_cup.Main.main(rutaS);;
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
            
            // Mover Sintax.java
            Path rutaSin = Paths.get("src/Sintax.java");
            if (Files.exists(rutaSin)) {
                Files.delete(rutaSin);
            }
            Files.move(
                Paths.get("Sintax.java"), 
                Paths.get("src/Sintax.java")
            );
           
        } catch (Exception e) {
            System.err.println("No se movieron archivos");
        }
    }
    

private static String obtenerNombreToken(int tipoToken) {
    try {
        Tokens[] valores = Tokens.values();
        
        // Verificar que el tipoToken esté dentro del rango del enum
        if (tipoToken >= 0 && tipoToken < valores.length) {
            String nombre = valores[tipoToken].name();
            return nombre;
        } else {
            // Si está fuera de rango
            return "TOKEN_" + tipoToken;
        }
    } catch (Exception e) {
        System.out.printf("[DEBUG] Error: %s%n", e.getMessage());
        return "TOKEN_" + tipoToken;
    }
}
    
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
                return  mensaje;
            } else {
                return  mensaje;
            }
        }
    }
    
    public static ResultadoSintactico analizarSintactico(String texto) {        
        try {
            // Primero mostrar tokens
            LexerCup lexer = new LexerCup(new StringReader(texto));  
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
            lexer = new LexerCup(new StringReader(texto));
            Sintax parser = new Sintax(lexer);
            parser.parse();
            
            return new ResultadoSintactico(true, "Análisis ejecutado", -1, -1, null);
            
        } catch (Exception ex) {
            System.err.println("\nERROR durante el parseo:");
            ex.printStackTrace();
            return new ResultadoSintactico(false, "Error: " + ex.getMessage(), -1, -1, null);
        }
    }
    

    public static void limpiarArchivosViejos() {
        String[] archivosAEliminar = {
            "src/LexerCup.java",
            "src/Lexer.java", 
            "src/Sintax.java",
            "src/sym.java",

        };
        
        for (String archivo : archivosAEliminar) {
            File f = new File(archivo);
            if (f.exists()) {
                if (f.delete()) {
                    System.out.println(" Eliminado: " + archivo);
                } else {
                    System.out.println(" No se pudo eliminar: " + archivo);
                }
            }
        }
        System.out.println();
    }

    // FUNCIÓN PARA LEER ARCHIVO FUENTE
    public static String leerArchivoFuente(String rutaArchivo) {
        System.out.println("LEYENDO ARCHIVO FUENTE: " + rutaArchivo);
        
        StringBuilder contenido = new StringBuilder();
        File archivo = new File(rutaArchivo);
        
        if (!archivo.exists()) {
            System.out.println("Archivo no encontrado: " + rutaArchivo);
            return "";
        }
        
        try (BufferedReader br = new BufferedReader(new FileReader(archivo))) {
            String linea;
            while ((linea = br.readLine()) != null) {
                contenido.append(linea).append("\n");
            }
            System.out.println("Archivo leído exitosamente (" + archivo.length() + " bytes)");
            return contenido.toString();
        } catch (IOException e) {
            System.err.println("Error al leer archivo: " + e.getMessage());
            return "";
        }
    }


    // FUNCIÓN PARA GUARDAR TOKENS EN FORMATO JSON PURO
    public static void guardarResultadosEnArchivo(String archivoTokens, Map<String, Object> resultados) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(archivoTokens))) {
            
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> tokens = (List<Map<String, Object>>) resultados.get("tokens");
            
            if (tokens != null && !tokens.isEmpty()) {
                writer.write("[\n");
                
                for (int i = 0; i < tokens.size(); i++) {
                    Map<String, Object> token = tokens.get(i);
                    
                    writer.write("  {\n");
                    writer.write("    \"nombre\": \"" + token.get("nombre") + "\",\n");
                    writer.write("    \"valor\": \"" + token.get("valor").toString().replace("\"", "\\\"") + "\",\n");
                    writer.write("    \"linea\": " + token.get("linea") + ",\n");
                    writer.write("    \"columna\": " + token.get("columna") + "\n");
                    writer.write("  }");
                    
                    if (i < tokens.size() - 1) {
                        writer.write(",\n");
                    } else {
                        writer.write("\n");
                    }
                }
                
                writer.write("]\n");
            } else {
                writer.write("[]\n");
            }
            
            System.out.println("Tokens guardados: " + archivoTokens);
            
        } catch (IOException e) {
            System.err.println("Error al guardar tokens: " + e.getMessage());
        }
    }


    // FUNCIÓN PARA REALIZAR ANÁLISIS LÉXICO
    public static List<Map<String, Object>> realizarAnalisisLexico(String contenidoArchivo) {
        
        List<Map<String, Object>> listaTokens = new ArrayList<>();
        
        try {
            LexerCup lexer = new LexerCup(new StringReader(contenidoArchivo));
            
            while (true) {
                Symbol token = lexer.next_token();
                int tipo = token.sym;
                
                if (tipo == sym.EOF) break;
                
                String nombreToken = obtenerNombreToken(tipo);
                String valor = (token.value != null) ? token.value.toString() : "";
                
                Map<String, Object> tokenInfo = new HashMap<>();
                tokenInfo.put("nombre", nombreToken);
                tokenInfo.put("valor", valor);
                tokenInfo.put("linea", token.left + 1);
                tokenInfo.put("columna", token.right + 1);
                tokenInfo.put("tipo", tipo);
                
                listaTokens.add(tokenInfo);
            }
            
           
            
        } catch (Exception e) {
            System.err.println("Error en análisis léxico: " + e.getMessage());
        }
        
        return listaTokens;
    }

    // FUNCIÓN PARA PROCESAR ANÁLISIS COMPLETO
    public static Map<String, Object> procesarAnalisisCompleto(String contenidoArchivo) {
        long inicioAnalisis = System.currentTimeMillis();
        
        // Análisis léxico
        List<Map<String, Object>> listaTokens = realizarAnalisisLexico(contenidoArchivo);
        
        // Análisis sintáctico
        ResultadoSintactico resultadoSintactico = analizarSintactico(contenidoArchivo);
        
        // Calcular tiempo total
        long finAnalisis = System.currentTimeMillis();
        long tiempoAnalisis = finAnalisis - inicioAnalisis;
        
        // Preparar resultados para guardar
        Map<String, Object> resultados = new HashMap<>();
        resultados.put("tokens", listaTokens);
        resultados.put("resultadoSintactico", resultadoSintactico.getMensaje());
        resultados.put("totalTokens", listaTokens.size());
        resultados.put("tiempoAnalisis", tiempoAnalisis + " ms");
        
        if (!resultadoSintactico.isExitoso()) {
            resultados.put("errorSintactico", resultadoSintactico.getTextoError());
        }
        
        return resultados;
    }

    // FUNCIÓN PARA GENERAR TODOS LOS ANALIZADORES
    
    // FUNCIÓN SIMPLIFICADA
public static List<Map<String, Object>> realizarAnalisisCompleto(String contenidoArchivo) {
    // Solo análisis léxico (el sintáctico ya muestra su mensaje por consola)
    return realizarAnalisisLexico(contenidoArchivo);
}

// FUNCIÓN SIMPLIFICADA DE GUARDADO
public static void guardarTokensEnJSON(String archivoTokens, List<Map<String, Object>> tokens) {
    try (BufferedWriter writer = new BufferedWriter(new FileWriter(archivoTokens))) {
        if (tokens != null && !tokens.isEmpty()) {
            writer.write("[\n");
            for (int i = 0; i < tokens.size(); i++) {
                Map<String, Object> token = tokens.get(i);
                writer.write("  {\n");
                writer.write("    \"nombre\": \"" + token.get("nombre") + "\",\n");
                writer.write("    \"valor\": \"" + token.get("valor").toString().replace("\"", "\\\"") + "\",\n");
                writer.write("    \"linea\": " + token.get("linea") + ",\n");
                writer.write("    \"columna\": " + token.get("columna") + "\n");
                writer.write("  }");
                writer.write(i < tokens.size() - 1 ? ",\n" : "\n");
            }
            writer.write("]\n");
        } else {
            writer.write("[]\n");
        }
        System.out.println("Tokens guardados: " + archivoTokens);
    } catch (IOException e) {
        System.err.println("Error al guardar tokens: " + e.getMessage());
    }
}

    // MAIN SIMPLIFICADO
    public static void main(String[] args) throws Exception {        
        // Rutas
        String rutaLexer = "src/Lexer.flex";
        String rutaLexerCup = "src/LexerCup.flex";
        String[] rutaSintax = {"-parser", "Sintax", "src/Sintax.cup"};
        String archivoEntrada = "./archivos/input.txt";
        String archivoTokens = "./archivos/output_tokens.json";
        
        // 1. Limpiar
        limpiarArchivosViejos();
        
        // 2. Generar analizadores
        generarLexer(rutaLexer);
        generarLexerCup(rutaLexerCup);
        generarSintax(rutaSintax);
        moverArchivosGenerados();
        System.out.println();
        
        // 3. Leer
        String contenidoArchivo = leerArchivoFuente(archivoEntrada);
        if (contenidoArchivo.isEmpty()) return;
        
        // 4. Análisis sintáctico (solo muestra mensajes por consola)
        analizarSintactico(contenidoArchivo);
        
        // 5. Análisis léxico y guardar
        List<Map<String, Object>> tokens = realizarAnalisisLexico(contenidoArchivo);
        
        // 6. Guardar JSON
        guardarTokensEnJSON(archivoTokens, tokens);
        
    }
}