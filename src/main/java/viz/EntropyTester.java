package viz;

import gen.grid.ColorGrid;
import gen.priors.pattern.Pattern;
import javafx.scene.control.Label;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import problem.Task;
import util.ArrayUtil;
import util.T2;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class EntropyTester extends SimplePreviewer
{
    Pattern pattern = new Pattern();

    @Override
    public void addBoardsToPane(TilePane tilePane)
    {
        List<Task> trainTasks = controller.getTasks(true);
//        int[] latticeGrids = { 8, 32, 58, 60, 162, 184, 243, 313, 390 };
//        List<Task> trainTasks = controller.getTasks(latticeGrids, true);

        final List<T2<Task.Sample, Float>> metrics = new ArrayList<>();
/*
        for(Task task : trainTasks)
        {
            double maxes, mins, avgs, tmaxes, tmins, tavgs, meta;
            maxes = mins = avgs = tmaxes = tmins = tavgs = meta = 0;
            double metaMinima = 0;
            for(Task.Sample sample : task.getTrainSamples())
            {
                ColorGrid.EntropyData inputEntropy = sample.input.calculateShannonEntropy();
                ColorGrid.EntropyData outputEntropy = sample.output.calculateShannonEntropy();
                ColorGrid.EntropyData inputTrans = sample.input.calculateCellTransitions();
                ColorGrid.EntropyData outputTrans = sample.output.calculateCellTransitions();

                Stats inVert = stats(inputEntropy.vert);
                Stats outVert = stats(outputEntropy.vert);
                Stats inHorz = stats(inputEntropy.horz);
                Stats outHorz = stats(outputEntropy.horz);

                Stats inTransVert = stats(inputTrans.vert);
                Stats outTransVert = stats(outputTrans.vert);
                Stats inTransHorz = stats(inputTrans.horz);
                Stats outTransHorz = stats(outputTrans.horz);

                double iw = 1;
                double ow = 0;

                maxes    += iw * inVert.max + iw * inHorz.max + ow * outVert.max + ow * outHorz.max;
                mins     += iw * inVert.min + iw * inHorz.min + ow * outVert.min + ow * outHorz.min;
                avgs     += iw * inVert.mean + iw * inHorz.mean + ow * outVert.mean + ow * outHorz.mean;

                tmaxes   += iw * inTransVert.max + iw * inTransHorz.max + ow * outTransVert.max + ow * outTransHorz.max;
                tmins    += iw * inTransVert.min + iw * inTransHorz.min + ow * outTransVert.min + ow * outTransHorz.min;
                tavgs    += iw * inTransVert.mean + iw * inTransHorz.mean + ow * outTransVert.mean + ow * outTransHorz.mean;
                meta     += iw * (inputTrans.metaHorz + inputTrans.metaVert) + ow * (outputTrans.metaHorz + outputTrans.metaVert);

                float[] hdiff = pattern.autocorrelation1D(inputTrans.horz);
                float[] vdiff = pattern.autocorrelation1D(inputTrans.vert);

//                float[] invH = ArrayUtil.copy(inputTrans.horz);
//                ArrayUtil.inv(invH, 0.1f);
//                float[] invV = ArrayUtil.copy(inputTrans.vert);
//                ArrayUtil.inv(invV, 0.1f);

                T2<Integer, Float> horzM = ArrayUtil.maxIndexAndValue(hdiff, 1);
                T2<Integer, Float> vertM = ArrayUtil.maxIndexAndValue(vdiff, 1);

                if(horzM.getA() >= 3 && horzM.getA() <= 6)
                    metaMinima += 2;
                if(vertM.getA() >= 3 && vertM.getA() <= 6)
                    metaMinima += 2;
//
//                metaMinima += Math.log(horzM.getA() * vertM.getA() * vertM.getB() * vertM.getA());

                metaMinima += pattern.dot(inputTrans.horz, inputTrans.vert) / Math.min(sample.input.getWidth(),sample.input.getHeight());
                metaMinima += pattern.dot(inputTrans.horz, ArrayUtil.reverse(inputTrans.vert)) / Math.min(sample.input.getWidth(), sample.input.getHeight());
            }

            double inv = 1.0 / task.getTrainSamples().size();
            maxes *= inv;
            mins  *= inv;
            avgs  *= inv;
            tmaxes *= inv;
            tmins  *= inv;
            tavgs  *= inv;
            meta *= inv;
            metaMinima *= inv;

            double metric = 5 * metaMinima - 3 * mins + avgs; //tavgs - tmins;
            for(Task.Sample sample : task.getTrainSamples())
                metrics.add(new T2<>(sample, metric));
        }
*/

        int[][] kernel = new int[][]
         {
//                 { 1,  1,  1 },
//                 { 1, -1,  1 },
                 { 1,  1, -1,  1,  1 },
                 { 1,  1,  1, -1,  1,  1,  1 },
                 { 1,  1,  1,  1, -1,  1,  1,  1,  1 },
                 { 1,  1,  1,  1,  1, -1,  1,  1,  1,  1,  1 }
         };

        for (Task task : trainTasks)
        {
            List<Task.Sample> train = task.getTrainSamples();

            for (Task.Sample sample : train)
            {
                ColorGrid input = sample.input;
//                ColorGrid.EntropyData trans = input.calculateShannonEntropy();
                ColorGrid.EntropyData trans = input.calculateCellTransitions();

                float[][] arrs = new float[][] { trans.horz, trans.vert };

                float avgMax = 1;

                for(float[] arr : arrs)
                {
                    float maxVal = 0;
                    int bestIdx = -1;

                    for(int j = 0; j < kernel.length; ++j)
                    {
                        int[] negPosNeg = kernel[j];
                        float avgKern = ArrayUtil.average(negPosNeg);
                        float maxArr = ArrayUtil.max(arr);

                        for(int k = 0; k < arr.length; ++k)
                        {
                            float sum = 0;
                            float negsum = 0;

                            for(int i = 0; i < negPosNeg.length; ++i)
                            {
                                int arrIdx = k + i - (negPosNeg.length - 1) / 2;

                                // treat out of bounds as a negative
                                boolean outBounds = arrIdx < 0 || arrIdx >= arr.length;
                                sum     += outBounds ? 0 : (negPosNeg[i] - avgKern) * (arr[arrIdx] / maxArr);
                                negsum  += outBounds ? 0 : (avgKern - negPosNeg[i]) * (arr[arrIdx] / maxArr);
                            }

                            float delta = sum - negsum / (float) Math.sqrt(arr.length);
                            if(delta > maxVal)
                            {
                                bestIdx = j;
                                maxVal = delta;
                            }
                        }
                    }

                    avgMax *= maxVal;

//                    if(bestIdx < 1)
//                        avgMax *= 0.1f;
//                    avgMax *= pattern.dot(arrs[0], arrs[1]);
                }

                metrics.add(new T2<>(sample, avgMax));
            }

        }

        metrics.sort(Comparator.comparing(a -> -a.getB()));

        int preferredWidth = 200; //(1900 - samples.size() * 10 - 20) / (samples.size());
        int preferredHeight = 200; //(960 - 70) / (2);

        for(T2<Task.Sample, Float> p : metrics)
        {
            VBox pair = new VBox(5);
            ColorGrid inGrid = p.getA().input;
            ColorGrid outGrid = p.getA().output;

            pair.getChildren().add(new Label(String.format("%d, %3.3f, %d x %d → %d x %d",
                                                           controller.getIndex(p.getA().task.getTaskCode()),
                                                           p.getB(), inGrid.getWidth(), inGrid.getHeight(),
                                                           outGrid.getWidth(), outGrid.getHeight())));

            pair.getChildren().add(new Board(inGrid, preferredWidth, preferredHeight));
            pair.getChildren().add(new Board(outGrid, preferredWidth, preferredHeight));

//            Pair<float[][]> fft2 = pattern.transform(inGrid.toFloat());
//            fft2.getA()[0][0] = 0;
//            pair.getChildren().add(new Board(fft2.getA(), preferredWidth, preferredHeight));
//            pair.getChildren().add(new Board(fft2.getB(), preferredWidth, preferredHeight));

//            float[][] indiff = pattern.autoTransCorr(outGrid.toFloat());
//            pair.getChildren().add(new Board(indiff, preferredWidth, preferredHeight));

            tilePane.getChildren().add(pair);
        }
    }

    public static void main(String[] args)
    {
        launch(args);
    }
}
