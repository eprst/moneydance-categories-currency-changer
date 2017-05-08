package com.moneydance.modules.features.ccc;

import com.moneydance.apps.md.controller.FeatureModule;
import com.moneydance.apps.md.controller.FeatureModuleContext;

import java.io.*;
import java.awt.*;

/**
 * Pluggable module used to give users access to a Account List
 * interface to Moneydance.
 */

public class Main
        extends FeatureModule {
    private CCCWindow cccWindow = null;

    public void init() {
        // the first thing we will do is register this module to be invoked
        // via the application toolbar
        FeatureModuleContext context = getContext();
        try {
            context.registerFeature(this, "showconsole",
                    getIcon(),
                    getName());
        } catch (Exception e) {
            e.printStackTrace(System.err);
        }
    }

    public void cleanup() {
        closeConsole();
    }

    private Image getIcon() {
        try {
            ClassLoader cl = getClass().getClassLoader();
            java.io.InputStream in =
                    cl.getResourceAsStream("/com/moneydance/modules/features/ccc/icon.gif");
            if (in != null) {
                ByteArrayOutputStream bout = new ByteArrayOutputStream(1000);
                byte buf[] = new byte[256];
                int n;
                while ((n = in.read(buf, 0, buf.length)) >= 0)
                    bout.write(buf, 0, n);
                return Toolkit.getDefaultToolkit().createImage(bout.toByteArray());
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    /**
     * Process an invocation of this module with the given URI
     */
    public void invoke(String uri) {
        String command = uri;
        int theIdx = uri.indexOf('?');
        if (theIdx >= 0) {
            command = uri.substring(0, theIdx);
        } else {
            theIdx = uri.indexOf(':');
            if (theIdx >= 0) {
                command = uri.substring(0, theIdx);
            }
        }

        if (command.equals("showconsole")) {
            showConsole();
        }
    }

    public String getName() {
        return "Change Categories Currency";
    }

    private synchronized void showConsole() {
        if (cccWindow == null) {
            cccWindow = new CCCWindow(this);
            cccWindow.setVisible(true);
        } else {
            cccWindow.setVisible(true);
            cccWindow.toFront();
            cccWindow.requestFocus();
        }
    }

    FeatureModuleContext getUnprotectedContext() {
        return getContext();
    }

    synchronized void closeConsole() {
        if (cccWindow != null) {
            cccWindow.goAway();
            cccWindow = null;
            System.gc();
        }
    }
}


