package viz;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.Scene;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.TilePane;
import javafx.stage.Stage;
import problem.Controller;

import java.io.File;
import java.net.URL;

public abstract class SimplePreviewer extends Application
{
    Controller controller = new Controller();

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

    public Stats stats(float[] values)
    {
        float m = 0;
        float min = Float.MAX_VALUE;
        float max = 0;

        for (int i = 0; i < values.length; ++i)
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
        controller.loadTasks(new File("/c/Users/Public/data/ARC-master/data/"));
    }

    public abstract void addBoardsToPane(TilePane pane);

    @Override
    public void start(Stage primaryStage)
    {
        primaryStage.setTitle("Board Previewer");
        TilePane tilePane = new TilePane(Orientation.HORIZONTAL);
        tilePane.setPadding(new Insets(20, 0, 0, 0));

        addBoardsToPane(tilePane);

        ScrollPane sp = new ScrollPane(tilePane);
        sp.setPrefViewportWidth(1900);
        sp.setFitToWidth(true);
        sp.setFitToHeight(true);

        final double SPEED = 0.05 / tilePane.getChildren().size();
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
}

