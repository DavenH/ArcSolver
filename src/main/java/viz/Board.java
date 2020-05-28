package viz;

import gen.grid.ColorGrid;
import gen.grid.Grid;
import gen.grid.Mask;
import gen.primitives.Colour;
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


    Grid grid;

    public ColorGrid getGrid() { return grid instanceof ColorGrid ? ((ColorGrid)grid) : null; }

    public Board()
    {
        setVgap(1);
        setHgap(1);
        setPadding(new Insets(1));
    }

    public Board(Grid grid, int preferredWidth, int preferredHeight)
    {
        this();
        this.grid = grid;
        updateGrid(grid, preferredWidth, preferredHeight);
    }

    public Board(float[][] grid, int preferredWidth, int preferredHeight)
    {
        this();
        updateGrid(grid, preferredWidth, preferredHeight);
    }

    public Board(int[][] grid, int preferredWidth, int preferredHeight)
    {
        this();
        updateGrid(grid, preferredWidth, preferredHeight);
    }

    public void updateGrid(Grid grid, int preferredWidth, int preferredHeight)
    {
        this.grid = grid;

        if(grid instanceof ColorGrid)
        {
            updateGrid((ColorGrid) grid, preferredWidth, preferredHeight);
        }
        else if(grid instanceof Grid)
        {
            Mask mask = (Mask) grid;
            int tileSize = Math.max(5, Math.min(30, Math.min(preferredWidth / (grid.getWidth()),
                                                             preferredHeight / (grid.getHeight())))) - 1;

            this.resize(tileSize * grid.getWidth(), tileSize * grid.getHeight());

            getChildren().clear();

            int brushIdx = mask.getBrush().ordinal();
            int negIdx = Colour.Black.ordinal();
            for (int y = 0; y < mask.getHeight(); ++y)
            {
                for (int x = 0; x < mask.getWidth(); ++x)
                {
                    Rectangle rect = new Rectangle(tileSize, tileSize, colors[mask.get(x, grid.getHeight() - 1 - y) ? brushIdx : negIdx]);
                    add(rect, x, y, 1, 1);
                }
            }
        }
    }

    public void updateGrid(ColorGrid grid, int preferredWidth, int preferredHeight)
    {
        this.grid = grid;
        int tileSize = Math.max(5, Math.min(30,
                                            Math.min(preferredWidth / (grid.getWidth()),
                                                     preferredHeight / (grid.getHeight())))) - 1;

        this.resize(tileSize * grid.getWidth(), tileSize * grid.getHeight());

        getChildren().clear();

        for(int y = 0; y < grid.getHeight(); ++y){
            for(int x = 0; x < grid.getWidth(); ++x){
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

    public void updateGrid(float[][] data, int preferredWidth, int preferredHeight)
    {
        if(data == null || data.length == 0)
            return;

        int tileSize = Math.max(5, Math.min(30,
                                            Math.min(preferredWidth / (data[0].length),
                                                     preferredHeight / (data.length)))) - 1;

        float minValue = Float.MAX_VALUE;
        float maxValue = Float.MIN_VALUE;
        for(int y = 0; y < data.length; ++y)
        {
            for(int x = 0; x < data[y].length; ++x)
            {
                minValue = Math.min(data[y][x], minValue);
                maxValue = Math.max(data[y][x], maxValue);
            }
        }

        if(maxValue == minValue)
            return;

        for(int y = 0; y < data.length; ++y)
        {
            for(int x = 0; x < data[y].length; ++x)
            {
                float normed = (data[y][x] - minValue) / (maxValue - minValue);
                Rectangle rect = new Rectangle(tileSize, tileSize, Color.hsb(240 * normed, 0.9, 1.0));
//                Rectangle rect = new Rectangle(tileSize, tileSize, Color.gray(normed));
                add(rect, x, data.length - 1 - y, 1, 1);
            }
        }
    }

    public void updateGrid(int[][] data, int preferredWidth, int preferredHeight)
    {
        if(data == null || data.length == 0)
            return;

        int tileSize = Math.max(5, Math.min(30,
                                            Math.min(preferredWidth / (data[0].length),
                                                     preferredHeight / (data.length)))) - 1;

        int minValue = Integer.MAX_VALUE;
        int maxValue = Integer.MIN_VALUE;
        for(int y = 0; y < data.length; ++y)
        {
            for(int x = 0; x < data[y].length; ++x)
            {
                minValue = Math.min(data[y][x], minValue);
                maxValue = Math.max(data[y][x], maxValue);
            }
        }

        if(maxValue == minValue)
            return;

        for(int y = 0; y < data.length; ++y)
        {
            for(int x = 0; x < data[y].length; ++x)
            {
                float normed = (data[y][x] - minValue) / (float)(maxValue - minValue);
                Rectangle rect = new Rectangle(tileSize, tileSize, Color.hsb(240 * normed, 0.9, 1.0));
//                Rectangle rect = new Rectangle(tileSize, tileSize, Color.gray(normed));
                add(rect, x, data.length - 1 - y, 1, 1);
            }
        }
    }
}
