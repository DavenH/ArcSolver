package problem;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import gen.grid.ColorGrid;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Task
{
    public static class Sample
    {
        public ColorGrid input;
        public ColorGrid output;
        public Task task;

        public JsonObject sampleToJson()
        {
            JsonObject taskJson = new JsonObject();
            taskJson.add("input", gridToJson(input));
            taskJson.add("output", gridToJson(output));

            return taskJson;
        }

        public Sample(Task task, JsonObject obj)
        {
            if(obj == null)
                return;

            this.task = task;
            JsonArray inputArr = obj.get("input").getAsJsonArray();
            JsonArray outputArr = obj.get("output").getAsJsonArray();

            this.input = buildGridFromArr(inputArr);
            this.output = buildGridFromArr(outputArr);
        }
    }

    List<Sample> trainSamples;
    List<Sample> testSamples;

    String taskCode;
    String abstractions;
    String description;
    String cues;

    String inputCode;
    String outputCode;

    boolean changed;

    public String getInputCode()    { return inputCode; }
    public String getOutputCode()   { return outputCode; }
    public String getCues()         { return cues; }

    public void setInputCode(String inputCode)
    {
        this.inputCode = inputCode;
        this.changed = true;
    }

    public void setOutputCode(String outputCode)
    {
        this.outputCode = outputCode;
        this.changed = true;
    }

    public void setCues(String cues)
    {
        this.cues = cues;
        this.changed = true;
    }

    public void setTaskCode(String taskCode)        { this.taskCode = taskCode; changed = true; }
    public void setDescription(String description)  { this.description = description; changed = true; }
    public void setAbstractions(String text)        { abstractions = text; changed = true;     }

    public boolean hasChanged()             { return changed; }
    public String getTaskCode()             { return taskCode;          }
    public String getAbstractions()         { return abstractions;      }
    public String getDescription()          { return description;       }
    public List<Sample> getTrainSamples()   { return trainSamples;      }
    public List<Sample> getTestSamples()    { return testSamples;       }

    public Task()
    {
        trainSamples = new ArrayList<>();
        testSamples = new ArrayList<>();
        changed = false;
    }

    public Task(String taskCode, JsonObject json)
    {
        this();

        setTaskCode(taskCode);

        description     = getField(json, "description");
        abstractions    = getField(json, "abstractions");
        cues            = getField(json, "cues");

        inputCode       = getField(json, "input_code");
        outputCode      = getField(json, "output_code");

        JsonArray trainArr = json.getAsJsonArray("train");
        JsonArray testArr = json.getAsJsonArray("test");

        loadSamples(trainArr, trainSamples);
        loadSamples(testArr, testSamples);

        changed = false;
    }

    private String getField(JsonObject obj, String fieldName)
    {
        JsonElement elem = obj.get(fieldName);
        if(elem != null)
            return elem.getAsString();
        return null;
    }

    private void setField(JsonObject obj, String fieldName, String value)
    {
        if(value != null)
            obj.add(fieldName, new JsonPrimitive(value));
    }

    public JsonObject toJson()
    {
        JsonArray trainArr = new JsonArray();
        JsonArray testArr = new JsonArray();

        for(Task.Sample sample : trainSamples)
            trainArr.add(sample.sampleToJson());

        for(Task.Sample sample : testSamples)
            testArr.add(sample.sampleToJson());

        JsonObject taskJson = new JsonObject();
        taskJson.add("train", trainArr);
        taskJson.add("test", testArr);

        setField(taskJson, "description", description);
        setField(taskJson, "abstractions", abstractions);
        setField(taskJson, "cues", cues);
        setField(taskJson, "input_code", inputCode);
        setField(taskJson, "output_code", outputCode);

        return taskJson;
    }

    private void loadSamples(JsonArray arr, List<Sample> dest)
    {
        for(JsonElement elem : arr)
        {
            JsonObject obj = elem.getAsJsonObject();
            Sample sample = new Sample(this, obj);
            dest.add(sample);
        }
    }

    protected static String getNotesFrom(JsonElement notes)
    {
        if(notes == null)
            return "";
        return notes.getAsString();
    }

    public static JsonArray gridToJson(ColorGrid grid)
    {
        JsonArray rowsArr = new JsonArray();

        for(int j = 0; j < grid.getHeight(); ++j)
        {
            JsonArray rowArr = new JsonArray();
            for(int i = 0; i < grid.getWidth(); ++i)
                rowArr.add(grid.get(i, grid.getHeight() - 1 - j).ordinal());
//                rowArr.add(grid.get(i, j).ordinal());

            rowsArr.add(rowArr);
        }

        return rowsArr;
    }


    protected static ColorGrid buildGridFromArr(JsonArray arr)
    {
        List<List<Integer>> values = new ArrayList<>();

        for(JsonElement elem : arr)
        {
            JsonArray row = elem.getAsJsonArray();
            List<Integer> ints = new ArrayList<>();
            for(JsonElement value : row)
            {
                ints.add(value.getAsInt());
            }
            values.add(ints);
        }

        Collections.reverse(values);

        return new ColorGrid(values);
    }

    public boolean evaluate(List<ColorGrid> candidates)
    {
        for(Sample sample : testSamples)
        {
            for(ColorGrid grid : candidates)
            {
                if(grid.equals(sample.output))
                    return true;
            }
        }

        return false;
    }

}
