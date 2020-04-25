package gen.priors.pattern;

import gen.grid.ColorGrid;
import gen.grid.Mask;
import gen.primitives.Colour;
import gen.primitives.Pos;
import gen.priors.adt.Array;
import org.antlr.v4.runtime.misc.Pair;
import org.jtransforms.fft.FloatFFT_1D;
import org.jtransforms.fft.FloatFFT_2D;

public class Fourier
{
    /**
     * @param corrupted the grid with the corrupted texture to infill
     * @param noiseReductionFactor harmonic partials will be filtered out when below a threshold determined
     *                             by this value (not a hard filter, but a sigmoid, to prevent Gibbs' phenomenon)
     * @return the number of errors from the desired output, to use as a gauge for adjusting the reduction factor
     */
    public Pair<Integer, Integer> infill(ColorGrid corrupted, ColorGrid pristine, int noiseReductionFactor)
    {
        Array<Mask> masks = corrupted.colorSplit();

        int width = corrupted.getWidth();
        int height = corrupted.getHeight();

        int h = height;
        int w = width;
        FloatFFT_2D fft = new FloatFFT_2D(h, w);

        //        int h = CommonUtils.nextPow2(height);
        //        int w = CommonUtils.nextPow2(width);

        float[][] prepared = new float[h][w * 2];
        ColorGrid output = corrupted.cloneInstance(width, height);

        double[][] maxLikelihoods = new double[h][w];
        Colour[][] colours = new Colour[h][w];

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

            for(int y = 0; y < h; y++) {
                for(int x = 0; x < w; x++) {
                    double magnitude = Math.sqrt(prepared[y][x * 2] * prepared[y][x * 2] +
                                                 prepared[y][x * 2 + 1] * prepared[y][x * 2 + 1]);

                    double mult = 1 / (1 + Math.exp(-(magnitude - noiseReductionFactor)));
                    if(mult < 0.999)
                    {
                        prepared[y][x * 2] *= mult;
                        prepared[y][x * 2 + 1] *= mult;
                    }
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
                output.set(c, r, colours[c][r]);
            }
        }

//        System.out.println(corrupted.toString());
//        System.out.println(output.toString());
//        System.out.println(pristine.toString());

        if(pristine != null)
        {
            int numErrors = output.compare(pristine, (a, b) -> a == b ? 0 : 1, Integer::sum, 0);
            int origErrors = corrupted.compare(pristine, (a, b) -> a == b ? 0 : 1, Integer::sum, 0);
            return new Pair<>(numErrors, origErrors);
        }

        return new Pair<>(0, 0);
    }

    public int getPeak(double[] array)
    {
        FloatFFT_1D fft = new FloatFFT_1D(array.length);

        float[] prepared = new float[array.length];
        for(int i = 0; i < array.length; ++i)
            prepared[i] = (float) array[i];

        fft.realForward(prepared);

        float[] result = new float[prepared.length / 2];

        float max = Float.MIN_VALUE;
        int maxIndex = 0;
        for(int i = 0; i < result.length; ++i)
        {
            float re = prepared[i * 2];
            float im = prepared[i * 2 + 1];

            result[i] = (float) Math.sqrt(re * re + im * im);
            if(result[i] > max)
            {
                maxIndex = i;
                max = result[i];
            }
        }

        return maxIndex;
    }
}
