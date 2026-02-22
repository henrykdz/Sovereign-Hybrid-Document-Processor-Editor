
package com.flowshift.editor;

import java.lang.reflect.Method;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyIntegerProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.Node;

/**
 * Helper für JavaFX 25 RichTextArea (Incubator API). Kapselt Reflection für bessere Type-Safety.
 */
public class RichTextAreaHelper {

	private final Object   richTextArea;
	private final Class<?> richTextAreaClass;

	public RichTextAreaHelper(Object richTextAreaInstance) throws Exception {
		this.richTextArea = richTextAreaInstance;
		this.richTextAreaClass = Class.forName("jfx.incubator.scene.control.richtext.RichTextArea");
	}

	// --- TEXT HANDLING ---
	public void setText(String text) {
		invoke("setText", text);
	}

	public String getText() {
		return (String) invoke("getText");
	}

	public StringProperty textProperty() {
		return (StringProperty) invoke("textProperty");
	}

	// --- STYLING (statt StyleSpans) ---
	public void setStyleClass(int start, int end, String styleClass) {
		invoke("setStyleClass", start, end, styleClass);
	}

	public void clearStyle(int start, int end) {
		invoke("clearStyle", start, end);
	}

	// --- EDITOR CONFIG ---
	public void setWrapText(boolean wrap) {
		invoke("setWrapText", wrap);
	}

	public void requestFocus() {
		((Node) richTextArea).requestFocus();
	}

	// --- CARET & SELECTION ---
	public int getCaretPosition() {
		return (int) invoke("getCaretPosition");
	}

	public ReadOnlyIntegerProperty caretPositionProperty() {
		return (ReadOnlyIntegerProperty) invoke("caretPositionProperty");
	}

	public String getSelectedText() {
		return (String) invoke("getSelectedText");
	}

	@SuppressWarnings("unchecked")
	public ObjectProperty<String> selectedTextProperty() {
		return (ObjectProperty<String>) invoke("selectedTextProperty");
	}

	// --- SCROLLING ---
	public void scrollTo(int position) {
		invoke("scrollTo", position);
	}

	public void scrollToPixel(double pixel) {
		invoke("scrollToPixel", pixel);
	}

	// --- HELPER ---
	private Object invoke(String methodName, Object... args) {
		try {
			Class<?>[] paramTypes = new Class<?>[args.length];
			for (int i = 0; i < args.length; i++) {
				// Sonderbehandlung für primitives
				if (args[i] instanceof Integer)
					paramTypes[i] = int.class;
				else if (args[i] instanceof Boolean)
					paramTypes[i] = boolean.class;
				else if (args[i] instanceof Double)
					paramTypes[i] = double.class;
				else
					paramTypes[i] = args[i].getClass();
			}

			Method method = richTextAreaClass.getMethod(methodName, paramTypes);
			return method.invoke(richTextArea, args);

		} catch (Exception e) {
			throw new RuntimeException("RichTextArea method failed: " + methodName, e);
		}
	}

	// Node access
	public Node asNode() {
		return (Node) richTextArea;
	}
}