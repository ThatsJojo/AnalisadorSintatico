package controller;

import java.util.HashSet;
import java.util.Iterator;
import model.Token;

public class AnalisadorSintatico {

    private Token currentToken;
    private static final HashSet<String> dataType = new HashSet();
    private boolean error = false;
    int estado = 0;
    boolean semicolon = false;

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
        } else {
            System.out.println("ERROS SINTATICOS DETECTADOS");
        }
    }

    public void Typedef(Iterator tokens) {
        estado = 0;
        while (tokens.hasNext()) {
            this.currentToken = (Token) tokens.next();
            switch (estado) {
                case 0:
                    if (dataType.contains(this.currentToken.getLexema())) {
                        estado = 2;
                    } else if (this.currentToken.getLexema().equals("struct")) {
                        estado = 1;
                    } else {
                        this.error = true;
                        System.out.println(this.currentToken.getLinha() + " Erro sintático: Esperava um tipo");
                        return;
                    }
                    break;
                case 1:
                    if (this.currentToken.getId().equals("IDE")) {
                        estado = 4;
                    } else {
                        System.out.println(this.currentToken.getLinha() + " Erro sintático: Esperava um IDE");
                        this.error = true;
                        return;
                    }
                    break;
                case 2:
                    if (this.currentToken.getId().equals("IDE")) {
                        estado = 3;
                        semicolon = false;
                    } else {
                        this.error = true;
                        System.out.println(this.currentToken.getLexema() + "IDE");
                    }
                    break;
                case 3:
                    if (!this.currentToken.getLexema().equals(";")) {
                        this.error = true;
                        System.out.println(this.currentToken.getLinha() + " Erro sintático: Esperava um ;");
                        return;

                    } else {
                        semicolon = true;
                        return;
                    }
                case 4:
                    if (this.currentToken.getId().equals("IDE")) {
                        estado = 3;
                        semicolon = false;
                    } else {
                        System.out.println(this.currentToken.getLinha() + " Erro sintático: Esperava um IDE");
                        this.error = true;
                        return;
                    }
                    break;
                default:
                    this.error = true;

            }
        }
        if (!semicolon) {
            System.out.println(this.currentToken.getLinha() + " Erro sintático: Esperava um ;");
            this.error = true;
        }
    }
}
