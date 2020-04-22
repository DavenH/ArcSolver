package gen.grid;

import gen.primitives.Colour;
import gen.primitives.Pos;
import gen.priors.abstraction.Symmetry;
import gen.priors.adt.Array;

import java.util.Set;

public interface Grid<T>
{
    int getWidth();
    int getHeight();
    default int getDim(int index) { return index == 0 ? getWidth() : getHeight(); }
    Pos getPos();
    default Pos getTopRight() { return getPos().plus(getWidth(), getHeight()); }
    void setPos(Pos pos);

    default void set(Pos pos, T value) { set(pos.x, pos.y, value); }
    void set(int x, int y, T value);
    void set(T value);
    void paint(int x, int y);

    default int rightmostIndex() { return getWidth() - 1; }
    default int topmostIndex() { return getHeight() - 1; }

    default T get(Pos p) { return get(p.x, p.y); }
    T get(int x, int y);

    Grid getBoard();

    boolean isEmpty(int x, int y);

    default boolean isEmpty(Pos xy)             { return isEmpty(xy.x, xy.y); }
    default boolean isNotEmpty(Pos p)           { return ! isEmpty(p); }
    default boolean isNotEmpty(int x, int y)    { return ! isEmpty(x, y); }
    default boolean inBounds(Pos xy)            { return inBounds(xy.x, xy.y); }

    Set<Symmetry> getSymmetries();

    default boolean inBounds(int x, int y)
    {
        return x >= 0 && x < getWidth() && y >= 0 && y < getHeight();
    }

    default Mask sides()
    {
        Mask m = new Mask(getBoard(), getWidth(), getHeight(), getPos(), false);

        for(int i = 0; i < m.getWidth(); ++i)   m.paint(i, 0);
        for(int i = 0; i < m.getWidth(); ++i)   m.paint(i, m.topmostIndex());
        for(int j = 0; j < m.getHeight(); ++j)  m.paint(0, j);
        for(int j = 0; j < m.getHeight(); ++j)  m.paint(m.rightmostIndex(), j);
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

        Pos abovePos = getPos().plus(0, getHeight() - maxVoidHeight);
        Mask mask = new Mask(getBoard(), getWidth(), heightOfAboveMask);

        mask.setPos(abovePos);

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

        Pos belowPos = new Pos(getPos().x, 0);
        Mask mask = new Mask(getBoard(), getWidth(), heightOfBelowMask);
        mask.setPos(belowPos);

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
            while(isEmpty(x, y) && x < getWidth() - 1)
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

        Array perms = Pos.permute(getWidth(), getHeight());
        for(Object obj : perms)
        {
            Pos p = (Pos) obj;
            if(isNotEmpty(p))
            {
                mask.paint(p);

                for(Pos dir : Pos.neighboursDiag)
                    mask.paint(p.plus(dir).plus(1, 1));
            }
        }

        for(Object obj : perms)
        {
            Pos p = (Pos) obj;
            if(isNotEmpty(p))
                mask.set(p.plus(1, 1), false);
        }

        return mask;
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

        Mask mask = new Mask(getBoard(), getWidth(), maxVoidHeight);

        for(int x = 0; x < getWidth(); ++x)
            paint(x, topmostIndex() - numEmptyCellsFromTop[x]);

        return mask;
    }

    default Mask bottom()
    {
        int[] numEmptyCellsFromBottom = new int[getWidth()];
        int botMaskIdx = 0;
        int maxVoidHeight = 0;

        for(int x = 0; x < getWidth(); ++x)
        {
            int y = botMaskIdx;
            while(isEmpty(x, y) && y < getHeight() - 1)
                ++y;

            numEmptyCellsFromBottom[x] = y;
            maxVoidHeight = Math.max(numEmptyCellsFromBottom[x], maxVoidHeight);
        }

        Mask mask = new Mask(getBoard(), getWidth(), maxVoidHeight);

        for(int x = 0; x < getWidth(); ++x)
            paint(x, numEmptyCellsFromBottom[x] + 1);

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


        Mask mask = new Mask(getBoard(), maxVoidWidth, getHeight());

        for(int y = 0; y < getHeight(); ++y)
            paint(numEmptyCellsFromLeft[y], y);

        return mask;
    }

    default Mask right()
    {
        int[] numEmptyCellsFromRight = new int[getHeight()];
        int rightMaskIdx = getWidth() - 1;
        int maxVoidWidth = 0;

        for(int y = 0; y < getHeight(); ++y)
        {
            int x = rightMaskIdx;
            while(isEmpty(x, y) && x > 0)
                --x;

            numEmptyCellsFromRight[y] = rightMaskIdx - x;
            maxVoidWidth = Math.max(numEmptyCellsFromRight[y], maxVoidWidth);
        }

        Mask mask = new Mask(getBoard(), maxVoidWidth, getHeight());

        for(int y = 0; y < getHeight(); ++y)
            paint(rightMaskIdx - numEmptyCellsFromRight[y], y);

        return mask;
    }

    default Mask perimeter()
    {
        Mask mask = new Mask(getBoard(), getWidth(), getHeight(), getPos(), false);
        Array perms = Pos.permute(getWidth(), getHeight());

        for(Object obj : perms)
        {
            Pos p = (Pos) obj;
            if(isNotEmpty(p))
            {
                for(Pos dir : Pos.neighboursDiag)
                {
                    if(isEmpty(dir))
                    {
                        mask.paint(p);
                        break;
                    }
                }
            }
        }

        return mask;
    }

    default Mask betweenHorz(Grid g)
    {
        if(g.centre().x < centre().x)
        {
            Mask leftOf1 = g.leftOf();
            Mask rightOf1 = rightOf();

            return leftOf1.and(rightOf1).trim();
        }
        else
        {
            Mask leftOf1 = leftOf();
            Mask rightOf1 = g.rightOf();

            return leftOf1.and(rightOf1).trim();
        }
    }

    default Mask betweenVert(Grid g)
    {
        if(g.centre().y < centre().y)
        {
            Mask below = below();
            Mask above = g.above();

            return above.and(below);
        }
        else
        {
            Mask below = g.below();
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
        Array perms = Pos.permute(getWidth(), getHeight());
        Pos moved = pos.minus(getPos());

        for(Object obj : perms)
        {
            Pos p = (Pos) obj;
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

        Array perms = Pos.permute(getWidth(), getHeight());
        for(Object obj : perms)
        {
            Pos p = (Pos) obj;
            if(! get(p).equals(grid.get(p)))
                return false;
        }

        return true;
    }

    // public Between between(Area other) { return new Between(this, other); }
    // attempting to generalize... sigh
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
                   subject.getPosition().getDim(dimension) :
                   board.getDim(dimension) - subject.getTopRight().getDim(dimension);

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
