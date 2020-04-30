package edu.cornell.gdiac.tempus.tempus.models;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import edu.cornell.gdiac.tempus.GameCanvas;
import edu.cornell.gdiac.tempus.MusicController;
import edu.cornell.gdiac.tempus.tempus.LevelController;
import edu.cornell.gdiac.util.ScreenListener;

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



    public LevelModel(int lv, boolean unlocked, boolean finished, int resume, LevelController[] rms){
        level_number = lv;
        level_unlocked = unlocked;
        level_finished = finished;
        current_room_idx = resume;
        rooms = rms;
        current_room = rooms[current_room_idx];
        listener = null;
        canvas = null;
        jsonReader = new JsonReader();
        assetDirectory = jsonReader.parse(Gdx.files.internal("jsons/assets.json"));
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
        current_room_idx = current_room_idx + 1;
        if(current_room_idx == rooms.length){
            //TODO: ROOM END SCREEN
            //current_room.stopMusic();
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
    }

    public void lockLevel(){
        level_unlocked = false;
    }

    public void finishLevel(){
        level_finished = true;
        current_room_idx = 0;
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
