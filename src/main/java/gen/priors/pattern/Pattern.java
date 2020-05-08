package gen.priors.pattern;

import gen.grid.ColorGrid;
import gen.grid.Grid;
import gen.grid.Mask;
import gen.primitives.Colour;
import gen.primitives.Pos;
import gen.priors.abstraction.Symmetry;
import gen.priors.abstraction.SymmetryType;
import gen.priors.adt.Array;
import org.apache.commons.math3.complex.Complex;
import org.jtransforms.fft.FloatFFT_1D;
import org.jtransforms.fft.FloatFFT_2D;
import util.ArrayUtil;
import util.Pair;
import util.T2;

import java.util.*;
import java.util.function.BinaryOperator;

public class Pattern
{
    public Pair<float[][]> transform(float[][] input)
    {
        int h = input.length;
        int w = input[0].length;

        float[][] prepared   = new float[h][w * 2];
        float[][] magnitudes = new float[h][w];
        float[][] phases     = new float[h][w];

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
            float[][] maxLikelihoods = new float[h][w];
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
                        float value = prepared[r][c*2];
                        float maxValue = maxLikelihoods[r][c];

                        Colour bestColor = colours[r][c];
                        if(bestColor == null || value > maxValue)
                        {
                            maxLikelihoods[r][c] = value;
                            colours[r][c] = mask.getBrush();
                        }
                    }
                }
            }

            for(int r = 0; r < h; r++) {
                for(int c = 0; c < w; c++) {
                    input.set(c, r, colours[r][c]);
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
            dot += (arr1[i] - avgA) * (arr2[i] - avgB);

        if(arr1.length > arr2.length)
        {
            for(int i = arr2.length; i < arr1.length; ++i)
                dot -= (arr1[i]);
        }
        else if(arr1.length < arr2.length)
        {
            for(int i = arr1.length; i < arr2.length; ++i)
                dot -= arr2[i];
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
                corr[i] += Math.abs((array[j - i] - average) *
                                    (array[j] - average));
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

    public float[] autoTransCorr(float[] values)
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

    public int[][] autoTransCorr(int[][] values)
    {
        int h = values.length;
        int w = values[0].length;

        int[][] autodiff = new int[h][w];

        for(int i = 0; i < h; ++i) {
            for(int j = 0; j < w; ++j) {
//                // slight linear increase to bias the minimum of the array toward preferring the longest overlaps
//                autoTransCorr[i][j] = (i + j) * 0.001f;

                for(int k = i; k < h; ++k)
                    for(int l = j; l < w; ++l)
                        autodiff[i][j] += values[k - i][l - j] == values[k][l] ? 1 : 0;
            }
        }

        return autodiff;
    }

    public T2<float[][], List<Symmetry>> autosym(int[][] values)
    {
        Pair<int[]> horzes  = getHorzSymmetry(values);  // odd, even
        Pair<int[]> verts   = getVertSymmetry(values);  // odd, even

        int[] negdiag       = getNegDiagSymCollapsed(values);
        int[] diag          = getDiagSymCollapsed(values);

        T2<Integer, Integer> hmax  = ArrayUtil.maxIndexAndValue(horzes.getA(), 0);
        T2<Integer, Integer> hemax = ArrayUtil.maxIndexAndValue(horzes.getB(), 0);
        T2<Integer, Integer> vmax  = ArrayUtil.maxIndexAndValue(verts.getA(), 0);
        T2<Integer, Integer> vemax = ArrayUtil.maxIndexAndValue(verts.getB(), 0);
        T2<Integer, Integer> ndmax  = ArrayUtil.maxIndexAndValue(negdiag, 0);
        T2<Integer, Integer> dmax = ArrayUtil.maxIndexAndValue(diag, 0);

        Integer max = Collections.max(Arrays.asList(hmax.getA(), hemax.getA(), vmax.getA(), vemax.getA(), dmax.getA(), ndmax.getA()));

        int h = values.length;
        int w = values[0].length;

        float imax = 1 / (float) max;
        float[][] autosym = new float[h][w];

        for(int y = 0; y < h; ++y)
        {
            for (int x = 0; x < w; ++x)
            {
                autosym[y][x] += imax * (horzes.getA()[y] + verts.getA()[x]);
                autosym[y][x] += imax * negdiag[h - 1 + x - y];
                autosym[y][x] += imax * diag[y + x];
            }

            for (int x = 1; x < w; ++x)
                autosym[y][x] += imax * verts.getB()[x - 1];
        }

        for(int x = 0; x < w; ++x)
            for(int y = 1; y < h; ++y)
                autosym[y][x] += imax * horzes.getB()[y - 1];

        T2<Pos, Float> maxPos = ArrayUtil.maxIndexAndValue(autosym, 0, 0);
        Symmetry hSym  = new Symmetry(SymmetryType.Horz,    false,  maxPos.getA(), hmax.getB());
        Symmetry heSym = new Symmetry(SymmetryType.Horz,    true,   maxPos.getA(), hemax.getB());
        Symmetry vSym  = new Symmetry(SymmetryType.Vert,    false,  maxPos.getA(), vmax.getB());
        Symmetry veSym = new Symmetry(SymmetryType.Vert,    true,   maxPos.getA(), vemax.getB());
        Symmetry dSym  = new Symmetry(SymmetryType.Diag,    false,  maxPos.getA(), dmax.getB());
        Symmetry ndSym = new Symmetry(SymmetryType.NegDiag, false,  maxPos.getA(), ndmax.getB());

        List<Symmetry> syms = Arrays.asList(hSym, heSym, vSym, veSym, dSym, ndSym);

        return new T2<>(autosym, syms);
    }

    public Pair<int[]> getHorzSymmetry(int[][] values)
    {
        int h = values.length;
        int w = values[0].length;

        int[] hodd  = new int[h];
        int[] heven = new int[h - 1];

        // horizontal symmetry
        for(int y = 0; y < h; ++y) {
            for(int yy = 1; yy + y < h && y - yy >= 0; ++yy)
                for (int x = 0; x < w; ++x)
                    hodd[y] += values[y + yy][x] == values[y - yy][x] ? 1 : 0;

            for(int yy = 0; yy + y + 1 < h && y - yy >= 0; ++yy)
                for (int x = 0; x < w; ++x)
                    heven[y] += values[y + yy + 1][x] == values[y - yy][x] ? 1 : 0;
        }

        return new Pair<>(hodd, heven);
    }

    public Pair<int[]> getVertSymmetry(int[][] values)
    {
        int h = values.length;
        int w = values[0].length;

        int[] vodd  = new int[w];
        int[] veven = new int[w - 1];

        // vertical symmetry
        for(int x = 0; x < w; ++x) {
            for(int xx = 1; xx + x < w && x - xx >= 0; ++xx)
                for (int y = 0; y < h; ++y)
                    vodd[x] += values[y][x + xx] == values[y][x - xx] ? 1 : 0;

            for(int xx = 0; xx + x + 1 < w && x - xx >= 0; ++xx)
                for (int y = 0; y < h; ++y)
                    veven[x] += values[y][x + xx + 1] == values[y][x - xx] ? 1 : 0;
        }

        return new Pair<>(vodd, veven);
    }

    public int[] getDiagSymCollapsed(int[][] values)
    {
        int h = values.length;
        int w = values[0].length;

        Pair<int[][]> nd = getDiagSym(values);
        int[][] ndodd = nd.getA();
        int[][] ndeven = nd.getB();

        int[] negdiag = new int[w + h - 1];

        for(int x = 0; x < w; ++x)
            for(int k = 0; k <= x && k < h; ++k)
                negdiag[x] += ndodd[k][x - k] + ndeven[k][x - k];

        for (int y = 1; y < h; ++y)
            for(int k = 0; k + y < h && (w - 1 - k >= 0); ++k)
                negdiag[w - 1 + y] += ndodd[y + k][w - 1 - k] + ndeven[y + k][w - 1 - k];

        return negdiag;
    }

    public Pair<int[][]> getNegDiagSym(int[][] values)
    {
        int h = values.length;
        int w = values[0].length;

        int[][] ndodd = new int[h][w];
        int[][] ndeven = new int[h][w];

        for(int x = 0; x < w; ++x) {
            for (int y = 0; y < h; ++y) {
                for(int k = 1; y - k >= 0 && x - k >= 0 && y + k < h && x + k < h; ++k)
                    ndodd[y][x] += values[y + k][x - k] == values[y - k][x + k] ? 1 : 0;
            }
        }

        for(int x = 1; x < w; ++x) {
            for (int y = 0; y < h; ++y) {
                for(int k = 0; y - k >= 0 && x - k  - 1 >= 0 && y + k + 1 < h && x + k < h; ++k)
                    ndeven[y][x - 1] += values[y + k + 1][x - k - 1] == values[y - k][x + k] ? 1 : 0;
            }
        }

        return new Pair<>(ndodd, ndeven);
    }

    public Pair<int[][]> getDiagSym(int[][] values)
    {
        int h = values.length;
        int w = values[0].length;
        int[][] dodd = new int[h][w];
        int[][] deven = new int[h][w];

        // diagonal symmetry
        for(int x = 0; x < w; ++x) {
            for (int y = 0; y < h; ++y) {
                for(int k = 1; y - k >= 0 && x - k >= 0 && y + k < h && x + k < h; ++k)
                    dodd[y][x] += values[y - k][x - k] == values[y + k][x + k] ? 1 : 0;
            }
        }

        for(int x = 0; x < w - 1; ++x) {
            for (int y = 0; y < h; ++y) {
                for(int k = 0; y - k >= 0 && x - k >= 0 && y + k + 1 < h && x + k + 1 < h; ++k)
                    deven[y][x + 1] += values[y - k][x - k] == values[y + k + 1][x + k + 1] ? 1 : 0;
            }
        }
        return new Pair<>(dodd, deven);
    }

    public int[] getNegDiagSymCollapsed(int[][] values)
    {
        int h = values.length;
        int w = values[0].length;

        Pair<int[][]> nd = getNegDiagSym(values);
        int[][] diagO = nd.getA();
        int[][] diagE = nd.getB();


        int[] diag = new int[w + h - 1];
        for (int y = 0; y < h; ++y)
            for(int k = 0; k <= y && k < w; ++k)
                diag[y] += diagO[h - 1 - y + k][k] + diagE[h - 1 - y + k][k];

        for(int x = 1; x < w; ++x)
            for(int k = 0; x + k < w && k < h; ++k)
                diag[h - 1 + x] += diagO[k][x + k] + diagE[k][x + k];

        return diag;
    }

    public T2<Pos, Float>[] getBasisVectors(ColorGrid grid)
    {
        int[][] auto = autoTransCorr(grid.toInt());
        auto[0][0] = 0;
        
        float[] xvals = new float[auto[0].length];
        float[] yvals = new float[auto.length];
        float[] xyvals = new float[Math.min(xvals.length, yvals.length)];

        for(int i = 0; i < xvals.length; ++i)   xvals[i] = auto[0][i];
        for(int i = 0; i < yvals.length; ++i)   yvals[i] = auto[i][0];
        for(int i = 0; i < xyvals.length; ++i)  xyvals[i] = auto[i][i];

        T2<Integer, Float> minX = ArrayUtil.maxIndexAndValue(xvals, 1);
        T2<Integer, Float> minY = ArrayUtil.maxIndexAndValue(yvals, 1);
        T2<Pos, Integer> minXY = ArrayUtil.maxIndexAndValue(auto, 1, 1);

        return new T2[] { new T2<>(new Pos(minX.getA(), 0), minX.getB()),
                          new T2<>(new Pos(0, minY.getA()), minY.getB()),
                          minXY
        };
    }

    public Iterable<T2<Pos, Integer>> getBasisVectors(ColorGrid grid, int bases)
    {
        int[][] auto = autoTransCorr(grid.toInt());
        auto[0][0] = 0;

        PriorityQueue<T2<Pos, Integer>> top = new PriorityQueue<>((o1, o2) -> o1.getB() < o2.getB() ? -1 :
                                                                            o1.getB() > o2.getB() ? 1 : 0);

        for(int y = 0; y < auto.length; ++y)
        {
            for(int x = 0; x < auto[y].length; ++x)
            {
                if(top.isEmpty() || top.peek().getB() < auto[y][x])
                    top.add(new T2<>(new Pos(x, y), auto[y][x]));

                if(top.size() > bases)
                    top.poll();
            }
        }

        return top;
    }

    public ColorGrid infill(ColorGrid input)
    {
        ColorGrid guess = input.copy();
        Iterable<T2<Pos, Integer>> bases = getBasisVectors(input, 2);

        for(int y = 0; y < guess.getHeight(); ++y) {
            for(int x = 0; x < guess.getWidth(); ++x) {
                int[] counts = new int[Colour.values().length];

                Pos xy = new Pos(x, y);
                for(T2<Pos, Integer> base : bases) {
                    int iSpan = 3;
                    for(int i = -iSpan; i <= iSpan; ++i) {
                        Pos scaled = base.getA().times(i);
                        Pos p = xy.plus(scaled);

                        if(guess.isNotEmpty(p)) {
                            Colour c = guess.get(p);
                            counts[c.ordinal()] += base.getB();
                        }
                    }
                }

                T2<Integer, Integer> idxVal = ArrayUtil.maxIndexAndValue(counts, 0);
                Colour bestColor = Colour.toColour(idxVal.getA());
                guess.set(xy, bestColor);
            }
        }

        return guess;
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
    public Iterable<T2<Pos, Integer>> findSymmetrySeeds(Grid grid, int limit)
    {
        PriorityQueue<T2<Pos, Integer>> top = new PriorityQueue<>(Comparator.comparing(T2<Pos, Integer>::getB));

        for(int i = 1; i < grid.getWidth() - 1; ++i)
        {
            for(int j = 1; j < grid.getHeight() - 1; ++j)
            {
                addToBestSeeds(top, grid, i, j, limit, true);
                addToBestSeeds(top, grid, i, j, limit, false);
            }
        }

        return top;
    }

    private void addToBestSeeds(PriorityQueue<T2<Pos, Integer>> top,
                               Grid grid, int i, int j, int limit, boolean even)
    {
        Pos xy = new Pos(i, j);
        Pos left = xy.left(1);
        Pos right = xy.right(even ? 0 : 1);

        if(grid.get(left).equals(grid.get(right)))
        {
            int[] horzSymIdc = expandHorz(grid, xy.left(even ? 1 : 0), even);
            int totalSymmetricalCells = 0;

            if(horzSymIdc[1] - horzSymIdc[0] >= (even ? 3 : 2))
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
                        int[] vertSymIdc = expandVert(grid, posAlongHorzSym, false);
                        totalSymmetricalCells += vertSymIdc[1] - vertSymIdc[0] + 1;
                    }
                }

                if(top.isEmpty() || top.peek().getB() < totalSymmetricalCells)
                    top.add(new T2<>(xy.copy(), totalSymmetricalCells));

                if(top.size() > limit)
                    top.poll();
            }
        }
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
