package gen.primitives;

import gen.grid.ColorGrid;
import gen.grid.Grid;
import gen.grid.Mask;
import gen.priors.adt.Array;

public class Group extends ColorGrid
{
    public Group(Grid board, Mask... masks)
    {
        super(board, 1, 1);

        if(masks.length == 0)
            return;

        int maxX = 0;
        int maxY = 0;
        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;

        for (Mask shape : masks)
        {
            minX = Math.min(shape.getPos().x, minX);
            maxY = Math.max(shape.getPos().y, maxY);
            maxX = Math.max(shape.getTopRight().x, maxX);
            minY = Math.min(shape.getTopRight().y, minY);
        }

        resize(maxX - minX, maxY - minY);

        for (Mask shape : masks)
        {
            Pos xy = shape.getPos();
            Array<Pos> arr = Pos.permute(shape.getWidth(), shape.getHeight());

            for(Pos p : arr)
                if(shape.isNotEmpty(p))
                    set(p.minus(getPos()).plus(xy),
                        shape.getBrush());
        }

        setPos(new Pos(minX, minY));
    }
}
