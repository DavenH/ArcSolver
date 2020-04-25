package gen.grid;

import gen.priors.pattern.Fourier;
import org.antlr.v4.runtime.misc.Pair;
import problem.Controller;
import problem.Task;

import java.io.File;
import java.util.List;

public class TestSymmetry extends GridTest
{
    public void testFFT(ColorGrid input, ColorGrid output)
    {
        Fourier fourier = new Fourier();
        Pair<Integer,Integer> error = fourier.infill(input, output, 6);

        System.out.println("Error, from " + error.b + " to " + error.a);
    }

    public static void main(String[] args)
    {
        TestSymmetry testSymmetry = new TestSymmetry();

        Controller controller = new Controller();
        controller.loadTasks(new File("C:\\Users\\Public\\data\\ARC-master\\data\\"));
        List<Task> trainTasks = controller.getTasks(true);

        int[] textureGrids = { 16, 73, 286 };
        for(int idx : textureGrids)
        {
            line();

            Task task = trainTasks.get(idx);

            List<Task.Sample> train = task.getTrainSamples();
            for(Task.Sample sample : train)
            {
                testSymmetry.testFFT(sample.input, sample.output);
            }
        }
    }
}
