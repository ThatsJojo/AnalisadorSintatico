package controller;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import javafx.util.Pair;
import model.Arquivo;
import model.Simbolo;
import model.TabelaSimbolos;
import model.Token;
import util.FimInesperadoDeArquivo;
import util.identificadorJaUtilizado;
import util.identificadorNaoEncontrado;

public class AnalisadorSintatico {

    private Token privateCurrentToken;
    private int erros;
    private ArrayList tokens;
    private int countToken = 0;
    private String analiseret = "";
    private static final ArrayList<TabelaSimbolos> escopos = new ArrayList();

    private static final HashSet<String> firstTypes = new HashSet();
    private static final HashSet<String> firstComando = new HashSet();
    private static final HashSet<String> firstInicio = new HashSet();
    private static TabelaSimbolos escopoAtual;
    private static TabelaSimbolos global;

    public AnalisadorSintatico() {
        //Conjunto first do método types
        firstTypes.add("int");
        firstTypes.add("real");
        firstTypes.add("boolean");
        firstTypes.add("string");

        //Conjunto first do método comando //faltam algumas coisas
        firstComando.add("print");
        firstComando.add("read");
        firstComando.add("while");
        firstComando.add("const");
        firstComando.add("typedef");
        firstComando.add("struct");
        firstComando.add("if");
        firstComando.add("++");
        firstComando.add("--");

        //conjunto first do método comando
        firstInicio.add("typedef");
        firstInicio.add("const");
        firstInicio.add("var");
        firstInicio.add("function");
        firstInicio.add("procedure");
        firstInicio.add("struct");

    }

    private void erroSemantico(String texto) {
        System.out.println(texto);
    }

    public String analise(Arquivo arq, ArrayList tokens) {
        escopos.clear();
        escopoAtual = new TabelaSimbolos(null);
        global = escopoAtual;
        escopos.add(escopoAtual);
        analiseret = "";
        this.erros = 0;
        this.tokens = tokens;
        countToken = 0;
        privateCurrentToken = null;
        //percorrer toda a lista de tokens até o ultimo elemento
        try {
            consumeToken();
            inicio();
        } catch (FimInesperadoDeArquivo e) {
            analiseret += "" + (privateCurrentToken == null ? "" + privateCurrentToken.getId() : "0000")
                    + " ERRO SINTÁTICO. EOF";
        } catch (identificadorNaoEncontrado ex) {
            System.out.println("Erro interno");
        }

        try {
            while (hasToken()) {
                error("ESPERADO: EOF", true);
            }
        } catch (FimInesperadoDeArquivo ex) {
        }

        System.out.println("Análise Sintática realizada " + (erros == 0 ? "com" : "sem") + " sucesso (" + String.format("%03d", erros)
                + " erros sintáticos encontrados) no arquivo " + arq.getNome() + ".");
        return analiseret;
    }

    private boolean consumeToken() throws FimInesperadoDeArquivo {
        return consumeToken(false);
    }

    private boolean consumeToken(Boolean tokenErro) throws FimInesperadoDeArquivo {
        if (countToken > tokens.size()) {
            throw new FimInesperadoDeArquivo();
        } else if (countToken == tokens.size()) {
            if (!tokenErro) {
                analiseret += "" + privateCurrentToken.getLinha() + " "
                        + privateCurrentToken.getId() + " " + privateCurrentToken.getLexema() + "\n";
            }
            this.privateCurrentToken = new Token("EOF", null, privateCurrentToken.getLinha());
            countToken++;
            return false;
        } else {
            if (countToken != 0 && !tokenErro) {
                analiseret += "" + privateCurrentToken.getLinha() + " "
                        + privateCurrentToken.getId() + " " + privateCurrentToken.getLexema() + "\n";
            }
            this.privateCurrentToken = (Token) tokens.get(countToken++);

            return true;
        }

    }

    private boolean hasToken() {
        return countToken < tokens.size() + 1;
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

    private Token lookaheadP() throws FimInesperadoDeArquivo {
        if (countToken >= tokens.size()) {
            erros++;
            throw new FimInesperadoDeArquivo();
        }
        return (Token) tokens.get(countToken + 1);
    }

    private void error(String esperado, boolean consumir) throws FimInesperadoDeArquivo {
        analiseret += "" + privateCurrentToken.getLinha() + " " + privateCurrentToken.getId() + " " + privateCurrentToken.getLexema() + " ERRO SINTÁTICO. "
                + esperado + ".  Encontrado: " + currentToken().getLexema() + "\n";

        this.erros++;

        if (consumir) {
            consumeToken(true);
        }
    }

//================================== Cabeçalhos de início do código ==================================
//****************************************************************************************************     
    private void inicio() throws FimInesperadoDeArquivo, identificadorNaoEncontrado {
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
                error(" (Token Ausente):  \"typedef\", \"struct\", \"var\", \"const\", \"procedure\" ou \"function\"", false);
                inicioErro();
        }
    }

    private void inicioErro() throws FimInesperadoDeArquivo, identificadorNaoEncontrado {
        while (!firstTypes.contains(currentToken().getLexema()) && !currentToken().getLexema().equals("{") && !currentToken().getId().equals("IDE")) {
            if (firstInicio.contains(currentToken().getLexema())) {
                inicio();
                return;
            }
            error(" (Token Ausente):  \"int\", \"real\", \"boolean\", \"string\", IDE ou '{'", true);
        }
        Token t = lookahead();
        if (currentToken().getId().equals("IDE")) {
            if (t.getId().equals("IDE")) {
                error(" assumiu token ausente = \"typedef\"", false);
                typedefDeclaration();
                inicio();
            } else if (t.getLexema().equals("(")) {
                error(" assumiu token ausente = \"procedure\"", false);
                procedureDefine();
            } else if (t.getLexema().equals("extends") || t.getLexema().equals("{")) {
                error(" assumiu token ausente = \"struct\"", false);
                structDeclaration();
                inicio();
            } else {
                error("tentativa falha de assumir produção de \"struct\", \"typedef\" ou \"procedure\"", true);
                error("ESPERAVA: \"extends\", '(' IDE ou '{'", true);
                inicio();
            }
        } else if (currentToken().getLexema().equals("{")) {
            error(" assumiu token ausente = \"var\"", false);
            varDeclaration();
            header1();
        } else {
            if (t.getId().equals("IDE")) {
                Token tp = lookaheadP();
                switch (tp.getLexema()) {
                    case ";":
                        error(" assumiu token ausente = \"typedef\"", false);
                        typedefDeclaration();
                        inicio();
                        break;
                    case "(":
                        error(" assumiu token ausente = \"function\"", false);
                        function();
                        functionList();
                        break;
                    default:
                        error("tentativa falha de assumir produção de \"typedef\" ou \"function\".", true);
                        error("tentativa falha de assumir produção de \"typedef\" ou \"function\".", true);
                        error("ESPERAVA: ';', ou '('.", true);
                        inicio();
                        break;
                }
            } else {
                error("ESPERAVA: PRE", true);
                error("ESPERAVA: PRE", true);
                error("ESPERAVA: \"int\", \"real\", \"boolean\", \"string\", IDE ou '{'", true);
                inicioErro();
            }
        }
    }

    //recebue var no início
    private void header1() throws FimInesperadoDeArquivo, identificadorNaoEncontrado {
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
                header1Erro();
        }
    }

    //recebeu var no início
    private void header1Erro() throws FimInesperadoDeArquivo, identificadorNaoEncontrado {
        if (currentToken().getLexema().equals("var")) {
            error(" campo \"var{}\" já declarado", false);
            while (currentToken().getLexema().equals("var") || (!currentToken().getLexema().equals("}") && !firstInicio.contains(currentToken().getLexema()))) {
                error("ESPERAVA: '}' \"typedef\", \"const\", \"function\", \"procedure\" ou \"struct\"", true);
            }
            if (currentToken().getLexema().equals("}")) {
                error("Token de sincronização", true);
            } else {
                error("Token de sincronização", false);
            }
            header1();
            return;
        }

        error(" (Token Ausente):  \"typedef\", \"struct\", \"const\", \"procedure\" ou \"function\"", false);

        while (currentToken().getLexema().equals("var") || (!firstTypes.contains(currentToken().getLexema())
                && !currentToken().getLexema().equals("{") && !currentToken().getId().equals("IDE") && !firstInicio.contains(currentToken().getLexema()))) {
            error("ESPERAVA: \"int\", \"real\", \"boolean\", \"string\", IDE ou '{'", true);
        }
        if (firstInicio.contains(currentToken().getLexema())) {
            error("Token de sincronização", false);
            header1();
            return;
        }

        Token t = lookahead();
        if (firstInicio.contains(t.getLexema())) {
            error("Token de sincronização no lookahead", true);
            header1();
            return;
        }

        if (currentToken().getId().equals("IDE")) {
            if (t.getId().equals("IDE")) {
                error(" assumiu token ausente = \"typedef\"", false);
                typedefDeclaration();
                header1();
            } else if (t.getLexema().equals("(")) {
                error(" assumiu token ausente = \"procedure\"", false);
                procedureDefine();
            } else if (t.getLexema().equals("extends") || t.getLexema().equals("{")) {
                error(" assumiu token ausente = \"struct\"", false);
                structDeclaration();
                header1();
            } else {
                error("tentativa falha de assumir produção de \"struct\", \"typedef\" ou \"procedure\"", true);
                error("ESPERAVA: \"extends\", '(' IDE ou '{'", true);
                header1();
            }
        } else if (currentToken().getLexema().equals("{")) {
            error(" assumiu token ausente = \"const\"", false);
            constDeclaration();
            header3();
        } else {
            if (t.getId().equals("IDE")) {
                Token tp = lookaheadP();
                if (firstInicio.contains(tp.getLexema())) {
                    error("Token de sincronização no lookahead", true);
                    error("Token de sincronização no lookahead", true);
                    header1();
                    return;
                }
                switch (tp.getLexema()) {
                    case ";":
                        error(" assumiu token ausente = \"typedef\"", false);
                        typedefDeclaration();
                        header1();
                        break;
                    case "(":
                        error(" assumiu token ausente = \"function\"", false);
                        function();
                        functionList();
                        break;
                    default:
                        error("tentativa falha de assumir produção de \"typedef\" ou \"function\".", true);
                        error("tentativa falha de assumir produção de \"typedef\" ou \"function\".", true);
                        error("ESPERAVA: ';', ou '('.", true);
                        inicio();
                        break;
                }
            } else {
                error("tentativa falha de assumir produção de \"typedef\" ou \"function\".", true);
                error("ESPERAVA: IDE", true);
                header1();
            }
        }
    }

    //Já recebeu o const
    private void header2() throws FimInesperadoDeArquivo, identificadorNaoEncontrado {
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
                header2Erro();
        }
    }

    //Já recebeu o const
    private void header2Erro() throws FimInesperadoDeArquivo, identificadorNaoEncontrado {
        if (currentToken().getLexema().equals("const")) {
            error(" campo \"const{}\" já declarado", false);
            while (currentToken().getLexema().equals("const") || (!currentToken().getLexema().equals("}") && !firstInicio.contains(currentToken().getLexema()))) {
                error("ESPERAVA: '}' \"typedef\", \"var\", \"function\", \"procedure\" ou \"struct\"", true);
            }
            if (currentToken().getLexema().equals("}")) {
                error("Token de sincronização", true);
            } else {
                error("Token de sincronização", false);
            }
            header2();
            return;
        }

        error(" (Token Ausente):  \"typedef\", \"struct\", \"var\", \"procedure\" ou \"function\"", false);

        while (currentToken().getLexema().equals("const") || (!firstTypes.contains(currentToken().getLexema()) && !currentToken().getId().equals("IDE") && !firstInicio.contains(currentToken().getLexema()))) {
            error("ESPERAVA: \"int\", \"real\", \"boolean\", \"string\", IDE, '{', ou \"var\"", true);
        }
        if (firstInicio.contains(currentToken().getLexema())) {
            error("Token de sincronização", false);
            header2();
            return;
        }

        Token t = lookahead();
        if (firstInicio.contains(t.getLexema())) {
            error("Token de sincronização no lookahead", true);
            header2();
            return;
        }

        if (currentToken().getId().equals("IDE")) {
            if (t.getId().equals("IDE")) {
                error(" assumiu token ausente = \"typedef\"", false);
                typedefDeclaration();
                header2();
            } else if (t.getLexema().equals("(")) {
                error(" assumiu token ausente = \"procedure\"", false);
                procedureDefine();
            } else if (t.getLexema().equals("extends") || t.getLexema().equals("{")) {
                error(" assumiu token ausente = \"struct\"", false);
                structDeclaration();
                header2();
            } else {
                error("tentativa falha de assumir produção de \"struct\", \"typedef\" ou \"procedure\"", true);
                error("ESPERAVA: \"extends\", '(' IDE ou '{'", true);
                header2();
            }
        } else if (currentToken().getLexema().equals("{")) {
            error(" assumiu token ausente = \"var\"", false);
            varDeclaration();
            header3();
        } else {
            if (t.getId().equals("IDE")) {
                Token tp = lookaheadP();
                if (firstInicio.contains(tp.getLexema())) {
                    error("Token de sincronização no lookahead", true);
                    error("Token de sincronização no lookahead", true);
                    header2();
                    return;
                }
                switch (tp.getLexema()) {
                    case ";":
                        error(" assumiu token ausente = \"typedef\"", false);
                        typedefDeclaration();
                        header2();
                        break;
                    case "(":
                        error(" assumiu token ausente = \"function\"", false);
                        function();
                        functionList();
                        break;
                    default:
                        error("tentativa falha de assumir produção de \"typedef\" ou \"function\".", true);
                        error("tentativa falha de assumir produção de \"typedef\" ou \"function\".", true);
                        error("ESPERAVA: ';', ou '('.", true);
                        inicio();
                        break;
                }
            } else {
                error("tentativa falha de assumir produção de \"typedef\" ou \"function\".", true);
                error("ESPERAVA: IDE", true);
                header1();
            }
        }
    }

    //Recebeu var e const;
    private void header3() throws FimInesperadoDeArquivo, identificadorNaoEncontrado {
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
                header3Erro();
        }
    }

    //Já recebeu o const
    private void header3Erro() throws FimInesperadoDeArquivo, identificadorNaoEncontrado {
        if (currentToken().getLexema().equals("const") || currentToken().getLexema().equals("var")) {
            error(" campos \"var{}\" e \"const{}\" já declarados", false);
            while (currentToken().getLexema().equals("const") || currentToken().getLexema().equals("var") || (!currentToken().getLexema().equals("}") && !firstInicio.contains(currentToken().getLexema()))) {
                error("ESPERAVA: '}' \"typedef\", \"function\", \"procedure\" ou \"struct\"", true);
            }
            if (currentToken().getLexema().equals("}")) {
                error("Token de sincronização", true);
            } else {
                error("Token de sincronização", false);
            }
            header3();
            return;
        }

        error(" (Token Ausente):  \"typedef\", \"struct\", \"var\", \"procedure\" ou \"function\"", false);

        while (currentToken().getLexema().equals("const") || currentToken().getLexema().equals("var") || (!firstTypes.contains(currentToken().getLexema()) && !currentToken().getId().equals("IDE") && !firstInicio.contains(currentToken().getLexema()))) {
            error("ESPERAVA: \"int\", \"real\", \"boolean\", \"string\" ou IDE ", true);
        }
        if (firstInicio.contains(currentToken().getLexema())) {
            error("Token de sincronização", false);
            header3();
            return;
        }

        Token t = lookahead();
        if (firstInicio.contains(t.getLexema())) {
            error("Token de sincronização no lookahead", true);
            header3();
            return;
        }

        if (currentToken().getId().equals("IDE")) {
            if (t.getId().equals("IDE")) {
                error(" assumiu token ausente = \"typedef\"", false);
                typedefDeclaration();
                header3();
            } else if (t.getLexema().equals("(")) {
                error(" assumiu token ausente = \"procedure\"", false);
                procedureDefine();
            } else if (t.getLexema().equals("extends") || t.getLexema().equals("{")) {
                error(" assumiu token ausente = \"struct\"", false);
                structDeclaration();
                header3();
            } else {
                error("tentativa falha de assumir produção de \"struct\", \"typedef\" ou \"procedure\"", true);
                error("ESPERAVA: \"extends\", '(' IDE ou '{'", true);
                header3();
            }
        } else {
            if (t.getId().equals("IDE")) {
                Token tp = lookaheadP();
                if (firstInicio.contains(tp.getLexema())) {
                    error("Token de sincronização no lookahead", true);
                    error("Token de sincronização no lookahead", true);
                    header3();
                    return;
                }
                switch (tp.getLexema()) {
                    case ";":
                        error(" assumiu token ausente = \"typedef\"", false);
                        typedefDeclaration();
                        header3();
                        break;
                    case "(":
                        error(" assumiu token ausente = \"function\"", false);
                        function();
                        functionList();
                        break;
                    default:
                        error("tentativa falha de assumir produção de \"typedef\" ou \"function\".", true);
                        error("tentativa falha de assumir produção de \"typedef\" ou \"function\".", true);
                        error("ESPERAVA: ';', ou '('.", true);
                        inicio();
                        break;
                }
            } else {
                error("tentativa falha de assumir produção de \"typedef\" ou \"function\".", true);
                error("ESPERAVA: IDE", true);
                header3();
            }
        }
    }

    private void procedureDefine() throws FimInesperadoDeArquivo, identificadorNaoEncontrado {
        if (currentToken().getLexema().equals("start")) {
            consumeToken();
            startProcedure();
        } else {
            procedure();
            functionList();
        }
    }

    private void functionList() throws FimInesperadoDeArquivo, identificadorNaoEncontrado {
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
                if (currentToken().getId().equals("IDE")) {
                    if (lookahead().getLexema().equals("(")) {
                        error("assumiu token ausente = \"procedure\"", false);
                        procedureDefine();
                    } else if (lookahead().getId().equals("IDE")) {
                        error("assumiu token ausente = \"function\"", false);
                        function();
                        functionList();
                    } else {
                        error("tentativa falha de assumir produção de \"procedure\" ou \"function\".", true);
                        error("ESPERAVA: IDE ou '('", true);
                        functionList();
                    }
                } else if (firstTypes.contains(currentToken().getLexema())) {
                    error("assumiu token ausente = \"function\"", false);
                    function();
                    functionList();
                } else {
                    error("tentativa falha de assumir produção de \"procedure\" ou \"function\".", true);
                }
        }
    }

    private void functionListErro() {

    }
//********************************** Cabeçalhos de início do código **********************************      
//====================================================================================================

//============================================ Data Types ============================================
//**************************************************************************************************** 
    //Incompleto... 
    private Token value() throws FimInesperadoDeArquivo {
        Token t = lookahead();
        switch (currentToken().getLexema()) {
            case "true":
            case "false":
                if (t.getId().equals("REL") || t.getId().equals("LOG")) {
                    operation();
                } else {
                    consumeToken();
                    return new Token("PRE", "boolean", currentToken().getLinha());
                }
                break;
            default:
                operation();
                break;
        }
        return new Token("PRE", "int", 9);
    }

    //verifica se é um tipo válido
    private Token dataType() throws FimInesperadoDeArquivo {
        Token ret = null;
        if (currentToken().getId().equals("IDE") || firstTypes.contains(currentToken().getLexema())) {
            ret = currentToken();
            consumeToken();
        } else {
            error("ESPERAVA: IDE", true);
        }
        return ret;
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
                error("ESPERAVA: NRO ou CAD", true);
        }
    }

    private Token variavel() throws FimInesperadoDeArquivo {
        String aheadToken = lookahead().getLexema();
        Token ret = currentToken(); //sem global e local por enquanto
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
                            error("ESPERAVA: IDE", true);
                        }
                    } else {
                        error("ESPERAVA: '.'", true);
                    }
                    break;
                default:
                    error("ESPERAVA: IDE, \"global\" ou \"local\"", true);
            }
        }
        return ret;
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
                            error("ESPERAVA: ']'", true);
                        }
                    }
                } else {
                    error("ESPERAVA: ]", true);
                }
                break;
            default:
                error("ESPERAVA: '.' ou '[' ", true);
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
            error("ESPERAVA: IDE", true);
        }
    }
//******************************************** Data Types ********************************************   
//====================================================================================================

//======================================= Variable Declaration =======================================
//****************************************************************************************************
    private void varDeclaration() throws FimInesperadoDeArquivo, identificadorNaoEncontrado {
        if (currentToken().getLexema().equals("{")) {
            consumeToken();
            primeiraVar();
        } else {
            error("ESPERAVA: '{'", true);
        }
    }

    private void primeiraVar() throws FimInesperadoDeArquivo, identificadorNaoEncontrado {
        Token apontado = continueVar();
        Token simbolo = varId(apontado);
        if (simbolo != null) {
            try {
                escopoAtual.inserirSimbolo(simbolo, "variavel", simbolo.getId(), simbolo.getLexema(), apontado.getId().equals("IDE") ? escopoAtual.getTipo(apontado).getToken() : apontado);
            } catch (identificadorJaUtilizado ex) {
                erroSemantico("" + simbolo + " Identificador já utilizado. " + escopoAtual.getSimbolo(simbolo).toString());
            } catch (identificadorNaoEncontrado ex) {
                erroSemantico("" + simbolo + " tipo não encontrado. " + apontado);
            }
        }
    }

    private Token continueVar() throws FimInesperadoDeArquivo {
        if (currentToken().getLexema().equals("struct")) {
            consumeToken();
        }
        return dataType();
    }

    private Token varId(Object apontado) throws FimInesperadoDeArquivo, identificadorNaoEncontrado {
        Token simbolo = null;
        if (currentToken().getId().equals("IDE")) {
            simbolo = currentToken();
            consumeToken();
            varExpression(apontado);
        } else {
            error("ESPERAVA: IDE", true);
        }
        return simbolo;
    }

    private void varExpression(Object apontado) throws FimInesperadoDeArquivo, identificadorNaoEncontrado {
        switch (currentToken().getLexema()) {
            case ",":
                consumeToken();
                Token simbolo = varId(apontado);
                try {
                    escopoAtual.inserirSimbolo(simbolo, "variavel", simbolo.getId(), simbolo.getLexema(), apontado);
                } catch (identificadorJaUtilizado ex) {
                    erroSemantico("" + simbolo + " Identificador já utilizado. " + escopoAtual.getSimbolo(simbolo).toString());
                }
                break;
            case "=":
                consumeToken();
                value();
                verifVar(apontado);
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
                    estrutura(apontado);
                }
                break;
            default:
                error("ESPERAVA: ',', '=', ';', '['", true);
                break;
        }
    }

    private void estrutura(Object apontado) throws FimInesperadoDeArquivo, identificadorNaoEncontrado {
        switch (currentToken().getLexema()) {
            case "[":
                consumeToken();
                vectMatIndex();
                if (currentToken().getLexema().equals("]")) {
                    consumeToken();
                    contMatriz(apontado);
                } else {
                    error("ESPERAVA: ']'", true);
                }
                break;
            case "=":
                consumeToken();
                initVetor(apontado);
                break;
            case ",":
                consumeToken();
                Token simbolo = varId(apontado);
                try {
                    escopoAtual.inserirSimbolo(simbolo, "variavel", simbolo.getId(), simbolo.getLexema(), apontado);
                } catch (identificadorJaUtilizado ex) {
                    erroSemantico("" + simbolo + " Identificador já utilizado. " + escopoAtual.getSimbolo(simbolo).toString());
                }
                break;
            case ";":
                consumeToken();
                proxVar();
                break;
            default:
                error("ESPERAVA: '[', '=', ',', ';'", true);
        }
    }

    private void initVetor(Object apontado) throws FimInesperadoDeArquivo, identificadorNaoEncontrado {
        if (currentToken().getLexema().equals("[")) {
            consumeToken();
            value();
            proxVetor(apontado);
        } else {
            error("ESPERAVA: '['", true);
        }
    }

    private void proxVetor(Object apontado) throws FimInesperadoDeArquivo, identificadorNaoEncontrado {
        switch (currentToken().getLexema()) {
            case ",":
                consumeToken();
                value();
                proxVetor(apontado);
                break;
            case "]":
                consumeToken();
                verifVar(apontado);
                break;
            default:
                error("ESPERAVA: ',', ']'", true);
        }
    }

    private void contMatriz(Object apontado) throws FimInesperadoDeArquivo, identificadorNaoEncontrado {
        switch (currentToken().getLexema()) {
            case "=":
                consumeToken();
                initMatriz(apontado);
                break;
            case ",":
                consumeToken();
                Token simbolo = varId(apontado);
                try {
                    escopoAtual.inserirSimbolo(simbolo, "variavel", simbolo.getId(), simbolo.getLexema(), apontado);
                } catch (identificadorJaUtilizado ex) {
                    erroSemantico("" + simbolo + " Identificador já utilizado. " + escopoAtual.getSimbolo(simbolo).toString());
                }
                break;
            case ";":
                consumeToken();
                proxVar();
                break;
            default:
                error("ESPERAVA: '=', ',', ';'", true);
        }
    }

    private void initMatriz(Object apontado) throws FimInesperadoDeArquivo, identificadorNaoEncontrado {
        if (currentToken().getLexema().equals("[")) {
            consumeToken();
            matrizValue(apontado);
        } else {
            error("ESPERAVA: '['", true);
        }
    }

    private void matrizValue(Object apontado) throws FimInesperadoDeArquivo, identificadorNaoEncontrado {
        if (currentToken().getLexema().equals("[")) {
            consumeToken();
            value();
            proxMatriz(apontado);
        } else {
            error("ESPERAVA: '['", true);
        }
    }

    private void proxMatriz(Object apontado) throws FimInesperadoDeArquivo, identificadorNaoEncontrado {
        switch (currentToken().getLexema()) {
            case ",":
                consumeToken();
                value();
                proxMatriz(apontado);
                break;
            case "]":
                consumeToken();
                next(apontado);
                break;
            default:
                error("ESPERAVA: ',', ']'", true);
        }
    }

    private void next(Object apontado) throws FimInesperadoDeArquivo, identificadorNaoEncontrado {
        switch (currentToken().getLexema()) {
            case ",":
                consumeToken();
                matrizValue(apontado);
                break;
            case "]":
                consumeToken();
                verifVar(apontado);
                break;
            default:
                error("ESPERAVA: ',', ']'", true);
        }
    }

    private void verifVar(Object apontado) throws FimInesperadoDeArquivo, identificadorNaoEncontrado {
        switch (currentToken().getLexema()) {
            case ",":
                consumeToken();
                Token simbolo = varId(apontado);
                try {
                    escopoAtual.inserirSimbolo(simbolo, "variavel", simbolo.getId(), simbolo.getLexema(), apontado);
                } catch (identificadorJaUtilizado ex) {
                    erroSemantico("" + simbolo + " Identificador já utilizado. " + escopoAtual.getSimbolo(simbolo).toString());
                }
                break;
            case ";":
                consumeToken();
                proxVar();
                break;
            default:
                error("ESPERAVA: ',', ';'", true);
                break;
        }
    }

    private void proxVar() throws FimInesperadoDeArquivo, identificadorNaoEncontrado {
        if (currentToken().getLexema().equals("}")) {
            consumeToken();
        } else {
            Object apontado = continueVar();
            Token simbolo = varId(apontado);
            try {
                escopoAtual.inserirSimbolo(simbolo, "variavel", simbolo.getId(), simbolo.getLexema(), apontado);
            } catch (identificadorJaUtilizado ex) {
                erroSemantico("" + simbolo + " Identificador já utilizado. " + escopoAtual.getSimbolo(simbolo).toString());
            }
        }
    }
//*************************************** Variable Declaration ***************************************      
//====================================================================================================

//========================================= Const Declaration ========================================
//**************************************************************************************************** 
    private void constDeclaration() throws FimInesperadoDeArquivo, identificadorNaoEncontrado {
        if (currentToken().getLexema().equals("{")) {
            consumeToken();
            Token tipo = continueConst();
            constId(tipo);
        } else {
            error("ESPERAVA: '{'", true);
        }
    }

    private Token continueConst() throws FimInesperadoDeArquivo {
        Token ret;
        if (currentToken().getLexema().equals("struct")) {
            consumeToken();
            if (currentToken().getId().equals("IDE")) {
                ret = currentToken();
                consumeToken();
                return ret;
            }

        }
        return dataType();
    }

    private void proxConst() throws FimInesperadoDeArquivo, identificadorNaoEncontrado {
        if (currentToken().getLexema().equals("}")) {
            consumeToken();
        } else {
            Token tipo = continueConst();
            constId(tipo);
        }
    }

    private void constId(Token tipo) throws FimInesperadoDeArquivo, identificadorNaoEncontrado {
        if (currentToken().getId().equals("IDE")) {
            Token simbolo = currentToken();
            try {
                escopoAtual.inserirSimbolo(simbolo, "constante", simbolo.getId(), simbolo.getLexema(), tipo);
            } catch (identificadorJaUtilizado ex) {
                erroSemantico("" + simbolo + " Identificador já utilizado. " + escopoAtual.getSimbolo(simbolo).toString());
            }
            consumeToken();
            constExpression(tipo);
        } else {
            error("ESPERAVA: IDE", true);
        }
    }

    private void constExpression(Token tipo) throws FimInesperadoDeArquivo, identificadorNaoEncontrado {
        switch (currentToken().getLexema()) {
            case "=":
                Token simbolo = currentToken();
                consumeToken();
                confereTipo(tipo, value());
                verifConst(tipo);
                break;
            case "[":
                consumeToken();
                vectMatIndex();
                if (currentToken().getLexema().equals("]")) {
                    consumeToken();
                    estruturaConst(tipo);
                } else {
                    error("ESPERAVA: ']'", true);
                }
                break;
            default:
                error("ESPERAVA: '=', '['", true);
                break;
        }
    }

    private void estruturaConst(Token tipo) throws FimInesperadoDeArquivo, identificadorNaoEncontrado {
        switch (currentToken().getLexema()) {
            case "=":
                consumeToken();
                if (currentToken().getLexema().equals("[")) {
                    consumeToken();
                    confereTipo(tipo, value());
                    proxConstVetor(tipo);
                } else {
                    error("ESPERAVA: '['", true);
                }
                break;
            case "[":
                consumeToken();
                vectMatIndex();
                if (currentToken().getLexema().equals("]")) {
                    consumeToken();
                    if (currentToken().getLexema().equals("=")) {
                        consumeToken();
                        initConstMatriz(tipo);
                    } else {
                        error("ESPERAVA: '='", true);
                    }
                } else {
                    error("ESPERAVA: ']'", true);
                }
                break;
            default:
                error("ESPERAVA: '=' ou '['", true);
                break;
        }
    }

    private void proxConstVetor(Token tipo) throws FimInesperadoDeArquivo, identificadorNaoEncontrado {
        switch (currentToken().getLexema()) {
            case ",":
                consumeToken();
                confereTipo(tipo, value());
                proxConstVetor(tipo);
                break;
            case "]":
                consumeToken();
                verifConst(tipo);
                break;
            default:
                error("ESPERAVA: ',' ou ']'", true);
                break;
        }
    }

    private void initConstMatriz(Token tipo) throws FimInesperadoDeArquivo, identificadorNaoEncontrado {
        if (currentToken().getLexema().equals("[")) {
            consumeToken();
            matrizConstValue(tipo);
        } else {
            error("ESPERAVA: '['", true);
        }
    }

    private void matrizConstValue(Token tipo) throws FimInesperadoDeArquivo, identificadorNaoEncontrado {
        if (currentToken().getLexema().equals("[")) {
            consumeToken();
            confereTipo(tipo, value());
            proxConstMatriz(tipo);
        } else {
            error("ESPERAVA: '['", true);
        }
    }

    private void proxConstMatriz(Token tipo) throws FimInesperadoDeArquivo, identificadorNaoEncontrado {
        switch (currentToken().getLexema()) {
            case ",":
                consumeToken();
                confereTipo(tipo, value());
                proxConstMatriz(tipo);
                break;
            case "]":
                consumeToken();
                nextConst(tipo);
                break;
            default:
                error("ESPERAVA: ',', ']'", true);
        }
    }

    private void nextConst(Token tipo) throws FimInesperadoDeArquivo, identificadorNaoEncontrado {
        switch (currentToken().getLexema()) {
            case ",":
                consumeToken();
                matrizConstValue(tipo);
                break;
            case "]":
                consumeToken();
                verifConst(tipo);
                break;
            default:
                error("ESPERAVA: ',' ou ']'", true);
        }
    }

    private void verifConst(Token tipo) throws FimInesperadoDeArquivo, identificadorNaoEncontrado {
        switch (currentToken().getLexema()) {
            case ",":
                consumeToken();
                constId(tipo);
                break;
            case ";":
                consumeToken();
                proxConst();
                break;
            default:
                error("ESPERAVA: ',' ou ';'", true);
                break;
        }
    }
//***************************************** Const Declaration ****************************************  
//====================================================================================================

//======================================= Function Declaration =======================================
//****************************************************************************************************
    private void function() throws FimInesperadoDeArquivo, identificadorNaoEncontrado {
        escopoAtual = new TabelaSimbolos(global);
        escopos.add(escopoAtual);
        Token apontado = dataType();
        if (currentToken().getId().equals("IDE")) {
            Token simbolo = currentToken();
            consumeToken();
            if (currentToken().getLexema().equals("(")) {
                LinkedList<String> lista = continueFunction();
                try {
                    global.inserirSimbolo(simbolo, "funcao", simbolo.getId(), simbolo.getLexema(), new Pair<>(apontado, lista));
                } catch (identificadorJaUtilizado ex) {
                    //erroSemantico("" + simbolo + " Identificador já utilizado. "+ escopoAtual.getSimbolo(simbolo).toString());
                    LinkedList<Simbolo> concorrentes = global.getSimbolo(simbolo);
                    boolean semErro = true;
                    for (Simbolo concorrente : concorrentes) {
                        if (concorrente.getCategoria().equals("procedure") || concorrente.getCategoria().equals("funcao")) {
                            LinkedList<String> apontadoConcorrente;
                            if (concorrente.getVariavel() instanceof LinkedList) {
                                apontadoConcorrente = (LinkedList<String>) concorrente.getVariavel();
                            } else if (concorrente.getVariavel() instanceof Pair) {
                                apontadoConcorrente = (LinkedList<String>) ((Pair) concorrente.getVariavel()).getValue();
                            } else {
                                erroSemantico("" + simbolo + " Identificador já utilizado. " + concorrente);
                                semErro = false;
                                break;
                            }
                            if (equalsParams(lista, apontadoConcorrente)) {
                                erroSemantico("" + simbolo + " Identificador já utilizado. " + concorrente);
                                semErro = false;
                                break;
                            }
                        } else {
                            erroSemantico("" + simbolo + " Identificador já utilizado. " + concorrente);
                            semErro = false;
                            break;
                        }
                    }
                    if (semErro) {
                        concorrentes.add(new Simbolo(simbolo, "funcao", simbolo.getId(), simbolo.getLexema(), new Pair<>(apontado, lista)));
                    }
                }
            } else {
                error("ESPERAVA: '('", true);
            }
        } else {
            error("ESPERAVA: IDE", true);
        }
    }

    private LinkedList<String> continueFunction() throws FimInesperadoDeArquivo, identificadorNaoEncontrado {
        LinkedList<String> ret = new LinkedList();
        consumeToken();
        if (currentToken().getLexema().equals(")")) {
            consumeToken();
            blockFunction();
        } else if (currentToken().getId().equals("IDE") || firstTypes.contains(currentToken().getLexema())) {
            parameters(ret);
            blockFunction();
        } else {
            error("ESPERAVA: IDE, ')', \"int\" \"real\" \"boolean\"ou \"string\"", true);
        }
        return ret;
    }

    private void blockFunction() throws FimInesperadoDeArquivo, identificadorNaoEncontrado {
        if (currentToken().getLexema().equals("{")) {
            consumeToken();
            blockFuncContent();
            if (currentToken().getLexema().equals(";")) {
                consumeToken();
                if (currentToken().getLexema().equals("}")) {
                    consumeToken();
                } else {
                    error("ESPERAVA: '}'", true);
                }
            } else {
                error("ESPERAVA: ';'", true);
            }
        } else {
            error("ESPERAVA: '{'", true);
        }
    }

    private void blockFuncContent() throws FimInesperadoDeArquivo, identificadorNaoEncontrado {
        switch (currentToken().getLexema()) {
            case "var":
                consumeToken();
                varDeclaration();
                content1();
                break;
            case "const":
                constDeclaration();
                content2();
                break;
            default:
                codigo();
                if (currentToken().getLexema().equals("return")) {
                    consumeToken();
                    value();
                } else {
                    error("ESPERAVA: \"return\"", true);
                }
                break;
        }

    }

    private void content1() throws FimInesperadoDeArquivo, identificadorNaoEncontrado {
        if (currentToken().getLexema().equals("const")) {
            constDeclaration();
            content3();
        } else {
            codigo();
            if (currentToken().getLexema().equals("return")) {
                consumeToken();
                value();
            } else {
                error("ESPERAVA: \"return\"", true);
            }
        }
    }

    private void content2() throws FimInesperadoDeArquivo, identificadorNaoEncontrado {
        if (currentToken().getLexema().equals("var")) {
            consumeToken();
            varDeclaration();
            content3();
        } else {
            codigo();
            if (currentToken().getLexema().equals("return")) {
                consumeToken();
                value();
            } else {
                error("ESPERAVA: \"return\"", true);
            }
        }
    }

    private void content3() throws FimInesperadoDeArquivo, identificadorNaoEncontrado {
        codigo();
        if (currentToken().getLexema().equals("return")) {
            consumeToken();
            value();
        } else {
            error("ESPERAVA: \"return\"", true);
        }
    }

    private void parameters(LinkedList<String> lista) throws FimInesperadoDeArquivo {
        Token tipo = dataType();
        if (tipo != null) {
            lista.add(tipo.getLexema());
        }
        if (currentToken().getId().equals("IDE")) {
            paramLoop(lista);
        } else {
            error("ESPERAVA: IDE", true);
        }
    }

    private void paramLoop(LinkedList<String> lista) throws FimInesperadoDeArquivo {
        consumeToken();
        switch (currentToken().getLexema()) {
            case ",":
                consumeToken();
                parameters(lista);
                break;
            case ")":
                consumeToken();
                break;
            default:
                error("ESPERAVA: ',' ou ')'", true);
                break;
        }
    }
//*************************************** Function Declaration ***************************************  
//====================================================================================================

//======================================== Struct Declaration ========================================
//**************************************************************************************************** 
    private void structDeclaration() throws FimInesperadoDeArquivo, identificadorNaoEncontrado {
        if (currentToken().getId().equals("IDE")) {
            structVars();
        } else {
            error("ESPERAVA: IDE", true);
        }
    }

    private void structVars() throws FimInesperadoDeArquivo, identificadorNaoEncontrado {
        Token simbolo = currentToken();
        consumeToken();
        switch (currentToken().getLexema()) {
            case "{":
                try {
                    escopoAtual.inserirSimbolo(simbolo, "struct", simbolo.getId(), simbolo.getLexema(), null);
                } catch (identificadorJaUtilizado ex) {
                    erroSemantico("" + simbolo + " Identificador já utilizado. " + escopoAtual.getSimbolo(simbolo).toString());
                }
                consumeToken();
                blockVarStruct();
                break;
            case "extends":
                consumeToken();
                if (currentToken().getId().equals("IDE")) {
                    Token apontado = currentToken();
                    try {
                        escopoAtual.inserirSimbolo(simbolo, "struct", simbolo.getId(), simbolo.getLexema(), escopoAtual.getTipo(apontado).getToken());
                    } catch (identificadorJaUtilizado ex) {
                        erroSemantico("" + simbolo + " Identificador já utilizado. " + escopoAtual.getSimbolo(simbolo).toString());
                    } catch (identificadorNaoEncontrado ex) {
                        erroSemantico("" + apontado + " Struct pai não declarada.");
                    }
                    consumeToken();
                    if (currentToken().getLexema().equals("{")) {
                        consumeToken();
                        blockVarStruct();
                    } else {
                        error("ESPERAVA: '{'", true);
                    }
                } else {
                    error("ESPERAVA: IDE", true);
                }
                break;
            default:
                error("ESPERAVA: '{' ou \"extends\"", true);
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
                error("ESPERAVA: '{'", true);
            }
        } else {
            error("ESPERAVA: \"var\"", true);
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
            error("ESPERAVA: IDE", true);
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
                    error("ESPERAVA: ]", true);
                }
                break;
            default:
                error("ESPERAVA: ',', ';' ou '['", true);
                break;
        }
    }

    private void proxStructVar() throws FimInesperadoDeArquivo {
        if (currentToken().getLexema().equals("}")) {
            consumeToken();
            if (currentToken().getLexema().equals("}")) {
                consumeToken();
            } else {
                error("ESPERAVA: '}'", true);
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
                    error("ESPERAVA: ']'", true);
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
                error("ESPERAVA: '[', ',' ou ';'", true);
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
                error("ESPERAVA: ',' ou ';'", true);
                break;
        }
    }

    //**************************************** Struct Declaration ****************************************  
    //====================================================================================================
    //====================================== Procedure Declaration =======================================
    //****************************************************************************************************     
    private void startProcedure() throws FimInesperadoDeArquivo, identificadorNaoEncontrado {
        if (currentToken().getLexema().equals("(")) {
            consumeToken();
            if (currentToken().getLexema().equals(")")) {
                consumeToken();
                if (currentToken().getLexema().equals("{")) {
                    consumeToken();
                    procedureContent();
                } else {
                    error("ESPERAVA: '{'", true);
                }
            } else {
                error("ESPERAVA: ')'", true);
            }
        } else {
            error("ESPERAVA: '('", true);
        }

    }

    private void procedureContent() throws FimInesperadoDeArquivo, identificadorNaoEncontrado {
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
                codigo();
                if (currentToken().getLexema().equals("}")) {
                    consumeToken();
                } else {
                    error("ESPERAVA: '}", true);
                }
                break;
        }
    }

    private void procedureContent2() throws FimInesperadoDeArquivo, identificadorNaoEncontrado {
        if (currentToken().getLexema().equals("const")) {
            constDeclaration();
            procedureContent4();
        } else {
            codigo();
            if (currentToken().getLexema().equals("}")) {
                consumeToken();
            } else {
                error("ESPERAVA: '}'", true);
            }
        }
    }

    private void procedureContent3() throws FimInesperadoDeArquivo, identificadorNaoEncontrado {
        if (currentToken().getLexema().equals("var")) {
            consumeToken();
            varDeclaration();
            procedureContent4();
        } else {
            codigo();
            if (currentToken().getLexema().equals("}")) {
                consumeToken();
            } else {
                error("ESPERAVA: '}'", true);
            }
        }
    }

    private void procedureContent4() throws FimInesperadoDeArquivo, identificadorNaoEncontrado {
        codigo();
        if (currentToken().getLexema().equals("}")) {
            consumeToken();
        } else {
            error("ESPERAVA: '}", true);
        }
    }

    private void procedure() throws FimInesperadoDeArquivo, identificadorNaoEncontrado {
        escopoAtual = new TabelaSimbolos(global);
        escopos.add(escopoAtual);
        if (currentToken().getId().equals("IDE")) {
            Token simbolo = currentToken();

            consumeToken();
            if (currentToken().getLexema().equals("(")) {
                LinkedList apontado = procedureParams(simbolo.getLexema());
                try {
                    global.inserirSimbolo(simbolo, "procedure", simbolo.getId(), simbolo.getLexema(), apontado);
                } catch (identificadorJaUtilizado ex) {
                    //erroSemantico("" + simbolo + " Identificador já utilizado. "+ escopoAtual.getSimbolo(simbolo).toString());
                    LinkedList<Simbolo> concorrentes = global.getSimbolo(simbolo);
                    boolean semErro = true;
                    for (Simbolo concorrente : concorrentes) {
                        if (concorrente.getCategoria().equals("procedure") || concorrente.getCategoria().equals("funcao")) {
                            LinkedList<String> apontadoConcorrente;
                            if (concorrente.getVariavel() instanceof LinkedList) {
                                apontadoConcorrente = (LinkedList<String>) concorrente.getVariavel();
                            } else if (concorrente.getVariavel() instanceof Pair) {
                                apontadoConcorrente = (LinkedList<String>) ((Pair) concorrente.getVariavel()).getValue();
                            } else {
                                erroSemantico("" + simbolo + " Identificador já utilizado. " + concorrente);
                                semErro = false;
                                break;
                            }
                            if (equalsParams(apontado, apontadoConcorrente)) {
                                erroSemantico("" + simbolo + " Identificador já utilizado. " + concorrente);
                                semErro = false;
                                break;
                            }
                        } else {
                            erroSemantico("" + simbolo + " Identificador já utilizado. " + concorrente);
                            semErro = false;
                            break;
                        }
                    }
                    if (semErro) {
                        concorrentes.add(new Simbolo(simbolo, "procedure", simbolo.getId(), simbolo.getLexema(), apontado));
                    }
                }

                if (currentToken().getLexema().equals("{")) {
                    consumeToken();
                    procedureContent();
                } else {
                    error("ESPERAVA: '{'", true);
                }
            } else {
                error("ESPERAVA: '('", true);
            }
        } else {
            error("ESPERAVA: IDE", true);
        }
    }

    private LinkedList<String> procedureParams(String procedureName) throws FimInesperadoDeArquivo {
        LinkedList<String> ret = new LinkedList();
        consumeToken();
        if (currentToken().getLexema().equals(")")) {
            consumeToken();
        } else if (currentToken().getId().equals("IDE") || firstTypes.contains(currentToken().getLexema())) {
            parameters(ret);
        } else {
            error("ESPERAVA: ')' ou IDE", true);
        }
        return ret;
    }
//************************************** Procedure Declaration ***************************************  
//====================================================================================================

//====================================== Codigo ======================================================
//****************************************************************************************************  
    private void codigo() throws FimInesperadoDeArquivo, identificadorNaoEncontrado {
        if (firstComando.contains(currentToken().getLexema()) || currentToken().getId().equals("IDE")) { //IDE para increment e decrement, functioncall e atribuição
            comando();
            codigo();
        }
    }

    private void comando() throws FimInesperadoDeArquivo, identificadorNaoEncontrado {
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
                if (currentToken().getLexema().equals(";")) {
                    consumeToken();
                } else {
                    error("ESPERAVA: ','", true);
                }
            } else { // casos de incrementop, decrementop e atribuição (first é variável nos 3 casos)
                Token tk = variavel();

                //atribuição
                switch (currentToken().getLexema()) {
                    case "=":
                        consumeToken();
                        Token v = value();
                        try {
                            LinkedList<Simbolo> simb = escopoAtual.getSimbolo(tk);
                            Simbolo s = simb.get(0);
                            if (s.getCategoria().equals("variavel")) {
                                confereTipo(v, (Token) s.getVariavel());
                            } else {
                                erroSemantico(s + " Não é variável");
                            }
                        } catch (identificadorNaoEncontrado e) {
                            erroSemantico(tk + " Variável não declarada");
                        }
                        if (currentToken().getLexema().equals(";")) {
                            consumeToken();
                        } else {
                            error("ESPERAVA: ';'", true);
                        }
                        break;
                    case "++":
                    case "--":
                        consumeToken();
                        if (currentToken().getLexema().equals(";")) {
                            consumeToken();
                        } else {
                            error("ESPERAVA: ';'", true);
                        }
                        break;
                    default:
                        error("ESPERAVA: '=', \"--\" ou \"++\"", true);
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
                error("ESPERAVA: ';'", true);
            }
        } else {
            error("ESPERAVA: \"++\" ou \"--\"", true);
        }
    }

    private void print() throws FimInesperadoDeArquivo {
        consumeToken();
        if (currentToken().getLexema().equals("(")) {
            consumeToken();
            printableList();
        } else {
            error("ESPERAVA: '('", true);
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
                    error("ESPERAVA: ';'", true);
                }
                break;
            default:
                error("ESPERAVA: ',' ou ')'", true);
                break;
        }
    }

    private void read() throws FimInesperadoDeArquivo {
        consumeToken();
        if (currentToken().getLexema().equals("(")) {
            consumeToken();
            readParams();
        } else {
            error("ESPERAVA: '('", true);
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
                    error("ESPERAVA: ';'", true);
                }
                break;
            default:
                error("ESPERAVA: ',' ou ')'", true);
                break;
        }
    }

    private void conditional() throws FimInesperadoDeArquivo, identificadorNaoEncontrado {
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
                            error("ESPERAVA: '}'", true);
                        }
                    } else {
                        error("ESPERAVA: '{'", true);
                    }
                } else {
                    error("ESPERAVA: \"then\"", true);
                }
            } else {
                error("ESPERAVA: ')'", true);
            }
        } else {
            error("ESPERAVA: '('", true);
        }

    }

    private void elsePart() throws FimInesperadoDeArquivo, identificadorNaoEncontrado {
        if (currentToken().getLexema().equals("else")) {
            consumeToken();
            if (currentToken().getLexema().equals("{")) {
                consumeToken();
                codigo();
                if (currentToken().getLexema().equals("}")) {
                    consumeToken();
                } else {
                    error("ESPERAVA: '}", true);
                }
            } else {
                error("ESPERAVA: \"else\"", true);
            }
        }
    }

    private void whileLoop() throws FimInesperadoDeArquivo, identificadorNaoEncontrado {
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
                        error("ESPERAVA: '}'", true);
                    }
                } else {
                    error("ESPERAVA: '{'", true);
                }
            } else {
                error("ESPERAVA: ')'", true);
            }
        } else {
            error("ESPERAVA: '('", true);
        }
    }

//************************************** Codigo ****************************************************** 
//====================================================================================================
//====================================== Typedef Declaration =======================================
//**************************************************************************************************** 
    private void typedefDeclaration() throws FimInesperadoDeArquivo, identificadorNaoEncontrado {
        if (currentToken().getLexema().equals("struct")) {
            consumeToken();
            if (currentToken().getId().equals("IDE")) {
                Token apontado = currentToken();
                consumeToken();
                if (currentToken().getId().equals("IDE")) {
                    Token simbolo = currentToken();
                    consumeToken();
                    if (currentToken().getLexema().equals(";")) {
                        try {
                            escopoAtual.inserirSimbolo(simbolo, "struct", simbolo.getId(), simbolo.getLexema(), escopoAtual.getTipo(apontado).getToken());
                        } catch (identificadorJaUtilizado ex) {
                            /*LinkedList<Simbolo> s = escopoAtual.getSimbolo(simbolo);
                            if (s.get(0).getCategoria().equals("struct")) {
                                Simbolo simbolo1 = new Simbolo(simbolo, "struct", simbolo.getId(), simbolo.getLexema(), escopoAtual.getTipo(apontado).getToken());
                                s.add(simbolo1);
                                escopoAtual.getTipos().put(simbolo, simbolo1);
                            } else {*/
                                erroSemantico("" + simbolo + " Identificador já utilizado. " + escopoAtual.getSimbolo(simbolo).toString());
                            //}
                        } catch (identificadorNaoEncontrado e) {
                            erroSemantico("" + simbolo + " tipo não encontrado. " + apontado);
                        }
                        consumeToken();
                    } else {
                        error("ESPERAVA: ';'", true);
                    }
                } else {
                    error("ESPERAVA: IDE", true);
                }
            } else {
                error("ESPERAVA: IDE", true);
            }
        } else {
            Token apontado = dataType();
            if (currentToken().getId().equals("IDE")) {
                Token simbolo = currentToken();
                consumeToken();
                if (currentToken().getLexema().equals(";")) {
                    try {
                        escopoAtual.inserirSimbolo(simbolo, "tipo", simbolo.getId(), simbolo.getLexema(), apontado.getId().equals("IDE") ? escopoAtual.getTipo(apontado).getToken() : apontado);
                    } catch (identificadorJaUtilizado ex) {
                        erroSemantico("" + simbolo + " Identificador já utilizado. " + escopoAtual.getSimbolo(simbolo).toString());
                    } catch (identificadorNaoEncontrado e) {
                        erroSemantico("" + simbolo + " tipo não encontrado. " + apontado);
                    }
                    consumeToken();
                } else {
                    error("ESPERAVA: ';'", true);
                }
            } else {
                error("ESPERAVA: IDE ou \"struc\"", true);
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
                } else {
                    aritmeticValue();
                }
                break;
            case "--":
            case "++":
                consumeToken();
                variavel();
            default:
                if (currentToken().getId().equals("IDE") && lookahead().getLexema().equals("(")) {
                    functionCall();
                } else {
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
                error("ESPERAVA: '*' ou '/'", true);
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
                error("ESPERAVA: REL ou LOG", true);
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
            error("ESPERAVA: '!'", true);

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
                            error("ESPERAVA: ')'", true);
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
                        error("ESPERAVA: ')'", true);
                    }
                }
                break;
            default:
                error("ESPERAVA: '!', '(', \"true\" ou \"false\"", true);
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
                            error("ESPERAVA: ')'", true);
                        }
                    }
                    if (currentToken().getLexema().equals(")")) {
                        consumeToken();
                        if (currentToken().getId().equals("REL") || currentToken().getId().equals("LOG")) {
                            logicOrRelacionalOp();
                        }
                    } else {
                        error("ESPERAVA: ')'", true);
                    }
                    break;
                default:
                    error("ESPERAVA: '!', '(', \"true\" ou \"false\"", true);
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
            error("ESPERAVA: '('", true);
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
                error("ESPERAVA: ',' ou ')'", true);
                break;
        }
    }
//****************************************** Function Call *******************************************  
//====================================================================================================

    private boolean equalsParams(List<String> a, List<String> b) {
        if (a.size() != b.size()) {
            return false;
        }
        LinkedList<String> c = new LinkedList(b);

        Iterator ia = a.iterator();
        Iterator ib = b.iterator();
        while (ia.hasNext()) {
            if (!ia.next().equals(ib.next())) {
                return false;
            }
        }
        return true;
    }

    private boolean confereTipo(Token t1, Token t2) {
        if (t1 == null || t2 == null) {
            return false;
        }

        Simbolo simbolo1 = null;
        Simbolo simbolo2 = null;

        String lexema1 = null;
        String lexema2 = null;

        if (t1.getId().equals("IDE")) {
            try {
                simbolo1 = tipoPrimitivoApontado(escopoAtual.getTipo(t1));
                if (simbolo1.getCategoria().equals("tipo")) {
                    lexema1 = ((Token) simbolo1.getVariavel()).getLexema();
                    simbolo1 = null;
                }

            } catch (identificadorNaoEncontrado ex) {
                erroSemantico("" + t1 + " o tipo utilizado não foi declarado. ");
                return false;
            }
        } else {
            lexema1 = t1.getLexema();
        }

        if (t2.getId().equals("IDE")) {
            try {

                simbolo2 = tipoPrimitivoApontado(escopoAtual.getTipo(t2));
                System.out.println(simbolo2.getToken().getLexema());
                if (simbolo2.getCategoria().equals("tipo")) {
                    lexema2 = ((Token) simbolo2.getVariavel()).getLexema();
                    simbolo2 = null;
                }
            } catch (identificadorNaoEncontrado ex) {
                erroSemantico("" + t2 + " o tipo utilizado não foi declarado. ");
                return false;
            }
        } else {
            lexema2 = t2.getLexema();
        }
        if ((lexema1 == null && lexema2 != null) || (lexema1 != null && lexema2 == null)) {
            erroSemantico("" + t1 + " Tipos Incompatíveis: " + (lexema1 == null ? "struct " + simbolo1.getToken().getLexema() : lexema1) + " e " + (lexema2 == null ? "struct " + simbolo2.getToken().getLexema() : lexema2));
            return false;
        } else if (lexema1 != null && lexema2 != null) {
            if (lexema1.equals(lexema2)) {
                return true;
            } else {
                erroSemantico("" + t1 + " Tipos Incompatíveis: " + lexema1 + " e " + lexema2);
                return false;
            }
        }

        return simbolo1.getToken().getLexema().equals(simbolo2.getToken().getLexema());

    }

    private Simbolo tipoPrimitivoApontado(Simbolo t) throws identificadorNaoEncontrado {
        Object variavel = t.getVariavel();
        if (variavel == null) {
            return t;
        }
        if (variavel instanceof Token) {
            Token tipoApontado = (Token) variavel;
            if (tipoApontado.getId().equals("IDE")) {
                return tipoPrimitivoApontado(escopoAtual.getTipo(tipoApontado));
            } else {
                return t;
            }
        } else {
            throw new identificadorNaoEncontrado();
        }

    }
}
