import java.util.ArrayList;
import java.util.List;

public class Simbolo {
    public final String nombre;      // id (x, matriz, etc.) o nombre de función
    public final String tipo;        // int, float, bool... o tipo de retorno
    public final String clase;       // local, global, parametro, funcion
    public final String ambito;      // "main", "func:miFuncion", "global", etc.
    public final int linea;
    public final int columna;
    public final boolean esArreglo;
    public final int dims;           // 0,1,2
    public final String detalle;     // texto libre
    
    // Campos específicos para funciones
    private final List<String> parametros; // Lista de tipos de parámetros
    private final String tipoRetorno;      // Tipo de retorno (para funciones)

    // Constructor para variables/parámetros
    public Simbolo(String nombre, String tipo, String clase, String ambito,
                   int linea, int columna,
                   boolean esArreglo, int dims, String detalle) {
        this(nombre, tipo, clase, ambito, linea, columna, 
             esArreglo, dims, detalle, null, null);
    }
    
    // Constructor para funciones
    public Simbolo(String nombre, String tipoRetorno, List<String> parametros,
                   String ambito, int linea, int columna) {
        this.nombre = nombre;
        this.tipo = tipoRetorno; // tipo de retorno se almacena en tipo
        this.clase = "funcion";
        this.ambito = ambito;
        this.linea = linea;
        this.columna = columna;
        this.esArreglo = false;
        this.dims = 0;
        this.detalle = "";
        this.parametros = parametros != null ? parametros : new ArrayList<>();
        this.tipoRetorno = tipoRetorno;
    }
    
    // Constructor completo
    private Simbolo(String nombre, String tipo, String clase, String ambito,
                    int linea, int columna,
                    boolean esArreglo, int dims, String detalle,
                    List<String> parametros, String tipoRetorno) {
        this.nombre = nombre;
        this.tipo = tipo;
        this.clase = clase;
        this.ambito = ambito;
        this.linea = linea;
        this.columna = columna;
        this.esArreglo = esArreglo;
        this.dims = dims;
        this.detalle = detalle;
        this.parametros = parametros != null ? parametros : new ArrayList<>();
        this.tipoRetorno = tipoRetorno;
    }
    
    // Métodos para funciones
    public List<String> getParametros() {
        return new ArrayList<>(parametros); // Devuelve copia para evitar modificaciones
    }
    
    public String getTipoRetorno() {
        return tipoRetorno != null ? tipoRetorno : tipo;
    }
    
    public boolean esFuncion() {
        return "funcion".equals(clase);
    }
    
    public int getNumeroParametros() {
        return parametros != null ? parametros.size() : 0;
    }
    
    @Override
    public String toString() {
        if (esFuncion()) {
            String params = parametros != null && !parametros.isEmpty() ? 
                String.join(", ", parametros) : "sin parametros";
            String pos = (linea > 0 ? (" [L:" + linea + ", C:" + columna + "]") : "");
            return nombre + " : " + tipoRetorno + " (" + clase + ") " +
                   "[" + params + "] @ " + ambito + pos;
        }
        
        String arr = esArreglo ? (" arreglo(" + dims + "D)") : "";
        String pos = (linea > 0 ? (" [L:" + linea + ", C:" + columna + "]") : "");
        String det = (detalle != null && !detalle.isEmpty()) ? (" {" + detalle + "}") : "";
        return nombre + " : " + tipo + " (" + clase + ")" + arr + " @ " + ambito + pos + det;
    }
    
    // Método para comparar símbolos
    public boolean esIgual(Simbolo otro) {
        if (otro == null) return false;
        
        // Comparar propiedades básicas
        boolean iguales = this.nombre.equals(otro.nombre) &&
                         this.clase.equals(otro.clase) &&
                         this.ambito.equals(otro.ambito);
        
        // Si son funciones, comparar parámetros
        if (this.esFuncion() && otro.esFuncion()) {
            iguales = iguales && 
                     this.tipoRetorno.equals(otro.tipoRetorno) &&
                     this.parametros.equals(otro.parametros);
        } else if (!this.esFuncion() && !otro.esFuncion()) {
            // Si son variables, comparar tipo
            iguales = iguales && this.tipo.equals(otro.tipo);
        } else {
            // Uno es función y el otro no
            return false;
        }
        
        return iguales;
    }
    
    // Método estático para crear símbolo de función
    public static Simbolo crearFuncion(String nombre, String tipoRetorno, 
                                       List<String> parametros, 
                                       String ambito, int linea, int columna) {
        return new Simbolo(nombre, tipoRetorno, parametros, ambito, linea, columna);
    }
    
    // Método estático para crear símbolo de variable
    public static Simbolo crearVariable(String nombre, String tipo, String clase, 
                                        String ambito, int linea, int columna,
                                        boolean esArreglo, int dims, String detalle) {
        return new Simbolo(nombre, tipo, clase, ambito, linea, columna, 
                          esArreglo, dims, detalle);
    }
}