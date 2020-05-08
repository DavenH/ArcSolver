package gen.priors.abstraction;

import gen.primitives.Pos;

public class Symmetry
{
    public SymmetryType type;
    public boolean isEven;
    public Pos pos;
    public int cellsObeying;

    public Symmetry(SymmetryType type, boolean isEven, Pos posAlongFoldOrTransVector, int cellsObeying)
    {
        this.type = type;
        this.isEven = isEven;
        this.pos = posAlongFoldOrTransVector;
        this.cellsObeying = cellsObeying;
    }

    @Override
    public String toString()
    {
        return "type=" + type + ", isEven=" + isEven + ", pos=" + pos + ", cellsObeying=" + cellsObeying;
    }
}
