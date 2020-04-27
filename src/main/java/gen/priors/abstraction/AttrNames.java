package gen.priors.abstraction;

public enum AttrNames
{
    // categorical
    Colour,
    Background,
    SymmetrySet,
    Shape,
    ShapeHash,
    Centre,

    // countable
    X, Y,
    ArrayX, ArrayY,
    W, H,
    NumChildren,
    NumPositive,
    NumNegative,
}

enum RelativeAttrNames
{
    // relative
    CompassDir,
    RelativeAngle,
    RelativeSymmetry,

    // external to 1
    LeftOf, RightOf, Above, Below, Outside, Around,

    // relative to 2+
    CentreOf, ContainedBy,

    // relative to an Axis
    Opposite, Parallel, OrthogonalTo
}
