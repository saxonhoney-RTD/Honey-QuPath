package qupath.ext.honey

import qupath.lib.common.Version
import qupath.lib.gui.QuPathGUI
import qupath.lib.gui.extensions.QuPathExtension

import javafx.scene.control.Menu
import javafx.scene.control.SeparatorMenuItem
import org.controlsfx.control.action.Action
import org.controlsfx.control.action.ActionUtils

import qupath.ext.honey.actions.HoneyActions
import qupath.ext.honey.ui.ToolbarManager
import javafx.application.Platform

class HoneyExecuter implements QuPathExtension {

    @Override String getName()        { "HoneyExecuter" }
    @Override String getDescription() { "Toolbar + menu for fixed-size rectangles & IHC export" }
    @Override Version getQuPathVersion() { Version.parse("v0.6.0") }

    // Toolbar manager (instantiate!)
    private ToolbarManager toolbar = new ToolbarManager()

    // Optional toggle action if you add it later
    private Action toggleAction

    // Shorthand markers for toolbar layout
    final Object SEP = ToolbarManager.SEP
    final Object GAP = ToolbarManager.GAP

    @Override
    void installExtension(final QuPathGUI qupath) {
        // Build actions
        def presetsByGroup = HoneyActions.buildAllPresetActions() // Map<String, List<Action>>

        Action rect10       = HoneyActions.rectPreset("10x")
        Action rect20       = HoneyActions.rectPreset("20x")
        Action rect40       = HoneyActions.rectPreset("40x")

        Action rectCustom   = HoneyActions.rectCustom("Custom Rectangle", "/icons/rectCustom.png")
        Action exportIHC    = HoneyActions.exportIHC("Export ROIs", "/icons/export.png")
        Action exportRaw    = HoneyActions.exportROIsRaw("Export ROIs (raw)", "/icons/exportraw.png")
        Action exportAllProj= HoneyActions.exportAllProjectROIs("Export all ROIs (project)", "/icons/exportall.png")

        // Toolbar layout: exact control over separators & gaps
        def customItems = [rect10, rect20, rect40, SEP, exportIHC]

        // Top-level menu
        def honeyMenu = qupath.getMenu("Honey", true)

        // Quick rectangle actions
        honeyMenu.items.add(ActionUtils.createMenuItem(rect10))
        honeyMenu.items.add(ActionUtils.createMenuItem(rect20))
        honeyMenu.items.add(ActionUtils.createMenuItem(rect40))
        honeyMenu.items.add(new SeparatorMenuItem())

        // Export actions
        honeyMenu.items.add(ActionUtils.createMenuItem(exportIHC))
        honeyMenu.items.add(ActionUtils.createMenuItem(exportAllProj))

        // Advanced submenu
        Menu adv_actions = new Menu("Advanced")

        // Preset Rectangles submenu built from groups
        Menu rectMenu = new Menu("Preset Rectangles")
        presetsByGroup.each { groupName, actions ->
            Menu groupMenu = new Menu(groupName)
            actions.each { act -> groupMenu.items.add(ActionUtils.createMenuItem(act)) }
            rectMenu.items.add(groupMenu)
        }
        adv_actions.items.add(rectMenu)

        // Other advanced items
        adv_actions.items.add(ActionUtils.createMenuItem(rectCustom))
        adv_actions.items.add(new SeparatorMenuItem())
        adv_actions.items.add(ActionUtils.createMenuItem(exportRaw))

        // Attach Advanced to Honey
        honeyMenu.items.add(adv_actions)

        // Build the toolbar with explicit separators/gaps
        toolbar.applyCustom(qupath, customItems, 22, 22)

        // Toggle action (default to custom toolbar on load)
        // Build the toolbar once on the FX thread
        Platform.runLater {
            toolbar.applyCustom(qupath, customItems, 22, 22)
        }

        // Optional toggle to switch default/custom
        toggleAction = new Action("Use default toolbar", {
            Platform.runLater {
                if (toolbar.isCustomActive()) {
                    toolbar.applyDefault(qupath)
                    toggleAction.setText("Use custom toolbar")
                } else {
                    toolbar.applyCustom(qupath, customItems, 22, 22)
                    toggleAction.setText("Use default toolbar")
                }
            }
        })
        honeyMenu.items.add(new SeparatorMenuItem())
        honeyMenu.items.add(ActionUtils.createMenuItem(toggleAction))

    }
}
