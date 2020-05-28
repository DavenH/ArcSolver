package gen.grid;

import common.Test;

public class GridTest extends Test
{
    public static ColorGrid makeShapelyGrid()
    {
        ColorGrid grid = new ColorGrid(null,
                                       "0000000000" +
                                       "0001224000" +
                                       "0001144000" +
                                       "0001554000" +
                                       "0022266600" +
                                       "0022266000" +
                                       "0022266000" +
                                       "0077766000", 10);

        return grid;
    }

    public static ColorGrid makeSegmentedGrid()
    {
        ColorGrid grid = new ColorGrid(null,
                                       "0040000011" +
                                       "0041224011" +
                                       "0040000011" +
                                       "0041554011" +
                                       "0042266611" +
                                       "4444444444" +
                                       "0022266400" +
                                       "0077766400", 10);

        return grid;
    }

    public static ColorGrid makeWackyGrid()
    {
        ColorGrid grid = new ColorGrid(null, "10239485610934109" +
                                             "46103941834123604" +
                                             "10000000000087483" +
                                             "90123491049384710" +
                                             "234817341", 9);
        return grid;
    }
}
