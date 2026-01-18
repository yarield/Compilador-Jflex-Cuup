import java.util.ArrayList;

public class Nodo {
    public final String lexema;
    public String tipo;
    public final ArrayList<Nodo> hijos;

    // opcional: posición
    private int linea = -1;
    private int columna = -1;

    public Nodo(String lexema) {
        this.lexema = lexema;
        this.tipo = "";
        this.hijos = new ArrayList<>();
    }

    public Nodo(String lexema, int linea, int columna) {
        this(lexema);
        this.linea = linea;
        this.columna = columna;
    }

    public String getLexema() { return lexema; }

    public void setTipo(String tipo) { this.tipo = tipo; }
    public String getTipo() { return tipo; }

    // ✅ NUEVO: getters de posición
    public int getLinea() { return linea; }
    public int getColumna() { return columna; }

    public void addHijo(Nodo hijo) {
        if (hijo != null) hijos.add(hijo);
    }

    public ArrayList<Nodo> getHijos() { return hijos; }

    // Impresión tipo árbol (bonita y clara)
    public void imprimir() {
        imprimir("", true);
    }

    private void imprimir(String prefijo, boolean esUltimo) {
        String pos = (linea >= 0 && columna >= 0) ? (" [L" + linea + ",C" + columna + "]") : "";
        System.out.println(prefijo + (esUltimo ? "└── " : "├── ") + lexema + pos);

        for (int i = 0; i < hijos.size(); i++) {
            boolean ultimo = (i == hijos.size() - 1);
            hijos.get(i).imprimir(prefijo + (esUltimo ? "    " : "│   "), ultimo);
        }
    }
}
