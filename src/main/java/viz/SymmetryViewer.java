package viz;

import gen.grid.ColorGrid;
import gen.primitives.Pos;
import gen.priors.abstraction.Symmetry;
import gen.priors.abstraction.SymmetryType;
import gen.priors.pattern.Pattern;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.StrokeType;
import util.ArrayUtil;
import util.T2;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SymmetryViewer extends SimplePreviewer
{
    Pattern pattern = new Pattern();

    public SymmetryViewer()
    {
    }

    @Override
    public void addBoardsToPane(TilePane tilePane)
    {
//        int[] grids = new int[] { 202 };
//        int[] grids = new int[] { 73, 174, 202, 241, 286, 350, 399 };
//        int[] grids = { 8, 32, 58, 16, 60, 109, 304 }; //312
//        List<Task> tasks = controller.getTasks(grids,true);
//        List<Task> tasks = controller.getRandomTasks(10,true);


        int preferredWidth = 250;
        int preferredHeight = 250;

//        for (Task task : tasks)
//        {
//            List<Task.Sample> train = task.getTrainSamples();
//
//            for (Task.Sample sample : train)
//            {
//                ColorGrid[] boards = new ColorGrid[] { sample.input, sample.output };

        ColorGrid boards[] = new ColorGrid[]{
//                new ColorGrid(null, "0000002100000012000031000000130000510000001500004100000014000000", 8),
//                new ColorGrid(null, "0000321000032120003212300001230000103000010000001000000000000000", 8),
//                new ColorGrid(null, "1200000021000000001600000061000000001500000051000000001300000031", 8),
                new ColorGrid(null, "1111111111111111111111111111111111111111111111111111111111111111", 8),

//                new ColorGrid(null, "11100000" +
//                                    "11100000" +
//                                    "11100000" +
//                                    "00010000" +
//                                    "00001000" +
//                                    "00000333" +
//                                    "00000333" +
//                                    "00000333", 8)
        };


        Map<SymmetryType, Color> symColors = new HashMap<>();
        symColors.put(SymmetryType.Horz, Color.RED);
        symColors.put(SymmetryType.Vert, Color.GREEN);
        symColors.put(SymmetryType.Diag, Color.BLUE);
        symColors.put(SymmetryType.NegDiag, Color.YELLOW);

                for(ColorGrid grid : boards)
                {
                    HBox pair = new HBox(5);

                    //                    if(grid.getWidth() > 5)
//                        grid.removeFromOriginEdge(false, 3);
//
//                    if(grid.getHeight() > 5)
//                        grid.removeFromOriginEdge(true, 3);

                    Board guessBoard = new Board(grid, preferredWidth, preferredHeight);

//                    Iterable<T2<Pos, Integer>> symmetrySeedsI = pattern.findSymmetrySeeds(grid, 1);

                    int h = grid.getHeight();
                    int w = grid.getWidth();

//                    for(T2<Pos, Integer> seed : symmetrySeedsI)
//                    {
//                        Pos xy = seed.getA();
//                        Node node = guessBoard.getChildren().get((h - 1 - xy.y) * w + xy.x);
//                        Rectangle rect = (Rectangle) node;
//
//                        rect.setStroke(Color.RED);
//                        rect.setStrokeWidth(2);
//                    }

                    int[][] intGrid = grid.toInt();
                    T2<float[][], List<Symmetry>> data = pattern.getFoldingSymmetries(intGrid);

                    VBox labels = new VBox(2);
                    labels.getChildren().add(new Label(String.format("%d", 1))); // controller.getIndex(task.getTaskCode())))); //1)));

                    for(Symmetry sym : data.getB())
                        labels.getChildren().add(new Label(String.format("type=%s, E=%b, pos=%s, frac=%3.2f",
                                                                         sym.type, sym.isEven, sym.pos.toString(),
                                                                         2 * sym.cellsObeying / (float) (h * w))));

                    pair.getChildren().add(labels);

                    float[][] autosym = data.getA();
                    int[] diag      = pattern.getNegDiagSymCollapsed(intGrid);
                    int[] negdiag   = pattern.getDiagSymCollapsed(intGrid);

                    float[] diagf    = ArrayUtil.toFloat(diag);
                    float[] negdiagf = ArrayUtil.toFloat(negdiag);

//                    Board diagBoard = new Board(new float[][] { diagf }, preferredWidth, preferredHeight);
//                    Board negdiagBoard = new Board(new float[][] { negdiagf }, preferredWidth, preferredHeight);

                    T2<Pos, Float> best = ArrayUtil.maxIndexAndValue(autosym, 0, 0);

                    for(Symmetry sym : data.getB())
                    {
                        Pos pos = sym.pos;
                        Node node = guessBoard.getChildren().get((h - 1 - pos.y) * w + pos.x);
                        Rectangle rect = (Rectangle) node;

                        rect.setStroke(symColors.get(sym.type));
                        rect.setStrokeWidth(2);
                        rect.setStrokeType(StrokeType.INSIDE);
                    }

                    Board autoBoard = new Board(autosym, preferredWidth, preferredHeight);
//                    VBox diags = new VBox(3, diagBoard, negdiagBoard, autoBoard);
                    pair.getChildren().add(guessBoard);
                    pair.getChildren().add(autoBoard);
                    tilePane.getChildren().add(pair);
                }

            }
//        }
//    }
}
