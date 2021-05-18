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
    private String analiseret = "";

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

        //Conjunto first do método comando //faltam algumas coisas
        firstComando.add("print");
        firstComando.add("read");
        firstComando.add("while");
        firstComando.add("const");
        firstComando.add("typedef");
        firstComando.add("struct");
        firstComando.add("if");

    }

    public String analise(Arquivo arq, ArrayList tokens) {
        analiseret = "";
        this.erros = 0;
        this.tokens = tokens;
        countToken = 0;
        privateCurrentToken = null;
        //percorrer toda a lista de tokens até o ultimo elemento
        try{
            consumeToken();
                inicio();
        }catch(FimInesperadoDeArquivo e){
            analiseret += ""+(privateCurrentToken==null?""+privateCurrentToken.getId():"0000")
                    +" ERRO SINTÁTICO. EOF";
        }
        System.out.println("Análise Sintática realizada "+(erros==0?"com":"sem")+" sucesso ("+String.format("%03d", erros)
                +" erros sintáticos encontrados) no arquivo " + arq.getNome() + ".");
        return analiseret;
    }

    private boolean consumeToken() throws FimInesperadoDeArquivo {
        return consumeToken(false);
    }
    
    private boolean consumeToken(Boolean tokenErro)throws FimInesperadoDeArquivo{
        if (countToken > tokens.size()) {
            throw new FimInesperadoDeArquivo();
        } else if (countToken == tokens.size()) {
            if(!tokenErro)
                analiseret += ""+privateCurrentToken.getLinha()+" "+
                    privateCurrentToken.getId()+" "+privateCurrentToken.getLexema()+"\n";
            this.privateCurrentToken = new Token("EOF", null, privateCurrentToken.getLinha());
            countToken++;
            return false;
        } else {
            if(countToken!=0&&!tokenErro)
                analiseret += ""+privateCurrentToken.getLinha()+" "+
                    privateCurrentToken.getId()+" "+privateCurrentToken.getLexema()+"\n";
            this.privateCurrentToken = (Token) tokens.get(countToken++);
            
            return true;
        }

    }

    private Token currentToken() throws FimInesperadoDeArquivo {
        if (privateCurrentToken == null || privateCurrentToken.getId().equals("EOF")) {
            erros++;
            throw new FimInesperadoDeArquivo();
        }
        return privateCurrentToken;
    }

    private Token lookahead() throws FimInesperadoDeArquivo {
        if (countToken >= tokens.size()) {
            erros++;
            throw new FimInesperadoDeArquivo();
        }
        return (Token) tokens.get(countToken);
    }
    
    private Token lookaheadP() throws FimInesperadoDeArquivo{
        if (countToken >= tokens.size()) {
            erros++;
            throw new FimInesperadoDeArquivo();
        }
        return (Token) tokens.get(countToken+1);
    }

    private void error(String esperado, boolean consumir) throws FimInesperadoDeArquivo {
        analiseret+=""+privateCurrentToken.getLinha()+" "+privateCurrentToken.getId()+" "+privateCurrentToken.getLexema()+" ERRO SINTÁTICO. ESPERAVA: "+
                esperado+".  Encontrado: "+currentToken().getLexema()+"\n";
        
        System.out.println("Erro"+erros+" no token " + currentToken().getId() + " na linha " 
                + currentToken().getLinha() + ": " + currentToken().getLexema() 
                + " lookahead: " + lookahead().getLexema());
        this.erros++;
        
        int i = 10/0;
        if(consumir)
            consumeToken(true);
    }

//================================== Cabeçalhos de início do código ==================================
//****************************************************************************************************     
    private void inicio() throws FimInesperadoDeArquivo {
        switch (currentToken().getLexema()) {
            case "typedef":
                consumeToken();
                typedefDeclaration();
                inicio();
                break;
            case "struct":
                consumeToken();
                structDeclaration();
                inicio();
                break;
            case "var":
                consumeToken();
                varDeclaration();
                header1();
                break;
            case "const":
                consumeToken();
                constDeclaration();
                header2();
                break;
            case "procedure":
                consumeToken();
                procedureDefine();
                break;
            case "function":
                consumeToken();
                function();
                functionList();
                break;
            default:
                error("\"typedef\", \"struct\", \"var\", \"const\", \"procedure\" ou \"function\" - Token Ausente.", false);
                inicioErro();
        }
    }
    
    private void inicioErro() throws FimInesperadoDeArquivo{
        while(!firstTypes.contains(currentToken().getLexema())&&!currentToken().getLexema().equals("{")&&!currentToken().getId().equals("IDE")){
            error("\"int\", \"real\", \"boolean\", \"string\", IDE ou '{'", true);
        }
        Token t = lookahead();
        if(currentToken().getId().equals("IDE")){
            if(t.getId().equals("IDE")){
                typedefDeclaration();
                inicio();
            }else if(t.getLexema().equals("(")){
                procedureDefine();
            }else if(t.getLexema().equals("extends")||t.getLexema().equals("{")){
                structDeclaration();
                inicio();
            }else{
                error("PRE ausente", true);
                error("\"extends\", IDE ou '{'", true);
                inicioErro();
            }
        }else if(currentToken().getLexema().equals("{")){
            varDeclaration();
            header1();
        }else{
            if(t.getId().equals("IDE")){
                Token tp = lookaheadP();
                switch (tp.getLexema()) {
                    case ";":
                        typedefDeclaration();
                        inicio();
                        break;
                    case "(":
                        function();
                        functionList();
                        break;
                    default:
                        error("';', ou '('", true);
                        inicioErro();
                        break;
                }
            }else{
                error("PRE ausente", true);
                error("PRE ausente", true);
                error("\"int\", \"real\", \"boolean\", \"string\", IDE ou '{'", true);
                inicioErro();
            }
        }
    }
    
    

    private void header1() throws FimInesperadoDeArquivo {
        switch (currentToken().getLexema()) {
            case "typedef":
                consumeToken();
                typedefDeclaration();
                header1();
                break;
            case "struct":
                consumeToken();
                structDeclaration();
                header1();
                break;
            case "const":
                consumeToken();
                constDeclaration();
                header3();
                break;
            case "procedure":
                consumeToken();
                procedureDefine();
                break;
            case "function":
                consumeToken();
                function();
                functionList();
                break;
            default:
                error("\"typedef\", \"struct\", \"const\", \"procedure\" ou \"function\"", true);
        }
    }

    private void header2() throws FimInesperadoDeArquivo {
        switch (currentToken().getLexema()) {
            case "typedef":
                consumeToken();
                typedefDeclaration();
                header2();
                break;
            case "struct":
                consumeToken();
                structDeclaration();
                header2();
                break;
            case "var":
                consumeToken();
                varDeclaration();
                header3();
                break;
            case "procedure":
                consumeToken();
                procedureDefine();
                break;
            case "function":
                consumeToken();
                function();
                functionList();
                break;
            default:
                error("\"typedef\", \"struct\", \"var\", \"procedure\" ou \"function\"", true);
        }
    }

    private void header3() throws FimInesperadoDeArquivo {
        switch (currentToken().getLexema()) {
            case "typedef":
                consumeToken();
                typedefDeclaration();
                header3();
                break;
            case "struct":
                consumeToken();
                structDeclaration();
                header3();
                break;
            case "procedure":
                consumeToken();
                procedureDefine();
                break;
            case "function":
                consumeToken();
                function();
                functionList();
                break;
            default:
                error("\"typedef\", \"struct\", \"procedure\", \"function\"", true);
        }
    }

    private void procedureDefine() throws FimInesperadoDeArquivo {
        if (currentToken().getLexema().equals("start")) {
            consumeToken();
            startProcedure();
        } else {
            procedure();
            functionList();
        }
    }

    private void functionList() throws FimInesperadoDeArquivo {
        switch (currentToken().getLexema()) {
            case "procedure":
                consumeToken();
                procedureDefine();
                break;
            case "function":
                consumeToken();
                function();
                functionList();
                break;
            default:
                error("\"procedure\" ou \"function\"", true);
                break;
        }
    }
//********************************** Cabeçalhos de início do código **********************************      
//====================================================================================================

//============================================ Data Types ============================================
//**************************************************************************************************** 
    //Incompleto... 
    private void value() throws FimInesperadoDeArquivo {
        Token t = lookahead();
        switch (currentToken().getLexema()) {
            case "true":
            case "false":
                if (t.getId().equals("REL") || t.getId().equals("LOG")) {
                    operation();
                } else {
                    consumeToken();
                }
                break;
            default:
                operation();
                break;
        }
    }

    //verifica se é um tipo válido
    private void dataType() throws FimInesperadoDeArquivo {
        if (currentToken().getId().equals("IDE") || firstTypes.contains(currentToken().getLexema())) {
            consumeToken();
        } else {
            error("IDE", true);
        }
    }

    private void vectMatIndex() throws FimInesperadoDeArquivo {
        
        aritmeticOp();
    }

    private void aritmeticValue() throws FimInesperadoDeArquivo {
        switch (currentToken().getId()) {
            case "NRO":
            case "CAD":
                consumeToken();
                break;
            default:
                error("NRO ou CAD", true);
        }
    }

    private void variavel() throws FimInesperadoDeArquivo {
        String aheadToken = lookahead().getLexema();
        if (currentToken().getId().equals("IDE")) {
            consumeToken();
            if (aheadToken.equals(".") || aheadToken.equals("[")) {
                contElement();
            }
        } else {
            switch (currentToken().getLexema()) {
                case "global":
                case "local":
                    consumeToken();
                    if (currentToken().getLexema().equals(".")) {
                        consumeToken();
                        if (currentToken().getId().equals("IDE")) {
                            consumeToken();
                            if (aheadToken.equals(".") | aheadToken.equals("[")) {
                                contElement();
                            }
                        } else {
                            error("IDE", true);
                        }
                    } else {
                        error("'.'", true);
                    }
                    break;
                default:
                    error("IDE, \"global\" ou \"local\"", true);
            }
        }
    }

    private void contElement() throws FimInesperadoDeArquivo {
        switch (currentToken().getLexema()) {
            case ".":
                structE1();
                break;
            case "[":
                consumeToken();
                vectMatIndex();
                if (currentToken().getLexema().equals("]")) {
                    consumeToken();
                    if (currentToken().getLexema().equals(".")) {
                        structE1();
                    } else if (currentToken().getLexema().equals("[")) {
                        consumeToken();
                        vectMatIndex();
                        if (currentToken().getLexema().equals("]")) {
                            consumeToken();
                            if (lookahead().getLexema().equals(".")) {
                                structE1();
                            }
                        } else {
                            error("']'", true);
                        }
                    }
                } else {
                    error("]", true);
                }
                break;
            default:
                error("'.' ou '[' ", true);
        }
    }

    private void structE1() throws FimInesperadoDeArquivo {
        consumeToken();
        String ahead = lookahead().getLexema();
        if (currentToken().getId().equals("IDE")) {
            consumeToken();
            if (ahead.equals(".") || ahead.equals("[")) {
                contElement();
            }
        } else {
            error("IDE", true);
        }
    }
//******************************************** Data Types ********************************************   
//====================================================================================================

//======================================= Variable Declaration =======================================
//****************************************************************************************************
    private void varDeclaration() throws FimInesperadoDeArquivo {
        if (currentToken().getLexema().equals("{")) {
            consumeToken();
            primeiraVar();
        } else {
            error("'{'", true);
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
            error("IDE", true);
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
                error("',', '=', ';', '['", true);
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
                    error("']'", true);
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
                error("'[', '=', ',', ';'", true);
        }
    }

    private void initVetor() throws FimInesperadoDeArquivo {
        if (currentToken().getLexema().equals("[")) {
            consumeToken();
            value();
            proxVetor();
        } else {
            error("'['", true);
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
                error("',', ']'", true);
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
                error("'=', ',', ';'", true);
        }
    }

    private void initMatriz() throws FimInesperadoDeArquivo {
        if (currentToken().getLexema().equals("[")) {
            consumeToken();
            matrizValue();
        } else {
            error("'['", true);
        }
    }

    private void matrizValue() throws FimInesperadoDeArquivo {
        if (currentToken().getLexema().equals("[")) {
            consumeToken();
            value();
            proxMatriz();
        } else {
            error("'['", true);
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
                error("',', ']'", true);
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
                error("',', ']'", true);
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
                error("',', ';'", true);
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
        if (currentToken().getLexema().equals("{")) {
            consumeToken();
            continueConst();
            constId();
        } else {
            error("'{'", true);
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
            error("IDE", true);
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
                    error("']'", true);
                }
                break;
            default:
                error("'=', '['", true);
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
                    error("'['", true);
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
                        error("'='", true);
                    }
                } else {
                    error("']'", true);
                }
                break;
            default:
                error("'=' ou '['", true);
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
                error("',' ou ']'", true);
                break;
        }
    }

    private void initConstMatriz() throws FimInesperadoDeArquivo {
        if (currentToken().getLexema().equals("[")) {
            consumeToken();
            matrizConstValue();
        } else {
            error("'['", true);
        }
    }

    private void matrizConstValue() throws FimInesperadoDeArquivo {
        if (currentToken().getLexema().equals("[")) {
            consumeToken();
            value();
            proxConstMatriz();
        } else {
            error("'['", true);
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
                error("',', ']'", true);
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
                error("',' ou ']'", true);
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
                error("',' ou ';'", true);
                break;
        }
    }
//***************************************** Const Declaration ****************************************  
//====================================================================================================

//======================================= Function Declaration =======================================
//****************************************************************************************************
    private void function() throws FimInesperadoDeArquivo {
        dataType();
        if (currentToken().getId().equals("IDE")) {
            consumeToken();
            if (currentToken().getLexema().equals("(")) {
                continueFunction();
            } else {
                error("'('", true);
            }
        } else {
            error("IDE", true);
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
            error("IDE, ')', \"int\" \"real\" \"boolean\"ou \"string\"", true);
        }
    }

    private void blockFunction() throws FimInesperadoDeArquivo {
        if (currentToken().getLexema().equals("{")) {
            consumeToken();
            blockFuncContent();
            if (currentToken().getLexema().equals(";")) {
                consumeToken();
                if (currentToken().getLexema().equals("}")) {
                    consumeToken();
                } else {
                    error("'}'", true);
                }
            } else {
                error("';'", true);
            }
        } else {
            error("'{'", true);
        }
    }

    private void blockFuncContent() throws FimInesperadoDeArquivo {
        if (currentToken().getLexema().equals("var")) {
            varDeclaration();
            content1();
        } else if (currentToken().getLexema().equals("const")) {
            constDeclaration();
            content2();
        } else if (firstComando.contains(currentToken().getLexema())) {
            codigo();
            if (currentToken().getLexema().equals("return")) {
                consumeToken();
                value();
            } else {
                error("\"return\"", true);
            }
        } else {
            error("\"var\", \"const\", \"print\", \"read\", \"while\", \"const\","
                    + " \"typedef\", \"struct\" ou \"if\"", true);
        }

    }

    private void content1() throws FimInesperadoDeArquivo {
        if (currentToken().getLexema().equals("const")) {
            constDeclaration();
            content3();
        } else if (firstComando.contains(currentToken().getLexema())) {
            codigo();
            if (currentToken().getLexema().equals("return")) {
                consumeToken();
                value();
            } else {
                error("\"return\"", true);
            }
        } else {
            error("\"const\", \"print\", \"read\", \"while\", \"const\","
                    + " \"typedef\", \"struct\" ou \"if\"", true);
        }
    }

    private void content2() throws FimInesperadoDeArquivo {
        if (currentToken().getLexema().equals("var")) {
            varDeclaration();
            content3();
        } else if (firstComando.contains(currentToken().getLexema())) {
            codigo();
            if (currentToken().getLexema().equals("return")) {
                consumeToken();
                value();
            } else {
                error("\"return\"", true);
            }
        } else {
            error("\"var\", \"print\", \"read\", \"while\", \"const\","
                    + " \"typedef\", \"struct\" ou \"if\"", true);
        }
    }

    private void content3() throws FimInesperadoDeArquivo {
        if (firstComando.contains(currentToken().getLexema())) {
            codigo();
            if (currentToken().getLexema().equals("return")) {
                consumeToken();
                value();
            } else {
                error("\"return\"", true);
            }
        }
    }

    private void parameters() throws FimInesperadoDeArquivo {
        dataType();
        if (currentToken().getId().equals("IDE")) {
            paramLoop();
        } else {
            error("IDE", true);
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
                error("',' ou ')'", true);
                break;
        }
    }
//*************************************** Function Declaration ***************************************  
//====================================================================================================

//======================================== Struct Declaration ========================================
//**************************************************************************************************** 
    private void structDeclaration() throws FimInesperadoDeArquivo {
        if (currentToken().getId().equals("IDE")) {
            structVars();
        } else {
            error("IDE", true);
        }
    }

    private void structVars() throws FimInesperadoDeArquivo {
        consumeToken();
        switch (currentToken().getLexema()) {
            case "{":
                consumeToken();
                blockVarStruct();
                break;
            case "extends":
                consumeToken();
                if (currentToken().getId().equals("IDE")) {
                    consumeToken();
                    if (currentToken().getLexema().equals("{")) {
                        consumeToken();
                        blockVarStruct();
                    }
                    else{
                        error("'{'", true);
                    }
                } else {
                    error("IDE", true);
                }   break;
            default:
                error("'{' ou \"extends\"", true);
                break;
        }
    }

    private void blockVarStruct() throws FimInesperadoDeArquivo {
        if (currentToken().getLexema().equals("var")) {
            consumeToken();
            if (currentToken().getLexema().equals("{")) {
                consumeToken();
                firstStructVar();
            } else {
                error("'{'", true);
            }
        } else {
            error("\"var\"", true);
        }
    }

    private void firstStructVar() throws FimInesperadoDeArquivo {
        dataType();
        structVarId();
    }

    private void structVarId() throws FimInesperadoDeArquivo {
        if (currentToken().getId().equals("IDE")) {
            consumeToken();
            structVarExp();
        } else {
            error("IDE", true);
        }
    }

    private void structVarExp() throws FimInesperadoDeArquivo {
        switch (currentToken().getLexema()) {
            case ",":
                consumeToken();
                structVarId();
                break;
            case ";":
                consumeToken();
                proxStructVar();
                break;
            case "[":
                consumeToken();
                vectMatIndex();
                if (currentToken().getId().equals("]")) {
                    consumeToken();
                    structMatriz();
                } else {
                    error("]", true);
                }
                break;
            default:
                error("',', ';' ou '['", true);
                break;
        }
    }

    private void proxStructVar() throws FimInesperadoDeArquivo {
        if (currentToken().getLexema().equals("}")) {
            consumeToken();
            if (currentToken().getLexema().equals("}")) {
                consumeToken();
            } else {
                error("'}'", true);
            }
        } else {
            dataType();
            structVarId();
        }
    }

    private void structMatriz() throws FimInesperadoDeArquivo {
        switch (currentToken().getLexema()) {
            case "[":
                consumeToken();
                vectMatIndex();
                if (currentToken().getLexema().equals("]")) {
                    consumeToken();
                    contStructMatriz();
                } else {
                    error("']'", true);
                }
                break;
            case ",":
                consumeToken();
                structVarId();
                break;
            case ";":
                consumeToken();
                proxStructVar();
                break;
            default:
                error("'[', ',' ou ';'", true);
                break;
        }
    }

    private void contStructMatriz() throws FimInesperadoDeArquivo {
        switch (currentToken().getLexema()) {
            case ",":
                consumeToken();
                structVarId();
                break;
            case ";":
                consumeToken();
                proxStructVar();
                break;
            default:
                error("',' ou ';'", true);
                break;
        }
    }

    //**************************************** Struct Declaration ****************************************  
    //====================================================================================================
    //====================================== Procedure Declaration =======================================
    //****************************************************************************************************     
    private void startProcedure() throws FimInesperadoDeArquivo {
        if (currentToken().getLexema().equals("(")) {
            consumeToken();
            if (currentToken().getLexema().equals(")")) {
                consumeToken();
                if (currentToken().getLexema().equals("{")) {
                    consumeToken();
                    procedureContent();
                } else {
                    error("'{'", true);
                }
            } else {
                error("')'", true);
            }
        } else {
            error("'('", true);
        }

    }

    private void procedureContent() throws FimInesperadoDeArquivo {
        switch (currentToken().getLexema()) {
            case "var":
                consumeToken();
                varDeclaration();
                procedureContent2();
                break;
            case "const":
                consumeToken();
                constDeclaration();
                procedureContent3();
                break;
            default:
                error("\"var\" ou \"const\"", true);
                break;
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
                error("'}'", true);
            }
        } else {
            error("\"const\", \"print\", \"read\", \"while\", \"const\","
                    + " \"typedef\", \"struct\" ou \"if\"", true);
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
                error("'}'", true);
            }
        } else {
            error("\"var\", \"print\", \"read\", \"while\", \"const\","
                    + " \"typedef\", \"struct\" ou \"if\"", true);
        }
    }

    private void procedureContent4() throws FimInesperadoDeArquivo {
        if (firstComando.contains(currentToken().getLexema())) {
            codigo();
            if (currentToken().getLexema().equals("}")) {
                consumeToken();
            } else {
                error("'}", true);
            }
        } else {
            error("\"print\", \"read\", \"while\", \"const\","
                    + " \"typedef\", \"struct\" ou \"if\"", true);
        }
    }

    private void procedure() throws FimInesperadoDeArquivo {
        if (currentToken().getId().equals("IDE")) {
            consumeToken();
            if (currentToken().getLexema().equals("(")) {
                procedureParams();
                if (currentToken().getLexema().equals("{")) {
                    consumeToken();
                    procedureContent();
                } else {
                    error("'{'", true);
                }
            } else {
                error("'('", true);
            }
        } else {
            error("IDE", true);
        }
    }

    private void procedureParams() throws FimInesperadoDeArquivo {
        consumeToken();
        if (currentToken().getLexema().equals(")")) {
            consumeToken();
        } else if (currentToken().getId().equals("IDE") || firstTypes.contains(currentToken().getLexema())) {
            parameters();
        } else {
            error("')' ou IDE", true);
        }
    }
//************************************** Procedure Declaration ***************************************  
//====================================================================================================

//====================================== Codigo ======================================================
//****************************************************************************************************  
    private void codigo() throws FimInesperadoDeArquivo {
        if (firstComando.contains(currentToken().getLexema()) || currentToken().getId().equals("IDE") || currentToken().getLexema().equals("++") || currentToken().getLexema().equals("--")) { //IDE para increment e decrement, functioncall e atribuição
            comando();
            codigo();
        }
    }

    private void comando() throws FimInesperadoDeArquivo {
        if (currentToken().getLexema().equals("print")) {
            print();
        } else if (currentToken().getLexema().equals("read")) {
            read();
        } else if (currentToken().getLexema().equals("typedef")) {
            consumeToken();
            typedefDeclaration();
        } else if (currentToken().getLexema().equals("struct")) {
            consumeToken();
            structDeclaration();
        } else if (currentToken().getLexema().equals("if")) {
            conditional();
        } else if (currentToken().getLexema().equals("while")) {
            whileLoop();
        } else if (currentToken().getId().equals("IDE")) {
            Token t = lookahead();
            if (t.getLexema().equals("(")) {
                functionCall();
                if(currentToken().getLexema().equals(";"))
                    consumeToken();
                else
                    error("','", true);
            } else { // casos de incrementop, decrementop e atribuição (first é variável nos 3 casos)
                variavel();
                //atribuição
                switch (currentToken().getLexema()) {
                    case "=":
                        consumeToken();
                        value();
                        if (currentToken().getLexema().equals(";")) {
                            consumeToken();
                        } else {
                            error("';'", true);
                        }
                        break;
                    case "++":
                    case "--":
                        consumeToken();
                        if (currentToken().getLexema().equals(";")) {
                            consumeToken();
                        } else {
                            error("';'", true);
                        }
                        break;
                    default:
                        error("'=', \"--\" ou \"++\"", true);
                        break;
                }

            }
        } //caso de pré incremento/decremento
        else if (currentToken().getLexema().equals("++") || currentToken().getLexema().equals("--")) {
            consumeToken();
            variavel();
            if (currentToken().getLexema().equals(";")) {
                consumeToken();
            } else {
                error("';'", true);
            }
        } else {
            error("\"++\" ou \"--\"", true);
        }
    }

    private void print() throws FimInesperadoDeArquivo {
        consumeToken();
        if (currentToken().getLexema().equals("(")) {
            consumeToken();
            printableList();
        } else {
            error("'('", true);
        }
    }

    private void printableList() throws FimInesperadoDeArquivo {
        value();
        nextPrintValue();
    }

    private void nextPrintValue() throws FimInesperadoDeArquivo {
        switch (currentToken().getLexema()) {
            case ",":
                consumeToken();
                printableList();
                break;
            case ")":
                consumeToken();
                if (currentToken().getLexema().equals(";")) {
                    consumeToken();
                } else {
                    error("';'", true);
                }
                break;
            default:
                error("',' ou ')'", true);
                break;
        }
    }

    private void read() throws FimInesperadoDeArquivo {
        consumeToken();
        if (currentToken().getLexema().equals("(")) {
            consumeToken();
            readParams();
        } else {
            error("'('", true);
        }
    }

    private void readParams() throws FimInesperadoDeArquivo {
        variavel();
        readLoop();
    }

    private void readLoop() throws FimInesperadoDeArquivo {
        switch (currentToken().getLexema()) {
            case ",":
                consumeToken();
                readParams();
                break;
            case ")":
                consumeToken();
                if (currentToken().getLexema().equals(";")) {
                    consumeToken();

                } else {
                    error("';'", true);
                }
                break;
            default:
                error("',' ou ')'", true);
                break;
        }
    }

    private void conditional() throws FimInesperadoDeArquivo {
        consumeToken();
        if (currentToken().getLexema().equals("(")) {
            consumeToken();
            boolOperation();
            if (currentToken().getLexema().equals(")")) {
                consumeToken();
                if (currentToken().getLexema().equals("then")) {
                    consumeToken();
                    if (currentToken().getLexema().equals("{")) {
                        consumeToken();
                        codigo();
                        if (currentToken().getLexema().equals("}")) {
                            consumeToken();
                            elsePart();
                        } else {
                            error("'}'", true);
                        }
                    } else {
                        error("'{'", true);
                    }
                } else {
                    error("\"then\"", true);
                }
            } else {
                error("')'", true);
            }
        } else {
            error("'('", true);
        }

    }

    private void elsePart() throws FimInesperadoDeArquivo {
        if (currentToken().getLexema().equals("else")) {
            consumeToken();
            if (currentToken().getLexema().equals("{")) {
                consumeToken();
                codigo();
                if (currentToken().getLexema().equals("}")) {
                    consumeToken();
                } else {
                    error("'}", true);
                }
            } else {
                error("\"else\"", true);
            }
        }
    }

    private void whileLoop() throws FimInesperadoDeArquivo {
        consumeToken();
        if (currentToken().getLexema().equals("(")) {
            consumeToken();
            boolOperation();
            if (currentToken().getLexema().equals(")")) {
                consumeToken();
                if (currentToken().getLexema().equals("{")) {
                    consumeToken();
                    codigo();
                    if (currentToken().getLexema().equals("}")) {
                        consumeToken();
                    } else {
                        error("'}'", true);
                    }
                } else {
                    error("'{'", true);
                }
            } else {
                error("')'", true);
            }
        } else {
            error("'('", true);
        }
    }

//************************************** Codigo ****************************************************** 
//====================================================================================================
//====================================== Procedure Declaration =======================================
//**************************************************************************************************** 
    private void typedefDeclaration() throws FimInesperadoDeArquivo {
        if (currentToken().getLexema().equals("struct")) {
            consumeToken();
            if (currentToken().getId().equals("IDE")) {
                consumeToken();
                if (currentToken().getId().equals("IDE")) {
                    consumeToken();
                    if (currentToken().getLexema().equals(";")) {
                        consumeToken();
                    } else {
                        error("';'", true);
                    }
                } else {
                    error("IDE", true);
                }
            } else {
                error("IDE", true);
            }
        } else {
            dataType();
            if (currentToken().getId().equals("IDE")) {
                consumeToken();
                if (currentToken().getLexema().equals(";")) {
                    consumeToken();
                } else {
                    error("';'", true);
                }
            } else {
                error("IDE ou \"struc\"", true);
            }
        }
    }
//*************************************** Typedef Declaration ****************************************  
//====================================================================================================

//=========================================== Operations =============================================  
//****************************************************************************************************    
    //Produções que podem assumir valores aritméticos
    private void opNegate() throws FimInesperadoDeArquivo {
        if (currentToken().getId().equals("CAD") || currentToken().getId().equals("NRO")) {
            consumeToken();
            return;
        }
        switch (currentToken().getLexema()) {
            case "-":
                consumeToken();
                if (currentToken().getId().equals("IDE")) {
                    if (lookahead().getLexema().equals("(")) {
                        functionCall();
                    } else {
                        variavel();
                    }
                }else {
                    aritmeticValue();
                }
                break;
            case "--":
            case "++":
                consumeToken();
                variavel();
            default:
                if (currentToken().getId().equals("IDE")&&lookahead().getLexema().equals("(")) {
                    functionCall();
                }else {
                    variavel();
                    if (currentToken().getLexema().equals("++") || currentToken().getLexema().equals("--")) {
                        consumeToken();
                    }
                }
        }
    }

    //Operação aritmética
    private void aritmeticOp() throws FimInesperadoDeArquivo {
        if (currentToken().getLexema().equals("-") && lookahead().getLexema().equals("(")) {
            consumeToken();
        }
        if (currentToken().getLexema().equals("(")) {
            consumeToken();
            aritmeticOp();
            if (currentToken().getLexema().equals(")")) {
                consumeToken();
                if (currentToken().getLexema().equals("*") || currentToken().getLexema().equals("/")) {
                    consumeToken();
                    aritmeticOp();
                }
            } else {
                error("'*' ou '/'", true);
            }
        } else {
            opNegate();
            if (currentToken().getLexema().equals("*") || currentToken().getLexema().equals("/")) {
                consumeToken();
                aritmeticOp();
            }
        }
        if (currentToken().getLexema().equals("+") || currentToken().getLexema().equals("-")) {
            consumeToken();
            aritmeticOp();
        }
    }

    //Operação relacional
    private void logicOrRelacionalOp() throws FimInesperadoDeArquivo {
        switch (currentToken().getId()) {
            case "REL":
            case "LOG":
                consumeToken();
                contRelLogic();
                break;
            default:
                error("REL ou LOG", true);
        }
    }

    //Verifica os possíveis valores de uma operação relacional ou lógica.
    private void contRelLogic() throws FimInesperadoDeArquivo {
        switch (currentToken().getLexema()) {
            case "true":
            case "false":
                Token t = (Token) lookahead();
                if (t.getId().equals("REL") || t.getId().equals("LOG")) {
                    consumeToken();
                    consumeToken();
                    operation();
                } else {
                    consumeToken();
                }
                break;
            default:
                operation();
                break;
        }
    }

    //Operação de negação.
    private void negBoolValue() throws FimInesperadoDeArquivo {
        if (currentToken().getLexema().equals("!")) {
            consumeToken();
            variavel();
        } else {
            error("'!'", true);

        }
    }

    //Produções de valor booleano que não possuem operações aritméticas no first.
    private void boolOnlyOp() throws FimInesperadoDeArquivo {
        switch (currentToken().getLexema()) {
            case "true":
            case "false":
                consumeToken();
                logicOrRelacionalOp();
                break;
            case "!":
                negBoolValue();
                break;
            case "(":
                consumeToken();
                if (currentToken().getId().equals("NRO") || currentToken().getId().equals("CAD")
                        || currentToken().getId().equals("IDE") || currentToken().getLexema().equals("-")) {
                    aritmeticOp();
                    logicOrRelacionalOp();
                    if (currentToken().getLexema().equals(")")) {
                        consumeToken();
                        logicOrRelacionalOp();
                    } else {
                        logicOrRelacionalOp();
                        if (currentToken().getLexema().equals(")")) {
                            consumeToken();
                            if (currentToken().getId().equals("REL") || currentToken().getId().equals("LOG")) {
                                logicOrRelacionalOp();
                            }
                        } else {
                            error("')'", true);
                        }
                    }
                } else {
                    boolOnlyOp();
                    if (currentToken().getLexema().equals(")")) {
                        consumeToken();
                        if (currentToken().getId().equals("REL") || currentToken().getId().equals("LOG")) {
                            consumeToken();
                            logicOrRelacionalOp();
                        }
                    } else {
                        error("')'", true);
                    }
                }
                break;
            default:
                error("'!', '(', \"true\" ou \"false\"", true);
        }
    }

    //Operações que podem retornar valores binários.
    private void boolOperation() throws FimInesperadoDeArquivo {
        if (currentToken().getId().equals("NRO") || currentToken().getId().equals("CAD")
                || currentToken().getId().equals("IDE") || currentToken().getLexema().equals("-")) {
            aritmeticOp();
            logicOrRelacionalOp();
        } else {
            boolOnlyOp();
        }
    }

    private void operation() throws FimInesperadoDeArquivo {
        if (currentToken().getId().equals("NRO") || currentToken().getId().equals("CAD")
                || currentToken().getId().equals("IDE") || currentToken().getLexema().equals("-")) {
            aritmeticOp();
            if (currentToken().getId().equals("REL") || currentToken().getId().equals("LOG")) {
                logicOrRelacionalOp();
            }
        } else {
            switch (currentToken().getLexema()) {
                case "true":
                case "false":
                    consumeToken();
                    logicOrRelacionalOp();
                    break;
                case "!":
                    negBoolValue();
                    break;
                case "(":
                    consumeToken();
                    if (currentToken().getId().equals("NRO") || currentToken().getId().equals("CAD")
                            || currentToken().getId().equals("IDE") || currentToken().getLexema().equals("-")) {
                        aritmeticOp();
                        if (currentToken().getId().equals("REL") || currentToken().getId().equals("LOG")) {
                            logicOrRelacionalOp();
                        }
                    } else {
                        boolOnlyOp();
                        if (currentToken().getLexema().equals(")")) {
                            consumeToken();
                            if (currentToken().getId().equals("REL") || currentToken().getId().equals("LOG")) {
                                logicOrRelacionalOp();
                            }
                        } else {
                            error("')'", true);
                        }
                    }
                    if (currentToken().getLexema().equals(")")) {
                        consumeToken();
                        if (currentToken().getId().equals("REL") || currentToken().getId().equals("LOG")) {
                            logicOrRelacionalOp();
                        }
                    } else {
                        error("')'", true);
                    }
                    break;
                default:
                    error("'!', '(', \"true\" ou \"false\"", true);
            }
        }
    }
//******************************************* Operations *********************************************  
//====================================================================================================

//========================================== Function Call ===========================================  
//****************************************************************************************************
    private void functionCall() throws FimInesperadoDeArquivo {
        consumeToken();
        if (currentToken().getLexema().equals("(")) {
            consumeToken();
            if (currentToken().getLexema().equals(")")) {
                consumeToken();
            } else {
                value();
                fCallParams();
            }
        } else {
            error("'('", true);
        }
    }

    private void fCallParams() throws FimInesperadoDeArquivo {
        switch (currentToken().getLexema()) {
            case ",":
                consumeToken();
                value();
                fCallParams();
                break;
            case ")":
                consumeToken();
                break;
            default:
                error("',' ou ')'", true);
                break;
        }
    }
//****************************************** Function Call *******************************************  
//====================================================================================================

}
