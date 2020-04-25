package gen.grid;

import gen.primitives.Colour;
import gen.primitives.Pos;
import gen.priors.abstraction.AttrNames;
import gen.priors.abstraction.Attribute;
import gen.priors.abstraction.Symmetry;
import gen.priors.adt.Array;
import gen.priors.spatial.Compass;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public interface Grid<T>
{
    // for conciseness
    default int w() { return getWidth(); }
    default int h() { return getHeight(); }

    int getWidth();
    int getHeight();
    default int getDim(int index) { return index == 0 ? getWidth() : getHeight(); }
    Pos getPos();
    default Pos getTopRight() { return getPos().plus(getWidth(), getHeight()); }
    void setPos(Pos pos);

    default void setPos(int x, int y)  { setPos(new Pos(x, y)); }
    default void set(Pos pos, T value) { set(pos.x, pos.y, value); }

    Grid<T> cloneInstance(int h, int w);

    void set(int x, int y, T value);
    void set(T value);
    void paint(int x, int y);

    default int rightmostIndex() { return getWidth() - 1; }
    default int topmostIndex() { return getHeight() - 1; }

    default T get(Pos p) { return get(p.x, p.y); }
    T get(int x, int y);

    Grid getBoard();

    default int countPositive()
    {
        int count = 0;
        for(int j = 0; j < getHeight(); ++j)
            for(int i = 0; i < getWidth(); ++i)
                if(isNotEmpty(i, j))
                    ++count;
        return count;
    }

    boolean isEmpty(int x, int y);

    default boolean isEmpty(Pos xy)             { return isEmpty(xy.x, xy.y); }
    default boolean isNotEmpty(Pos p)           { return inBounds(p) && ! isEmpty(p); }
    default boolean isNotEmpty(int x, int y)    { return inBounds(x, y) && ! isEmpty(x, y); }
    default boolean inBounds(Pos xy)            { return inBounds(xy.x, xy.y); }

    Map<AttrNames, Attribute> getAttributes();

    default Set<Symmetry> getSymmetries()
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

    default Grid<T> reflect(Symmetry sym)
    {
        Grid<T> m = (sym.isDiagonal()) ?
                    cloneInstance(getHeight(), getWidth()) :
                    cloneInstance(getWidth(), getHeight());

        int w = getWidth();
        int h = getHeight();
        Array<Pos> arr = Pos.permute(w, h);

        for(Pos p : arr)
        {
            switch (sym)
            {
                case Vert:      m.set(p,             get(p.fromTop(h)));
                case Horz:      m.set(p,             get(p.fromRight(w)));
                case Diag:      m.set(p.transpose(), get(p));
                case NegDiag:   m.set(p.transpose(), get(p.fromTopRight(w, h)));
            }
        }

        return m;
    }

    default Grid<T> rotate(int quartersCW)
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
            if(quartersCW == 1)
            {
                for(Pos p : arr)
                    m.set(p.transpose(), get(p.fromTop(h)));
            }
            else if(quartersCW == 3)
            {
                for(Pos p : arr)
                    m.set(p.transpose(), get(p.fromRight(w)));
            }
        }

        return m;
    }

    default boolean inBounds(int x, int y)
    {
        return x >= 0 && x < getWidth() && y >= 0 && y < getHeight();
    }

    default Mask sides()
    {
        return horzSides().or(vertSides());
    }

    default Mask horzSides()
    {
        Mask m = new Mask(getBoard(), getWidth(), getHeight(), getPos(), false);

        for(int i = 0; i < m.getHeight(); ++i)
        {
            m.paint(0, i);
            m.paint(m.rightmostIndex(), i);
        }

        return m;
    }

    default Mask vertSides()
    {
        Mask m = new Mask(getBoard(), getWidth(), getHeight(), getPos(), false);

        for(int i = 0; i < m.getHeight(); ++i)
        {
            m.paint(i, 0);
            m.paint(i, m.topmostIndex());
        }

        return m;
    }

    default Mask above()
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

    default Mask below()
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

    default Mask leftOf()
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

    default Mask rightOf()
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

    default Mask around()
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

    default void getCellsFromEdge(Compass compass)
    {
        // for each unit along the edge of the board that is in the compass direction provided

        // approach this mask perpendicularly until you hit a positive cell, counting the cells

        //
    }

    default Mask getExtent(Compass direction)
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

    default Mask top()
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

    default Mask bottom()
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

    default Mask left()
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

    default Mask right()
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

    default Mask perimeter()
    {
        return top().or(left()).or(bottom()).or(right());
    }


    default Mask betweenHorz(Grid other)
    {
        if(other.centre().x < centre().x)
        {
            Mask leftOf1 = leftOf();
            Mask rightOf1 = other.rightOf();

            return leftOf1.and(rightOf1).trim();
        }
        else
        {
            Mask leftOf1 = other.leftOf();
            Mask rightOf1 = rightOf();

            return leftOf1.and(rightOf1).trim();
        }
    }

    default Mask betweenVert(Grid other)
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

    default Mask corners()
    {
        Mask m = new Mask(getBoard(), getWidth(), getHeight(), getPos(), false);

        m.paint(0, 0);
        m.paint(rightmostIndex(), topmostIndex());
        m.paint(rightmostIndex(), 0);
        m.paint(0, topmostIndex());

        return m;
    }

    default Mask between(Grid g)
    {
        return betweenHorz(g).or(betweenVert(g));
    }

    default Pos centre()
    {
        return getPos().plus(getWidth() / 2, getHeight() / 2);
    }

    default boolean contains(Pos pos)
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

    Colour getBrush();

    default boolean equals(Grid grid)
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

    default int hash()
    {
        int hash = 109238457;
        for(int i = 0; i < getWidth(); ++i)
            for(int j = 0; j < getHeight(); ++j)
                hash *= get(i, j).hashCode() *
                        ((i + 1) * -432147) + (j + 1) * 7751013;

        return hash;
    }

    // public Between between(Area other) { return new Between(this, other); }
    // attempting to generalize... sigh. If positive cells were represented as [x,y] coords all could be rotated, processed, unrotated
    /*
    public int[] getNumEmptyCellsByLine(Grid subjGrid, boolean isVertical, boolean isOrigin)
    {
        int dimension   = isVertical ? 0 : 1;
        int perpDim     = 1 - dimension;
        int length      = subjGrid.getDim(perpDim);
        int perpLength  = subjGrid.getDim(dimension);

        int[] numEmptyCellsFromGridEdge = new int[length];
        int endToNotExceed = isOrigin ? perpLength - 1 : 0;

        // if our direction is from the origin inward, we count up, if not, count down from the edge
        int polarity = isOrigin ? 1 : -1;
        int maxEmptyCells = 0;

        for(int j = 0; j < length; ++j)
        {
            int count = isOrigin ? 0 : endToNotExceed;
            while(subjGrid.isEmpty(isVertical ? count : j, isVertical ? j : count) && polarity * count < polarity * endToNotExceed)
                count += polarity;

            numEmptyCellsFromGridEdge[j] = count;
            maxEmptyCells = Math.max(numEmptyCellsFromGridEdge[j], maxEmptyCells);
        }

        int cellsTowardEdgeOfSubject = isOrigin ?
                                       getPos().get(dimension) :
                                       getBoard().getDim(dimension) - getTopRight().get(dimension);

        int sizeOfMask = cellsTowardEdgeOfSubject + maxEmptyCells;
        int one = perpLength - maxEmptyCells;
        int bot = isVertical ? subject.getPosition().y : one;
        int left = isOrigin ? 0 : isVertical ? one : subject.getPosition().x;

        // origin
        Pos leftOfPos = new Pos(0, subject.getPosition().y);    // vert
        Pos belowPos = new Pos(subject.getPosition().x, 0);     // ! vert

        // ! origin
        Pos rightOfPos = subject.getPosition().plus(one, 0); // vert
        Pos abovePos = subject.getPosition().plus(0, one); // ! vert

        MaskGrid output = new MaskGrid(sizeOfMask, subjGrid.getHeight());
        output.setPos(new Pos(bot, left));

        for(int j = 0; j < length; ++j)
        {
            // if it's equal, then the whole column is empty -- nothing to be 'above'
            if(numEmptyCellsFromGridEdge[j] != length)
            {
                // each column is
                for(int x = 0; x < numEmptyCellsFromGridEdge[j] + cellsTowardEdgeOfSubject; ++x)
                    output.paint(x, j);
            }
        }
    }
*/
}
