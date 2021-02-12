package problem;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.PrintWriter;
import java.util.*;

public class Controller
{
    List<Task> trainTasks;
    List<Task> testTasks;
    Map<String, Integer> codeToIndex = new HashMap<>();

    public Controller()
    {
        trainTasks = new ArrayList<>();
        testTasks = new ArrayList<>();
    }

    public void loadTasks(File directory)
    {
        File trainingDir = new File(directory.getAbsolutePath() + File.separatorChar + "training");
        File evalDir = new File(directory.getAbsolutePath() + File.separatorChar + "evaluation");

        try
        {
            int count = 0;
            for(File file : trainingDir.listFiles())
            {
                JsonReader reader = new JsonReader(new FileReader(file));
                JsonElement elem = JsonParser.parseReader(reader);

                Task task = new Task(file.getName(), elem.getAsJsonObject());
                trainTasks.add(task);

                codeToIndex.put(file.getName(), count++);
            }

            count = 0;
            for(File file : evalDir.listFiles())
            {
                JsonReader reader = new JsonReader(new FileReader(file));
                JsonElement elem = JsonParser.parseReader(reader);

                Task task = new Task(file.getName(), elem.getAsJsonObject());
                testTasks.add(task);
                codeToIndex.put(file.getName(), count++);
            }
        }
        catch (FileNotFoundException e)
        {
            e.printStackTrace();
        }
    }

    public List<Task> getTasks(int[] indices, boolean training)
    {
        List<Task> trainTasks = getTasks(training);
        List<Task> indexedTasks = new ArrayList<>();

        for(int index : indices)
        {
            indexedTasks.add(trainTasks.get(index));
        }

        return indexedTasks;
    }

    public List<Task> getRandomTasks(int number, boolean training)
    {
        List<Task> trainTasks = getTasks(training);
        List<Task> indexedTasks = new ArrayList<>();

        Random random = new Random();
        for(int i = 0; i < number; ++i)
        {
            int index = random.nextInt(trainTasks.size());
            indexedTasks.add(trainTasks.get(index));
        }

        return indexedTasks;
    }


    public void saveTasks(File directory)
    {
        File trainingDir = new File(directory.getAbsolutePath() + File.separatorChar + "training");
        File evalDir = new File(directory.getAbsolutePath() + File.separatorChar + "evaluation");

        saveTasks(trainingDir, trainTasks);
        saveTasks(evalDir, testTasks);
    }

    private void saveTasks(File trainingDir, List<Task> tasks)
    {
        try
        {
            for(Task task : tasks)
            {
                if(! task.hasChanged())
                    continue;

                try(PrintWriter pw = new PrintWriter(trainingDir.getAbsolutePath() + File.separatorChar + task.getTaskCode()))
                {
                    JsonObject taskJson = task.toJson();

                    pw.write(taskJson.toString());
                    pw.flush();
                }
                catch (FileNotFoundException e)
                {
                    e.printStackTrace();
                }
            }
        }
        catch (Throwable e)
        {
            e.printStackTrace();
        }
    }


    public static void main(String[] args)
    {
//        Controller controller = new Controller();
//        controller.loadTasks(new File("G:\\Data\\reasoning\\ARC-master\\data"));

        File file = new File("/c/Users/Public/data/test.txt");
//        File file = new File("G:/Data/test.txt");

        try
        {
            boolean result = file.createNewFile();

            PrintWriter pw = new PrintWriter(file);
            pw.print("Hello.");
            pw.flush();

            if(pw.checkError())
            {
                System.out.println("Print error");
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    public List<Task> getTasks(boolean training)
    {
        return training ? trainTasks : testTasks;
    }

    public int getIndex(String code)
    {
        return codeToIndex.getOrDefault(code, 0);
    }
}
