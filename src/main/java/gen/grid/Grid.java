package gen.grid;

import gen.primitives.Colour;
import gen.primitives.Pos;
import gen.priors.abstraction.*;
import gen.priors.adt.Array;
import gen.priors.spatial.Compass;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public abstract class Grid<T>
{
    protected T[][] grid;
    protected int width, height;
    protected Grid board;
    protected Pos pos;
    protected Array<Grid> children = new Array<>();
    protected Colour brush;

    // for conciseness using dsl
    public int w() { return getWidth(); }
    public int h() { return getHeight(); }

    public int getWidth()        { return width; }
    public int getHeight()       { return height; }
    public Colour getBrush()     { return brush; }
    public Pos getPos()          { return pos; }
    public int getDim(int index) { return index == 0 ? getWidth() : getHeight(); }
    public Pos getTopRight()     { return getPos().plus(getWidth(), getHeight()); }

    public void addChild(Grid child) { this.children.add(child); }
    public void setPos(Pos pos)  { this.pos = pos; }
    public void setPos(int x, int y)  { setPos(new Pos(x, y)); }
    public void set(Pos pos, T value) { set(pos.x, pos.y, value); }

    public abstract Grid<T> cloneInstance(int w, int h);
    public abstract void set(int x, int y, T value);
    public abstract void set(T value);
    public abstract void paint(int x, int y);

    public int rightmostIndex() { return getWidth() - 1; }
    public int topmostIndex() { return getHeight() - 1; }

    public abstract T get(int x, int y);
    public T get(Pos p) { return get(p.x, p.y); }

    public Grid getBoard() { return board; }
    public abstract void resize(int w, int h);

    public int countPositive()
    {
        int count = 0;
        for(int j = 0; j < getHeight(); ++j)
            for(int i = 0; i < getWidth(); ++i)
                if(isNotEmpty(i, j))
                    ++count;
        return count;
    }

    public abstract boolean isEmpty(int x, int y);

    public boolean isEmpty(Pos xy)             { return isEmpty(xy.x, xy.y); }
    public boolean isNotEmpty(Pos p)           { return inBounds(p) && ! isEmpty(p); }
    public boolean isNotEmpty(int x, int y)    { return inBounds(x, y) && ! isEmpty(x, y); }
    public boolean inBounds(Pos xy)            { return inBounds(xy.x, xy.y); }

    public Array<Grid> getChildren() { return children; }

    public Map<AttrNames, Attribute> getAttributes()
    {
        Map<AttrNames, Attribute> attributes = new HashMap<>();

        Set<Symmetry> symmetries = getSymmetries();
        attributes.put(AttrNames.SymmetrySet, new ValueCategoricalAttr<>(symmetries));
        attributes.put(AttrNames.ShapeHash, new ShapeHashAttr(this));
        attributes.put(AttrNames.X, new ValueCategoricalAttr<>(pos.x));
        attributes.put(AttrNames.Y, new ValueCategoricalAttr<>(pos.y));
        attributes.put(AttrNames.W, new ValueCategoricalAttr<>(width));
        attributes.put(AttrNames.H, new ValueCategoricalAttr<>(height));
        attributes.put(AttrNames.Colour, new ValueCategoricalAttr<>(getBrush()));
        attributes.put(AttrNames.Centre, new ValueCategoricalAttr<>(getPos()));

        return attributes;
    }

    public Set<Symmetry> getSymmetries()
    {
        Set<Symmetry> syms = new HashSet<>();

        if(equals(reflect(Symmetry.Horz)))
            syms.add(Symmetry.Horz);
        if(equals(reflect(Symmetry.Vert)))
            syms.add(Symmetry.Vert);

        if(getWidth() == getHeight())
        {
            if(equals(reflect(Symmetry.Diag)))
                syms.add(Symmetry.Diag);
            if(equals(reflect(Symmetry.NegDiag)))
                syms.add(Symmetry.NegDiag);
        }

        // translational symmetries
        {

        }

        return syms;
    }

    public Grid<T> reflect(Symmetry sym)
    {
        Grid<T> m = (sym.isDiagonal()) ?
                    cloneInstance(getHeight(), getWidth()) :
                    cloneInstance(getWidth(), getHeight());

        int w = getWidth();
        int h = getHeight();
        Array<Pos> arr = Pos.permute(w, h);

        // surely this can be sped up with System.arrayCopy in certain cases.
        for(Pos p : arr)
        {
            switch (sym)
            {
                case Vert:      m.set(p,             get(p.fromTop(h)));        break;
                case Horz:      m.set(p,             get(p.fromRight(w)));      break;
                case Diag:      m.set(p.transpose(), get(p));                   break;
                case NegDiag:   m.set(p.transpose(), get(p.fromTopRight(w, h)));break;
            }
        }

        return m;
    }

    public Grid<T> rotate(int quartersCW)
    {
        if(quartersCW == 0)
            return this;

        quartersCW = quartersCW % 4;
        Grid<T> m = (quartersCW == 2) ?
                    cloneInstance(getWidth(), getHeight()) :
                    cloneInstance(getHeight(), getWidth());

        int w = getWidth();
        int h = getHeight();

        Array<Pos> arr = Pos.permute(w, h);

        if(quartersCW == 2)
        {
            for(Pos p : arr)
                m.set(p, get(p.fromTopRight(w, h)));
        }
        else
        {
            if(quartersCW == 3)
            {
                for(Pos p : arr)
                    m.set(p.transpose(), get(p.fromTop(h)));
            }
            else if(quartersCW == 1)
            {
                for(Pos p : arr)
                    m.set(p.transpose(), get(p.fromRight(w)));
            }
        }

        return m;
    }

    public boolean inBounds(int x, int y)
    {
        return x >= 0 && x < getWidth() && y >= 0 && y < getHeight();
    }

    public Mask sides()
    {
        return horzSides().or(vertSides());
    }

    public Mask horzSides()
    {
        Mask m = new Mask(getBoard(), getWidth(), getHeight(), getPos(), false);

        for(int i = 0; i < m.getHeight(); ++i)
        {
            m.paint(0, i);
            m.paint(m.rightmostIndex(), i);
        }

        return m;
    }

    public Mask vertSides()
    {
        Mask m = new Mask(getBoard(), getWidth(), getHeight(), getPos(), false);

        for(int i = 0; i < m.getHeight(); ++i)
        {
            m.paint(i, 0);
            m.paint(i, m.topmostIndex());
        }

        return m;
    }

    public Mask above()
    {
        int[] numEmptyCellsFromTop = new int[getWidth()];
        int topMaskIdx = getHeight() - 1;
        int maxVoidHeight = 0;

        for(int x = 0; x < getWidth(); ++x)
        {
            int y = topMaskIdx;
            while(isEmpty(x, y) && y > 0)
                --y;

            numEmptyCellsFromTop[x] = topMaskIdx - y;
            maxVoidHeight = Math.max(numEmptyCellsFromTop[x], maxVoidHeight);
        }

        int cellsAboveSubject = getBoard().getHeight() - getTopRight().y;
        int heightOfAboveMask = cellsAboveSubject + maxVoidHeight;

        Mask mask = new Mask(getBoard(),
                             getWidth(),
                             heightOfAboveMask,
                             getPos().plus(0, getHeight() - maxVoidHeight));

        for(int x = 0; x < mask.getWidth(); ++x)
        {
            if(numEmptyCellsFromTop[x] != getHeight())
                for(int y = maxVoidHeight - numEmptyCellsFromTop[x]; y < mask.getHeight(); ++y)
                    mask.paint(x, y);
        }

        return mask;
    }

    public Mask below()
    {
        int[] numEmptyCellsFromBottom = new int[getWidth()];
        int botMaskIdx = 0;
        int maxVoidHeight = 0;

        for(int x = 0; x < getWidth(); ++x)
        {
            int y = botMaskIdx;
            while(isEmpty(x, y) && y < topmostIndex())
                ++y;

            numEmptyCellsFromBottom[x] = y;
            maxVoidHeight = Math.max(numEmptyCellsFromBottom[x], maxVoidHeight);
        }

        int cellsBelowSubject = getPos().y;
        int heightOfBelowMask = cellsBelowSubject + maxVoidHeight;

        Mask mask = new Mask(getBoard(),
                             getWidth(),
                             heightOfBelowMask,
                             new Pos(getPos().x, 0));

        for(int x = 0; x < mask.getWidth(); ++x)
        {
            for(int y = 0; y < numEmptyCellsFromBottom[x] + cellsBelowSubject; ++y)
                mask.paint(x, y);
        }

        return mask;
    }

    public Mask leftOf()
    {
        int[] numEmptyCellsFromLeft = new int[getHeight()];
        int maxVoidWidth = 0;

        for(int y = 0; y < getHeight(); ++y)
        {
            int x = 0;
            while(isEmpty(x, y) && x < rightmostIndex())
                ++x;

            numEmptyCellsFromLeft[y] = x;
            maxVoidWidth = Math.max(numEmptyCellsFromLeft[y], maxVoidWidth);
        }

        int cellsLeftOfSubject = getPos().x;
        int widthOfLeftOfMask = cellsLeftOfSubject + maxVoidWidth;

        Pos leftOfPos = new Pos(0, getPos().y);
        Mask mask = new Mask(getBoard(), widthOfLeftOfMask, getHeight());
        mask.setPos(leftOfPos);

        for(int y = 0; y < mask.getHeight(); ++y)
        {
            for(int x = 0; x < numEmptyCellsFromLeft[y] + cellsLeftOfSubject; ++x)
                mask.paint(x, y);
        }

        return mask;
    }

    public Mask rightOf()
    {
        int[] numEmptyCellsFromRight = new int[getHeight()];
        int maxVoidWidth = 0;

        for(int y = 0; y < getHeight(); ++y)
        {
            int x = rightmostIndex();
            while(isEmpty(x, y) && x > 0)
                --x;

            numEmptyCellsFromRight[y] = rightmostIndex() - x;
            maxVoidWidth = Math.max(numEmptyCellsFromRight[y], maxVoidWidth);
        }

        int cellsRightOfSubject = getBoard().getWidth() - getTopRight().x;
        int widthOfRightOfMask = cellsRightOfSubject + maxVoidWidth;

        Pos rightOfPos = getPos().plus(getWidth() - maxVoidWidth, 0);
        Mask output = new Mask(getBoard(), widthOfRightOfMask, getHeight());
        output.setPos(rightOfPos);

        for(int y = 0; y < output.getHeight(); ++y)
        {
            for(int x = maxVoidWidth - numEmptyCellsFromRight[y]; x < output.getWidth(); ++x)
                output.paint(x, y);
        }

        return output;
    }

    public Mask around()
    {
        Mask mask = new Mask(getBoard(),
                             getWidth() + 2,
                             getHeight() + 2,
                             getPos().minus(1, 1),
                             false);

        Array<Pos> perms = Pos.permute(getWidth(), getHeight());
        for(Pos p : perms)
        {
            if(isNotEmpty(p))
            {
                mask.paint(p);

                for(Pos dir : Pos.neighboursDiag)
                    mask.paint(p.plus(dir).plus(1, 1));
            }
        }

        for(Pos p : perms)
        {
            if(isNotEmpty(p))
                mask.set(p.plus(1, 1), false);
        }

        return mask;
    }

    public void getCellsFromEdge(Compass compass)
    {
        // for each unit along the edge of the board that is in the compass direction provided

        // approach this mask perpendicularly until you hit a positive cell, counting the cells

        //
    }

    public Mask getExtent(Compass direction)
    {
        switch (direction)
        {
            case N: return top();
            case S: return bottom();
            case E: return right();
            case W: return left();
        }

        throw new RuntimeException("Direction " + direction + " not a viable extent.");
    }

    public Mask top()
    {
        int[] numEmptyCellsFromTop = new int[getWidth()];
        int maxVoidHeight = 0;

        for(int x = 0; x < getWidth(); ++x)
        {
            int y = topmostIndex();
            while(isEmpty(x, y) && y > 0)
                --y;

            numEmptyCellsFromTop[x] = topmostIndex() - y;
            maxVoidHeight = Math.max(numEmptyCellsFromTop[x], maxVoidHeight);
        }

        Mask mask = new Mask(getBoard(),
                             getWidth(),
                             maxVoidHeight + 1,
                             getPos().plus(0, topmostIndex() - maxVoidHeight),
                             getBrush());

        for(int x = 0; x < getWidth(); ++x)
            mask.paint(x, maxVoidHeight - numEmptyCellsFromTop[x]);

        return mask;
    }

    public Mask bottom()
    {
        int[] numEmptyCellsFromBottom = new int[getWidth()];
        int maxVoidHeight = 0;

        for(int x = 0; x < getWidth(); ++x)
        {
            int y = 0;
            while(isEmpty(x, y) && y < topmostIndex())
                ++y;

            numEmptyCellsFromBottom[x] = y;
            maxVoidHeight = Math.max(numEmptyCellsFromBottom[x], maxVoidHeight);
        }

        Mask mask = new Mask(getBoard(),
                             getWidth(),
                             maxVoidHeight + 1,
                             getPos(),
                             getBrush());

        for(int x = 0; x < getWidth(); ++x)
            mask.paint(x, numEmptyCellsFromBottom[x]);

        return mask;
    }

    public Mask left()
    {
        int[] numEmptyCellsFromLeft = new int[getHeight()];
        int maxVoidWidth = 0;

        for(int y = 0; y < getHeight(); ++y)
        {
            int x = 0;
            while(isEmpty(x, y) && x < getWidth() - 1)
                ++x;

            numEmptyCellsFromLeft[y] = x;
            maxVoidWidth = Math.max(numEmptyCellsFromLeft[y], maxVoidWidth);
        }

        Mask mask = new Mask(getBoard(),
                             maxVoidWidth + 1,
                             getHeight(),
                             getPos(),
                             getBrush());

        for(int y = 0; y < getHeight(); ++y)
            mask.paint(numEmptyCellsFromLeft[y], y);

        return mask;
    }

    public Mask right()
    {
        int[] numEmptyCellsFromRight = new int[getHeight()];
        int rightMaskIdx = rightmostIndex();
        int maxVoidWidth = 0;

        for(int y = 0; y < getHeight(); ++y)
        {
            int x = rightMaskIdx;
            while(isEmpty(x, y) && x > 0)
                --x;

            numEmptyCellsFromRight[y] = rightMaskIdx - x;
            maxVoidWidth = Math.max(numEmptyCellsFromRight[y], maxVoidWidth);
        }

        Mask mask = new Mask(getBoard(),
                             maxVoidWidth + 1,
                             getHeight(),
                             getPos().plus(rightmostIndex() - maxVoidWidth, 0),
                             getBrush());

        for(int y = 0; y < getHeight(); ++y)
            mask.paint(maxVoidWidth - (numEmptyCellsFromRight[y]), y);

//        for(int x = 0; x < getWidth(); ++x)
//            mask.paint(x, maxVoidHeight - numEmptyCellsFromTop[x]);
        return mask;
    }

    public Mask perimeter()
    {
        return top().or(left()).or(bottom()).or(right());
    }


    public Mask betweenHorz(Grid other)
    {
        if(other.centre().x < centre().x)
        {
            Mask leftOf1 = leftOf();
            Mask rightOf1 = other.rightOf();
            Mask between = leftOf1.and(rightOf1);
            between.trim();
            return between;
        }
        else
        {
            Mask leftOf1 = other.leftOf();
            Mask rightOf1 = rightOf();

            Mask between = leftOf1.and(rightOf1);
            between.trim();
            return between;
        }
    }

    public Mask betweenVert(Grid other)
    {
        if(other.centre().y < centre().y)
        {
            Mask below = below();
            Mask above = other.above();

            return above.and(below);
        }
        else
        {
            Mask below = other.below();
            Mask above = above();

            return above.and(below);
        }
    }

    public Mask corners()
    {
        Mask m = new Mask(getBoard(), getWidth(), getHeight(), getPos(), false);

        m.paint(0, 0);
        m.paint(rightmostIndex(), topmostIndex());
        m.paint(rightmostIndex(), 0);
        m.paint(0, topmostIndex());

        return m;
    }

    public Mask between(Grid g)
    {
        return betweenHorz(g).or(betweenVert(g));
    }

    public Pos centre()
    {
        return getPos().plus(getWidth() / 2, getHeight() / 2);
    }


    public boolean boundsContain(Grid grid)
    {
        Pos bl = getPos();
        Pos tr = getTopRight();
        Pos gridBl = grid.getPos();
        Pos gridTr = grid.getTopRight();

        if(bl.x > gridBl.x || bl.y > gridBl.y ||
           tr.x < gridTr.x || tr.y < gridTr.y)
            return false;

        return true;
    }


    public boolean contains(Pos pos)
    {
        Pos moved = pos.minus(getPos());

        Array<Pos> perms = Pos.permute(getWidth(), getHeight());
        for(Pos p : perms)
        {
            if(isNotEmpty(p) && p.equals(moved))
                return true;
        }

        return false;
    }


    public boolean equals(Grid grid)
    {
        if(getWidth() != grid.getWidth() || getHeight() != grid.getHeight())
            return false;

        Array<Pos> perms = Pos.permute(getWidth(), getHeight());
        for(Pos p : perms)
        {
            if(! get(p).equals(grid.get(p)))
                return false;
        }

        return true;
    }

    public int hash()
    {
        int hash = 109238457;
        for(int i = 0; i < getWidth(); ++i)
            for(int j = 0; j < getHeight(); ++j)
                hash *= get(i, j).hashCode() *
                        ((i + 1) * -432141) + (j + 1) * 7751013;

        return hash;
    }

    public Grid<T> scaled(int multiple)
    {
        if(multiple == 1)
            return this;


        int w = multiple * getWidth();
        int h = multiple * getHeight();
        Grid<T> scaledClone = cloneInstance(w, h);
        scaledClone.setPos(getPos());

        for(int i = 0; i < w; ++i)
        {
            for(int j = 0; j < h; ++j)
            {
                scaledClone.set(i, j, get(i / multiple, j / multiple));
            }
        }

        return scaledClone;
    }

    public void trim()
    {
        int x = 0, y = 0;
        int width = getWidth();
        int height = getHeight();

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

        T[][] oldGrid = grid;
        resize(newWidth, newHeight);

        for(x = 0; x < newWidth; ++x)
            for(y = 0; y < newHeight; ++y)
                grid[y][x] = oldGrid[y + fromBotY][x + fromLeftX];

        setPos(getPos().plus(fromLeftX, fromBotY));
    }

    public abstract float[][] toFloat();
}
