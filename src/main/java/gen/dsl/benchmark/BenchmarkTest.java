package gen.dsl.benchmark;

import gen.dsl.arc.SimpleLexer;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CodePointCharStream;
import org.antlr.v4.runtime.CommonTokenStream;

public class BenchmarkTest
{
    public static void main(String[] args)
    {
        String code = "i = 0;\n" +
                      "if i == 1 then \n" +
                      "\tif i == 0 then \n" +
                      "\t\ti = 10;\n";

        CodePointCharStream charStream  = CharStreams.fromString(code);
        SimpleLexer lexer               = new SimpleLexer(charStream);
        CommonTokenStream tokenStream   = new CommonTokenStream(lexer);
//        BenchmarkParser parser          = new BenchmarkParser(tokenStream);
//        BenchmarkParser.AssignContext assign = parser.assign();
//        System.out.println(assign.getText());

    }
}
