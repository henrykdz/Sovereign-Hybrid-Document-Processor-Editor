package com.flowshift.editor.ui;

import java.net.URL;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.scene.web.WebView;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.Window;
import utils.logging.Log;

/**
 * SovereignPreviewSatellite v1.3
 * 
 * Manages an independent preview stage with dynamic, screen-aware geometry.
 * Features:
 * 1. Right-Edge Anchoring: Snaps to the right of the primary screen.
 * 2. Taskbar Evasion: Uses VisualBounds to avoid overlapping system bars.
 * 3. Session Persistence: Maintains user-defined sizes after the initial show.
 */
public class SovereignPreviewSatellite {

    private final Stage stage;
    private final StackPane root;
    private final Scene scene;
    private boolean isFirstShow = true;

    private static final String CSS_PATH = "/com/flowshift/editor/editor-style.css";
    
    // Geometry Constants
    private static final double DEFAULT_WIDTH = 860;
    private static final double SAFETY_MARGIN = 40.0; // Distance from screen edges/taskbars

    public SovereignPreviewSatellite(Window owner) {
        this.stage = new Stage();
        this.root = new StackPane();
        this.root.getStyleClass().add("nexus-dialog");
        
        // Setup initial scene container
        this.scene = new Scene(root, DEFAULT_WIDTH, 600);
        
        loadInternalStyles();

        this.stage.setScene(scene);
        this.stage.initOwner(owner);
        this.stage.setAlwaysOnTop(true);
        
        // Set constant width for the initial right-edge calculation
        this.stage.setWidth(DEFAULT_WIDTH);
    }

    private void loadInternalStyles() {
        try {
            URL cssUrl = getClass().getResource(CSS_PATH);
            if (cssUrl != null) {
                this.scene.getStylesheets().add(cssUrl.toExternalForm());
            }
        } catch (Exception e) {
            Log.error(e, "Satellite: Style initialization failed.");
        }
    }

    public void setContent(WebView webView) {
        if (!root.getChildren().contains(webView)) {
            root.getChildren().setAll(webView);
        }
    }

    public void removeContent(WebView webView) {
        root.getChildren().remove(webView);
    }

    /**
     * Reveals the satellite window. 
     * Triggers dynamic geometry calculation only during the very first show.
     * @param title The window title (usually including the filename).
     */
    public void show(String title) {
        stage.setTitle(title);
        
        if (isFirstShow) {
            initializeDynamicGeometry();
            isFirstShow = false;
        }
        
        stage.show();
        stage.toFront();
        
        Log.fine(String.format("Satellite: Window active at %.0fx%.0f", stage.getWidth(), stage.getHeight()));
    }

    /**
     * Calculates and applies the initial window placement and sizing.
     * Logic:
     * - Height: Fits the available visual area between top and bottom system bars.
     * - X-Position: Anchors the window to the right edge of the primary screen with a 10px padding.
     * - Y-Position: Aligns with the top safety margin of the visual bounds.
     */
    private void initializeDynamicGeometry() {
        // 1. Get visual real estate (excluding Taskbar/Dock)
        Rectangle2D visualBounds = Screen.getPrimary().getVisualBounds();
        
        // 2. Calculate vertical fill height
        double targetHeight = visualBounds.getHeight() - (SAFETY_MARGIN * 2);
        
        // 3. Anchor to the Right Edge
        double targetX = visualBounds.getMaxX() - DEFAULT_WIDTH - 10;
        
        // 4. Align with Top margin
        double targetY = visualBounds.getMinY() + SAFETY_MARGIN;

        stage.setHeight(targetHeight);
        stage.setX(targetX);
        stage.setY(targetY);
        
        Log.fine(String.format("Satellite: Right-edge anchor forged (H: %.0f, Y: %.0f)", targetHeight, targetY));
    }

    public void hide() {
        if (stage.isShowing()) {
            stage.hide();
        }
    }

    public boolean isShowing() {
        return stage.isShowing();
    }

    public Stage getStage() {
        return stage;
    }
}