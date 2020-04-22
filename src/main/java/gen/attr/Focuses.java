package gen.attr;

public enum Focuses
{
    // object internal
    Left, Right, Top, Bottom, Centre, Within, Perimeter, Corners,

    // linear
    Ends,

    // external to 1
    LeftOf, RightOf, Above, Below, Outside, Around,

    // relative to 2+
    CentreOf, Between,

    // relative to an Axis
    Opposite, Parallel, OrthogonalTo
}
