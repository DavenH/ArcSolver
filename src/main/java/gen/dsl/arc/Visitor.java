package gen.dsl.arc;

import gen.grid.ColorGrid;
import gen.grid.Mask;
import gen.primitives.*;
import gen.priors.adt.Array;
import gen.priors.pattern.Pattern;
import gen.priors.spatial.Compass;
import gen.priors.topology.Topology;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.misc.Pair;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

public class Visitor extends ArcBaseVisitor<Object>
{
    int indent;

    private Map<String, Object> globalVariables = new HashMap<>();
    private Map<String, BiConsumer<Object, Token>> specialVarConsumers = new HashMap<>();
    private Map<String, Supplier<Object>> specialVarSuppliers = new HashMap<>();
    private Set<String> immutableVars = new HashSet<>();
    private Map<String, Method> globalMethods = new HashMap<>();
    private Map<String, Method> localMethods = new HashMap<>();
    private Map<String, Object> foreachVarBindName = new HashMap<>();
    private Map<String, Visitor> namespaces = new HashMap<>();
//    private Map<Class, Class> toPrimitive = new HashMap<>();

    static Class<Integer> ci = Integer.class;
    static Class<Object> co = Object.class;
    static Class<String> cs = String.class;

    ColorGrid board;
    ArcParser recognizer;
    String namespaceName;
    private ColorGrid input;

    public Visitor(String name, ArcParser parser)
    {
        indent = 0;
        this.namespaceName = name;
        this.recognizer = parser;

        try
        {
            addLocalMethod("draw|gen.grid.ColorGrid", Visitor.class.getMethod("draw", ColorGrid.class));
            addLocalMethod("draw|gen.grid.Mask", Visitor.class.getMethod("draw", Mask.class));
            addLocalMethod("draw|gen.grid.Mask|gen.primitives.Colour", Visitor.class.getMethod("draw", Mask.class, Colour.class));
            addLocalMethod("draw|gen.grid.Mask|gen.primitives.Pos|gen.primitives.Colour", Visitor.class.getMethod("draw", Mask.class, Pos.class, Colour.class));
            addLocalMethod("draw|gen.grid.Mask|gen.primitives.Pos", Visitor.class.getMethod("draw", Mask.class, Pos.class));

            addLocalMethod("fill|gen.primitives.Colour", Visitor.class.getMethod("fill", Colour.class));
//            addLocalMethod("draw|gen.primitives.Rectangle", DslVisitor.class.getMethod("draw", Rectangle.class));
//            addLocalMethod("draw|gen.primitives.Square", DslVisitor.class.getMethod("draw", Square.class));
//            addLocalMethod("draw|gen.primitives.Line", DslVisitor.class.getMethod("draw", Line.class));
            addLocalMethod("draw|gen.priors.adt.Array", Visitor.class.getMethod("draw", Array.class));

            addLocalMethod("dims|java.lang.Integer|java.lang.Integer", Visitor.class.getMethod("dims", ci, ci));
            addLocalMethod("dims|java.lang.Integer", Visitor.class.getMethod("dims", ci));
            addLocalMethod("dims|gen.grid.ColorGrid", Visitor.class.getMethod("dims", ColorGrid.class));

//            addLocalMethod("set_brush|gen.primitives.Colour", DslVisitor.class.getMethod("setBrush", Colour.class));

            addLocalMethod("grid|java.lang.Integer", Visitor.class.getMethod("grid", ci));
            addLocalMethod("grid|java.lang.Integer|java.lang.Integer", Visitor.class.getMethod("grid", ci, ci));
            addLocalMethod("grid|java.lang.String|java.lang.Integer", Visitor.class.getMethod("grid", cs, ci));

            addLocalMethod("layout|java.lang.Integer|java.lang.Integer|java.lang.Integer|java.lang.Integer", Visitor.class.getMethod("layout", ci, ci, ci, ci));

            addLocalMethod("list", Visitor.class.getMethod("list"));
            addLocalMethod("list|java.lang.Object", Visitor.class.getMethod("list", co));
            addLocalMethod("list|java.lang.Object|java.lang.Object", Visitor.class.getMethod("list", co, co));
            addLocalMethod("list|java.lang.Object|java.lang.Object|java.lang.Object", Visitor.class.getMethod("list", co, co, co));

            addLocalMethod("pos|java.lang.Integer|java.lang.Integer", Visitor.class.getMethod("pos", ci, ci));

            addLocalMethod("mask|java.lang.String|java.lang.Integer", Visitor.class.getMethod("mask", cs, ci));
            addLocalMethod("mask|java.lang.String|java.lang.Integer|gen.primitives.Colour", Visitor.class.getMethod("mask", cs, ci, Colour.class));
//            public Mask mask(String bitString, Integer width, Colour colour)


            addLocalMethod("line|java.lang.Integer|gen.priors.spatial.Compass", Visitor.class.getMethod("line", ci, Compass.class));
            addLocalMethod("line|gen.primitives.Pos|java.lang.Integer|gen.priors.spatial.Compass", Visitor.class.getMethod("line", Pos.class, ci, Compass.class));

            addLocalMethod("square|java.lang.Integer", Visitor.class.getMethod("square", ci));
            addLocalMethod("square|gen.primitives.Pos|java.lang.Integer", Visitor.class.getMethod("square", Pos.class, ci));

            addLocalMethod("dot|gen.primitives.Pos", Visitor.class.getMethod("dot", Pos.class));
            addLocalMethod("dot|gen.primitives.Pos|gen.primitives.Colour", Visitor.class.getMethod("dot", Pos.class, Colour.class));

            addLocalMethod("rect|java.lang.Integer|java.lang.Integer", Visitor.class.getMethod("rect", ci, ci));
            addLocalMethod("rect|java.lang.Integer|gen.primitives.Pos|java.lang.Integer", Visitor.class.getMethod("rect", Pos.class, ci, ci));

            addGlobalVariable("brush", Colour.None);
            addGlobalVariable("background", Colour.Black);
            addGlobalVariable("topology", new Topology());
            addGlobalVariable("pattern", new Pattern());

            specialVarConsumers.put("brush", (val, token) -> {
                if(val instanceof Colour)
                    board.setBrush((Colour) val);
                else
                    throw new InterpreterException(token, "Cannot set 'brush' to anything but a colour.");
            });

            specialVarConsumers.put("background", (val, token) -> {
                if(val instanceof Colour)
                    board.setBackground((Colour) val);
                else
                    throw new InterpreterException(token, "Cannot set 'background' to anything but a colour.");
            });

            specialVarSuppliers.put("brush",      () -> board.getBrush());
            specialVarSuppliers.put("background", () -> board.getBackground());
            specialVarSuppliers.put("board",      () -> board);
            specialVarSuppliers.put("source",     () -> input);

            immutableVars.addAll(Arrays.asList("topology", "pattern", "source", "board"));
        }
        catch (NoSuchMethodException e)
        {
            e.printStackTrace();
        }
    }

    public ColorGrid getBoard()
    {
        return board;
    }

    public void setInputGrid(ColorGrid input)
    {
        this.input = input;
        addGlobalVariable("source", input);
    }

    private void log(String text)
    {
//        System.out.println(text);
    }

    public void dims(Integer width)
    {
        log("setting dims " + width);
        dims(width, width);
    }

    public void dims(Integer width, Integer height)
    {
        log("setting dims " + width + " " + height);
        board = new ColorGrid(null, width, height);
    }

    public void dims(ColorGrid input)
    {
        log("setting dims " + input.getWidth() + " " + input.getHeight());
        board = new ColorGrid(null, input.getWidth(), input.getHeight());
    }

    public void fill(Colour c)                  { log("filling board"); board.fill(c); }
    public void draw(ColorGrid g)               { log("drawing colorgrid to board");        board.draw(g);  }
    public void draw(Mask mask)                 { log("drawing mask to board");             board.draw(mask); }
    public void draw(Mask g, Colour c)          { log("drawing mask,clr to board");         board.draw(g, c);   }
    public void draw(Mask g, Pos p)             { log("drawing mask,pos to board");         board.draw(g, p); }
    public void draw(Mask g, Pos p, Colour c)   { log("drawing mask,pos,clr to board");     board.draw(g, p, c); }

    public void draw(Array grids)
    {
        log("drawing array to board");

        for(Object o : grids)
        {
            if(o instanceof ColorGrid)
                draw((ColorGrid) o);
            else if(o instanceof Mask)
                draw((Mask) o);
        }
    }

    public Array list()
    {
        return new Array();
    }

    public Array list(Object object)
    {
        Array list = new Array();
        if(object instanceof Object[])
        {
            Object[] many = (Object[]) object;
            for(Object one : many)
            {
                list.add(one);
            }
        }
        else
        {
            list.add(object);
        }

        return list;
    }

    public Array list(Object one, Object two)
    {
        Array list = new Array();
        list.add(one);
        list.add(two);
        return list;
    }

    public Array list(Object one, Object two, Object thr)
    {

        Array list = new Array();
        list.add(one);
        list.add(two);
        list.add(thr);
        return list;
    }

    public Line line(gen.priors.spatial.Vector vector)
    {
        log("making new line,vec");

        return new Line(board, vector);
    }

    public Line line(Integer length, Compass sym)
    {
        log("making new line,comp");

        return new Line(board, new gen.priors.spatial.Vector(sym, length));
    }

    public Line line(Pos pos, Integer length, Compass sym)
    {
        log("making new line,pos,len,comp");

        Line line = line(length, sym);
        line.setPos(pos);
        return line;
    }

    public Mask mask(String bitString, Integer width, Colour colour)
    {
        Mask grid = mask(bitString, width, colour);
        grid.setBrush(colour);
        return grid;
    }

    public Pos pos(Integer x, Integer y)                {   return new Pos(x, y);   }
    public Mask mask(String bitString, Integer width)   {   return new Mask(board, bitString, width);   }
    public Mask rect(Integer width, Integer height)     {   return Mask.rect(board, width, height, new Pos(0, 0)); }
    public Mask rect(Pos xy, Integer width, Integer height) {   return Mask.rect(board, width, height, xy); }
    public Mask rect(Pos xy, Integer w, Integer h, Colour c) {   return Mask.rect(board, w, h, xy, c); }
    public Mask square(Pos xy, Integer size, Colour c)  {   return Mask.square(board, size, xy, c); }
    public Mask square(Pos xy, Integer size)            {   return Mask.square(board, size, xy); }
    public Mask square(Integer size)                    {   return Mask.square(board, size, new Pos(0, 0)); }
    public Mask dot(Pos xy)                             {   return Mask.dot(board, xy, Colour.None); }
    public Mask dot(Pos xy, Colour c)                   {   return Mask.dot(board, xy, c); }

    public ColorGrid grid(Integer scale)                {   return new ColorGrid(board, scale, scale);  }
    public ColorGrid grid(Integer w, Integer h)         {   return new ColorGrid(board, w, h);  }
    public ColorGrid grid(String bitString, Integer width)  {   return new ColorGrid(board, bitString, width);  }

    public Array layout(Integer columns, Integer rows, Integer scale, Integer gap)
    {
        log("making new layout,col,row,scale,gap");

        Array array = new Array();
        for(int x = 0; x < columns; ++x)
        {
            for(int y = 0; y < rows; ++y)
            {
                ColorGrid grid = new ColorGrid(board, scale, scale);
                Pos pos = new Pos(x * scale + Math.max(0, (x - 1)) * gap,
                                  y * scale + Math.max(0, (y - 1)) * gap);
                grid.setArrayPos(x, y);
                grid.setPos(pos);
                array.add(grid);
            }
        }

        return array;
    }

    public void addGlobalMethod(String methodName, Method method)
    {
        globalMethods.put(methodName, method);
    }

    public void addLocalMethod(String methodName, Method method)
    {
        localMethods.put(methodName, method);
    }

    public void addGlobalVariable(String variableName, Object value)
    {
        globalVariables.put(variableName, value);
    }

    public Object getGlobalVariable(String variableName)
    {
        return globalVariables.get(variableName);
    }

    public static String buildMethodName(String root, Class[] types)
    {
        StringBuilder builder = new StringBuilder();
        builder.append(root);

        if(types.length > 0)
        {
            for(Class c : types)
                builder.append("|").append(c.getName());
        }

        return builder.toString();
    }

    @Override
    public Pair<Object[], Class[]> visitParamList(ArcParser.ParamListContext ctx)
    {
        // in the case of no-arg func
        if(ctx == null)
        {
            log("    " + namespaceName + ": empty param list");
            return new Pair<>(new Object[0], new Class[0]);
        }

        log("    " + namespaceName + ": param list: " + ctx.getText());

        List<ArcParser.ExprContext> params = ctx.expr();

        Object[] args = new Object[params.size()];
        Class[] types = new Class[params.size()];

        int idx = 0;
        for (ArcParser.ExprContext expr : params)
        {
            Object arg = visitExpr(expr);
            args[idx] = arg;
            types[idx] = arg.getClass();
            ++idx;
        }

        return new Pair<>(args, types);
    }

    private Object invokeMethod(Object instance,
                                ArcParser.MethodInvocationContext ctx,
                                Visitor namespaceToReturnTo)
    {
        log("    " + namespaceName + ": method: " + ctx.getText());

        TerminalNode id = ctx.ID();

        ArcParser.ParamListContext paramList = ctx.paramList();
        Pair<Object[], Class[]> params = namespaceToReturnTo.visitParamList(paramList);

        try
        {
            String methodName = id.getText();

            if(instance != null)
            {
                Method m = instance.getClass().getMethod(methodName, params.b);
                return m.invoke(instance, params.a);
            }
            else
            {
                String methodKey = buildMethodName(methodName, params.b);

                if(localMethods.containsKey(methodKey))
                {
                    Method m = localMethods.get(methodKey);
                    return m.invoke(this, params.a);
                }

                if(globalMethods.containsKey(methodKey))
                {
                    Method m = globalMethods.get(methodKey);
                    return m.invoke(null, params.a);
                }
            }

            throw new InterpreterException(id.getSymbol(),
                                     "No method" + (instance != null ? " on object " + instance.getClass().getName() : "")
                                     + " associated with name '" + methodName + "'" + params);
        }
        catch (NoSuchMethodException e)
        {
            Token startToken = ctx.getStart();
            throw new InterpreterException(startToken, "No such method: " + e.getMessage());
        }
        catch (Exception e)
        {
            Token startToken = ctx.getStart();
            throw new InterpreterException(startToken, e.getMessage());
        }
    }

    @Override
    public Object visitMethodInvocation(ArcParser.MethodInvocationContext ctx)
    {
        System.out.println("methinvoke: " + ctx.getText());

        return invokeMethod(null, ctx, this);
    }

    @Override
    public Object visitExpr(ArcParser.ExprContext ctx)
    {
        log("    " + namespaceName + ": expr: " + ctx.getText());

        List<ArcParser.ExprContext> expressions = ctx.expr();
        Token op = ctx.op;
        ArcParser.GeneratorContext genCtx = ctx.generator();
        Token startToken = ctx.getStart();

        if(genCtx != null)
        {
            return visitGenerator(genCtx);
        }

        if(ctx.op != null && !ctx.expr().isEmpty())
        {
            if(expressions.size() == 1)
            {
                Object value = visitExpr(expressions.get(0));
                switch (op.getType())
                {
                    case ArcParser.NOT:
                    {
                        if(value instanceof Boolean)
                            return !((Boolean) value);
                        if(value instanceof Mask)
                            return ((Mask) value).negate();
                    }

                    case ArcParser.MINUS:
                    {
                        if(value instanceof Integer)
                            return -((Integer) value);
                    }
                }

                throw new InterpreterException(startToken, "Operator " + op.getText() + " inapplicable to object " + value);
            }
            else
            {
                if(expressions.size() == 2)
                {
                    Object leftValue = visitExpr(expressions.get(0));
                    Object rightValue = visitExpr(expressions.get(1));

                    if(leftValue == null || rightValue == null)
                    {
                        throw new InterpreterException(startToken,
                                                       "One of the arguments of binary expression is null " + ctx.getText());
                    }

                    if(leftValue instanceof Boolean && rightValue instanceof Boolean)
                    {
                        boolean boolLeft = (Boolean) leftValue;
                        boolean boolRight = (Boolean) rightValue;

                        switch (op.getType())
                        {
                            case ArcParser.EQ:   return boolLeft == boolRight;
                            case ArcParser.NEQ:  return boolLeft != boolRight;
                            case ArcParser.AND:  return boolLeft && boolRight;
                            case ArcParser.OR:   return boolLeft || boolRight;
                            case ArcParser.XOR:  return boolLeft ^ boolRight;
                            default:
                                throw new RuntimeException(
                                        "Boolean expression values but not a boolean operator: " + ctx.getText());
                        }
                    }
                    else if(leftValue instanceof Integer && rightValue instanceof Integer)
                    {
                        int intLeft = (Integer) leftValue;
                        int intRight = (Integer) rightValue;

                        switch (op.getType())
                        {
                            case ArcParser.EQ:   return intLeft == intRight;
                            case ArcParser.NEQ:  return intLeft != intRight;
                            case ArcParser.GT:   return intLeft > intRight;
                            case ArcParser.GTEQ: return intLeft >= intRight;
                            case ArcParser.LT:   return intLeft < intRight;
                            case ArcParser.LTEQ: return intLeft <= intRight;
                            case ArcParser.PLUS: return intLeft + intRight;
                            case ArcParser.MINUS:return intLeft - intRight;
                            case ArcParser.MUL:  return intLeft * intRight;
                            case ArcParser.DIV:
                                if(intRight == 0)
                                {
                                    throw new RuntimeException("Division by zero");
                                }
                                return intLeft / intRight;

                            default:
                                throw new RuntimeException("" + ctx.getText());
                        }
                    }
                    else if(leftValue instanceof Mask && rightValue instanceof Mask)
                    {
                        Mask leftGrid = (Mask) leftValue;
                        Mask rightGrid = (Mask) rightValue;

                        switch (op.getType())
                        {
                            case ArcParser.XOR:  return leftGrid.xor(rightGrid);
                            case ArcParser.OR:   return leftGrid.or(rightGrid);
                            case ArcParser.AND:  return leftGrid.and(rightGrid);
                            case ArcParser.NAND: return leftGrid.nand(rightGrid);
                            case ArcParser.EQ:   return leftGrid.eq(rightGrid);
                        }
                    }
                    else
                    {
                        switch (op.getType())
                        {
                            case ArcParser.EQ:   return leftValue.equals(rightValue);
                            case ArcParser.NEQ:  return !leftValue.equals(rightValue);
                        }
                    }
                }
            }
        }

        throw new RuntimeException("Expression has unexpected number of subexpressions: " +
                                   expressions.size());
    }


    @Override
    public Object visitLiteral(ArcParser.LiteralContext ctx)
    {
        log("    " + namespaceName + ": literal: " + ctx.getText());

        TerminalNode enumVal   = ctx.ENUM();
        TerminalNode integer   = ctx.INTEGER();
        TerminalNode bitString = ctx.BIT_STRING();

        if(enumVal != null)
        {
            Object val = enumToColor(enumVal);
            if(val != null)
                return val;

            val = enumToCompass(enumVal);
            if(val != null)
                return val;

            return enumVal.getText();
        }
        else if(integer != null)
        {
            return Integer.valueOf(integer.getText());
        }
        else if(bitString != null)
        {
            return removeQuotes(bitString.getText());
        }

        throw new RuntimeException("Literal not recognized: " + ctx.getText());
    }

    @Override public Object visitTuple(ArcParser.TupleContext ctx)
    {
        Integer one = Integer.valueOf(ctx.INTEGER(0).getText());
        Integer two = Integer.valueOf(ctx.INTEGER(1).getText());
        return new Pos(one, two);
    }

    private String removeQuotes(String text)
    {
        return text.substring(1, text.length() - 1);
    }

    private Colour enumToColor(TerminalNode enumVal)
    {
        switch (enumVal.getText())
        {
            case "Black":   return Colour.Black;
            case "Blue":    return Colour.Blue;
            case "Orange":  return Colour.Orange;
            case "Magenta": return Colour.Magenta;
            case "Maroon":  return Colour.Maroon;
            case "Cyan":    return Colour.Cyan;
            case "Yellow":  return Colour.Yellow;
            case "Grey":    return Colour.Grey;
            case "Green":   return Colour.Green;
            case "Red":     return Colour.Red;
            case "None":    return Colour.None;
            default:
                break;
        }

        return null;
    }

    private Compass enumToCompass(TerminalNode compassVal)
    {
        switch(compassVal.getText())
        {
            case "N" : return Compass.N;
            case "NE" : return Compass.NE;
            case "E" : return Compass.E;
            case "SE" : return Compass.SE;
            case "S" : return Compass.S;
            case "SW" : return Compass.SW;
            case "W" : return Compass.W;
            case "NW" : return Compass.NW;
        }

        return null;
    }

    @Override
    public Object visitArray(ArcParser.ArrayContext ctx)
    {
        List<ArcParser.GeneratorContext> generators = ctx.generator();
        Object[] values = new Object[generators.size()];
        int idx = 0;

        for (ArcParser.GeneratorContext genCtx : generators)
            values[idx++] = visitGenerator(genCtx);

        return values;
    }

    @Override
    public Object visitAssignment(ArcParser.AssignmentContext ctx)
    {
        log("    " + namespaceName + ": assignment: " + ctx.getText());

        TerminalNode dependentVar = ctx.ID();
        ArcParser.GeneratorContext gen = ctx.generator();
        Object value = visitGenerator(gen);

        resolveAndSetVariable(dependentVar, value);

        return null;
    }

    private void iterateForeach(String text, Iterable<?> iterable, ArcParser.BlockContext ctx)
    {
        for (Object val : iterable)
        {
            foreachVarBindName.put(text, val);
            visitBlock(ctx);
            // whenever we see 'id' in block, bind to value
        }

        foreachVarBindName.remove(text);
    }

    @Override
    public Object visitForeachBlock(ArcParser.ForeachBlockContext ctx)
    {
        log("    " + namespaceName + ": foreach block: " + ctx.getText());

        ArcParser.GeneratorContext genCtx = ctx.generator();

        TerminalNode id = ctx.ID();

        // push foreach state

        Object iterableObj = visitGenerator(genCtx);

        if(iterableObj instanceof Object[])
        {
            iterateForeach(id.getText(), Arrays.asList((Object[]) iterableObj), ctx.block());
        }
        else if(iterableObj instanceof Iterable<?>)
        {
            iterateForeach(id.getText(), ((Iterable<?>) iterableObj), ctx.block());
        }
        else
        {
            throw new InterpreterException(id.getSymbol(),
                                     "Foreach has to be given an Object[] array or Iterable<?> for final argument, not "
                                     + iterableObj.getClass().getName() + " in " + ctx.getText());
        }
        return null;
    }

    private boolean handleIfBlock(Object value, ArcParser.BlockContext blockIfTrue)
    {
        if(value instanceof Boolean)
        {
            if((Boolean) value)
            {
                visit(blockIfTrue);
                return true;
            }
        }
        else
        {
            throw new RuntimeException("IF expression did not resolve in boolean value");
        }

        return false;
    }

    @Override
    public Object visitIfBlock(ArcParser.IfBlockContext ctx)
    {
        log("    " + namespaceName + ": if block: " + ctx.getText());

        Object value = visitExpr(ctx.expr());
        boolean handled = handleIfBlock(value, ctx.block());

        if(! handled)
        {
            List<ArcParser.ElseIfBlockContext> elseIfs = ctx.elseIfBlock();
            if(elseIfs != null)
            {
                for (ArcParser.ElseIfBlockContext elseIf : elseIfs)
                {
                    Object elseValue = visitExpr(elseIf.expr());
                    handled = handleIfBlock(elseValue, elseIf.block());
                    if(handled)
                        break;
                }
            }

            if(! handled)
            {
                ArcParser.ElseBlockContext elseBlockCtx = ctx.elseBlock();
                if(elseBlockCtx != null)
                    handleIfBlock(value, elseBlockCtx.block());
            }
        }

        return null;
    }

    private Object resolveVariable(TerminalNode varName)
    {
        String fieldName = varName.getText();

        Supplier<Object> try0 = specialVarSuppliers.get(fieldName);
        if(try0 != null)
            return try0.get();

        Object try1 = foreachVarBindName.get(fieldName);
        if(try1 != null)
            return try1;

        Object try2 = globalVariables.get(fieldName);

        if(try2 != null)
            return try2;

        throw new InterpreterException(varName.getSymbol(),
                                       "Cannot resolve variable " + fieldName + " within namespace " + namespaceName);
    }

    private void resolveAndSetVariable(TerminalNode varName, Object value)
    {
        // current object is set by a member access expression
        /*
        if(currentObject != null)
        {
            Field field = getField(currentObject, varName);
            try
            {
                field.set(currentObject, value);
            }
            catch (IllegalAccessException e)
            {
                throw new RuntimeException(e);
            }
        }
        else */

        String identifier = varName.getText();

        if(foreachVarBindName.containsKey(varName))
        {
            foreachVarBindName.put(identifier, value);
        }
        else
        {
            log("setting var " + identifier + " to " + value);

            BiConsumer<Object, Token> consumer = specialVarConsumers.get(identifier);

            if(consumer != null)
            {
                consumer.accept(value, varName.getSymbol());
            }
            else if(immutableVars.contains(identifier))
            {
                throw new InterpreterException(varName.getSymbol(), "Cannot set the '" + identifier + "' variable");
            }
            else
            {
                globalVariables.put(identifier, value);
            }
        }
    }

    private Field getField(Object instance, String fieldName)
    {
        try
        {
            Field field = instance.getClass().getField(fieldName);
            return field;
        }
        catch (NoSuchFieldException e)
        {
            throw new RuntimeException(e);
        }
    }

    private Object getFieldValue(Object instance, TerminalNode fieldName)
    {
        try
        {
            Field field = instance.getClass().getField(fieldName.getText());
            return field.get(instance);
        }
        catch (NoSuchFieldException | IllegalAccessException e)
        {
            throw new RuntimeException(e);
        }
    }

    /*
     memberAccess : member | memberAccess '.' member ;
     a.b().c --> ((a.b()).c) -->

                      memberAccess  ((a.b()).c)
                        /
       (a.b()) memberAccess    '.'    member (c)
                    /                     \
         (a)  memberAccess '.' member      id (c)
                     \           \          |
                (a) member       b()        c
                      |           |
                  (a) id     methodInvoke
                      |           |
                      a          *b

     in::a().b[1].c --> ((in::a().b[1]).c) -->

                          memberAccess    ((in::a().b[1]).c)
                            /
        (in::a().b[1])  memberAccess    '.'    member (c)
                       /                        \
         (a())  memberAccess '.' member (b[1])   c
                     \             \
           (a())    member      arrayAccess
                      |             |
              a() methodInvoke     b[1]
                      |
                      a()

     */
    @Override
    public Object visitMemberAccess(ArcParser.MemberAccessContext ctx)
    {
        log("    " + namespaceName + ": member acc: " + ctx.getText());

        ArcParser.MemberAccessContext memoryAccess = ctx.memberAccess();
        ArcParser.MemberContext member = ctx.member();
        ArcParser.NamespaceAccessContext nsContext = ctx.namespaceAccess();

        // if we're in here, it means a left-hand branch above
        if(memoryAccess != null)
        {
            Object instanceWithMember = visitMemberAccess(memoryAccess);
            return visitMember(instanceWithMember, member, this);
        }
        else if(member != null)
        {
            // this doesn't need an instance variable because it's the left-most in the chain -- comes from global
            // scope or from the foreach bound variable name.
            return visitMember(null, member, this);
        }
        else if(nsContext != null)
        {
            return visitNamespaceAccess(nsContext);
        }

        Token startToken = ctx.getStart();
        throw new InterpreterException(startToken, "Member access undefined: " + ctx.getText());
    }

    @Override public Object visitNamespaceAccess(ArcParser.NamespaceAccessContext ctx)
    {
        log("    " + namespaceName + ": namespace access " + ctx.getText());

        TerminalNode id = ctx.ID();

        Visitor namespace = namespaces.get(id.getText());

        if(namespace == null)
            throw new InterpreterException(id.getSymbol(), "Namespace " + id.getText() + " is not registered");

        return namespace.visitMember(null, ctx.member(), this);
    }

    @Override
    public Object visitArrayAccess(ArcParser.ArrayAccessContext ctx)
    {
        return visitArrayAccess(null, ctx, this);
    }

    private Object visitArrayAccess(Object instance, ArcParser.ArrayAccessContext ctx, Visitor namespaceToReturnTo)
    {
        log("    " + namespaceName + ": array access: " + ctx.getText());

        ArcParser.ExprContext expr = ctx.expr();
        TerminalNode id = ctx.ID();

        Object value = namespaceToReturnTo.visitExpr(expr);
        Object variable = instance == null ?
                          resolveVariable(id) :
                          getFieldValue(instance, id);

        if(variable == null)
        {
            throw new RuntimeException("Unresolved array reference: " + id.getText());
        }

        if(value instanceof Integer)
        {
            int index = (Integer) value;

            if(variable instanceof Object[])
            {
                Object[] objs = (Object[]) variable;

                if(index < objs.length && index >= 0)
                    return objs[index];

                throw new RuntimeException("Array out of bounds: index=" + index + " size=" + objs.length);
            }
            else if(variable instanceof Array)
            {
                Array list = (Array) variable;

                if(index < list.size() && index >= 0)
                    return list.get(index);

                throw new RuntimeException("Array out of bounds: index=" + index + " size=" + list.size());
            }
            else if(variable instanceof Set)
            {
                // TODO
                throw new UnsupportedOperationException("Sets are not a supported iterator yet.");
            }
            else
            {
                throw new RuntimeException("Lookup value is integer, but dependent object is not an array");
            }
        }
        else if(value instanceof Pos)
        {
            try
            {
                Method m = variable.getClass().getMethod("get", Pos.class);
                return m.invoke(variable, (Pos) value);
            }
            catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e)
            {
                throw new RuntimeException("Object " + variable.getClass().getName() + " could not be indexed by [pos]", e);
            }
        }

        throw new RuntimeException("Value is not an indexable type [integer, pos]");
    }

    private Object visitMember(Object instanceWithMember, ArcParser.MemberContext ctx, Visitor namespaceToReturnTo)
    {
        log("    " + namespaceName + ": member: " + ctx.getText());

        TerminalNode fieldName = ctx.ID();

        // this member is a field of 'instanceWithMember' ex: inst.field1
        if(fieldName != null)
        {
            if(instanceWithMember != null)
            {
                return getFieldValue(instanceWithMember, fieldName);
            }

            return resolveVariable(fieldName);
        }

        // this member is an array access ex: inst.arr[0]
        ArcParser.ArrayAccessContext arrayAccessContext = ctx.arrayAccess();
        if(arrayAccessContext != null)
        {
            return visitArrayAccess(instanceWithMember, arrayAccessContext, namespaceToReturnTo);
        }

        ArcParser.MethodInvocationContext invocationContext = ctx.methodInvocation();
        if(invocationContext != null)
        {
            return invokeMethod(instanceWithMember, invocationContext, namespaceToReturnTo);
        }

        throw new RuntimeException("Not a viable member " + ctx.getText());
    }

    @Override
    public Object visitMember(ArcParser.MemberContext ctx)
    {
        return visitMember(null, ctx, this);
    }

    private String indentSpaces(int n)
    {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < 4 * n; ++i)
            builder.append(" ");

        return builder.toString();
    }

    private void print(Object o)
    {
        System.out.println(indentSpaces(indent) + (o == null ? "null" : o.toString()));
    }

    public void addNamespace(String namespaceTag, Visitor visitorWithinScope)
    {
        namespaces.put(namespaceTag, visitorWithinScope);
    }
}
