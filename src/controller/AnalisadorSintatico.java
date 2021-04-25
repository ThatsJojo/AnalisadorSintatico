package controller;

import java.util.HashSet;
import java.util.Iterator;
import model.Token;

public class AnalisadorSintatico {

    private Token currentToken;
    private static final HashSet<String> dataType = new HashSet();
    private boolean error = false;

    void analise(Iterator tokens) {

        dataType.add("int");
        dataType.add("real");
        dataType.add("boolean");
        dataType.add("string");

        while (tokens.hasNext()) { //percorrer toda a lista de tokens até o ultimo elemento
            currentToken = (Token) tokens.next();
            if (currentToken.getLexema().equals("typedef")) {
                this.Typedef(tokens);
            } else {
                //System.out.println("Sad" + currentToken);
            }
        }

        if (!this.error) {
            System.out.println("Analise Sintatica não retornou erros");
        }
    }

    public void Typedef(Iterator tokens) {
        this.currentToken = (Token) tokens.next();
        if (dataType.contains(this.currentToken.getLexema())) {

            this.currentToken = (Token) tokens.next();
            if (this.currentToken.getId().equals("IDE")) {
                this.currentToken = (Token) tokens.next();
                if (!this.currentToken.getLexema().equals(";")) {
                    this.error = true;
                }
            } else {
                this.error = true;
            }
        } else if (this.currentToken.getLexema().equals("struct")) {

            this.currentToken = (Token) tokens.next();
            for (int i = 0; i < 2; i++) {
                if (this.currentToken.getId().equals("IDE")) {
                    this.currentToken = (Token) tokens.next();
                } else {
                    this.error = true;
                    System.out.println("Erro " + this.currentToken.getLexema() + "linha: " + this.currentToken.getLinha() + "Esperava um IDE");
                    return;
                }
            }
            if (!this.currentToken.getLexema().equals(";")) {
                this.error = true;
                System.out.println("Erro " + this.currentToken.getLexema() + "linha: " + this.currentToken.getLinha() + "Esperava um ;");
                return;
            }
        } else {
            this.error = true;
        }
    }
}
