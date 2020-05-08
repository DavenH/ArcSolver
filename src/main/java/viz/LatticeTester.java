package viz;

import gen.grid.ColorGrid;
import gen.priors.pattern.Pattern;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.TilePane;
import problem.Task;
import util.ArrayUtil;

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
//        int[] latticeGrids = { 8, 32, 58, 60, 162 };
        controller.getTasks(latticeGrids, true);

        List<Task> tasks = controller.getTasks(latticeGrids, true);
        int preferredWidth = 300; //(1900 - samples.size() * 10 - 20) / (samples.size());
        int preferredHeight = 300; //(960 - 70) / (2);

        int[][] kernel = new int[][]
         {
             { 1,  1,  1 },
             { 1, -1,  1 },
             { 1,  1, -1,  1,  1 },
             { 1,  1,  1, -1,  1,  1,  1 },
             { 1,  1,  1,  1, -1,  1,  1,  1,  1 },
             { 1,  1,  1,  1,  1, -1,  1,  1,  1,  1,  1 }
         };

        for (Task task : tasks)
        {
            List<Task.Sample> train = task.getTrainSamples();

            for (Task.Sample sample : train)
            {
                ColorGrid input = sample.input;
                ColorGrid.EntropyData trans = input.calculateCellTransitions();

                float maxVal = 0;
                int bestIdx = -1;

                for (int j = 0; j < kernel.length; ++j)
                {
                    int[] negPosNeg = kernel[j];
                    float avg = ArrayUtil.average(negPosNeg);

                    // extend by 1 around the horz array because kernel will span one cell to left and right of current index
                    for (int k = 0; k < trans.horz.length; ++k)
                    {
                        float sum = 0;
                        float negsum = 0;

                        for (int i = 0; i < negPosNeg.length; ++i)
                        {
                            int arrIdx = k + i - (negPosNeg.length - 1) / 2;

                            // treat out of bounds as a negative
                            boolean outBounds = arrIdx < 0 || arrIdx >= trans.horz.length;
                            sum += outBounds ? avg : (negPosNeg[i] - avg) * trans.horz[arrIdx];
                            negsum += outBounds ? avg : (avg - negPosNeg[i]) * trans.horz[arrIdx];
                        }

                        float delta = sum - negsum;
                        if(delta > maxVal)
                        {
                            bestIdx = j;
                            maxVal = delta;
                        }
                    }
                }

                HBox pair = new HBox(5);
                pair.getChildren().add(new Label(String.format("%d, bestIdx: %d, %3.2f", controller.getIndex(task.getTaskCode()), bestIdx, maxVal)));
                pair.getChildren().add(new Board(sample.input, preferredWidth, preferredHeight));
                tilePane.getChildren().add(pair);

                System.out.println("Best index " + bestIdx + " maxval: " + maxVal);
            }
        }
//
//        for(Task task : tasks)
//        {
//            List<Task.Sample> train = task.getTrainSamples();
//            for(Task.Sample sample : train)
//            {
//
//
//                float[][] sampleInput = sample.input.toFloat();
//                float[][] autoTransCorr = pattern.autoTransCorr(sampleInput);
//
//                HBox pair = new HBox(5);
//                ColorGrid inGrid = sample.input;
//                ColorGrid outGrid = sample.output;
//
//                pair.getChildren().add(new Label(String.format("%d", controller.getIndex(task.getTaskCode()))));
//
//                pair.getChildren().add(new Board(inGrid, preferredWidth, preferredHeight));
//                pair.getChildren().add(new Board(autoTransCorr, preferredWidth, preferredHeight));
////                pair.getChildren().add(new Board(outGrid, preferredWidth, preferredHeight));
////                ColorGrid constructedOutput = pattern.reduceFourierNoise(sample.input, 5, 1);
//
//                ColorGrid constructedOutput = sample.input.copy();
//                ColorGrid guess = constructedOutput.copy();
////                Pos pos = pattern.getTransSymmetries(constructedOutput);
//
//                Iterable<T2<Pos, Float>> bases = pattern.getTransSymmetries(constructedOutput, 3);
//
//                constructedOutput.setBackground(Colour.Black);
//
//                for(int y = 0; y < constructedOutput.getHeight(); ++y) {
//                    for(int x = 0; x < constructedOutput.getWidth(); ++x) {
//                        float[] counts = new float[Colour.values().length];
//
//                        Pos xy = new Pos(x, y);
//                        for(T2<Pos, Float> base : bases) {
//                            int iSpan = 3;
//                            for(int i = -iSpan; i <= iSpan; ++i) {
//                                Pos scaled = base.getA().times(i);
//                                Pos p = xy.plus(scaled);
//
////                                if(constructedOutput.isNotEmpty(p)) {
//                                if(constructedOutput.inBounds(p)) {
//                                    Colour c = constructedOutput.get(p);
//                                    counts[c.ordinal()] += base.getB();
//                                }
//                            }
//                        }
//
//                        T2<Integer, Float> idxVal = ArrayUtil.maxIndexAndValue(counts, 0);
//                        Colour bestColor = Colour.toColour(idxVal.getA());
//                        guess.set(xy, bestColor);
//                    }
//                }
//
//                pair.getChildren().add(new Board(guess, preferredWidth, preferredHeight));
//
//                Pair<float[][]> fft2 = pattern.transform(guess.toFloat());
//                pair.getChildren().add(new Board(fft2.getA(), preferredWidth, preferredHeight));
//                pair.getChildren().add(new Board(fft2.getB(), preferredWidth, preferredHeight));
//
//
//                tilePane.getChildren().add(pair);
//            }
//        }

    }

    public static void main(String[] args)
    {
        launch(args);
    }
}
