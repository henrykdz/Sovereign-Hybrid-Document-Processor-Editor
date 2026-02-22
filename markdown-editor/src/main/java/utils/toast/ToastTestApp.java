package utils.toast;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;

/**
 * Umfassende Testklasse für Toast-Funktionalität und Fokus-Probleme
 */
public class ToastTestApp extends Application {

	private Toast            toast1, toast2, toast3;
	private ListView<String> listView;
	private TextField        textField;
	private TextArea         logArea;

	@Override
	public void start(Stage primaryStage) {
		System.out.println("Starting Toast Comprehensive Test App...");

		// --- Create Main UI ---
		VBox root = new VBox(10);
		root.setPadding(new Insets(20));

		// Title
		Label titleLabel = new Label("Toast Test - Fokus & Key Event Analysis");
		titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
		root.getChildren().add(titleLabel);

		// Interactive controls for focus testing
		Label controlsLabel = new Label("Interactive Controls (test arrow keys):");
		controlsLabel.setStyle("-fx-font-weight: bold; -fx-padding: 5 0 0 0;");
		root.getChildren().add(controlsLabel);

		HBox controlsBox = new HBox(10);
		listView = new ListView<>();
		listView.getItems().addAll("Item 1", "Item 2", "Item 3", "Item 4", "Item 5");
		listView.setPrefSize(150, 100);

		textField = new TextField("Type here...");
		textField.setPrefWidth(150);

		Button testButton = new Button("Test Button");

		controlsBox.getChildren().addAll(listView, textField, testButton);
		root.getChildren().add(controlsBox);

		// Add debug logging area
		logArea = new TextArea();
		logArea.setPrefHeight(150);
		logArea.setEditable(false);
		logArea.setPromptText("Debug logs will appear here...");
		root.getChildren().add(logArea);

		// Toast Test Buttons
		Label toastLabel = new Label("Toast Tests:");
		toastLabel.setStyle("-fx-font-weight: bold; -fx-padding: 10 0 0 0;");
		root.getChildren().add(toastLabel);

		// Button grid for tests
		GridPane buttonGrid = new GridPane();
		buttonGrid.setHgap(10);
		buttonGrid.setVgap(10);
		buttonGrid.setPadding(new Insets(10, 0, 0, 0));

		int row = 0;

		// Test 1: Simple Toast
		Button btnSimple = createTestButton("Show Simple Toast", e -> showSimpleToast());
		buttonGrid.add(btnSimple, 0, row);

		Button btnWithButtons = createTestButton("Toast with Buttons", e -> showToastWithButtons());
		buttonGrid.add(btnWithButtons, 1, row++);

		// Test 2: Multiple Toasts
		Button btnToast1 = createTestButton("Show Toast 1 (Bottom Right)", e -> showToast1());
		buttonGrid.add(btnToast1, 0, row);

		Button btnToast2 = createTestButton("Show Toast 2 (Bottom Left)", e -> showToast2());
		buttonGrid.add(btnToast2, 1, row++);

		Button btnToast3 = createTestButton("Show Toast 3 (Top Right)", e -> showToast3());
		buttonGrid.add(btnToast3, 0, row);

		Button btnTestIsShowing = createTestButton("Test isShowing()", e -> testIsShowing());
		buttonGrid.add(btnTestIsShowing, 1, row++);

		// Test 3: Focus Analysis
		Button btnFocusTest = createTestButton("Focus Debug Test", e -> focusDebugTest());
		buttonGrid.add(btnFocusTest, 0, row);

		Button btnKeyTest = createTestButton("Arrow Key Test", e -> arrowKeyTest());
		buttonGrid.add(btnKeyTest, 1, row++);

		// Test 4: Management
		Button btnCloseAll = createTestButton("Close All Toasts", e -> closeAllToasts());
		buttonGrid.add(btnCloseAll, 0, row);

		Button btnExit = createTestButton("Exit App", e -> Platform.exit());
		buttonGrid.add(btnExit, 1, row++);

		root.getChildren().add(buttonGrid);

		// Scene and Stage
		Scene scene = new Scene(root, 600, 700);
		primaryStage.setScene(scene);
		primaryStage.setTitle("Toast Focus & Key Event Test");
		primaryStage.show();

		// --- Setup Focus and Key Event Debugging ---
		setupDebugListeners(primaryStage);

		// Initial focus
		textField.requestFocus();

		log("App started. Focus initially on TextField.");
		log("Test arrow keys in ListView/TextField while toast is visible.");
	}

	private Button createTestButton(String text, javafx.event.EventHandler<javafx.event.ActionEvent> handler) {
		Button button = new Button(text);
		button.setMaxWidth(Double.MAX_VALUE);
		HBox.setHgrow(button, Priority.ALWAYS);
		button.setOnAction(handler);
		return button;
	}

	private void setupDebugListeners(Stage stage) {
		// Focus owner changes
		stage.getScene().focusOwnerProperty().addListener((obs, oldVal, newVal) -> {
			log("FOCUS OWNER: " + (oldVal != null ? oldVal.getClass().getSimpleName() : "null") + " -> " + (newVal != null ? newVal.getClass().getSimpleName() : "null"));
		});

		// Key events on root
		stage.getScene().addEventFilter(javafx.scene.input.KeyEvent.ANY, event -> {
			log("ROOT KEY: " + event.getEventType() + " " + event.getCode() + " consumed=" + event.isConsumed() + " target=" + event.getTarget());
		});

		// Stage focus
		stage.focusedProperty().addListener((obs, oldVal, newVal) -> {
			log("STAGE FOCUS: " + oldVal + " -> " + newVal);
		});

		// Spezifisch für ListView Debugging:
		listView.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
			System.out.println(
			        "LISTVIEW KEY_PRESSED: " + event.getCode() + " | Consumed: " + event.isConsumed() + " | Target: " + event.getTarget() + " | Focused: " + listView.isFocused());
		});

		listView.addEventFilter(KeyEvent.KEY_RELEASED, event -> {
			System.out.println("LISTVIEW KEY_RELEASED: " + event.getCode());
		});

		// Prüfe ob ListView wirklich Fokus hat
		listView.focusedProperty().addListener((obs, oldVal, newVal) -> {
			System.out.println("LISTVIEW FOCUS: " + oldVal + " -> " + newVal);
		});

		// Prüfe Selection Model
		listView.getSelectionModel().selectedIndexProperty().addListener((obs, oldVal, newVal) -> {
			System.out.println("LISTVIEW SELECTION: " + oldVal + " -> " + newVal);
		});

		// TextField key events
		textField.addEventFilter(javafx.scene.input.KeyEvent.KEY_PRESSED, event -> {
			log("TEXTFIELD KEY PRESS: " + event.getCode() + " consumed=" + event.isConsumed());
		});
	}

	private void log(String message) {
		System.out.println(message);
		Platform.runLater(() -> {
			logArea.appendText(message + "\n");
		});
	}

	// --- Test Methods ---

	private void showSimpleToast() {
		log("\n=== Showing Simple Toast ===");
		ToastNotifier.showInfo("Simple Toast", "This should not steal focus");
	}

	private void showToastWithButtons() {
		log("\n=== Showing Toast with Buttons ===");
		Button actionBtn = new Button("Action");
		actionBtn.setOnAction(e -> log("Toast button clicked!"));

		Toast.create().title("Toast with Buttons").text("This toast has interactive buttons").buttons(actionBtn).hideAfter(Duration.seconds(10)).show();

		log("Toast with buttons created. Test if arrow keys still work.");
	}

	private void showToast1() {
		log("\n=== Showing Toast 1 ===");
		toast1 = Toast.create().title("Toast 1").text("This is the first toast").position(Pos.BOTTOM_RIGHT).hideAfter(Duration.seconds(30)).show();
		log("Toast 1 created. isShowing: " + toast1.isShowing());
	}

	private void showToast2() {
		log("\n=== Showing Toast 2 ===");
		toast2 = Toast.create().title("Toast 2").text("This is the second toast").position(Pos.BOTTOM_LEFT).hideAfter(Duration.seconds(30)).show();
		log("Toast 2 created. isShowing: " + toast2.isShowing());
	}

	private void showToast3() {
		log("\n=== Showing Toast 3 ===");
		toast3 = Toast.create().title("Toast 3").text("This is the third toast").position(Pos.TOP_RIGHT).hideAfter(Duration.seconds(30)).show();
		log("Toast 3 created. isShowing: " + toast3.isShowing());
	}

	private void testIsShowing() {
		log("\n=== Testing isShowing() ===");
		log("Toast 1 isShowing: " + (toast1 != null ? toast1.isShowing() : "null"));
		log("Toast 2 isShowing: " + (toast2 != null ? toast2.isShowing() : "null"));
		log("Toast 3 isShowing: " + (toast3 != null ? toast3.isShowing() : "null"));
	}

	private void focusDebugTest() {
		log("\n=== Focus Debug Test ===");

		// Show a toast
//        Toast testToast = Toast.create()
		Toast.create().title("Focus Test").text("Testing focus behavior...").hideAfter(Duration.seconds(15)).show();

		// After 1 second, check focus
		new Thread(() -> {
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
			}
			Platform.runLater(() -> {
				Stage stage = (Stage) textField.getScene().getWindow();
				log("Stage focused: " + stage.isFocused());
				log("Scene focus owner: " + (stage.getScene().getFocusOwner() != null ? stage.getScene().getFocusOwner().getClass().getSimpleName() : "null"));
				log("ListView focused: " + listView.isFocused());
				log("TextField focused: " + textField.isFocused());

				// Try to force focus
				textField.requestFocus();
				log("Requested focus on TextField");
			});
		}).start();
	}

	private void arrowKeyTest() {
		log("\n=== Arrow Key Test ===");

		// Create list view for arrow key testing
		ListView<String> testList = new ListView<>();
		testList.getItems().addAll("Test 1", "Test 2", "Test 3", "Test 4");
		testList.setPrefSize(200, 100);

		Stage testStage = new Stage();
		VBox testRoot = new VBox(10, new Label("Use arrow keys to navigate"), testList);
		testRoot.setPadding(new Insets(20));
		testStage.setScene(new Scene(testRoot, 300, 200));
		testStage.setTitle("Arrow Key Test Window");
		testStage.show();

		testList.requestFocus();

		// Add key listener
		testList.addEventFilter(javafx.scene.input.KeyEvent.KEY_PRESSED, event -> {
			if (event.getCode() == KeyCode.UP || event.getCode() == KeyCode.DOWN) {
				log("ARROW KEY in ListView: " + event.getCode() + " consumed=" + event.isConsumed());
			}
		});

		Toast.create().title("Arrow Key Test").text("Try using arrow keys in the other window")
//	     .owner(testStage)    // <--- hier das Owner-Window setzen
		        .hideAfter(Duration.seconds(20)).show(); // oder show(this), wenn du den Parameter durchreichst
		// Show toast
//		Toast.create().title("Arrow Key Test").text("Try using arrow keys in the other window").owner(testStage).hideAfter(Duration.seconds(20)).show(this);

		log("Toast shown. Try arrow keys in the new window.");
	}

	private void closeAllToasts() {
		log("\n=== Closing All Toasts ===");
		toast1 = null;
		toast2 = null;
		toast3 = null;
		log("All toast references cleared.");
	}

	@Override
	public void stop() {
		log("Toast Test App stopped.");
	}

	public static void main(String[] args) {
		launch(args);
	}
}