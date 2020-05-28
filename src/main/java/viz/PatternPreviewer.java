package viz;

import gen.grid.ColorGrid;
import gen.primitives.Colour;
import gen.primitives.Pos;
import gen.priors.abstraction.Symmetry;
import gen.priors.pattern.Pattern;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.TilePane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import problem.Task;
import util.ArrayUtil;
import util.Pair;
import util.T2;

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
//        int[] grids = { 16, 60, 304, 312 };
        int[] grids = { 16, 60, 109, 304 }; //312
//        int[] grids = { 60, 109 }; //312

        List<Task> tasks = controller.getTasks(grids,true);
        int preferredWidth = 250; //(1900 - samples.size() * 10 - 20) / (samples.size());
        int preferredHeight = 250; //(960 - 70) / (2);
        int totalErrors = 0;

        for(Task task : tasks)
        {
            List<Task.Sample> train = task.getTrainSamples();
            for(Task.Sample sample : train)
            {
//                ColorGrid constructedOutput = pattern.filter(sample.input, 1, (freq, ratio) -> {
//                        return  (float) (1 / (1 + Math.exp( -(Math.sqrt(ratio) * 0.3 - 5))));
//                                                             });
                ColorGrid inputCopy = sample.input.copy();
//                ColorGrid constructedOutput = pattern.reduceFourierNoise(sample.input, 5, 2);

                int origErrors = sample.input.compare(sample.output, (a, b) -> a.equals(b) ? 0 : 1, Integer::sum, 0);
                ColorGrid guess = sample.input.copy();
                List<Symmetry> bases = pattern.getTransSymmetries(inputCopy, 2);

                for(int y = 0; y < inputCopy.getHeight(); ++y) {
                    for(int x = 0; x < inputCopy.getWidth(); ++x) {
                        float[] counts = new float[Colour.values().length];

                        Pos xy = new Pos(x, y);
                        for(Symmetry base : bases) {
                            int iSpan = 3;
                            for(int i = -iSpan; i <= iSpan; ++i) {
                                Pos scaled = base.pos.times(i);
                                Pos p = xy.plus(scaled);

                                if(inputCopy.isNotEmpty(p)) {
                                    Colour c = inputCopy.get(p);
                                    counts[c.ordinal()] += base.cellsObeying;
                                }
                            }
                        }

                        T2<Integer, Float> idxVal = ArrayUtil.maxIndexAndValue(counts, 0);
                        Colour bestColor = Colour.toColour(idxVal.getA());
                        guess.set(xy, bestColor);
                    }
                }

                int numErrors = guess.compare(sample.output, (a, b) -> a.equals(b) ? 0 : 1, Integer::sum, 0);
                totalErrors += numErrors;

                HBox pair = new HBox(5);
                ColorGrid inGrid = sample.input;
                ColorGrid outGrid = sample.output;

                pair.getChildren().add(new Label(String.format("%d, orig err: %d, new err: %d",
                                                               controller.getIndex(task.getTaskCode()),
                                                               origErrors, numErrors)));

                pair.getChildren().add(new Board(inGrid, preferredWidth, preferredHeight));
                Board guessBoard = new Board(guess, preferredWidth, preferredHeight);

                int index = 0;
                int h = inputCopy.getHeight();
                int w = inputCopy.getWidth();

                for(int y = 0; y < h; ++y) {
                    for(int x = 0; x < w; ++x) {
                        if(! guess.get(x, h - 1 - y).equals(outGrid.get(x, h - 1 - y))) {
                            Node node = guessBoard.getChildren().get(index);
                            Rectangle rect = (Rectangle) node;

                            rect.setStroke(Color.BLACK);
                            rect.setStrokeWidth(2);
                        }

                        ++index;
                    }
                }
                pair.getChildren().add(guessBoard);
                pair.getChildren().add(new Board(outGrid, preferredWidth, preferredHeight));

                int[][] indiff = pattern.autoTransCorr(inGrid.toInt());
                pair.getChildren().add(new Board(indiff, preferredWidth, preferredHeight));

//                float[][] outdiff = pattern.autoTransCorr(outGrid.toFloat());
//                pair.getChildren().add(new Board(outdiff, preferredWidth, preferredHeight));

                Pair<float[][]> fft1 = pattern.transform(inGrid.toFloat());
                Pair<float[][]> fft2 = pattern.transform(guess.toFloat());
                Pair<float[][]> fft3 = pattern.transform(outGrid.toFloat());
//                fft2.getA()[0][0] = 0;
                pair.getChildren().add(new Board(fft1.getA(), preferredWidth, preferredHeight));
                pair.getChildren().add(new Board(fft2.getA(), preferredWidth, preferredHeight));
                pair.getChildren().add(new Board(fft3.getA(), preferredWidth, preferredHeight));
//                pair.getChildren().add(new Board(fft2.getB(), preferredWidth, preferredHeight));

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
