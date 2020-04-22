package gen.primitives;

import gen.grid.ColorGrid;
import gen.grid.Grid;
import gen.grid.Mask;

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

            for (int i = 0; i < shape.getWidth(); ++i)
            {
                for (int j = 0; j < shape.getHeight(); ++j)
                {
                    if(shape.isNotEmpty(i, j))
                    {
                        set(i - minX + xy.x,
                            j - minY + xy.y,
                            shape.getBrush());
                    }
                }
            }
        }

        setPos(new Pos(minX, minY));
    }
}
