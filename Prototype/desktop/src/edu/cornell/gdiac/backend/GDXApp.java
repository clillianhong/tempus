/*
 * GDXApp.java
 *
 * This is a replacement for LwjglApplication. It allows us to force GL30.  Originally we used this class
 * to force our new audio engine (and we hope to do that again someday), but access issues forced us to
 * place a proxy class in com.badlogic.gdx.backends.lwjgl.
 *
 * @author Walker M. White
 * @data April 10, 2020
 */
package edu.cornell.gdiac.backend;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Audio;
import com.badlogic.gdx.backends.lwjgl.*;
import com.badlogic.gdx.backends.lwjgl.audio.OpenALAudio;

/**
 * This class represents a desktop game application using Lwjgl.
 *
 * This class is preferable to {@link LwjglApplication} because it is guaranteed to use GL30, and it has
 * a slightly more useful audio engine.  It extends {@link LwjglProxy} because we needed to access some
 * package internals.
 */
public class GDXApp extends LwjglProxy {
    /** Settings, remembered for second initialization phase */
    protected GDXAppSettings config;

    /** The replacement audio engine */
    protected GDXAudio engine;

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
     * Return the settings for this application
     */
    public GDXAppSettings getSettings() {
        return config;
    }

    /**
     * Encapsulate audio initialization so it can be overridden.
     */
    @Override
    protected void initAudio(LwjglApplicationConfiguration config) {
        if (!LwjglApplicationConfiguration.disableAudio) {
            try {
                // Piece of sh*t OpenAL only guarantees 16 sources, so we need to be guilty
                audio = new OpenALAudio(config.audioDeviceSimultaneousSources,
                        config.audioDeviceBufferCount,
                        config.audioDeviceBufferSize);
            } catch (Throwable t) {
                log("GDXApp", "Couldn't initialize audio; disabling audio", t);
                LwjglApplicationConfiguration.disableAudio = true;
            }
        }

        boolean fallback = false;
        if (!LwjglApplicationConfiguration.disableAudio) {
            try {
                // No that we have initialized OpenAL, steel its resources
                engine = new GDXAudio(config.audioDeviceSimultaneousSources,
                        config.audioDeviceBufferCount,
                        config.audioDeviceBufferSize);
            } catch (Throwable t) {
                log("GDXApp", "Couldn't initialize secondary audio; falling back", t);
                engine = null;
                fallback = true;
            }
        }

        if (fallback) {
            try {
                // Dispose of the audio engine and reinitialize
                if (audio != null) { audio.dispose(); }
                audio = new OpenALAudio(config.audioDeviceSimultaneousSources,
                        config.audioDeviceBufferCount,
                        config.audioDeviceBufferSize);
            } catch (Throwable t) {
                log("LwjglApplication", "Couldn't reinitialize audio; disabling audio", t);
                LwjglApplicationConfiguration.disableAudio = true;
            }
        }
    }

    /**
     * Disposes the audio on cleanup.
     */
    @Override
    protected void disposeAudio() {
        if (engine != null) { engine.dispose(); }
        if (audio != null) { audio.dispose(); }
    }

    /**
     * Return the active audio engine.
     */
    @Override
    protected Audio chooseAudio() {
        if (engine != null) {
            return engine;
        } else if (audio != null) {
            return audio;
        }
        return null;
    }

    /**
     * Updates the audio loop for any PCM buffering
     */
    @Override
    protected void updateAudio() {
        if (engine != null) {
            engine.update();
        } else if (audio != null) {
            audio.update();
        }
    }

}