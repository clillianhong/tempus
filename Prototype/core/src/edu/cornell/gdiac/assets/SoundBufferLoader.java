/*
 * SoundBufferLoader.java
 *
 * This is a simple loader for processing sound buffers (and making them assets managed
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
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.badlogic.gdx.utils.Array;
import edu.cornell.gdiac.audio.*;

/**
 * This class is an {@link AssetLoader} to load {@link SoundBuffer} assets.
 *
 * A sound buffer asset should be specified by filename:name where name is a unique
 * name for the buffer.
 */
public class SoundBufferLoader extends AsynchronousAssetLoader<SoundBuffer, SoundBufferLoader.SoundBufferParameters> {
    /** A reference to the file handle resolver (inaccessible in parent class) */
    protected FileHandleResolver resolver;
    /** The asynchronously read SoundBuffer */
    private SoundBuffer cachedBuffer;

    /**
     * The definable parameters for a {@link SoundBuffer}.
     * 
     * A sound buffer is derived from an audio source. It is simply an audio source
     * that is actively attached to the audio engine.
     */ 
	static public class SoundBufferParameters extends AssetLoaderParameters<SoundBuffer> {
        /** The reference to the audio source in the asset manager */
        public String source;

        /**
         * Creates sound buffer parameters for the give audio source.
         *
         * @param fileName    The file for the parent audio source.
         */
        public SoundBufferParameters(String fileName) {
            this.source = fileName;
        }
    }

    /**
     * Creates a new SoundBufferLoader with an internal file resolver
     */
    public SoundBufferLoader() {
        this(new InternalFileHandleResolver());
    }

    /**
     * Creates a new SoundBufferLoader with the given file resolver
     *
     * @param resolver    The file resolver
     */
    public SoundBufferLoader (FileHandleResolver resolver) {
        super(resolver);
        this.resolver = resolver;
    }

    /** 
     * Returns the {@link SoundBuffer} instance currently loaded by this loader.
     *
     * If nothing has been loaded, this returns {@code null}.
     *
     * @return the {@link SoundBuffer} instance currently loaded by this loader.
     */
    protected SoundBuffer getLoadedSound () {
        return cachedBuffer;
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
    public void loadAsync (AssetManager manager, String fileName, FileHandle file, SoundBufferParameters params) {
        AudioSource source = manager.get(manager.getDependencies(fileName).first(),AudioSource.class);
        cachedBuffer = ((AudioEngine)Gdx.audio).newSoundBuffer(source);
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
    public SoundBuffer loadSync (AssetManager manager, String fileName, FileHandle file, SoundBufferParameters params) {
        SoundBuffer sound = cachedBuffer;
        cachedBuffer = null;
        return sound;
    }

    /**
     * Resolves the file for this sound buffer.
     *
     * A texture region asset should be specified by filename:name where name is a unique
     * name for the sound asset.
     *
     * @param fileName  The file name to resolve
     *
     * @return handle to the file, as resolved by the file resolver.
     */
    @Override
    public FileHandle resolve (String fileName) {
        int suffix = fileName.lastIndexOf(':');
        if (suffix == -1) {
            throw new GdxRuntimeException( "Sound buffer file name must end in ':alias'." );
        }
        String prefix = fileName.substring( 0,suffix );
        return resolver.resolve(prefix);
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
    public Array<AssetDescriptor> getDependencies (String fileName, FileHandle file, SoundBufferParameters params) {
        if (params == null) {
            int suffix = fileName.lastIndexOf(':');
            String prefix = (suffix == -1) ? fileName : fileName.substring( 0,suffix );
            params = new SoundBufferLoader.SoundBufferParameters( prefix );
        }
        Array<AssetDescriptor> deps = new Array<AssetDescriptor>(1);
        deps.add(new AssetDescriptor<AudioSource>( params.source, AudioSource.class));
        return deps;
    }

}  
