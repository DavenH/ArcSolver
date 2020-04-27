package gen.priors.pattern;

import gen.grid.ColorGrid;
import gen.grid.Grid;
import gen.grid.Mask;
import gen.primitives.Colour;
import gen.primitives.Pos;
import gen.priors.adt.Array;
import org.apache.commons.math3.complex.Complex;
import org.jtransforms.fft.FloatFFT_1D;
import org.jtransforms.fft.FloatFFT_2D;
import util.ArrayUtil;
import util.Pair;
import util.T2;

import java.util.function.BinaryOperator;

public class Pattern
{
    public Pair<float[][]> transform(float[][] input)
    {
        int h = input.length;
        int w = input[0].length;

        float[][] prepared = new float[h][w * 2];
        float[][] magnitudes = new float[h][w];
        float[][] phases = new float[h][w];
        FloatFFT_2D fft = new FloatFFT_2D(h, w);
        float average = ArrayUtil.average(input);
        fft.complexForward(prepared);
        for(int r = 0; r < h; r++) {
            for(int c = 0; c < w; c++) {
                prepared[r][c*2] = input[r][c] - average;
                prepared[r][c*2+1] = 0;
            }
        }

        fft.complexForward(prepared);

        for(int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                float imag = x == 0 ? 0 : prepared[y][x * 2 + 1];
                float magnitude = (float) Math.sqrt(mag2(prepared[y][x * 2], imag));

                magnitudes[y][x] = magnitude;
                phases[y][x] = (float) Math.atan2((double) imag, (double) prepared[y][x * 2]);
            }
        }

        return new Pair<>(magnitudes, phases);
    }

    /**
     * @param corrupted the grid with the corrupted texture to reduceFourierNoise
     * @param noiseReductionFactor harmonic partials will be filtered out when below a threshold determined
     *                             by this value (not a hard filter, but a sigmoid, to prevent Gibbs' phenomenon)
     * @param iterations
     * @return the number of errors from the desired output, to use as a gauge for adjusting the reduction factor,
     *         or for detecting translational symmetry
     */
    public ColorGrid reduceFourierNoise(ColorGrid corrupted,
                                        int noiseReductionFactor,
                                        int iterations)
    {
        return filter(corrupted, iterations,
                      (freq, ratio) -> {
                        return (float) (1 / (1 + Math.exp(-(Math.sqrt(ratio) * 0.3 - noiseReductionFactor))));
                      });
    }

    public ColorGrid highpass(ColorGrid corrupted)
    {
        return filter(corrupted, 1,
                      (freq, ratio) -> {
                          return Float.valueOf(freq < 0.6 ? 1 : 0);
                      });
    }

    public ColorGrid filter(ColorGrid input, int iterations, BinaryOperator<Float> opReturningMagnAdjustment)
    {
        input = input.copy();

        int width = input.getWidth();
        int height = input.getHeight();

        int h = height;
        int w = width;
        FloatFFT_2D fft = new FloatFFT_2D(h, w);

        //        int h = CommonUtils.nextPow2(height);
        //        int w = CommonUtils.nextPow2(width);

        float[][] prepared = new float[h][w * 2];

        for(int iter = 0; iter < iterations; ++iter)
        {
            Colour[][] colours = new Colour[h][w];
            double[][] maxLikelihoods = new double[h][w];
            Array<Mask> masks = input.colorSplit();
            for(Mask mask : masks)
            {
                Pos mPos = mask.getPos();
                for(int r = mPos.y; r < h; r++) {
                    for(int c = mPos.x; c < w; c++) {

                        prepared[r][c*2] = mask.get(c - mPos.x, r - mPos.y) ? 1 : 0;
                        prepared[r][c*2+1] = 0;
                    }
                }

                fft.complexForward(prepared);

                float maxMagnitude = 0;
                for(int y = 0; y < h; y++) {
                    for (int x = 0; x < w; x++) {
                        float imag = prepared[y][2 * x + 1];
                        maxMagnitude = Math.max(maxMagnitude, mag2(prepared[y][x * 2], imag));
                    }
                }

                for(int y = 0; y < h; y++) {
                    for(int x = 0; x < w; x++) {
                        float freq = (x + y) / (float) (h + w - 1);
                        float imag = prepared[y][2 * x + 1];
                        float magnitude = mag2(prepared[y][x * 2], imag);
                        float magnitudeRatio = magnitude / maxMagnitude;
                        float mult = opReturningMagnAdjustment.apply(freq, magnitudeRatio);

                        prepared[y][x * 2] *= mult;
                        prepared[y][x * 2 + 1] *= mult;
                    }
                }


                fft.complexInverse(prepared, true);

                for(int r = 0; r < h ; r++) {
                    for(int c = 0; c < w; c++) {
                        double value = prepared[r][c*2];
                        double maxValue = maxLikelihoods[c][r];

                        Colour bestColor = colours[c][r];
                        if(bestColor == null || value > maxValue)
                        {
                            maxLikelihoods[c][r] = value;
                            colours[c][r] = mask.getBrush();
                        }
                    }
                }
            }

            for(int r = 0; r < h; r++) {
                for(int c = 0; c < w; c++) {
                    input.set(c, r, colours[c][r]);
                }
            }
        }

        return input.copy();
    }



    private float mag2(float re, float im)
    {
        return re * re + im * im;
    }

    public T2<Integer, Float> getPeak(float[] array)
    {
        FloatFFT_1D fft = new FloatFFT_1D(array.length);

        float[] prepared = new float[array.length];
        float average = 0;
        for(int i = 0; i < array.length; ++i)
            average += array[i];
        average /= (float) array.length;

        for(int i = 0; i < array.length; ++i)
            prepared[i] = array[i] - average;

        fft.realForward(prepared);

        float[] result = new float[prepared.length / 2];
        for(int i = 0; i < result.length; ++i)
        {
            float imag = i == 0 ? 0 : prepared[i * 2 + 1];
            result[i] = (float) Math.sqrt(mag2(prepared[i * 2], imag));
        }

        ArrayUtil.print(result, 2);
        return ArrayUtil.maxIndexAndValue(result, 0);
    }

    /*
    if n is even then
       a[2*k] = Re[k], 0<=k<n/2
       a[2*k+1] = Im[k], 0<k<n/2
       a[1] = Re[n/2]

    if n is odd then
           a[2*k] = Re[k], 0<=k<(n+1)/2
           a[2*k+1] = Im[k], 0<k<(n-1)/2
           a[1] = Im[(n-1)/2]
     */

    public float dot(float[] arr1, float[] arr2)
    {
        float avgA = 0;
        float avgB = 0;
        for(int i = 0; i < arr1.length; ++i)    avgA += arr1[i];
        for(int i = 0; i < arr2.length; ++i)    avgB += arr2[i];
        avgA /= (float) arr1.length;
        avgB /= (float) arr2.length;
        float dot = 0;
        for(int i = 0; i < Math.min(arr1.length, arr2.length); ++i)
        {
            dot += (arr1[i] - avgA) * (arr2[i] - avgB);
        }

        if(arr1.length > arr2.length)
        {
            for(int i = arr2.length; i < arr1.length; ++i)
                dot -= (arr1[i]);
        }
        else if(arr1.length < arr2.length)
        {
            for(int i = arr1.length; i < arr2.length; ++i)
                dot -= (arr2[i]);
        }

        return dot;
    }

    public float[] autocorrelation1D(float[] array)
    {
        float[] corr = new float[array.length];

        float average = 0;
        for(int i = 0; i < array.length; ++i)
            average += array[i];

        average /= (float) array.length;

        for(int i = 0; i < array.length; ++i)
        {
            corr[i] = 0;
            for(int j = i; j < corr.length; ++j)
                corr[i] += Math.abs((array[j - i] - average) * (array[j] - average));
        }

        return corr;
        /*
        FloatFFT_1D fft = new FloatFFT_1D(array.length * 2);

        // double the size to pad with zeros so that FFT's cyclical convolution doesn't wrap
        float[] prepared = new float[array.length * 4];
        for(int i = 0; i < array.length; ++i)
            prepared[2 * i] = array[i];

        for(int i = prepared.length / 2; i < prepared.length; ++i)
            prepared[i] = 0;

        fft.complexForward(prepared);

        for(int x = 0; x < prepared.length / 2; ++x)
        {
            // Jtransforms packs the imaginary component from bin[nyquist/2] at index 1;
            // the actual imaginary value of bin[0] is always 0 (DC offset can't have a phase)
            float imag = x == 0 ? 0 : prepared[2 * x + 1];
            Complex c = new Complex(prepared[2 * x], imag);
            c = c.multiply(c.conjugate());
            prepared[2 * x] = (float) c.getReal();
            prepared[2 * x + 1] = (float) c.getImaginary();
        }

        fft.complexInverse(prepared, true);

        float[] halved = new float[prepared.length / 4];
        for(int i = 0; i < halved.length; ++i)
            halved[i] = prepared[i * 2];

        return halved;
        */
    }
    public float[] autodiff(float[] values)
    {
        float[] autodiff = new float[values.length];

        for(int i = 0; i < values.length; ++i)
        {
            // slight linear increase to bias the minimum of the array toward preferring the longest overlaps
            autodiff[i] = i * 0.001f;
            for(int j = i; j < values.length; ++j)
                autodiff[i] += Math.abs(values[j - i] - values[j]);
        }

        return autodiff;
    }

    public float[][] autodiff(float[][] values)
    {
        float[][] autodiff = new float[values.length][values[0].length];

        for(int i = 0; i < values.length; ++i)
        {
            for(int j = 0; j < values[i].length; ++j)
            {
                // slight linear increase to bias the minimum of the array toward preferring the longest overlaps
                autodiff[i][j] = (i + j) * 0.001f;

                for(int k = i; k < values.length; ++k)
                    for(int l = j; l < values[i].length; ++l)
                        autodiff[i][j] += values[k - i][l - j] == values[k][l] ? 1 : 0;
            }
        }

        return autodiff;
    }

    public float[][] autocorrelation2D(ColorGrid grid)
    {
        int h = grid.getHeight();
        int w = grid.getWidth();

        float[][] data = new float[h][w];

        for(int y = 0; y < h; ++y) {
            for (int x = 0; x < w; ++x) {
                data[y][x] = grid.get(x, y).ordinal();
            }
        }

        return autocorrelation2D(data);
    }

    public float[][] autocorrelation2D(float[][] input)
    {
        int h = input.length;
        int w = input[0].length;
        FloatFFT_2D fft = new FloatFFT_2D(h * 2, w * 2);

        // width x4 because 2x for real->complex and 2x for zero padding to prevent cyclical convolution
        float[][] prepared = new float[h * 2][w * 4];
        for(int y = 0; y < h; ++y) {
            for(int x = 0; x < w; ++x) {
                prepared[y][x * 2] = input[y][x];
                prepared[y][x * 2 + 1] = 0;
            }
        }

        fft.complexForward(prepared);

        for(int y = 0; y < 2 * h; y++) {
            for(int x = 0; x < 2 * w; x++) {

                // Jtransforms packs the imaginary component from bin[nyquist/2] at index 1;
                // the actual imaginary value of bin[0] is always 0 (DC offset can't have a phase)
                float imag = x == 0 ? 0 : prepared[y][2 * x + 1];

                Complex c = new Complex(prepared[y][2 * x], imag);
                c = c.multiply(c.conjugate());
                prepared[y][2 * x] = (float) c.getReal();
                prepared[y][2 * x + 1] = (float) c.getImaginary();
            }
        }

        fft.complexInverse(prepared, true);
        float[][] subsample = new float[h][w];

        for(int y = 0; y < h; ++y) {
            for(int x = 0; x < w; ++x) {
                subsample[y][x] = prepared[2 * y][2*x];
            }
        }

        return subsample;
    }

    /*
        For an empty board, or for areas with large contiguous regions, this will be very slow because every
        coordinate is the seed of trivial symmetry.

        So only do this if there are a many cell transitions detected, which will fail-fast the search
        in cases without symmetry.
     */
    public Pos findSymmetrySeeds(Grid grid)
    {
        int maxSymmetricalCells = 0;
        Pos bestSeed = new Pos(0, 0);

        for(int i = 1; i < grid.getWidth() - 1; ++i)
        {
            for(int j = 1; j < grid.getHeight() - 1; ++j)
            {
                Pos xy = new Pos(i, j);

                // even-sized symmetry along horz
                if(grid.get(xy).equals(grid.get(xy.left(1))))
                {
                    int[] horzSymIdc = expandHorz(grid, xy.left(1), true);
                    int totalSymmetricalCells = 0;

                    if(horzSymIdc[1] - horzSymIdc[0] >= 3)
                    {
                        for(int k = horzSymIdc[0]; k <= horzSymIdc[1]; ++k)
                        {
                            Pos posAlongHorzSym = new Pos(k, j);

                            // even-sized symmetry along vert
                            if(grid.get(posAlongHorzSym.down(1)).equals(grid.get(posAlongHorzSym)))
                            {
                                int[] vertSymIdc = expandVert(grid, posAlongHorzSym.down(1), true);
                                totalSymmetricalCells += vertSymIdc[1] - vertSymIdc[0] + 1;
                            }
                            // odd-sized symmetry along vert
                            else if(grid.get(posAlongHorzSym.down(1)).equals(grid.get(posAlongHorzSym.up(1))))
                            {
                                int[] vertSymIdc = expandVert(grid, posAlongHorzSym, true);
                                totalSymmetricalCells += vertSymIdc[1] - vertSymIdc[0] + 1;
                            }
                        }

                        if(totalSymmetricalCells > maxSymmetricalCells)
                        {
                            bestSeed = xy;
                            maxSymmetricalCells = totalSymmetricalCells;
                        }
                    }
                }
                // odd-sized symmetry along horz
                if(grid.get(xy.left(1)).equals(grid.get(xy.right(1))))
                {
                    int[] horzSymIdc = expandHorz(grid, xy, false);
                    int totalSymmetricalCells = 0;

                    if(horzSymIdc[1] - horzSymIdc[0] >= 2)
                    {
                        for(int k = horzSymIdc[0]; k <= horzSymIdc[1]; ++k)
                        {
                            Pos posAlongHorzSym = new Pos(k, j);

                            // even-sized symmetry along vert
                            if(grid.get(posAlongHorzSym.down(1)).equals(grid.get(posAlongHorzSym)))
                            {
                                int[] vertSymIdc = expandVert(grid, posAlongHorzSym.down(1), true);
                                totalSymmetricalCells += vertSymIdc[1] - vertSymIdc[0] + 1;
                            }
                            // odd-sized symmetry along vert
                            else if(grid.get(posAlongHorzSym.down(1)).equals(grid.get(posAlongHorzSym.up(1))))
                            {
                                int[] vertSymIdc = expandVert(grid, posAlongHorzSym, true);
                                totalSymmetricalCells += vertSymIdc[1] - vertSymIdc[0] + 1;
                            }
                        }

                        if(totalSymmetricalCells > maxSymmetricalCells)
                        {
                            bestSeed = xy;
                            maxSymmetricalCells = totalSymmetricalCells;
                        }
                    }
                }
            }
        }

        return bestSeed;
    }

    /**
     * @param grid
     * @param leftPos
     * @param isEven
     * @return the min and max indices that have symmetry between them
     */
    public int[] expandHorz(Grid grid, Pos leftPos, boolean isEven)
    {
        Pos rightPos = leftPos.copy();
        if(isEven)
            rightPos = rightPos.right(1);

        while(leftPos.x > 0 && rightPos.x < grid.getWidth() - 1 &&
              grid.get(leftPos).equals(grid.get(rightPos)))
        {
            --leftPos.x;
            ++rightPos.x;
        }

        return new int[] { leftPos.x, rightPos.x };
    }

    public int[] expandVert(Grid grid, Pos lowPos, boolean isEven)
    {
        Pos topPos = lowPos.copy();

        if(isEven)
            topPos = topPos.up(1);

        while(lowPos.y > 0 && topPos.y < grid.getHeight() - 1 &&
              grid.get(lowPos).equals(grid.get(topPos)))
        {
            --lowPos.y;
            ++topPos.y;
        }

        return new int[] { lowPos.y, topPos.y };
    }
}
