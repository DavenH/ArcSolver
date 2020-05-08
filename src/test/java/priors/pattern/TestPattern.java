package priors.pattern;

import common.Test;
import gen.grid.ColorGrid;
import gen.primitives.Colour;
import gen.primitives.Pos;
import gen.priors.abstraction.Symmetry;
import gen.priors.pattern.Pattern;
import problem.Task;
import util.ArrayUtil;
import util.T2;

import java.util.Arrays;
import java.util.List;

public class TestPattern extends Test
{
    Pattern pattern = new Pattern();

    public void testInfill2()
    {
        List<Task> tasks = getTasks(new int[]{ 60 });

        for (Task task : tasks)
        {
            System.out.println("---------------------------");

            List<Task.Sample> train = task.getTrainSamples();
            List<Task.Sample> train2 = Arrays.asList(train.get(3));

            for (Task.Sample sample : train2)
            {
                ColorGrid constructedOutput = sample.input.copy();
                ColorGrid guess = sample.input.copy();

                System.out.println(constructedOutput.toString());

                List<Symmetry> bases = pattern.getTransSymmetries(constructedOutput, 2);
                constructedOutput.setBackground(Colour.Black);

                for(Symmetry base : bases)
                    System.out.println(base.pos + ", " + base.cellsObeying);

                for(int y = 0; y < constructedOutput.getHeight(); ++y) {
                    for(int x = 0; x < constructedOutput.getWidth(); ++x) {
                        int[] counts = new int[Colour.values().length];
                        Pos xy = new Pos(x, y);
                        for(Symmetry base : bases) {
                            int iSpan = 3;
                            for(int i = -iSpan; i <= iSpan; ++i) {
                                Pos scaled = base.pos.times(i);
                                Pos p = xy.plus(scaled);

                                if(constructedOutput.isNotEmpty(p)) {
                                    Colour c = constructedOutput.get(p);
                                    System.out.println(x + ", " + y + "; " + i + "; " + p.x + ", " + p.y + " -\t" + c);
                                    counts[c.ordinal()] += base.cellsObeying;
                                }
                            }
                        }

                        T2<Integer, Integer> idxVal = ArrayUtil.maxIndexAndValue(counts, 0);
                        Colour bestColor = Colour.toColour(idxVal.getA());
                        guess.set(xy, bestColor);
                        boolean correct = sample.output.get(xy) == bestColor;

                        System.out.println(x + ", " + y + ": [" + idxVal.getB() + "] best = " + bestColor);
                        if(! correct)
                            System.out.println("Incorrect");

                    }
                }

                // try to reconstruct by copying texture along these basic vectors and vote
                int numErrors = guess.compare(sample.output, (a, b) -> a.equals(b) ? 0 : 1, Integer::sum, 0);
                int origErrors = sample.input.compare(sample.output, (a, b) -> a.equals(b) ? 0 : 1, Integer::sum, 0);

                System.out.println(guess.toString());
                System.out.println("Num errors: " + numErrors + " origin errors: " + origErrors);
            }
        }
    }

    public void testFindSymmetry()
    {
        List<Task> tasks = getTasks(new int[] {73,174,202,241,286,350,399});


        for (Task task : tasks)
        {
            List<Task.Sample> train = task.getTrainSamples();

            for (Task.Sample sample : train)
            {
                Iterable<T2<Pos, Integer>> symmetrySeedsI = pattern.findSymmetrySeeds(sample.input, 5);
                log("Input:");
                log(symmetrySeedsI);

                Iterable<T2<Pos, Integer>> symmetrySeedsO = pattern.findSymmetrySeeds(sample.output, 5);
                log("Output:");
                log(symmetrySeedsO);
            }
        }
    }

    public void testCrosshatchDetect()
    {
        List<Task> tasks = getTasks(new int[]{ 243, 8, 10, 79, 32, 184, 60, 256, 162, 148, 58, 390, 313 });
//        List<Task> tasks = getTasks(new int[]{ 8  });
        // what are the "null hypothesis" kernels? Ones that will reject all crosshatches
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

                for(int j = 0; j < kernel.length; ++j)
                {
                    int[] negPosNeg = kernel[j];
                    float avg = ArrayUtil.average(negPosNeg);
                    float[][] arrs = new float[][] { trans.horz, trans.vert };
                    for(float[] arr : arrs)
                    {
                        for(int k = 0; k < arr.length; ++k)
                        {
                            float sum = 0;
                            float negsum = 0;

                            for(int i = 0; i < negPosNeg.length; ++i)
                            {
                                int arrIdx = k + i - (negPosNeg.length - 1) / 2;

                                // treat out of bounds as a negative
                                boolean outBounds = arrIdx < 0 || arrIdx >= arr.length;
                                sum     += outBounds ? avg : (negPosNeg[i] - avg) * arr[arrIdx];
                                negsum  += outBounds ? avg : (avg - negPosNeg[i]) * arr[arrIdx];
                            }

                            float delta = sum - negsum;
                            if(delta > maxVal)
                            {
                                bestIdx = j;
                                maxVal = delta;
                            }
                        }
                    }
                }

                System.out.println("Best index " + bestIdx + " maxval: " + maxVal);
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
        int[][] subpattern = { { 1, 3, 4, 5, 2 },
                               { 4, 4, 7, 1, 2 },
                               { 5, 5, 7, 3, 4 },
                               { 1, 5, 5, 2, 3 } };

        int[][] periodicPat = makePeriodic(subpattern, 4, 4);

        ArrayUtil.print(periodicPat);
        float[][] floatArr = ArrayUtil.toFloat(periodicPat);
        float[][] correlated = pattern.autocorrelation2D(floatArr);

        ArrayUtil.print(correlated, 1);
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

    private int[][] makePeriodic(int[][] subpattern, int copiesW, int copiesH)
    {
        int numRows = subpattern.length;
        int numCols = subpattern[0].length;
        int[][] array = new int[copiesH * numRows][copiesW * numCols];
        int average = 0;

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
        float[] autodiff = pattern.autoTransCorr(values);

        ArrayUtil.print(autodiff, 1);
        T2<Integer, Float> minima = ArrayUtil.minIndexAndValue(autodiff, 1);
        System.out.println();
        System.out.println("Expected periodic length: " + subpattern.length + " autoTransCorr min index: " + minima.getA());
    }

    public void testAutoDiff2D()
    {
        int[][] subpattern = { { 1, 3, 4, 5, 2, 3 },
                                 { 4, 4, 7, 1, 2, 2 },
                                 { 5, 5, 7, 3, 4, 1 },
                                 { 1, 5, 5, 2, 3, 7 } };

        int values[][] = makePeriodic(subpattern, 4, 6);

        long timeMillis = System.currentTimeMillis();
        int[][] autodiff = pattern.autoTransCorr(values);
        long endMillis = System.currentTimeMillis();

        System.out.println(endMillis - timeMillis + " millis");
        System.out.println();

        ArrayUtil.print(autodiff);
        T2<Pos, Integer> minima = ArrayUtil.maxIndexAndValue(autodiff, 1, 1);
        Pos minPos = minima.getA();
        System.out.println("Expected periodic length: (" + subpattern.length +
                           ", " + subpattern[0].length + ") autoTransCorr min position: " +
                           minPos.toString() + " indicating subpattern dim recurs every " +
                           String.format("%dx%d", minPos.x, minPos.y) + " cells");
    }

    public void testInfill()
    {
//        int[] grids = { 16, 60, 304 }; //312
        int[] grids = { 73, 286, 393 };
        int totalErrors = 0;

        List<Task> tasks = getTasks(grids);

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

    public static void main(String[] args)
    {
        TestPattern testFFT = new TestPattern();
//        testFFT.testInfill();
//        testFFT.testAutocorrelation1D();
//        testFFT.testAutocorrelation2D();
//        testFFT.testAutoDiff1D();
//        testFFT.testGetPeak();
//        testFFT.testAutoDiff2D();
//        testFFT.testInfill2();
//        testFFT.testCrosshatchDetect();
        testFFT.testFindSymmetry();
    }

    private void testGetPeak()
    {
        float subpattern[] = new float[]{ 0, 0, 1, 1, 0  };
        float signal[] = makePeriodic(subpattern, 50);
        T2<Integer, Float> maxima = pattern.getPeak(signal);

        System.out.println("Period: " + subpattern.length + ", Peak harmonic index: " + maxima.getA() + ", peak value: " + maxima.getB());

    }
}
