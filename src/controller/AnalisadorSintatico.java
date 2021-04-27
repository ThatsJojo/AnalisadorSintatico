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
    
    
    public AnalisadorSintatico(){
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
        if(firstInicio.contains(currentToken.getLexema()))
            inicio();
        else
            error();
        if (erros==0) {
            System.out.println("Analise Sintatica não retornou erros");
        } else {
            System.out.println("FORAM DETECTADOS "+this.erros+" SINTATICOS DETECTADOS");
        }
    }
    
    
    private boolean consumeToken() throws FimInesperadoDeArquivo {
        if(countToken>=tokens.size())
            throw new FimInesperadoDeArquivo();
        this.currentToken = (Token) tokens.get(countToken++);
        return true;
    }
    
    private Token lookahead() throws FimInesperadoDeArquivo{
        if(countToken>=tokens.size())
            throw new FimInesperadoDeArquivo();
        return (Token) tokens.get(countToken+1);
    }
    
    private void error(){
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
    
    private void methods() throws FimInesperadoDeArquivo{
        Token t = lookahead();
        if(t.getLexema().equals("start"))
            startProcedure();
        else{
            functionList();
            methods();
        }
        
    }
    
    private void startProcedure() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    private void functionList() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
    private void dataType() throws FimInesperadoDeArquivo{
            if(currentToken.getId().equals("IDE")||firstTypes.contains(currentToken.getLexema()))
                consumeToken();
            else
                error();
    }

    private void typedefDeclaration() throws FimInesperadoDeArquivo{
        consumeToken();
        if(currentToken.getLexema().equals("struct")){
            consumeToken();
            if(currentToken.getId().equals("IDE")){
                consumeToken();
                if(currentToken.getId().equals("IDE")){
                    consumeToken();
                    if(currentToken.getLexema().equals(";"))
                        consumeToken();
                    else
                        error();
                }else
                    error();
            }else
                error();
        }else{
            dataType();
            if(currentToken.getId().equals("IDE")){
                consumeToken();
                if(currentToken.getLexema().equals(";"))
                   consumeToken();
                else
                    error();
            }else
                error();
        }
    }

    

    private void constDeclaration() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    private void varDeclaration() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    private void structDeclaration() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    
}
