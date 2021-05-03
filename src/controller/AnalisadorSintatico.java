package controller;

import java.util.ArrayList;
import java.util.HashSet;
import model.Arquivo;
import model.Token;
import util.FimInesperadoDeArquivo;

public class AnalisadorSintatico {

    private Token privateCurrentToken;
    private int erros;
    private ArrayList tokens;
    private int countToken = 0;

    private static final HashSet<String> firstTypes = new HashSet();
    private static final HashSet<String> firstInicio = new HashSet();
    private static final HashSet<String> firstComando = new HashSet();

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

        //Conjunto first do método comando
        firstComando.add("print");
        firstComando.add("read");
        firstComando.add("while");
        firstComando.add("const");
        firstComando.add("typedef");
        firstComando.add("struct");
        firstComando.add("if");
    }

    void analise(Arquivo arq, ArrayList tokens) throws FimInesperadoDeArquivo {
        this.erros = 0;
        this.tokens = tokens;
        //percorrer toda a lista de tokens até o ultimo elemento
        consumeToken();
        if (firstInicio.contains(currentToken().getLexema())) {
            inicio();
        } else {
            error();
        }
        if (erros == 0) {
            System.out.println("Análise Sintática no arquivo " + arq.getNome() + " não retornou erros");
        } else {
            System.out.println("FORAM DETECTADOS " + this.erros + " ERROS SINTÁTICOS NO ARQUIVO " + arq.getNome());
        }
    }

    private boolean consumeToken() throws FimInesperadoDeArquivo {
        if (countToken > tokens.size()) {
            throw new FimInesperadoDeArquivo();
        }
        else if(countToken == tokens.size()){
            this.privateCurrentToken = new Token("EOF", null, privateCurrentToken.getLinha());
            countToken++;
            return false;
        }
        else{
            this.privateCurrentToken = (Token) tokens.get(countToken++); 
            return true;
        }
        
    }
    
    private Token currentToken() throws FimInesperadoDeArquivo{
        if(privateCurrentToken == null || privateCurrentToken.getId().equals("EOF")){
            throw new FimInesperadoDeArquivo();
        }
        return privateCurrentToken;
    }

    private Token lookahead() throws FimInesperadoDeArquivo {
        if (countToken >= tokens.size()) {
            throw new FimInesperadoDeArquivo();
        }
        return (Token) tokens.get(countToken);
    }

    private void error() {
        this.erros++;
    }

//================================== Cabeçalhos de início do código ==================================
//****************************************************************************************************     
    private void inicio() throws FimInesperadoDeArquivo {
        switch (currentToken().getLexema()) {
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
        switch (currentToken().getLexema()) {
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
        switch (currentToken().getLexema()) {
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
        switch (currentToken().getLexema()) {
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
        if (t.getLexema().equals("start") && currentToken().getLexema().equals("procedure")) {
            consumeToken();
            startProcedure();
        } else if (t.getId().equals("IDE")) {
            functionList();
            methods();
        } else {
            error();
        }
    }

    private void functionList() throws FimInesperadoDeArquivo {
        switch (currentToken().getLexema()) {
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
//********************************** Cabeçalhos de início do código **********************************      
//====================================================================================================

//============================================ Data Types ============================================
//**************************************************************************************************** 
    //Incompleto
    private void value() throws FimInesperadoDeArquivo {
        Token t = lookahead();
        if (currentToken().getId().equals("IDE")) {
            if (t.getLexema().equals("(")) {
                consumeToken();
                functionCall();
            } else {
                consumeToken();
            }
        } else if (currentToken().getLexema().equals("true") || currentToken().getLexema().equals("false")) {
            consumeToken();
        } //else if()
        else {
            error();
        }
    }

    //verifica se é um tipo válido
    private void dataType() throws FimInesperadoDeArquivo {
        if (currentToken().getId().equals("IDE") || firstTypes.contains(currentToken().getLexema())) {
            consumeToken();
        } else {
            error();
        }
    }

    private void vectMatIndex() throws FimInesperadoDeArquivo {
        if (currentToken().getId().equals("NRO")) {
            consumeToken();
        } else {
            error();
        }
    }

    private void aritmeticValue() throws FimInesperadoDeArquivo{
        switch (currentToken().getId()) {
            case "NRO":
            case "CAD":
                consumeToken();
                break;
            default:
                variavel();
        }
    }
    
    private void variavel() throws FimInesperadoDeArquivo{
        if(currentToken().getId().equals("IDE")){
            consumeToken();
            String aheadToken = lookahead().getLexema();
            if(aheadToken.equals(".")|aheadToken.equals("["))
                contElement();
        }else{
            switch (currentToken().getLexema()) {
                case "global":
                case "local":
                    consumeToken();
                    if(currentToken().getLexema().equals(".")){
                        consumeToken();
                        if(currentToken().getId().equals("IDE"))
                            consumeToken();
                        else
                            error();
                    }else
                        error();
                    break;
                default:
                    startElement();
            }
        }
        
    }
    
    private void startElement(){
        
    }
    
    private void contElement(){
        
    }
//******************************************** Data Types ********************************************   
//====================================================================================================
    
    
    
//======================================= Variable Declaration =======================================
//****************************************************************************************************
    private void varDeclaration() throws FimInesperadoDeArquivo {
        consumeToken();
        if (currentToken().getLexema().equals("{")) {
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

    private void continueVar() throws FimInesperadoDeArquivo {
        if (currentToken().getLexema().equals("struct")) {
            consumeToken();
        }
        dataType();
    }

    private void varId() throws FimInesperadoDeArquivo {
        if (currentToken().getId().equals("IDE")) {
            consumeToken();
            varExpression();
        } else {
            error();
        }
    }

    private void varExpression() throws FimInesperadoDeArquivo {
        switch (currentToken().getLexema()) {
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
                if (currentToken().getLexema().equals("]")) {
                    consumeToken();
                    estrutura();
                }
                break;
            default:
                error();
                break;
        }
    }

    private void estrutura() throws FimInesperadoDeArquivo {
        switch (currentToken().getLexema()) {
            case "[":
                consumeToken();
                vectMatIndex();
                if (currentToken().getLexema().equals("]")) {
                    consumeToken();
                    contMatriz();
                } else {
                    error();
                }
                break;
            case "=":
                consumeToken();
                initVetor();
                break;
            case ",":
                consumeToken();
                varId();
                break;
            case ";":
                consumeToken();
                proxVar();
                break;
            default:
                error();
        }
    }

    private void initVetor() throws FimInesperadoDeArquivo {
        if (currentToken().getLexema().equals("[")) {
            consumeToken();
            value();
            proxVetor();
        } else {
            error();
        }
    }

    private void proxVetor() throws FimInesperadoDeArquivo {
        switch (currentToken().getLexema()) {
            case ",":
                consumeToken();
                value();
                proxVetor();
                break;
            case "]":
                consumeToken();
                verifVar();
                break;
            default:
                error();
        }
    }

    private void contMatriz() throws FimInesperadoDeArquivo {
        switch (currentToken().getLexema()) {
            case "=":
                consumeToken();
                initMatriz();
                break;
            case ",":
                consumeToken();
                varId();
                break;
            case ";":
                consumeToken();
                proxVar();
                break;
            default:
                error();
        }
    }

    private void initMatriz() throws FimInesperadoDeArquivo {
        if (currentToken().getLexema().equals("[")) {
            consumeToken();
            matrizValue();
        } else {
            error();
        }
    }

    private void matrizValue() throws FimInesperadoDeArquivo {
        if (currentToken().getLexema().equals("[")) {
            consumeToken();
            value();
            proxMatriz();
        } else {
            error();
        }
    }

    private void proxMatriz() throws FimInesperadoDeArquivo {
        switch (currentToken().getLexema()) {
            case ",":
                consumeToken();
                value();
                proxMatriz();
                break;
            case "]":
                consumeToken();
                next();
                break;
            default:
                error();
        }
    }

    private void next() throws FimInesperadoDeArquivo {
        switch (currentToken().getLexema()) {
            case ",":
                consumeToken();
                matrizValue();
                break;
            case "]":
                consumeToken();
                verifVar();
                break;
            default:
                error();
        }
    }

    private void verifVar() throws FimInesperadoDeArquivo {
        switch (currentToken().getLexema()) {
            case ",":
                consumeToken();
                varId();
                break;
            case ";":
                consumeToken();
                proxVar();
                break;
            default:
                error();
                break;
        }
    }

    private void proxVar() throws FimInesperadoDeArquivo {
        if (currentToken().getLexema().equals("}")) {
            consumeToken();
        } else {
            continueVar();
            varId();
        }
    }
//*************************************** Variable Declaration ***************************************      
//====================================================================================================

//========================================= Const Declaration ========================================
//**************************************************************************************************** 
    private void constDeclaration() throws FimInesperadoDeArquivo {
        consumeToken();
        if (currentToken().getLexema().equals("{")) {
            consumeToken();
            continueConst();
            constId();
        } else {
            error();
        }
    }

    private void continueConst() throws FimInesperadoDeArquivo {
        if (currentToken().getLexema().equals("struct")) {
            consumeToken();
        }
        dataType();
    }

    private void proxConst() throws FimInesperadoDeArquivo {
        if (currentToken().getLexema().equals("}")) {
            consumeToken();
        } else {
            continueConst();
            constId();
        }
    }

    private void constId() throws FimInesperadoDeArquivo {
        if (currentToken().getId().equals("IDE")) {
            consumeToken();
            constExpression();
        } else {
            error();
        }
    }

    private void constExpression() throws FimInesperadoDeArquivo {
        switch (currentToken().getLexema()) {
            case "=":
                consumeToken();
                value();
                verifConst();
                break;
            case "[":
                consumeToken();
                vectMatIndex();
                if (currentToken().getLexema().equals("]")) {
                    consumeToken();
                    estruturaConst();
                } else {
                    error();
                }
                break;
            default:
                error();
                break;
        }
    }

    private void estruturaConst() throws FimInesperadoDeArquivo {
        switch (currentToken().getLexema()) {
            case "=":
                consumeToken();
                if (currentToken().getLexema().equals("[")) {
                    consumeToken();
                    value();
                    proxConstVetor();
                } else {
                    error();
                }
                break;
            case "[":
                consumeToken();
                vectMatIndex();
                if (currentToken().getLexema().equals("]")) {
                    consumeToken();
                    if (currentToken().getLexema().equals("=")) {
                        consumeToken();
                        initConstMatriz();
                    } else {
                        error();
                    }
                } else {
                    error();
                }
                break;
            default:
                error();
                break;
        }
    }

    private void proxConstVetor() throws FimInesperadoDeArquivo {
        switch (currentToken().getLexema()) {
            case ",":
                consumeToken();
                value();
                proxConstVetor();
                break;
            case "]":
                consumeToken();
                verifConst();
                break;
            default:
                error();
                break;
        }
    }

    private void initConstMatriz() throws FimInesperadoDeArquivo {
        if (currentToken().getLexema().equals("[")) {
            consumeToken();
            matrizConstValue();
        } else {
            error();
        }
    }

    private void matrizConstValue() throws FimInesperadoDeArquivo {
        if (currentToken().getLexema().equals("[")) {
            consumeToken();
            value();
            proxConstMatriz();
        } else {
            error();
        }
    }

    private void proxConstMatriz() throws FimInesperadoDeArquivo {
        switch (currentToken().getLexema()) {
            case ",":
                consumeToken();
                value();
                proxConstMatriz();
                break;
            case "]":
                consumeToken();
                nextConst();
                break;
            default:
                error();
        }
    }

    private void nextConst() throws FimInesperadoDeArquivo {
        switch (currentToken().getLexema()) {
            case ",":
                consumeToken();
                matrizConstValue();
                break;
            case "]":
                consumeToken();
                verifConst();
                break;
            default:
                error();
        }
    }

    private void verifConst() throws FimInesperadoDeArquivo {
        switch (currentToken().getLexema()) {
            case ",":
                consumeToken();
                constId();
                break;
            case ";":
                consumeToken();
                proxConst();
                break;
            default:
                error();
                break;
        }
    }
//***************************************** Const Declaration ****************************************  
//====================================================================================================

//======================================= Function Declaration =======================================
//****************************************************************************************************
    private void function() throws FimInesperadoDeArquivo {
        consumeToken();
        dataType();
        if (currentToken().getId().equals("IDE")) {
            consumeToken();
            if (currentToken().getLexema().equals("(")) {
                continueFunction();
            } else {
                error();
            }
        } else {
            error();
        }
    }

    private void continueFunction() throws FimInesperadoDeArquivo {
        consumeToken();
        if (currentToken().getLexema().equals(")")) {
            consumeToken();
            blockFunction();
        } else if (currentToken().getId().equals("IDE") || firstTypes.contains(currentToken().getLexema())) {
            parameters();
            blockFunction();
        } else {
            error();
        }
    }

    private void blockFunction() throws FimInesperadoDeArquivo {
        if (currentToken().getLexema().equals("{")) {
            blockFuncContent();
            if (currentToken().getLexema().equals(";")) {
                consumeToken();
                if (currentToken().getLexema().equals("}")) {
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
    }

    private void blockFuncContent() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    private void parameters() throws FimInesperadoDeArquivo {
        dataType();
        if (currentToken().getId().equals("IDE")) {
            paramLoop();
        } else {
            error();
        }

    }

    private void paramLoop() throws FimInesperadoDeArquivo {
        consumeToken();
        switch (currentToken().getLexema()) {
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
//*************************************** Function Declaration ***************************************  
//====================================================================================================

//======================================== Struct Declaration ========================================
//**************************************************************************************************** 
    private void structDeclaration() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
//**************************************** Struct Declaration ****************************************  
//====================================================================================================

//====================================== Procedure Declaration =======================================
//****************************************************************************************************     
    private void startProcedure() throws FimInesperadoDeArquivo {
        consumeToken();
        if (currentToken().getLexema().equals("(")) {
            consumeToken();
            if (currentToken().getLexema().equals(")")) {
                consumeToken();
                if (currentToken().getLexema().equals("{")) {
                    consumeToken();
                    procedureContent();
                } else {
                    error();
                }
            } else {
                error();
            }
        } else {
            error();
        }

    }

    private void procedureContent() throws FimInesperadoDeArquivo {
        if (currentToken().getLexema().equals("var")) {
            varDeclaration();
            procedureContent2();
        } else if (currentToken().getLexema().equals("const")) {
            constDeclaration();
            procedureContent3();
        } else {
            error();
        }
    }

    private void procedureContent2() throws FimInesperadoDeArquivo {
        if (currentToken().getLexema().equals("const")) {
            constDeclaration();
            procedureContent4();
        } else if (firstComando.contains(currentToken().getLexema())) {
            codigo();
            if (currentToken().getLexema().equals("}")) {
                consumeToken();
            } else {
                error();
            }
        } else if (firstComando.contains(currentToken().getLexema())) {
            codigo();

        } else {
            error();
        }
    }

    private void procedureContent3() throws FimInesperadoDeArquivo {
        if (currentToken().getLexema().equals("var")) {
            varDeclaration();
            procedureContent4();
        } else if (firstComando.contains(currentToken().getLexema())) {
            codigo();
            if (currentToken().getLexema().equals("}")) {
                consumeToken();
            } else {
                error();
            }
        } else {
            error();
        }
    }

    private void procedureContent4() throws FimInesperadoDeArquivo {
        if (firstComando.contains(currentToken().getLexema())) {
            codigo();
            if (currentToken().getLexema().equals("}")) {
                consumeToken();
            } else {
                error();
            }
        } else {
            error();
        }
    }

    private void procedure() throws FimInesperadoDeArquivo {
        consumeToken();
        if (currentToken().getId().equals("IDE")) {
            consumeToken();
            if (currentToken().getLexema().equals("(")) {
                procedureParams();
                if (currentToken().getLexema().equals("{")) {
                    consumeToken();
                    procedureContent();
                } else {
                    error();
                }
            } else {
                error();
            }
        } else {
            error();
        }
    }

    private void procedureParams() throws FimInesperadoDeArquivo {
        consumeToken();
        if (currentToken().getLexema().equals(")")) {
            consumeToken();
        } else if (currentToken().getId().equals("IDE") || firstTypes.contains(currentToken().getLexema())) {
            parameters();
        } else {
            error();
        }
    }
//************************************** Procedure Declaration ***************************************  
//====================================================================================================

//====================================== Codigo ======================================================
//****************************************************************************************************  
    private void codigo() throws FimInesperadoDeArquivo {
        if (firstComando.contains(currentToken().getLexema())) { // adicionar opções de functionCall, incremento, decremento
            comando();                                       //e atribuição
            codigo();
        }
    }

    private void comando() throws FimInesperadoDeArquivo {
        switch (currentToken().getLexema()) {
            case "print":
                print();
                break;
            case "read":
                read();
                break;
            case "typedef":
                typedefDeclaration();
                break;
            case "struct":
                structDeclaration();
                break;
            case "if":
                conditional();
                break;
            case "while":
                whileLoop();
                break;
            default:
                break;
        }
    }

    private void print() throws FimInesperadoDeArquivo {
        consumeToken();
        if (currentToken().getLexema().equals("(")) {
            printableList();
        } else {
            error();
        }
    }

    private void printableList() throws FimInesperadoDeArquivo {
        value();
        nextPrintValue();
    }

    private void nextPrintValue() throws FimInesperadoDeArquivo {
        consumeToken();
        switch (currentToken().getLexema()) {
            case ",":
                printableList();
                break;
            case ")":
                consumeToken();
                if (currentToken().getLexema().equals(";")) {
                    consumeToken();
                } else {
                    error();
                }
                break;
            default:
                error();
                break;
        }
    }

    private void read() throws FimInesperadoDeArquivo {
        consumeToken();
        if (currentToken().getLexema().equals("(")) {
            readParams();
        } else {
            error();
        }
    }

    private void readParams() throws FimInesperadoDeArquivo {
        variavel();
        readLoop();
    }

    private void readLoop() throws FimInesperadoDeArquivo {
        switch (currentToken().getLexema()) {
            case ",":
                readParams();
                break;
            case ")":
                consumeToken();
                if (currentToken().getLexema().equals(";")) {
                    consumeToken();
                    
                } else {
                    error();
                }
                break;
            default:
                error();
                break;
        }
    }

    private void conditional() {

    }

    private void whileLoop() {

    }

//************************************** Codigo ****************************************************** 
//====================================================================================================
//====================================== Procedure Declaration =======================================
//**************************************************************************************************** 
    private void typedefDeclaration() throws FimInesperadoDeArquivo {
        consumeToken();
        if (currentToken().getLexema().equals("struct")) {
            consumeToken();
            if (currentToken().getId().equals("IDE")) {
                consumeToken();
                if (currentToken().getId().equals("IDE")) {
                    consumeToken();
                    if (currentToken().getLexema().equals(";")) {
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
            if (currentToken().getId().equals("IDE")) {
                consumeToken();
                if (currentToken().getLexema().equals(";")) {
                    consumeToken();
                } else {
                    error();
                }
            } else {
                error();
            }
        }
    }
//*************************************** Typedef Declaration ****************************************  
//====================================================================================================

    

//=========================================== Operations =============================================  
//****************************************************************************************************    
    private void opNeage() throws FimInesperadoDeArquivo{
        switch (currentToken().getLexema()) {
            case "-":
                consumeToken();
                aritmeticValue();
                break;
            case "(":
                consumeToken();
                aritmeticValue();
                if(currentToken().getLexema().equals(")"))
                    consumeToken();
                else
                    error();
                break;
            default:
                aritmeticValue();
        }
    }


//verifica se é um operador relacional
    private void relSymbol() throws FimInesperadoDeArquivo {
        if (currentToken().getId().equals("REL")) {
            consumeToken();
        } else {
            error();
        }
    }

    //verifica se é um operador lógico
    private void logicSymbol() throws FimInesperadoDeArquivo {
        if (currentToken().getId().equals("LOG")) {
            consumeToken();
        } else {
            error();
        }
    }
//******************************************* Operations *********************************************  
//====================================================================================================
    
    
    
//========================================== Function Call ===========================================  
//****************************************************************************************************
    private void functionCall() throws FimInesperadoDeArquivo {
        int i = 0; //deixar assim enquanto nao tiver o <VALUE> pronto
        if (i == 2) { //colocar conjunto first de value;
            value();
            fCallParams();
        } else if (currentToken().getLexema().equals(")")) {
            consumeToken();
        } else {
            error();
        }

    }

    private void fCallParams() throws FimInesperadoDeArquivo {
        switch (currentToken().getLexema()) {
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
//****************************************** Function Call *******************************************  
//====================================================================================================

}
