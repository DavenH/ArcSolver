package gen.priors.abstraction;

import gen.grid.Grid;

public class ShapeHashAttr<T> extends ValueCategoricalAttr<Integer>
{
    public ShapeHashAttr(Grid<T> mask)
    {
        int minHash = Integer.MAX_VALUE;

        for(int i = 0; i < 4; ++i)
        {
            int hash = mask.rotate(i).hash();
            minHash = Math.min(hash, minHash);
        }

        for(Symmetry sym : Symmetry.values())
        {
            int hash = mask.reflect(sym).hash();
            minHash = Math.min(hash, minHash);
        }

        this.value = minHash;
    }
}
