package viz;

import gen.grid.ColorGrid;
import javafx.geometry.Insets;
import javafx.scene.layout.GridPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

public class Board extends GridPane
{
    static Color colors[] =
    {
        new Color(0, 0, 0, 1), // black
        new Color(0.2, 0.2, 1, 1), // blue
        new Color(1, 0.2, 0, 1), // red
        new Color(0.2, 1, 0.2, 1), // green
        new Color(1, 0.8, 0.2, 1), // yellow
        new Color(0.5, 0.5, 0.5, 1), // grey
        new Color(1, 0, 1, 1), // magenta
        new Color(1, 0.5, 0, 1), // orange
        new Color(0, 0.8, 1, 1), // cyan
        new Color(0.4, 0, 0, 1), // maroon
        new Color(0, 0, 0, 0), // none
    };

    ColorGrid grid;
    public ColorGrid getGrid() { return grid; }

    public Board()
    {
        setVgap(1);
        setHgap(1);
        setPadding(new Insets(1));
    }

    public Board(ColorGrid grid, int preferredWidth, int preferredHeight)
    {
        this();
        this.grid = grid;
        updateGrid(grid, preferredWidth, preferredHeight);
    }

    public void updateGrid(ColorGrid grid, int preferredWidth, int preferredHeight)
    {
        this.grid = grid;
        int tileSize = Math.max(5, Math.min(30,
                                            Math.min(preferredWidth / (grid.getWidth() + 2),
                                                     preferredHeight / (grid.getHeight() + 2)))) - 1;

        this.resize(tileSize * grid.getWidth(), tileSize * grid.getHeight());

        getChildren().clear();

        for(int y = 0; y < grid.getHeight(); ++y)
        {
            for(int x = 0; x < grid.getWidth(); ++x)
            {
                Rectangle rect = new Rectangle(tileSize, tileSize, colors[grid.get(x, grid.getHeight() - 1 - y).ordinal()]);
                add(rect, x, y, 1, 1);
            }
        }

        ColorGrid.EntropyData data = grid.calculateCellTransitions();

        double maxVert = 0, maxHorz = 0;
        for(double d : data.vert) { maxVert = Math.max(maxVert, d); }
        for(double d : data.horz) { maxHorz = Math.max(maxHorz, d); }

        if(maxVert > 0)
        {
            for(int y = 0; y < grid.getHeight(); ++y)
            {
                Rectangle rect = new Rectangle(tileSize, tileSize, Color.gray(data.vert[grid.getHeight() - 1 - y] / maxVert));
                add(rect, grid.getWidth() + 1, y, 1, 1);
            }
        }

        if(maxHorz > 0)
        {
            for(int x = 0; x < grid.getWidth(); ++x)
            {
                Rectangle rect = new Rectangle(tileSize, tileSize, Color.gray(data.horz[x] / maxHorz));
                add(rect, x, grid.getHeight() + 1, 1, 1);
            }
        }

    }
}
