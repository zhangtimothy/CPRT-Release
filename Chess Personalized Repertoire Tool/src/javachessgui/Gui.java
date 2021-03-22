package javachessgui;

import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;


public class Gui {

    private Board b;
    private Game g;
    private Stage s;

    public HBox horizontal_box = new HBox(5);

    public void shutdown() {
        b.stop_engine_process();
    }

    public Gui(Stage set_s) {

        s = set_s;

        b = new Board(true);
        b.s = s;

        g = new Game(s, b);

        g.reset(b.report_fen());

        b.g = g;

        HBox leftPadding = new HBox();
        leftPadding.setMinWidth(10);
        leftPadding.setMaxWidth(10);
        horizontal_box.getChildren().add(leftPadding);
        horizontal_box.getChildren().add(b.main_box);
        horizontal_box.getChildren().add(new VBox());
        horizontal_box.getChildren().add(g.vertical_box);
        HBox rightPadding = new HBox();
        rightPadding.setMinWidth(10);
        rightPadding.setMaxWidth(10);
        horizontal_box.getChildren().add(rightPadding);

    }

}
