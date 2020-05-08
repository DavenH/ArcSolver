package common;

import problem.Controller;
import problem.Task;

import java.io.File;
import java.util.List;

public class Test
{
    public List<Task> getTasks(int[] indices)
    {
        Controller controller = new Controller();
        controller.loadTasks(new File("C:\\Users\\Public\\data\\ARC-master\\data\\"));
        return controller.getTasks(indices, true);
    }

    public static void log(Object obj)
    {
        System.out.println(obj == null ? null : obj.toString());
    }

    public static void log(Iterable<?> obj)
    {
        System.out.print("[");
        for(Object o : obj)
            System.out.print(o == null ? null : o.toString() + " ");
        System.out.println("]");
    }

    public static void log2(Object obj)
    {
        System.out.println("\n" + obj == null ? null : obj.toString());
    }

    public static void line()
    {
        log("\n-----------------------------------------\n");
    }
}
