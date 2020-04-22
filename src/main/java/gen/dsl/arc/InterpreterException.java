package gen.dsl.arc;

import org.antlr.v4.runtime.Token;

public class InterpreterException extends RuntimeException
{
    public int line;
    public int offset;

    public InterpreterException(Token token, String message)
    {
        super(message);
        if(token != null)
        {
            this.line = token.getLine();
            this.offset = token.getCharPositionInLine();
        }
    }
}
