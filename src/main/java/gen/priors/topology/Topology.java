package gen.priors.topology;

import gen.grid.ColorGrid;
import gen.grid.Mask;
import gen.primitives.Pos;
import gen.priors.adt.Array;

import java.util.function.Consumer;

public class Topology
{
    public Array holes(ColorGrid cg) { return holes(cg.positive()); }

    public Array holes(Mask m)
    {
        Mask visited = m.copy();
        Array arr = new Array();

        Consumer<Pos> floodIfEmpty = (p) -> {
            if(visited.isEmpty(p))
                visited.flood(p);
        };

        Array positions = visited.sides().toArray();

        for(Object p : positions)
            floodIfEmpty.accept((Pos) p);

        // now, any "empty" squares are guaranteed to be landlocked by positive cells -- return them.
        for(int i = 0; i < m.getWidth(); ++i)
        {
            for(int j = 0; j < m.getHeight(); ++j)
            {
                if(visited.isEmpty(i, j))
                {
                    Mask shape = new Mask(m);
                    Pos xy = new Pos(i, j);

                    addFloodToMask(visited, shape, xy);
                    arr.add(shape.trim());
                }
            }
        }

        return arr;
    }

    private void addFloodToMask(Mask visited, Mask toAddTo, Pos xy)
    {
        visited.paint(xy);
        toAddTo.paint(xy);

        for(Pos pos : Pos.neighbours)
            if(visited.inBounds(pos) && visited.isEmpty(pos))
                addFloodToMask(visited, toAddTo, pos);
    }
}
