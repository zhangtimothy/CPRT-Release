package javachessgui;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;
import javafx.scene.image.Image;

import java.io.File;

public class Javachessgui extends Application {

    public Gui gui;

    public static TextArea message_text = new TextArea();

    final static String message_text_style =
            "-fx-border-width: 10px;"
                    + "-fx-border-radius: 10px;"
                    + "-fx-border-style: solid;"
                    + "-fx-control-inner-background: #ffffff;"
                    + "-fx-border-color: #696969;";

    public static int timer;

    public static void system_message(String what, int set_timer) {

        timer = set_timer;

        message_text.setText(what);
        message_text.setStyle("-fx-opacity: 1;" + message_text_style);
        message_text.setMinHeight(350);
        message_text.toFront();

        Thread system_message_thread = new Thread(new Runnable() {

            public void run() {

                try {
                    Thread.sleep(timer);
                } catch (InterruptedException ex) {

                }

                Platform.runLater(new Runnable() {

                    public void run() {

                        message_text.setStyle("-fx-opacity: 0;" + message_text_style);
                        message_text.toBack();

                    }

                });

            }

        });

        system_message_thread.start();

    }

    @Override
    public void start(Stage primaryStage) {
        // make a book folder
        try {
            new File("book").mkdir();
        } catch (Exception e) {

        }

        init_app();

        // set icon for app
        primaryStage.getIcons().add(new Image("file:Chess Personalized Repertoire Tool/src/javachessgui/resources/CPRT.png"));

        primaryStage.setTitle("Chess Personalized Repertoire Tool");
        primaryStage.setX(0);
        primaryStage.setY(0);

        Group root = new Group();

        gui = new Gui(primaryStage);

        root.getChildren().add(gui.horizontal_box);

        message_text.setWrapText(true);
        message_text.setTranslateX(30);
        message_text.setTranslateY(30);
        message_text.setStyle("-fx-opacity: 0;" + message_text_style);
        root.getChildren().add(message_text);

        primaryStage.setScene(new Scene(root));
        primaryStage.show();

        system_message("Welcome to CPRT!", 2000); // for some reason removing this breaks the program

//        System.out.println("application started");
    }

    @Override
    public void stop() {

        gui.shutdown();

//        System.out.println("application stopped");
    }

    private void init_app() {
        Board.init_class();

//        System.out.println("application initialized");
    }

    public static void main(String[] args) {
        launch(args);
    }

}
