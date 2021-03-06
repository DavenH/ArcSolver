package gen.priors.abstraction;

import gen.priors.spatial.Compass;

public enum SymmetryType
{
    Diag,
    NegDiag,
    Vert,
    Horz,
    Trans,
    ;

    public static SymmetryType fromCompass(Compass dir)
    {
        switch (dir)
        {
            case N:
            case S:
                return Vert;
            case E:
            case W:
                return Horz;
            case NE:
            case SW:
                return Diag;
            case SE:
            case NW:
                return NegDiag;
        }

        throw new RuntimeException("No applicable symmetry for compass direction " + dir);
    }

    public boolean isDiagonal()
    {
        return this == Vert || this == Horz;
    }
}
