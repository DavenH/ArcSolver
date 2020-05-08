package gen.primitives;

import gen.grid.Grid;
import gen.grid.Mask;
import gen.priors.abstraction.ShapeAttr;
import gen.priors.abstraction.SymmetryType;
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
              SymmetryType.fromCompass(vector.direction) == SymmetryType.Vert ? 1 : vector.length,
              SymmetryType.fromCompass(vector.direction) == SymmetryType.Horz ? 1 : vector.length);

        this.vector = vector;
        this.thickness = thickness;
        this.mostSpecifiedShape = ShapeAttr.Shape.Line;
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
