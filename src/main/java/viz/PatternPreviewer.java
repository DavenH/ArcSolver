package viz;

import gen.grid.ColorGrid;
import gen.priors.pattern.Pattern;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.TilePane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import problem.Task;
import util.Pair;

import java.util.List;

public class PatternPreviewer extends SimplePreviewer
{
    Pattern pattern = new Pattern();

    public PatternPreviewer()
    {
    }

    @Override
    public void addBoardsToPane(TilePane tilePane)
    {
        int[] transGrids = { 16, 60, 304 }; //312
//        int[] transGrids = { 16, 60, 109, 286, 304, 312 }; //312
        controller.getTasks(transGrids, true);

        List<Task> tasks = controller.getTasks(transGrids,true);
        int preferredWidth = 250; //(1900 - samples.size() * 10 - 20) / (samples.size());
        int preferredHeight = 250; //(960 - 70) / (2);
        int totalErrors = 0;

        for(Task task : tasks)
        {
            List<Task.Sample> train = task.getTrainSamples();
            for(Task.Sample sample : train)
            {
                ColorGrid constructedOutput = pattern.filter(sample.input, 2, (freq, ratio) -> {
                        return  (float) (1 / (1 + Math.exp( -(Math.sqrt(ratio) * 0.3 - 6))));
                                                             });
//                ColorGrid constructedOutput = pattern.reduceFourierNoise(sample.input, 5, 2);

                int numErrors = constructedOutput.compare(sample.output, (a, b) -> a.equals(b) ? 0 : 1, Integer::sum, 0);
                int origErrors = sample.input.compare(sample.output, (a, b) -> a.equals(b) ? 0 : 1, Integer::sum, 0);

//                System.out.println("Error, from " + origErrors + " to " + numErrors);
//                System.out.println(sample.input.toString());
//                System.out.println(constructedOutput.toString());
//                System.out.println(sample.output.toString());

                totalErrors += numErrors;

                HBox pair = new HBox(5);
                ColorGrid inGrid = sample.input;
                ColorGrid outGrid = sample.output;

                pair.getChildren().add(new Label(String.format("%d, orig err: %d, new err: %d",
                                                               controller.getIndex(task.getTaskCode()),
                                                               origErrors, numErrors)));

                pair.getChildren().add(new Board(inGrid, preferredWidth, preferredHeight));
                Board board = new Board(constructedOutput, preferredWidth, preferredHeight);

                int index = 0;
                int numChildren = board.getChildren().size();
                int h = constructedOutput.getHeight();
                int w = constructedOutput.getWidth();
                for(int y = 0; y < h; ++y) {
                    for(int x = 0; x < w; ++x) {

                        if(! constructedOutput.get(x, h - 1 - y).equals(outGrid.get(x, h - 1 - y)))
                        {
                            Node node = board.getChildren().get(index);
                            Rectangle rect = (Rectangle) node;

                            rect.setStroke(Color.BLACK);
                            rect.setStrokeWidth(2);
                        }

                        ++index;
                    }
                }
                pair.getChildren().add(board);
                pair.getChildren().add(new Board(outGrid, preferredWidth, preferredHeight));

                float[][] indiff = pattern.autodiff(inGrid.toFloat());
                pair.getChildren().add(new Board(indiff, preferredWidth, preferredHeight));

//                float[][] outdiff = pattern.autodiff(outGrid.toFloat());
//                pair.getChildren().add(new Board(outdiff, preferredWidth, preferredHeight));

                Pair<float[][]> fft1 = pattern.transform(inGrid.toFloat());
                Pair<float[][]> fft2 = pattern.transform(constructedOutput.toFloat());
//                fft2.getA()[0][0] = 0;
                pair.getChildren().add(new Board(fft1.getA(), preferredWidth, preferredHeight));
                pair.getChildren().add(new Board(fft2.getA(), preferredWidth, preferredHeight));
                pair.getChildren().add(new Board(fft2.getB(), preferredWidth, preferredHeight));

                tilePane.getChildren().add(pair);
            }
        }
        tilePane.getChildren().add(new Label("Total errors: " + totalErrors));

    }

    public static void main(String[] args)
    {
        launch(args);
    }
}
