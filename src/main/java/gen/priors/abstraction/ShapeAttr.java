package gen.priors.abstraction;

public class ShapeAttr implements CategoricalAttr
{
    public enum Shape
    {
        Dot, Line, Square, Rectangle, Noise, Other
    }

    Shape shape;

    public ShapeAttr(Shape shape)
    {
        this.shape = shape;
    }

    @Override
    public boolean equals(CategoricalAttr other)
    {
        return other instanceof ShapeAttr && ((ShapeAttr) other).shape == shape;
    }
}
