/*
 * AudioSourceLoader.java
 *
 * This is a simple loader for processing audio sources (and making them assets managed
 * by the asset manager.  This is required for using the new audio engine.
 *
 * This code is based on the template for SoundLoader by mzechner.
 *
 * @author Walker M. White
 * @data   04/20/2020
 */
package edu.cornell.gdiac.assets;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.*;
import com.badlogic.gdx.assets.loaders.*;
import com.badlogic.gdx.assets.loaders.resolvers.InternalFileHandleResolver;
import com.badlogic.gdx.files.FileHandle;
import edu.cornell.gdiac.audio.*;
import com.badlogic.gdx.utils.Array;

/**
 * This class is an {@link AssetLoader} to load {@link AudioSource} assets.
 */
public class AudioSourceLoader extends AsynchronousAssetLoader<AudioSource, AudioSourceLoader.AudioSourceParameters> {

    /** The asynchronously read audio source */
    private AudioSource cachedSource;
    
    /**
     * The definable parameters for an {@link AudioSource}.
     */
    static public class AudioSourceParameters extends AssetLoaderParameters<AudioSource> {
        // Since everything is defined in the file, nothing to do here
    }

    /**
     * Creates a new AudioSourceLoader with an internal file resolver
     */
    public AudioSourceLoader() {
        this(new InternalFileHandleResolver());
    }

    /**
     * Creates a new AudioSourceLoader with the given file resolver
     *
     * @param resolver    The file resolver
     */
    public AudioSourceLoader(FileHandleResolver resolver) {
        super(resolver);
    }

    /** 
     * Returns the {@link AudioSource} instance currently loaded by this loader.
     *
     * If nothing has been loaded, this returns {@code null}.
     *
     * @return the {@link AudioSource} instance currently loaded by this loader.
     */
    protected AudioSource getLoadedSource () {
        return cachedSource;
    }

    /** 
     * Loads thread-safe part of the asset and injects any dependencies into the AssetManager.
     *
     * This is used to load non-OpenGL parts of the asset that do not require the context
     * of the main thread.
     *
     * @param manager   The asset manager
     * @param fileName  The name of the asset to load
     * @param file      The resolved file to load
     * @param params    The parameters to use for loading the asset 
     */
    @Override
    public void loadAsync (AssetManager manager, String fileName, FileHandle file, AudioSourceParameters params) {
        cachedSource = ((AudioEngine)Gdx.audio).newSource(file);
    }

    /** 
     * Loads the main thread part of the asset.
     *
     * This is used to load OpenGL parts of the asset that require the context of the
     * main thread.
     *
     * @param manager   The asset manager
     * @param fileName  The name of the asset to load
     * @param file      The resolved file to load
     * @param params    The parameters to use for loading the asset 
     */
    @Override
    public AudioSource loadSync (AssetManager manager, String fileName, FileHandle file, AudioSourceParameters params) {
        AudioSource source = cachedSource;
        cachedSource = null;
        return source;
    }

    /** 
     * Returns the other assets this asset requires to be loaded first. 
     * 
     * This method may be called on a thread other than the GL thread. It may return
     * null if there are no dependencies.
     *
     * @param fileName  The name of the asset to load
     * @param file      The resolved file to load
     * @param params parameters for loading the asset
     *
     * @return the other assets this asset requires to be loaded first. 
     */
    @Override
    public Array<AssetDescriptor> getDependencies (String fileName, FileHandle file, AudioSourceParameters params) {
        return null;
    }



}