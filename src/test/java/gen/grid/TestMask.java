package gen.grid;

import gen.primitives.Colour;
import gen.primitives.Pos;
import gen.priors.abstraction.ShapeHashAttr;

public class TestMask extends GridTest
{
    public void testCompass()
    {
        line();

        ColorGrid board = new ColorGrid(null, 6, 6);
        Mask m = new Mask(board, "001001111100100", 5);
        m.setPos(1, 1);
        m.setBrush(Colour.Yellow);

        Mask top = m.top();
        Mask left = m.left();
        Mask right = m.right();
        Mask bottom = m.bottom();

        board.draw(m);
        log("\noriginal:\n" + m.toString());
        log("\nboard:\n" + board.toString());

        log("\ntop:\n" + top.toString());
        log("\nleft:\n" + left.toString());
        log("\nright:\n" + right.toString());
        log("\nbott:\n" + bottom.toString());
    }

    public void testOutsideCompass()
    {
        line();

        ColorGrid board = new ColorGrid(null, 10, 10);
        Mask m = new Mask(board, "01011111", 4);
        m.setPos(new Pos(3, 3));

        Mask above = m.above();
        Mask below = m.below();
        Mask leftOf = m.leftOf();
        Mask rightOf = m.rightOf();

        board.draw(m, Colour.Red);
        board.draw(above, Colour.Green);
        board.draw(below, Colour.Yellow);
        board.draw(leftOf, Colour.Orange);
        board.draw(rightOf, Colour.Blue);

        log("\nOriginal:\n" + m.toString());
        log("\nBoard:\n" + board.toString());
    }

    public void testOtherFocuses()
    {
        line();

        ColorGrid board = new ColorGrid(null, 6, 6);
        Mask m = new Mask(board, "0100001010111", 4);
        m.setPos(new Pos(1,1));
        m.setBrush(Colour.Yellow);

        log("\noriginal:\n" + m.toString());

        Mask around = m.around();
        log("\nAround\n" + around.toString());
        board.draw(around, Colour.Blue);
        log("\nwith around:\n" + board.toString());

        Mask corners = m.corners();
        board.draw(corners, Colour.Red);
        log("\ncorners\n" + corners.toString());
        log("\nwith corners:\n" + board.toString());

        board = new ColorGrid(null, 6, 6);
        m = new Mask(board, "0010110111111111", 4);

        Mask perimeter = m.perimeter();
        board.draw(perimeter, Colour.Grey);
        log("\nperimeter\n" + perimeter.toString());
    }

    public void testBinaryOps()
    {
        line();

        ColorGrid board = new ColorGrid(null, 5, 5);

        Mask a = new Mask(board, "10101100", 4);
        Mask b = new Mask(board, "01011101", 4);
        b.setPos(new Pos(1,1));
        board.draw(a, Colour.Red);
        board.draw(b, Colour.Cyan);

        log("\nA\n" + a.toString());
        log("\nB\n" + b.toString());
        log("\nBoard\n" + board.toString());
        log("\nOr A B\n" + a.or(b).toString());
        log("\nAnd A B\n" + a.and(b).toString());
        log("\nXOR A B\n" + a.xor(b).toString());
        log("\nnand A B\n" + a.nand(b).toString());
        log("\nneg A B\n" + a.neg(b).toString());
    }

    public void testTrim()
    {
        line();

        ColorGrid b2 = new ColorGrid(null, 10, 10);
        Mask m2 = new Mask(b2, 10, 10);
        m2.paint(4,4);
        m2.paint(4,5);
        m2.trim();

        log("Should be 1x2: " + m2.getWidth() + "x" + m2.getHeight());
    }

    private void testBetween()
    {
        ColorGrid board = new ColorGrid(null, 10, 10);
        Mask a = new Mask(board, "010111101", 3);
        Mask b = new Mask(board, "010111101", 3);
        a.setPos(3,1);
        b.setPos(4,6);

        board.draw(a, Colour.Green);
        board.draw(b, Colour.Red);

        Mask between = a.between(b);
        board.draw(between, Colour.Blue);

        log("\nBoard\n" + board.toString());
        log("\nA\n" + a.toString());
        log("\nB\n" + b.toString());
        log("\nbetween\n" + between.toString());
    }

    private void testHashing()
    {
        ColorGrid board = new ColorGrid(null, 10, 10);

        Mask a = new Mask(board, "010111101", 3);
        Mask b = new Mask(board, "010111101", 3);
        Mask c = new Mask(board, "000111101", 3);
        Mask d = new Mask(board, "110011110", 3);
        Mask e = new Mask(board, "101111010", 3);
        Mask f = new Mask(board, "1", 1);

        log("Expecting a == b != d != e != c");
        log("Hash a = " + a.hash());
        log("Hash b = " + b.hash());
        log("Hash c = " + c.hash());
        log("Hash d = " + d.hash());
        log("Hash e = " + e.hash());
        log("Hash f = " + f.hash());

        log("\nExpecting a == b == d == e != c");
        log("Shape hash a = " + new ShapeHashAttr<>(a).getValue());
        log("Shape hash b = " + new ShapeHashAttr<>(b).getValue());
        log("Shape hash c = " + new ShapeHashAttr<>(c).getValue());
        log("Shape hash d = " + new ShapeHashAttr<>(d).getValue());
        log("Shape hash e = " + new ShapeHashAttr<>(e).getValue());
    }

    public static void main(String[] args)
    {
        TestMask testMask = new TestMask();
//        testMask.testOutsideCompass();
//        testMask.testCompass();
//        testMask.testOtherFocuses();
//        testMask.testTrim();
//        testMask.testBinaryOps();
//        testMask.testBetween();
        testMask.testHashing();
    }
}
