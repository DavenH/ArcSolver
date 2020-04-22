package viz;

import gen.dsl.Dsl;
import gen.grid.ColorGrid;
import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.util.converter.IntegerStringConverter;
import javafx.util.converter.NumberStringConverter;
import org.antlr.v4.runtime.ANTLRErrorListener;
import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.runtime.atn.ATNConfigSet;
import org.antlr.v4.runtime.dfa.DFA;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.LineNumberFactory;
import org.fxmisc.richtext.model.StyleSpans;
import org.fxmisc.richtext.model.StyleSpansBuilder;
import org.fxmisc.wellbehaved.event.EventPattern;
import org.fxmisc.wellbehaved.event.InputMap;
import org.fxmisc.wellbehaved.event.Nodes;
import org.reactfx.Subscription;
import problem.Controller;
import problem.Task;

import java.io.File;
import java.net.URL;
import java.time.Duration;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Previewer extends Application implements ANTLRErrorListener
{
    int currentIndex;
    boolean isTraining;
    Controller controller = new Controller();
    Random rand = new Random();

    TextArea descriptionArea;
    TextArea abstractionsArea;
    TextArea cuesArea;

    Label taskCodeLabel = new Label();
    Label inputCodeErrors = new Label();
    Label outputCodeErrors = new Label();

    TextField numberField = new TextField();
    ComboBox comboBox = new ComboBox(FXCollections.observableArrayList("Train", "Test"));
    IntegerStringConverter parser = new IntegerStringConverter();

    CodeArea inputCodeArea = new CodeArea();
    CodeArea testOutputCodeArea = new CodeArea();

    private boolean showingSolution;
    Board genOutputBoard = new Board();
    Board testInputBoard = new Board();

    Dsl testInputInterpreter;
    Dsl testOutputInterpreter;

    private HBox taskPairs;
    private HBox testBoards;

    public Previewer()
    {
        currentIndex = 0;
        isTraining = true;
        showingSolution = false;
        taskCodeLabel.setTextFill(Color.WHITE);
        comboBox.setEditable(false);
        comboBox.getSelectionModel().select(0);

        numberField.setTextFormatter(new TextFormatter<>(new NumberStringConverter()));
        numberField.setText("0");

        testInputInterpreter = new Dsl("[in]", this);
        testOutputInterpreter = new Dsl("[out]", this);
        testOutputInterpreter.addNamespace("input", testInputInterpreter.getVisitor());

        initCodeArea(inputCodeArea, testInputInterpreter, testInputBoard, inputCodeErrors, (t, s) -> t.setInputCode(s));
        initCodeArea(testOutputCodeArea, testOutputInterpreter, genOutputBoard, outputCodeErrors, (t, s) -> t.setOutputCode(s));

        Subscription compile = inputCodeArea.multiPlainChanges()
                                       .successionEnds(Duration.ofMillis(200))
                                       .subscribe(ignore -> {
                                           compile(testOutputCodeArea, genOutputBoard,
                                                   testOutputInterpreter, outputCodeErrors);
                                       });

        int h = 200;
        int w = 400;
        cuesArea = initTextArea(w+100, h, (task, text) -> task.setCues(text));
        descriptionArea = initTextArea(w, h, (task, text) -> task.setDescription(text));
        abstractionsArea = initTextArea(w-100, h, (task, text) -> task.setAbstractions(text));
    }

    private TextArea initTextArea(int width, int height,
                                  BiConsumer<Task, String> whatToDo)
    {
        TextArea area = new TextArea();
        area.setMaxWidth(width);
        area.setMinHeight(height);
        area.setMaxHeight(height);
        area.getStyleClass().add("notes");
        area.setWrapText(true);

        area.setOnKeyReleased(e -> {
            Task task = getCurrentTask();
            whatToDo.accept(task, area.getText());
        });

        return area;
    }

    private void initCodeArea(CodeArea codeArea,
                              Dsl interpreter,
                              Board board,
                              Label errorLabel,
                              BiConsumer<Task, String> taskFunc
                             )
    {
        codeArea.setParagraphGraphicFactory(LineNumberFactory.get(codeArea));
        codeArea.getStyleClass().add("codeArea");

        Subscription cleanUp = codeArea.multiPlainChanges()
                                       .successionEnds(Duration.ofMillis(200))
                                       .subscribe(ignore -> codeArea.setStyleSpans(0, computeHighlighting(
                                               codeArea.getText())));

        codeArea.setMinHeight(250);
        codeArea.setMinWidth(400);
        board.setMinWidth(300);
        board.setMinHeight(300);
        codeArea.setWrapText(true);

        errorLabel.setTextFill(Color.RED);

        InputMap<KeyEvent> inputMap = InputMap.consume(
                EventPattern.keyPressed(KeyCode.TAB),
                e -> codeArea.replaceSelection("    "));

        Nodes.addInputMap(codeArea, inputMap);

        Subscription compile = codeArea.multiPlainChanges()
                 .successionEnds(Duration.ofMillis(100))
                 .subscribe(ignore -> compile(codeArea, board, interpreter, errorLabel));

        Subscription saveToTask = codeArea.multiPlainChanges()
                .successionEnds(Duration.ofMillis(100))
                .subscribe(ignore -> {
                    Task task = getCurrentTask();
                    taskFunc.accept(task, codeArea.getText());
                });
    }

    private void compile(CodeArea codeArea, Board board, Dsl interpreter, Label errorLabel)
    {
//        System.out.println("Compiling " + codeArea.getText());
        errorLabel.setText("");
        Task task = getCurrentTask();
        ColorGrid input = task.getTestSamples().get(0).input;
        ColorGrid outputGrid = interpreter.renderGrid(input.copy(), codeArea.getText());

        if(outputGrid == null)
            outputGrid = new ColorGrid(null, ":", 1);

        board.updateGrid(outputGrid, (int) board.getWidth() - 10, (int) board.getHeight() - 10);
    }

    private void moveIndex(int delta)
    {
        currentIndex = currentIndex + delta;
        if(currentIndex < 0)
            currentIndex = 0;

        currentIndex = currentIndex % getCurrentTasks().size();
        numberField.setText(String.valueOf(currentIndex));
    }

    private Label makeLabel(String text)
    {
        Label l = new Label(text);
        l.setTextFill(Color.WHITE);
        l.setWrapText(true);
        return l;
    }

    List<Task> getCurrentTasks()
    {
        return controller.getTasks(isTraining);
    }

    Task getCurrentTask()
    {
        List<Task> tasks = getCurrentTasks();

        if(currentIndex < 0 || currentIndex >= tasks.size())
        {
            throw new RuntimeException("Bad task index: " + currentIndex + " tasks size: " + tasks.size());
        }

        Task task = tasks.get(currentIndex);
        return task;
    }

    @Override
    public void start(Stage primaryStage)
    {
        primaryStage.setTitle("ARC Explorer");
        controller.loadTasks(new File("C:\\Users\\Public\\data\\ARC-master\\data\\"));

        Button next = new Button("Next");
        Button prev = new Button("Prev");
        Button random = new Button("Random");
        Button showButton = new Button("Show");

        numberField.setPrefWidth(40);

        // buttons
        HBox buttonPane = new HBox(10);
        buttonPane.getChildren().addAll(prev, numberField, next, random, comboBox, taskCodeLabel, showButton);
        buttonPane.setPadding(new Insets(0, 0, 10, 0));
        buttonPane.setAlignment(Pos.CENTER_LEFT);

        // task notes
        VBox descrStack = new VBox(5, makeLabel("Description"), descriptionArea);
        VBox abstrStack = new VBox(5, makeLabel("Abstractions"), abstractionsArea);
        VBox cueStack   = new VBox(5, makeLabel("Cues"), cuesArea);
        HBox taskNotes  = new HBox(10, descrStack, abstrStack, cueStack);

        // task pairs
        taskPairs = new HBox(10);
        taskPairs.setPadding(new Insets(20, 0, 0, 0));

        // right
        testBoards = new HBox(5);
        VBox inputCodeAndErrors = new VBox(5, inputCodeArea, inputCodeErrors);
        VBox outputCodeAndErrors = new VBox(5, testOutputCodeArea, outputCodeErrors);
        HBox testInputPair = new HBox(15, inputCodeAndErrors, testInputBoard);
        HBox testOutputPair = new HBox(15, outputCodeAndErrors, genOutputBoard);
        testBoards.setPadding(new Insets(65, 0, 0, 0));

        // left right
        VBox right = new VBox(10, testBoards, testInputPair, testOutputPair);
        BorderPane left = new BorderPane(taskPairs, buttonPane, null, taskNotes, null);
        left.setPadding(new Insets(10));

        HBox main = new HBox(20, left, right);
        main.setPadding(new Insets(10));
        main.setStyle("-fx-background-color: #333;");

        updateTask();

        Scene mainScene = new Scene(main, 1920, 1080);

        next.setOnAction(event -> { moveIndex(1); updateTask(); });
        prev.setOnAction(event -> { moveIndex(-1); updateTask(); });
        random.setOnAction(event -> { moveIndex(Math.abs(rand.nextInt())); updateTask(); });

        comboBox.setOnAction(e -> {
            isTraining = comboBox.getSelectionModel().getSelectedIndex() == 0;
            moveIndex(0);
            updateTask();
        });

        numberField.setOnKeyReleased(e -> {
            if( e.getCode() == KeyCode.ENTER) {
                Integer val = parser.fromString(numberField.getText());

                if(val != null) {
                    currentIndex = val;
                    moveIndex(0);
                    updateTask();
                }
            }
        });

        showButton.setOnMouseReleased(e -> {
            showingSolution = ! showingSolution;
            showButton.setText(showingSolution ? "Hide" : "Show");
            updateTask();
        });

        URL javaUrl = this.getClass().getResource("/java-keywords.css");
        mainScene.getStylesheets().add(javaUrl.toExternalForm());
        URL url = this.getClass().getResource("/dark_theme.css");
        boolean succeeded = mainScene.getStylesheets().add(url.toExternalForm());

        primaryStage.setScene(mainScene);
        primaryStage.show();
    }

    @Override
    public void stop()
    {
        System.out.println("Saving changes...");
        controller.saveTasks(new File("C:\\Users\\Public\\data\\ARC-master\\data\\"));
    }

    public void updateTask()
    {
        Task task = getCurrentTask();
        if(task == null)
            return;

        cuesArea.setText(task.getCues());
        descriptionArea.setText(task.getDescription());
        abstractionsArea.setText(task.getAbstractions());

        if(! inputCodeArea.getText().equalsIgnoreCase(task.getInputCode()))
        {
            inputCodeArea.clear();
            String inputCode = task.getInputCode();
            if(inputCode != null)
                inputCodeArea.appendText(inputCode);
        }

        if(! testOutputCodeArea.getText().equalsIgnoreCase(task.getOutputCode()))
        {
            testOutputCodeArea.clear();
            String outputCode = task.getOutputCode();
            if(outputCode != null)
                testOutputCodeArea.appendText(outputCode);
        }

        taskCodeLabel.setText(task.getTaskCode());

        List<Task.Sample> trainSamples = task.getTrainSamples();
        List<Task.Sample> testSamples = task.getTestSamples();

        int preferredWidth = (1600 - 500 - trainSamples.size() * 10 - 20) / (trainSamples.size());
        int preferredHeight = (960 - 300 - 70) / (2);

        taskPairs.getChildren().clear();

        for(Task.Sample sample : trainSamples)
        {
            VBox pair = new VBox(5);
            ColorGrid inGrid = sample.input;
            ColorGrid outGrid = sample.output;

            pair.getChildren().add(new Label(String.format("%d x %d → %d x %d", inGrid.getWidth(), inGrid.getHeight(), outGrid.getWidth(), outGrid.getHeight())));
            pair.getChildren().add(new Board(inGrid, preferredWidth, preferredHeight));
            pair.getChildren().add(new Board(outGrid, preferredWidth, preferredHeight));
            taskPairs.getChildren().add(pair);
        }

        testBoards.getChildren().clear();
        testBoards.setMinHeight(400);

        for(Task.Sample sample : testSamples)
        {
            VBox labelBoard = new VBox(5);
            String hiddenW = showingSolution ? String.valueOf(sample.output.getWidth()) : "?";
            String hiddenH = showingSolution ? String.valueOf(sample.output.getHeight()) : "?";
            labelBoard.getChildren().add(new Label(String.format("%d x %d → %s x %s", sample.input.getWidth(), sample.input.getHeight(), hiddenW, hiddenH)));
            labelBoard.getChildren().add(new Board(sample.input, 300, 300));
            testBoards.getChildren().add(labelBoard);

            if(showingSolution)
                testBoards.getChildren().add(new Board(sample.output, 300, 300));

        }

//        mainPane.autosize();
    }

    private static final String[] GLOBAL = new String[] { "brush", "input", "source", "board", "background" };
    private static final String[] DRAW   = new String[] { "draw", "dims", "fill" };

    private static final String[] PREP   = new String[] { "left", "right", "top", "bottom", "leftOf", "rightOf", "below", "above", "between", "around", "corners", "ends" };
    private static final String[] FUNC   = new String[] { "layout", "solve", "find", "groupBy", "infill", "shapes" };
    private static final String[] OBJ    = new String[] { "pos", "mask", "dot", "square", "rect", "line", "grid" };
    private static final String[] BOOL   = new String[] { "nand", "xor", "or", "and" };
    private static final String[] ADT    = new String[] { "list", "orderBy", "first", "last", "foreach" };
    private static final String[] AFFINE = new String[] { "rotate", "repeat", "translate" };

    private static final String GLOBAL_PATTERN = "\\b(" + String.join("|", GLOBAL) + ")\\b";
    private static final String DRAW_PATTERN = "\\b(" + String.join("|", DRAW) + ")\\b";
    private static final String PREP_PATTERN = "\\b(" + String.join("|", PREP) + ")\\b";
    private static final String FUNC_PATTERN = "\\b(" + String.join("|", FUNC) + ")\\b";
    private static final String OBJ_PATTERN = "\\b(" + String.join("|", OBJ) + ")\\b";
    private static final String ADT_PATTERN = "\\b(" + String.join("|", ADT) + ")\\b";
    private static final String BOOL_PATTERN = "\\b(" + String.join("|", BOOL) + ")\\b";
    private static final String AFFINE_PATTERN = "\\b(" + String.join("|", AFFINE) + ")\\b";
    private static final String PAREN_PATTERN = "\\(|\\)";
    private static final String NUM_PATTERN = "\\b\\d+\\b";
    private static final String BRACE_PATTERN = "\\{|\\}";
    private static final String BRACKET_PATTERN = "\\[|\\]";
    private static final String SEMICOLON_PATTERN = "\\;";
    private static final String STRING_PATTERN = "\"([^\"\\\\]|\\\\.)*\"";
    private static final String COMMENT_PATTERN = "//[^\n]*" + "|" + "/\\*(.|\\R)*?\\*/";
    private static final Pattern PATTERN = Pattern.compile(
            "(?<DRAW>" + DRAW_PATTERN + ")"
            + "|(?<GLOBAL>" + GLOBAL_PATTERN + ")"
            + "|(?<FUNC>" + FUNC_PATTERN + ")"
            + "|(?<OBJ>" + OBJ_PATTERN + ")"
            + "|(?<ADT>" + ADT_PATTERN + ")"
            + "|(?<BOOL>" + BOOL_PATTERN + ")"
            + "|(?<AFFINE>" + AFFINE_PATTERN + ")"
            + "|(?<PREP>" + PREP_PATTERN + ")"
            + "|(?<PAREN>" + PAREN_PATTERN + ")"
            + "|(?<NUM>" + NUM_PATTERN + ")"
            + "|(?<BRACE>" + BRACE_PATTERN + ")"
            + "|(?<BRACKET>" + BRACKET_PATTERN + ")"
            + "|(?<SEMICOLON>" + SEMICOLON_PATTERN + ")"
            + "|(?<STRING>" + STRING_PATTERN + ")"
            + "|(?<COMMENT>" + COMMENT_PATTERN + ")"
                                                          );

    private static StyleSpans<Collection<String>> computeHighlighting(String text) {
        Matcher matcher = PATTERN.matcher(text);
        int lastKwEnd = 0;
        StyleSpansBuilder<Collection<String>> spansBuilder = new StyleSpansBuilder<>();

        while(matcher.find()) {
            String styleClass =
                    matcher.group("DRAW") != null ? "draw" :
                    matcher.group("GLOBAL") != null ? "global" :
                    matcher.group("FUNC") != null ? "func" :
                    matcher.group("OBJ") != null ? "obj" :
                    matcher.group("ADT") != null ? "adt" :
                    matcher.group("BOOL") != null ? "bool" :
                    matcher.group("AFFINE") != null ? "affine" :
                    matcher.group("PREP") != null ? "prep" :
                    matcher.group("PAREN") != null ? "paren" :
                    matcher.group("NUM") != null ? "num" :
                    matcher.group("BRACE") != null ? "brace" :
                    matcher.group("BRACKET") != null ? "bracket" :
                    matcher.group("SEMICOLON") != null ? "semicolon" :
                    matcher.group("STRING") != null ? "string" :
                    matcher.group("COMMENT") != null ? "comment" :
                    null; /* never happens */ assert styleClass != null;
            spansBuilder.add(Collections.emptyList(), matcher.start() - lastKwEnd);
            spansBuilder.add(Collections.singleton(styleClass), matcher.end() - matcher.start());
            lastKwEnd = matcher.end();
        }
        spansBuilder.add(Collections.emptyList(), text.length() - lastKwEnd);
        return spansBuilder.create();
    }

    public static void main(String[] args)
    {
        launch(args);
    }

    @Override
    public void syntaxError(Recognizer<?, ?> recognizer,
                            Object offendingSymbol,
                            int line, int charPositionInLine,
                            String message,
                            RecognitionException e)
    {
        String error = String.format("[%d,%d]: ", line, charPositionInLine) + message;
        if(testInputInterpreter.ownsRecognizer(recognizer))
        {
            System.err.println("[in]: " + error);
            inputCodeErrors.setText(error);
        }
        else if(testOutputInterpreter.ownsRecognizer(recognizer))
        {
            System.err.println("[out]: " + error);
            outputCodeErrors.setText(error);
        }
    }

    @Override
    public void reportAmbiguity(Parser parser,
                                DFA dfa, int startIndex, int stopIndex, boolean exactlyKnown, BitSet ambigAlts,
                                ATNConfigSet atnConfigSet)
    {

    }

    @Override
    public void reportAttemptingFullContext(Parser parser, DFA dfa, int i, int i1, BitSet bitSet,
                                            ATNConfigSet atnConfigSet)
    {

    }

    @Override
    public void reportContextSensitivity(Parser parser, DFA dfa, int i, int i1, int i2, ATNConfigSet atnConfigSet)
    {

    }
}

