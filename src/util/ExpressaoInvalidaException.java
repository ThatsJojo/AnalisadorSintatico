package util;

import model.Token;

public class ExpressaoInvalidaException extends Exception{
    
    private final Token erro;

    public ExpressaoInvalidaException(Token erro) {
        this.erro = erro;
    }

    public Token getErro() {
        return erro;
    }
    
    
    
}
 