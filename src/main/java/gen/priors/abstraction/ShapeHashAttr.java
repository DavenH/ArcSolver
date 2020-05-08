package gen.priors.abstraction;

import gen.grid.Grid;

public class ShapeHashAttr<T> extends ValueCategoricalAttr<Integer>
{
    public ShapeHashAttr(Grid<T> mask)
    {
        int minHash = Integer.MAX_VALUE;

        // obviously if the scaling extends outside of the board it's not gonna match
        int maxScale = Math.min(5, Math.max(mask.getBoard().getWidth(),
                                            mask.getBoard().getHeight()) /
                                   Math.max(mask.getWidth(), mask.getHeight()));

        for(int scale = 1; scale <= maxScale; ++scale)
        {
            Grid<T> scaled = mask.scaled(scale);

            for(int i = 0; i < 4; ++i)
            {
                int hash = scaled.rotate(i).hash();
                minHash = Math.min(hash, minHash);
            }

            for(SymmetryType sym : SymmetryType.values())
            {
                int hash = scaled.reflect(sym).hash();
                minHash = Math.min(hash, minHash);
            }
        }

        this.value = minHash;
    }
}
