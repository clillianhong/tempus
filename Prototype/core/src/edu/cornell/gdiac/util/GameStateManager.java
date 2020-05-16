package edu.cornell.gdiac.util;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.*;
import edu.cornell.gdiac.tempus.GameCanvas;
import edu.cornell.gdiac.tempus.MusicController;
import edu.cornell.gdiac.tempus.tempus.LevelController;
import edu.cornell.gdiac.tempus.tempus.TutorialController;
import edu.cornell.gdiac.tempus.tempus.models.LevelModel;
import edu.cornell.gdiac.tempus.tempus.models.ScreenExitCodes;
import edu.cornell.gdiac.tempus.tempus.models.TutorialModel;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.HashMap;

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
        public int[] room_unlocked;
        public int num_levels;
        public String[] level_jsons;
        public ArrayList<float[]> gameTimes;

        public GameState(int h, int[] r, int n, String[] lj, ArrayList<float[]> times) {
            highest_level = h;
            room_unlocked = new int[n];
            for (int i = 0; i < n; i++) {
                room_unlocked[i] = r[i];
            }
            num_levels = n;
            level_jsons = lj;
            gameTimes = times;
        }
        public GameState(int h, int[] r, int n, String[] lj) {
            highest_level = h;
            room_unlocked = new int[n];
            for (int i = 0; i < n; i++) {
                room_unlocked[i] = r[i];
            }
            num_levels = n;
            level_jsons = lj;
            gameTimes = new ArrayList<>();
        }

        public int getHighest_level() {
            return highest_level;
        }

        public void setHighest_level(int highest_level) {
            this.highest_level = highest_level;
        }

        public int[] getRoom_unlocked() {
            return room_unlocked;
        }

        public void setRoom_unlocked(int i, int room_unlocked) {
            this.room_unlocked[i] = room_unlocked;
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

        /**
         * Updates the gameTimes with the level times
         * 
         * @param lvs the array of level models to load game time from
         */
        public void setGameTimes(LevelModel[] lvs) {
            gameTimes.clear();
            for (int i = 0; i < lvs.length; i++) {
                gameTimes.add(i, lvs[i].getBestTime());
            }
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
    /** The player progress JSON */
    private JsonValue gameDirectory;
    /** List of Level JSONs */
    private JsonValue[] levelDirectories;
    /** List of all Levels in game */
    private LevelModel[] levels;
    /** whether or not the game is completed */
    private boolean endGame;
    // /** Current level **/
    // private LevelModel currentLevel;
    /** Highest level unlocked this session **/
    private LevelModel highestUnlockedLevel;

    /** current level index **/
    private int current_level_idx;
    /** index of last level **/
    private int last_level_idx;

    public GameStateManager() {
        listener = null;
        canvas = null;
        // currentLevel = null;
        gameState = null;
        highestUnlockedLevel = null;
        current_level_idx = 0;
        last_level_idx = 3;
        levelloader = this;
        endGame = false;
    }

    public void setCanvas(GameCanvas canvas) {
        this.canvas = canvas;
    }

    public void setListener(ScreenListener listener) {
        this.listener = listener;
    }

    /**
     * Returns a reference to the singleton level loader
     *
     * If there is no active level loader, this method will initialize one
     * immediately.
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
        } else if (type.equals(FilmStrip.class)) {
            return "filmstrips";
        }
        // Should never reach here
        assert false : "JSON directory does not support this assets class";
        return null;
    }

    /**
     * true if the current room is the last room in the level
     */
    public boolean lastRoom() {
        return levels[current_level_idx].getCurrentRoomNumber() == (levels[current_level_idx].getRoomCount() - 1);
    }

    public int getCurrentLevelIndex(){
        return current_level_idx;
    }

    public GameState writeNewGameState(){
        FileHandle gamefile = Gdx.files.external("tempus/jsons/game.json");
        Json json = new Json(JsonWriter.OutputType.json);
        json.setWriter(gamefile.writer(false));
        json.setOutputType(JsonWriter.OutputType.json);
        int unfinishedLevel = 3;
        int[] unfinishedRoom = {20, 14, 14, 9};
        int num_levels = 4;

        String[] level_paths = {
                "jsons/levels/tutorial.json",
        "jsons/levels/level_1.json",
                "jsons/levels/level_2.json",
                "jsons/levels/level_3.json"};
        ArrayList<float []> timelist = new ArrayList<>();
        //TODO: MAKE THIS DYNAMIC
        float [] t0 = {100f,100f,100f,100f,100f,
                100f,100f,100f,100f,100f,
                100f,100f,100f,100f,100f,
                100f,100f,100f,100f,100f,100f};
        float [] t1 = {100f,100f,100f,100f,100f,
                100f,100f,100f,100f,100f,
                100f,100f,100f,100f,100f,};
        float [] t2 = {100f,100f,100f,100f,100f,
                100f,100f,100f,100f,100f,
                100f,100f,100f,100f,100f,
                100f,100f,100f,100f,100f,100f};
        float [] t3 = {100f,100f,100f,100f,100f,
                100f,100f,100f,100f,100f,
                100f,100f,100f,100f,100f,
                100f,100f,100f,100f,100f,100f};

        timelist.add(t1);
        timelist.add(t1);
        timelist.add(t2);
        timelist.add(t3);

        GameState freshGame = new GameState(unfinishedLevel, unfinishedRoom, num_levels, level_paths, timelist);
        gamefile.writeString(json.prettyPrint(freshGame), false);
        return freshGame;
    }
    /**
     * Loads the game state from json.
     * 
     * @param game_state_json
     * @return generated LevelModel
     */
    public void loadGameState(String game_state_json) {

        jsonReader = new JsonReader();
        String[] level_paths = {"jsons/levels/tutorial.json",
                "jsons/levels/level_1.json",
                "jsons/levels/level_2.json",
                "jsons/levels/level_3.json"};
        // TODO: CHANGE THIS TO LOCAL (UNCOMMENT AND REPLACE LINE) FOR FINISHED VERSION
        try{
            gameDirectory = jsonReader.parse(Gdx.files.external(game_state_json));
            // parsing game_state json

//        String[] level_paths = gameDirectory.get("level_jsons").asStringArray();

        }catch(Exception e){
            gameState = writeNewGameState();
            gameDirectory = jsonReader.parse(Gdx.files.external(game_state_json));
        }
        int unfinishedLevel = gameDirectory.getInt("highest_level");
        int[] unfinishedRoom = gameDirectory.get("room_unlocked").asIntArray();
        int num_levels = gameDirectory.getInt("num_levels");
        if(gameState == null){
            gameState = new GameState(unfinishedLevel, unfinishedRoom, num_levels, level_paths);
        }

//        gameDirectory = jsonReader.parse(Gdx.files.internal(game_state_json));

        levelDirectories = new JsonValue[num_levels];
        levels = new LevelModel[num_levels];
        last_level_idx = levels.length - 1;

        levelDirectories[0] = jsonReader.parse(Gdx.files.internal(level_paths[0]));

        levels[0] = loadTutorial(levelDirectories[0]);
        levels[0].preloadLevel();
        JsonValue gameTime = gameDirectory.get("gameTimes");
        for (int i = 0; i < num_levels; i++) {
            // System.out.println("GDX ERROR "+ (i+1) +": " + Gdx.gl.glGetError());

            levelDirectories[i] = jsonReader.parse(Gdx.files.internal(level_paths[i]));
            levels[i] = loadLevel(levelDirectories[i], unfinishedLevel, unfinishedRoom[i]);
            if (levels[i].getLevelNumber() == unfinishedLevel) {
                highestUnlockedLevel = levels[i];
            }
            levels[i].setBestTime(gameTime.get(i).asFloatArray());
            levels[i].preloadLevel();
        }
        // System.out.println("GDX ERROR end:" + Gdx.gl.glGetError());
//        for (int j = 0; j < num_levels; j++) {
//            System.out.println(Arrays.toString(gameTime.get(j).asFloatArray()));
//        }
    }

    /**
     * Fully creates all levels during the loading period.
     */
    public void readyLevels() {
        for (LevelModel level : levels) {
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
    protected LevelModel loadLevel(JsonValue levelJson, int unfinishedLevel, int unfinishedRoom) {
        int lv = levelJson.getInt("level");
        int room_count = levelJson.getInt("room_count");
        String[] room_paths = levelJson.get("rooms").asStringArray();

        LevelController[] rooms = new LevelController[room_count];

        for (int i = 0; i < room_count; i++) {
            rooms[i] = new LevelController(room_paths[i]);
        }

        if (lv == unfinishedLevel) {
        return new LevelModel(lv, true, false, unfinishedRoom, rooms);
        } else if (lv > unfinishedLevel) {
            return new LevelModel(lv, false, false, unfinishedRoom, rooms);
        } else {
            return new LevelModel(lv, true, false, unfinishedRoom, rooms);
        }

    }

    public JsonValue getJson(int level) {
        return levelDirectories[level];
    }

    /**
     * Loads a level model from JSON.
     *
     * @param levelJson
     * @return LevelModel generated from levelJson
     */
    protected TutorialModel loadTutorial(JsonValue levelJson) {
        int lv = levelJson.getInt("level");
        int room_count = levelJson.getInt("room_count");
        JsonValue jsonCards = levelJson.get("cards");
        String[] room_paths = levelJson.get("rooms").asStringArray();
        String[] bg_paths = levelJson.get("story_backgrounds").asStringArray();
        String[] dl_paths = levelJson.get("dialogues").asStringArray();
        float[] map = levelJson.get("mapping").asFloatArray();
        float[] stoparray = levelJson.get("stop_array").asFloatArray();
        float[] startarray = levelJson.get("start_array").asFloatArray();



        TextureRegionDrawable[] ctBackgrounds = new TextureRegionDrawable[bg_paths.length];
        TextureRegionDrawable[] ctDialogues = new TextureRegionDrawable[dl_paths.length];

        for(int i = 0; i<bg_paths.length; i++){
            ctBackgrounds[i] = new TextureRegionDrawable(new TextureRegion(new Texture(Gdx.files.internal(bg_paths[i]))));
        }
        for(int i = 0; i<dl_paths.length; i++){
            ctDialogues[i] = new TextureRegionDrawable(new TextureRegion(new Texture(Gdx.files.internal(dl_paths[i]))));
        }

        LevelController[] rooms = new LevelController[room_count];
        HashMap<Integer, String[]> cards = new HashMap<>();

        for (int i = 0; i < room_count; i++) {
            cards.put(i, jsonCards.get("c" + (i + 1)).asStringArray());
            rooms[i] = new TutorialController(room_paths[i]);
        }

        return new TutorialModel(lv, true, true, 0, rooms, cards, ctBackgrounds, ctDialogues, map,stoparray, startarray);

    }

    /**
     *
     * @return the Screen to the current room being played
     */
    public LevelController getCurrentRoom() {
        return levels[current_level_idx].getCurrentRoom();
    }

    /**
     * Method called to step game progress, which involves either: 1. Moving to the
     * next room 2. Finishing last room in level and finishing level/moving on to
     * next level 3. Finishing the game
     */

    public void stepGame(boolean is_exit) {
        LevelModel currentLevel = levels[current_level_idx];
        // Updates the level timer
        currentLevel.updateBestTime(currentLevel.getCurrentRoomNumber());
        if (is_exit) {
            boolean finished = currentLevel.stepLevel();
            if (currentLevel.getHighestUnlockedRoom() == 10 && current_level_idx != 0){
                highestUnlockedLevel = levels[current_level_idx + 1];
                levels[current_level_idx + 1].unlockLevel();
            }
            if (finished) {
                MusicController.getInstance().stopAll();
                levels[current_level_idx].finishLevel();
                if (finished && current_level_idx == last_level_idx) {
                    endGame = true;
                    // endGameState(); //TODO: end game state accouncement/screen
                } else if (finished) {
                    current_level_idx++;
                    levels[current_level_idx].setCurrentRoom(0);
                    levels[current_level_idx].playMusic();
                    if (!levels[current_level_idx].isUnlocked()) {
                        highestUnlockedLevel = levels[current_level_idx];
                        levels[current_level_idx].unlockLevel();
                    }
                    // TODO: LEVEL FINISH SCREEN
                }
            }
        } else { // simply want to unlock next level if locked
                 // Update the best time of the level
            if(current_level_idx < levels.length-1) {
                if (!levels[current_level_idx + 1].isUnlocked()) {
                    highestUnlockedLevel = levels[current_level_idx + 1];
                    levels[current_level_idx + 1].unlockLevel();
                }
            }
        }

    }

    public void printGameState() {
        System.out.println("current level index" + current_level_idx);
        System.out.println("Current Level: " + levels[current_level_idx].getLevelNumber());
        System.out.println("Current Room: " + levels[current_level_idx].getCurrentRoomNumber());
        System.out.println("--------- LEVELS--------");
        for (LevelModel l : levels) {
            System.out.println("Level: " + l.getLevelNumber() + "| Unlocked: " + l.isUnlocked());
        }

    }

    /**
     * Returns the current Level
     * 
     * @return current LevelModel
     */
    public LevelModel getCurrentLevel() {
        return levels[current_level_idx];
    }

    /**
     * Sets the current Level
     * 
     * @return current LevelModel
     */
    public void setCurrentLevel(int idx, int roomidx) {
        current_level_idx = idx;
        levels[current_level_idx].setCurrentRoom(roomidx);
        System.out.println("idx " + idx);
        System.out.println("roomidx " + roomidx);
    }

    /**
     *
     * @param idx
     * @return the level at index idx
     */
    public LevelModel getLevel(int idx) {
        return levels[idx];
    }

    /**
     * Player beat the whole game!
     */
    public boolean endGameState() {
        return endGame;
    }

    /** resets the end game trigger **/
    public void resetEndGame(){
        endGame = false;
    }

    /**
     * Updates the whole game state in the directories
     */
    public void updateGameState() {
        gameState.setHighest_level(highestUnlockedLevel.getLevelNumber());
        for (int i = 0; i < gameState.num_levels; i++) {
            gameState.setRoom_unlocked(i, getLevel(i).getHighestUnlockedRoom());
        }
        gameState.setGameTimes(levels);
    }

    /**
     * Saves the whole game state to game.json and level jsons
     */
    public void saveGameState() {
        String currentdir = System.getProperty("user.dir");
        FileHandle gamefile = Gdx.files.external("tempus/jsons/game.json");
        Json json = new Json(JsonWriter.OutputType.json);
        json.setWriter(gamefile.writer(false));
        json.setOutputType(JsonWriter.OutputType.json);
        gamefile.writeString(json.prettyPrint(gameState), false);
    }

}
