package utils.toast;

import javafx.css.PseudoClass;

/**
 * Centralizes all CSS-related constants for the toast notification system.
 */
public final class ToastCssConstants {
    
    // ===== STYLESHEET PATHS =====
    public static final String NOTIFICATION_CSS = "/resources/utils/toast/notification.css";
    
    // ===== STYLE CLASSES (für styleClass-Attribut) =====
    public static final String DARK_STYLE = "dark";
    public static final String TEXT_STYLE = "text";
    public static final String BRACKET_STYLE = "bracket";
    public static final String HIGHLIGHT_STYLE = "highlight";
    public static final String TITLE_STYLE = "title";
    public static final String CLOSE_BUTTON_STYLE = "close-button";
    public static final String CLOSE_BUTTON_GRAPHIC = "graphic";
    public static final String SEPARATOR_LINE = "separator-line";
    public static final String ICON_CONTAINER = "icon-container";
    public static final String NOTIFICATION_BAR = "notification-bar";
    public static final String PROGRESS_BAR = "progress-bar";
    
    // ===== COUNTER STYLE CLASSES =====
    public static final String COUNTER_BOX = "toast-counter-box";
    public static final String COUNTER_TEXT = "toast-counter-text";
    
    // ===== ICON PATHS =====
    public static final String ICON_BASE_PATH = "/resources/utils/toast/";
    public static final String ICON_WARNING = "dialog-warning.png";
    public static final String ICON_INFORMATION = "dialog-information.png";
    public static final String ICON_ERROR = "dialog-error.png";
    public static final String ICON_SUCCESS = "dialog-confirm.png";
    
    // ===== PSEUDO CLASSES (für pseudoClassStateChanged) =====
    public static final String PSEUDO_COMPLETED = "completed";
    public static final String PSEUDO_PAUSED = "paused";  // Korrekt hier bei Pseudo-Classes
    
    // ===== HELPER METHODS =====
    /**
     * Returns a PseudoClass instance for the completed state.
     */
    public static PseudoClass completedPseudoClass() {
        return PseudoClass.getPseudoClass(PSEUDO_COMPLETED);
    }
    
    /**
     * Returns a PseudoClass instance for the paused state.
     */
    public static PseudoClass pausedPseudoClass() {
        return PseudoClass.getPseudoClass(PSEUDO_PAUSED);
    }
    
    private ToastCssConstants() {}
}