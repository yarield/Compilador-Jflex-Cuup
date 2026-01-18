public class Simbolo {
    public final String nombre;      // id (x, matriz, etc.)
    public final String tipo;        // int, float, bool...
    public final String clase;       // local, global, parametro, funcion
    public final String ambito;      // "main", "func:miFuncion", "global", etc.
    public final int linea;
    public final int columna;

    // extras útiles (por si es arreglo)
    public final boolean esArreglo;
    public final int dims;           // 0,1,2
    public final String detalle;     // texto libre

    public Simbolo(String nombre, String tipo, String clase, String ambito,
                   int linea, int columna,
                   boolean esArreglo, int dims, String detalle) {
        this.nombre = nombre;
        this.tipo = tipo;
        this.clase = clase;
        this.ambito = ambito;
        this.linea = linea;
        this.columna = columna;
        this.esArreglo = esArreglo;
        this.dims = dims;
        this.detalle = detalle;
    }

    @Override
    public String toString() {
        String arr = esArreglo ? (" arreglo(" + dims + "D)") : "";
        String pos = (linea > 0 ? (" [L:" + linea + ", C:" + columna + "]") : "");
        String det = (detalle != null && !detalle.isEmpty()) ? (" {" + detalle + "}") : "";
        return nombre + " : " + tipo + " (" + clase + ")" + arr + " @ " + ambito + pos + det;
    }
}
