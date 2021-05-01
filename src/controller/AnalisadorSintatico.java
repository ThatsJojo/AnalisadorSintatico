package controller;

import java.util.ArrayList;
import java.util.HashSet;
import model.Token;
import util.FimInesperadoDeArquivo;

public class AnalisadorSintatico {

    private Token currentToken;
    private int erros;
    private ArrayList tokens;
    private int countToken = 0;

    private static final HashSet<String> firstTypes = new HashSet();
    private static final HashSet<String> firstInicio = new HashSet();

    public AnalisadorSintatico() {
        //Conjunto first do método types
        firstTypes.add("int");
        firstTypes.add("real");
        firstTypes.add("boolean");
        firstTypes.add("string");

        //Conjunto first do método Inicio
        firstInicio.add("typedef");
        firstInicio.add("struct");
        firstInicio.add("var");
        firstInicio.add("const");
        firstInicio.add("procedure");
        firstInicio.add("function");
    }

    void analise(ArrayList tokens) throws FimInesperadoDeArquivo {
        this.erros = 0;
        this.tokens = tokens;
        //percorrer toda a lista de tokens até o ultimo elemento
        consumeToken();
        if (firstInicio.contains(currentToken.getLexema())) {
            inicio();
        } else {
            error();
        }
        if (erros == 0) {
            System.out.println("Analise Sintatica não retornou erros");
        } else {
            System.out.println("FORAM DETECTADOS " + this.erros + " SINTATICOS DETECTADOS");
        }
    }

    private boolean consumeToken() throws FimInesperadoDeArquivo {
        if (countToken >= tokens.size()) {
            throw new FimInesperadoDeArquivo();
        }
        this.currentToken = (Token) tokens.get(countToken++);
        return true;
    }

    private Token lookahead() throws FimInesperadoDeArquivo {
        if (countToken >= tokens.size()) {
            throw new FimInesperadoDeArquivo();
        }
        return (Token) tokens.get(countToken + 1);
    }

    private void error() {
        this.erros++;
    }

    private void inicio() throws FimInesperadoDeArquivo {
        switch (currentToken.getLexema()) {
            case "typedef":
                typedefDeclaration();
                inicio();
                break;
            case "struct":
                structDeclaration();
                inicio();
                break;
            case "var":
                varDeclaration();
                header1();
                break;
            case "const":
                constDeclaration();
                header2();
                break;
            case "procedure":
            case "function":
                methods();
                break;
            default:
                error();
        }
    }

    private void header1() throws FimInesperadoDeArquivo {
        switch (currentToken.getLexema()) {
            case "typedef":
                typedefDeclaration();
                header1();
                break;
            case "struct":
                structDeclaration();
                header1();
                break;
            case "const":
                constDeclaration();
                header3();
                break;
            case "procedure":
            case "function":
                methods();
                break;
            default:
                error();
        }
    }

    private void header2() throws FimInesperadoDeArquivo {
        switch (currentToken.getLexema()) {
            case "typedef":
                typedefDeclaration();
                header2();
                break;
            case "struct":
                structDeclaration();
                header2();
                break;
            case "var":
                varDeclaration();
                header3();
                break;
            case "procedure":
            case "function":
                methods();
                break;
            default:
                error();
        }
    }

    private void header3() throws FimInesperadoDeArquivo {
        switch (currentToken.getLexema()) {
            case "typedef":
                typedefDeclaration();
                header3();
                break;
            case "struct":
                structDeclaration();
                header3();
                break;
            case "procedure":
            case "function":
                methods();
                break;
            default:
                error();
        }
    }

    private void methods() throws FimInesperadoDeArquivo {
        Token t = lookahead();
        if (t.getLexema().equals("start") && currentToken.getLexema().equals("procedure")) {
            startProcedure();
        } else {
            functionList();
            methods();
        }

    }

    private void startProcedure() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    private void functionList() throws FimInesperadoDeArquivo {
        switch (currentToken.getLexema()) {
            case "procedure":
                procedure();
                break;
            case "function":
                function();
                break;
            default:
                error();
                break;
        }
    }
    
    private void procedure() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    private void function() throws FimInesperadoDeArquivo {
        consumeToken();
        dataType();
        if(currentToken.getId().equals("IDE")){
            consumeToken();
            if(currentToken.getLexema().equals("(")){
                continueFunction();
            }
            else{
                error();
            }
        }
        else{
            error();
        }
    }
    
    private void continueFunction() throws FimInesperadoDeArquivo {
        consumeToken();
        if(currentToken.getLexema().equals(")")){
            consumeToken();
            parameters();
            blockFunction();
        }
        else if (currentToken.getId().equals("IDE") || firstTypes.contains(currentToken.getLexema())) {
            blockFunction();
        }
        else{
            error();
        }
    }

    private void blockFunction() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    private void parameters() throws FimInesperadoDeArquivo {
        dataType();
        if(currentToken.getId().equals("IDE")){
            paramLoop();
        }
        else{
            error();
        }
        
    }
    
    private void paramLoop() throws FimInesperadoDeArquivo {
        consumeToken();
        switch (currentToken.getLexema()) {
            case ",":
                parameters();
                break;
            case ")":
                consumeToken();
                break;
            default:
                error();
                break;
        }
    }


    //verifica se é um tipo válido
    private void dataType() throws FimInesperadoDeArquivo {
        if (currentToken.getId().equals("IDE") || firstTypes.contains(currentToken.getLexema())) {
            consumeToken();
        } else {
            error();
        }
    }

    //verifica se é um operador relacional
    private void relSymbol() throws FimInesperadoDeArquivo {
        if (currentToken.getId().equals("REL")) {
            consumeToken();
        } else {
            error();
        }
    }

    //verifica se é um operador lógico
    private void logicSymbol() throws FimInesperadoDeArquivo {
        if (currentToken.getId().equals("LOG")) {
            consumeToken();
        } else {
            error();
        }
    }

    private void typedefDeclaration() throws FimInesperadoDeArquivo {
        consumeToken();
        if (currentToken.getLexema().equals("struct")) {
            consumeToken();
            if (currentToken.getId().equals("IDE")) {
                consumeToken();
                if (currentToken.getId().equals("IDE")) {
                    consumeToken();
                    if (currentToken.getLexema().equals(";")) {
                        consumeToken();
                    } else {
                        error();
                    }
                } else {
                    error();
                }
            } else {
                error();
            }
        } else {
            dataType();
            if (currentToken.getId().equals("IDE")) {
                consumeToken();
                if (currentToken.getLexema().equals(";")) {
                    consumeToken();
                } else {
                    error();
                }
            } else {
                error();
            }
        }
    }

    private void constDeclaration() throws FimInesperadoDeArquivo {
        consumeToken();
        if (currentToken.getLexema().equals("{")) {
            consumeToken();
            primeiraConst();
        } else {
            error();
        }
    }

    private void primeiraConst() throws FimInesperadoDeArquivo {
        continueConst();
        constId();
    }

    private void continueConst() throws FimInesperadoDeArquivo {
        if (currentToken.getLexema().equals("struct")) {
            consumeToken();
        }
        dataType();
    }

    private void constId() throws FimInesperadoDeArquivo {
        if (currentToken.getId().equals("IDE")) {
            consumeToken();
            constExpression();
        } else {
            error();
        }
    }

    private void constExpression() throws FimInesperadoDeArquivo {
        switch (currentToken.getLexema()) {
            case "=":
                consumeToken();
                value();
                verifConst();
                break;
            case "[":
                consumeToken();
                vectMatIndex();
                if (currentToken.getLexema().equals("]")) {
                    consumeToken();
                    estruturaConst();
                }
                break;
            default:
                error();
                break;
        }
    }

    private void varDeclaration() throws FimInesperadoDeArquivo {
        consumeToken();
        if (currentToken.getLexema().equals("{")) {
            consumeToken();
            primeiraVar();
        } else {
            error();
        }
    }

    private void primeiraVar() throws FimInesperadoDeArquivo {
        continueVar();
        varId();
    }

    private void structDeclaration() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    private void continueVar() throws FimInesperadoDeArquivo {
        if (currentToken.getLexema().equals("struct")) {
            consumeToken();
        }
        dataType();
    }

    private void varId() throws FimInesperadoDeArquivo {
        if (currentToken.getId().equals("IDE")) {
            consumeToken();
            varExpression();
        } else {
            error();
        }
    }

    private void varExpression() throws FimInesperadoDeArquivo {
        switch (currentToken.getLexema()) {
            case ",":
                consumeToken();
                varId();
                break;
            case "=":
                consumeToken();
                value();
                verifVar();
                break;
            case ";":
                consumeToken();
                proxVar();
                break;
            case "[":
                consumeToken();
                vectMatIndex();
                if (currentToken.getLexema().equals("]")) {
                    consumeToken();
                    estrutura();
                }
                break;
            default:
                error();
                break;
        }
    }

    private void value() throws FimInesperadoDeArquivo {
        Token t = lookahead();
        if (t.getLexema().equals("(") && currentToken.getId().equals("IDE")) {
            consumeToken();
            functionCall();
        } else if (currentToken.getLexema().equals("true") || currentToken.getLexema().equals("false")) {
            consumeToken();
        } //else if()
        else {
            error();
        }
    }

    private void verifVar() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    private void proxVar() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    private void vectMatIndex() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    private void estrutura() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    private void functionCall() throws FimInesperadoDeArquivo {
        int i = 0; //deixar assim enquanto nao tiver o <VALUE> pronto
        if (i == 2) { //colocar conjunto first de value;
            value();
            fCallParams();
        } else if (currentToken.getLexema().equals(")")) {
            consumeToken();
        } else {
            error();
        }

    }

    private void fCallParams() throws FimInesperadoDeArquivo {
        switch (currentToken.getLexema()) {
            case ",":
                value();
                fCallParams();
                break;
            case ")":
                consumeToken();
                break;
            default:
                error();
                break;
        }
    }

    private void verifConst() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    private void estruturaConst() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

}
