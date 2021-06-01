package model;

import java.util.HashMap;
import java.util.LinkedList;
import util.EscopoPaiException;
import util.identificadorJaUtilizado;
import util.identificadorNaoEncontrado;

public class TabelaSimbolos {
    private final HashMap<Token, LinkedList<Simbolo>>  simbolos;
    private final TabelaSimbolos escopoPai;
    
    public TabelaSimbolos(TabelaSimbolos ep){
        simbolos = new HashMap();
        escopoPai = ep;
    }
    
    public Simbolo inserirSimbolo (Token t, String categoria, String tipo, String valor, Object variavel) throws identificadorJaUtilizado {
        Simbolo ret = new Simbolo(t,categoria,tipo,valor,variavel);
        //System.out.println("INSERINDO SÍMBOLO: "+t.getLexema()+" "+(simbolos.containsKey(t)));
        LinkedList<Simbolo> lista = simbolos.get(t);
        boolean flag = true;
        
        if(lista == null){
            flag = false;
            lista = new LinkedList<>();
            lista.add(ret);
        }
        if(flag){
            throw new identificadorJaUtilizado();
        }
        simbolos.put(t, lista);
        return ret;
    }
    
    public LinkedList<Simbolo> getSimbolo(Token t) throws identificadorNaoEncontrado{
        LinkedList<Simbolo> lista = simbolos.get(t);
        if(lista == null)
            throw new identificadorNaoEncontrado();
        return lista;
    }

    public TabelaSimbolos getEscopoPai() throws EscopoPaiException{
        if (escopoPai == null)
            throw new EscopoPaiException();
        return escopoPai;
    }
    
}
