package gen.priors.topology;

import gen.grid.ColorGrid;
import gen.grid.Mask;
import gen.primitives.Pos;
import gen.priors.adt.Array;

import java.util.function.Consumer;

public class Topology
{
    public Array holes(ColorGrid cg) { return holes(cg.positive()); }

    public Array<Mask> holes(Mask m)
    {
        Mask visited = m.copy();
        Array<Mask> arr = new Array<>();

        Consumer<Pos> floodIfEmpty = (p) -> {
            if(visited.isEmpty(p))
                visited.flood(p);
        };

        Array<Pos> sidePositions = visited.sides().positiveToArray();

        for(Pos p : sidePositions)
            floodIfEmpty.accept(p);

        // now, any "empty" squares are guaranteed to be landlocked by positive cells -- return them.
        Array<Pos> allPos = Pos.permute(m.getWidth(), m.getHeight());
        for(Pos p : allPos)
        {
            if(visited.isEmpty(p))
            {
                Mask shape = new Mask(m);
                addFloodToMask(visited, shape, p);
                shape.trim();
                arr.add(shape);
            }
        }

        return arr;
    }

    private void addFloodToMask(Mask visited, Mask toAddTo, final Pos xy)
    {
        visited.paint(xy);
        toAddTo.paint(xy);

        for(Pos pos : Pos.neighbours)
        {
            Pos moved = xy.plus(pos);
            if(visited.inBounds(moved) && visited.isEmpty(moved))
                addFloodToMask(visited, toAddTo, moved);
        }
    }
}
