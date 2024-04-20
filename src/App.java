import java.io.*;
import java.util.*;
import javafx.animation.*;
import javafx.application.Application;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.*;
import javafx.geometry.*;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.layout.*;
import javafx.scene.paint.*;
import javafx.scene.shape.Rectangle;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.*;
import javafx.util.Duration;
// Turns out this app needs a lot of imports.
// Most of them are just from various places in JavaFX, though unfortunately I can't really collapse them at all.

// Apologies for the large download, I wasn't sure how to ensure that you would have JavaFX installed to the right place
// without just including it myself.
// To run the project, use the buildandrun.ps1 script. It will make sure everything that needs to be included is included.
// Just make sure to run it from the project directory, otherwise the relative paths I used to include JavaFX won't work right.
// In VSCode, Ctrl-Shift-B should also work due to the tasks.json file I've created.

// Also included in this repo is the updated Word doc, because there have been a couple of changes to it
// (and the flowchart, to which there is a link in the doc) that are worth looking at.

public class App extends Application {
    // All global variables for the program are stored here above the functions, as
    // statics.
    static int rows = 50;
    static int columns = 75;

    static boolean doWraparound = true;

    // This needs to be a property so that its display can see when it changes.
    static SimpleIntegerProperty generationIndex = new SimpleIntegerProperty(0);

    // cellSize is 0 because it's recomputed whenever initializeGrid is called.
    static double cellSize = 0;
    static double maxWidth = 512;
    static double maxHeight = 256;

    static int screenWidth = 640;
    static int screenHeight = 480;

    // The innermost lists must also be observable, for the same reason as the
    // generationIndex.
    // The display tiles need to be able to know when a value is updated (such as
    // from tick()).
    static ArrayList<ObservableList<Boolean>> gridData = new ArrayList<>();

    // The only reason this variable is static is so initializeGrid can always
    // access it.
    static GridPane grid = new GridPane();

    // Colour scheme for everything that has a colour (grid cells, backgrounds).
    static Paint deadColour = Color.rgb(20, 20, 20);
    static Paint aliveColour = Color.rgb(255, 255, 255);
    static Paint borderColour = Color.rgb(110, 110, 110);

    static Paint gridColour = Color.rgb(0, 0, 0);
    static Paint backgroundColour = Color.rgb(180, 180, 180);

    // Button images from Icons8 (icons8.com).
    static String stepImageName = "https://img.icons8.com/?size=256&id=120436&format=png";
    static String playImageName = "https://img.icons8.com/?size=256&id=59862&format=png";
    static String pauseImageName = "https://img.icons8.com/?size=256&id=9987&format=png";
    static String saveImageName = "https://img.icons8.com/?size=256&id=82831&format=png";
    static String loadImageName = "https://img.icons8.com/?size=256&id=82888&format=png";
    static String editImageName = "https://img.icons8.com/?size=256&id=86372&format=png";

    public static void main(String[] args) {
        // Open the window and run the program.
        launch(args);
    }

    public static void tick() {
        // Go through the gridData and make an element-wise copy of it, in
        // regular ArrayList format. This is for keeping track of what was
        // where from last frame.
        ArrayList<ArrayList<Boolean>> data = new ArrayList<>();
        for (ObservableList<Boolean> column : gridData) {
            ArrayList<Boolean> newColumn = new ArrayList<>();
            data.add(newColumn);
            for (boolean value : column) {
                newColumn.add(value);
            }
        }

        // Loop over every cell in the grid, keeping track of its row/column index.
        for (int xPos = 0; xPos < gridData.size(); xPos++) {
            ObservableList<Boolean> arr = gridData.get(xPos);
            for (int yPos = 0; yPos < arr.size(); yPos++) {
                // Store whether or not the cell is alive.
                // We can use the currently-updating array for this,
                // since we haven't changed this cell yet.
                boolean cellValue = arr.get(yPos);

                // Loop over a 3x3 grid centered on the current cell,
                // counting the number of living cells encountered.

                // The neighbourhood looks like this: .....
                // The O is the current cell, Ns are .NNN.
                // its neighbours (though the O is .NON.
                // also counted), and . is irrelevant .NNN.
                // as it's too far away to be counted .....
                int neighboursAlive = 0;
                for (int dx = -1; dx <= 1; dx++) {
                    for (int dy = -1; dy <= 1; dy++) {
                        try {
                            // Need to pull from the copied data,
                            // so that previous alterations don't disturb
                            // others in the same tick
                            if (data.get(xPos + dx).get(yPos + dy)) {
                                neighboursAlive++;
                            }
                        } catch (IndexOutOfBoundsException _) {
                            // This will error when data.get tries to check outside the range of the grid.
                            // Reindex where the neighbour would be, using modulo operator.
                            // Only do this if wraparound is enabled.
                            if ( doWraparound
                                    && data.get((xPos + dx + columns) % columns).get((yPos + dy + rows) % rows)) {
                                neighboursAlive++;
                            }
                        }
                    }
                }

                // If either 3 neighbours alive or (I'm alive and so are 3 other tiles around
                // me).
                // This accounts for 3 cases: dead + 3 neighbours alive, alive + 2 neighbours
                // alive (in which case neighboursAlive counts 3 within the square), and
                // alive + 3 neighbours alive (in which case neighboursAlive sees 4).
                arr.set(yPos, neighboursAlive == 3 || (neighboursAlive == 4 && cellValue));
            }
        }

        // This is the SimpleIntegerProperty version of generationIndex++
        generationIndex.set(generationIndex.get() + 1);
    }

    // .cgol file spec (for saving and loading):
    // "1 int" refers to the amount of space an int takes up in binary.
    // 1 int for rows
    // 1 int for columns
    // 1 int for whether wraparound is enabled (1 if yes, 0 if no)
    // 1 int for the current generation index
    // A series of ints for the cells of the grid (1 if enabled, 0 if not)
    // arranged right-to-left, top-to-bottom.

    public static void save(File targetFile) {
        // If no file has been selected, don't try to save to it.
        if (targetFile == null)
            return;

        // Open a BufferedOutputStream targeting the file.
        try (FileOutputStream fos = new FileOutputStream(targetFile);
                BufferedOutputStream bos = new BufferedOutputStream(fos);) {

            // Write all of the specified data to the file.
            bos.write(rows);
            bos.write(columns);
            bos.write(doWraparound ? 0b1 : 0b0);
            bos.write(generationIndex.get());

            // Loop over the cells, writing their values to the file in sequence.
            for (ObservableList<Boolean> column : gridData) {
                for (boolean value : column) {
                    bos.write(value ? 0b1 : 0b0);
                }
            }
        } catch (IOException e) {
            // In theory this should never happen but you never know.
            e.printStackTrace();
        }
    }

    public static void load(File targetFile) {
        // If no file has been selected, don't try to load from nowhere.
        if (targetFile == null)
            return;

        // Open a BufferedInputStream pointed at the file.
        try (FileInputStream fos = new FileInputStream(targetFile);
                BufferedInputStream bos = new BufferedInputStream(fos);) {

            // Read all of the specified data from the file.
            rows = bos.read();
            columns = bos.read();
            doWraparound = bos.read() == 0b1;
            initializeGrid();
            // Have to set generationIndex after initializing the grid, as
            // that function sets it to 0.
            generationIndex.set(bos.read());

            // Loop over the grid's cells, reading data from the file to
            // populate them. They should always be in the right order,
            // by the specification.
            for (int xPos = 0; xPos < gridData.size(); xPos++) {
                ObservableList<Boolean> column = gridData.get(xPos);
                for (int yPos = 0; yPos < column.size(); yPos++) {
                    column.set(yPos, bos.read() == 0b1);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void initializeGrid() {
        // Set some static data (cell pixel size, generation index)
        cellSize = Math.min(maxHeight / rows, maxWidth / columns);
        generationIndex.set(0);

        // Clear out the grid and its data, in case it's being reinitialized and already
        // has values in it.
        gridData.clear();
        grid.getChildren().clear();
        grid.getColumnConstraints().clear();
        grid.getRowConstraints().clear();

        // Set the size of the grid. I use prefSize and maxSize, which seems to give the
        // most consistent results.
        grid.setPrefSize(columns * cellSize, rows * cellSize);
        grid.setMaxSize(columns * cellSize, rows * cellSize);

        grid.setBackground(Background.fill(gridColour));

        // Assign widths and heights to the cells in the grid. They'll be applied later.
        ColumnConstraints columnConstraints = new ColumnConstraints();
        columnConstraints.setPercentWidth(100.0 / columns);
        RowConstraints rowConstraints = new RowConstraints();
        rowConstraints.setPercentHeight(100.0 / rows);

        for (int xPos = 0; xPos < columns; xPos++) {
            // dataRow holds the cells' states, and nodeRow holds their screen components.
            final ObservableList<Boolean> dataRow = FXCollections.<Boolean>observableArrayList();
            final ArrayList<Rectangle> nodeRow = new ArrayList<>();
            gridData.add(dataRow);
            for (int yPos = 0; yPos < rows; yPos++) {
                // Create a new rectangle to represent the cell at this x/y position.
                Rectangle cell = new Rectangle(cellSize, cellSize, deadColour);
                cell.setStroke(aliveColour);
                cell.setStrokeWidth(0.25);
                // These values need to be stored as final so onMouseClicked can access them.
                final int finalxPos = xPos;
                final int finalyPos = yPos;
                cell.setOnMouseClicked(event -> {
                    ObservableList<Boolean> dataColumn = gridData.get(finalxPos);
                    dataColumn.set(finalyPos, !dataColumn.get(finalyPos));
                });
                grid.add(cell, xPos, yPos);

                dataRow.add(false);
                nodeRow.add(cell);
            }

            // Update the node row whenever the data row changes.
            dataRow.addListener((ListChangeListener<Boolean>) (c) -> {
                while (c.next()) {
                    assert c.wasUpdated();
                    for (int i = c.getFrom(); i < c.getTo(); ++i) {
                        nodeRow.get(i).setFill(dataRow.get(i) ? aliveColour : deadColour);
                    }
                }
            });

            // This is where the column constraints get applied.
            grid.getColumnConstraints().add(columnConstraints);
        }

        // A separate loop for row constraints, to make sure the correct number are
        // added.
        for (int yPos = 0; yPos < rows; yPos++) {
            grid.getRowConstraints().add(rowConstraints);
        }
    }

    public void showPropertiesModal(Stage parentStage) {
        // Create a new stage, which will be displayed over the top of the base one.
        Stage modalStage = new Stage();
        modalStage.initModality(Modality.APPLICATION_MODAL);
        modalStage.initOwner(parentStage);
        modalStage.setTitle("Edit Properties");

        // Make a label and a slider for the width of the grid, and tie the two together
        // with a listener.
        Label widthText = new Label("Width: ");
        Label widthValue = new Label("##");
        HBox widthLabel = new HBox(widthText, widthValue);
        Slider width = new Slider(10, 100, 10);
        width.setShowTickLabels(true);
        width.setShowTickMarks(true);
        width.setMajorTickUnit(10);
        width.setMinorTickCount(1);
        width.valueProperty().addListener((ChangeListener<Number>) ((_, _, endValue) -> {
            widthValue.setText(String.valueOf(endValue.intValue()));
        }));
        width.setValue(columns);

        // Same as before, but for the grid's height this time.
        Label heightText = new Label("Height: ");
        Label heightValue = new Label("##");
        HBox heightLabel = new HBox(heightText, heightValue);
        Slider height = new Slider(10, 100, 10);
        height.setShowTickLabels(true);
        height.setShowTickMarks(true);
        height.setMajorTickUnit(10);
        height.setMinorTickCount(1);
        height.valueProperty().addListener((ChangeListener<Number>) ((_, _, endValue) -> {
            heightValue.setText(String.valueOf(endValue.intValue()));
        }));
        height.setValue(rows);

        // Make a checkbox for whether the grid should allow cells to wrap around its
        // edges.
        CheckBox wraparoundCheckBox = new CheckBox("Wraparound?");
        wraparoundCheckBox.setSelected(doWraparound);

        CheckBox randomizeCheckbox = new CheckBox("Randomize cells?");

        // Make a button to close the modal, and give it a listener to do so when it's
        // clicked. At that time, also reinitialize the grid with the new settings.
        Button closeButton = new Button("Done (Overwrite)");
        closeButton.setOnAction(ev -> {
            // These need to be converted to ints, as their standard value is a double. This
            // is always file to do, though, since they're restricted to whole values
            // anyway.
            columns = (int) width.getValue();
            rows = (int) height.getValue();
            doWraparound = wraparoundCheckBox.isSelected();
            initializeGrid();
            if (randomizeCheckbox.isSelected()) {
                Random rand = new Random();
                for (ObservableList<Boolean> row : gridData) {
                    for (int i = 0; i < row.size(); i++) {
                        // The 0.25 represents the percentage of cells that are alive
                        row.set(i, rand.nextDouble() < 0.25);
                    }
                }
            }
            modalStage.close();
        });

        // Make a button to cancel the proposed changes, in case the user doesn't want
        // to overwrite their creations.
        Button cancelButton = new Button("Cancel");
        cancelButton.setOnAction(ev -> {
            modalStage.close();
        });

        HBox buttonPanel = new HBox(closeButton, cancelButton);
        buttonPanel.setAlignment(Pos.BOTTOM_CENTER);

        VBox modalContents = new VBox(widthLabel, width, heightLabel, height, wraparoundCheckBox, randomizeCheckbox,
                buttonPanel);
        modalContents.setAlignment(Pos.BOTTOM_CENTER);
        modalContents.setPadding(new Insets(5));
        modalContents.setMinWidth(250);

        Scene modalScene = new Scene(modalContents, backgroundColour);
        modalStage.setScene(modalScene);
        modalStage.showAndWait(); // ShowAndWait ensures this stage blocks interaction until closed
    }

    @Override
    public void start(Stage stage) {
        // Runs the tick function every second (or less, if its rate is increased above
        // 1)
        Timeline tickClock = new Timeline(new KeyFrame(Duration.millis(1000), ev -> tick()));
        tickClock.setCycleCount(Animation.INDEFINITE);
        tickClock.setRate(0);

        // Create buttons for playing, pausing, and stepping through individual
        // generations.
        Paint playImage = new ImagePattern(new Image(playImageName));
        Rectangle play = new Rectangle(32, 32, playImage);
        play.setOnMouseClicked(ev -> tickClock.play());
        Paint pauseImage = new ImagePattern(new Image(pauseImageName));
        Rectangle pause = new Rectangle(32, 32, pauseImage);
        pause.setOnMouseClicked(ev -> tickClock.stop());
        Paint stepImage = new ImagePattern(new Image(stepImageName));
        Rectangle step = new Rectangle(32, 32, stepImage);
        step.setOnMouseClicked(ev -> tick());

        // Create a slider for controlling automatic steps. It's initially 0.
        Label tps_title = new Label("Ticks per second: ");
        Label tps_count = new Label("0");
        Slider tps = new Slider(0, 50, 0);
        tps.setShowTickLabels(true);
        tps.setShowTickMarks(true);
        tps.setMajorTickUnit(10);
        tps.setMinorTickCount(1);

        // When the tps slider is changed, update the tickClock and the tps display.
        tps.valueProperty().addListener((ChangeListener<Number>) ((_, _, endValue) -> {
            tickClock.setRate(endValue.doubleValue());
            tps_count.setText(String.valueOf(endValue.intValue()));
        }));

        // Wrap the tps label and slider in a box.
        HBox tps_label = new HBox(tps_title, tps_count);
        VBox tps_box = new VBox(tps_label, tps);
        tps_label.setAlignment(Pos.TOP_CENTER);

        // Group the buttons and tps controls into a box as well.
        HBox controls = new HBox(10, play, pause, step, tps_box);
        HBox.setHgrow(controls, Priority.ALWAYS);

        // Create a generation index display, to go on the other side of the screen.
        Label generationTitle = new Label("Generation: ");
        Label generationDisplay = new Label("0");
        // This ensures that the generation index always matches the simulator's value
        // for it.
        generationIndex.addListener((ChangeListener<Number>) ((_, _, endValue) -> {
            generationDisplay.setText(String.valueOf(endValue.intValue()));
        }));
        HBox generationInfo = new HBox(generationTitle, generationDisplay);
        generationInfo.setAlignment(Pos.TOP_RIGHT);
        HBox.setHgrow(generationInfo, Priority.ALWAYS);

        // Make a top bar for the simulation controls and generation info.
        HBox topBar = new HBox(10, controls, generationInfo);

        // Run the necessary code to set up the grid.
        initializeGrid();

        // Set up the extension options for saving/loading files.
        FileChooser.ExtensionFilter ext = new ExtensionFilter("CGoL Files", "*.cgol");

        // Make a button to save the current grid.
        FileChooser saveChooser = new FileChooser();
        saveChooser.setTitle("Save File");
        saveChooser.getExtensionFilters().add(ext);
        Paint saveImage = new ImagePattern(new Image(saveImageName));
        Rectangle saveButton = new Rectangle(32, 32, saveImage);
        saveButton.setOnMouseClicked(ev -> {
            File saveLocation = saveChooser.showSaveDialog(stage);
            save(saveLocation);
        });

        // Make a button to load a grid from a file.
        FileChooser loadChooser = new FileChooser();
        loadChooser.setTitle("Load File");
        loadChooser.getExtensionFilters().add(ext);
        Paint loadImage = new ImagePattern(new Image(loadImageName));
        Rectangle loadButton = new Rectangle(32, 32, loadImage);
        loadButton.setOnMouseClicked(ev -> {
            File loadLocation = loadChooser.showOpenDialog(stage);
            load(loadLocation);
        });

        // Make a button to edit the properties (size, wraparound) of the grid.
        Paint editImage = new ImagePattern(new Image(editImageName));
        Rectangle editButton = new Rectangle(32, 32, editImage);
        editButton.setOnMouseClicked(ev -> {
            tickClock.stop();
            showPropertiesModal(stage);
            tickClock.play();
        });

        // Make a bottom bar to hold all of the bottom buttons.
        HBox bottomBar = new HBox(saveButton, loadButton, editButton);
        bottomBar.setAlignment(Pos.BOTTOM_LEFT);
        VBox.setVgrow(bottomBar, Priority.ALWAYS);

        // Make a container that can hold the whole screen's components.
        VBox globalContainer = new VBox(10, topBar, grid, bottomBar);
        globalContainer.setAlignment(Pos.TOP_CENTER);
        globalContainer.setPadding(new Insets(10));

        // Initialize the scene and stage, set the tick clock running (at a rate of 0 to
        // begin with), and start the program.
        Scene scene = new Scene(globalContainer, screenWidth, screenHeight, backgroundColour);
        stage.setTitle("Game of Life Simulator");
        stage.setScene(scene);
        tickClock.play();
        stage.show();
    }
}