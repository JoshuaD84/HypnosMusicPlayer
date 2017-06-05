package net.joshuad.musicplayer;


import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
import javafx.geometry.Side;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;

/* Copyright "Cameron Tauxe": http://stackoverflow.com/questions/31051756/javafx-tab-fit-full-size-of-header
 */

public class StretchedTabPane extends TabPane {

    public StretchedTabPane() {
        super();
        setUpChangeListeners();
    }

    public StretchedTabPane(Tab... tabs) {
        super(tabs);
        setUpChangeListeners();
    }

    private void setUpChangeListeners() {

        widthProperty().addListener(new ChangeListener<Number>() {
            @Override public void changed(ObservableValue<? extends Number> value, Number oldWidth, Number newWidth) {
                Side side = getSide();
                int numTabs = getTabs().size();
                if ((side == Side.BOTTOM || side == Side.TOP) && numTabs != 0) {
                    setTabMinWidth(newWidth.intValue() / numTabs - (23));
                    setTabMaxWidth(newWidth.intValue() / numTabs - (23));
                }
            }
        });

        heightProperty().addListener(new ChangeListener<Number>() {
            @Override public void changed(ObservableValue<? extends Number> value, Number oldHeight, Number newHeight) {
                Side side = getSide();
                int numTabs = getTabs().size();
                if ((side == Side.LEFT || side == Side.RIGHT) && numTabs != 0) {
                    setTabMinWidth(newHeight.intValue() / numTabs - (23));
                    setTabMaxWidth(newHeight.intValue() / numTabs - (23));
               }
           }
        });

        getTabs().addListener(new ListChangeListener<Tab>() {
            public void onChanged(ListChangeListener.Change<? extends Tab> change){
                Side side = getSide();
                int numTabs = getTabs().size();
                if (numTabs != 0) {
                    if (side == Side.LEFT|| side == Side.RIGHT) {
                        setTabMinWidth(heightProperty().intValue() / numTabs - (20));
                        setTabMaxWidth(heightProperty().intValue() / numTabs - (20));
                    }
                    if (side == Side.BOTTOM || side == Side.TOP) {
                        setTabMinWidth(widthProperty().intValue() / numTabs - (20));
                        setTabMaxWidth(widthProperty().intValue() / numTabs - (20));
                    }
                }
            }
        });
   }
}