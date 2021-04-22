package model;

public class Simbolo {
    private final Token token;
    private String categoria;
    private String tipo;
    private String valor;
    private Object variavel;

    public Simbolo(Token token) {
        this.token = token;
    }

    public Token getToken() {
        return token;
    }

    public String getCategoria() {
        return categoria;
    }

    public void setCategoria(String categoria) {
        this.categoria = categoria;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public String getValor() {
        return valor;
    }

    public void setValor(String valor) {
        this.valor = valor;
    }

    public Object getVariavel() {
        return variavel;
    }

    public void setVariavel(Object variavel) {
        this.variavel = variavel;
    }
    
}