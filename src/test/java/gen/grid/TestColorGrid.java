package gen.grid;

import gen.priors.adt.Array;
import problem.Task;

import java.util.List;

public class TestColorGrid extends GridTest
{
    public void testEntropy()
    {
//        log("Top 5: \n\n");
//        for(int i = 0; i < 5; ++i)
//        {
//            Task.Sample sample = metrics.get(i).a;
//            log(sample.input.toString());
//            log(sample.output.toString());
//            line();
//        }
//
//        log("Bottom 5: \n\n");


    }

    public void testColorSplit()
    {
        log("Colour split");
        ColorGrid grid = makeWackyGrid();

        Array<Mask> planes = grid.colorSplit();
        for(Mask mask : planes)
            log(mask.toString() + "\n");
        log("\nShapes\n");
    }

    public void testDetectShapes()
    {
        log("Testing shapes");

        ColorGrid grid = makeShapelyGrid();
        Array<Mask> shapes = grid.shapesDiag();
        for(Mask mask : shapes)
            log(mask.toString() + "\n");
        log("\n");

        ColorGrid cg = new ColorGrid(null, 10, 8);
        for(Mask mask : shapes)
            cg.draw(mask);
        log(cg.toString());

        log("is same as original = " + cg.equals(grid));
    }

    public void testBuildTree()
    {
        List<Task> tasks = getTasks(new int[] {73,174,202,241,286,350,399});
    }

    public static void main(String[] args)
    {
        TestColorGrid testColorGrid = new TestColorGrid();
        testColorGrid.testEntropy();
    }
}
