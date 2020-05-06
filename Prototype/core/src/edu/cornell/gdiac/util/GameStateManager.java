package edu.cornell.gdiac.util;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.JsonWriter;
import edu.cornell.gdiac.tempus.GameCanvas;
import edu.cornell.gdiac.tempus.MusicController;
import edu.cornell.gdiac.tempus.tempus.LevelController;
import edu.cornell.gdiac.tempus.tempus.TutorialController;
import edu.cornell.gdiac.tempus.tempus.models.LevelModel;
import edu.cornell.gdiac.tempus.tempus.models.ScreenExitCodes;
import edu.cornell.gdiac.tempus.tempus.models.TutorialModel;

import java.io.File;

/**
 * Responsible for managing the loading of an entire level from JSON
 *
 *
 * Lillian Hong, 4/20/2020
 *
 */
public class GameStateManager {

    private class GameState {
        public int highest_level;
        public int room_unlocked;
        public int num_levels;
        public String[] level_jsons;

        public GameState(int h, int r, int n, String [] lj){
            highest_level = h;
            room_unlocked = r;
            num_levels = n;
            level_jsons = lj;
        }

        public int getHighest_level() {
            return highest_level;
        }

        public void setHighest_level(int highest_level) {
            this.highest_level = highest_level;
        }

        public int getRoom_unlocked() {
            return room_unlocked;
        }

        public void setRoom_unlocked(int room_unlocked) {
            this.room_unlocked = room_unlocked;
        }

        public int getNum_levels() {
            return num_levels;
        }

        public void setNum_levels(int num_levels) {
            this.num_levels = num_levels;
        }

        public String[] getLevel_jsons() {
            return level_jsons;
        }

        public void setLevel_jsons(String[] level_jsons) {
            this.level_jsons = level_jsons;
        }
    }

    /** GDXRoot screen listener instance */
    private static ScreenListener listener;
    /** GameCanvas instance */
    private static GameCanvas canvas;
    /** GameState container class */
    private GameState gameState;
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
    /** Highest level unlocked this session **/
    private LevelModel highestUnlockedLevel;

    /** current level index **/
    private int current_level_idx;
    /** index of last level **/
    private int last_level_idx;

    public GameStateManager(){
        listener = null;
        canvas = null;
        currentLevel = null;
        gameState = null;
        highestUnlockedLevel = null;
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
     * true if the current room is the last room in the level
     */
    public boolean lastRoom(){
        return currentLevel.getCurrentRoomNumber() == (currentLevel.getRoomCount() - 1);
    }


    /**
     * Loads the game state from json.
     * @param game_state_json
     * @return generated LevelModel
     */
    public void loadGameState(String game_state_json){

        jsonReader = new JsonReader();
        //TODO: CHANGE THIS TO LOCAL (UNCOMMENT AND REPLACE LINE) FOR FINISHED VERSION
//        gameDirectory = jsonReader.parse(Gdx.files.local(game_state_json));

        gameDirectory = jsonReader.parse(Gdx.files.internal(game_state_json));

        //parsing game_state json
        int unfinishedLevel = gameDirectory.getInt("highest_level");
        int unfinishedRoom = gameDirectory.getInt("room_unlocked");
        int num_levels = gameDirectory.getInt("num_levels");
        String[] level_paths = gameDirectory.get("level_jsons").asStringArray();
        gameState = new GameState(unfinishedLevel, unfinishedRoom, num_levels, level_paths);
        levelDirectories = new JsonValue[num_levels];
        levels = new LevelModel[num_levels];
        last_level_idx = levels.length-1;

        levelDirectories[0] = jsonReader.parse(Gdx.files.internal(level_paths[0]));
//        System.out.println("GDX ERROR 0:" + Gdx.gl.glGetError());

        levels[0] = loadTutorial(levelDirectories[0]);
//        System.out.println("GDX ERROR 1 preload:" + Gdx.gl.glGetError());

        levels[0].preloadLevel();


        for(int i = 1; i<num_levels; i++){
//            System.out.println("GDX ERROR "+ (i+1) +": " + Gdx.gl.glGetError());

            levelDirectories[i] = jsonReader.parse(Gdx.files.internal(level_paths[i]));
            levels[i] = loadLevel(levelDirectories[i], unfinishedLevel, unfinishedRoom);
            if(levels[i].getLevelNumber() == unfinishedLevel){
                highestUnlockedLevel = levels[i];
            }
            levels[i].preloadLevel();
        }
        currentLevel = levels[1];
//        System.out.println("GDX ERROR end:" + Gdx.gl.glGetError());

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
    protected LevelModel loadLevel(JsonValue levelJson, int unfinishedLevel, int unfinishedRoom){
        int lv = levelJson.getInt("level");
        int room_count = levelJson.getInt("room_count");
        String[] room_paths = levelJson.get("rooms").asStringArray();

        LevelController [] rooms = new LevelController[room_count];

        for(int i=0; i<room_count; i++){
            rooms[i] = new LevelController(room_paths[i]);
        }

       if(lv == unfinishedLevel){
            return new LevelModel(lv, true, false, unfinishedRoom, rooms);
        }else if(lv > unfinishedLevel){
            return new LevelModel(lv, false, false, 0, rooms);
        }else{
            return new LevelModel(lv, true, true, 0, rooms);
        }

    }

    /**
     * Loads a level model from JSON.
     *
     * @param levelJson
     * @return LevelModel generated from levelJson
     */
    protected TutorialModel loadTutorial(JsonValue levelJson){
        int lv = levelJson.getInt("level");
        int room_count = levelJson.getInt("room_count");
        String[] room_paths = levelJson.get("rooms").asStringArray();

        LevelController [] rooms = new LevelController[room_count];

        for(int i=0; i<room_count; i++){
            rooms[i] = new TutorialController(room_paths[i]);
        }

       return new TutorialModel(lv, true, true, 0, rooms);

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
            boolean level_finished = currentLevel.stepLevel();

            if(level_finished){ // LEVEL HAS FINISHED
                //TODO: Finish level announcement/screen
                MusicController.getInstance().stopAll();
                currentLevel.finishLevel();
                if(current_level_idx == last_level_idx){
                    //endGameState(); //TODO: end game state accouncement/screen
                }
                else{
                    current_level_idx++;
                    currentLevel = levels[current_level_idx];
                    currentLevel.playMusic();
                    if(!currentLevel.isUnlocked()){
                        highestUnlockedLevel = currentLevel;
                        currentLevel.unlockLevel();
                    }
                    //TODO: LEVEL FINISH SCREEN
                }
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
    public boolean endGameState(){
        return current_level_idx == last_level_idx && currentLevel.getCurrentRoomNumber() == currentLevel.getRoomCount() - 1;
    }


    /**
     * Updates the whole game state in the directories
     */
    public void updateGameState(){
        gameState.setHighest_level(highestUnlockedLevel.getLevelNumber());
        gameState.setRoom_unlocked(highestUnlockedLevel.getCurrentRoomNumber());
    }


    /**
     * Saves the whole game state to game.json and level jsons
     */
    public void saveGameState(){
        FileHandle gamefile = Gdx.files.local("jsons/game.json");
        Json json=new Json(JsonWriter.OutputType.json);
        json.setWriter(gamefile.writer(false));
        json.setOutputType(JsonWriter.OutputType.json);
        gamefile.writeString(json.prettyPrint(gameState), false);
    }

}
