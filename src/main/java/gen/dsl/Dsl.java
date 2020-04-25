package gen.dsl;

import gen.dsl.arc.*;
import gen.grid.ColorGrid;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.atn.ATNConfigSet;
import org.antlr.v4.runtime.dfa.DFA;

import java.util.BitSet;
import java.util.Random;

public class Dsl
{
    String name;
    Visitor visitor;
    Random random;
    private ANTLRErrorListener errorListener;
    private final ArcParser parser;
    public final ArcLexer lexer;
    private CommonTokenStream tokenStream;

    public Dsl(String name, ANTLRErrorListener errorListener)
    {
        this.name = name;
        this.errorListener = errorListener;
        random = new Random();
        lexer = new ArcLexer(null);
        tokenStream = new CommonTokenStream(lexer);
        parser = new ArcParser(tokenStream);
        parser.addErrorListener(errorListener);

        visitor = new Visitor(name, parser);
    }

    public Visitor getVisitor() { return visitor; }

    public ColorGrid renderGrid(ColorGrid input, String dslCode)
    {
        CodePointCharStream charStream = CharStreams.fromString(dslCode);
        lexer.setInputStream(charStream);
        tokenStream = new CommonTokenStream(lexer);
        parser.setTokenStream(tokenStream);
        parser.reset();
//        parser.setBuildParseTree();
//        tokenStream.fill();

        ArcParser.ProgramContext program = parser.program();

        try
        {
            visitor.setInputGrid(input);
            visitor.visit(program);
        }
        catch (RecognitionException re)
        {
            Token offendor = re.getOffendingToken();
            errorListener.syntaxError(re.getRecognizer(), null, offendor.getLine(), offendor.getStartIndex(), re.getMessage(), re);

            re.printStackTrace();
        }
        catch (InterpreterException e)
        {
            errorListener.syntaxError(parser, null, e.line, e.offset, e.getMessage(), null);
            e.printStackTrace();
        }
        catch (RuntimeException e)
        {
            errorListener.syntaxError(parser, null, -1, -1, e.getMessage(), null);
            e.printStackTrace();
        }
        catch (Throwable e)
        {
            System.err.println("Throwable");
            errorListener.syntaxError(parser, null, -1, -1, e.getMessage(), null);
            e.printStackTrace();
        }

        return visitor.getBoard();
    }

    public static ANTLRErrorListener dummyListener()
    {
        return new ANTLRErrorListener()
        {
            @Override
            public void syntaxError(Recognizer<?, ?> recognizer, Object o, int i, int i1, String s,RecognitionException e)
            {
                System.err.println(String.format("[%d,%d]: ", i, i1) + s);
            }

            @Override
            public void reportAmbiguity(Parser parser, DFA dfa, int i, int i1, boolean b, BitSet bitSet,ATNConfigSet atnConfigSet)
            {
                System.err.println(String.format("[%d,%d]: ", i, i1) + "Ambiguity!");
            }

            @Override
            public void reportAttemptingFullContext(Parser parser, DFA dfa, int i, int i1, BitSet bitSet,ATNConfigSet atnConfigSet)
            {
            }

            @Override
            public void reportContextSensitivity(Parser parser, DFA dfa, int i, int i1, int i2,ATNConfigSet atnConfigSet)
            {
            }
        };
    }

    public static void main(String[] args)
    {
        String inputCode1 = "dims(3, 3)\n" +
                           "m = mask(\"110101101\", 3)\n" +
                           "brush = Orange\n" +
                           "draw(m)";

        String outputCode1 = "scale = 3\n"
                   +  "dims(scale * 3)\n"
                   +  "grids = layout(scale, scale, 3, 0)\n"
                   +  "\n"
                   +  "brush = input::brush\n"
                   +  "foreach g in grids {\n"
                   +  "    if input::m[g.arrayPos] {\n"
                   +  "        g.draw(input::m)\n"
                   +  "    }\n"
                   +  "}\n"
                   +  "draw(grids)\n"
                ;

        ANTLRErrorListener listener = dummyListener();
        ColorGrid input = new ColorGrid(null, "0111102222033330444405555", 5);

        Dsl inputInterpreter = new Dsl("[in]", listener);
        ColorGrid inputReconstruction = inputInterpreter.renderGrid(input, inputCode1);

        System.out.println(inputReconstruction);

        Dsl outputInterpreter = new Dsl("[out]", listener);
        outputInterpreter.addNamespace("input", inputInterpreter.visitor);

        System.out.println("-------------------------");
        ColorGrid grid = outputInterpreter.renderGrid(null, outputCode1);

        System.out.println(grid.toString());
    }

    public void addNamespace(String namespaceTag, Visitor visitorWithinScope)
    {
        visitor.addNamespace(namespaceTag, visitorWithinScope);
    }

    public boolean ownsRecognizer(Recognizer<?, ?> r) { return parser.equals(r); }
}
