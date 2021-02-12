package viz;

import gen.grid.ColorGrid;
import gen.grid.Grid;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.Scene;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.TilePane;
import javafx.stage.Stage;
import problem.Controller;
import problem.Task;

import java.io.File;
import java.net.URL;
import java.util.List;

public class TreePreviewer extends Application
{
    Controller controller = new Controller();

    static class CellPane extends Pane
    {
    }

    public TreePreviewer()
    {
        controller.loadTasks(new File("C:\\Users\\Public\\data\\ARC-master\\data\\"));
    }

    @Override
    public void start(Stage primaryStage)
    {
        primaryStage.setTitle("Board Previewer");
        TilePane tilePane = new TilePane(Orientation.HORIZONTAL);
        tilePane.setPadding(new Insets(20, 0, 0, 0));
        int[] grids = { 16, 60, 109, 304 };

        List<Task> tasks = controller.getTasks(grids, true);
        int preferredWidth = 250;
        int preferredHeight = 250;

        CellPane wrapper = new CellPane();
        for(Task task : tasks)
        {
            List<Task.Sample> train = task.getTrainSamples();
            for (Task.Sample sample : train)
            {
                ColorGrid copy = sample.input.copy();
                copy.buildTreeByEntropySplits(false);

                addChildrenToWrapper(wrapper, copy, preferredWidth, preferredHeight, 100, 100);

                // add children recursively to wrapper
                tilePane.getChildren().add(wrapper);
            }
        }

        ScrollPane sp = new ScrollPane(tilePane);
        sp.setPrefViewportWidth(1900);
        sp.setFitToWidth(true);
        sp.setFitToHeight(true);

        Scene mainScene = new Scene(sp, 1920, 1080);
        URL url = this.getClass().getResource("/dark_theme.css");
        boolean succeeded = mainScene.getStylesheets().add(url.toExternalForm());

        primaryStage.setScene(mainScene);
        primaryStage.show();
    }

    private int addChildrenToWrapper(CellPane wrapper, Grid grid,
                                      int preferredWidth,
                                      int preferredHeight,
                                      int parentX, int parentY)
    {
        Board board = new Board(grid, preferredWidth, preferredHeight);
        board.relocate(parentX, parentY);
        wrapper.getChildren().add(board);

        int x = parentX;

        if(grid instanceof ColorGrid)
        {
            for(Grid child : ((ColorGrid)grid).getChildren())
            {
                Board cboard = new Board(child, preferredWidth, preferredHeight);
                x += cboard.getWidth();

                int childY = parentY + (int)(board.getHeight()) + 50;

                cboard.relocate(x, childY);
                wrapper.getChildren().add(cboard);
                x += addChildrenToWrapper(wrapper, child, preferredWidth, preferredHeight, x, childY);
            }
        }

        return x;
    }

    @Override
    public void stop()
    {
    }
}
