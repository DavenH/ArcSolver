package gen.grid;

import gen.primitives.Colour;
import gen.primitives.Pos;
import gen.priors.abstraction.Symmetry;
import gen.priors.adt.Array;

import java.util.*;
import java.util.function.BiFunction;

public class ColorGrid implements Grid<Colour>
{
    public Pos pos, arrayPos;
    public int width, height;

    Grid board;
    Colour brush, background;
    Colour[][] grid;

    @Override public int getWidth()        { return width; }
    @Override public int getHeight()       { return height; }
    @Override public int getDim(int index) { return index == 0 ? width : height; }
    @Override public Pos getPos()          { return pos; }
    @Override public void setPos(Pos pos)  { this.pos = pos; }
    @Override public Grid getBoard()       { return board; }
    @Override public Colour getBrush()     { return brush; }
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

    @Override
    public Set<Symmetry> getSymmetries()
    {
        // TODO
        return Collections.emptySet();
    }

    public ColorGrid(Grid board, int width, int height)
    {
        this.board = board == null ? this : board;
        this.width = width;
        this.height = height;
        grid = new Colour[height][width];

        set(Colour.None);

        brush = Colour.None;
        background = Colour.Black;

        pos = new Pos(0, 0);
        arrayPos = new Pos(0, 0);
    }

    public ColorGrid(Grid board, String bitString, int width)
    {
        this(board, width, bitString.length() / width);

        int charIdx  = 0;
        for(int i = 0; i < width; ++i)
        {
            for(int j = 0; j < height; ++j)
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

    public Mask positive()
    {
        Mask mask = new Mask(getBoard(), width, height);
        mask.setPos(pos.copy());
        mask.set(false);

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

        return mask;
    }

    public Array colorSplit()
    {
        Array array = new Array();
        Map<Colour, Mask> colourToMask = new HashMap<>();

        for(int i = 0; i < width; ++i)
        {
            for(int j = 0; j < height; ++j)
            {
                if(isNotEmpty(i, j))
                {
                    Colour c = get(i, j);
                    Mask m = colourToMask.get(c);
                    if(m == null)
                    {
                        m = new Mask(board, width, height, c);
                        colourToMask.put(c, m);
                    }

                    m.paint(i, j);
                }
            }
        }

        colourToMask.forEach((c, m) -> array.add(m.trim()));
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
    }

    public void draw(Mask grid, Pos pos)
    {
        draw(grid, pos, grid.brush);
    }

    public void draw(Mask grid, Colour c)
    {
        draw(grid, grid.pos, c);
    }

    public void draw(Mask grid)
    {
        Pos pos = grid.getPos();
        draw(grid, pos);
    }

    @Override
    public String toString()
    {
        StringBuilder builder = new StringBuilder();

        for(int y = height - 1; y >= 0; --y)
        {
            for(int x = 0; x < width; ++x)
            {
                builder.append(grid[y][x].toString(), 0, 3);
                builder.append("\t");
            }

            builder.append(System.lineSeparator());
        }

        return builder.toString();
    }

    public <R> R compare(ColorGrid other,
                         BiFunction<Colour, Colour, R> op,
                         BiFunction<R, R, R> foldFunction,
                         R startValue)
    {
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

    public Array shapesDiag() { return shapes(Pos.neighboursDiag); }
    public Array shapes() { return shapes(Pos.neighbours); }

    public Array shapes(Pos[] neighbours)
    {
        Array splitByColor = colorSplit();
        Array array = new Array();

        for(Object obj : splitByColor)
        {
            Mask m = (Mask) obj;
            Mask visited = m.copy();

            for(int i = 0; i < m.getWidth(); ++i)
            {
                for(int j = 0; j < m.getHeight(); ++j)
                {
                    if(visited.isNotEmpty(i, j))
                    {
                        List<Pos> connected = new ArrayList<>();
                        addConnected(new Pos(i, j), visited, connected, neighbours);

                        Mask shape = new Mask(board, connected);
                        shape.setBrush(m.getBrush());
                        array.add(shape);
                    }
                }
            }
        }

        return array;
    }

    private void addConnectedDiag(Pos p, Mask visited, List<Pos> connected)
    {
        addConnected(p, visited, connected, Pos.neighboursDiag);
    }

    private void addConnected(Pos p, Mask visited, List<Pos> connected, Pos[] neighbours)
    {
        visited.set(p, false);
        connected.add(p);

        for(Pos d : neighbours)
        {
            Pos moved = p.plus(d);
            if(visited.isNotEmpty(moved))
                addConnectedDiag(moved, visited, connected);
        }
    }

    public static class EntropyData
    {
        public double[] vert, horz;
        public double metaVert, metaHorz;

        EntropyData(int w, int h)
        {
            vert = new double[h];
            horz = new double[w];
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

    private double shannonEntropy(Iterable<Integer> list, int dataLength)
    {
        double entropy = 0.0;
        for(Integer count : list)
        {
            double p = 1.0 * count / dataLength;
            if(count > 0)
                entropy -= p * Math.log(p) / Math.log(2);
        }

        return entropy;
    }

    public static void main(String[] args)
    {
        ColorGrid grid = new ColorGrid(null, "102394856109341093461039418341236041000000000008748390123491049384710234817341", 9);
        ColorGrid.EntropyData entropy = grid.calculateShannonEntropy();

        System.out.println("vert:");
        for(int i = 0; i < entropy.vert.length; ++i)
            System.out.println(entropy.vert[i]);

        System.out.println("horz:");
        for(int i = 0; i < entropy.horz.length; ++i)
            System.out.println(entropy.horz[i]);

        System.out.println("Meta vert: " + entropy.metaVert);
        System.out.println("Meta horz: " + entropy.metaHorz);
    }
}
