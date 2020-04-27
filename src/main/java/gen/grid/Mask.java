package gen.grid;

import gen.primitives.Colour;
import gen.primitives.Pos;
import gen.priors.abstraction.AttrNames;
import gen.priors.abstraction.Attribute;
import gen.priors.abstraction.ShapeAttr;
import gen.priors.abstraction.ValueCategoricalAttr;
import gen.priors.adt.Array;
import gen.priors.spatial.Compass;

import java.util.List;
import java.util.Map;
import java.util.function.BinaryOperator;

public class Mask extends Grid<Boolean>
{
    protected ShapeAttr.Shape mostSpecifiedShape = ShapeAttr.Shape.Other;

    @Override
    public Mask cloneInstance(int w, int h)
    {
        return new Mask(board, w, h, pos, brush);
    }

    public Mask(Mask m)
    {
        this(m.getBoard(), m.getWidth(), m.getHeight(), m.getPos(), m.getBrush());
    }

    public Mask(Grid board, String bitString, int width)
    {
        this(board, width, bitString.length() / width);
        int charIdx  = 0;

        for(int j = 0; j < height; ++j)
        {
            for(int i = 0; i < width; ++i)
            {
                char ch = charIdx < bitString.length() ? bitString.charAt(charIdx) : '0';

                int colorIndex = ch - '0';
                set(i, j, colorIndex != 0);

                ++charIdx;
            }
        }
    }

    public Mask(Grid board, int width, int height)                          { this(board, width, height, Colour.None);          }
    public Mask(Grid board, int width, int height, Colour color)            { this(board, width, height, new Pos(0, 0), color); }
    public Mask(Grid board, int width, int height, Pos pos)                 { this(board, width, height, pos, Colour.None);     }
    public Mask(Grid board, int width, int height, Pos pos, boolean value)  { this(board, width, height, pos); set(value);      }

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
            paint(p.minus(pos));
    }

    public static Mask square(Grid board, int size, Pos pos) { return square(board, size, pos, Colour.None); }
    public static Mask square(Grid board, int size, Pos pos, Colour c)
    {
        Mask m = new Mask(board, size, size, pos, c);
        m.set(true);
        m.mostSpecifiedShape = ShapeAttr.Shape.Square;
        return m;
    }

    public static Mask rect(Grid board, int width, int height) { return rect(board, width, height, new Pos(0, 0), Colour.None); }
    public static Mask rect(Grid board, int width, int height, Pos pos) { return rect(board, width, height, pos, Colour.None); }
    public static Mask rect(Grid board, int width, int height, Pos pos, Colour c)
    {
        Mask m = new Mask(board, width, height, pos, c);
        m.set(true);
        m.mostSpecifiedShape = ShapeAttr.Shape.Rectangle;
        return m;
    }

    public static Mask line(Grid board, Compass compass, int length, Pos pos) { return line(board, compass, length, 1, pos, Colour.None); }
    public static Mask line(Grid board, Compass compass, int length, Pos pos, Colour c) { return line(board, compass, length, 1, pos, c); }
    public static Mask line(Grid board, Compass compass, int length, int thickness, Pos pos) { return line(board, compass, length, thickness, pos, Colour.None); }
    public static Mask line(Grid board, Compass compass, int length, int thickness, Pos pos, Colour c)
    {
        int w = compass.isHorizontal() ? length : thickness;
        int h = compass.isHorizontal() ? thickness : length;

        Mask m = new Mask(board, w, h, pos, c);
        m.set(true);
        m.mostSpecifiedShape = ShapeAttr.Shape.Line;
        return m;
    }

    public static Mask dot(Grid board, Pos pos) { return dot(board, pos, Colour.None); }
    public static Mask dot(Grid board, Pos pos, Colour c)
    {
        Mask m = new Mask(board, 1, 1, pos, c);
        m.set(true);
        m.mostSpecifiedShape = ShapeAttr.Shape.Dot;
        return m;
    }

    @Override
    public Map<AttrNames, Attribute> getAttributes()
    {
        Map<AttrNames, Attribute> attributes = super.getAttributes();
        specifyShape();
        attributes.put(AttrNames.Shape, new ShapeAttr(mostSpecifiedShape));

        int positive = countPositive();
        attributes.put(AttrNames.NumNegative, new ValueCategoricalAttr<>(width * height - positive));
        attributes.put(AttrNames.NumPositive, new ValueCategoricalAttr<>(positive));

        return attributes;
    }

    private void specifyShape()
    {
        if(mostSpecifiedShape == ShapeAttr.Shape.Other)
        {
            boolean filled = isFilled();
            if(filled)
            {
                // must be at least a rectangle
                if(width == height)
                    mostSpecifiedShape = width == 1 ? ShapeAttr.Shape.Dot : ShapeAttr.Shape.Square;

                else if(width == 1 || height == 1)
                    mostSpecifiedShape = ShapeAttr.Shape.Line;

                else
                    mostSpecifiedShape = ShapeAttr.Shape.Rectangle;
            }
            else
            {
                // noise detection?
                if(false)
                    mostSpecifiedShape = ShapeAttr.Shape.Noise;
                else
                    mostSpecifiedShape = ShapeAttr.Shape.Other;
            }
        }
    }

    private boolean isFilled()
    {
        for(int i = 0; i < width; ++i)
            for(int j = 0; j < height; ++j)
                if(isEmpty(i, j))
                    return false;

        return true;
    }

    @Override public void resize(int width, int height)
    {
        this.width = width;
        this.height = height;

        grid = (width <= 0 || height <= 0) ? new Boolean[1][1] : new Boolean[height][width];
        set(false);
    }

    public Mask within()
    {
        Mask m = neg(perimeter());
        m.trim();
        return m;
    }

    public Mask doOp(Mask mask, BinaryOperator<Boolean> op)
    {
        int leftmost   = Math.min(pos.x, mask.pos.x);
        int rightmost  = Math.max(pos.x + width, mask.pos.x + mask.width);
        int bottommost = Math.min(pos.y, mask.pos.y);
        int topmost    = Math.max(pos.y + height, mask.pos.y + mask.height);

        Mask product = new Mask(getBoard(),
                                rightmost - leftmost,
                                topmost - bottommost,
                                new Pos(leftmost, bottommost),
                                brush);

        Pos thisDelta = product.pos.minus(pos);
        Pos thatDelta = product.pos.minus(mask.pos);

        Array arr = Pos.permute(product.width, product.height);
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

        product.trim();
        return product;
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

    public Array<Pos> positiveToArray()
    {
        Array<Pos> arr = Pos.permute(width, height);
        Array<Pos> positive = new Array<>();

        for(Pos p : arr)
            if(isNotEmpty(p))
                positive.add(p);

        return positive;
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

    @Override
    public Boolean get(int x, int y)
    {
        if(! inBounds(x, y))
            return false;

        return grid[y][x];
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
        return grid[y][x] == null || grid[y][x] == false;
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
        builder.append("\t").append(brush);

        return builder.toString();
    }

    public void setBrush(Colour brush)
    {
        this.brush = brush;
    }


    public float[][] toFloat()
    {
        float[][] array = new float[height][width];

        for(int y = 0; y < height; ++y)
            for(int x = 0; x < width; ++x)
                array[y][x] = get(x, y) ? 1 : 0;

        return array;
    }
}
