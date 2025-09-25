package qupath.ext.honey.ui

import qupath.lib.gui.QuPathGUI
import qupath.lib.gui.dialogs.Dialogs
import javafx.scene.Node
import javafx.scene.control.Button
import javafx.scene.control.Separator
import javafx.scene.control.ToolBar
import javafx.scene.image.ImageView
import javafx.scene.layout.Region
import org.controlsfx.control.action.Action
import org.controlsfx.control.action.ActionUtils

class ToolbarManager {

    // Explicit markers you can use in your list
    static final Object SEP = new Object()   // vertical separator
    static final Object GAP = new Object()   // fixed-width gap (default width below)

    private java.util.List<Node> oldItems = null
    private boolean customActive = false

    boolean isCustomActive() { customActive }

    /**
     * Build the toolbar from a list that can contain:
     *  - org.controlsfx.control.action.Action  -> turned into a Button
     *  - ToolbarManager.SEP                    -> thin vertical Separator
     *  - ToolbarManager.GAP                    -> fixed-width gap (defaultWidth px)
     *  - Integer (e.g., 24)                    -> fixed-width gap of that many pixels
     *  - null                                  -> treated like GAP (defaultWidth px)
     */
    void applyCustom(final QuPathGUI qupath, final java.util.List<?> items,
                     final int defaultGapWidth = 12, final int iconPx = 18) {
        ToolBar tb = resolveToolbar(qupath)
        if (tb == null) return

        if (oldItems == null)
            oldItems = new java.util.ArrayList<>(tb.getItems())

        tb.getItems().clear()
        tb.setMinHeight(36); tb.setPrefHeight(36)
        tb.setStyle("-fx-padding: 4 6 4 6;")

        items.each { it ->
            if (it instanceof Action) {
                Button btn = ActionUtils.createButton((Action)it)
                // keep icons tidy
                if (btn.getGraphic() instanceof ImageView) {
                    ImageView iv = (ImageView) btn.getGraphic()
                    iv.setFitWidth(iconPx); iv.setFitHeight(iconPx); iv.setPreserveRatio(true)
                }
                btn.setStyle("-fx-padding: 6 8 6 8;")
                tb.getItems().add(btn)

            } else if (it === SEP) {
                tb.getItems().add(new Separator())

            } else if (it === GAP || it == null) {
                tb.getItems().add(fixedGap(defaultGapWidth))

            } else if (it instanceof Integer) {
                tb.getItems().add(fixedGap((Integer)it))

            } else {
                // Ignore unknown entries rather than failing
            }
        }

        customActive = true
    }

    void applyDefault(final QuPathGUI qupath) {
        ToolBar tb = resolveToolbar(qupath)
        if (tb == null) return
        if (oldItems != null) {
            tb.getItems().setAll(oldItems)
            customActive = false
        } else {
            Dialogs.showInfoNotification("HoneyExecuter", "No saved default toolbar to restore.")
        }
    }

    private static Region fixedGap(int px) {
        Region r = new Region()
        r.setMinWidth(px); r.setPrefWidth(px); r.setMaxWidth(px)
        return r
    }

    private ToolBar resolveToolbar(QuPathGUI qupath) {
        ToolBar tb = null
        if (qupath.metaClass.respondsTo(qupath, 'getToolBar'))      tb = qupath.getToolBar()
        else if (qupath.metaClass.respondsTo(qupath, 'getToolbar')) tb = qupath.getToolbar()
        if (tb == null)
            Dialogs.showInfoNotification("HoneyExecuter", "Toolbar API not found; menu items added only.")
        return tb
    }
}
