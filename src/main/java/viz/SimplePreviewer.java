package viz;

import gen.grid.ColorGrid;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.antlr.v4.runtime.misc.Pair;
import problem.Controller;
import problem.Task;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class SimplePreviewer extends Application
{
    Controller controller = new Controller();

    private List<Task.Sample> samples;
    private final List<Pair<Task.Sample, Double>> metrics;

    class Stats
    {
        double mean;
        double min;
        double max;

        public Stats(double mean, double min, double max)
        {
            this.mean = mean;
            this.min = min;
            this.max = max;
        }
    }

    public Stats stats(double[] values)
    {
        double m = 0;
        double min = Double.MAX_VALUE;
        double max = 0;

        for(int i = 0; i < values.length; ++i)
        {
            m += values[i];
            min = Math.min(min, values[i]);
            max = Math.max(max, values[i]);
        }
        m /= values.length;

        return new Stats(m, min, max);
    }

    public SimplePreviewer()
    {
        controller.loadTasks(new File("C:\\Users\\Public\\data\\ARC-master\\data\\"));
        List<Task> trainTasks = controller.getTasks(true);
        metrics = new ArrayList<>();

        for(Task task : trainTasks)
        {
            double maxes, mins, avgs, tmaxes, tmins, tavgs, meta;
            maxes = mins = avgs = tmaxes = tmins = tavgs = meta = 0;

            for(Task.Sample sample : task.getTrainSamples())
            {
                ColorGrid.EntropyData inputEntropy = sample.input.calculateShannonEntropy();
                ColorGrid.EntropyData outputEntropy = sample.output.calculateShannonEntropy();
                ColorGrid.EntropyData inputTrans = sample.input.calculateCellTransitions();
                ColorGrid.EntropyData outputTrans = sample.output.calculateCellTransitions();

                Stats inVert = stats(inputEntropy.vert);
                Stats outVert = stats(outputEntropy.vert);
                Stats inHorz = stats(inputEntropy.horz);
                Stats outHorz = stats(outputEntropy.horz);

                Stats inTransVert = stats(inputTrans.vert);
                Stats outTransVert = stats(outputTrans.vert);
                Stats inTransHorz = stats(inputTrans.horz);
                Stats outTransHorz = stats(outputTrans.horz);

                double iw = 1;
                double ow = 1;

                maxes    += iw * inVert.max + iw * inHorz.max + ow * outVert.max + ow * outHorz.max;
                mins     += iw * inVert.min + iw * inHorz.min + ow * outVert.min + ow * outHorz.min;
                avgs     += iw * inVert.mean + iw * inHorz.mean + ow * outVert.mean + ow * outHorz.mean;

                tmaxes   += iw * inTransVert.max + iw * inTransHorz.max + ow * outTransVert.max + ow * outTransHorz.max;
                tmins    += iw * inTransVert.min + iw * inTransHorz.min + ow * outTransVert.min + ow * outTransHorz.min;
                tavgs    += iw * inTransVert.mean + iw * inTransHorz.mean + ow * outTransVert.mean + ow * outTransHorz.mean;
                meta     += iw * (inputTrans.metaHorz + inputTrans.metaVert) + ow * (outputTrans.metaHorz + outputTrans.metaVert);
            }

            double inv = 1.0 / task.getTrainSamples().size();
            maxes *= inv;
            mins  *= inv;
            avgs  *= inv;
            tmaxes *= inv;
            tmins  *= inv;
            tavgs  *= inv;
            meta *= inv;

            double metric = mins; //tavgs - tmins;
            metrics.add(new Pair<>(task.getTrainSamples().get(0), metric));
        }

        metrics.sort(Comparator.comparing(a -> -a.b));

//        samples = new ArrayList<>();
//        for(int i = 0; i < 40; ++i)
//        {
//            Task.Sample sample = metrics.get(i).a;
//            samples.add(sample);
//        }

//        for(int i = metrics.size() - 1; i > metrics.size() - 6; --i)
//        {
//            Task.Sample sample = metrics.get(i).a;
//            samples.add(sample);
//        }
    }

    @Override
    public void start(Stage primaryStage)
    {
        primaryStage.setTitle("Board Previewer");

        // task pairs
//        HBox taskPairs = new HBox(10);
//        taskPairs.setPadding(new Insets(20, 0, 0, 0));

        TilePane tilePane = new TilePane(Orientation.HORIZONTAL);
        tilePane.setPadding(new Insets(20, 0, 0, 0));

        int preferredWidth = 200; //(1900 - samples.size() * 10 - 20) / (samples.size());
        int preferredHeight = 200; //(960 - 70) / (2);

        for(Pair<Task.Sample, Double> p : metrics)
        {
            VBox pair = new VBox(5);
            ColorGrid inGrid = p.a.input;
            ColorGrid outGrid = p.a.output;

            pair.getChildren().add(new Label(String.format("%d, %3.3f, %d x %d → %d x %d", controller.getIndex(p.a.task.getTaskCode()), p.b,
                                                           inGrid.getWidth(), inGrid.getHeight(), outGrid.getWidth(), outGrid.getHeight())));
            pair.getChildren().add(new Board(inGrid, preferredWidth, preferredHeight));
            pair.getChildren().add(new Board(outGrid, preferredWidth, preferredHeight));
            tilePane.getChildren().add(pair);
        }

        ScrollPane sp = new ScrollPane(tilePane);
        sp.setPrefViewportWidth(1900);
        sp.setFitToWidth(true);
        sp.setFitToHeight(true);

        final double SPEED = 0.0005;
        sp.getContent().setOnScroll(scrollEvent -> {
            double deltaY = scrollEvent.getDeltaY() * SPEED;
            sp.setVvalue(sp.getVvalue() - deltaY);
        });

        Scene mainScene = new Scene(sp, 1920, 1080);
        URL url = this.getClass().getResource("/dark_theme.css");
        boolean succeeded = mainScene.getStylesheets().add(url.toExternalForm());

        primaryStage.setScene(mainScene);
        primaryStage.show();
    }

    @Override
    public void stop()
    {
    }

    public static void main(String[] args)
    {
        launch(args);
    }
}

