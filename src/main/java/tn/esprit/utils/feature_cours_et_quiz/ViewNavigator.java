package tn.esprit.utils.feature_cours_et_quiz;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.util.Objects;
import java.util.function.Consumer;

public final class ViewNavigator {
    private static Stage stage;
    private static Scene scene;

    private ViewNavigator() {
    }

    public static void init(Stage primaryStage, Scene primaryScene) {
        stage = Objects.requireNonNull(primaryStage, "stage");
        scene = Objects.requireNonNull(primaryScene, "scene");
    }

    public static LoadedView load(String fxmlResourcePath) throws IOException {
        URL resourceUrl = ViewNavigator.class.getResource(fxmlResourcePath);
        if (resourceUrl == null) {
            throw new IOException("FXML not found: " + fxmlResourcePath);
        }
        FXMLLoader loader = new FXMLLoader(resourceUrl);
        Parent root = loader.load();
        return new LoadedView(root, loader.getController());
    }

    public static void switchRoot(String fxmlResourcePath) throws IOException {
        switchRoot(fxmlResourcePath, null);
    }

    public static void switchRoot(String fxmlResourcePath, Consumer<Object> controllerInit) throws IOException {
        ensureInit();
        LoadedView view = load(fxmlResourcePath);
        if (controllerInit != null && view.controller() != null) {
            controllerInit.accept(view.controller());
        }
        scene.setRoot(view.root());
        stage.sizeToScene();
    }

    private static void ensureInit() {
        if (stage == null || scene == null) {
            throw new IllegalStateException("ViewNavigator not initialized. Call init(stage, scene) first.");
        }
    }

    public record LoadedView(Parent root, Object controller) {
    }
}
