package model;

import java.util.HashMap;
import util.EscopoPaiException;

public class TabelaSimbolos {
    private final HashMap<Token,Simbolo>  simbolos;
    private final TabelaSimbolos escopoPai;
    
    public TabelaSimbolos(TabelaSimbolos ep){
        simbolos = new HashMap();
        escopoPai = ep;
    }
    
    public Simbolo inserirSimbolo (Token t, String categoria, String tipo, String valor, Object variavel) {
        Simbolo ret = new Simbolo(t,categoria,tipo,valor,variavel);
        simbolos.put(t, ret);
        return ret;
    }
    
    public Simbolo getSimbolo(Token t){
        return simbolos.get(t);
    }

    public TabelaSimbolos getEscopoPai() throws EscopoPaiException{
        if (escopoPai == null)
            throw new EscopoPaiException();
        return escopoPai;
    }
    
    
    
}
