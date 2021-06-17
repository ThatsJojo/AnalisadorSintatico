package model;

import java.util.HashMap;
import java.util.LinkedList;
import util.EscopoPaiException;
import util.identificadorJaUtilizado;
import util.identificadorNaoEncontrado;

public class TabelaSimbolos {

    private final HashMap<Token, LinkedList<Simbolo>> simbolos;
  // private final HashMap<Token, LinkedList<Simbolo>> functions;
    private final HashMap<Token, Simbolo> tipos;
    private final TabelaSimbolos escopoPai;
    private final LinkedList<Token> array;

    public TabelaSimbolos(TabelaSimbolos ep) {
        //functions = new HashMap();
        Token t = new Token("PRE", "tipo", 0);
        simbolos = new HashMap();
        tipos = new HashMap();
        escopoPai = ep;
        array = new LinkedList();
    }

    public Simbolo inserirSimbolo(Token t, String categoria, String tipo, String valor, Object variavel) throws identificadorJaUtilizado {
        Simbolo ret = new Simbolo(t, categoria, tipo, valor, variavel);
        //System.out.println("INSERINDO SÍMBOLO: "+t.getLexema()+" "+(simbolos.containsKey(t)));
        LinkedList<Simbolo> lista = simbolos.get(t);
        boolean flag = true;

        if (lista == null) {
            flag = false;
            lista = new LinkedList<>();
            lista.add(ret);
        }
        if (flag) {
            throw new identificadorJaUtilizado();
        }
        if (categoria.equals("tipo") || categoria.equals("struct")) {
            if (escopoPai != null && escopoPai.contains(t)) {
                throw new identificadorJaUtilizado();
            }
            tipos.put(t, ret);
        }
        simbolos.put(t, lista);
        array.add(t);
        return ret;
    }

    /*public Simbolo inserirFuncao(Token t, LinkedList<Simbolo> lista){
        Simbolo ret = new Simbolo(t, categoria, tipo, valor, variavel);
        functions.put(t, lista);
        return ret;
    }*/
    
    private boolean contains(Token identificador) {
        return simbolos.containsKey(identificador) || (escopoPai != null && escopoPai.contains(identificador));
    }

    public void printTipos() {
        tipos.forEach((a, b) -> {
            System.out.println(a.getLexema());
        });
    }
    
    public void printSimbolos() {
        /*simbolos.forEach((a, b) -> {
            System.out.println(a.getLexema());
        });*/
        array.forEach((simb)->{
            System.out.println(simb.getLexema());
        });
    }

    public LinkedList<Simbolo> getSimbolo(Token t) throws identificadorNaoEncontrado {
        LinkedList<Simbolo> lista = simbolos.get(t);
        if ((lista == null) && (escopoPai != null)) {
            lista = escopoPai.getSimbolo(t);
        }
        if (lista == null) {
            throw new identificadorNaoEncontrado();
        }
        return lista;
    }

    public Simbolo getTipo(Token t) throws identificadorNaoEncontrado {
        Simbolo ret = tipos.get(t);
        if (ret == null) {
            if (escopoPai != null) {
                ret = escopoPai.getTipo(t);
            }
            if (ret == null) {
                throw new identificadorNaoEncontrado();
            }
        }
        return ret;
    }
    
    public HashMap<Token, Simbolo> getTipos(){
        return tipos;
    }

    public TabelaSimbolos getEscopoPai() throws EscopoPaiException {
        if (escopoPai == null) {
            throw new EscopoPaiException();
        }
        return escopoPai;
    }

}
