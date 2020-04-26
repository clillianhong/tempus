package edu.cornell.gdiac.util;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import edu.cornell.gdiac.tempus.GameCanvas;
import edu.cornell.gdiac.tempus.tempus.LevelController;
import edu.cornell.gdiac.tempus.tempus.models.LevelModel;

/**
 * Responsible for managing the loading of an entire level from JSON
 *
 *
 * Lillian Hong, 4/20/2020
 *
 */
public class GameStateManager {

    /** GDXRoot screen listener instance */
    private static ScreenListener listener;
    /** GameCanvas instance */
    private static GameCanvas canvas;
    /** The singleton level loader (for easy access) */
    private static GameStateManager levelloader;
    /** The reader to process JSON files */
    private JsonReader jsonReader;
    /** The player progress JSON*/
    private JsonValue gameDirectory;
    /** List of Level JSONs*/
    private JsonValue[] levelDirectories;
    /** List of all Levels in game */
    private LevelModel[] levels;
    /** Current level **/
    private LevelModel currentLevel;
    /** current level index **/
    private int current_level_idx;
    /** index of last level **/
    private int last_level_idx;

    public GameStateManager(){
        listener = null;
        canvas = null;
        currentLevel = null;
        current_level_idx = 0;
        last_level_idx = 3;
        levelloader = this;
    }

    public void setCanvas(GameCanvas canvas){
        this.canvas = canvas;
    }

    public void setListener(ScreenListener listener){
        this.listener = listener;
    }

    /**
     * Returns a reference to the singleton level loader
     *
     * If there is no active level loader, this method will initialize
     * one immediately.
     *
     * @return a reference to the singleton level loader
     */
    public static GameStateManager getInstance() {
        if (levelloader == null) {
            levelloader = new GameStateManager();
        }
        return levelloader;
    }

    /**
     * Returns the JSON key for a given asset type
     *
     * @param type the asset type
     *
     * @return the JSON key for a given asset type
     */
    private <T> String getClassIdentifier(Class<T> type) {
        if (type.equals(Texture.class) || type.equals(TextureRegion.class)) {
            return "textures";
        } else if (type.equals(BitmapFont.class)) {
            return "fonts";
        } else if (type.equals(Sound.class)) {
            return "sounds";
        } else if (type.equals(FilmStrip.class)){
            return "filmstrips";
        }
        // Should never reach here
        assert false : "JSON directory does not support this assets class";
        return null;
    }


    /**
     * Loads the game state from json.
     * @param game_state_json
     * @return generated LevelModel
     */
    public void loadGameState(String game_state_json){

        jsonReader = new JsonReader();
        gameDirectory = jsonReader.parse(Gdx.files.internal(game_state_json));

        //parsing game_state json
        int highest_level_unlocked = gameDirectory.getInt("highest_level");
        int room_unlocked = gameDirectory.getInt("room_unlocked");
        int num_levels = gameDirectory.getInt("num_levels");
        String[] level_paths = gameDirectory.get("level_jsons").asStringArray();
        levelDirectories = new JsonValue[num_levels];
        levels = new LevelModel[num_levels];
        last_level_idx = levels.length-1;

        for(int i = 0; i<num_levels; i++){
            levelDirectories[i] = jsonReader.parse(Gdx.files.internal(level_paths[i]));
            levels[i] = loadLevel(levelDirectories[i]);
            levels[i].preloadLevel();
        }
        currentLevel = levels[1];
    }

    /**
     * Fully creates all levels during the loading period.
     */
    public void readyLevels(){
        for(LevelModel level: levels){
            level.setCanvas(canvas);
            level.setListener(listener);
            level.createLevel();
        }
    }

    /**
     * Loads a level model from JSON.
     *
     * @param levelJson
     * @return LevelModel generated from levelJson
     */
    protected LevelModel loadLevel(JsonValue levelJson){
        int lv = levelJson.getInt("level");
        int room_count = levelJson.getInt("room_count");
        boolean unlocked = levelJson.getBoolean("unlocked");
        boolean finished = levelJson.getBoolean("finished");
        int resume_room_index = levelJson.getInt("resume_room_index");
        String[] room_paths = levelJson.get("rooms").asStringArray();

        LevelController [] rooms = new LevelController[room_count];

        for(int i=0; i<room_count; i++){
            rooms[i] = new LevelController(room_paths[i]);
        }

        return new LevelModel(lv, unlocked, finished, resume_room_index, rooms);
    }

    /**
     *
     * @return the Screen to the current room being played
     */
    public LevelController getCurrentRoom(){
        return currentLevel.getCurrentRoom();
    }

    /**
     * Method called to step game progress, which involves either:
     * 1. Moving to the next room
     * 2. Finishing last room in level and finishing level/moving on to next level
     * 3. Finishing the game
     */
    public void stepGame(boolean is_exit){
        if(!is_exit){
            boolean level_finished = currentLevel.stepLevel();

            if(level_finished){ // LEVEL HAS FINISHED
                //TODO: Finish level announcement/screen
                currentLevel.finishLevel();
                if(current_level_idx == last_level_idx){
                    endGameState(); //TODO: end game state accouncement/screen
                }
                else{
                    current_level_idx++;
                    currentLevel = levels[current_level_idx];
                    currentLevel.unlockLevel();
                    //TODO: LEVEL FINISH SCREEN
                }
            }
        }else{
            currentLevel.getCurrentRoom().stopMusic();
        }
    }

    public void printGameState(){
        System.out.println("Current Level: " + currentLevel.getLevelNumber());
        System.out.println("Current Room: " + currentLevel.getCurrentRoomNumber());
        System.out.println("--------- LEVELS--------");
        for(LevelModel l : levels){
            System.out.println("Level: " + l.getLevelNumber() + "| Unlocked: " + l.isUnlocked());
        }

    }

    /**
     * Returns the current Level
     * @return current LevelModel
     */
    public LevelModel getCurrentLevel(){
        return currentLevel;
    }

    /**
     * Sets the current Level
     * @return current LevelModel
     */
    public void setCurrentLevel(int idx){
        current_level_idx = idx;
        currentLevel = levels[current_level_idx];
    }

    /**
     *
     * @param idx
     * @return the level at index idx
     */
    public LevelModel getLevel(int idx){
        return levels[idx];
    }

    /**
     * Player beat the whole game!
     */
    public void endGameState(){
        current_level_idx = 0;
        currentLevel = levels[current_level_idx];
    }


    /**
     * Saves the whole game state.
     */
    public void saveGameState(){
        //update 
    }

}
