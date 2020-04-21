package edu.cornell.gdiac.tempus.tempus.models;

import edu.cornell.gdiac.tempus.GameCanvas;
import edu.cornell.gdiac.tempus.tempus.LevelController;
import edu.cornell.gdiac.util.ScreenListener;

import java.util.logging.Level;

public class LevelModel {

    /** GDXRoot screen listener instance */
    private ScreenListener listener;
    /** GameCanvas instance */
    private GameCanvas canvas;
    /** corresponds to the int ordinal of the level */
    private int level_number;
    /** true if the level has been unlocked */
    private boolean level_unlocked;
    /** true if player has cleared the entire level **/
    private boolean level_finished;
    /** index of current room */
    private int current_room_idx;
    /** pointer to current room */
    private LevelController current_room;
    /** list of rooms */
    private LevelController[] rooms;

    public LevelModel(int lv, boolean unlocked, boolean finished, int resume, LevelController[] rms){
        level_number = lv;
        level_unlocked = unlocked;
        level_finished = finished;
        current_room_idx = resume;
        rooms = rms;
        current_room = rooms[current_room_idx];
        listener = null;
        canvas = null;
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

}
