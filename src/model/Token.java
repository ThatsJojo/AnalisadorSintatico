/*
Autores: Cleyton Almeida da Silva e Esther de Santana Araújo
Componente Curricular: MI - Processadores de Linguagens de Programação
Concluido em: 12/03/2021
Declaramos que este código foi elaborado por nós de forma "individual" e não contém nenhum
trecho de código de outro colega ou de outro autor, tais como provindos de livros e
apostilas, e páginas ou documentos eletrônicos da Internet. Qualquer trecho de código
de outra autoria que não a nossa está destacado com uma citação para o autor e a fonte
do código, e estamos ciente que estes trechos não serão considerados para fins de avaliação.
 */

package model;

import java.util.Objects;

public class Token {
    private String flag;
    private String lexema;
    private  int linha;
    private  String id;
    private final int codigo;

    public void setLexema(String lexema) {
        this.lexema = lexema;
    }

    public void setLinha(int linha) {
        this.linha = linha;
    }

    public void setId(String id) {
        this.id = id;
    }
    private static int count = 0;
    
    private static synchronized int getCodigo(){
        return count++;
    }

    public Token(String id, String lexema, int linha) {
        flag = null;
        this.lexema = lexema;
        this.linha = linha;
        this.id = id;
        this.codigo = Token.getCodigo();
        
    }

    public String getLexema() {
        return lexema;
    }
    
    public String getFlag() {
        return flag;
    }
    
    public void setFlag(String flag){
        this.flag = flag;
    }

    public int getLinha() {
        return linha;
    }

    public String getId() {
        return id;
    }

    @Override
    public String toString() {
        return String.format("%04d", linha)+" "+id+" "+lexema;
    }

    @Override
    public int hashCode() {
        return lexema.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Token other = (Token) obj;
        if (!Objects.equals(this.lexema, other.lexema)) {
            return false;
        }
        return true;
    }

    

    
    
    
}
