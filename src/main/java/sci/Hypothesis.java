package sci;


import java.util.List;

/**
 * A model that reconstructs data
 */
public abstract class Hypothesis<I, O>
{
    List<Entity> entities;

    public double fidelity()
    {
        return 0;
    }

    public abstract O generate(I input);
}
