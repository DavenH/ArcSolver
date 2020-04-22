package gen.grid;

import gen.primitives.Colour;
import gen.primitives.Pos;
import gen.priors.abstraction.Symmetry;
import gen.priors.adt.Array;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BinaryOperator;

public class Mask implements Grid<Boolean>
{
    boolean grid[][];

    protected Pos pos;
    protected int width, height;
    protected Colour brush;
    Grid board;

    @Override public int getWidth()        { return width; }
    @Override public int getHeight()       { return height; }
    @Override public Grid getBoard()       { return board; }
    @Override public Colour getBrush()     { return brush; }

    @Override public int getDim(int index) { return index == 0 ? width : height; }
    @Override public Pos getPos()          { return pos; }
    @Override public void setPos(Pos pos)  { this.pos = pos; }

    @Override
    public Set<Symmetry> getSymmetries()
    {
        Set<Symmetry> syms = new HashSet<>();

        for(int i = 0; i < width; ++i)
        {
            for(int j = 0; j < height; ++j)
            {

            }
        }

        if(equals(reflect(Symmetry.Horz)))
        {
            syms.add(Symmetry.Horz);
        }

        return Collections.emptySet();
    }

    public Mask(Mask m)
    {
        this(m.getBoard(), m.getWidth(), m.getHeight(), m.getBrush());
        this.pos = m.pos.copy();
    }

    public Mask(Grid board, String bitString, int width)
    {
        this(board, width, bitString.length() / width);
        int charIdx  = 0;

        for(int j = 0; j < height; ++j)
        {
            for(int i = 0; i < width; ++i)
            {
                char ch = bitString.charAt(charIdx);

                int colorIndex = ch - '0';
                set(i, j, colorIndex != 0);

                ++charIdx;
            }
        }
    }

    public Mask(Grid board, int width, int height)
    {
        this(board, width, height, Colour.None);
    }

    public Mask(Grid board, int width, int height, Colour color)
    {
        this(board, width, height, new Pos(0, 0), color);
    }

    public Mask(Grid board, int width, int height, Pos pos)
    {
        this(board, width, height, pos, Colour.None);
    }

    public Mask(Grid board, int width, int height, Pos pos, boolean value)
    {
        this(board, width, height, pos);
        set(value);
    }

    public Mask(Grid board, int width, int height, Pos pos, Colour color)
    {
        brush = color;
        this.board = board;
        this.pos = pos.copy();

        resize(width, height);
    }

    public Mask(Grid board, List<Pos> cells)
    {
        this.board = board;
        int minX = Integer.MAX_VALUE;
        int maxX = 0;
        int minY = Integer.MAX_VALUE;
        int maxY = 0;

        for(Pos p : cells)
        {
            minX = Math.min(minX, p.x);
            minY = Math.min(minY, p.y);
            maxX = Math.max(maxX, p.x);
            maxY = Math.max(maxY, p.y);
        }

        resize(maxX - minX + 1, maxY - minY + 1);
        pos = new Pos(minX, minY);

        for(Pos p : cells)
            paint(p.x - minX, p.y - minY);
    }

    public void resize(int width, int height)
    {
        this.width = width;
        this.height = height;
        grid = (width <= 0 || height <= 0) ? new boolean[1][1] : new boolean[height][width];
    }

    public Mask doOp(Mask mask, BinaryOperator<Boolean> op)
    {
        int leftmost   = Math.min(pos.x, mask.pos.x);
        int rightmost  = Math.max(pos.x + width, mask.pos.x + mask.width);
        int bottommost = Math.min(pos.y, mask.pos.y);
        int topmost    = Math.max(pos.y + height, mask.pos.y + mask.height);

        Mask product = new Mask(getBoard(),
                                rightmost - leftmost + 1,
                                topmost - bottommost + 1,
                                new Pos(leftmost, bottommost),
                                brush);

        Pos thisDelta = product.pos.minus(pos);
        Pos thatDelta = product.pos.minus(mask.pos);

        Array arr = Pos.permute(width, height);
        for(Object obj : arr)
        {
            Pos p = (Pos) obj;
            product.set(p, op.apply(get(p.plus(thisDelta)),
                                    mask.get(p.plus(thatDelta))));
        }

//        for(int i = 0; i < product.width; ++i)
//            for(int j = 0; j < product.height; ++j)
//                product.set(i, j, op.apply(get(i + thisDeltaX, j + thisDeltaY),
//                                           mask.get(i + thatDeltaX, j + thatDeltaY)));

        return product.trim();
    }

    public Mask or  (Mask grid) { return doOp(grid, (a, b) -> a || b);   }
    public Mask and (Mask grid) { return doOp(grid, (a, b) -> a && b);   }
    public Mask eq  (Mask grid) { return doOp(grid, (a, b) -> a == b);   }
    public Mask nand(Mask grid) { return doOp(grid, (a, b) -> !(a & b)); }
    public Mask xor (Mask grid) { return doOp(grid, (a, b) -> a != b);   }
    public Mask neg (Mask grid) { return doOp(grid, (a, b) -> a && ! b);   }

    public Mask negate()
    {
        Mask mask = new Mask(getBoard(), width, height, pos, brush);

        for(int i = 0; i < width; ++i)
            for(int j = 0; j < height; ++j)
                mask.set(i, j, ! get(i, j));
        return mask;
    }

    public Mask trim()
    {
        int x = 0, y = 0;

        f1:
        for(x = 0; x < width; ++x)
            for(y = 0; y < height; ++y)
                if(isNotEmpty(x, y))
                    break f1;

        int fromLeftX = x;

        f2:
        for(x = width - 1; x >= 0; --x)
            for(y = 0; y < height; ++y)
                if(isNotEmpty(x, y))
                    break f2;

        int fromRightX = x;

        f3:
        for(y = 0; y < height; ++y)
            for(x = 0; x < width; ++x)
                if(isNotEmpty(x, y))
                    break f3;

        int fromBotY = y;

        f4:
        for(y = height - 1; y >= 0; --y)
            for(x = 0; x < width; ++x)
                if(isNotEmpty(x, y))
                    break f4;

        int fromTopY = y;

        int newWidth = fromRightX - fromLeftX + 1;
        int newHeight = fromTopY - fromBotY + 1;

        boolean[][] oldGrid = grid;
        resize(newWidth, newHeight);

        for(x = 0; x < newWidth; ++x)
            for(y = 0; y < newHeight; ++y)
                grid[y][x] = oldGrid[y + fromBotY][x + fromLeftX];

        pos = pos.plus(fromLeftX, fromBotY);
        return this;
    }

    public Mask rotate(int quartersCW)
    {
        Mask m;
        quartersCW = quartersCW % 4;

        if(quartersCW == 2)
        {
            m = new Mask(this);
            for(int i = 0; i < width; ++i)
                for(int j = 0; j < height; ++j)
                    m.set(i, j, get(width - 1 - i, height - 1 - j));
        }
        else
        {
            m = new Mask(board, getHeight(), getWidth(), brush);
            m.pos = pos.copy();

            if(quartersCW == 1)
            {
                for(int i = 0; i < width; ++i)
                    for(int j = 0; j < height; ++j)
                        m.set(j, i, get(i, height - 1 - j));
            }
            else if(quartersCW == 3)
            {
                for(int i = 0; i < width; ++i)
                    for(int j = 0; j < height; ++j)
                        m.set(j, i, get(width - 1 - i, j));
            }
        }

        return m;
    }

    public Array toArray()
    {
        Array arr = new Array();
        for(int i = 0; i < width; ++i)
            for(int j = 0; j < height; ++j)
                if(isNotEmpty(i, j))
                    arr.add(new Pos(i, j));
        return arr;
    }

    public Mask reflect(Symmetry sym)
    {
        Mask m= (sym.isDiagonal()) ?
            new Mask(board, getHeight(), getWidth(), pos, brush) :
            new Mask(this);

        Array arr = Pos.permute(width, height);
        for(Object obj : arr)
        {
            Pos p = (Pos) obj;
            switch (sym)
            {
                case Vert:      m.set(p,             get(p.fromTop(height)));
                case Horz:      m.set(p,             get(p.fromRight(width)));
                case Diag:      m.set(p.transpose(), get(p));
                case NegDiag:   m.set(p.transpose(), get(p.fromTopRight(width, height)));
            }
        }

        return m;
    }


    public void flood(Pos xy) { flood(xy.x, xy.y); }

    public void flood(int x, int y)
    {
        paint(x, y);

        for(Pos coord : Pos.neighbours)
        {
            Pos moved = coord.plus(x, y);
            if(inBounds(moved) && isEmpty(moved))
                flood(moved);
        }
    }

    @Override
    public void set(int x, int y, Boolean value)
    {
        grid[y][x] = value;
    }

    public void set(Boolean value)
    {
        for(int y = 0; y < height; ++y)
            for(int x = 0; x < width; ++x)
                grid[y][x] = value;
    }


    public void paint(Pos pos) { paint(pos.x, pos.y); }
    public void paint(int x, int y)
    {
        if(! inBounds(x, y))
            return;

        grid[y][x] = true;
    }

    public void paint(int[][] pairs)
    {
        for(int[] pair : pairs)
        {
            grid[pair[1]][pair[0]] = true;
        }
    }

    public Boolean get(int x, int y)
    {
        if(! inBounds(x, y))
            return false;

        return grid[y][x];
    }

    public Boolean get(Pos pos)
    {
        return get(pos.x, pos.y);
    }

    public Mask copy()
    {
        Mask copy = new Mask(board, width, height);

        for(int i = 0; i < width; ++i)
            for(int j = 0; j < height; ++j)
                copy.set(i, j, get(i, j));

        copy.brush = brush;
        copy.pos = pos;
        return copy;
    }

    @Override
    public boolean isEmpty(int x, int y)
    {
        return ! grid[y][x];
    }

    @Override
    public String toString()
    {
        StringBuilder builder = new StringBuilder();

        for(int y = height - 1; y >= 0; --y)
        {
            for(int x = 0; x < width; ++x)
            {
                builder.append(grid[y][x] ? "1" : "0");
                builder.append("\t");
            }

            builder.append(System.lineSeparator());
        }
        builder.append(getPos().toString());
        return builder.toString();
    }

    public void setBrush(Colour brush)
    {
        this.brush = brush;
    }


    public static void main(String[] args)
    {
        ColorGrid grid = new ColorGrid(null,
                                       "0000000000" +
                                       "0001224000" +
                                       "0001144000" +
                                       "0001554000" +
                                       "0022266600" +
                                       "0022266000" +
                                       "0022266000" +
                                       "0077766000"
                , 10);

        ColorGrid board = new ColorGrid(null, 10, 10);
        Mask m = new Mask(board, "01011111", 4);
        Mask m2 = new Mask(board, "10101100", 4);
        Mask m3 = new Mask(board, "01011101", 4);
        m.setPos(new Pos(3,3));

        Mask around = m.around();
        Mask above = m.above();
        Mask below = m.below();

        board.draw(m, Colour.Red);
        board.draw(above, Colour.Green);
//        board.draw(around, Colour.Blue);
        board.draw(below, Colour.Yellow);

        System.out.println("Board:\n" + board.toString());
        System.out.println("Around\n" + around.toString());
        System.out.println("Above\n" + above.toString());
        System.out.println("--------------------\n");
        System.out.println("\nM2\n" + m2.toString());
        System.out.println("\nM3\n" + m3.toString());
        System.out.println("\nOr M2 M3\n" + m2.or(m3).toString());
        System.out.println("\nAnd M2 M3\n" + m2.and(m3).toString());
        System.out.println("\nXOR M2 M3\n" + m2.xor(m3).toString());
        System.out.println("\nnand M2 M3\n" + m2.nand(m3).toString());
        System.out.println("\nneg M2 M3\n" + m2.neg(m3).toString());
    }
}
