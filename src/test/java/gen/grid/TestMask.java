package gen.grid;

import gen.primitives.Colour;
import gen.primitives.Pos;
import gen.priors.abstraction.ShapeHashAttr;
import gen.priors.abstraction.SymmetryType;

import java.util.Set;

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
        log2("original:\n" + m.toString());
        log2("board:\n" + board.toString());

        log2("top:\n" + top.toString());
        log2("left:\n" + left.toString());
        log2("right:\n" + right.toString());
        log2("bott:\n" + bottom.toString());
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

        log2("Original:\n" + m.toString());
        log2("Board:\n" + board.toString());
    }

    public void testOtherFocuses()
    {
        line();

        ColorGrid board = new ColorGrid(null, 6, 6);
        Mask m = new Mask(board, "0100001010111", 4);
        m.setPos(new Pos(1,1));
        m.setBrush(Colour.Yellow);

        log2("original:\n" + m.toString());

        Mask around = m.around();
        log2("Around\n" + around.toString());
        board.draw(around, Colour.Blue);
        log2("with around:\n" + board.toString());

        Mask corners = m.corners();
        board.draw(corners, Colour.Red);
        log2("corners\n" + corners.toString());
        log2("with corners:\n" + board.toString());

        board = new ColorGrid(null, 6, 6);
        m = new Mask(board, "0010110111111111", 4);

        Mask perimeter = m.perimeter();
        board.draw(perimeter, Colour.Grey);
        log2("perimeter\n" + perimeter.toString());
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

        log2("A\n" + a.toString());
        log2("B\n" + b.toString());
        log2("Board\n" + board.toString());
        log2("Or A B\n" + a.or(b).toString());
        log2("And A B\n" + a.and(b).toString());
        log2("XOR A B\n" + a.xor(b).toString());
        log2("nand A B\n" + a.nand(b).toString());
        log2("neg A B\n" + a.neg(b).toString());
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

        log2("Board\n" + board.toString());
        log2("A\n" + a.toString());
        log2("B\n" + b.toString());
        log2("between\n" + between.toString());
    }

    private void testRotations()
    {
        ColorGrid board = new ColorGrid(null, 10, 10);
        Mask a = new Mask(board, "0101111010100001", 4);   // original
//        Mask a = new Mask(board, "010111101", 3);

        log("Original hash: " + a.hash());
        log("Original mask:\n" + a.toString());

        for(int i = 0; i < 4; ++i)
        {
            Grid rotated = a.rotate(i);
            int hash = rotated.hash();

            log("Hash at " + i + " 90 deg rotations: " + hash);
            log("Rotated Mask:\n" + rotated.toString());
        }
    }

    private void reportSymmetries(Grid m, String expected)
    {
        line();
        log("Original hash: " + m.hash());

        Set<SymmetryType> syms = m.getSymmetries();

        log("Expecting " + expected + " symmetries:");
        log(syms.toString());
        log(m.toString());
        log("Hashes of different symmetries: ");

        for(SymmetryType sym : SymmetryType.values())
        {
            log(sym.toString());

            Grid symMask = m.reflect(sym);
            log(symMask.hash());
        }

        line();
    }

    private void testSymmetries()
    {
        ColorGrid board = new ColorGrid(null, 10, 10);
        Mask fourSym = new Mask(board, "1001000000001001", 4);   // original
        Mask vertSym = new Mask(board, "1001100010001001", 4);   // original

        reportSymmetries(fourSym, "horz/vert/diag/negdiag");
        reportSymmetries(vertSym, "vert");

        ColorGrid fourSym2 = new ColorGrid(board, "1551566556651551", 4);   // original
        ColorGrid negdiag = new ColorGrid(board, "0002002002002000", 4);   // original

        reportSymmetries(fourSym2, "horz/vert/diag/negdiag");
        reportSymmetries(negdiag, "negdiag");
    }

    private void testTopologicalHashing()
    {
        ColorGrid board = new ColorGrid(null, 10, 10);

        long timeMillis = System.currentTimeMillis();

        Mask a = new Mask(board, "010111101", 3);   // original
//        Mask b = new Mask(board, "010111101", 3);   // equal
//        Mask c = new Mask(board, "000111101", 3);   // different
        Mask d = new Mask(board, "110011110", 3);   // rotated by 90
//        Mask e = new Mask(board, "101111010", 3);   // rotated by 180
//        Mask f = new Mask(board, "1", 1);

        log("Expecting a == b != d != e != c");
        log("Hash a = " + a.hash());
//        log("Hash b = " + b.hash());
//        log("Hash c = " + c.hash());
        log("Hash d = " + d.hash());
//        log("Hash e = " + e.hash());
//        log("Hash f = " + f.hash());

        log2("Expecting a == b == d == e != c");
        log("Shape hash a = " + new ShapeHashAttr<>(a).getValue());
//        log("Shape hash b = " + new ShapeHashAttr<>(b).getValue());
//        log("Shape hash c = " + new ShapeHashAttr<>(c).getValue());
        log("Shape hash d = " + new ShapeHashAttr<>(d).getValue());
//        log("Shape hash e = " + new ShapeHashAttr<>(e).getValue());

        long endMillis = System.currentTimeMillis();
        log(endMillis - timeMillis + " millis");
    }

    public static void main(String[] args)
    {
        TestMask testMask = new TestMask();
//        testMask.testOutsideCompass();
        testMask.testCompass();
//        testMask.testOtherFocuses();
//        testMask.testTrim();
//        testMask.testBinaryOps();
//        testMask.testBetween();
//        testMask.testTopologicalHashing();
//        testMask.testSymmetries();
//        testMask.testRotations();
    }
}
