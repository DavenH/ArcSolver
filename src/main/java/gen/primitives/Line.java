package gen.primitives;

import gen.grid.Grid;
import gen.grid.Mask;
import gen.priors.abstraction.Symmetry;
import gen.priors.spatial.Vector;

public class Line extends Mask
{
    private final int thickness;
    public Vector vector;

    public Line(Grid board, Vector vector)
    {
        this(board, vector, 1);
        this.vector = vector;
    }

    public Line(Grid board, Vector vector, int thickness)
    {
        super(board,
              Symmetry.fromCompass(vector.direction) == Symmetry.Vert ? 1 : vector.length,
              Symmetry.fromCompass(vector.direction) == Symmetry.Horz ? 1 : vector.length);

        this.vector = vector;
        this.thickness = thickness;
    }

    public Vector getVector()
    {
        return vector;
    }

    public int getLength()
    {
        return Math.max(width, height);
    }

    public int getThickness()
    {
        return thickness;
    }
}
