package util;

import gen.primitives.Pos;

public class ArrayUtil
{
    public static T2<Integer, Float> minIndexAndValue(float[] values, int startIdx)
    {
        float minValue = Float.MAX_VALUE;
        int minIdx = 0;
        for(int i = startIdx; i < values.length; ++i)
        {
            if(values[i] < minValue)
            {
                minIdx = i;
                minValue = values[i];
            }
        }

        return new T2<>(minIdx, minValue);
    }

    public static T2<Integer, Float> maxIndexAndValue(float[] values, int startIdx)
    {
        float maxValue = Float.MIN_VALUE;
        int maxIdx = 0;
        for(int i = startIdx; i < values.length; ++i)
        {
            if(values[i] > maxValue)
            {
                maxIdx = i;
                maxValue = values[i];
            }
        }

        return new T2<>(maxIdx, maxValue);
    }

    public static T2<Pos, Float> minIndexAndValue(float[][] values, int startI, int startJ)
    {
        float minValue = Float.MAX_VALUE;
        Pos pos = new Pos(0, 0);

        for(int i = startI; i < values.length; ++i)
        {
            for(int j = startJ; j < values[i].length; ++j)
            {
                if(values[i][j] < minValue)
                {
                    pos = new Pos(i, j);
                    minValue = values[i][j];
                }
            }
        }

        return new T2<>(pos, minValue);
    }

    public static float[] copy(float[] array)
    {
        float[] arr = new float[array.length];
        System.arraycopy(array, 0, arr, 0, array.length);

        return arr;
    }

    public static void add(float[] array, float value)
    {
        for(int i = 0; i < array.length; ++i)
            array[i] += value;
    }

    public static void inv(float[] array, float c)
    {
        for(int i = 0; i < array.length; ++i)
            array[i] = 1 / (array[i] + c);
    }

    public static float average(float[] array)
    {
        float average = 0;
        for(int i = 0; i < array.length; ++i)
            average += array[i];
        average /= (float) array.length;
        return average;
    }

    public static float average(float[][] array)
    {
        float average = 0;
        for(int i = 0; i < array.length; ++i) {
            for (int j = 0; j < array[i].length; ++j) {
                average += array[i][j];
            }
        }
        average /= (float) (array.length * array[0].length);
        return average;
    }

    public static float[] reverse(float[] array)
    {
        float[] arr = new float[array.length];
        for(int i = 0; i < array.length; ++i)
            arr[array.length - 1 - i] = array[i];
        return arr;
    }

    public static T2<Pos, Float> maxIndexAndValue(float[][] values, int startI, int startJ)
    {
        float maxValue = Float.MIN_VALUE;
        Pos pos = new Pos(0, 0);

        for(int i = startI; i < values.length; ++i)
        {
            for(int j = startJ; j < values[i].length; ++j)
            {
                if(values[i][j] > maxValue)
                {
                    pos = new Pos(i, j);
                    maxValue = values[i][j];
                }
            }
        }

        return new T2<>(pos, maxValue);
    }

    public static void print(float[][] values, int decimals)
    {
        for (int y = 0; y < values.length; ++y) {
            for(int x = 0 ; x < values[y].length; ++x) {
                System.out.print(String.format("%3." + decimals + "f ", values[y][x]));
            }
            System.out.println();
        }
    }

    public static void print(float[] values, int decimals)
    {
        for (int y = 0; y < values.length; ++y) {
            System.out.println(String.format("%3." + decimals + "f", values[y]));
        }
        System.out.println();
    }
}
