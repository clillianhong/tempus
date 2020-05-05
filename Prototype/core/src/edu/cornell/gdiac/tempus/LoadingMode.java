/*
 * LoadingMode.java
 *
 * Asset loading is a really tricky problem.  If you have a lot of sound or images,
 * it can take a long time to decompress them and load them into memory.  If you just
 * have code at the start to load all your assets, your game will look like it is hung
 * at the start.
 *
 * The alternative is asynchronous asset loading.  In asynchronous loading, you load a
 * little bit of the assets at a time, but still animate the game while you are loading.
 * This way the player knows the game is not hung, even though he or she cannot do 
 * anything until loading is complete. You know those loading screens with the inane tips 
 * that want to be helpful?  That is asynchronous loading.  
 *
 * This player mode provides a basic loading screen.  While you could adapt it for
 * between level loading, it is currently designed for loading all assets at the 
 * start of the game.
 *
 * Author: Walker M. White
 * Based on original PhysicsDemo Lab by Don Holden, 2007
 * LibGDX version, 2/6/2015
 */
package edu.cornell.gdiac.tempus;

import com.badlogic.gdx.*;
import com.badlogic.gdx.math.*;
import com.badlogic.gdx.assets.*;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.controllers.*;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import edu.cornell.gdiac.tempus.tempus.models.ScreenExitCodes;
import edu.cornell.gdiac.util.*;
import jdk.nashorn.internal.runtime.JSONFunctions;


/**
 * Class that provides a loading screen for the state of the game.
 *
 * You still DO NOT need to understand this class for this lab.  We will talk about this
 * class much later in the course.  This class provides a basic template for a loading
 * screen to be used at the start of the game or between levels.  Feel free to adopt
 * this to your needs.
 *
 * You will note that this mode has some textures that are not loaded by the AssetManager.
 * You are never required to load through the AssetManager.  But doing this will block
 * the application.  That is why we try to have as few resources as possible for this
 * loading screen.
 */
public class LoadingMode implements Screen {
	// Textures necessary to support the loading screen 
	private static final String BACKGROUND_FILE = "textures/background/loadingbackground_2.png";
	private static final String FLOWER_1_FILE = "textures/gui/loadingmode/flower1.png";
	private static final String FLOWER_2_FILE = "textures/gui/loadingmode/flower2.png";
	private static final String FLOWER_3_FILE = "textures/gui/loadingmode/flower3.png";
	private static final String FLOWER_4_FILE = "textures/gui/loadingmode/flower4.png";

	private boolean shouldBeRendered;
	/** Background texture for start-up */
	private Texture background;
	/** Flower texture 1 */
	private Texture flower1;
	/** Flower texture 2 */
	private Texture flower2;
	/** Flower texture 3 */
	private Texture flower3;
	/** Flower texture 4 */
	private Texture flower4;



	int sw = 1920/2;
	int sh = 1080/2;
	/** Default budget for asset loader (do nothing but load 60 fps) */
	private static int DEFAULT_BUDGET = 15;


	/** AssetManager to be loading in the background */
	private AssetManager manager;
	/** Reference to GameCanvas created by the root */
	private GameCanvas canvas;
	/** Listener that will update the player mode when we are done */
	private ScreenListener listener;
	/** Stage for drawing assets */
	private Stage stage;



	/** Current progress (0 to 1) of the asset manager */
	private float progress;
	/** message labels */
	Label loadingLabel;
	Label percentLabel;

	/** The amount of time to devote to loading assets (as opposed to on screen hints, etc.) */
	private int   budget;

	/** Whether or not this player mode is still active */
	private boolean active;

	/** Font */
	private BitmapFont font;
	/** Glyph layout for on the fly generation */
	private static GlyphLayout glyphLayout;
	/**
	 * Returns the budget for the asset loader.
	 *
	 * The budget is the number of milliseconds to spend loading assets each animation
	 * frame.  This allows you to do something other than load assets.  An animation 
	 * frame is ~16 milliseconds. So if the budget is 10, you have 6 milliseconds to 
	 * do something else.  This is how game companies animate their loading screens.
	 *
	 * @return the budget in milliseconds
	 */
	public int getBudget() {
		return budget;
	}

	/**
	 * Sets the budget for the asset loader.
	 *
	 * The budget is the number of milliseconds to spend loading assets each animation
	 * frame.  This allows you to do something other than load assets.  An animation 
	 * frame is ~16 milliseconds. So if the budget is 10, you have 6 milliseconds to 
	 * do something else.  This is how game companies animate their loading screens.
	 *
	 * @param millis the budget in milliseconds
	 */
	public void setBudget(int millis) {
		budget = millis;
	}

	/**
	 * Creates a LoadingMode with the default budget, size and position.
	 *
	 * @param manager The AssetManager to load in the background
	 */
	public LoadingMode(GameCanvas canvas, AssetManager manager) {
		this(canvas,DEFAULT_BUDGET);
	}

	/**
	 * Creates a LoadingMode with the default size and position.
	 *
	 * The budget is the number of milliseconds to spend loading assets each animation
	 * frame.  This allows you to do something other than load assets.  An animation 
	 * frame is ~16 milliseconds. So if the budget is 10, you have 6 milliseconds to 
	 * do something else.  This is how game companies animate their loading screens.
	 *
	 * @param canvas The AssetManager to load in the background
	 * @param millis The loading budget in milliseconds
	 */
	public LoadingMode(GameCanvas canvas, int millis) {
		this.manager = JsonAssetManager.getInstance();
		this.canvas  = canvas;
		budget = millis;

		background = new Texture(BACKGROUND_FILE);
		flower1 = new Texture(FLOWER_1_FILE);
		flower2 = new Texture(FLOWER_2_FILE);
		flower3 = new Texture(FLOWER_3_FILE);
		flower4 = new Texture(FLOWER_4_FILE);

		active = true;

		stage = new Stage(canvas.getViewport());
		Gdx.input.setInputProcessor(stage);
		stage.getCamera().viewportWidth = sw;
		stage.getCamera().viewportHeight = sh;

		Table bg = new Table();
		bg.setBackground(new TextureRegionDrawable(background));
		bg.setFillParent(true);
		stage.addActor(bg);

		font = new BitmapFont(Gdx.files.internal("fonts/carterone.fnt"));
		glyphLayout  = new GlyphLayout();
		Gdx.gl.glClearColor(0,0,0,1);

		loadingMessage = "Loading . . .";
		font.getData().setScale(1.5f);
		glyphLayout.setText(font, loadingMessage);
		Label.LabelStyle carterStyle = new Label.LabelStyle(font, Color.WHITE);
		float loadingHeight = glyphLayout.height;
		float loadingWidth = glyphLayout.width;
		loadingLabel = new Label("Loading . . .", carterStyle);
		loadingLabel.setHeight(loadingHeight);
		loadingLabel.setWidth(loadingWidth);
//		loadingLabel.setFontScale(0.3f);
		loadingLabel.setPosition(canvas.getWidth()- 1.3f*loadingWidth, canvas.getHeight()/2);

		loadingLabel = new Label("Loading . . .", carterStyle);
		loadingLabel.setHeight(loadingHeight);
		loadingLabel.setWidth(loadingWidth);
		loadingLabel.setPosition(canvas.getWidth()- 1.3f*loadingWidth, canvas.getHeight()/2);

		BitmapFont font2 = new BitmapFont(Gdx.files.internal("fonts/carterone.fnt"));

		font2.getData().setScale(0.75f);
		String percentMessage = (this.progress*100) + "%";
		Label.LabelStyle carterStyle2 = new Label.LabelStyle(font2, Color.WHITE);
		percentLabel = new Label(percentMessage, carterStyle2);
		glyphLayout.setText(font2, percentMessage);
		percentLabel.setHeight(glyphLayout.height);
		percentLabel.setWidth(glyphLayout.width);
//		percentLabel.setFontScale(0.75f);
		percentLabel.setPosition(canvas.getWidth()- 1.3f*loadingWidth, canvas.getHeight()/2 - loadingHeight*1.1f);

		stage.addActor(percentLabel);
		stage.addActor(loadingLabel);

		// Custom cursor texture
		Pixmap pm = new Pixmap(Gdx.files.internal("textures/gui/cursor.png"));
		Cursor cursor = Gdx.graphics.newCursor(pm, 0, 0);
		Gdx.graphics.setCursor(cursor);
		pm.dispose();
	}
	
	/**
	 * Called when this screen should release all resources.
	 */
	public void dispose() {
		flower1 = null;
		flower2 = null;
		flower3 = null;
		flower4 = null;
		background = null;
		stage.dispose();
	}
	
	/**
	 * Update the status of this player mode.
	 *
	 * We prefer to separate update and draw from one another as separate methods, instead
	 * of using the single render() method that LibGDX does.  We will talk about why we
	 * prefer this in lecture.
	 *
	 * @param delta Number of seconds since last animation frame
	 */
	private void update(float delta) {
		manager.update(budget);
		this.progress = manager.getProgress();
		if (progress >= 1.0f) {
			this.progress = 1.0f;
		}
		String percentMessage = (this.progress*100) + "%";
		percentLabel.setText(percentMessage);
	}

	String loadingMessage;

	/**
	 * Draw the status of this player mode.
	 *
	 * We prefer to separate update and draw from one another as separate methods, instead
	 * of using the single render() method that LibGDX does.  We will talk about why we
	 * prefer this in lecture.
	 */
	private void draw() {
		canvas.clear();
		stage.draw();
		stage.act();
	}
	
	/**
	 * Updates the progress bar according to loading progress
	 *
	 * The progress bar is composed of parts: two rounded caps on the end, 
	 * and a rectangle in a middle.  We adjust the size of the rectangle in
	 * the middle to represent the amount of progress.
	 *
	 * @param canvas The drawing context
	 */	
	private void drawProgress(GameCanvas canvas) {	
			if(this.progress <= 0.25){

			}else if (this.progress <= 0.5){

			}else if(this.progress <= 0.75){

			}else {

			}
	}

	// ADDITIONAL SCREEN METHODS
	/**
	 * Called when the Screen should render itself.
	 *
	 * We defer to the other methods update() and draw().  However, it is VERY important
	 * that we only quit AFTER a draw.
	 *
	 * @param delta Number of seconds since last animation frame
	 */
	public void render(float delta) {
		if (active) {
			canvas.updateSpriteBatch();
			stage.getBatch().setProjectionMatrix(stage.getCamera().combined);
			stage.getCamera().update();
			update(delta);
			draw();
			// We are are ready, notify our listener
			if(progress >= 1.0f){
				stage.addAction(Actions.sequence(Actions.fadeOut(0.5f), Actions.run(new Runnable() {
					@Override
					public void run() {
						exitToMainMenu();
					}
				})));
			}
		}
	}

	public void exitToMainMenu(){
		active = false;
		listener.exitScreen(this, ScreenExitCodes.EXIT_LOAD.ordinal());
	}

	/**
	 * Called when the Screen is resized. 
	 *
	 * This can happen at any point during a non-paused state but will never happen 
	 * before a call to show().
	 *
	 * @param width  The new width in pixels
	 * @param height The new height in pixels
	 */
	public void resize(int width, int height) {
		canvas.resize(width, height);
		stage.getViewport().update(width, height);
		stage.getCamera().position.set(stage.getCamera().viewportWidth / 2, stage.getCamera().viewportHeight / 2, 0);
		stage.getCamera().update();
	}

	/**
	 * Called when the Screen is paused.
	 * 
	 * This is usually when it's not active or visible on screen. An Application is 
	 * also paused before it is destroyed.
	 */
	public void pause() {
		// TODO Auto-generated method stub

	}

	/**
	 * Called when the Screen is resumed from a paused state.
	 *
	 * This is usually when it regains focus.
	 */
	public void resume() {
		// TODO Auto-generated method stub

	}
	
	/**
	 * Called when this screen becomes the current screen for a Game.
	 */
	public void show() {
		// Useless if called in outside animation loop
		active = true;
	}

	/**
	 * Called when this screen is no longer the current screen for a Game.
	 */
	public void hide() {
		// Useless if called in outside animation loop
		active = false;
	}
	
	/**
	 * Sets the ScreenListener for this mode
	 *
	 * The ScreenListener will respond to requests to quit.
	 */
	public void setScreenListener(ScreenListener listener) {
		this.listener = listener;
	}



	// UNSUPPORTED METHODS FROM InputProcessor

	/** 
	 * Called when a key is pressed (UNSUPPORTED)
	 *
	 * @param keycode the key pressed
	 * @return whether to hand the event to other listeners. 
	 */
	public boolean keyDown(int keycode) { 
		return true; 
	}

	/** 
	 * Called when a key is typed (UNSUPPORTED)
	 *
	 * @param character the key typed
	 * @return whether to hand the event to other listeners. 
	 */
	public boolean keyTyped(char character) { 
		return true; 
	}


	/** 
	 * Called when the mouse was moved without any buttons being pressed. (UNSUPPORTED)
	 *
	 * @param screenX the x-coordinate of the mouse on the screen
	 * @param screenY the y-coordinate of the mouse on the screen
	 * @return whether to hand the event to other listeners. 
	 */	
	public boolean mouseMoved(int screenX, int screenY) { 
		return true; 
	}

	/** 
	 * Called when the mouse wheel was scrolled. (UNSUPPORTED)
	 *
	 * @param amount the amount of scroll from the wheel
	 * @return whether to hand the event to other listeners. 
	 */	
	public boolean scrolled(int amount) { 
		return true; 
	}

	/** 
	 * Called when the mouse or finger was dragged. (UNSUPPORTED)
	 *
	 * @param screenX the x-coordinate of the mouse on the screen
	 * @param screenY the y-coordinate of the mouse on the screen
	 * @param pointer the button or touch finger number
	 * @return whether to hand the event to other listeners. 
	 */		
	public boolean touchDragged(int screenX, int screenY, int pointer) { 
		return true; 
	}
	
	// UNSUPPORTED METHODS FROM ControllerListener
	
	/**
	 * Called when a controller is connected. (UNSUPPORTED)
	 *
	 * @param controller The game controller
	 */
	public void connected (Controller controller) {}

	/**
	 * Called when a controller is disconnected. (UNSUPPORTED)
	 *
	 * @param controller The game controller
	 */
	public void disconnected (Controller controller) {}

	/** 
	 * Called when an axis on the Controller moved. (UNSUPPORTED) 
	 *
	 * The axisCode is controller specific. The axis value is in the range [-1, 1]. 
	 *
	 * @param controller The game controller
	 * @param axisCode 	The axis moved
	 * @param value 	The axis value, -1 to 1
	 * @return whether to hand the event to other listeners. 
	 */
	public boolean axisMoved (Controller controller, int axisCode, float value) {
		return true;
	}

	/** 
	 * Called when a POV on the Controller moved. (UNSUPPORTED) 
	 *
	 * The povCode is controller specific. The value is a cardinal direction. 
	 *
	 * @param controller The game controller
	 * @param povCode 	The POV controller moved
	 * @param value 	The direction of the POV
	 * @return whether to hand the event to other listeners. 
	 */
	public boolean povMoved (Controller controller, int povCode, PovDirection value) {
		return true;
	}

	/** 
	 * Called when an x-slider on the Controller moved. (UNSUPPORTED) 
	 *
	 * The x-slider is controller specific. 
	 *
	 * @param controller The game controller
	 * @param sliderCode The slider controller moved
	 * @param value 	 The direction of the slider
	 * @return whether to hand the event to other listeners. 
	 */
	public boolean xSliderMoved (Controller controller, int sliderCode, boolean value) {
		return true;
	}

	/** 
	 * Called when a y-slider on the Controller moved. (UNSUPPORTED) 
	 *
	 * The y-slider is controller specific. 
	 *
	 * @param controller The game controller
	 * @param sliderCode The slider controller moved
	 * @param value 	 The direction of the slider
	 * @return whether to hand the event to other listeners. 
	 */
	public boolean ySliderMoved (Controller controller, int sliderCode, boolean value) {
		return true;
	}

	/** 
	 * Called when an accelerometer value on the Controller changed. (UNSUPPORTED) 
	 * 
	 * The accelerometerCode is controller specific. The value is a Vector3 representing 
	 * the acceleration on a 3-axis accelerometer in m/s^2.
	 *
	 * @param controller The game controller
	 * @param accelerometerCode The accelerometer adjusted
	 * @param value A vector with the 3-axis acceleration
	 * @return whether to hand the event to other listeners. 
	 */
	public boolean accelerometerMoved(Controller controller, int accelerometerCode, Vector3 value) {
		return true;
	}

}