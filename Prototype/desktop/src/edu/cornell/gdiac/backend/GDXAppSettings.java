/*
 * GDXAppSettings.java
 *
 * This is a (necessary) replacement for LwjglApplicationConfiguration. It allows us to disable audio and then
 * re-enable it later.
 *
 * @author Walker M. White
 * @data April 10, 2020
 */
package edu.cornell.gdiac.backend;

import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;
import com.badlogic.gdx.LifecycleListener;
import com.badlogic.gdx.Graphics;
import com.badlogic.gdx.graphics.Color;
import org.lwjgl.LWJGLUtil;

/**
 * This class configures the launch settings for an instance of {@link GDXApp}.
 *
 * This class is a simplification of {@link LwjglApplicationConfiguration}, removing some of the settings that we
 * do not want students to touch.
 */
public class GDXAppSettings {
    /** The title of the application **/
    public String title;
    /** The width of the application window **/
    public int width  = 1280;
    /** The width of the application window **/
    public int height = 720;
    /** The x coordinate of the application, -1 for center **/
    public int x = -1;
    /** The x coordinate of the application, -1 for center **/
    public int y = -1;
    /** Whether to enable fullscreen **/
    public boolean fullscreen = false;
    /** Whether to enable HDPI mode on Mac OS X **/
    public boolean useHDPI = true;
    /**
     * Whether to enable vsync
     *
     * This value can be changed at runtime via {@link Graphics#setVSync(boolean)}
     */
    public boolean vSyncEnabled = true;
    /** The number of samples for MSAA **/
    public int samples = 2;
    /**
     * Whether to call System.exit() on tear-down.
     *
     * This is needed for Webstarts on some versions of Mac OS X it seems
     */
    public boolean forceExit = false;
    /** Whether the window is resizable **/
    public boolean resizable = true;
    /** Whether to use audio in this application */
    public boolean useAudio = true;
    /** The maximum number of sources that can be played simultaneously */
    public int audioDeviceSimultaneousSources = 24;
    /** The audio device buffer size in samples **/
    public int audioDeviceBufferSize = 512;
    /** The audio device buffer count **/
    public int audioDeviceBufferCount = 9;
    /** The initial background color */
    public Color initialBackgroundColor = Color.BLACK;
    /**
     * The target framerate when the window is in the foreground.
     *
     * The CPU sleeps as needed. Use 0 to never sleep.
     */
    public int foregroundFPS = 60;
    /**
     * The target framerate when the window is not in the foreground.
     *
     * The CPU sleeps as needed. Use 0 to never sleep, -1 to not render.
     */
    public int backgroundFPS = 60;
    /**
     * Do not render when the window is minimized.
     *
     * The app will also call {@link LifecycleListener#pause()} any lifecycle listener when minimized.
     */
    public boolean pauseWhenMinimized = true;
    /**
     * Pauses any lifecycle listeners when the window is not minimized but not in the foreground.
     *
     * This will call {@link LifecycleListener#pause()} any lifecycle listener when the window moves the background.
     * However, it will still render. To stop rendering when not the foreground window, use backgroundFPS -1.
     */
    public boolean pauseWhenBackground = false;

    /**
     * Returns the equivalent {@link LwjglApplicationConfiguration} for this {@link GDXApp} configuration.
     *
     * @return the equivalent {@link LwjglApplicationConfiguration} for this {@link GDXApp} configuration.
     */
    LwjglApplicationConfiguration getLwjglConfiguration() {
        LwjglApplicationConfiguration config = new LwjglApplicationConfiguration();
        config.title = title;
        config.width = width;
        config.height = height;
        config.x = x;
        config.y = y;
        config.fullscreen = fullscreen;
        config.vSyncEnabled = vSyncEnabled;
        config.useHDPI = useHDPI;
        config.samples = samples;
        config.useGL30 = false; //true;
        config.forceExit = forceExit;
        config.resizable = resizable;
        config.audioDeviceBufferCount = audioDeviceBufferCount;
        config.audioDeviceBufferSize  = audioDeviceBufferSize;
        config.audioDeviceSimultaneousSources = audioDeviceSimultaneousSources;
        config.initialBackgroundColor = initialBackgroundColor;
        config.foregroundFPS = foregroundFPS;
        config.backgroundFPS = backgroundFPS;
        config.pauseWhenBackground = pauseWhenBackground;
        config.pauseWhenMinimized = pauseWhenMinimized;
        config.disableAudio = false;
        return config;
    }
}