package edu.cornell.gdiac.tempus;


import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.IdentityMap;
import com.badlogic.gdx.utils.ObjectMap;
import edu.cornell.gdiac.assets.*;
import edu.cornell.gdiac.audio.*;
import edu.cornell.gdiac.util.*;

public class MusicController {

    /**
     * Inner class to track and active sound instance
     *
     * A sound instance is a Music object and a number.  That is because
     * a single Music object may have multiple instances.  We do not
     * know when a sound ends.  Therefore, we simply let the sound go
     * and we garbage collect when the lifespace is greater than the
     * sound limit.
     */
    private class ActiveMusic {
        /** Reference to the sound resource */
        public MusicBuffer music;
        /** The id number representing the sound instance */
        public long  id;
        /** Is the sound looping (so no garbage collection) */
        public boolean loop;
        /** How long this sound has been running */
        public long lifespan;

        /**
         * Creates a new active sound with the given values
         *
         * @param m	Reference to the sound resource
         * @param b Is the sound looping (so no garbage collection)
         */
        public ActiveMusic(MusicBuffer m, boolean b) {
            music = m;
            loop = b;
            lifespan = 0;
        }
    }


    /** The singleton Music controller instance */
    private static MusicController controller;

    /** Keeps track of all of the allocated sound resources */
    private ObjectMap<String, MusicBuffer> musicbank;
    /** Reverse look up of source files */
    private IdentityMap<MusicBuffer,String> musicsrc;
    /** Keeps track of all of the "active" sounds */
    private ObjectMap<String, MusicController.ActiveMusic> actives;
    /** Support class for garbage collection */
    private Array<String> collection;

    private boolean shifted;


    /**
     * Creates a new MusicController with the default settings.
     */
    private MusicController() {
        musicbank = new ObjectMap<String, MusicBuffer>();
        musicsrc = new IdentityMap<MusicBuffer,String>();
        actives = new ObjectMap<String, ActiveMusic>();
        collection = new Array<String>();
        shifted = false;
    }

    /**
     * Returns the single instance for the MusicController
     *
     * The first time this is called, it will construct the MusicController.
     *
     * @return the single instance for the MusicController
     */
    public static MusicController getInstance() {
        if (controller == null) {
            controller = new MusicController();
        }
        return controller;
    }

    /// Music Management
    /**
     * Uses the asset manager to allocate a sound
     *
     * All sound assets are managed internally by the controller.  Do not try
     * to access the sound directly.  Use the play and stop methods instead.
     *
     * @param manager  A reference to the asset manager loading the sound
     * @param filename The filename for the sound asset
     */
    public void allocate(AssetManager manager, String filename) {
        MusicBuffer music = (MusicBuffer) manager.get(filename, Music.class);
        musicbank.put(filename, music);
        musicsrc.put(music,filename);
    }

    /**Deallocate all sounds*/
    public void deallocate(AssetManager manager, String filename) {
        MusicBuffer music = (MusicBuffer) manager.get(filename,Music.class);
        musicbank.remove(filename);
        musicsrc.remove(music);
    }

    public String getSource(MusicBuffer music) {
        return musicsrc.get(music);
    }

    /**
     * Plays the an instance of the given sound
     *
     * A sound is identified by its filename.  You can have multiple instances of the
     * same sound playing.  You use the key to identify a sound instance.  You can only
     * have one key playing at a time.  If a key is in use, the existing sound may
     * be garbage collected to allow you to reuse it, depending on the settings.
     *
     * However, it is also possible that the key use may fail.  In the latter case,
     * this method returns false.  In addition, if the sound is currently looping,
     * then this method will return true but will not stop and restart the sound.
     *
     *
     * @param key		The identifier for this sound instance
     * @param filename	The filename of the sound asset
     * @param loop		Whether to loop the sound
     *
     * @return True if the sound was successfully played
     */
    public boolean play(String key, String filename, boolean loop) {
        return play(key,filename,loop,1.0f);
    }


    /**
     * Plays the an instance of the given sound
     *
     * A sound is identified by its filename.  You can have multiple instances of the
     * same sound playing.  You use the key to identify a sound instance.  You can only
     * have one key playing at a time.  If a key is in use, the existing sound may
     * be garbage collected to allow you to reuse it, depending on the settings.
     *
     * However, it is also possible that the key use may fail.  In the latter case,
     * this method returns false.  In addition, if the sound is currently looping,
     * then this method will return true but will not stop and restart the sound.
     *
     *
     * @param key		The identifier for this sound instance
     * @param filename	The filename of the sound asset
     * @param loop		Whether to loop the sound
     * @param volume	The sound volume in the range [0,1]
     *
     * @return True if the sound was successfully played
     */
    public boolean play(String key, String filename, boolean loop, float volume) {
        // Get the sound for the file
//		System.out.println(musicbank.containsKey(filename));
        if (!musicbank.containsKey(filename)) {
            return false;
        }

        // If there is a sound for this key, stop it
        MusicBuffer music = musicbank.get(filename);
        if (actives.containsKey(key)) {
            MusicController.ActiveMusic snd = actives.get(key);
            if (!snd.loop) {
                // This is a workaround for the OS X sound bug
                //snd.sound.stop(snd.id);
                snd.music.setVolume(0);
            } else {
                return true;
            }
        }

        // Play the new sound and add it
        music.setVolume(volume);
        music.play();
        if (loop) {
            music.setLooping(true);
        }

        actives.put(key,new MusicController.ActiveMusic(music, loop));
        return true;
    }

    /**
     * Stops the sound, allowing its key to be reused.
     *
     * This is the only way to stop a sound on a loop.  Otherwise it will
     * play forever.
     *
     * If there is no sound instance for the key, this method does nothing.
     *
     * @param key	The sound instance to stop.
     */
    public void stop(String key) {
        // Get the active sound for the key
        if (!actives.containsKey(key)) {
            return;
        }

        MusicController.ActiveMusic snd = actives.get(key);

        // This is a workaround for the OS X sound bug
        //snd.sound.stop(snd.id);
        snd.music.setLooping(false); // Will eventually garbage collect
        snd.music.setVolume(0.0f);
        actives.remove(key);
    }

    public void stopAll() {
        for (String m : actives.keys()){
            MusicBuffer music = actives.get(m).music;

            music.stop();
            music.setLooping(false);
            music.setVolume(0.0f);
            actives.remove(m);
        }
    }

    /**
     * Returns true if the sound instance is currently active
     *
     * @param key	The sound instance identifier
     *
     * @return true if the sound instance is currently active
     */
    public boolean isActive(String key) {
        return actives.containsKey(key);
    }

    /**
     * Updates the current frame of the sound controller.
     *
     * This method serves two purposes.  First, it allows us to limit the number
     * of sounds per animation frame.  In addition it allows us some primitive
     * garbage collection.
     */
    public void update(boolean shifted) {

        MusicBuffer present_music = actives.get("present").music;
        MusicBuffer past_music = actives.get("past").music;
        float presVol = present_music.getVolume();
        float pastVol = past_music.getVolume();
        float crossfade = 0.05f;
        if (shifted) {
            //present_music.setVolume(0);
            //past_music.setVolume(1);
            if (presVol > 0){
                present_music.setVolume(Math.max(0, presVol - crossfade));
            }
            if (pastVol < 1){
                past_music.setVolume(Math.min(1, pastVol + crossfade));
            }
        } else {
            //present_music.setVolume(1);
            //past_music.setVolume(0);
            if (presVol < 1){
                present_music.setVolume(Math.max(1, presVol + crossfade));
            }
            if (pastVol > 0){
                past_music.setVolume(Math.min(0, pastVol - crossfade));
            }
        }

        for(String key : actives.keys()) {
            MusicController.ActiveMusic snd = actives.get(key);
            snd.lifespan++;
        }
        for(String key : collection) {
            actives.remove(key);
        }
        collection.clear();
    }
}
