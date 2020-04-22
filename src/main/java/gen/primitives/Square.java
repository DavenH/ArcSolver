package gen.primitives;

import gen.grid.Grid;

public class Square extends Rectangle
{
    public Square(Grid board, int size)
    {
        super(board, new Pos(0, 0), size, size);
    }

    public Square(Grid board, Pos pos, int size)
    {
        super(board, pos, size, size);
    }

}
