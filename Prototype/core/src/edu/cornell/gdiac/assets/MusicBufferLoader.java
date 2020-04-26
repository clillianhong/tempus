/*
 * MusicBufferLoader.java
 *
 * This is a simple loader for processing music buffers (and making them assets managed
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
 * This class is an {@link AssetLoader} to load {@link SoundBuffer} assets.
 *
 * All music buffers are named symbolically, since that may span multiple audio sources.
 * They are fully defined by their loader parameters.
 */
public class MusicBufferLoader extends AsynchronousAssetLoader<MusicBuffer, MusicBufferLoader.MusicBufferParameters> {
    /** The asynchronously read MusicBuffer */
    private MusicBuffer cachedBuffer;
    
    /**
     * The definable parameters for a {@link SoundBuffer}.
     * 
     * While a music buffer may be derived from an audio source, it can be empty so long
     * as the number of channels (mono/stereo) and sample rate is defined.  Set sampleRate
     * to -1 to use the sample rate of the first source.
     */ 
    public static  class MusicBufferParameters extends AssetLoaderParameters<MusicBuffer> {
        /** The sources pre-attached to this music buffer (may be empty) */
        public Array<String> sources;
        /** 
         * Whether the audio sources in this buffer are required to be mono.
         * 
         * If sources is not empty, this value will be ignored and the buffer will 
         * use the setting of the first audio source.
         */
        public boolean isMono;
        /** 
         * The samples per second of all audio sources
         * 
         * If sources is not empty, this value will be ignored and the buffer will 
         * use the setting of the first audio source.
         */
        public int sampleRate;
        /** The initial volume in range [0,1] */
        public float volume;
        /** The pitch multiplier volume in range [0.5,2.0] */
        public float pitch;
        /** The stereo pan in range [-1,1] (only valid on mono sources) */
        public float panning;
        /** Whether to loop this music buffer */
        public boolean looping;
        /** Whether to use a local loop behavior (loop one source only) */
        public boolean shortLoop;

        /**
         * Creates music buffer parameters for stereo CD sound.
         *
         * There will be no sources.  isMono is false and sampleRate is 44.1k.
         */
        public MusicBufferParameters() {
            this(false,44100);
        }

        /**
         * Creates music buffer parameters for a single audio source
         *
         * @param fileName  The file for the parent audio source.
         */
        public MusicBufferParameters(String fileName) {
            this(false,0);
            sources.add(fileName);
        }

        /**
         * Creates music buffer parameters for the given stream settings.
         *
         * There will be no initial sources.
         *
         * @param isMono        Whether this music buffer is a mono audio stream
         * @param sampleRate    The number of samples per second
         */
        public MusicBufferParameters(boolean isMono, int sampleRate) {
            this.isMono = isMono;
            this.sampleRate = sampleRate;
            sources = new Array<String>();
            volume = 1.0f;
            pitch = 1.0f;
            panning = 0.0f;
            looping = false;
            shortLoop = false;
        }

    }

    /**
     * Creates a new MusicBufferLoader with an internal file resolver
     */
    public MusicBufferLoader() {
        this(new InternalFileHandleResolver());
    }

    /**
     * Creates a new MusicBufferLoader with the given file resolver
     *
     * @param resolver    The file resolver
     */
    public MusicBufferLoader (FileHandleResolver resolver) {
        super(resolver);
    }

    /** 
     * Returns the {@link MusicBuffer} instance currently loaded by this loader.
     *
     * If nothing has been loaded, this returns {@code null}.
     *
     * @return the {@link MusicBuffer} instance currently loaded by this loader.
     */
    protected MusicBuffer getLoadedMusic () {
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
    public void loadAsync (AssetManager manager, String fileName, FileHandle file, MusicBufferParameters params) {
        if (params == null) {
            params = new MusicBufferParameters();
        }
        
        if (params.sources.size == 0) {
            cachedBuffer = ((AudioEngine)Gdx.audio).newMusicBuffer(params.isMono,params.sampleRate);
        } else {
            AudioSource first = manager.get(manager.getDependencies(fileName).first(), AudioSource.class);
            cachedBuffer = ((AudioEngine)Gdx.audio).newMusicBuffer(first.getChannels() == 1, first.getSampleRate());
        }
        cachedBuffer.setVolume( params.volume );
        cachedBuffer.setPitch( params.pitch );
        cachedBuffer.setPan( params.panning );
        cachedBuffer.setLooping( params.looping );
        cachedBuffer.setLoopBehavior( params.shortLoop );
        for(String deps : manager.getDependencies(fileName)) {
            cachedBuffer.addSource( manager.get(deps,AudioSource.class) );
        }
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
    public MusicBuffer loadSync (AssetManager manager, String fileName, FileHandle file, MusicBufferParameters params) {
        MusicBuffer music = cachedBuffer;
        cachedBuffer = null;
        return music;
    }

    /**
     * Eliminate the file resolution as all file names are logical.
     *
     * @param fileName    Pointless
     */
    @Override
    public FileHandle resolve (String fileName) {
        return null;
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
    public Array<AssetDescriptor> getDependencies (String fileName, FileHandle file, MusicBufferParameters params) {
        if (params == null) {
            params = new MusicBufferParameters();
        }
        Array<AssetDescriptor> deps = new Array<AssetDescriptor>(params.sources.size);
        for(String name : params.sources) {
            deps.add( new AssetDescriptor<AudioSource>( name, AudioSource.class ) );
        }
        return deps;
    }

}
