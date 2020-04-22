package gen.primitives;

import gen.grid.Grid;

public class Dot extends Square
{
    public Dot(Grid board)
    {
        super(board, new Pos(0, 0), 1);
    }

    public Dot(Grid board, Pos pos)
    {
        super(board, pos, 1);
    }
}
