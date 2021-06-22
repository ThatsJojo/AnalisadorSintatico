package controller;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.util.Pair;
import model.Arquivo;
import model.Simbolo;
import model.TabelaSimbolos;
import model.Token;
import util.ExpressaoInvalidaException;
import util.FimInesperadoDeArquivo;
import util.VariavelInvalidaException;
import util.identificadorJaUtilizado;
import util.identificadorNaoEncontrado;

public class AnalisadorSintatico {

    private Token privateCurrentToken;
    private int erros;
    private int errosSemanticos;
    private ArrayList tokens;
    private int countToken = 0;
    private String analiseret = "";
    private static final ArrayList<TabelaSimbolos> ESCOPOS = new ArrayList();

    private static final HashSet<String> FIRSTTYPES = new HashSet();
    private static final HashSet<String> FIRSTCOMANDO = new HashSet();
    private static final HashSet<String> FIRSTINICIO = new HashSet();
    private static TabelaSimbolos escopoAtual;
    private static TabelaSimbolos global;

    public AnalisadorSintatico() {
        //Conjunto first do método types
        FIRSTTYPES.add("int");
        FIRSTTYPES.add("real");
        FIRSTTYPES.add("boolean");
        FIRSTTYPES.add("string");

        //Conjunto first do método comando //faltam algumas coisas
        FIRSTCOMANDO.add("print");
        FIRSTCOMANDO.add("read");
        FIRSTCOMANDO.add("while");
        FIRSTCOMANDO.add("const");
        FIRSTCOMANDO.add("typedef");
        FIRSTCOMANDO.add("struct");
        FIRSTCOMANDO.add("if");
        FIRSTCOMANDO.add("++");
        FIRSTCOMANDO.add("--");

        //conjunto first do método comando
        FIRSTINICIO.add("typedef");
        FIRSTINICIO.add("const");
        FIRSTINICIO.add("var");
        FIRSTINICIO.add("function");
        FIRSTINICIO.add("procedure");
        FIRSTINICIO.add("struct");

    }

    private void erroSemantico(String texto) {
        analiseret += texto + "\n";
        this.errosSemanticos++;
        System.out.println(texto);
    }

    public String analise(Arquivo arq, ArrayList tokens) {
        ESCOPOS.clear();
        escopoAtual = new TabelaSimbolos(null);
        global = escopoAtual;
        ESCOPOS.add(escopoAtual);
        analiseret = "";
        this.erros = 0;
        this.errosSemanticos = 0;
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
            try {
                System.out.println("Erro interno + " + currentToken() + lookahead() + lookaheadP());
            } catch (FimInesperadoDeArquivo ex1) {
                Logger.getLogger(AnalisadorSintatico.class.getName()).log(Level.SEVERE, null, ex1);
            }
        }

        try {
            while (hasToken()) {
                error("ESPERADO: EOF", true);
            }
        } catch (FimInesperadoDeArquivo ex) {
        }

        System.out.println("Análise Sintática realizada " + (erros == 0 ? "com" : "sem") + " sucesso (" + String.format("%03d", erros)
                + " erros sintáticos encontrados) no arquivo " + arq.getNome() + ".");

        if (this.erros > 0) {
            analiseret = "O aqruivo atual possui erros sintáticos. Assim sendo, a análise sintática pode não ter sido feita corretamente.\n\n" + analiseret;
        }

        System.out.println("Análise Semântica realizada " + (this.errosSemanticos == 0 ? "com" : "sem") + " sucesso (" + String.format("%03d", this.errosSemanticos)
                + " erros semânticos encontrados) no arquivo " + arq.getNome() + ".");
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
//                analiseret += "" + privateCurrentToken.getLinha() + " "
//                        + privateCurrentToken.getId() + " " + privateCurrentToken.getLexema() + "\n";
            }
            this.privateCurrentToken = new Token("EOF", null, privateCurrentToken.getLinha());
            countToken++;
            return false;
        } else {
            if (countToken != 0 && !tokenErro) {
//                analiseret += "" + privateCurrentToken.getLinha() + " "
//                        + privateCurrentToken.getId() + " " + privateCurrentToken.getLexema() + "\n";
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

    private Token lookback() throws FimInesperadoDeArquivo {
        return (Token) tokens.get(countToken - 2);
    }

    private Token lookaheadP() throws FimInesperadoDeArquivo {
        if (countToken >= tokens.size()) {
            erros++;
            throw new FimInesperadoDeArquivo();
        }
        return (Token) tokens.get(countToken + 1);
    }

    private void error(String esperado, boolean consumir) throws FimInesperadoDeArquivo {
//        analiseret += "" + privateCurrentToken.getLinha() + " " + privateCurrentToken.getId() + " " + privateCurrentToken.getLexema() + " ERRO SINTÁTICO. "
//                + esperado + ".  Encontrado: " + currentToken().getLexema() + "\n";
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
        while (!FIRSTTYPES.contains(currentToken().getLexema()) && !currentToken().getLexema().equals("{") && !currentToken().getId().equals("IDE")) {
            if (FIRSTINICIO.contains(currentToken().getLexema())) {
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
            while (currentToken().getLexema().equals("var") || (!currentToken().getLexema().equals("}") && !FIRSTINICIO.contains(currentToken().getLexema()))) {
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

        while (currentToken().getLexema().equals("var") || (!FIRSTTYPES.contains(currentToken().getLexema())
                && !currentToken().getLexema().equals("{") && !currentToken().getId().equals("IDE") && !FIRSTINICIO.contains(currentToken().getLexema()))) {
            error("ESPERAVA: \"int\", \"real\", \"boolean\", \"string\", IDE ou '{'", true);
        }
        if (FIRSTINICIO.contains(currentToken().getLexema())) {
            error("Token de sincronização", false);
            header1();
            return;
        }

        Token t = lookahead();
        if (FIRSTINICIO.contains(t.getLexema())) {
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
                if (FIRSTINICIO.contains(tp.getLexema())) {
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
            while (currentToken().getLexema().equals("const") || (!currentToken().getLexema().equals("}") && !FIRSTINICIO.contains(currentToken().getLexema()))) {
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

        while (currentToken().getLexema().equals("const") || (!FIRSTTYPES.contains(currentToken().getLexema()) && !currentToken().getId().equals("IDE") && !FIRSTINICIO.contains(currentToken().getLexema()))) {
            error("ESPERAVA: \"int\", \"real\", \"boolean\", \"string\", IDE, '{', ou \"var\"", true);
        }
        if (FIRSTINICIO.contains(currentToken().getLexema())) {
            error("Token de sincronização", false);
            header2();
            return;
        }

        Token t = lookahead();
        if (FIRSTINICIO.contains(t.getLexema())) {
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
                if (FIRSTINICIO.contains(tp.getLexema())) {
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
            while (currentToken().getLexema().equals("const") || currentToken().getLexema().equals("var") || (!currentToken().getLexema().equals("}") && !FIRSTINICIO.contains(currentToken().getLexema()))) {
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

        while (currentToken().getLexema().equals("const") || currentToken().getLexema().equals("var") || (!FIRSTTYPES.contains(currentToken().getLexema()) && !currentToken().getId().equals("IDE") && !FIRSTINICIO.contains(currentToken().getLexema()))) {
            error("ESPERAVA: \"int\", \"real\", \"boolean\", \"string\" ou IDE ", true);
        }
        if (FIRSTINICIO.contains(currentToken().getLexema())) {
            error("Token de sincronização", false);
            header3();
            return;
        }

        Token t = lookahead();
        if (FIRSTINICIO.contains(t.getLexema())) {
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
                if (FIRSTINICIO.contains(tp.getLexema())) {
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
                } else if (FIRSTTYPES.contains(currentToken().getLexema())) {
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
    private Token value() throws FimInesperadoDeArquivo, ExpressaoInvalidaException {
        Token t = lookahead();
        Token tipo = new Token(null, null, currentToken().getLinha());
        if (currentToken().getId().equals("CAD")) {
            Token cadeia = currentToken();
            consumeToken();
            while (currentToken().getLexema().equals("+")) {
                consumeToken();
                if (currentToken().getId().equals("CAD")) {
                    consumeToken();
                } else {
                    error("CAD", true);
                }
            }
            tipo.setId("CAD");
            tipo.setLexema(cadeia.getLexema());
            return tipo;
        }

        switch (currentToken().getLexema()) {
            case "true":
            case "false":
                if (t.getId().equals("REL") || t.getId().equals("LOG")) {
                    Token tk = operation(tipo);
                    if (tipo.getId() == null) {
                        tipo.setId(tk.getId());
                        tipo.setLexema(tk.getLexema());
                    } else {
                        confereTipo(tipo, tk);
                    }
                } else {
                    consumeToken();
                    return new Token("PRE", "boolean", currentToken().getLinha());
                }
                break;
            default:
                Token tk = operation(tipo);
                if (tipo.getId() == null) {
                    tipo.setId(tk.getId());
                    tipo.setLexema(tk.getLexema());
                } else {
                    confereTipo(tipo, tk);
                }
                break;
        }
        if (tipo.getId() == null) {
            throw new ExpressaoInvalidaException(tipo);
        }
        return tipo;
    }
    //verifica se é um tipo válido

    private Token dataType(String struct) throws FimInesperadoDeArquivo {
        Token ret = null;
        if (currentToken().getId().equals("IDE") || FIRSTTYPES.contains(currentToken().getLexema())) {
            ret = new Token(currentToken().getId(), struct + currentToken().getLexema(), currentToken().getLinha());
            consumeToken();
        } else {
            error("ESPERAVA: IDE", true);
        }
        if (ret != null) {
            try {
                ret = ret.getId().equals("IDE") ? escopoAtual.getTipo(ret).getToken() : ret;
            } catch (identificadorNaoEncontrado ex) {
                erroSemantico("" + ret + " tipo não encontrado. ");
            }
        }
        return ret;
    }

    private void vectMatIndex() throws FimInesperadoDeArquivo {
        Token atual = currentToken();
        try {
            aritmeticOp(new Token("NRO", "2", currentToken().getLinha()));
        } catch (ExpressaoInvalidaException ex) {
            erroSemantico(String.format("%04d", ex.getErro().getLinha()) + " " + atual.getId() + " " + atual.getLexema() + " Expressão inválida: possui tipos não inteiros ou operadores lógicos/relacionais. ");
        }
    }

    private Token aritmeticValue() throws FimInesperadoDeArquivo {
        Token ret = new Token("PRE", "int", currentToken().getLinha());
        switch (currentToken().getId()) {
            case "NRO":
                consumeToken();
                ret = new Token("PRE", "int", currentToken().getLinha());
                break;
            case "NRO REAL":
                consumeToken();
                ret = new Token("PRE", "real", currentToken().getLinha());
                break;
            case "CAD":
                consumeToken();
                ret = new Token("PRE", "string", currentToken().getLinha());
                break;
            default:
                error("ESPERAVA: NRO ou CAD", true);
        }
        return ret;
    }

    private Token variavel() throws FimInesperadoDeArquivo, VariavelInvalidaException {
        Token flag = null;
        switch (currentToken().getLexema()) {
            case "global":
            case "local":
                flag = currentToken();
                consumeToken();
                if (currentToken().getLexema().equals(".")) {
                    consumeToken();
                    if (!currentToken().getId().equals("IDE")) {
                        error("ESPERAVA: IDE, \"global\" ou \"local\"", true);
                        throw new VariavelInvalidaException("");
                    }
                    return auxVariavel(flag);
                } else {
                    error("ESPERAVA: '.'", true);
                }
                break;
            default:
                if (!currentToken().getId().equals("IDE")) {
                    error("ESPERAVA: IDE, \"global\" ou \"local\"", true);
                    throw new VariavelInvalidaException("");
                }
                return auxVariavel(flag);
        }
        throw new VariavelInvalidaException("");
    }

    private Token auxVariavel(Token flag) throws FimInesperadoDeArquivo, VariavelInvalidaException {
        String message = "";
        Token tipo = new Token(null, null, currentToken().getLinha());
        TabelaSimbolos tempEscopo = escopoAtual;
        String concat = "";
        if (!message.equals("")) {
            concat = "\n";
        }

        String aheadToken = lookahead().getLexema();
        tipo = new Token(null, null, currentToken().getLinha()); //sem global e local por enquanto
        Token atual = currentToken();
        boolean isStruct = false;
        Simbolo s = null;
        if (currentToken().getId().equals("IDE")) {
            consumeToken();
            Simbolo s2 = null;
            try {
                s = (flag == null) ? tempEscopo.getSimbolo(atual).get(0) : (flag.getLexema().equals("global") ? global.getSimbolo(atual).get(0) : tempEscopo.localSimbolo(atual));
                tipo.setFlag(s.getCategoria());
                if (s.getCategoria().equals("variavel") || s.getCategoria().equals("constante")) {
                    Token tipoApontado = (Token) s.getVariavel();
                    if (!tipoApontado.getId().equals("IDE")) {
                        tipo.setId(tipoApontado.getId());
                        tipo.setLexema(tipoApontado.getLexema());
                    } else {
                        try {
                            s2 = tempEscopo.getTipo(tipoApontado);
                            Token tipoEncontrado = s2.getToken();
                            if (s2.getCategoria().equals("struct")) {
                                isStruct = true;
                            }
                            tipo.setId(tipoEncontrado.getId());
                            tipo.setLexema(tipoEncontrado.getLexema());
                        } catch (identificadorNaoEncontrado e) {
                            message = "" + atual + " tipo da variável ou constante não foi encontrado. " + s + concat;
                            //erroSemantico("" + atual + " tipo da variável ou constante não foi encontrado. " + s);
                        }
                    }
                } else {
                    s = null;
                    message = "" + atual + " o identificador não está vinculado a uma variável nem constante. " + (s == null ? "" : s) + concat;
                }

            } catch (identificadorNaoEncontrado e) {
                message = atual + " Variável não declarada" + concat;
            }

            if (aheadToken.equals(".") || aheadToken.equals("[")) {
                if ((!isStruct && aheadToken.equals(".")) || (aheadToken.equals("[") && !(s != null && (s.getVetorStruct().equals("matriz") || s.getVetorStruct().equals("vetor"))))) {
                    erroSemantico("" + atual + " Busca de elemento em variável não declarada como struct. Obs: vetor de struct não é tratada como uma struct, apenas seus elementos");
                    s = null;
                }
                boolean nVetor = true;
                Simbolo campo = s;
                do {
                    //ContElemento
                    switch (currentToken().getLexema()) {
                        case ".":
                            nVetor = true;
                            Token ret = null;
                            consumeToken();
                            if (currentToken().getId().equals("IDE")) {
                                Token ideToken = currentToken();
                                consumeToken();
                                if (s == null) {
                                    if (isStruct) {
                                        erroSemantico(ideToken + " variável não indica uma struct.");
                                    }
                                } else if (isStruct) {
                                    try {
                                        if (s2 != null) {
                                            campo = s2.getEscopo().getSimbolo(ideToken).getFirst();
                                            Token tipoCampo = (Token) campo.getVariavel();

                                            tipo.setId(tipoCampo.getId());
                                            tipo.setLexema(tipoCampo.getLexema());
                                            tipo.setLinha(ideToken.getLinha());
                                            if (campo.getCategoria().equals("struct")) {
                                                s2 = escopoAtual.getTipo((Token) campo.getVariavel());
                                            }
                                        }
                                    } catch (identificadorNaoEncontrado ex) {
                                        erroSemantico(ideToken + " campo ausente na struct passada");
                                    }
                                }
                            } else {
                                error("ESPERAVA: IDE", true);
                            }
                            break;
                        case "[":
                            nVetor = false;
                            boolean nMatriz = !(campo != null && (campo.getVetorStruct().equals("matriz") || campo.getVetorStruct().equals("vetor")));
                            if (nMatriz) {
                                erroSemantico(((campo != null) ? campo.getToken() : currentToken()) + " a variável não é um vetor ou matriz. " + ((campo != null) ? campo.getVetorStruct() : ""));
                                s = null;
                            }
                            if (campo != null) {
                                tipo.setId(((Token) campo.getVariavel()).getId());
                                tipo.setLexema(((Token) campo.getVariavel()).getLexema());
                            }
                            consumeToken();
                            vectMatIndex();
                            if (currentToken().getLexema().equals("]")) {
                                consumeToken();
                                if (currentToken().getLexema().equals("[")) {
                                    consumeToken();
                                    vectMatIndex();
                                    if (currentToken().getLexema().equals("]")) {
                                        if (campo != null && !(campo.getVetorStruct().equals("matriz"))) {
                                            if (!nMatriz) {
                                                erroSemantico(campo.getToken() + " a variável não é uma matriz.");
                                            }
                                            s = null;
                                        }
                                        consumeToken();
                                    } else {
                                        error("ESPERAVA: ']'", true);
                                    }
                                } else if (campo != null && !(campo.getVetorStruct().equals("vetor"))) {
                                    if (!nMatriz) {
                                        erroSemantico(campo.getToken() + " a variável não é um vetor.");
                                    }
                                    s = null;
                                }
                            } else {
                                error("ESPERAVA: ]", true);
                            }
                            break;
                        default:
                            error("ESPERAVA: '.' ou '[' ", true);
                    }
                    //contElemento
                } while (currentToken().getLexema().equals(".") || (currentToken().getLexema().equals("[") && nVetor));
            } else {
                if (s != null) {
                    tipo.setId(((Token) s.getVariavel()).getId());
                    tipo.setLexema(((Token) s.getVariavel()).getLexema());
                }
            }
        } else {
            error("ESPERAVA: IDE " + currentToken() + "  " + lookahead() + "  " + lookaheadP(), true);
        }

        if (tipo.getId() == null) {
            VariavelInvalidaException var = new VariavelInvalidaException(message);
            throw var;
        }
        return tipo;
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
        /*System.out.println("******************************************************************");
        System.out.println(currentToken()+"  "+lookahead()+"    "+lookaheadP());
        escopoAtual.printSimbolos();
        System.out.println("******************************************************************");*/

    }

    private void primeiraVar() throws FimInesperadoDeArquivo, identificadorNaoEncontrado {
        Token apontado = continueVar();
        Token simbolo = varId(apontado);
        Simbolo s = null;
        if (simbolo != null) {
            try {
                s = escopoAtual.inserirSimbolo(simbolo, "variavel", simbolo.getId(), simbolo.getLexema(), apontado);
            } catch (identificadorJaUtilizado ex) {
                erroSemantico("" + simbolo + " Identificador já utilizado. " + escopoAtual.getSimbolo(simbolo).toString());
            }
        }
        varExpression(apontado, s);
    }

    private Token continueVar() throws FimInesperadoDeArquivo {
        String struct = "";
        if (currentToken().getLexema().equals("struct")) {
            consumeToken();
            struct = "struct ";
        }
        Token retorno = dataType(struct);
        Token ret = new Token(retorno.getId(), retorno.getLexema(), retorno.getLinha());
        return ret;
    }

    private Token varId(Token apontado) throws FimInesperadoDeArquivo, identificadorNaoEncontrado {
        Token simbolo = null;
        if (currentToken().getId().equals("IDE")) {
            simbolo = currentToken();
            consumeToken();
        } else {
            error("ESPERAVA: IDE", true);
        }
        return simbolo;
    }

    private void varExpression(Token apontado, Simbolo s) throws FimInesperadoDeArquivo, identificadorNaoEncontrado {
        switch (currentToken().getLexema()) {
            case ",":
                consumeToken();
                Token simbolo = varId(apontado);
                if (simbolo != null) {
                    try {
                        s = escopoAtual.inserirSimbolo(simbolo, "variavel", simbolo.getId(), simbolo.getLexema(), apontado);
                    } catch (identificadorJaUtilizado ex) {
                        erroSemantico("" + simbolo + " Identificador já utilizado. " + escopoAtual.getSimbolo(simbolo).toString());
                    }
                }
                varExpression(apontado, s);
                break;
            case "=":
                consumeToken();
                Token val;
                try {
                    val = value();
                    confereTipo(apontado, val);
                } catch (ExpressaoInvalidaException ex) {
                    erroSemantico(String.format("%04d", ex.getErro().getLinha()) + " " + apontado.getId() + " " + apontado.getLexema() + " Expressão inválida ");
                }
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
                    estrutura(apontado, s);
                } else {
                    error("ESPERAVA: ']'", true);
                }
                break;
            default:
                error("ESPERAVA: ',', '=', ';', '['", true);
                break;
        }
    }

    private void estrutura(Token apontado, Simbolo s) throws FimInesperadoDeArquivo, identificadorNaoEncontrado {
        switch (currentToken().getLexema()) {
            case "[":
                consumeToken();
                vectMatIndex();
                if (currentToken().getLexema().equals("]")) {
                    consumeToken();
                    contMatriz(apontado, s);
                } else {
                    error("ESPERAVA: ']'", true);
                }
                break;
            case "=":
                if (s != null) {
                    s.setVetorStruct("vetor");
                }
                consumeToken();
                initVetor(apontado);
                break;
            case ",":
                if (s != null) {
                    s.setVetorStruct("vetor");
                }
                consumeToken();
                Token simbolo = varId(apontado);
                if (simbolo != null) {
                    try {
                        s = escopoAtual.inserirSimbolo(simbolo, "variavel", simbolo.getId(), simbolo.getLexema(), apontado);
                    } catch (identificadorJaUtilizado ex) {
                        erroSemantico("" + simbolo + " Identificador já utilizado. " + escopoAtual.getSimbolo(simbolo).toString());
                    }
                }
                varExpression(apontado, s);
                break;
            case ";":
                consumeToken();
                proxVar();
                break;
            default:
                error("ESPERAVA: '[', '=', ',', ';'", true);
        }
    }

    private void initVetor(Token apontado) throws FimInesperadoDeArquivo, identificadorNaoEncontrado {
        if (currentToken().getLexema().equals("[")) {
            consumeToken();
            try {
                confereTipo(apontado, value());
            } catch (ExpressaoInvalidaException ex) {
                erroSemantico(String.format("%04d", ex.getErro().getLinha()) + " " + apontado.getId() + " " + apontado.getLexema() + " Expressão inválida ");
            }
            proxVetor(apontado);
        } else {
            error("ESPERAVA: '['", true);
        }
    }

    private void proxVetor(Token apontado) throws FimInesperadoDeArquivo, identificadorNaoEncontrado {
        switch (currentToken().getLexema()) {
            case ",":
                consumeToken();
                try {
                    confereTipo(apontado, value());
                } catch (ExpressaoInvalidaException ex) {
                    erroSemantico(String.format("%04d", ex.getErro().getLinha()) + " " + apontado.getId() + " " + apontado.getLexema() + " Expressão inválida ");
                }
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

    private void contMatriz(Token apontado, Simbolo s) throws FimInesperadoDeArquivo, identificadorNaoEncontrado {
        if (s != null) {
            s.setVetorStruct("matriz");
        }
        switch (currentToken().getLexema()) {
            case "=":
                consumeToken();
                initMatriz(apontado);
                break;
            case ",":
                consumeToken();
                Token simbolo = varId((Token) apontado);
                if (simbolo != null) {
                    try {
                        s = escopoAtual.inserirSimbolo(simbolo, "variavel", simbolo.getId(), simbolo.getLexema(), apontado);
                    } catch (identificadorJaUtilizado ex) {
                        erroSemantico("" + simbolo + " Identificador já utilizado. " + escopoAtual.getSimbolo(simbolo).toString());
                    }
                }
                varExpression(apontado, s);
                break;
            case ";":
                consumeToken();
                proxVar();
                break;
            default:
                error("ESPERAVA: '=', ',', ';'", true);
        }
    }

    private void initMatriz(Token apontado) throws FimInesperadoDeArquivo, identificadorNaoEncontrado {
        if (currentToken().getLexema().equals("[")) {
            consumeToken();
            matrizValue(apontado);
        } else {
            error("ESPERAVA: '['", true);
        }
    }

    private void matrizValue(Token apontado) throws FimInesperadoDeArquivo, identificadorNaoEncontrado {
        if (currentToken().getLexema().equals("[")) {
            consumeToken();
            try {
                confereTipo(apontado, value());
            } catch (ExpressaoInvalidaException ex) {
                erroSemantico(String.format("%04d", ex.getErro().getLinha()) + " " + apontado.getId() + " " + apontado.getLexema() + " Expressão inválida ");
            }
            proxMatriz(apontado);
        } else {
            error("ESPERAVA: '['", true);
        }
    }

    private void proxMatriz(Token apontado) throws FimInesperadoDeArquivo, identificadorNaoEncontrado {
        switch (currentToken().getLexema()) {
            case ",":
                consumeToken();
                try {
                    confereTipo(apontado, value());
                } catch (ExpressaoInvalidaException ex) {
                    erroSemantico(String.format("%04d", ex.getErro().getLinha()) + " " + apontado.getId() + " " + apontado.getLexema() + " Expressão inválida ");
                }
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

    private void next(Token apontado) throws FimInesperadoDeArquivo, identificadorNaoEncontrado {
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

    private void verifVar(Token apontado) throws FimInesperadoDeArquivo, identificadorNaoEncontrado {
        switch (currentToken().getLexema()) {
            case ",":
                consumeToken();
                Token simbolo = varId((Token) apontado);
                Simbolo s = null;
                if (simbolo != null) {
                    try {
                        s = escopoAtual.inserirSimbolo(simbolo, "variavel", simbolo.getId(), simbolo.getLexema(), apontado);
                    } catch (identificadorJaUtilizado ex) {
                        erroSemantico("" + simbolo + " Identificador já utilizado. " + escopoAtual.getSimbolo(simbolo).toString());
                    }
                }
                varExpression(apontado, s);
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
            Token apontado = continueVar();
            Token simbolo = varId(apontado);
            Simbolo s = null;
            if (simbolo != null) {
                try {
                    s = escopoAtual.inserirSimbolo(simbolo, "variavel", simbolo.getId(), simbolo.getLexema(), apontado);
                } catch (identificadorJaUtilizado ex) {
                    erroSemantico("" + simbolo + " Identificador já utilizado. " + escopoAtual.getSimbolo(simbolo).toString());
                }
            }
            varExpression(apontado, s);
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
        String struct = "";
        if (currentToken().getLexema().equals("struct")) {
            struct = "struct ";
            consumeToken();
        }
        return dataType(struct);
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
            Simbolo s = null;
            try {
                s = escopoAtual.inserirSimbolo(simbolo, "constante", simbolo.getId(), simbolo.getLexema(), tipo);
            } catch (identificadorJaUtilizado ex) {
                erroSemantico("" + simbolo + " Identificador já utilizado. " + escopoAtual.getSimbolo(simbolo).toString());
            }
            consumeToken();
            constExpression(tipo, s);
        } else {
            error("ESPERAVA: IDE", true);
        }
    }

    private void constExpression(Token tipo, Simbolo s) throws FimInesperadoDeArquivo, identificadorNaoEncontrado {
        switch (currentToken().getLexema()) {
            case "=":
                consumeToken();
                try {
                    confereTipo(tipo, value());
                } catch (ExpressaoInvalidaException ex) {
                    erroSemantico(String.format("%04d", ex.getErro().getLinha()) + " " + tipo.getId() + " " + tipo.getLexema() + " Expressão inválida ");
                }
                verifConst(tipo);
                break;
            case "[":
                consumeToken();
                vectMatIndex();
                if (currentToken().getLexema().equals("]")) {
                    consumeToken();
                    estruturaConst(tipo, s);
                } else {
                    error("ESPERAVA: ']'", true);
                }
                break;
            default:
                error("ESPERAVA: '=', '['", true);
                break;
        }
    }

    private void estruturaConst(Token tipo, Simbolo s) throws FimInesperadoDeArquivo, identificadorNaoEncontrado {
        switch (currentToken().getLexema()) {
            case "=":
                if (s != null) {
                    s.setVetorStruct("vetor");
                }
                consumeToken();
                if (currentToken().getLexema().equals("[")) {
                    consumeToken();
                    try {
                        confereTipo(tipo, value());
                    } catch (ExpressaoInvalidaException ex) {
                        erroSemantico(String.format("%04d", ex.getErro().getLinha()) + " " + tipo.getId() + " " + tipo.getLexema() + " Expressão inválida ");
                    }
                    proxConstVetor(tipo);
                } else {
                    error("ESPERAVA: '['", true);
                }
                break;
            case "[":
                if (s != null) {
                    s.setVetorStruct("matriz");
                }
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
                try {
                    confereTipo(tipo, value());
                } catch (ExpressaoInvalidaException ex) {
                    erroSemantico(String.format("%04d", ex.getErro().getLinha()) + " " + tipo.getId() + " " + tipo.getLexema() + " Expressão inválida ");
                }
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
            try {
                confereTipo(tipo, value());
            } catch (ExpressaoInvalidaException ex) {
                erroSemantico(String.format("%04d", ex.getErro().getLinha()) + " " + tipo.getId() + " " + tipo.getLexema() + " Expressão inválida ");
            }
            proxConstMatriz(tipo);
        } else {
            error("ESPERAVA: '['", true);
        }
    }

    private void proxConstMatriz(Token tipo) throws FimInesperadoDeArquivo, identificadorNaoEncontrado {
        switch (currentToken().getLexema()) {
            case ",":
                consumeToken();
                try {
                    confereTipo(tipo, value());
                } catch (ExpressaoInvalidaException ex) {
                    erroSemantico(String.format("%04d", ex.getErro().getLinha()) + " " + tipo.getId() + " " + tipo.getLexema() + " Expressão inválida ");
                }
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
        ESCOPOS.add(escopoAtual);
        String struct = "";
        if (currentToken().getLexema().equals("struct")) {
            struct = "struct ";
            consumeToken();
        }
        Token apontado = dataType(struct);
        if (currentToken().getId().equals("IDE")) {
            Token simbolo = currentToken();
            consumeToken();
            if (currentToken().getLexema().equals("(")) {
                LinkedList<Token> lista = continueFunction(apontado);
                try {
                    global.inserirSimbolo(simbolo, "funcao", simbolo.getId(), simbolo.getLexema(), new Pair<>(apontado, lista));
                } catch (identificadorJaUtilizado ex) {
                    //erroSemantico("" + simbolo + " Identificador já utilizado. "+ escopoAtual.getSimbolo(simbolo).toString());
                    LinkedList<Simbolo> concorrentes = global.getSimbolo(simbolo);
                    boolean semErro = true;
                    for (Simbolo concorrente : concorrentes) {
                        if (concorrente.getCategoria().equals("procedure") || concorrente.getCategoria().equals("funcao")) {
                            LinkedList<Token> apontadoConcorrente;
                            if (concorrente.getVariavel() instanceof LinkedList) {
                                apontadoConcorrente = (LinkedList<Token>) concorrente.getVariavel();
                            } else if (concorrente.getVariavel() instanceof Pair) {
                                apontadoConcorrente = (LinkedList<Token>) ((Pair) concorrente.getVariavel()).getValue();
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

    private LinkedList<Token> continueFunction(Token tipo) throws FimInesperadoDeArquivo, identificadorNaoEncontrado {
        LinkedList<Token> ret = new LinkedList();
        consumeToken();
        if (currentToken().getLexema().equals(")")) {
            consumeToken();
            blockFunction(tipo);
        } else if (currentToken().getId().equals("IDE") || FIRSTTYPES.contains(currentToken().getLexema())) {
            parameters(ret, new LinkedList());
            blockFunction(tipo);
        } else {
            error("ESPERAVA: IDE, ')', \"int\" \"real\" \"boolean\"ou \"string\"", true);
        }
        return ret;
    }

    private void blockFunction(Token tipo) throws FimInesperadoDeArquivo, identificadorNaoEncontrado {
        if (currentToken().getLexema().equals("{")) {
            consumeToken();
            blockFuncContent(tipo);
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

    private void blockFuncContent(Token tipo) throws FimInesperadoDeArquivo, identificadorNaoEncontrado {
        switch (currentToken().getLexema()) {
            case "var":
                consumeToken();
                varDeclaration();
                content1(tipo);
                break;
            case "const":
                constDeclaration();
                content2(tipo);
                break;
            default:
                codigo();
                if (currentToken().getLexema().equals("return")) {
                    consumeToken();
                    try {
                        confereTipo(tipo, value());
                    } catch (ExpressaoInvalidaException ex) {
                        erroSemantico(String.format("%04d", ex.getErro().getLinha()) + " " + tipo.getId() + " " + tipo.getLexema() + " Expressão inválida ");
                    }
                } else {
                    error("ESPERAVA: \"return\"", true);
                }
                break;
        }

    }

    private void content1(Token tipo) throws FimInesperadoDeArquivo, identificadorNaoEncontrado {
        if (currentToken().getLexema().equals("const")) {
            constDeclaration();
            content3(tipo);
        } else {
            codigo();
            if (currentToken().getLexema().equals("return")) {
                consumeToken();
                try {
                    confereTipo(tipo, value());
                } catch (ExpressaoInvalidaException ex) {
                    erroSemantico(String.format("%04d", ex.getErro().getLinha()) + " " + tipo.getId() + " " + tipo.getLexema() + " Expressão inválida ");
                }
            } else {
                error("ESPERAVA: \"return\"", true);
            }
        }
    }

    private void content2(Token tipo) throws FimInesperadoDeArquivo, identificadorNaoEncontrado {
        if (currentToken().getLexema().equals("var")) {
            consumeToken();
            varDeclaration();
            content3(tipo);
        } else {
            codigo();
            if (currentToken().getLexema().equals("return")) {
                consumeToken();
                try {
                    confereTipo(tipo, value());
                } catch (ExpressaoInvalidaException ex) {
                    erroSemantico(String.format("%04d", ex.getErro().getLinha()) + " " + tipo.getId() + " " + tipo.getLexema() + " Expressão inválida ");
                }
            } else {
                error("ESPERAVA: \"return\"", true);
            }
        }
    }

    private void content3(Token tipo) throws FimInesperadoDeArquivo, identificadorNaoEncontrado {
        codigo();
        if (currentToken().getLexema().equals("return")) {
            consumeToken();
            try {
                confereTipo(tipo, value());
            } catch (ExpressaoInvalidaException ex) {
                erroSemantico(String.format("%04d", ex.getErro().getLinha()) + " " + tipo.getId() + " " + tipo.getLexema() + " Expressão inválida ");
            }
        } else {
            error("ESPERAVA: \"return\"", true);
        }
    }

    private void parameters(LinkedList<Token> lista, LinkedList<Token> lista2) throws FimInesperadoDeArquivo {
        String struct = "";
        if (currentToken().getLexema().equals("struct")) {
            struct = "struct ";
            consumeToken();
        }
        Token tipo = dataType(struct);
        if (tipo != null) {
            lista.add(tipo);
        }
        if (currentToken().getId().equals("IDE")) {
            paramLoop(lista, lista2);
        } else {
            error("ESPERAVA: IDE", true);
        }
    }

    private void paramLoop(LinkedList<Token> lista, LinkedList<Token> lista2) throws FimInesperadoDeArquivo {
        lista2.add(currentToken());
        consumeToken();
        switch (currentToken().getLexema()) {
            case ",":
                consumeToken();
                parameters(lista, lista2);
                break;
            case ")":
                addParametrosEscopo(lista, lista2);
                consumeToken();
                break;
            default:
                error("ESPERAVA: ',' ou ')'", true);
                break;
        }
    }

    private void addParametrosEscopo(LinkedList<Token> lista, LinkedList<Token> lista2) {
        Iterator i1 = lista.iterator();
        Iterator i2 = lista2.iterator();
        while (i1.hasNext()) {
            Token tipo = (Token) i1.next();
            Token simbolo = (Token) i2.next();
            try {
                escopoAtual.inserirSimbolo(simbolo, "variavel", simbolo.getId(), simbolo.getLexema(), tipo);
            } catch (identificadorJaUtilizado ex) {
                erroSemantico("" + simbolo + " Identificador já utilizado. ");
            }
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
        Token simbolo = new Token(currentToken().getId(), "struct " + currentToken().getLexema(), currentToken().getLinha());
        Token temp = currentToken();
        consumeToken();
        Simbolo s = null;
        TabelaSimbolos escopoStruct = new TabelaSimbolos(null);
        switch (currentToken().getLexema()) {
            case "{":
                try {
                    s = escopoAtual.inserirSimbolo(simbolo, "struct", simbolo.getId(), simbolo.getLexema(), null);
                } catch (identificadorJaUtilizado ex) {
                    erroSemantico("" + simbolo + " Identificador já utilizado. " + escopoAtual.getSimbolo(simbolo).toString());
                }
                consumeToken();
                blockVarStruct(escopoStruct);
                break;
            case "extends":
                consumeToken();
                String struct = "";
                if (currentToken().getLexema().equals("struct")) {
                    struct = "struct ";
                    consumeToken();
                }
                if (currentToken().getId().equals("IDE")) {
                    Token apontado = new Token(currentToken().getId(), struct + currentToken().getLexema(), currentToken().getLinha());
                    try {
                        //tipoPrimitivo(apontado
                        Simbolo escopoPai = escopoAtual.getTipo(apontado);
                        escopoStruct.setEscopoPai(escopoPai.getEscopo());
                        s = escopoAtual.inserirSimbolo(simbolo, "struct", simbolo.getId(), simbolo.getLexema(), escopoPai.getToken());
                    } catch (identificadorJaUtilizado ex) {
                        erroSemantico("" + simbolo + " Identificador já utilizado. " + escopoAtual.getSimbolo(simbolo).toString());
                    } catch (identificadorNaoEncontrado ex) {
                        erroSemantico("" + apontado + " O identificador não corresponde a uma Struct pai já declarada.");
                    }
                    consumeToken();
                    if (currentToken().getLexema().equals("{")) {
                        consumeToken();
                        blockVarStruct(escopoStruct);
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
        if (s != null) {
            s.setEscopo(escopoStruct);
        }

    }

    private void blockVarStruct(TabelaSimbolos escopoStruct) throws FimInesperadoDeArquivo, identificadorNaoEncontrado {
        if (currentToken().getLexema().equals("var")) {
            consumeToken();
            if (currentToken().getLexema().equals("{")) {
                consumeToken();
                firstStructVar(escopoStruct);
            } else {
                error("ESPERAVA: '{'", true);
            }
        } else {
            error("ESPERAVA: \"var\"", true);
        }
    }

    private void firstStructVar(TabelaSimbolos escopoStruct) throws FimInesperadoDeArquivo, identificadorNaoEncontrado {
        String struct = "";
        if (currentToken().getLexema().equals("struct")) {
            struct = "struct ";
            consumeToken();
        }
        Token tipo = dataType(struct);
        structVarId(escopoStruct, tipo);
    }

    private void structVarId(TabelaSimbolos escopoStruct, Token tipo) throws FimInesperadoDeArquivo, identificadorNaoEncontrado {
        if (currentToken().getId().equals("IDE")) {
            Token simbolo = currentToken();
            consumeToken();
            Simbolo s = null;
            try {
                s = escopoStruct.inserirSimboloGlobal(simbolo, "variavel", simbolo.getId(), simbolo.getLexema(), tipo);
            } catch (identificadorJaUtilizado ex) {
                System.out.println("FOI AQUI" + currentToken() + " " + (escopoStruct.contains(simbolo)) + "  ");
                erroSemantico("" + simbolo + " Identificador já utilizado na struct atual ou na struct pai. " + escopoStruct.getSimbolo(simbolo).toString());
                System.out.println("DEPOIS" + currentToken());
            }
            structVarExp(escopoStruct, tipo, s);
        } else {
            error("ESPERAVA: IDE", true);
        }
    }

    private void structVarExp(TabelaSimbolos escopoStruct, Token tipo, Simbolo s) throws FimInesperadoDeArquivo, identificadorNaoEncontrado {
        switch (currentToken().getLexema()) {
            case ",":
                consumeToken();
                structVarId(escopoStruct, tipo);
                break;
            case ";":
                consumeToken();
                proxStructVar(escopoStruct);
                break;
            case "[":
                consumeToken();
                vectMatIndex();
                if (currentToken().getLexema().equals("]")) {
                    consumeToken();
                    structMatriz(escopoStruct, tipo, s);
                } else {
                    error("ESPERAVA: ]", true);
                }
                break;
            default:
                error("ESPERAVA: ',', ';' ou '['", true);
                break;
        }
    }

    private void proxStructVar(TabelaSimbolos escopoStruct) throws FimInesperadoDeArquivo, identificadorNaoEncontrado {
        if (currentToken().getLexema().equals("}")) {
            consumeToken();
            if (currentToken().getLexema().equals("}")) {
                consumeToken();
            } else {
                error("ESPERAVA: '}'", true);
            }
        } else {
            String struct = "";
            if (currentToken().getLexema().equals("struct")) {
                struct = "struct ";
                consumeToken();
            }
            Token tipo = dataType(struct);
            structVarId(escopoStruct, tipo);
        }
    }

    private void structMatriz(TabelaSimbolos escopoStruct, Token tipo, Simbolo s) throws FimInesperadoDeArquivo, identificadorNaoEncontrado {
        switch (currentToken().getLexema()) {
            case "[":
                s.setVetorStruct("matriz");
                consumeToken();
                vectMatIndex();
                if (currentToken().getLexema().equals("]")) {
                    consumeToken();
                    contStructMatriz(escopoStruct, tipo);
                } else {
                    error("ESPERAVA: ']'", true);
                }
                break;
            case ",":
                s.setVetorStruct("vetor");
                consumeToken();
                structVarId(escopoStruct, tipo);
                break;
            case ";":
                s.setVetorStruct("vetor");
                consumeToken();
                proxStructVar(escopoStruct);
                break;
            default:
                s.setVetorStruct("vetor");
                error("ESPERAVA: '[', ',' ou ';'", true);
                break;
        }
    }

    private void contStructMatriz(TabelaSimbolos escopoStruct, Token tipo) throws FimInesperadoDeArquivo, identificadorNaoEncontrado {
        switch (currentToken().getLexema()) {
            case ",":
                consumeToken();
                structVarId(escopoStruct, tipo);
                break;
            case ";":
                consumeToken();
                proxStructVar(escopoStruct);
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
        escopoAtual = new TabelaSimbolos(global);
        ESCOPOS.add(escopoAtual);
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
        ESCOPOS.add(escopoAtual);
        if (currentToken().getId().equals("IDE")) {
            Token simbolo = currentToken();

            consumeToken();
            if (currentToken().getLexema().equals("(")) {
                LinkedList<Token> apontado = procedureParams(simbolo.getLexema());
                try {
                    global.inserirSimbolo(simbolo, "procedure", simbolo.getId(), simbolo.getLexema(), apontado);
                } catch (identificadorJaUtilizado ex) {
                    //erroSemantico("" + simbolo + " Identificador já utilizado. "+ escopoAtual.getSimbolo(simbolo).toString());
                    LinkedList<Simbolo> concorrentes = global.getSimbolo(simbolo);
                    boolean semErro = true;
                    for (Simbolo concorrente : concorrentes) {
                        if (concorrente.getCategoria().equals("procedure") || concorrente.getCategoria().equals("funcao")) {
                            LinkedList<Token> apontadoConcorrente;
                            if (concorrente.getVariavel() instanceof LinkedList) {
                                apontadoConcorrente = (LinkedList<Token>) concorrente.getVariavel();
                            } else if (concorrente.getVariavel() instanceof Pair) {
                                apontadoConcorrente = (LinkedList<Token>) ((Pair) concorrente.getVariavel()).getValue();
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

    private LinkedList<Token> procedureParams(String procedureName) throws FimInesperadoDeArquivo {
        LinkedList<Token> ret = new LinkedList();
        consumeToken();
        if (currentToken().getLexema().equals(")")) {
            consumeToken();
        } else if (currentToken().getId().equals("IDE") || FIRSTTYPES.contains(currentToken().getLexema())) {
            parameters(ret, new LinkedList());
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
        if (FIRSTCOMANDO.contains(currentToken().getLexema()) || currentToken().getId().equals("IDE") || currentToken().getLexema().equals("global")
                || currentToken().getLexema().equals("local")) { //IDE para increment e decrement, functioncall e atribuição
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
        } else if (currentToken().getId().equals("IDE") || currentToken().getLexema().equals("global")
                || currentToken().getLexema().equals("local")) {
            Token t = lookahead();
            if (t.getLexema().equals("(")) {
                functionCall(null);
                if (currentToken().getLexema().equals(";")) {
                    consumeToken();
                } else {
                    error("ESPERAVA: ','", true);
                }
            } else { // casos de incrementop, decrementop e atribuição (first é variável nos 3 casos)
                Token atual = currentToken();

                Token tk = null;
                try {
                    tk = variavel();

                } catch (VariavelInvalidaException ex) {
                    erroSemantico(atual + " Variável inválida: " + ex.getMensagem());
                }
                //atribuição
                switch (currentToken().getLexema()) {
                    case "=":
                        consumeToken();
                        try {
                            Token v = value();
                            if (tk != null) {
                                confereTipo(v, tk);
                                if (tk.getFlag() != null && !tk.getFlag().equals("variavel")) {
                                    erroSemantico(tk + " Atribuição não pode ser realizada, pois o identificador não indica uma variável.");
                                }
                            }
                        } catch (ExpressaoInvalidaException ex) {
                            erroSemantico(String.format("%04d", ex.getErro().getLinha()) + " Expressão inválida ");
                        }
                        if (currentToken().getLexema().equals(";")) {
                            consumeToken();
                        } else {
                            error("ESPERAVA: ';'", true);
                        }
                        break;
                    case "++":
                    case "--":
                        try {
                            LinkedList<Simbolo> simb = escopoAtual.getSimbolo(atual);
                            Simbolo s = simb.get(0);

                            if (tk != null && tk.getId() != null) {
                                if (!(confereTipo(tk, new Token("PRE", "real", currentToken().getLinha()), false) || confereTipo(tk, new Token("PRE", "int", currentToken().getLinha()), false))) {
                                    erroSemantico(atual + " Tipo de váriavel não permite incremento ou decremento: " + tk.getLexema());
                                }
                            }

                            if (!s.getCategoria().equals("variavel")) {
                                erroSemantico(s + " Não é variável");
                            }

                        } catch (identificadorNaoEncontrado e) {
                            erroSemantico(atual + " Variável não declarada");
                        }
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
            Token atual = currentToken();

            Token tk = null;
            try {
                tk = variavel();

            } catch (VariavelInvalidaException ex) {
                erroSemantico(atual + " Variável inválida: " + ex.getMensagem());
            }

            try {
                LinkedList<Simbolo> simb = escopoAtual.getSimbolo(atual);
                Simbolo s = simb.get(0);
                if (!s.getCategoria().equals("variavel")) {
                    erroSemantico(s + " Não é variável");
                }
                if (tk != null) {
                    try {
                        Token token = tipoPrimitivo(escopoAtual.getTipo(tk).getToken()); //ainda nao tem tipos primitivos
                        if (!(token.getLexema().equals("real") || token.getLexema().equals("int"))) {
                            erroSemantico(atual + " Tipo de váriavel não permite incremento ou decremento: " + token.getLexema());
                        }
                    } catch (identificadorNaoEncontrado e) {
                        erroSemantico(atual + " Tipo de váriavel não declarado");
                    }
                }
            } catch (identificadorNaoEncontrado e) {
                erroSemantico(atual + " Variável não declarada");
            }
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
        Token atual = currentToken();
        try {
            value();
        } catch (ExpressaoInvalidaException ex) {
            erroSemantico(String.format("%04d", ex.getErro().getLinha()) + " " + atual.getId() + " " + atual.getLexema() + " Expressão inválida ");
        }
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
        Token atual = currentToken();
        try {
            variavel();
        } catch (VariavelInvalidaException ex) {
            erroSemantico(atual + " Variável inválida: " + ex.getMensagem());
        }
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
            Token atual = currentToken();
            Token tipoRet = boolOperation(null);
            if (tipoRet != null && tipoRet.getId() != null) {
                confereTipo(new Token("PRE", "boolean", currentToken().getLinha()), tipoRet);
            } else {
                erroSemantico(atual + " valor passado não é booleano");
            }
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
            Token atual = currentToken();
            Token tipoRet = boolOperation(null);
            if (tipoRet != null && tipoRet.getId() != null) {
                confereTipo(new Token("PRE", "boolean", currentToken().getLinha()), tipoRet);
            } else {
                erroSemantico(atual + " valor passado não é booleano");
            }
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
                Token apontado = new Token(currentToken().getId(), "struct " + currentToken().getLexema(), currentToken().getLinha());
                consumeToken();
                if (currentToken().getId().equals("IDE")) {
                    Token simbolo = new Token(currentToken().getId(), currentToken().getLexema(), currentToken().getLinha());
                    consumeToken();
                    if (currentToken().getLexema().equals(";")) {
                        try {
                            Simbolo s = escopoAtual.inserirSimbolo(simbolo, "struct", simbolo.getId(), simbolo.getLexema(), escopoAtual.getTipo(apontado).getToken());
                            s.setEscopo(escopoAtual.getTipo(apontado).getEscopo());
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

            Token apontado = dataType("");
            if (currentToken().getId().equals("IDE")) {
                Token simbolo = currentToken();
                consumeToken();
                if (currentToken().getLexema().equals(";")) {
                    try {
                        escopoAtual.inserirSimbolo(simbolo, "tipo", simbolo.getId(), simbolo.getLexema(), apontado);
                    } catch (identificadorJaUtilizado ex) {
                        erroSemantico("" + simbolo + " Identificador já utilizado. " + escopoAtual.getTipo(simbolo));
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
    private Token opNegate(Token tipo) throws FimInesperadoDeArquivo {
        if (currentToken().getId().equals("NRO")) { //removi cadeia 
            if (tipo.getId() != null) {
                confereTipo(tipo, currentToken());
            } else {
                tipo.setId(currentToken().getId());
                tipo.setLexema(currentToken().getLexema());
                tipo.setLinha(currentToken().getLinha());
            }
            consumeToken();
            return new Token("PRE", "int", currentToken().getLinha());
        } else if (currentToken().getId().equals("NRO REAL")) { //removi cadeia 
            if (tipo.getId() != null) {
                confereTipo(tipo, currentToken());
            } else {
                tipo.setId(currentToken().getId());
                tipo.setLexema(currentToken().getLexema());
                tipo.setLinha(currentToken().getLinha());
            }
            consumeToken();
            return new Token("PRE", "real", currentToken().getLinha());
        }
        switch (currentToken().getLexema()) {
            case "-":
                consumeToken();
                Token atual = currentToken();
                if (currentToken().getId().equals("IDE")) {

                    if (lookahead().getLexema().equals("(")) {
                        Token tipo2 = new Token(null, null, currentToken().getLinha());
                        Token t = functionCall(tipo2);
                        if (tipo2.getId() != null && tipo.getId() == null) {
                            tipo.setId(tipo2.getId());
                            tipo.setLexema(tipo2.getLexema());
                            tipo.setLinha(currentToken().getLinha());
                        } else if (tipo2.getId() != null) {
                            confereTipo(tipo, tipo2);
                        }
                        if (tipo.getId() == null) {
                            tipo.setId(t.getId());
                            tipo.setLexema(t.getLexema());
                            tipo.setLinha(currentToken().getLinha());
                        } else {
                            if (tipo.getId() != null) {
                                confereTipo(tipo, t);
                            }
                        }

                    } else {
                        Token t = null;
                        try {
                            t = variavel();

                        } catch (VariavelInvalidaException ex) {
                            erroSemantico(atual + " Variável inválida: " + ex.getMensagem());
                        }
                        if (t != null) {
                            if (tipo.getId() == null) {
                                tipo.setId(t.getId());
                                tipo.setLexema(t.getLexema());
                                tipo.setLinha(currentToken().getLinha());
                            } else {
                                confereTipo(tipo, t);
                            }
                        }
                    }
                } else {
                    Token t = aritmeticValue();
                    if (tipo.getId() == null) {
                        tipo.setId(t.getId());
                        tipo.setLexema(t.getLexema());
                        tipo.setLinha(currentToken().getLinha());
                    } else {
                        if (tipo.getId() != null) {
                            confereTipo(tipo, t);
                        }
                    }
                }
                break;
            case "--":
            case "++":
                consumeToken();
                Token t = null;
                Token atual1 = currentToken();
                try {
                    t = variavel();
                    if (!(confereTipo(t, new Token("PRE", "real", currentToken().getLinha()), false) || confereTipo(t, new Token("PRE", "int", currentToken().getLinha()), false))) {
                        erroSemantico(atual1 + " o tipo da variável não permite incremento ou decremento");
                    }
                } catch (VariavelInvalidaException ex) {
                    erroSemantico(atual1 + " Variável inválida: " + ex.getMensagem());
                }
                if (t != null) {
                    if (tipo.getId() == null) {
                        tipo.setId(t.getId());
                        tipo.setLexema(t.getLexema());
                        tipo.setLinha(currentToken().getLinha());
                    } else {
                        confereTipo(tipo, t);
                    }
                }
            default:
                if (currentToken().getId().equals("IDE") && lookahead().getLexema().equals("(")) {
                    Token tipo2 = new Token(null, null, currentToken().getLinha());
                    Token ret = functionCall(tipo2);
                    if (tipo2.getId() != null && tipo.getId() == null) {
                        tipo.setId(tipo2.getId());
                        tipo.setLexema(tipo2.getLexema());
                        tipo.setLinha(currentToken().getLinha());
                    } else if (tipo2.getId() != null) {
                        confereTipo(tipo, tipo2);
                    }
                } else {
                    Token atual2 = currentToken();
                    Token ret = null;
                    Token temp = null;
                    try {
                        temp = variavel();
                        ret = new Token(temp.getId(), temp.getLexema(), temp.getLinha());
                    } catch (VariavelInvalidaException ex) {
                        erroSemantico(atual2 + " Variável inválida: " + ex.getMensagem());
                    }
                    if (currentToken().getLexema().equals("++") || currentToken().getLexema().equals("--")) {
                        if (temp != null && temp.getId() != null) {
                            if (!(confereTipo(temp, new Token("PRE", "real", currentToken().getLinha()), false) || confereTipo(temp, new Token("PRE", "int", currentToken().getLinha()), false))) {
                                erroSemantico(atual2 + " o tipo da variável não permite incremento ou decremento");
                            }
                            confereTipo(tipo, temp);
                        }
                        consumeToken();
                    }
                    if (ret != null) {
                        tipo.setId(ret.getId());
                        tipo.setLexema(ret.getLexema());
                    }
                }
        }
        return tipo; //int como default
    }

    //Operação aritmética
    private Token aritmeticOp(Token tipo) throws FimInesperadoDeArquivo, ExpressaoInvalidaException {
        Token ret = new Token(null, null, currentToken().getLinha());
        if (tipo == null) {
            tipo = new Token(null, null, currentToken().getLinha());
        }
        if (currentToken().getLexema().equals("-") && lookahead().getLexema().equals("(")) {
            consumeToken();
        }
        if (currentToken().getLexema().equals("(")) {
            consumeToken();
            aritmeticOp(tipo);
            if (currentToken().getLexema().equals(")")) {
                consumeToken();
                if (currentToken().getLexema().equals("*") || currentToken().getLexema().equals("/")) {
                    if (currentToken().getLexema().equals("/") && (confereTipo(tipo, new Token("NRO", "0", currentToken().getLinha())))) {
                        erroSemantico(currentToken() + " Divisão não é permitida com tipos inteiros.");
                    }
                    consumeToken();
                    aritmeticOp(tipo);
                }
            } else {
                error("ESPERAVA: '*' ou '/'", true);
            }
        } else {
            ret = opNegate(tipo);
            if (currentToken().getLexema().equals("*") || currentToken().getLexema().equals("/")) {
                if (currentToken().getLexema().equals("/") && !(confereTipo(tipo, new Token("NRO REAL", "0.0", currentToken().getLinha()), false))) {
                    erroSemantico(currentToken() + " Divisão é permitida apenas com valores reais.");
                }
                consumeToken();
                aritmeticOp(tipo);
            }
        }
        if (currentToken().getLexema().equals("+") || currentToken().getLexema().equals("-")) {
            consumeToken();
            aritmeticOp(tipo);
        }
        if (tipo.getId() == null) {
            throw new ExpressaoInvalidaException(ret);
        }
        return ret;
    }

    //Operação relacional
    private void logicOrRelacionalOp(Token tipo) throws FimInesperadoDeArquivo {
        if (tipo == null) {
            tipo = new Token(null, null, currentToken().getLinha());
        }
        switch (currentToken().getId()) {
            case "REL":
            case "LOG":
                switch (currentToken().getLexema()) {
                    case "&&":
                    case "!=":
                    case "||":
                    case "==":
                        tipo.setId(null);
                        tipo.setLexema(null);
                        break;
                }
                consumeToken();
                contRelLogic(tipo);
                break;
            default:
                error("ESPERAVA: REL ou LOG", true);
        }
    }

    //Verifica os possíveis valores de uma operação relacional ou lógica.
    private void contRelLogic(Token tipo) throws FimInesperadoDeArquivo {
        Token atual = currentToken();
        switch (currentToken().getLexema()) {
            case "true":
            case "false":
                Token t = (Token) lookahead();
                if (t.getId().equals("REL") || t.getId().equals("LOG")) {
                    consumeToken();
                    consumeToken();
                    try {
                        operation(tipo);
                    } catch (ExpressaoInvalidaException ex) {
                        erroSemantico(String.format("%04d", ex.getErro().getLinha()) + " " + atual.getId() + " " + atual.getLexema() + " Expressão inválida ");
                    }
                } else {
                    consumeToken();
                }
                break;
            default: {
                try {
                    operation(tipo);
                } catch (ExpressaoInvalidaException ex) {
                    erroSemantico(String.format("%04d", ex.getErro().getLinha()) + " " + atual.getId() + " " + atual.getLexema() + " Expressão inválida ");
                }
            }
            break;
        }
    }

    //Operação de negação.
    private void negBoolValue(Token tipo) throws FimInesperadoDeArquivo {
        if (currentToken().getLexema().equals("!")) {
            consumeToken();
            Token atual = currentToken();
            try {
                Token tipoRet = variavel();
                tipo.setId(tipoRet.getId());
                tipo.setLexema(tipoRet.getLexema());
                tipo.setLinha(tipoRet.getLinha());
            } catch (VariavelInvalidaException ex) {
                erroSemantico(atual + " Variável inválida: " + ex.getMensagem());
            }
        } else {
            error("ESPERAVA: '!'", true);
        }
    }

    //Produções de valor booleano que não possuem operações aritméticas no first.
    private void boolOnlyOp(Token tipo) throws FimInesperadoDeArquivo {
        if (tipo == null) {
            tipo = new Token(null, null, currentToken().getLinha());
        }
        switch (currentToken().getLexema()) {
            case "true":
            case "false":
                consumeToken();
                logicOrRelacionalOp(tipo);
                break;
            case "!":
                negBoolValue(tipo);
                break;
            case "(":
                consumeToken();
                if (currentToken().getId().equals("NRO") || currentToken().getId().equals("CAD")
                        || currentToken().getId().equals("IDE") || currentToken().getLexema().equals("-")
                        || currentToken().getLexema().equals("global")
                        || currentToken().getLexema().equals("local")) {
                    Token atual = currentToken();
                    try {
                        aritmeticOp(tipo);
                    } catch (ExpressaoInvalidaException ex) {
                        erroSemantico(String.format("%04d", ex.getErro().getLinha()) + " " + atual.getId() + " " + atual.getLexema() + " Expressão inválida ");
                    }
                    logicOrRelacionalOp(tipo);
                    if (currentToken().getLexema().equals(")")) {
                        consumeToken();
                        logicOrRelacionalOp(tipo);
                    } else {
                        logicOrRelacionalOp(tipo);
                        if (currentToken().getLexema().equals(")")) {
                            consumeToken();
                            if (currentToken().getId().equals("REL") || currentToken().getId().equals("LOG")) {
                                logicOrRelacionalOp(tipo);
                            }
                        } else {
                            error("ESPERAVA: ')'", true);
                        }
                    }
                } else {
                    boolOnlyOp(tipo);
                    if (currentToken().getLexema().equals(")")) {
                        consumeToken();
                        if (currentToken().getId().equals("REL") || currentToken().getId().equals("LOG")) {
                            //consumeToken();
                            logicOrRelacionalOp(tipo);
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
    private Token boolOperation(Token tipo) throws FimInesperadoDeArquivo {
        if (tipo == null) {
            tipo = new Token(null, null, currentToken().getLinha());
        }
        if (currentToken().getId().equals("NRO") || currentToken().getId().equals("CAD")
                || currentToken().getId().equals("IDE") || currentToken().getLexema().equals("-")) {
            Token atual = currentToken();
            try {
                aritmeticOp(tipo);
            } catch (ExpressaoInvalidaException ex) {
                erroSemantico(String.format("%04d", ex.getErro().getLinha()) + " " + atual.getId() + " " + atual.getLexema() + " Expressão inválida ");
            }
            if (!atual.getId().equals("IDE")) {
                logicOrRelacionalOp(tipo);
            }
        } else {
            boolOnlyOp(tipo);
        }
        return tipo;
    }

    private Token operation(Token tipo) throws FimInesperadoDeArquivo, ExpressaoInvalidaException {
        Token ret = new Token(null, null, currentToken().getLinha());
        if (tipo == null) {
            tipo = new Token(null, null, currentToken().getLinha());
        }
        if (currentToken().getId().equals("NRO") || currentToken().getId().equals("NRO REAL") || currentToken().getId().equals("CAD")
                || currentToken().getId().equals("IDE") || currentToken().getLexema().equals("-") || currentToken().getLexema().equals("global")
                || currentToken().getLexema().equals("local")) {
            Token atual = currentToken();
            try {
                Token tipoRecebido = aritmeticOp(tipo);
                ret.setId(tipoRecebido.getId());
                ret.setLexema(tipoRecebido.getLexema());
            } catch (ExpressaoInvalidaException ex) {
                erroSemantico(String.format("%04d", ex.getErro().getLinha()) + " " + atual.getId() + " " + atual.getLexema() + " Expressão inválida ");
            }

            if (currentToken().getId().equals("REL") || currentToken().getId().equals("LOG")) {
                logicOrRelacionalOp(tipo);
                return new Token("PRE", "boolean", currentToken().getLinha());
            }
        } else {
            switch (currentToken().getLexema()) {
                case "true":
                case "false":
                    consumeToken();
                    logicOrRelacionalOp(tipo);
                    break;
                case "!":
                    negBoolValue(tipo);
                    break;
                case "(":
                    consumeToken();
                    if (currentToken().getId().equals("NRO") || currentToken().getId().equals("NRO REAL") || currentToken().getId().equals("CAD")
                            || currentToken().getId().equals("IDE") || currentToken().getLexema().equals("-") || currentToken().getLexema().equals("global")
                            || currentToken().getLexema().equals("local")) {

                        try {
                            Token tipoRecebico = aritmeticOp(tipo);

                            ret.setId(tipoRecebico.getId());
                            ret.setLexema(tipoRecebico.getLexema());
                            if (currentToken().getId().equals("REL") || currentToken().getId().equals("LOG")) {
                                logicOrRelacionalOp(tipo);
                            }
                        } catch (ExpressaoInvalidaException e) {
                            if (currentToken().getId().equals("REL") || currentToken().getId().equals("LOG")) {
                                logicOrRelacionalOp(tipo);
                            }
                            if (currentToken().getLexema().equals(")")) {
                                consumeToken();
                                if (currentToken().getId().equals("REL") || currentToken().getId().equals("LOG")) {
                                    logicOrRelacionalOp(tipo);
                                }
                            } else {
                                error("ESPERAVA: ')'", true);
                            }
                            throw e;
                        }

                    } else {
                        boolOnlyOp(tipo);
                        if (currentToken().getLexema().equals(")")) {
                            consumeToken();
                            if (currentToken().getId().equals("REL") || currentToken().getId().equals("LOG")) {
                                logicOrRelacionalOp(tipo);
                            } else {
                                return ret;
                            }
                        } else {
                            error("ESPERAVA: ')'", true);
                        }
                    }
                    if (currentToken().getLexema().equals(")")) {
                        consumeToken();
                        if (currentToken().getId().equals("REL") || currentToken().getId().equals("LOG")) {
                            logicOrRelacionalOp(tipo);
                        }
                    } else {
                        error("ESPERAVA: ')'", true);
                    }
                    break;
                default:
                    error("ESPERAVA: '!', '(', \"true\" ou \"false\"", true);
                    break;
            }
        }
        if (ret.getId() == null) {
            throw new ExpressaoInvalidaException(ret);
        }
        return ret;
    }
//******************************************* Operations *********************************************  
//====================================================================================================

//========================================== Function Call ===========================================  
//****************************************************************************************************
    private Token functionCall(Token tipo) throws FimInesperadoDeArquivo {
        Token ret = currentToken();
        if (tipo == null) {
            tipo = new Token(null, null, currentToken().getLinha());
        }
        LinkedList<Simbolo> lista = null;
        try {
            lista = escopoAtual.getSimbolo(ret);
        } catch (identificadorNaoEncontrado ex) {
            erroSemantico(ret + " Função não declarada. ");
        }
        consumeToken();
        if (currentToken().getLexema().equals("(")) {
            consumeToken();
            if (currentToken().getLexema().equals(")")) {
                consumeToken();
            } else {
                LinkedList<Token> tiposParametros = new LinkedList();
                fCallParams(tiposParametros);
                if (lista != null) {
                    Token retTipo = comparaFuncoes(lista, tiposParametros, ret);
                    tipo.setId(retTipo.getId());
                    tipo.setLexema(retTipo.getLexema());
                    tipo.setLinha(retTipo.getLinha());
                }
            }
        } else {
            error("ESPERAVA: '('", true);
        }
        return ret;
    }

    private void fCallParams(LinkedList<Token> tiposParametros) throws FimInesperadoDeArquivo {
        Token atual = currentToken();

        try {
            tiposParametros.add(value());
        } catch (ExpressaoInvalidaException ex) {
            erroSemantico(String.format("%04d", ex.getErro().getLinha()) + " " + atual.getId() + " " + atual.getLexema() + " Expressão inválida ");
        }
        switch (currentToken().getLexema()) {
            case ",":
                consumeToken();
                fCallParams(tiposParametros);/*
                try {
                    tiposParametros.add(value());
                } catch (ExpressaoInvalidaException ex) {
                    erroSemantico(String.format("%04d", ex.getErro().getLinha()) + " " + atual.getId() + " " + atual.getLexema() + " Expressão inválida ");
                }
                if(currentToken().getLexema().equals(","))
                    consumeToken();
                fCallParams(tiposParametros);*/
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

    private boolean equalsParams(List<Token> a, List<Token> b) {
        if (a.size() != b.size()) {
            return false;
        }
        LinkedList<Token> c = new LinkedList(b);

        Iterator ia = a.iterator();
        Iterator ib = b.iterator();
        while (ia.hasNext()) {
            if (!((Token) ia.next()).equals((Token) ib.next())) {
                return false;
            }
        }
        return true;
    }

    private boolean confereTipo(Token t1, Token t2) throws FimInesperadoDeArquivo {
        return confereTipo(t1, t2, true);
    }

    private boolean confereTipo(Token t1, Token t2, boolean flag) throws FimInesperadoDeArquivo {
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
                if (flag) {
                    erroSemantico("" + String.format("%04d", currentToken().getLinha()) + " o tipo utilizado não foi declarado. ");
                }
                return false;
            }
        } else if (t1.getId().equals("NRO REAL")) {
            lexema1 = "real";
        } else if (t1.getId().equals("NRO")) {
            lexema1 = "int";
        } else if (t1.getId().equals("CAD")) {
            lexema1 = "string";
        } else {
            lexema1 = t1.getLexema();
        }
        if (t2.getId().equals("IDE")) {
            try {
                simbolo2 = tipoPrimitivoApontado(escopoAtual.getTipo(t2));
                if (simbolo2.getCategoria().equals("tipo")) {
                    lexema2 = ((Token) simbolo2.getVariavel()).getLexema();
                    simbolo2 = null;
                }
            } catch (identificadorNaoEncontrado ex) {
                if (flag) {
                    erroSemantico("" + String.format("%04d", currentToken().getLinha()) + " o tipo utilizado não foi declarado. ");
                }
                return false;
            }
        } else if (t2.getId().equals("NRO REAL")) {
            lexema2 = "real";
        } else if (t2.getId().equals("NRO")) {
            lexema2 = "int";
        } else if (t2.getId().equals("CAD")) {
            lexema2 = "string";
        } else {
            lexema2 = t2.getLexema();
        }
        if ((lexema1 == null && lexema2 != null) || (lexema1 != null && lexema2 == null)) {
            if (flag) {
                erroSemantico("" + String.format("%04d", currentToken().getLinha()) + " Tipos Incompatíveis: " + (lexema1 == null ? simbolo1.getToken().getLexema() : lexema1) + " e " + (lexema2 == null ? simbolo2.getToken().getLexema() : lexema2));
            }
            return false;
        } else if (lexema1 != null && lexema2 != null) {
            if (lexema1.equals(lexema2)) {
                return true;
            } else {
                if (flag) {
                    erroSemantico("" + String.format("%04d", currentToken().getLinha()) + " Tipos Incompatíveis: " + lexema1 + " e " + lexema2);
                }
                return false;
            }
        }
        boolean ret = simbolo1.getToken().getLexema().equals(simbolo2.getToken().getLexema());
        if (!ret && flag) {
            erroSemantico("" + String.format("%04d", currentToken().getLinha()) + " Tipos Incompatíveis: " + simbolo1.getToken().getLexema() + " e " + simbolo2.getToken());
        }
        return ret;

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

    private Token tipoPrimitivo(Token tipo) throws identificadorNaoEncontrado {
        if (tipo == null) {
            //    throw new identificadorNaoEncontrado();
        }
        if (tipo.getId().equals("IDE")) {
            return tipoPrimitivo((Token) escopoAtual.getTipo(tipo).getVariavel());
        } else {
            return tipo;
        }
    }

    private Token comparaFuncoes(LinkedList<Simbolo> lista, LinkedList<Token> tiposParametros, Token ret) throws FimInesperadoDeArquivo {
        Token retorno = new Token(null, null, currentToken().getLinha());
        int paramsPassados = tiposParametros.hashCode();
        Iterator i1 = lista.iterator();

        boolean matchParams = false;
        while (i1.hasNext()) {
            Simbolo funcao = (Simbolo) i1.next();
            LinkedList pParams = null;
            if (funcao.getCategoria().equals("procedure")) {
                pParams = (LinkedList<Token>) funcao.getVariavel();
            } else if (funcao.getCategoria().equals("funcao")) {
                pParams = (LinkedList) ((Pair) funcao.getVariavel()).getValue();
            }
            if (pParams != null) {
                Iterator i2 = tiposParametros.iterator();
                Iterator i3 = pParams.iterator();
                boolean flag = false;
                while (i2.hasNext()) {
                    flag = false;

                    //System.out.println(""+currentToken().getLinha()+" t1: "+t1+" t2: "+t2+" result: "+(result)+" size func: "+pParams.size()+" i2.has: "+i2.hasNext()+"  lista: "+lista.size());
                    if (i3.hasNext() && confereTipo((Token) i2.next(), (Token) i3.next(), false)) {
                        if (!i3.hasNext() && !i2.hasNext()) {
                            flag = true;
                        }
                    } else {
                        break;
                    }
                }
                if (flag == true) {
                    if (funcao.getCategoria().equals("funcao")) {
                        Token retType = (Token) ((Pair) funcao.getVariavel()).getKey();
                        retorno.setId(retType.getId());
                        retorno.setLexema(retType.getLexema());
                        retorno.setLinha(retType.getLinha());
                    }
                    return retorno;
                }
            }
        }
        erroSemantico(ret + " Função não encontrada.");
        return retorno;
    }
}
