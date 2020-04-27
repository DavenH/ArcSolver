package gen.grid;

import gen.primitives.Colour;
import gen.primitives.Pos;
import gen.priors.abstraction.*;
import gen.priors.adt.Array;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;

public class ColorGrid extends Grid<Colour>
{
    public Pos arrayPos;

    @Override
    public ColorGrid cloneInstance(int w, int h)
    {
        return new ColorGrid(board, w, h, pos, arrayPos, brush, background);
    }

    Colour background;

    @Override public int getWidth()        { return width; }
    @Override public int getHeight()       { return height; }
    @Override public Pos getPos()          { return pos; }
    public Colour getBackground()          { return background; }

    public void setBackground(Colour bg)
    {
        this.background = bg;
        for(int i = 0; i < width; ++i)
        {
            for(int j = 0; j < height; ++j)
            {
                if(isEmpty(i, j))
                    set(i, j, background);
            }
        }
    }

    public ColorGrid(Grid board, int width, int height, Pos pos, Pos arraypos, Colour brush, Colour background)
    {
        this.board = board == null ? this : board;
        this.width = width;
        this.height = height;
        this.brush = brush;
        this.arrayPos = arraypos.copy();
        this.pos = pos.copy();
        grid = new Colour[height][width];

        this.background = background;
        set(Colour.None);
    }

    public ColorGrid(Grid board, int width, int height, Pos pos, Pos arraypos, Colour brush)
    {
        this(board, width, height, pos, arraypos, brush, Colour.Black);
    }

    public ColorGrid(ColorGrid grid)
    {
        this(grid.board, grid.width, grid.height, grid.pos, grid.arrayPos, grid.brush);
    }

    public ColorGrid(Grid board, int width, int height)
    {
        this(board, width, height, new Pos(0, 0), new Pos(0, 0), Colour.None);
    }

    public ColorGrid(Grid board, String bitString, int width)
    {
        this(board, width, bitString.length() / width);

        int charIdx  = 0;
        for(int j = 0; j < height; ++j)
        {
            for(int i = 0; i < width; ++i)
            {
                char ch = charIdx < bitString.length() ? bitString.charAt(charIdx) : '0';

                int colorIndex = ch - '0';
                set(i, j, Colour.toColour(colorIndex));

                ++charIdx;
            }
        }
    }

    public void setBrush(Colour c)
    {
        brush = c;
    }

    public void resize(int width, int height)
    {
        this.width = width;
        this.height = height;
        grid = new Colour[height][width];
    }

    /* These constructors are taking raw data so are going to be the original 'board' */
    public ColorGrid(int[][] values)
    {
        this(null, values.length == 0 ? 0 : values[0].length, values.length);
        board = this;
        for(int y = 0; y < height; ++y)
            for(int x = 0; x < width; ++x)
                grid[y][x] = Colour.toColour(values[y][x]);
    }

    public ColorGrid(List<List<Integer>> values)
    {
        this(null, values.size() == 0 ? 0 : values.get(0).size(), values.size());
        board = this;

        for(int y = 0; y < height; ++y)
            for(int x = 0; x < width; ++x)
                grid[y][x] = Colour.toColour(values.get(y).get(x));
    }

    public void set(int x, int y, int c)
    {
        set(x, y, Colour.toColour(c));
    }

    public void set(int x, int y, Colour colour)
    {
        if(inBounds(x, y))
            grid[y][x] = colour;
    }

    public void paint(int x, int y)
    {
        set(x, y, Colour.Black);
    }

    public void set(Colour colour)
    {
        for(int y = 0; y < height; ++y)
            for(int x = 0; x < width; ++x)
                grid[y][x] = colour;
    }

    public Colour get(Pos pos)
    {
        return get(pos.x, pos.y);
    }

    public Colour get(int x, int y)
    {
        if(! inBounds(x, y))
            return Colour.None;

        return grid[y][x] == null ? Colour.None : grid[y][x];
    }

    @Override
    public boolean isEmpty(int x, int y)
    {
        return grid[y][x] == null || grid[y][x] == Colour.None || grid[y][x] == background;
    }

    @Override
    public Map<AttrNames, Attribute> getAttributes()
    {
        Map<AttrNames, Attribute> attributes = super.getAttributes();

        attributes.put(AttrNames.Background, new ValueCategoricalAttr<>(getBackground()));
        attributes.put(AttrNames.Centre, new ValueCategoricalAttr<>(getPos()));
        attributes.put(AttrNames.ArrayX, new ValueCategoricalAttr<>(arrayPos.x));
        attributes.put(AttrNames.ArrayY, new ValueCategoricalAttr<>(arrayPos.y));

        return attributes;
    }

    public Mask positive()
    {
        Mask mask = new Mask(getBoard(), width, height, pos, false);
        Array<Pos> arr = Pos.permute(width, height);
        for(Pos p : arr)
        {
            if(isEmpty(p))
                continue;

            mask.paint(p);
        }

        /*
        for(int i = 0; i < width; ++i)
        {
            for(int j = 0; j < height; ++j)
            {
                Colour c = get(i, j);
                if(c == Colour.None || c == background)
                    continue;

                mask.paint(i, j);
            }
        }
        */

        return mask;
    }

    public Array<Mask> colorSplit()
    {
        Array<Mask> array = new Array();
        Map<Colour, Mask> colourToMask = new HashMap<>();

        Array<Pos> arr = Pos.permute(width, height);
        for(Pos p : arr)
        {
            if(isNotEmpty(p))
            {
                Colour c = get(p);
                Mask m = colourToMask.get(c);

                if(m == null)
                {
                    m = new Mask(board, width, height, pos, c);
                    colourToMask.put(c, m);
                }

                m.paint(p);
            }
        }

        colourToMask.forEach((c, m) -> {m.trim(); array.add(m);});
        return array;
    }

    public void draw(ColorGrid grid, Pos pos)
    {
        for(int i = 0; i < grid.getWidth(); ++i)
        {
            for(int j = 0; j < grid.getHeight(); ++j)
            {
                int x = i + pos.x;
                int y = j + pos.y;

                if(inBounds(x, y))
                {
                    Colour value = grid.get(i, j);

                    if(value != Colour.None && value != grid.getBackground())
                        set(x, y, value);
                }
            }
        }
    }

    public void fill(Colour c)
    {
        for(int i = 0; i < width; ++i)
            for(int j = 0; j < height; ++j)
                set(i, j, c);
    }

    public void fill()
    {
        for(int i = 0; i < width; ++i)
            for(int j = 0; j < height; ++j)
                set(i, j, brush);
    }

    public void draw(ColorGrid grid)
    {
        draw(grid, grid.getPos());
    }

    public void draw(Mask grid, Pos pos, Colour c)
    {
        Colour colorToUse = c;
        if(colorToUse == Colour.None)
            colorToUse = brush;
        if(colorToUse == Colour.None)
            colorToUse = getBoard().getBrush();

        Array<Pos> arr = Pos.permute(grid.getWidth(), grid.getHeight());
        for(Pos p : arr)
        {
            Pos pp = p.plus(pos);

            if(inBounds(pp))
                if(grid.get(p))
                    set(pp, colorToUse);
        }

        /*
        for(int i = 0; i < grid.getWidth(); ++i)
        {
            for(int j = 0; j < grid.getHeight(); ++j)
            {
                int x = i + pos.x;
                int y = j + pos.y;

                if(inBounds(x, y))
                {
                    if(grid.get(i, j))
                        set(x, y, colorToUse);
                }
            }
        }
        */
    }

    public void draw(Mask grid, Pos pos)    { draw(grid, pos, grid.brush);  }
    public void draw(Mask grid, Colour c)   { draw(grid, grid.pos, c);      }
    public void draw(Mask grid)             { draw(grid, grid.getPos());    }

    @Override
    public String toString()
    {
        StringBuilder builder = new StringBuilder();

        for(int y = height - 1; y >= 0; --y)
        {
            for(int x = 0; x < width; ++x)
            {
                builder.append(grid[y][x].ordinal());
                builder.append("\t");
            }

            builder.append(System.lineSeparator());
        }

        return builder.toString();
    }

    public <R> R compare(ColorGrid other,
                         BiFunction<Colour, Colour, R> op,
                         BinaryOperator<R> foldFunction,
                         R startValue)
    {
        if(other.getWidth() != width || other.getHeight() != height)
            return startValue;

        R lastValue = startValue;
        for(int i = 0; i < grid.length; ++i)
        {
            for(int j = 0; j < grid[i].length; ++j)
            {
                R cellwiseResult = op.apply(grid[j][i], other.grid[j][i]);
                lastValue = foldFunction.apply(lastValue, cellwiseResult);
            }
        }

        return lastValue;
    }

    public double fractionEqualCells(ColorGrid other)
    {
        int equalCells = compare(other, (a, b) -> a == b ? 0 : 1, Integer::sum, 0);
        return equalCells / (double) (width * height);
    }

    @Override
    public boolean equals(Object other)
    {
        if(! (other instanceof ColorGrid))
            return false;

        ColorGrid g = (ColorGrid) other;
        Boolean allAreEqual = compare(g, (a, b) -> a == b, Boolean::logicalAnd, true);

        return allAreEqual;
    }

    /* This grid's position in a layout */
    public void setArrayPos(int x, int y)
    {
        arrayPos = new Pos(x, y);
    }

    public ColorGrid copy()
    {
        ColorGrid copy = new ColorGrid(board, width, height);

        for(int i = 0; i < width; ++i)
            for(int j = 0; j < height; ++j)
                copy.set(i, j, get(i, j));

        copy.brush = brush;
        copy.pos = pos;
        copy.arrayPos = arrayPos;

        return copy;
    }

    public Array<Mask> shapesDiag()   { return shapes(Pos.neighboursDiag); }
    public Array<Mask> shapes()       { return shapes(Pos.neighbours); }

    public Array<Mask> shapes(Pos[] neighbours)
    {
        Array<Mask> splitByColor = colorSplit();
        Array shapes = new Array();

        for(Mask m : splitByColor)
        {
            Mask visited = m.copy();
            Array<Pos> arr = Pos.permute(m.getWidth(), m.getHeight());

            for(Pos p : arr)
            {
                if(visited.isNotEmpty(p))
                {
                    List<Pos> connected = new ArrayList<>();
                    addConnected(p, visited, connected, neighbours);

                    Mask shape = new Mask(board, connected);
                    shape.setBrush(m.getBrush());
                    shape.setPos(shape.getPos().plus(m.getPos()));
                    shapes.add(shape);
                }
            }
        }

        return shapes;
    }

    private void addConnected(final Pos p, Mask visited, List<Pos> connected, Pos[] neighbours)
    {
        visited.set(p, false);
        connected.add(p);

        for(Pos d : neighbours)
        {
            Pos moved = p.plus(d);
            if(visited.isNotEmpty(moved))
                addConnected(moved, visited, connected, neighbours);
        }
    }

    public static class EntropyData
    {
        public float[] vert, horz;
        public float metaVert, metaHorz;

        EntropyData(int w, int h)
        {
            vert = new float[h];
            horz = new float[w];
        }
    }

    public EntropyData calculateShannonEntropy()
    {
        EntropyData data = new EntropyData(width, height);

        for(int j = 0; j < height; ++j)
        {
            Map<Integer, Integer> vertFreq = new HashMap<>();

            for(int i = 0; i < width; ++i)
                vertFreq.merge(get(i, j).ordinal(), 1, Integer::sum);

            data.vert[j] = shannonEntropy(vertFreq.values(), width);
        }

        Map<Integer, Integer> metaVertFreq = new HashMap<>();

        for(int j = 0; j < height; ++j)
            metaVertFreq.merge((int) Math.round(10000 * data.vert[j]), 1, Integer::sum);

        data.metaVert = shannonEntropy(metaVertFreq.values(), height);

        for(int i = 0; i < width; ++i)
        {
            Map<Integer, Integer> horzFreq = new HashMap<>();

            for(int j = 0; j < height; ++j)
                horzFreq.merge(get(i, j).ordinal(), 1, Integer::sum);

            data.horz[i] = shannonEntropy(horzFreq.values(), height);
        }

        Map<Integer, Integer> metaHorzFreq = new HashMap<>();

        for(int j = 0; j < width; ++j)
            metaHorzFreq.merge((int) Math.round(10000 * data.horz[j]), 1, Integer::sum);

        data.metaHorz = shannonEntropy(metaHorzFreq.values(), width);

        return data;
    }

    private float shannonEntropy(Iterable<Integer> list, int dataLength)
    {
        float entropy = 0.f;
        for(Integer count : list)
        {
            double p = 1.0 * count / dataLength;
            if(count > 0)
                entropy -= p * Math.log(p) / Math.log(2);
        }

        return entropy;
    }

    public EntropyData calculateCellTransitions()
    {
        EntropyData data = new EntropyData(width, height);

        for(int j = 0; j < height; ++j)
        {
            int num = 0;
            Colour last = get(0, j);

            for(int i = 1; i < width; ++i)
            {
                Colour curr = get(i, j);
                if(curr != last)
                    ++num;
                last = curr;
            }
            data.vert[j] = num / (float) width;
        }

        for(int i = 0; i < width; ++i)
        {
            int num = 0;
            Colour last = get(i, 0);

            for(int j = 1; j < height; ++j)
            {
                Colour curr = get(i, j);
                if(curr != last)
                    ++num;
                last = curr;
            }

            data.horz[i] = num / (float) height;
        }

        double last = data.vert[0];
        for(int i = 1; i < data.vert.length; ++i)
        {
            if(data.vert[i] != last)
                ++data.metaVert;
            last = data.vert[i];
        }

        last = data.horz[0];
        for(int i = 1; i < data.horz.length; ++i)
        {
            if(data.horz[i] != last)
                ++data.metaHorz;
            last = data.horz[i];
        }

        data.metaVert /= (double) data.vert.length;
        data.metaHorz /= (double) data.horz.length;

        return data;
    }

    public void dissolveCrosshatch(Colour crosshatch)
    {

    }

    public float[][] toFloat()
    {
        float[][] array = new float[height][width];

        for(int y = 0; y < height; ++y)
            for(int x = 0; x < width; ++x)
                array[y][x] = get(x, y).ordinal();

        return array;
    }
}
