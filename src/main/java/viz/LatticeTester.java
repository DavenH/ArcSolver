package viz;

import gen.grid.ColorGrid;
import gen.priors.pattern.Pattern;
import javafx.scene.control.Label;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import problem.Task;
import util.Pair;

import java.util.List;

public class LatticeTester extends SimplePreviewer
{
    Pattern pattern = new Pattern();

    public LatticeTester()
    {
    }

    @Override
    public void addBoardsToPane(TilePane tilePane)
    {
        int[] latticeGrids = { 8, 32, 58, 60, 162, 184, 243, 313, 390 };
        controller.getTasks(latticeGrids, true);

        List<Task> tasks = controller.getTasks(latticeGrids, true);
        int preferredWidth = 300; //(1900 - samples.size() * 10 - 20) / (samples.size());
        int preferredHeight = 300; //(960 - 70) / (2);

        for(Task task : tasks)
        {
            List<Task.Sample> train = task.getTrainSamples();
            for(Task.Sample sample : train)
            {
                float[][] sampleInput = sample.input.toFloat();
                float[][] autodiff = pattern.autodiff(sampleInput);

                VBox pair = new VBox(5);
                ColorGrid inGrid = sample.input;
                ColorGrid outGrid = sample.output;

                pair.getChildren().add(new Label(String.format("%d", controller.getIndex(task.getTaskCode()))));

                pair.getChildren().add(new Board(inGrid, preferredWidth, preferredHeight));
                pair.getChildren().add(new Board(autodiff, preferredWidth, preferredHeight));
//                pair.getChildren().add(new Board(outGrid, preferredWidth, preferredHeight));
                Pair<float[][]> fft2 = pattern.transform(autodiff);
                pair.getChildren().add(new Board(fft2.getA(), preferredWidth, preferredHeight));
                pair.getChildren().add(new Board(fft2.getB(), preferredWidth, preferredHeight));

                tilePane.getChildren().add(pair);
            }
        }

    }

    public static void main(String[] args)
    {
        launch(args);
    }
}
