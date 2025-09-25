package qupath.ext.honey.actions

import javafx.event.ActionEvent
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyCodeCombination
import javafx.scene.input.KeyCombination
import org.controlsfx.control.action.Action

import qupath.lib.gui.dialogs.Dialogs
import qupath.lib.gui.scripting.QPEx
import qupath.lib.roi.ROIs
import qupath.lib.regions.ImagePlane
import qupath.lib.objects.PathObjects

import qupath.lib.regions.RegionRequest
import javax.imageio.ImageIO
import javax.imageio.ImageTypeSpecifier
import javax.imageio.metadata.IIOMetadataNode
import java.awt.image.BufferedImage

class HoneyActions {

  

    // Single source of truth for ROI presets
    static final List<Map> ROI_TYPES = [
        [ name:"Rectangles",  aspectRatio:16210.0/12160.0, targetWidth:1500, targetHeight:1125,
        exactDimensions:["16210x12160":"2x","8105x6080":"4x","3242x2432":"10x","1621x1216":"20x","811x608":"40x"] ],
        [ name:"Squares",     aspectRatio:1.0,             targetWidth:625,  targetHeight:625,
        exactDimensions:["6755x6755":"2x","3377x3377":"4x","1351x1351":"10x","675x675":"20x","338x338":"40x"] ],
        [ name:"SmallRects",  aspectRatio:8105.0/6080.0,   targetWidth:750,  targetHeight:562,
        exactDimensions:["8105x6080":"2x","4053x3040":"4x","1621x1216":"10x","811x608":"20x","406x304":"40x"] ],
        [ name:"SmallSquares",aspectRatio:1.0,             targetWidth:330,  targetHeight:330,
        exactDimensions:["3567x3567":"2x","1783x1783":"4x","713x713":"10x","357x357":"20x","178x178":"40x"] ],
        [ name:"Covers",      aspectRatio:29178.0/19456.0, targetWidth:2700, targetHeight:1800,
        exactDimensions:["29178x19456":"2x","14589x9728":"4x","5836x3891":"10x","2918x1946":"20x","1460x973":"40x"] ],
    ]

    // Static getter used by other actions [export all in project]
    static List<Map> roiTypesDef() {
        // Return a shallow copy so counts/skips don’t mutate the constant
        return ROI_TYPES.collect { new LinkedHashMap<>(it) }
    }

    private static int[] parseDim(String s) {
        def (w, h) = s.split("x").collect { it.toInteger() }
        return [w, h] as int[]
    }

    // Icon & shortcut just for the main Rectangles 10x/20x/40x
    private static final Map<String,String> MAIN_RECT_ICONS = [
        "2x": "/icons/rect2.png",
        "4x": "/icons/rect4.png",
        "10x": "/icons/rect10.png",
        "20x": "/icons/rect20.png",
        "40x": "/icons/rect40.png"
    ]

    private static void maybeAttachIconAndAccel(org.controlsfx.control.action.Action a,
                                                String groupName, String labelMag) {
        if (groupName != "Rectangles") return
        // accelerators
        switch (labelMag) {
            case "10x":
                a.accelerator = new javafx.scene.input.KeyCodeCombination(
                    javafx.scene.input.KeyCode.DIGIT1,
                    javafx.scene.input.KeyCodeCombination.SHORTCUT_DOWN,
                    javafx.scene.input.KeyCodeCombination.SHIFT_DOWN)
                break
            case "20x":
                a.accelerator = new javafx.scene.input.KeyCodeCombination(
                    javafx.scene.input.KeyCode.DIGIT2,
                    javafx.scene.input.KeyCodeCombination.SHORTCUT_DOWN,
                    javafx.scene.input.KeyCodeCombination.SHIFT_DOWN)
                break
            case "40x":
                a.accelerator = new javafx.scene.input.KeyCodeCombination(
                    javafx.scene.input.KeyCode.DIGIT4,
                    javafx.scene.input.KeyCodeCombination.SHORTCUT_DOWN,
                    javafx.scene.input.KeyCodeCombination.SHIFT_DOWN)
                break
        }
        // icons
        def iconRes = MAIN_RECT_ICONS[labelMag]
        if (iconRes) setIcon(a, iconRes)
    }




    // ---------- Actions ----------
    /** Create Actions for all preset rectangle sizes defined in roiTypes. */

    private static org.controlsfx.control.action.Action makeRectAction(String title, int w, int h) {
        return new org.controlsfx.control.action.Action(title, { javafx.event.ActionEvent e ->
            try {
                def imgData = qupath.lib.gui.scripting.QPEx.getCurrentImageData()
                if (imgData == null) {
                    qupath.lib.gui.dialogs.Dialogs.showInfoNotification("HoneyExecuter", "No image open!")
                    return
                }
                def viewer = qupath.lib.gui.scripting.QPEx.getCurrentViewer()
                double cx = viewer.getCenterPixelX(), cy = viewer.getCenterPixelY()
                def roi = qupath.lib.roi.ROIs.createRectangleROI(
                    cx - w/2.0, cy - h/2.0, w, h, qupath.lib.regions.ImagePlane.getDefaultPlane())
                def ann = qupath.lib.objects.PathObjects.createAnnotationObject(roi)
                qupath.lib.gui.scripting.QPEx.addObject(ann)
                qupath.lib.gui.scripting.QPEx.selectObjects(ann)
                qupath.lib.gui.dialogs.Dialogs.showInfoNotification("HoneyExecuter", "Created ${title}: ${w}×${h}")
            } catch (Throwable t) {
                qupath.lib.gui.dialogs.Dialogs.showErrorNotification(
                    "HoneyExecuter", "Failed: ${t.class.simpleName}: ${t.message}")
            }
        })
    }

    /** Returns a Map: groupName -> List<Action> (e.g. "Rectangles" -> [2x,4x,10x,20x,40x]) */
    static Map<String, List<org.controlsfx.control.action.Action>> buildAllPresetActions() {
        def result = new LinkedHashMap<String, List<org.controlsfx.control.action.Action>>()
        ROI_TYPES.each { def group ->
            def list = []
            (group.exactDimensions as Map<String,String>).each { dim, labelMag ->
                def (w, h) = parseDim(dim)
                def title = "${labelMag} ${group.name}"
                def a = makeRectAction(title, w, h)
                // only give icons/accelerators to the 10x/20x/40x in the "Rectangles" family
                maybeAttachIconAndAccel(a, group.name as String, labelMag as String)
                list << a
            }
            result[group.name as String] = list
        }
        return result
    }

    static org.controlsfx.control.action.Action rectPreset(String labelMag /* "10x"|"20x"|"40x" */,
                                                        String iconRes = null) {
        // Find the "Rectangles" entry
        def rects = ROI_TYPES.find { it.name == "Rectangles" }?.exactDimensions
        if (!rects) throw new IllegalStateException("Rectangles not defined in ROI_TYPES!")

        // Find the dim string whose value == labelMag (e.g. "3242x2432" for "10x")
        def dimStr = rects.find { k, v -> v == labelMag }?.key
        if (!dimStr) throw new IllegalArgumentException("No ${labelMag} in Rectangles.exactDimensions")

        def (w, h) = parseDim(dimStr)
        def a = makeRectAction("${labelMag} Rectangles", w, h)

        // icons/accelerators for these three
        maybeAttachIconAndAccel(a, "Rectangles", labelMag)
        if (iconRes) setIcon(a, iconRes)  // override if you want a different icon
        return a
    }


////////////////////////////////////////////////////////////////////////
    static Action rectCustom(String label, String iconRes = null) {
        Action a = new Action(label, { ActionEvent e ->
            try {
                def res = promptSize(1621, 1216)
                if (res == null) return
                makeRect(res.width as int, res.height as int)
            } catch (Throwable t) {
                Dialogs.showErrorNotification("HoneyExecuter", "Custom rectangle failed: ${t.class.simpleName}: ${t.message}")
            }
        })
        a.accelerator = new KeyCodeCombination(KeyCode.C, KeyCombination.SHORTCUT_DOWN, KeyCombination.SHIFT_DOWN)
        setIcon(a, iconRes)
        return a
    }

    // === Export ALL project ROIs (preset-matched, resized, no color correction) ===
    static Action exportAllProjectROIs(String label = "Export all ROIs (project)", String iconRes = null) {
        Action a = new Action(label, { javafx.event.ActionEvent ev ->
            // Run off the FX thread so the UI doesn’t freeze
            new Thread({
                try {
                    exportAllProjectImpl()
                } catch (Throwable t) {
                    qupath.lib.gui.dialogs.Dialogs.showErrorNotification("HoneyExecuter",
                            "Project export failed: ${t.class.simpleName}: ${t.message}")
                }
            }, "HoneyExecuter-ProjectExport").start()
        })
        // Optional shortcut: Ctrl/Cmd+Shift+A
        a.accelerator = new javafx.scene.input.KeyCodeCombination(
                javafx.scene.input.KeyCode.A,
                javafx.scene.input.KeyCombination.SHORTCUT_DOWN,
                javafx.scene.input.KeyCombination.SHIFT_DOWN
        )
        setIcon(a, iconRes) // silently ignores if iconRes not found
        return a
    }


    // === RAW export (no color correction, no resizing, any ROI size) ===
    static Action exportROIsRaw(String label = "Export ROIs (raw)", String iconRes = null) {
        Action a = new Action(label, { ActionEvent ev ->
            try { exportRawImpl() }
            catch (Throwable t) { Dialogs.showErrorNotification("HoneyExecuter", "Raw export failed: ${t.class.simpleName}: ${t.message}") }
        })
        // Optional shortcut (Ctrl/Cmd+Shift+R)
        a.accelerator = new KeyCodeCombination(KeyCode.R, KeyCombination.SHORTCUT_DOWN, KeyCombination.SHIFT_DOWN)
        setIcon(a, iconRes)
        return a
    }


    static Action exportIHC(String label, String iconRes = null) {
        Action a = new Action(label, { ActionEvent ev ->
            try { exportIhcImpl() }
            catch (Throwable t) { Dialogs.showErrorNotification("HoneyExecuter", "Export failed: ${t.class.simpleName}: ${t.message}") }
        })
        a.accelerator = new KeyCodeCombination(KeyCode.E, KeyCombination.SHORTCUT_DOWN, KeyCombination.SHIFT_DOWN)
        setIcon(a, iconRes)
        return a
    }

    // ---------- UI helpers ----------

    private static void setIcon(Action a, String resPath, int px = 30) {
        if (resPath == null) return
        try {
            def url = HoneyActions.class.getResource(resPath)
            if (url == null) return
            def img = new Image(url.toExternalForm(), px as double, px as double, true, true)
            def iv  = new ImageView(img)
            iv.setFitWidth(px); iv.setFitHeight(px); iv.setPreserveRatio(true)
            a.setGraphic(iv)
        } catch (ignored) {}
    }

    private static Map promptSize(int defW, int defH) {
        // simple dialog using JavaFX controls (same as we used before)
        def dlg = new javafx.scene.control.Dialog<Map>()
        dlg.setTitle("Create Custom Rectangle")
        dlg.setHeaderText("Enter width × height (pixels)")
        def okType = new javafx.scene.control.ButtonType("Create", javafx.scene.control.ButtonBar.ButtonData.OK_DONE)
        dlg.getDialogPane().getButtonTypes().addAll(okType, javafx.scene.control.ButtonType.CANCEL)

        def wField = new javafx.scene.control.TextField(String.valueOf(defW))
        def hField = new javafx.scene.control.TextField(String.valueOf(defH))
        def grid = new javafx.scene.layout.GridPane()
        grid.setHgap(8); grid.setVgap(8); grid.setPadding(new javafx.geometry.Insets(10,10,10,10))
        grid.add(new javafx.scene.control.Label("Width (px):"), 0, 0); grid.add(wField, 1, 0)
        grid.add(new javafx.scene.control.Label("Height (px):"),0, 1); grid.add(hField, 1, 1)
        dlg.getDialogPane().setContent(grid)

        def okBtn = dlg.getDialogPane().lookupButton(okType)
        def validate = {
            boolean v = wField.text?.isInteger() && hField.text?.isInteger() &&
                        wField.text.toInteger() > 0 && hField.text.toInteger() > 0
            okBtn.setDisable(!v)
        }
        wField.textProperty().addListener({ a,b,c -> validate() } as javafx.beans.value.ChangeListener)
        hField.textProperty().addListener({ a,b,c -> validate() } as javafx.beans.value.ChangeListener)
        validate()

        dlg.setResultConverter { btn -> btn == okType ? [width: wField.text.toInteger(), height: hField.text.toInteger()] : null }
        return dlg.showAndWait().orElse(null)
    }

    // ---------- Rectangle core ----------

    private static void makeRect(int w, int h) {
        def imageData = QPEx.getCurrentImageData()
        if (imageData == null) {
            Dialogs.showInfoNotification("HoneyExecuter", "No image open!")
            return
        }
        def viewer = QPEx.getCurrentViewer()
        double cx = viewer.getCenterPixelX(), cy = viewer.getCenterPixelY()
        double x = cx - w/2.0, y = cy - h/2.0
        def roi = ROIs.createRectangleROI(x, y, w, h, ImagePlane.getDefaultPlane())
        def annotation = PathObjects.createAnnotationObject(roi)
        QPEx.addObject(annotation)
        QPEx.selectObjects(annotation)
        Dialogs.showInfoNotification("HoneyExecuter", "Created rectangle: ${w} × ${h} px")
    }

    // ---------- Export implementation (your color curve + TIFF DPI) ----------

    private static void exportRawImpl() {
        def imageData = QPEx.getCurrentImageData()
        if (imageData == null) { Dialogs.showInfoNotification("HoneyExecuter", "No image open!"); return }
        def server = imageData.getServer()

        def annotations = QPEx.getAnnotationObjects()
        if (annotations.isEmpty()) { Dialogs.showInfoNotification("HoneyExecuter", "No annotations found!"); return }

        // Ask once: apply color curve?
        boolean doCurve = Dialogs.showYesNoDialog(
            "HoneyExecuter",
            "Apply color curve (140 → 114) to exported ROIs?"
        )

        // Output folder (separate from the IHC-corrected ones)
        File outDir
        try {
            def dirPath = QPEx.buildFilePath(QPEx.PROJECT_BASE_DIR, 'roi_exports_raw')
            QPEx.mkdirs(dirPath)
            outDir = new File(dirPath)
        } catch (Throwable t) {
            outDir = new File(System.getProperty("user.home"), "roi_exports_raw"); outDir.mkdirs()
        }

        double dpi = 300.0

        // Base filename
        def originalFilename = new File(server.getMetadata().getName()).getName()
        def baseFilename = originalFilename.lastIndexOf('.') > 0 ?
            originalFilename.substring(0, originalFilename.lastIndexOf('.')) : originalFilename

        String tag = doCurve ? "RAW_CURVE" : "RAW"

        int exported = 0
        for (int idx = 0; idx < annotations.size(); idx++) {
            def ann = annotations[idx]
            def roi = ann.getROI(); if (roi == null) continue

            double bx = roi.getBoundsX(), by = roi.getBoundsY()
            double bw = roi.getBoundsWidth(), bh = roi.getBoundsHeight()

            // Read ROI at native resolution (no resize)
            def req = qupath.lib.regions.RegionRequest.createInstance(
                server.getPath(), 1.0, (int)bx, (int)by, (int)bw, (int)bh
            )
            BufferedImage patch = server.readRegion(req)

            // Optionally apply curve
            if (doCurve) {
                applyCurve(patch, 140, 114)  // <— uses your existing helper
            }

            String outName = "${baseFilename}_${tag}_${idx}_${(int)bw}x${(int)bh}.tif"
            File outFile = new File(outDir, outName)

            boolean ok = writeTiffWithDPI(patch, outFile, dpi)
            if (ok) {
                exported++
                println "Exported ${tag} ROI ${idx}: ${outName}"
            } else {
                println "Failed to export ${tag} ROI ${idx}: ${outName}"
            }
        }

        Dialogs.showInfoNotification("HoneyExecuter",
            "Export complete (total: ${exported}).\nSaved to: ${outDir}\nCurve applied: ${doCurve}")
    }

    private static void exportIhcImpl() {
        def imageData = QPEx.getCurrentImageData()
        if (imageData == null) { Dialogs.showInfoNotification("HoneyExecuter", "No image open!"); return }
        def server = imageData.getServer()

        def annotations = QPEx.getAnnotationObjects()
        if (annotations.isEmpty()) { Dialogs.showInfoNotification("HoneyExecuter", "No annotations found!"); return }

        File outDir
        try {
            def dirPath = QPEx.buildFilePath(QPEx.PROJECT_BASE_DIR, 'roi_exports')
            QPEx.mkdirs(dirPath)
            outDir = new File(dirPath)
        } catch (Throwable t) {
            outDir = new File(System.getProperty("user.home"), "roi_exports"); outDir.mkdirs()
        }

        double dpi = 300.0, aspectTol = 0.01
        int dimTol = 10

        def roiTypes = [
            [ name:"Rectangles", aspectRatio:16210.0/12160.0, targetWidth:1500, targetHeight:1125,
              exactDimensions:["16210x12160":"2x","8105x6080":"4x","3242x2432":"10x","1621x1216":"20x","811x608":"40x"], count:0, skippedCount:0 ],
            [ name:"Squares", aspectRatio:1.0, targetWidth:625, targetHeight:625,
              exactDimensions:["6755x6755":"2x","3377x3377":"4x","1351x1351":"10x","675x675":"20x","338x338":"40x"], count:0, skippedCount:0 ],
            [ name:"SmallRects", aspectRatio:8105.0/6080.0, targetWidth:750, targetHeight:562,
              exactDimensions:["8105x6080":"2x","4053x3040":"4x","1621x1216":"10x","811x608":"20x","406x304":"40x"], count:0, skippedCount:0 ],
            [ name:"SmallSquares", aspectRatio:1.0, targetWidth:330, targetHeight:330,
              exactDimensions:["3567x3567":"2x","1783x1783":"4x","713x713":"10x","357x357":"20x","178x178":"40x"], count:0, skippedCount:0 ],
            [ name:"Covers", aspectRatio:29178.0/19456.0, targetWidth:2700, targetHeight:1800,
              exactDimensions:["29178x19456":"2x","14589x9728":"4x","5836x3891":"10x","2918x1946":"20x","1460x973":"40x"], count:0, skippedCount:0 ]
        ]

        // Per-magnification counters for the main "Rectangles" family
        def rectMagCounters = new LinkedHashMap<String,Integer>().withDefault { 0 }

        // Per-(family, magnification) counters for everything else
        def otherCounters = new LinkedHashMap<String,Integer>().withDefault { 0 }

        // helpers
        def nextRectIndex = { String mag ->
            rectMagCounters[mag] = (rectMagCounters[mag] ?: 0) + 1
            rectMagCounters[mag]
        }
        def nextOtherIndex = { String family, String mag ->
            String key = family + "|" + mag
            otherCounters[key] = (otherCounters[key] ?: 0) + 1
            otherCounters[key]
        }


        def originalFilename = new File(server.getMetadata().getName()).getName()
        def baseFilename = originalFilename.lastIndexOf('.') > 0 ?
            originalFilename.substring(0, originalFilename.lastIndexOf('.')) : originalFilename

        for (int idx = 0; idx < annotations.size(); idx++) {
            def ann = annotations[idx]
            def roi = ann.getROI(); if (roi == null) continue

            double bx = roi.getBoundsX(), by = roi.getBoundsY()
            double bw = roi.getBoundsWidth(), bh = roi.getBoundsHeight()
            int roiW = (int)bw, roiH = (int)bh
            String dims = "${roiW}x${roiH}"

            boolean matched = false
            for (params in roiTypes) {
                double rAR = bw / bh
                if (Math.abs(rAR - params.aspectRatio) > aspectTol) continue

                String mag = null
                params.exactDimensions.each { dim, label ->
                    def (w,h) = dim.split("x").collect { it.toInteger() }
                    if (Math.abs(roiW - w) <= dimTol && Math.abs(roiH - h) <= dimTol) mag = label
                }
                if (mag == null) { params.skippedCount++; continue }

                matched = true

                double downsample = Math.max(bw/params.targetWidth, bh/params.targetHeight)
                def request = RegionRequest.createInstance(server.getPath(), downsample, (int)bx, (int)by, (int)bw, (int)bh)

                BufferedImage img = server.readRegion(request)
                BufferedImage resized = new BufferedImage(params.targetWidth as int, params.targetHeight as int, BufferedImage.TYPE_INT_RGB)
                def g = resized.createGraphics()
                g.drawImage(img, 0, 0, params.targetWidth as int, params.targetHeight as int, null); g.dispose()

                applyCurve(resized, 140, 114)

                String outName
                if (params.name == "Rectangles" && ["2x","4x","10x","20x","40x"].contains(mag)) {
                    // No family name in filename; counter is per magnification
                    int n = nextRectIndex(mag)
                    outName = "${baseFilename}_${mag}_${n}.tif"
                } else {
                    // Keep family name; counter is per (family, magnification)
                    int n = nextOtherIndex(params.name as String, mag as String)
                    outName = "${baseFilename}_${params.name}_${mag}_${n}.tif"
                }
                File outFile = new File(outDir, outName)


                boolean ok = writeTiffWithDPI(resized, outFile, dpi)
                if (ok) { params.count++; println "Exported ${params.name}: ${outName} (${dims}, ${mag}, DPI ${dpi})" }
                else    { println "Failed to export: ${outName}" }
                break
            }
            if (!matched) println "ROI ${idx} (${dims}) did not match any type"
        }

        int total = 0
        roiTypes.each { p -> total += p.count; println "${p.name}: curve adjusted and exported ${p.count} (skipped: ${p.skippedCount})" }
        println "Total exported: ${total}"
        Dialogs.showInfoNotification("HoneyExecuter", "Export complete (total: ${total}).")
    }
    // ---------- PROJECT Export implementation----------
    private static void exportAllProjectImpl() {
        def project = qupath.lib.gui.scripting.QPEx.getProject()
        if (project == null) {
            println "No project open!"
            qupath.lib.gui.dialogs.Dialogs.showInfoNotification("HoneyExecuter", "No project open!")
            return
        }

        // Output folder under the project dir
        java.io.File outDir
        try {
            def dirPath = qupath.lib.gui.scripting.QPEx.buildFilePath(
                    qupath.lib.gui.scripting.QPEx.PROJECT_BASE_DIR, "all_roi_exports")
            qupath.lib.gui.scripting.QPEx.mkdirs(dirPath)
            outDir = new java.io.File(dirPath)
        } catch (Throwable t) {
            outDir = new java.io.File(System.getProperty("user.home"), "all_roi_exports")
            outDir.mkdirs()
        }

        final double DPI = 300.0
        final double aspectTol = 0.01
        final int dimTol = 10

        // Clone the canonical definitions & add counters
        def roiTypes = qupath.ext.honey.actions.HoneyActions.roiTypesDef().collect { m ->
            def copy = new java.util.LinkedHashMap<>(m)
            copy.count = 0
            copy.skippedCount = 0
            return copy
        }


        def entries = project.getImageList()
        int totalImages = entries.size()
        int imageIdx = 0
        println "Processing ${totalImages} images in project..."
        int totalExported = 0

        for (def entry : entries) {
            imageIdx++
            def imageData = entry.readImageData()   // does not steal UI focus
            def server = imageData.getServer()

            // Base name
            def originalFilename = new java.io.File(server.getMetadata().getName()).getName()
            def baseFilename = originalFilename.lastIndexOf('.') > 0
                    ? originalFilename.substring(0, originalFilename.lastIndexOf('.'))
                    : originalFilename

            def annotations = imageData.getHierarchy().getAnnotationObjects()
            if (annotations.isEmpty()) {
                println "No annotations in ${baseFilename}, skipping."
                // Clean up this server
                try { server.close() } catch (ignored) {}
                continue
            }

            println "\n===== ${imageIdx}/${totalImages}: ${baseFilename} (${annotations.size()} annotations) ====="



            for (int ai = 0; ai < annotations.size(); ai++) {
                def ann = annotations[ai]
                def roi = ann.getROI()
                if (roi == null) continue

                double bx = roi.getBoundsX(), by = roi.getBoundsY()
                double bw = roi.getBoundsWidth(), bh = roi.getBoundsHeight()
                int roiW = (int)bw, roiH = (int)bh
                boolean matched = false

                // Try to match this ROI against all defined groups
                for (params in roiTypes) {
                    double rAR = bw / bh
                    if (Math.abs(rAR - (params.aspectRatio as double)) > aspectTol) {
                        continue
                    }

                    String mag = null
                    (params.exactDimensions as Map<String,String>).each { dim, label ->
                        def (w, h) = dim.split("x").collect { it.toInteger() }
                        if (Math.abs(roiW - w) <= dimTol && Math.abs(roiH - h) <= dimTol) {
                            mag = label
                        }
                    }

                    if (mag == null) {
                        params.skippedCount = (params.skippedCount as int) + 1
                        continue
                    }

                    // Make a request sized for targetWidth/Height
                    double downsample = Math.max(bw / (params.targetWidth as double), bh / (params.targetHeight as double))
                    def request = qupath.lib.regions.RegionRequest.createInstance(
                            server.getPath(), downsample, (int)bx, (int)by, (int)bw, (int)bh)

                    // Read & resize to exact target dims
                    java.awt.image.BufferedImage patch = server.readRegion(request)
                    java.awt.image.BufferedImage resized = new java.awt.image.BufferedImage(
                            params.targetWidth as int, params.targetHeight as int, java.awt.image.BufferedImage.TYPE_INT_RGB)
                    def g = resized.createGraphics()
                    g.drawImage(patch, 0, 0, params.targetWidth as int, params.targetHeight as int, null)
                    g.dispose()

                    applyCurve(resized, 140, 114)

                    // Write (with color correction)
                    String outName = "${baseFilename}_${params.name}_${mag}_${params.count}.tif"
                    java.io.File outFile = new java.io.File(outDir, outName)
                    boolean ok = writeTiffWithDPI(resized, outFile, DPI)
                    if (ok) {
                        params.count = (params.count as int) + 1
                        totalExported++
                        println "Exported ${params.name}: ${outName} (ROI ${roiW}x${roiH}, ${mag}, DPI ${DPI})"
                    } else {
                        println "Failed to export: ${outName}"
                    }

                    matched = true
                    break // stop searching roiTypes once matched
                }

                if (!matched) {
                    println "ROI ${ai} (${roiW}x${roiH}) did not match any defined type"
                }
            }

            // Free resources for this image
            try { server.close() } catch (ignored) {}
        }

        // Summary
        println "\n=== EXPORT SUMMARY ==="
        println "All exports saved to: ${outDir}"
        roiTypes.each { p ->
            println "${p.name}: curve adjusted and exported ${p.count} (skipped: ${p.skippedCount})"
        }
        println "Total exported ROIs: ${totalExported}"

        qupath.lib.gui.dialogs.Dialogs.showInfoNotification(
                "HoneyExecuter", "Project export complete.\nTotal ROIs: ${totalExported}\nSaved to: ${outDir}")
    }

    // ---------- Image ops (color curve + TIFF DPI) ----------

    private static void applyCurve(BufferedImage img, int pivotIn, int pivotOut) {
        double m1 = pivotOut / (double)pivotIn
        double m2 = (255.0 - pivotOut) / (255.0 - pivotIn)
        int[] lut = new int[256]
        for (int v = 0; v < 256; v++) {
            int out = (v <= pivotIn) ? Math.round((float)(m1 * v))
                                     : Math.round((float)(pivotOut + m2 * (v - pivotIn)))
            lut[v] = Math.max(0, Math.min(255, out))
        }
        int w = img.getWidth(), h = img.getHeight()
        int[] px = img.getRGB(0, 0, w, h, null, 0, w)
        for (int i = 0; i < px.length; i++) {
            int p = px[i]
            int a = (p >>> 24) & 0xFF
            int r = (p >>> 16) & 0xFF
            int g = (p >>>  8) & 0xFF
            int b = (p)        & 0xFF
            r = lut[r]; g = lut[g]; b = lut[b]
            px[i] = (a << 24) | (r << 16) | (g << 8) | b
        }
        img.setRGB(0, 0, w, h, px, 0, w)
    }

    private static boolean writeTiffWithDPI(BufferedImage image, File outputFile, double dpi) {
        try {
            def writer = ImageIO.getImageWritersByFormatName("tiff").next()
            def ios = ImageIO.createImageOutputStream(outputFile)
            writer.setOutput(ios)
            def writeParam = writer.getDefaultWriteParam()
            try { writeParam.setCompressionMode(javax.imageio.ImageWriteParam.MODE_EXPLICIT); writeParam.setCompressionType("LZW") } catch (ignored) {}

            def typeSpecifier = ImageTypeSpecifier.createFromBufferedImageType(BufferedImage.TYPE_INT_RGB)
            def metadata = writer.getDefaultImageMetadata(typeSpecifier, writeParam)
            if (!metadata.isReadOnly() && metadata.isStandardMetadataFormatSupported()) {
                int num = (int)dpi, den = 1
                def root = new IIOMetadataNode("javax_imageio_tiff_image_1.0")
                def ifd  = new IIOMetadataNode("TIFFIFD"); root.appendChild(ifd)
                def xRes = new IIOMetadataNode("TIFFField"); xRes.setAttribute("number","282"); xRes.setAttribute("name","XResolution")
                def xRat = new IIOMetadataNode("TIFFRationals"); def xVal = new IIOMetadataNode("TIFFRational"); xVal.setAttribute("value","$num/$den"); xRat.appendChild(xVal); xRes.appendChild(xRat); ifd.appendChild(xRes)
                def yRes = new IIOMetadataNode("TIFFField"); yRes.setAttribute("number","283"); yRes.setAttribute("name","YResolution")
                def yRat = new IIOMetadataNode("TIFFRationals"); def yVal = new IIOMetadataNode("TIFFRational"); yVal.setAttribute("value","$num/$den"); yRat.appendChild(yVal); yRes.appendChild(yRat); ifd.appendChild(yRes)
                def unit = new IIOMetadataNode("TIFFField"); unit.setAttribute("number","296"); unit.setAttribute("name","ResolutionUnit")
                def s = new IIOMetadataNode("TIFFShorts"); def v = new IIOMetadataNode("TIFFShort"); v.setAttribute("value","2"); s.appendChild(v); unit.appendChild(s); ifd.appendChild(unit)
                metadata.mergeTree("javax_imageio_tiff_image_1.0", root)
                writer.write(null, new javax.imageio.IIOImage(image, null, metadata), writeParam)
            } else writer.write(image)
            ios.close(); writer.dispose()
            return true
        } catch (Exception e) {
            try { ImageIO.write(image, "tiff", outputFile); return true } catch (Exception e2) { return false }
        }
    }
}
