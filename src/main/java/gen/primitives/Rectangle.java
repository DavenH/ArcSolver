package gen.primitives;

import gen.grid.Grid;
import gen.grid.Mask;

public class Rectangle extends Mask
{
    public Rectangle(Grid board, Pos pos, int width, int height)
    {
        super(board, width, height, pos, true);
    }

    public Mask horzSides()
    {
        Mask m = new Mask(getBoard(), getWidth(), getHeight(), getPos(), false);

        for(int i = 0; i < height; ++i)
        {
            m.paint(0, i);
            m.paint(width - 1, i);
        }
        return m;
    }

    public Mask vertSides()
    {
        Mask m = new Mask(getBoard(), getWidth(), getHeight(), getPos(), false);

        for(int i = 0; i < width; ++i)
        {
            m.paint(i, 0);
            m.paint(i, height - 1);
        }

        return m;
    }
}
