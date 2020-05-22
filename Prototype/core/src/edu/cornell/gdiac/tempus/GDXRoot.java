/*
 * GDXRoot.java
 *
 * This is the primary class file for running the game.  It is the "static main" of
 * LibGDX.  In the first lab, we extended ApplicationAdapter.  In previous lab
 * we extended Game.  This is because of a weird graphical artifact that we do not
 * understand.  Transparencies (in 3D only) is failing when we use ApplicationAdapter. 
 * There must be some undocumented OpenGL code in setScreen.
 *
 * Author: Walker M. White
 * Based on original PhysicsDemo Lab by Don Holden, 2007
 * LibGDX version, 2/6/2015
 */
package edu.cornell.gdiac.tempus;

import com.badlogic.gdx.*;
import com.badlogic.gdx.assets.*;

import edu.cornell.gdiac.tempus.tempus.*;
import edu.cornell.gdiac.tempus.tempus.models.ScreenExitCodes;
import edu.cornell.gdiac.util.*;

/**
 * Root class for a LibGDX.
 * 
 * This class is technically not the ROOT CLASS. Each platform has another class
 * above this (e.g. PC games use DesktopLauncher) which serves as the true root.
 * However, those classes are unique to each platform, while this class is the
 * same across all plaforms. In addition, this functions as the root class all
 * intents and purposes, and you would draw it as a root class in an
 * architecture specification.
 */
public class GDXRoot extends Game implements ScreenListener {
	/** AssetManager to load game assets (textures, sounds, etc.) */
	private AssetManager manager;
	/** Drawing context to display graphics (VIEW CLASS) */
	private GameCanvas canvas;
	/** Player mode for the asset loading screen (CONTROLLER CLASS) */
	private LoadingMode loading;
	/** Player mode for the the game proper (CONTROLLER CLASS) */
	private int current;
	/** List of all WorldControllers */
	private WorldController[] controllers;
	/** Main Menu mode */
	private MainMenuMode menu;
	/** Select Level mode */
	private SelectLevelMode levelselect;
	/** Help Menu mode */
	private HelpMode helpmenu;
	/** About Menu mode */
	private AboutMode aboutmenu;
	/** Game State Manager **/
	private GameStateManager gameManager;
	/** Room Select Mode tester */
	private SelectRoomMode roomSelectMode;
	/** End Game controller */
	private LongRoomController endgame;


	/**
	 * Creates a new game from the configuration settings.
	 *
	 * This method configures the asset manager, but does not load any assets or
	 * assign any screen.
	 */
	public GDXRoot() {
		gameManager = new GameStateManager();


		// Start loading with the asset manager
		// manager = new AssetManager();

		// Add font support to the asset manager
		// FileHandleResolver resolver = new InternalFileHandleResolver();
		// manager.setLoader(FreeTypeFontGenerator.class, new
		// FreeTypeFontGeneratorLoader(resolver));
		// manager.setLoader(BitmapFont.class, ".ttf", new
		// FreetypeFontLoader(resolver));

	}

	/**
	 * Called when the Application is first created.
	 * 
	 * This is method immediately loads assets for the loading screen, and prepares
	 * the asynchronous loader for all other assets.
	 */
	public void create() {
		canvas = new GameCanvas();
//		canvas.setFullscreen(true,true);
		loading = new LoadingMode(canvas, 1);
		gameManager.loadGameState("tempus/jsons/game_state_final.json");
		gameManager.setCanvas(canvas);
		gameManager.setListener(this);
		menu = new MainMenuMode();
		levelselect = new SelectLevelMode();
		helpmenu = new HelpMode();
		aboutmenu = new AboutMode();
		current = 0;
		loading.setScreenListener(this);
		roomSelectMode = new SelectRoomMode(0);
//		endgame = new EndGameController("jsons/rooms/level_tallboi.json");
//		endgame.preLoadContent();

		setScreen(loading);

	}

	/**
	 * Called when the Application is destroyed.
	 *
	 * This is preceded by a call to pause().
	 */
	public void dispose() {
		gameManager.updateGameState();
		gameManager.saveGameState();
		// Call dispose on our children
		setScreen(null);
		if (controllers != null) {
			for (int ii = 0; ii < controllers.length; ii++) {
				controllers[ii].unloadContent();
				JsonAssetManager.clearInstance();
				controllers[ii].dispose();
			}
		}

		menu.dispose();
		levelselect.dispose();

		canvas.dispose();
		canvas = null;

		super.dispose();
	}

	/**
	 * Called when the Application is resized.
	 *
	 * This can happen at any point during a non-paused state but will never happen
	 * before a call to create().
	 *
	 * @param width  The new width in pixels
	 * @param height The new height in pixels
	 */
	public void resize(int width, int height) {
		canvas.resize(width, height);
		super.resize(width, height);
	}

	/**
	 * The given screen has made a request to exit its player mode.
	 *
	 * The value exitCode can be used to implement menu options.
	 *
	 * @param screen   The screen requesting to exit
	 * @param exitCode The state of the screen upon exit
	 */
	public void exitScreen(Screen screen, int exitCode) {

		if ((screen.getClass() == TutorialController.class ||
				screen.getClass() == LevelController.class ||
				screen.getClass() == LongRoomController.class )) {

			if(exitCode == ScreenExitCodes.EXIT_NEXT.ordinal()){
				if (gameManager.endGameState()) {
					gameManager.updateGameState();
					gameManager.resetEndGame();
					gameManager.getEndGame().reset();
					MusicController.getInstance().stopAll();
					setScreen(gameManager.getEndGame());
				} else {
					gameManager.updateGameState();
					LevelController room = gameManager.getCurrentRoom();
					canvas.setBlendState(GameCanvas.BlendState.NO_PREMULT);
					room.reset();
					setScreen(room);
				}
			}else if (exitCode == ScreenExitCodes.EXIT_PREV.ordinal()) {
				MusicController.getInstance().stopAll();
				levelselect.setScreenListener(this);
				levelselect.setCanvas(canvas);
				levelselect.createMode();
				setScreen(levelselect);
			}
		}else if(screen == roomSelectMode){
			if(exitCode == ScreenExitCodes.EXIT_PREV.ordinal()){
				levelselect.setScreenListener(this);
				levelselect.setCanvas(canvas);
				levelselect.createMode();
				setScreen(levelselect);
			}else{
				gameManager.getCurrentRoom().reset();
				gameManager.getCurrentLevel().playMusic();
				setScreen(gameManager.getCurrentRoom());
			}
			roomSelectMode.dispose();
			roomSelectMode = null;
		}else if(exitCode ==  ScreenExitCodes.EXIT_ENDGAME.ordinal()){
			levelselect.setScreenListener(this);
			levelselect.setCanvas(canvas);
			levelselect.createMode();
			setScreen(levelselect);
		}
		else if (screen == loading || exitCode == ScreenExitCodes.EXIT_LOAD.ordinal()) {
			gameManager.readyLevels();
			menu.createMode();
			menu.setScreenListener(this);
			menu.setCanvas(canvas);
			setScreen(menu);
			loading.dispose();
			loading = null;
		} else if (screen == menu) {
			if (exitCode == ScreenExitCodes.MENU_START.ordinal()) {
				levelselect.createMode();
				levelselect.setScreenListener(this);
				levelselect.setCanvas(canvas);
				MusicController.getInstance().stopAll();
				setScreen(levelselect);
			} else if (exitCode == ScreenExitCodes.MENU_ABOUT.ordinal()) {
				// TODO menu about hookup
				aboutmenu.createMode();
				aboutmenu.setScreenListener(this);
				aboutmenu.setCanvas(canvas);
				MusicController.getInstance().stopAll();
				setScreen(aboutmenu);
			} else if (exitCode == ScreenExitCodes.MENU_HELP.ordinal()) {
				// TODO menu help hookup
				helpmenu.createMode();
				helpmenu.setScreenListener(this);
				helpmenu.setCanvas(canvas);
				MusicController.getInstance().stopAll();
				setScreen(helpmenu);
			} else {
				// We quit the main application
				gameManager.updateGameState();
				gameManager.saveGameState();
				Gdx.app.exit();
			}
			menu.dispose();
		} else if (screen == levelselect) {
			if (exitCode == ScreenExitCodes.EXIT_PREV.ordinal()) {
				menu.createMode();
				menu.setScreenListener(this);
				menu.setCanvas(canvas);
				setScreen(menu);
			} else if (exitCode == ScreenExitCodes.ROOM_SELECT.ordinal()) {
				roomSelectMode = new SelectRoomMode(GameStateManager.getInstance().getCurrentLevel().getLevelNumber());
				roomSelectMode.createMode();
				roomSelectMode.setScreenListener(this);
				roomSelectMode.setCanvas(canvas);
				setScreen(roomSelectMode);
			}else { // go to a level
					MusicController.getInstance().stopAll();
					gameManager.setCurrentLevel(exitCode, gameManager.getCurrentLevel().getCurrentRoomNumber());
					gameManager.getCurrentRoom().reset();
//				gameManager.printGameState();
					gameManager.getCurrentLevel().playMusic();
					setScreen(gameManager.getCurrentRoom());

			}
		} else if (screen == helpmenu) {
			if (exitCode == ScreenExitCodes.EXIT_PREV.ordinal()) {
				menu.createMode();
				menu.setScreenListener(this);
				menu.setCanvas(canvas);
				setScreen(menu);
			}
		}
		else if (screen == aboutmenu) {
			if (exitCode == ScreenExitCodes.EXIT_PREV.ordinal()) {
				menu.createMode();
				menu.setScreenListener(this);
				menu.setCanvas(canvas);
				setScreen(menu);
			}
		}
		 else if (exitCode == ScreenExitCodes.EXIT_PREV.ordinal()) {
			gameManager.updateGameState();
			MusicController.getInstance().stopAll();
			levelselect.setScreenListener(this);
			levelselect.setCanvas(canvas);
			levelselect.createMode();
			setScreen(levelselect);
		} else if (exitCode == ScreenExitCodes.EXIT_QUIT.ordinal()) {
			// We quit the main application
			gameManager.updateGameState();
			gameManager.saveGameState();
			Gdx.app.exit();
		}
	}

}
