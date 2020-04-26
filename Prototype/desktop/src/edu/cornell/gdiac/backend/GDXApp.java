/*
 * GDXApp.java
 *
 * This is a (necessary) replacement for LwjglApplication. It allows us to disable audio and then
 * re-enable it later. It also allows us to force GL30.
 *
 * @author Walker M. White
 * @data April 10, 2020
 */
package edu.cornell.gdiac.backend;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.backends.lwjgl.*;

/**
 * This class represents a desktop game application using Lwjgl.
 *
 * This class is preferable to {@link LwjglApplication} because it is guaranteed to use GL30, and it has
 * a slightly more useful audio engine.
 */
public class GDXApp extends LwjglApplication {
    /** Settings, remembered for second initialization phase */
    GDXAppSettings config;

    /**
     * Creates a game application with the given listener and settings.
     *
     * The application listener drives the primary game loop.  It should be a platform-independent class
     * in the core package.
     *
     * @param listener  The game driver
     * @param config    The game settings
     */
    public GDXApp(ApplicationListener listener, GDXAppSettings config) {
        super( listener, config.getLwjglConfiguration() );
        this.config = config;
    }

    /**
     * Invokes the main application loop.
     *
     * This is a replacement to {@link LwjglApplication#mainLoop()} that prepends it with an second
     * initialization phase.  This is the phase where we generate our audio engine.
     */
    @Override
    protected void mainLoop() {
        try {
            audio = new GDXAudio(config.audioDeviceSimultaneousSources,
                                 config.audioDeviceBufferCount,
                                 config.audioDeviceBufferSize);
            Gdx.audio = audio;
            LwjglApplicationConfiguration.disableAudio = false;
        } catch (Throwable t) {
            log("GDXApp", "Couldn't initialize audio, disabling audio", t);
            LwjglApplicationConfiguration.disableAudio = true;
            config.useAudio = false;
        }
        super.mainLoop();
    }

}