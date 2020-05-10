package edu.cornell.gdiac.tempus.tempus.models;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import edu.cornell.gdiac.tempus.GameCanvas;
import edu.cornell.gdiac.tempus.MusicController;
import edu.cornell.gdiac.tempus.tempus.LevelController;
import edu.cornell.gdiac.util.ScreenListener;

import java.util.Arrays;
import java.util.logging.Level;

public class LevelModel {

    /** GDXRoot screen listener instance */
    protected ScreenListener listener;
    /** GameCanvas instance */
    protected GameCanvas canvas;
    /** corresponds to the int ordinal of the level */
    protected int level_number;
    /** true if the level has been unlocked */
    protected boolean level_unlocked;
    /** true if player has cleared the entire level **/
    protected boolean level_finished;
    /** index of current room */
    protected int current_room_idx;
    /** pointer to current room */
    protected LevelController current_room;
    /** list of rooms */
    protected LevelController[] rooms;

    protected JsonReader jsonReader;

    protected JsonValue assetDirectory;

    protected int highest_room_unlocked;

    /**The best time on the level*/
    protected float [] bestTime;

    public LevelModel(int lv, boolean unlocked, boolean finished, int resume, LevelController[] rms){
        level_number = lv;
        level_unlocked = unlocked;
        level_finished = finished;
        if(finished){
            highest_room_unlocked = rms.length-1;
        }else{
            highest_room_unlocked = resume;
        }
        current_room_idx = resume;
        rooms = rms;
        current_room = rooms[current_room_idx];
        listener = null;
        canvas = null;
        //Initializes bestTime for this level.
        bestTime = new float [rms.length];
        Arrays.fill(bestTime, Float.MAX_VALUE);

        jsonReader = new JsonReader();
        assetDirectory = jsonReader.parse(Gdx.files.internal("jsons/assets.json"));
    }

    /**
     * Updates the best time for the room in this level
     * @param room_idx the index of the room to be updated
     */
    public void updateBestTime(int room_idx){
        float time = rooms[room_idx].getTimer();
        if (time<bestTime[room_idx]){bestTime[room_idx]=time;}
        rooms[room_idx].resetTimer();
    }

    /**
     * Gets the best time of this level
     * @return The best time of this level
     */
    public float[] getBestTime(){return bestTime;}

    /**
     * Sets the best time of the level to the loaded time
     * @param time the level times to be set
     *
     */
    public void setBestTime(float [] time){bestTime = time;}

    public int getHighestUnlockedRoom(){
        return highest_room_unlocked;
    }

    public int getRoomCount(){
        return rooms.length;
    }
    public void preloadLevel(){
        for(LevelController rc: rooms ) {
            rc.preLoadContent();
        }
    }

    public void setCanvas(GameCanvas canvas){
        this.canvas = canvas;
    }

    public void setListener(ScreenListener listener){
        this.listener = listener;
    }

    /** Handles preloading of all room controller content*/
    public void createLevel(){
        for(LevelController rc: rooms ) {
            rc.loadContent();
            rc.setScreenListener(listener);
            rc.setCanvas(canvas);
        }
    }

    /** Transitions to the next room
     * @returns whether or not the level has ended */
    public boolean stepLevel(){

        //Steps the room
        current_room_idx = current_room_idx + 1;
        highest_room_unlocked = Math.max(highest_room_unlocked, current_room_idx);
        if(current_room_idx == rooms.length){
            current_room_idx = 0;
            return true;
        }
        current_room = rooms[current_room_idx];
        return false;
    }

    public void resetLevel(){
        for(LevelController rc: rooms){
            rc.reset();
        }
    }
    public void endLevel(){
        for(LevelController rc: rooms){
            rc.dispose();
        }
    }

    public void unlockLevel(){
        level_unlocked = true;
        current_room_idx = 0;
        current_room = rooms[0];
    }

    public void lockLevel(){
        level_unlocked = false;
    }

    public void finishLevel(){
        level_finished = true;
        current_room_idx = 0;
        current_room = rooms[current_room_idx];
    }

    public void setCurrentRoom(int idx){
        current_room_idx = idx;
        current_room = rooms[current_room_idx];
    }
    public LevelController getCurrentRoom(){
        return current_room;
    }

    public int getLevelNumber(){
        return level_number;
    }

    public int getCurrentRoomNumber(){
        return current_room_idx;
    }

    public boolean isUnlocked(){
        return level_unlocked;
    }

    public boolean isFinished(){
        return level_finished;
    }

    public void playMusic(){
        String past = "past2";
        String present = "present2";
        if (level_number == 1) {
            past = "past1";
            present = "present1";
        } else if (level_number == 2) {
            past = "past2";
            present = "present2";
        } else if (level_number == 3) {
            past = "past3";
            present = "present3";
        } else if (level_number == 4) {
            past = "past4";
            present = "present4";
        }
        JsonValue pastMus = assetDirectory.get("music").get(past);
        MusicController.getInstance().play("past", pastMus.get("file").asString(), true, 0.0f);
        JsonValue presentMus = assetDirectory.get("music").get(present);
        MusicController.getInstance().play("present", presentMus.get("file").asString(), true, 1.0f);
    }

}
