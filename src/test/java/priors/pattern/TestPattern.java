package priors.pattern;

import gen.grid.ColorGrid;
import gen.primitives.Pos;
import gen.priors.pattern.Pattern;
import problem.Controller;
import problem.Task;
import util.ArrayUtil;
import util.T2;

import java.io.File;
import java.util.List;

public class TestPattern
{
    Pattern pattern = new Pattern();

    public void testCrosshatchDetection()
    {
        List<Task> tasks = getTasks(new int[]{ 243, 8, 10, 79, 32, 184, 60, 256, 162, 148, 58, 390, 313 });
        for (Task task : tasks)
        {
            System.out.println("---------------------------");

            List<Task.Sample> train = task.getTrainSamples();
            for (Task.Sample sample : train)
            {
                ColorGrid constructedOutput = pattern.reduceFourierNoise(sample.input, 5, 2);


            }
        }
    }

    public void testAutocorrelation1D()
    {
        float subpattern[] = new float[]{ 6, 2, 1, 5, 1, 4, 2 };
        float values[] = makePeriodic(subpattern, 3);

        // O(n log n) autocorrelation -- probably much slower on small n though
        float[] autocorr = pattern.autocorrelation1D(values);

        for(int i = 0; i < autocorr.length; ++i)
            System.out.println(autocorr[i]);

        System.out.println();

        // O(n²) correlation to test with
        float[] corr = new float[values.length];

        for(int i = 0; i < values.length; ++i)
        {
            corr[i] = 0;
            for(int j = i; j < values.length; ++j)
                corr[i] += Math.abs(values[j - i] * values[j]);
        }

        for(int i = 0; i < corr.length; ++i)
            System.out.println(corr[i]);
    }

    public void testAutocorrelation2D()
    {
        float[][] subpattern = { { 1, 3, 4, 5, 2 }, { 4, 4, 7, 1, 2 }, { 5, 5, 7, 3, 4 }, { 1, 5, 5, 2, 3 } };

        float[][] periodicPat = makePeriodic(subpattern, 4, 4);

        for (int y = 0; y < periodicPat.length; ++y) {
            for(int x = 0 ; x < periodicPat[y].length; ++x) {
                System.out.print(String.format("%3.1f ", periodicPat[y][x]));
            }
            System.out.println();
        }
        System.out.println();

        float[][] correlated = pattern.autocorrelation2D(periodicPat);

        for (int y = 1; y < correlated.length; ++y) {
            for(int x = 1 ; x < correlated[y].length; ++x) {
                System.out.print(String.format("%3.1f ", correlated[y][x]));
            }
            System.out.println();
        }
    }

    private float[] makePeriodic(float[] subpattern, int copies)
    {
        float[] array = new float[copies * subpattern.length];
        for(int i = 0; i < copies; ++i) {
            for(int j = 0; j < subpattern.length; ++j) {
                array[i * subpattern.length + j] = subpattern[j];
            }
        }

        return array;
    }

    private float[][] makePeriodic(float[][] subpattern, int copiesW, int copiesH)
    {
        int numRows = subpattern.length;
        int numCols = subpattern[0].length;
        float[][] array = new float[copiesH * numRows][copiesW * numCols];
        float average = 0;

        for(int i = 0; i < subpattern.length; ++i)
            for(int j = 0; j < subpattern[i].length; ++j)
                average += subpattern[i][j];

        average /= (double) (numRows * numCols);

        for(int hIdx = 0; hIdx < copiesH; ++hIdx) {
            for(int wIdx = 0; wIdx < copiesW; ++wIdx) {
                for(int rowIdx = 0; rowIdx < numRows; ++rowIdx) {

                    for(int colIdx = 0; colIdx < numCols; ++colIdx) {
                        array[hIdx * numRows + rowIdx][wIdx * numCols + colIdx] = subpattern[rowIdx][colIdx] - average;
                    }
                }
            }
        }

        return array;
    }

    public void testAutoDiff1D()
    {
        float subpattern[] = new float[]{ 6, 2, 1, 5, 5, 1, 4, 2 };
        float values[] = makePeriodic(subpattern, 16);
        float[] autodiff = pattern.autodiff(values);

        ArrayUtil.print(autodiff, 1);
        T2<Integer, Float> minima = ArrayUtil.minIndexAndValue(autodiff, 1);
        System.out.println();
        System.out.println("Expected periodic length: " + subpattern.length + " autodiff min index: " + minima.getA());
    }

    public void testAutoDiff2D()
    {
        float[][] subpattern = { { 1, 3, 4, 5, 2, 3 },
                                 { 4, 4, 7, 1, 2, 2 },
                                 { 5, 5, 7, 3, 4, 1 },
                                 { 1, 5, 5, 2, 3, 7 } };
        float values[][] = makePeriodic(subpattern, 4, 6);

        long timeMillis = System.currentTimeMillis();
        float[][] autodiff = pattern.autodiff(values);
        long endMillis = System.currentTimeMillis();

        System.out.println(endMillis - timeMillis + " millis");
        System.out.println();

        ArrayUtil.print(autodiff, 2);
        T2<Pos, Float> minima = ArrayUtil.maxIndexAndValue(autodiff, 1, 1);
        Pos minPos = minima.getA();
        System.out.println("Expected periodic length: (" + subpattern.length +
                           ", " + subpattern[0].length + ") autodiff min position: " +
                           minPos.toString() + " indicating subpattern dim recurs every " +
                           String.format("%dx%d", minPos.x, minPos.y) + " cells");
    }

    public void testInfill()
    {
        int[] transGrids = { 16, 60, 304 }; //312
        int[] mirroredGrids = { 73, 286, 393 };
        int totalErrors = 0;

        List<Task> tasks = getTasks(transGrids);

        for(Task task : tasks)
        {
            System.out.println("---------------------------");

            List<Task.Sample> train = task.getTrainSamples();
            for(Task.Sample sample : train)
            {
                ColorGrid constructedOutput = pattern.reduceFourierNoise(sample.input, 5, 2);

                int numErrors = constructedOutput.compare(sample.output, (a, b) -> a.equals(b) ? 0 : 1, Integer::sum, 0);
                int origErrors = sample.input.compare(sample.output, (a, b) -> a.equals(b) ? 0 : 1, Integer::sum, 0);

                System.out.println("Error, from " + origErrors + " to " + numErrors);
                System.out.println(sample.input.toString());
                System.out.println(constructedOutput.toString());
                System.out.println(sample.output.toString());

                totalErrors += numErrors;
            }

        }

        System.out.println("Total errors: " + totalErrors);
    }

    public List<Task> getTasks(int[] indices)
    {
        Controller controller = new Controller();
        controller.loadTasks(new File("C:\\Users\\Public\\data\\ARC-master\\data\\"));
        return controller.getTasks(indices, true);
    }

    public static void main(String[] args)
    {
        TestPattern testFFT = new TestPattern();
//        testFFT.testInfill();
//        testFFT.testAutocorrelation1D();
//        testFFT.testAutocorrelation2D();
        testFFT.testAutoDiff1D();
//        testFFT.testGetPeak();
//        testFFT.testAutoDiff2D();
    }

    private void testGetPeak()
    {
        float subpattern[] = new float[]{ 0, 0, 1, 1, 0  };
        float signal[] = makePeriodic(subpattern, 50);
        T2<Integer, Float> maxima = pattern.getPeak(signal);

        System.out.println("Period: " + subpattern.length + ", Peak harmonic index: " + maxima.getA() + ", peak value: " + maxima.getB());

    }
}
