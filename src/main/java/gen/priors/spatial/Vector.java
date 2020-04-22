package gen.priors.spatial;

public class Vector
{
    public Compass direction;
    public int length;

    public Vector(Compass direction, int length)
    {
        this.direction = direction;
        this.length = length;
    }

    public Vector times(int n)
    {
        return new Vector(direction, length * n);
    }

    public Vector unit()
    {
        return new Vector(direction, 1);
    }
}
