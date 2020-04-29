/*
 * PlatformController.java
 *
 * This is one of the files that you are expected to modify. Please limit changes to
 * the regions that say INSERT CODE HERE.
 *
 * Author: Walker M. White
 * Based on original PhysicsDemo Lab by Don Holden, 2007
 * LibGDX version, 2/6/2015
 */
package edu.cornell.gdiac.tempus.tempus;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.graphics.g2d.freetype.FreetypeFontLoader;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.box2d.*;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.*;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import edu.cornell.gdiac.audio.MusicBuffer;
import edu.cornell.gdiac.tempus.InputController;
import edu.cornell.gdiac.tempus.MusicController;
import edu.cornell.gdiac.tempus.WorldController;
import edu.cornell.gdiac.util.*;
import edu.cornell.gdiac.tempus.GameCanvas;
import edu.cornell.gdiac.tempus.obstacle.*;
import edu.cornell.gdiac.tempus.tempus.models.*;

import static edu.cornell.gdiac.tempus.tempus.models.EntityType.PAST;
import static edu.cornell.gdiac.tempus.tempus.models.EntityType.PRESENT;

/**
 * Gameplay specific controller for the platformer game.
 * <p>
 * You will notice that asset loading is not done with static methods this time.
 * Instance asset loading makes it easier to process our game modes in a loop,
 * which is much more scalable. However, we still want the assets themselves to
 * be static. This is the purpose of our AssetState variable; it ensures that
 * multiple instances place nicely with the static assets.
 */
public class LevelController extends WorldController {

	/** Stage for adding UI components **/
	protected Skin skin;
	protected Stage stage;

	protected Table table;
	protected Table pauseTable;
	protected Container pauseButtonContainer;

	/**
	 * RIPPLE SHADER ** /
	 * 
	 * /** vertex shader source code
	 */
	protected String vert;
	/** fragment shader source code */
	protected String frag;
	/** custom shader */
	protected ShaderProgram shaderprog;
	/** background sprite batch for rendering w shader */
	SpriteBatch batch;
	/** background sprite for rendering w shader */
	Sprite bgSprite;

	/*SHADER IMPLEMENTATION*/
	/** time ticks for sine/cosine wave in frag shader */
	float ticks;
	/** current mouse position */
	Vector2 mouse_pos;
	/** current delta x */
	float delta_x;
	/** current delta y */
	float delta_y;
	/** tick value to reset */
	float ripple_reset;
	Vector2 fish_pos;
	float m_rippleDistance;
	float prev_m_rippleDistance;
	float rippleSpeed = 0.25f;
	float maxRippleDistance = 2f;
	float m_rippleRange;
	boolean rippleOn;

	float time_incr;

	/** whether or not game is paused **/
	protected boolean paused;
	/** whether game input should stall in case of pause **/
	protected boolean prepause;

	/** Checks if did debug */
	protected boolean debug;
	/** counts down beginning of game to avoid opening mis-dash */
	protected int begincount;

	/** The sound file for a bullet fire */
	protected static final String PEW_FILE = "sounds/pew.mp3";

	/** The texture for walls and platforms */
	protected TextureRegion earthTile;
	/** The texture for the exit condition */
	protected TextureRegion goalTile;
	/** The font for giving messages to the player */
	protected BitmapFont displayFont;

	/** Texture asset for the big bullet */
	protected TextureRegion bulletBigTexture;
	protected TextureRegion presentBullet = JsonAssetManager.getInstance().getEntry("projpresent", TextureRegion.class);
	protected TextureRegion pastBullet = JsonAssetManager.getInstance().getEntry("projpast", TextureRegion.class);

	/** Texture asset for the background */
	protected TextureRegion pastBackgroundTexture;
	protected TextureRegion presentBackgroundTexture;

	protected Music present_music;
	protected Music past_music;

	/** The reader to process JSON files */
	protected JsonReader jsonReader;
	/** The JSON asset directory */
	protected JsonValue assetDirectory;
	/** The JSON defining the level model */
	protected JsonValue levelFormat;

	/** Track asset loading from all instances and subclasses */
	protected AssetState platformAssetState = AssetState.EMPTY;
	/** Freeze time */
	protected boolean timeFreeze;
	protected Vector2 avatarStart;
	protected int numEnemies;

	/**
	 * Preloads the assets for this controller.
	 *
	 * To make the game modes more for-loop friendly, we opted for nonstatic loaders
	 * this time. However, we still want the assets themselves to be static. So we
	 * have an AssetState that determines the current loading state. If the assets
	 * are already loaded, this method will do nothing.
	 */
	public void preLoadContent() {
		if (platformAssetState != AssetState.EMPTY) {
			return;
		}
		platformAssetState = AssetState.LOADING;

		jsonReader = new JsonReader();
		assetDirectory = jsonReader.parse(Gdx.files.internal("jsons/assets.json"));

		JsonAssetManager.getInstance().loadDirectory(assetDirectory);
		// super.preLoadContent(manager);
	}

	/**
	 * Load the assets for this controller.
	 *
	 * To make the game modes more for-loop friendly, we opted for nonstatic loaders
	 * this time. However, we still want the assets themselves to be static. So we
	 * have an AssetState that determines the current loading state. If the assets
	 * are already loaded, this method will do nothing.
	 */
	public void loadContent() {
		if (platformAssetState != AssetState.LOADING) {
			return;
		}
		JsonAssetManager.getInstance().allocateDirectory();
		displayFont = JsonAssetManager.getInstance().getEntry("display", BitmapFont.class);
		platformAssetState = AssetState.COMPLETE;

	}

	// Physics constants for initialization
	/** The new heavier gravity for this world (so it is not so floaty) */
	protected static final float DEFAULT_GRAVITY = -14.7f;
	/** The density for most physics objects */
	protected static final float BASIC_DENSITY = 0.0f;
	/** The density for a bullet */
	protected static final float HEAVY_DENSITY = 10.0f;
	/** Friction of most platforms */
	protected static final float BASIC_FRICTION = 0.6f;
	/** The restitution for all physics objects */
	protected static final float BASIC_RESTITUTION = 0.1f;
	// /** Offset for bullet when firing */
	// protected static final float BULLET_OFFSET = 1.5f;
	/** The volume for sound effects */
	protected static final float EFFECT_VOLUME = 0.1f;

	// Since these appear only once, we do not care about the magic numbers.
	// In an actual game, this information would go in a data file.
	// Wall vertices
	protected static final float[][] WALLS = { { 0.0f, 0.0f, 1.0f, 0.0f, 1.0f, 18.0f, 0.0f, 18.0f },
			{ 1.0f, 18.0f, 1.0f, 17.0f, 31.0f, 17.0f, 31.0f, 18.0f },
			{ 31.0f, 18.0f, 31.0f, 0.0f, 32.0f, 0.0f, 32.0f, 18.0f } };
	/*
	 * {16.0f, 18.0f, 16.0f, 17.0f, 1.0f, 17.0f, 1.0f, 0.0f, 0.0f, 0.0f, 0.0f,
	 * 18.0f}, {32.0f, 18.0f, 32.0f, 0.0f, 31.0f, 0.0f, 31.0f, 17.0f, 16.0f, 17.0f,
	 * 16.0f, 18.0f} };
	 */

	/** The outlines of all of the platforms */
	protected static final float[][] PLATFORMS = {

			{ 1.0f, 4.0f, 3.0f, 4.0f, 3.0f, 2.5f, 1.0f, 2.5f }, // { 3.0f, 8.0f, 5.0f, 8.0f, 5.0f, 7.5f, 3.0f, 7.5f },
			{ 5.5f, 4.5f, 7.5f, 4.5f, 7.5f, 5.0f, 5.5f, 5.0f }, // downwards diagonal
			{ 9.0f, 6.0f, 9.0f, 7.5f, 9.5f, 7.5f, 9.5f, 6.0f }, { 7.0f, 9.0f, 7.0f, 10.5f, 7.5f, 10.5f, 7.5f, 9.0f },
			{ 9.0f, 11.5f, 9.0f, 13.0f, 9.5f, 13.0f, 9.5f, 11.5f }, { 7.0f, 15.5f, 7.5f, 15.5f, 7.5f, 14.0f, 7.0f, 14.0f },
			{ 11.0f, 15.5f, 11.5f, 15.5f, 11.5f, 14.0f, 11.0f, 14.0f },
			{ 12.0f, 6.5f, 13.5f, 6.5f, 13.5f, 6.0f, 12.0f, 6.0f }, { 15.0f, 8.0f, 15.0f, 8.5f, 16.5f, 8.5f, 16.5f, 8.0f },
			{ 18.0f, 5.5f, 18.0f, 6.0f, 19.5f, 6.0f, 19.5f, 5.5f }, { 21.0f, 8.0f, 21.0f, 8.5f, 22.5f, 8.5f, 22.5f, 8.0f },
			{ 24.0f, 5.5f, 24.0f, 6.0f, 25.5f, 6.0f, 25.5f, 5.5f },
			{ 25.5f, 10.0f, 25.5f, 11.5f, 26.0f, 11.5f, 26.0f, 10.0f },
			{ 23.5f, 13.0f, 23.5f, 14.5f, 24.0f, 14.5f, 24.0f, 13.0f },
			{ 26.5f, 13.0f, 26.5f, 14.0f, 31.0f, 14.0f, 31.0f, 13.0f } };

	/** The positions of all present capsule platforms */
	protected static final float[][] PRESENT_CAPSULES = { { 3.0f, 7.0f }, { 6.0f, 4.0f }, { 24.0f, 11.5f } };
	/** The positions of all present diamond platforms */
	protected static final float[][] PRESENT_DIAMONDS = { { 1.0f, 2.0f }, { 11.0f, 7.0f } };
	/** The positions of all present rounded platforms */
	protected static final float[][] PRESENT_ROUNDS = { { 11.5f, 2.0f }, { 9.5f, 13.0f } };
	/** The positions of all past capsule platforms */
	protected static final float[][] PAST_CAPSULES = { { 4.5f, 1.0f }, { 14.5f, 9.0f } };
	/** The positions of all past diamond platforms */
	protected static final float[][] PAST_DIAMONDS = { { 13.5f, 3.0f }, { 20.0f, 5.0f } };
	/** The positions of all past rounded platforms */
	protected static final float[][] PAST_ROUNDS = { { 2.0f, 13.0f }, { 18.5f, 13.0f } };

	// Other game objects
	/** The goal door position */
	protected static Vector2 GOAL_POS = new Vector2(29.5f, 15.5f);
	/** The initial position of the dude */
	protected static Vector2 DUDE_POS = new Vector2(2.5f, 5.0f);
	/** The initial position of the turret */
	protected static Vector2 TURRET_POS = new Vector2(8.5f, 10.0f);
	// /** The initial position of the enemy */
	// protected static Vector2 ENEMY_POS = new Vector2(13.0f, 7.5f);

	// Physics objects for the game
	/** Reference to the character avatar */
	protected Avatar avatar;
	/** Reference to the goalDoor (for collision detection) */
	protected Door goalDoor;
	/** is tutorial mode */
	protected boolean isTutorial;

	/** The information of all the enemies */
	protected int NUMBER_ENEMIES = 2;
	protected EntityType[] TYPE_ENEMIES = { PRESENT, PAST };
	protected float[][] COOR_ENEMIES = { { 13.0f, 6.0f }, { 15.625f, 11.03125f } };
	protected int[] CD_ENEMIES = { 80, 80 };

	/** The information of all the turrets */
	protected int NUMBER_TURRETS = 2;
	protected EntityType[] TYPE_TURRETS = { PRESENT, PAST };
	protected float[][] COOR_TURRETS = { { TURRET_POS.x + 10.0f, TURRET_POS.y + 0.3f },
			{ TURRET_POS.x, TURRET_POS.y - 5.0f } };
	protected float[][] DIR_TURRETS = { // direction of proj which the turrets shoot
			{ -3.0f, 0 }, { 0, 2.0f } };
	protected int[] CD_TURRETS = { 90, 120 };

	protected Enemy enemy;

	/** Whether the avatar is shifted to the other world or not */
	protected boolean shifted;

	/** Collision Controller instance **/
	protected CollisionController collisionController;
	/** Enemy Controller instance */
	protected EnemyController enemyController;

	/** FILEPATH TO JSON RESOURCE FOR THE LEVEL **/
	protected String json_filepath;

	/** TextureRegion for room win state **/
	protected TextureRegion win_room;

	int sw = 1920/2;
	int sh = 1080/2;

	protected Vector3 cursor;

	protected OrthographicCamera camera;

	/**
	 * Creates and initialize a new instance of the platformer game
	 *
	 * The game has default gravity and other settings
	 */
	public LevelController(String json) {
		super(DEFAULT_WIDTH, DEFAULT_HEIGHT, DEFAULT_GRAVITY);
		setDebug(false);
		setComplete(false);
		setFailure(false);
		shifted = false;
		paused = false;
		prepause = false;
		debug = false;
		timeFreeze = false;
		json_filepath = json;
		numEnemies = 0;
		begincount = 10;
		enemyController = new EnemyController(enemies, objects, avatar, world, scale, this, assetDirectory);
		isTutorial = false;

		// ripple shader
		ticks = 0f;
		rippleOn = false;
		vert = Gdx.files.internal(".vertex.glsl").readString();
		frag = Gdx.files.internal(".fragment.glsl").readString();
		shaderprog = new ShaderProgram(vert, frag);
		shaderprog.pedantic = false;
		m_rippleDistance = 0;
		m_rippleRange = 0;
		ticks = 0;
		time_incr = (float) 0.002;
		ripple_reset = sw * 0.00025f;
		shaderprog.setUniformf("time", ticks);
		batch = new SpriteBatch();
		mouse_pos = new Vector2(0.5f, 0.5f);
		delta_x = 1000;
		delta_y = 1000;
		fish_pos = new Vector2(0, 0);

		camera = new OrthographicCamera(sw,sh);
		viewport = new FitViewport(sw, sh, camera);
		viewport.apply();
	}

	private FitViewport viewport;

	/**
	 * Resets the status of the game so that we can play again.
	 *
	 * This method disposes of the world and creates a new one.
	 */
	public void reset() {
		// Vector2 gravity = new Vector2(world.getGravity());

		numEnemies = 0;
		paused = false;
		prepause = false;
		enemyController.reset();
		for (Obstacle obj : objects) {
			obj.deactivatePhysics(world);
		}
		objects.clear();
		addQueue.clear();
		world.dispose();
		shifted = false;
		begincount = 10;
		// world = new World(gravity, false);
		world.setContactListener(collisionController);
		// world.setContactListener(this);
		setComplete(false);
		setFailure(false);
		levelFormat = jsonReader.parse(Gdx.files.internal(json_filepath));
		// levelFormat =
		// jsonReader.parse(Gdx.files.internal("jsons/test_level_editor.json"));
		/*
		 * present_music =
		 * JsonAssetManager.getInstance().getEntry(levelFormat.getString("present_music"
		 * ), Music.class); past_music =
		 * JsonAssetManager.getInstance().getEntry(levelFormat.getString("past_music"),
		 * Music.class); //present_music.setVolume(1); present_music.play();
		 * present_music.setLooping(true); //past_music.setVolume(0); past_music.play();
		 * past_music.setLooping(true);
		 */
		populateLevel();
		goalDoor.setOpen(false);
		timeFreeze = false;
		canvas.updateSpriteBatch();
		canvas.resize();
		viewport.getCamera().update();
		viewport.apply();
		stage.getCamera().update();
		stage.getViewport().apply();


	}

	protected void exitGame() {
		listener.exitScreen(this, ScreenExitCodes.EXIT_QUIT.ordinal());
	}

	protected void exitLevelSelect() {
		listener.exitScreen(this, ScreenExitCodes.EXIT_PREV.ordinal());
	}

	/**
	 * Lays out the game geography.
	 */
	private void populateLevel() {

		// tester stage!
		skin = new Skin(Gdx.files.internal("jsons/uiskin.json"));
		stage = new Stage(viewport);
		Gdx.input.setInputProcessor(stage);//BUGGY CHANGE?
		table = new Table();
		table.setWidth(stage.getWidth());
		table.align(Align.center | Align.top);
		table.setPosition(0, sh);

		//initialize backgrounds
		pastBackgroundTexture = JsonAssetManager.getInstance().getEntry(levelFormat.get("past_background").asString(),
						TextureRegion.class);
		presentBackgroundTexture = JsonAssetManager.getInstance().getEntry(levelFormat.get("present_background").asString(),
				TextureRegion.class);
		bgSprite = new Sprite(presentBackgroundTexture);

		win_room = new TextureRegion(new Texture(Gdx.files.local("textures/gui/level_complete.png")));
		createUI();

		// Initializes the world
		float gravity = levelFormat.getFloat("gravity");
		float[] pSize = levelFormat.get("bounds").asFloatArray();
		world = new World(new Vector2(0, gravity), false);
		bounds = new Rectangle(0, 0, pSize[0], pSize[1]);
		scale.x = canvas.getWidth() / pSize[0];
		scale.y = canvas.getHeight() / pSize[1];
		// Add level goal
		goalDoor = new Door();
		goalDoor.initialize(levelFormat.get("door"));
		// TODO: Delete
		// goalTile =
		// JsonAssetManager.getInstance().getEntry(key.get("texture").asString(),
		// TextureRegion.class);
		// float [] pos = key.get("pos").asFloatArray();
		// goalDoor = new Door(pos[0], pos[1], dwidth, dheight, 0);
		//
		// goalDoor.setBodyType(key.get("bodytype").asString().equals("static")?BodyDef.BodyType.StaticBody
		// : BodyDef.BodyType.DynamicBody);
		// goalDoor.setDensity(key.get("density").asFloat());
		// goalDoor.setFriction(key.get("friction").asFloat());
		// goalDoor.setRestitution(key.get("restitution").asFloat());
		// goalDoor.setSensor(key.getBoolean("sensor"));
		// goalDoor.setTexture(goalTile);
		// goalDoor.setName("goal");
		goalDoor.setDrawScale(scale);
		addObject(goalDoor);

		earthTile = JsonAssetManager.getInstance().getEntry("earth", TextureRegion.class);
		String nameWall = "wall";
		for (int ii = 0; ii < WALLS.length; ii++) {
			PolygonObstacle obj;
			obj = new Platform(WALLS[ii], 0, 0);
			obj.setBodyType(BodyDef.BodyType.StaticBody);
			obj.setDensity(BASIC_DENSITY);
			obj.setFriction(BASIC_FRICTION);
			obj.setRestitution(BASIC_RESTITUTION);
			obj.setDrawScale(scale);
			obj.setTexture(earthTile);
			obj.setName(nameWall);
			// addObject(obj);
		}

		String namePlatform = "platform";
		for (int ii = 0; ii < PLATFORMS.length; ii++) {
			PolygonObstacle obj;
			obj = new Platform(PLATFORMS[ii], 0, 0);
			obj.setBodyType(BodyDef.BodyType.StaticBody);
			obj.setDensity(BASIC_DENSITY);
			obj.setFriction(BASIC_FRICTION);
			obj.setRestitution(BASIC_RESTITUTION);
			obj.setDrawScale(scale);
			obj.setTexture(earthTile);
			obj.setName(namePlatform);
			if (ii <= PLATFORMS.length / 2) {
				obj.setSpace(1);
			}
			if (ii > PLATFORMS.length / 2) {
				obj.setSpace(2);
			}
			// addObject(obj);
		}
		/*float[] newPlatCapsule = levelFormat.get("capsuleshape").asFloatArray();
		float[] newPlatDiamond = levelFormat.get("diamondshape").asFloatArray();
		float[] newPlatRounded = levelFormat.get("roundshape").asFloatArray();
		float[] newSpikes = levelFormat.get("spikeshape").asFloatArray();*/

		float[] newPlatCapsule = {0.4f, 1.1f, 0.5f, 1.1f, 2.6f, 1.1f, 2.7f, 1.1f, 2.6f, 0.6f, 2.0f, 0.3f, 1.1f, 0.3f, 0.5f, 0.6f};
		float[] newPlatDiamond = {0.4f, 1.8f, 0.5f, 1.8f, 2.1f, 1.8f, 2.2f, 1.8f, 1.4f, 0.1f};
		float[] newPlatRounded = {0.4f, 1.4f, 0.8f, 1.7f, 2.1f, 1.7f, 2.4f, 1.4f, 2.3f, 0.8f, 1.7f, 0.3f, 1.1f, 0.3f};
		float[] newSpikes = {0.3f, -0.6f, 0.0f, -0.2f, -0.6f, 0.0f, -0.5f, 0.4f, 0.0f, 0.6f, 0.4f, -0.2f, 0.6f, -0.3f};
		float[] newPlatLongcapsule = {0.4f, 1.1f, 0.5f, 1.1f, 2.7f, 1.1f,
				4.9f, 1.1f, 5.0f, 1.1f,
				4.9f, 0.6f, 4.3f, 0.3f, 3.4f, 0.3f,
				2.7f, 0.5f, 2.0f, 0.3f, 1.1f, 0.3f, 0.5f, 0.6f};
		float[] newPlatTall = {0.4f, 3.9f, 0.5f, 3.9f, 1.6f, 3.9f, 1.7f, 3.9f, 1.1f, 0.5f};
		float[] newPlatPillar = {1.2f, 4.0f, 1.3f, 4.0f, 2.0f, 4.0f, 2.1f, 4.0f, 2.1f, 1.0f, 1.2f, 1.0f};


		JsonValue capsule = levelFormat.get("capsules").child();
		while (capsule != null) {
			Platform obj = new Platform(newPlatCapsule);
			obj.initialize(capsule);
			obj.setDrawScale(scale);
			addObject(obj);
			capsule = capsule.next();
		}

		JsonValue longcapsule = levelFormat.get("longcapsules").child();
		while (longcapsule != null) {
			Platform obj = new Platform(newPlatLongcapsule);
			obj.initialize(longcapsule);
			obj.setDrawScale(scale);
			addObject(obj);
			longcapsule = longcapsule.next();
		}

		JsonValue pillar = levelFormat.get("pillars").child();
		while (pillar != null) {
			Platform obj = new Platform(newPlatPillar);
			obj.initialize(pillar);
			obj.setDrawScale(scale);
			addObject(obj);
			pillar = pillar.next();
		}

		JsonValue tall = levelFormat.get("talls").child();
		while (tall != null) {
			Platform obj = new Platform(newPlatTall);
			obj.initialize(tall);
			obj.setDrawScale(scale);
			addObject(obj);
			tall = tall.next();
		}
		// TODO: Delete
		// for (int ii = 0; ii < PRESENT_CAPSULES.length; ii++) {
		// PolygonObstacle obj;
		// obj = new Platform(newPlatCapsule, PRESENT_CAPSULES[ii][0],
		// PRESENT_CAPSULES[ii][1]);
		// obj.setBodyType(BodyDef.BodyType.StaticBody);
		// obj.setDensity(BASIC_DENSITY);
		// obj.setFriction(BASIC_FRICTION);
		// obj.setRestitution(BASIC_RESTITUTION);
		// obj.setDrawScale(scale);
		// obj.setTexture(JsonAssetManager.getInstance().getEntry("present_capsule",
		// TextureRegion.class));
		// obj.setName("present_capsule");
		// obj.setSpace(1);
		// addObject(obj);
		// }
		JsonValue diamond = levelFormat.get("diamonds").child();
		while (diamond != null) {
			Platform obj = new Platform(newPlatDiamond);
			obj.initialize(diamond);
			obj.setDrawScale(scale);
			addObject(obj);
			diamond = diamond.next();
		}
		// TODO:Delete
		// for (int ii = 0; ii < PRESENT_DIAMONDS.length; ii++) {
		// PolygonObstacle obj;
		// obj = new Platform(newPlatDiamond, PRESENT_DIAMONDS[ii][0],
		// PRESENT_DIAMONDS[ii][1]);
		// obj.setBodyType(BodyDef.BodyType.StaticBody);
		// obj.setDensity(BASIC_DENSITY);
		// obj.setFriction(BASIC_FRICTION);
		// obj.setRestitution(BASIC_RESTITUTION);
		// obj.setDrawScale(scale);
		// obj.setTexture(JsonAssetManager.getInstance().getEntry("present_diamond",
		// TextureRegion.class));
		// obj.setName("present_diamond");
		// obj.setSpace(1);
		// addObject(obj);
		// }
		JsonValue round = levelFormat.get("rounds").child();
		while (round != null) {
			Platform obj = new Platform(newPlatRounded);
			obj.initialize(round);
			obj.setDrawScale(scale);
			addObject(obj);
			round = round.next();
		}
		// TODO:Delete
		// for (int ii = 0; ii < PRESENT_ROUNDS.length; ii++) {
		// PolygonObstacle obj;
		// obj = new Platform(newPlatRounded, PRESENT_ROUNDS[ii][0],
		// PRESENT_ROUNDS[ii][1]);
		// obj.setBodyType(BodyDef.BodyType.StaticBody);
		// obj.setDensity(BASIC_DENSITY);
		// obj.setFriction(BASIC_FRICTION);
		// obj.setRestitution(BASIC_RESTITUTION);
		// obj.setDrawScale(scale);
		// obj.setTexture(JsonAssetManager.getInstance().getEntry("present_round",
		// TextureRegion.class));
		// obj.setName("present_round");
		// obj.setSpace(1);
		// addObject(obj);
		// }
		// float[] newSpikes = {0.3f, -0.6f, 0.0f, -0.2f, -0.6f, 0.0f, -0.5f, 0.4f,
		// 0.0f, 0.6f, 0.4f, -0.2f, 0.6f, -0.3f};

		JsonValue spikes = levelFormat.get("spikes").child();
		while (spikes != null) {
			Spikes obj = new Spikes(newSpikes);
			obj.initialize(spikes);
			obj.setDrawScale(scale);
			addObject(obj);
			spikes = spikes.next();
		}
		// TODO:Delete
		// for (int ii = 0; ii < PAST_ROUNDS.length; ii++) {
		// PolygonObstacle obj;
		// obj = new Platform(newPlatRounded, PAST_ROUNDS[ii][0], PAST_ROUNDS[ii][1]);
		// obj.setBodyType(BodyDef.BodyType.StaticBody);
		// obj.setDensity(BASIC_DENSITY);
		// obj.setFriction(BASIC_FRICTION);
		// obj.setRestitution(BASIC_RESTITUTION);
		// obj.setDrawScale(scale);
		// obj.setTexture(JsonAssetManager.getInstance().getEntry("past_round",
		// TextureRegion.class));
		// obj.setName("past_round");
		// obj.setSpace(2);
		// addObject(obj);
		// }
		// TODO:Delete
		// for (int ii = 0; ii < PAST_DIAMONDS.length; ii++) {
		// PolygonObstacle obj;
		// obj = new Platform(newPlatDiamond, PAST_DIAMONDS[ii][0],
		// PAST_DIAMONDS[ii][1]);
		// obj.setBodyType(BodyDef.BodyType.StaticBody);
		// obj.setDensity(BASIC_DENSITY);
		// obj.setFriction(BASIC_FRICTION);
		// obj.setRestitution(BASIC_RESTITUTION);
		// obj.setDrawScale(scale);
		// obj.setTexture(JsonAssetManager.getInstance().getEntry("past_diamond",
		// TextureRegion.class));
		// obj.setName("past_diamond");
		// obj.setSpace(2);
		// addObject(obj);
		// }
		// TODO: Delete
		// for (int ii = 0; ii < PAST_CAPSULES.length; ii++) {
		// PolygonObstacle obj;
		// obj = new Platform(newPlatCapsule, PAST_CAPSULES[ii][0],
		// PAST_CAPSULES[ii][1]);
		// obj.setBodyType(BodyDef.BodyType.StaticBody);
		// obj.setDensity(BASIC_DENSITY);
		// obj.setFriction(BASIC_FRICTION);
		// obj.setRestitution(BASIC_RESTITUTION);
		// obj.setDrawScale(scale);
		// obj.setTexture(JsonAssetManager.getInstance().getEntry("past_capsule",
		// TextureRegion.class));
		// obj.setName("past_capsule");
		// obj.setSpace(2);
		// addObject(obj);
		// }

		// { 1.0f, 4.0f, 3.0f, 4.0f, 3.0f, 2.5f, 1.0f, 2.5f }, { 3.0f, 8.0f, 5.0f, 8.0f,
		// 5.0f, 7.5f, 3.0f, 7.5f },
		// { 5.5f, 4.5f, 7.5f, 4.5f, 7.5f, 5.0f, 5.5f, 5.0f }

		// Create avatar
		JsonValue json = levelFormat.get("avatar");
		avatar = new Avatar();
		avatar.setCanvas(camera);
		avatar.setDrawScale(scale);
		avatar.initialize(json);
		addObject(avatar);
		float[] pos = json.get("pos").asFloatArray();
		avatarStart = new Vector2(pos[0], pos[1]);

		// TODO: Delete
		// avatarTexture =
		// JsonAssetManager.getInstance().getEntry(json.get("texture").asString(),
		// TextureRegion.class);
		// float dwidth = avatarTexture.getRegionWidth() / scale.x;
		// float dheight = avatarTexture.getRegionHeight() / scale.y;

		// avatar = new Avatar(DUDE_POS.x, DUDE_POS.y, dwidth, dheight, scale);

		// avatar.setTexture(avatarTexture);
		// avatar.setBodyType(BodyDef.BodyType.DynamicBody);
		// avatar.setName("avatar");

		// Set film strips to animate avatar states
		// avatarStandingTexture =
		// JsonAssetManager.getInstance().getEntry("avatarstanding", FilmStrip.class);
		// avatarCrouchingTexture =
		// JsonAssetManager.getInstance().getEntry("avatarcrouching", FilmStrip.class);
		// avatarDashingTexture =
		// JsonAssetManager.getInstance().getEntry("avatardashing", FilmStrip.class);
		// avatarFallingTexture =
		// JsonAssetManager.getInstance().getEntry("avatarfalling", FilmStrip.class);
		//
		// avatar.setFilmStrip(Avatar.AvatarState.STANDING, avatarStandingTexture);
		// avatar.setFilmStrip(Avatar.AvatarState.CROUCHING, avatarCrouchingTexture);
		// avatar.setFilmStrip(Avatar.AvatarState.DASHING, avatarDashingTexture);
		// avatar.setFilmStrip(Avatar.AvatarState.FALLING, avatarFallingTexture);

		// Set textures for caught projectiles
		// projPresentCaughtTexture =
		// JsonAssetManager.getInstance().getEntry("projpresentcaught",
		// TextureRegion.class);
		// projPastCaughtTexture =
		// JsonAssetManager.getInstance().getEntry("projpastcaught",
		// TextureRegion.class);
		// avatar.setCaughtProjTexture(PRESENT, projPresentCaughtTexture);
		// avatar.setCaughtProjTexture(PAST, projPastCaughtTexture);

		JsonValue enemy = levelFormat.get("enemies").child();
		while (enemy != null) {
			Enemy obj = new Enemy(avatar, enemy);
			obj.setDrawScale(scale);
			addEnemy(obj);
			numEnemies++;
			enemy = enemy.next();
		}

		// TODO: Delete

		// enemyPresentTexture = JsonAssetManager.getInstance().getEntry("enemypresent",
		// TextureRegion.class);
		// enemyPastTexture = JsonAssetManager.getInstance().getEntry("enemypast",
		// TextureRegion.class);
		// for (int ii = 0; ii < NUMBER_ENEMIES; ii++) {
		// TextureRegion texture;
		// if (TYPE_ENEMIES[ii] == PRESENT)
		// texture = enemyPresentTexture;
		// else
		// texture = enemyPastTexture;
		// float dwidth = texture.getRegionWidth() / scale.x;
		// float dheight = texture.getRegionHeight() / scale.y;
		// Enemy enemy = new Enemy(TYPE_ENEMIES[ii], COOR_ENEMIES[ii][0],
		// COOR_ENEMIES[ii][1], dwidth, dheight, texture,
		// CD_ENEMIES[ii], avatar, scale);
		// enemy.setBodyType(BodyDef.BodyType.DynamicBody);
		// enemy.setDrawScale(scale);
		// enemy.setName("enemy");
		// addObject(enemy);
		// }

		JsonValue turret = levelFormat.get("turrets").child();
		while (turret != null) {
			Enemy obj = new Enemy(turret);
			obj.setDrawScale(scale);
			addEnemy(obj);
			turret = turret.next();
		}
		// turretTexture = JsonAssetManager.getInstance().getEntry("turret",
		// TextureRegion.class);
		// for (int ii = 0; ii < NUMBER_TURRETS; ii++) {
		// TextureRegion texture = turretTexture;
		// float dwidth = texture.getRegionWidth() / scale.x;
		// float dheight = texture.getRegionHeight() / scale.y;
		// Vector2 projDir = new Vector2(DIR_TURRETS[ii][0], DIR_TURRETS[ii][1]);
		// Enemy turret = new Enemy(TYPE_TURRETS[ii], COOR_TURRETS[ii][0],
		// COOR_TURRETS[ii][1], dwidth, dheight, texture,
		// CD_TURRETS[ii], projDir, scale);
		// turret.setBodyType(BodyDef.BodyType.StaticBody);
		// turret.setDrawScale(scale);
		// turret.setName("turret");
		// addObject(turret);
		// }

		collisionController = new CollisionController(this);
		enemyController = new EnemyController(enemies, objects, avatar, world, scale, this, assetDirectory);
		world.setContactListener(collisionController);
	}

	/**
	 * Creates all UI features for a room mode.
	 *
	 */
	public void createUI() {

		// table container to center main table
		Container<Stack> edgeContainer = new Container<Stack>();
		edgeContainer.setSize(sw, sh);
		edgeContainer.setPosition(0, 0);
		edgeContainer.fillX();
		edgeContainer.fillY();

		Stack tableStack = new Stack();

		/*
		 * START PAUSE SCREEN SETUP ---------------------
		 */
		TextureRegionDrawable pauseButtonResource = new TextureRegionDrawable(
				new TextureRegion(new Texture(Gdx.files.internal("textures/gui/pausebutton.png"))));
		TextureRegionDrawable pauseBG = new TextureRegionDrawable(
				new TextureRegion(new Texture(Gdx.files.internal("textures/gui/pause_filter_50_black.png"))));
		TextureRegionDrawable pauseBox = new TextureRegionDrawable(
				new TextureRegion(new Texture(Gdx.files.internal("textures/gui/frame_pause.png"))));
		TextureRegionDrawable resumeResource = new TextureRegionDrawable(
				new TextureRegion(new Texture(Gdx.files.internal("textures/gui/pause_resume_button.png"))));
		TextureRegionDrawable restartResource = new TextureRegionDrawable(
				new TextureRegion(new Texture(Gdx.files.internal("textures/gui/pause_restart_button.png"))));
		TextureRegionDrawable exitResource = new TextureRegionDrawable(
				new TextureRegion(new Texture(Gdx.files.internal("textures/gui/pause_exit_button.png"))));

		pauseButtonContainer = new Container<>();
		pauseButtonContainer.setBackground(pauseBG);
		pauseButtonContainer.setPosition(0, 0);
		pauseButtonContainer.fillX();
		pauseButtonContainer.fillY();

		pauseTable = new Table();
		pauseButtonContainer.setActor(pauseTable);
		pauseButtonContainer.setVisible(false);

		Button resumeButton = new Button(resumeResource);
		resumeButton.addListener(new ClickListener() {
			@Override
			public void clicked(InputEvent event, float x, float y) {
				super.clicked(event, x, y);
				unpauseGame();
			}
		});

		Button restartButton = new Button(restartResource);
		restartButton.addListener(new ClickListener() {
			@Override
			public void clicked(InputEvent event, float x, float y) {
				super.clicked(event, x, y);
				reset();
				unpauseGame();
			}
		});

		Button exitButton = new Button(exitResource);
		exitButton.addListener(new ClickListener() {
			@Override
			public void clicked(InputEvent event, float x, float y) {
				super.clicked(event, x, y);
				exitLevelSelect();
			}
		});
		pauseTable.add(resumeButton).width(sw / 4 / 1.5f).height(sh / 5.1f / 1.5f).center().expandX().padBottom(sh / 20);
		pauseTable.row();
		pauseTable.add(restartButton).width(sw / 4 / 1.5f).height(sh / 5.1f / 1.5f).center().expandX().padBottom(sh / 20);

		pauseTable.row();
		pauseTable.add(exitButton).width(sw / 4 / 1.5f).height(sh / 5.1f / 1.5f).expandX();

		tableStack.add(table);
		tableStack.add(pauseButtonContainer);
		edgeContainer.setActor(tableStack);
		/*
		 * END PAUSE SCREEN SETUP---------------------
		 */

		stage.addActor(edgeContainer);

	}

	public void pauseGame() {
		paused = true;
		// table.setVisible(false);
		pauseButtonContainer.setVisible(true);
		pauseTable.setVisible(true);
	}

	public void unpauseGame() {
		paused = false;
		begincount = 30;
		pauseButtonContainer.setVisible(false);
		pauseTable.setVisible(false);

	}

	public void wonLevel() {

	}

	public PooledList<Obstacle> getObjects() {
		return objects;
	}

	public boolean isShifted() {
		return shifted;
	}

	/**
	 * Returns whether to process the update loop
	 *
	 * At the start of the update loop, we check if it is time to switch to a new
	 * game mode. If not, the update proceeds normally.
	 *
	 * @param dt Number of seconds since last animation frame
	 * 
	 * @return whether to process the update loop
	 */
	public boolean preUpdate(float dt) {

		if (paused || prepause) {
			return false;
		}

		if (complete) {
			avatar.setBodyType(BodyDef.BodyType.StaticBody);
		}
		if (!super.preUpdate(dt)) {
			return false;
		}

		if (!isFailure() && avatar.getY() < -6 || avatar.getEnemyContact()) {
			avatar.removeLife();
			if (avatar.getLives() > 0 ) {
				if (shifted) {
					shifted = false;
					enemyController.shift();
				}
				avatar.setEnemyContact(false);
				avatar.setPosition(avatarStart);
				avatar.getBody().setLinearVelocity(0, 0);
				avatar.setHolding(false);
				avatar.setHeldBullet(null);
				avatar.setBodyType(BodyDef.BodyType.DynamicBody);
				timeFreeze = false;
				return true;
			} else{
				avatar.setPosition(avatarStart);
				avatar.setEnemyContact(false);
				setFailure(true);
			}
			return false;
		}
		if (!isFailure() && avatar.getLives() == 0) {
			setFailure(true);
			return false;
		}

		if (begincount > 0) {
			begincount--;
			return false;
		}

		// enemy.createLineOfSight(world);

		return true;
	}

	/**
	 * Makes the object sleep if it is not in this world
	 *
	 */
	public void sleepIfNotInWorld() {
		enemyController.sleepIfNotInWorld();

		for (Obstacle obj : objects) {

			if (obj instanceof Enemy && ((Enemy) obj).isTurret()) {
				if (obj.getSpace() == 3) {
					obj.setSensor(false);
				} else if (!shifted && (obj.getSpace() == 2)) {
					obj.setSensor(true);
				} else if (shifted && (obj.getSpace() == 1)) {
					obj.setSensor(true);
				}

			} else if (!(obj instanceof Projectile)) {
				obj.setSensor(false);
				if (obj.getSpace() == 3) {
					obj.setSensor(false);
				} else if (!shifted && (obj.getSpace() == 2)) {
					obj.setSensor(true);
				} else if (shifted && (obj.getSpace() == 1)) {
					obj.setSensor(true);
				}
			}
		}
	}

	/**
	 * The core gameplay loop of this world.
	 *
	 * This method contains the specific update code for this mini-game. It does not
	 * handle collisions, as those are managed by the parent class WorldController.
	 * This method is called after input is read, but before collisions are
	 * resolved. The very last thing that it should do is apply forces to the
	 * appropriate objects.
	 *
	 * @param dt Number of seconds since last animation frame
	 */
	public void update(float dt) {
		// Turn the physics engine crank.
		// world.step(WORLD_STEP,WORLD_VELOC,WORLD_POSIT);

		if (InputController.getInstance().didPause() && !paused) {
			pauseGame();
		}

		MusicController.getInstance().update(shifted);

		if (avatar.getShifted() > 0) {
			avatar.setShifted(avatar.getShifted() - 1);
		}

		// test slow down time
		if (timeFreeze) {
			world.step(WORLD_STEP / 8, WORLD_VELOC, WORLD_POSIT);
			// for (Obstacle o : objects) {
			// if (o instanceof Enemy) {
			// ((Enemy) o).slowCoolDown(true);
			// }
			// }
			enemyController.slowCoolDown(true);

		} else {
			world.step(WORLD_STEP, WORLD_VELOC, WORLD_POSIT);
			// for (Obstacle o : objects) {
			// if (o instanceof Enemy) {
			// ((Enemy) o).slowCoolDown(false);
			// }
			// }
			enemyController.slowCoolDown(false);
		}
		avatar.decImmortality();
		int t = avatar.getStartedDashing();
		if (t > 0) {
			t = t - 1;
			avatar.setStartedDashing(t);
		}
		// System.out.println(numEnemies);
		if (enemyController.getEnemies() == 0) {
			goalDoor.setOpen(true);
		} else {
			goalDoor.setOpen(false);
		}

		if (avatar.isHolding()) {
			avatar.setAngle(0);
			timeFreeze = true;
			avatar.resetDashNum(1);
			if (avatar.getBodyType() != BodyDef.BodyType.StaticBody) {
				avatar.setBodyType(BodyDef.BodyType.StaticBody);
			} else if (InputController.getInstance().releasedRightMouseButton()) {
				timeFreeze = false;
				cursor = new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0);
				cursor = camera.unproject(cursor);
				cursor.scl(1/scale.x, 1/scale.y,0);
				Vector2 mousePos = new Vector2(cursor.x , cursor.y );
//				Vector2 mousePos = canvas.getViewport().unproject(InputController.getInstance().getMousePosition());
				avatar.setBodyType(BodyDef.BodyType.DynamicBody);
				avatar.setSticking(false);
				avatar.setWasSticking(false);
				avatar.setDashing(true);
				avatar.setDashStartPos(avatar.getPosition().cpy());
				avatar.setDashDistance(avatar.getDashRange());
				// avatar.setDashDistance(Math.min(avatar.getDashRange(),
				// avatar.getPosition().dst(mousePos)));
				avatar.setDashForceDirection(mousePos.cpy().sub(avatar.getPosition()));
				avatar.setHolding(false);
				createRedirectedProj();
				avatar.setHeldBullet(null); // NOTE: gives error if called before createRedirectedProj()

			}
		}
		if (InputController.getInstance().pressedShiftKey()) {
			// update ripple shader params

			rippleOn = true;
			ticks = 0;
			m_rippleDistance = 0;
			m_rippleRange = 0;


			if (avatar.isSticking()) {
				avatar.resetDashNum(-1);
			}
			shifted = !shifted;
			avatar.setShifted(15);
			if (shifted) {
				JsonValue ripple = assetDirectory.get("sounds").get("ripple_to_past");
				SoundController.getInstance().play(ripple.get("file").asString(), ripple.get("file").asString(), false, EFFECT_VOLUME * 2);
			} else {
				JsonValue ripple = assetDirectory.get("sounds").get("ripple_to_present");
				SoundController.getInstance().play(ripple.get("file").asString(), ripple.get("file").asString(), false, EFFECT_VOLUME * 2);
			}
			// MusicController.getInstance().shift(shifted);
			// avatar.resetDashNum();
			/*
			 * if (!avatar.isHolding()) { avatar.setPosition(avatar.getPosition().x,
			 * avatar.getPosition().y + 0.0001f * sw); }
			 */
			if (avatar.getCurrentPlatform() != null) {
				if (avatar.isSticking()) {
					if (!shifted && (avatar.getCurrentPlatform().getSpace() == 2)) { // past world
						avatar.setSticking(false);
						avatar.setWasSticking(false);
						avatar.setBodyType(BodyDef.BodyType.DynamicBody);
					} else if (shifted && (avatar.getCurrentPlatform().getSpace() == 1)) { // present world
						avatar.setSticking(false);
						avatar.setWasSticking(false);
						avatar.setBodyType(BodyDef.BodyType.DynamicBody);
					}
				}
			}
			// for (Obstacle o: objects) {
			// if (o instanceof Enemy) {
			// Enemy e = (Enemy) o;
			// if (!e.isTurret()) {
			// e.coolDown(false);
			// e.setLeftFixture(null);
			// e.setRightFixture(null);
			// }
			// }
			// }
			enemyController.shift();
		}
		// Check if the platform is in this world or other world. If in the other world,
		// make the platform sleep.
		sleepIfNotInWorld();
		if (InputController.getInstance().didDebug()) {
			debug = !debug;
		}

		// prototype: Dash
		boolean dashAttempt = InputController.getInstance().releasedLeftMouseButton();
		if (dashAttempt) {
			if(avatar.isSticking()){
				avatar.setDashing(false);
				avatar.resetDashes();
			}
			boolean candash = avatar.canDash();
			if(candash){
				cursor = new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0);
				cursor = camera.unproject(cursor);
				cursor.scl(1/scale.x, 1/scale.y,0);
				Vector2 mousePos = new Vector2(cursor.x , cursor.y );
				avatar.setBodyType(BodyDef.BodyType.DynamicBody);
				avatar.setSticking(false);
				avatar.setWasSticking(false);
				avatar.setDashing(true);
				avatar.setDashStartPos(avatar.getPosition().cpy());
				avatar.setDashDistance(Math.min(avatar.getDashRange(), mousePos.cpy().sub(avatar.getPosition()).len()));
				avatar.setDashForceDirection(mousePos.cpy().sub(avatar.getPosition()));
				avatar.setStartedDashing(1);
				if (Math.abs(mousePos.cpy().sub(avatar.getPosition()).angleRad() + Math.PI / 2 - avatar.getAngle()) > Math.PI / 2.5f) {
					avatar.setDimension(avatar.width / 4f, avatar.height / 4f);
					avatar.setDensity(avatar.getDensity() * 16f);
				}

				avatar.decDash();
			}
		}

		if (rippleOn) {

			ticks += time_incr;
			if (ticks > ripple_reset) {
				rippleOn = false;
				ticks = 0;
				m_rippleDistance = 0;
				m_rippleRange = 0;
			}
			m_rippleDistance += rippleSpeed * ticks;
			m_rippleRange = (1 - m_rippleDistance / maxRippleDistance) * 0.009f;
		}

		// Process actions in object model
		avatar.setJumping(InputController.getInstance().didPrimary());
		avatar.setShooting(InputController.getInstance().didSecondary());

		// Sets which direction the avatar is facing (left or right)
		if (InputController.getInstance().pressedLeftMouseButton()) {
			cursor = new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0);
			cursor = camera.unproject(cursor);
			cursor.scl(1/scale.x, 1/scale.y,0);
			Vector2 mousePos = new Vector2(cursor.x , cursor.y );
//			Vector2 mousePos = canvas.getViewport().unproject(InputController.getInstance().getMousePosition());
			Vector2 avatarPos = avatar.getPosition().cpy();
			avatar.setMovement(mousePos.x - avatarPos.x);
		}

		enemyController.processAction();

		// Add bullet if enemy can fire
		// for (Obstacle o : objects) {
		// if (o instanceof Enemy) {
		// Enemy e = (Enemy) o.getBody().getUserData();
		// if (e.isTurret()) {
		// if (e.canFire()) {
		// createBullet(e);
		// } else
		// e.coolDown(true);
		// } else if ((shifted && e.getSpace() == 2) || (!shifted && e.getSpace() == 1))
		// {
		// if (!e.isTurret() && e.getLeftFixture() != null && e.getRightFixture() !=
		// null) {
		// e.createLineOfSight(world, BULLET_OFFSET);
		// e.applyForce();
		// if (e.canFire()) {
		// if (o.getName() == "enemy") {
		// e.setVelocity(BULLET_OFFSET);
		// }
		// createBullet(e);
		// } else
		// e.coolDown(true);
		// }
		// }
		// }
		// }

		avatar.applyForce();
		// enemy.applyForce();

		// Update avatar animation state
		if (InputController.getInstance().pressedLeftMouseButton()
				|| InputController.getInstance().pressedRightMouseButton()) {
			// If either mouse button is held, set animation to be crouching
			avatar.animate(Avatar.AvatarState.CROUCHING, false);
			avatar.setAnimationState(Avatar.AvatarState.CROUCHING);
		} else if (avatar.isSticking()) {
			// Default animation if player is stationary
			avatar.animate(Avatar.AvatarState.STANDING, false);
			avatar.setAnimationState(Avatar.AvatarState.STANDING);
		} else if (avatar.getLinearVelocity().y > 0) {
			avatar.animate(Avatar.AvatarState.DASHING, false);
			avatar.setAnimationState(Avatar.AvatarState.DASHING);
		} else {
			avatar.animate(Avatar.AvatarState.FALLING, false);
			avatar.setAnimationState(Avatar.AvatarState.FALLING);
		}

		// Update enemy animation state
//		enemyController.animateEnemies();

		/*
		 * if (avatar.isJumping()) { JsonValue data =
		 * assetDirectory.get("sounds").get("jump");
		 * SoundController.getInstance().play("jump", data.get("file").asString(),
		 * false, data.get("volume").asFloat()); }
		 */

		// If we use sound, we must remember this.
		SoundController.getInstance().update();

		// Print location of the mouse position when 'X' key is pressed
		// so we can know where to spawn enemies for testing purposes.
		printCoordinates();
	}

	/**
	 * 
	 * Add a new bullet to the world and send it in the direction specified by the
	 * turret it originated from. Processes physics
	 *
	 * Once the update phase is over, but before we draw, we are ready to handle
	 * physics. The primary method is the step() method in world. This
	 * implementation works for all applications and should not need to be
	 * overwritten.
	 *
	 * If the avatar is sticking, sets the avatar's velocity to zero and makes the
	 * avatar's body type static so that it sticks.
	 *
	 * @param dt Number of seconds since last animation frame
	 */
	public void postUpdate(float dt) {
		super.postUpdate(dt);
		if (avatar.getStartedDashing() > 0) {
			avatar.setStartedDashing(0);
		}
		if (rippleOn) {
			updateShader(false);
		}

		if (avatar.isSticking() && !avatar.getWasSticking()) {
			avatar.setDashing(false);
			avatar.getBody().setLinearVelocity(0, 0);
			avatar.getBody().setAngularVelocity(0);
			avatar.setBodyType(BodyDef.BodyType.StaticBody);
			avatar.setWasSticking(true);
			avatar.setAngle(avatar.getNewAngle());
		}
	}

	// /**
	// * Add a new bullet to the world and send it in the right direction.
	// *
	// * @param enemy enemy
	// */
	// private void createBullet(Enemy enemy) {
	// float offset = BULLET_OFFSET;
	//
	//// //TODO: quick fix for enemy projectile offsets
	//// if (!enemy.isTurret() && enemy.getType() == PAST) {
	//// offset = 2.5f;
	//// }
	//// if (!enemy.isTurret() && enemy.getType() == PRESENT) {
	//// offset = 1.5f;
	//// }
	//
	// bulletBigTexture = JsonAssetManager.getInstance().getEntry("bulletbig",
	// TextureRegion.class);
	// presentBullet = JsonAssetManager.getInstance().getEntry("projpresent",
	// TextureRegion.class);
	// pastBullet = JsonAssetManager.getInstance().getEntry("projpast",
	// TextureRegion.class);
	// float radius = bulletBigTexture.getRegionWidth() / (30.0f);
	// Projectile bullet = new Projectile(enemy.getType(), enemy.getX(),
	// enemy.getY() + offset, radius,
	// enemy.getBody().getUserData());
	//
	// Filter f = new Filter();
	// f.groupIndex = -1;
	// enemy.setFilterData(f);
	// bullet.setFilterData(f);
	//
	// bullet.setName("bullet");
	// bullet.setDensity(HEAVY_DENSITY);
	// bullet.setDrawScale(scale);
	// //bullet.setTexture(bulletBigTexture);
	// bullet.setBullet(true);
	// bullet.setGravityScale(0);
	// bullet.setLinearVelocity(enemy.getProjVelocity());
	// bullet.setSpace(enemy.getSpace());
	// addQueuedObject(bullet);
	// if (bullet.getType().equals(PRESENT)){
	// bullet.setTexture(presentBullet);
	// } else {
	// bullet.setTexture(pastBullet);
	// }
	//
	// if (shifted && enemy.getSpace() == 2) { // past world
	// JsonValue data = assetDirectory.get("sounds").get("pew");
	//// System.out.println("sound volume: " + data.get("volume").asFloat());
	// SoundController.getInstance().play("pew", data.get("file").asString(), false,
	// data.get("volume").asFloat());
	// } else if (!shifted && enemy.getSpace() == 1) { // present world
	// JsonValue data = assetDirectory.get("sounds").get("pew");
	//// System.out.println("sound volume: " + data.get("volume").asFloat());
	// SoundController.getInstance().play("pew", data.get("file").asString(), false,
	// data.get("volume").asFloat());
	// }
	//
	// // Reset the firing cooldown.
	// enemy.coolDown(false);
	// }

	/**
	 * Add a new bullet to the world and send it in the right direction.
	 */
	protected void createRedirectedProj() {
		cursor = new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0);
		cursor = camera.unproject(cursor);
		cursor.scl(1/scale.x, 1/scale.y,0);
		Vector2 mousePos = new Vector2(cursor.x , cursor.y );
//		Vector2 mousePos = canvas.getViewport().u
//		nproject(InputController.getInstance().getMousePosition());
		Vector2 redirection = avatar.getPosition().cpy().sub(mousePos).nor();
		float x0 = avatar.getX() + (redirection.x * avatar.getWidth() * 1.5f);
		float y0 = avatar.getY() + (redirection.y * avatar.getHeight() * 1.5f);
		bulletBigTexture = JsonAssetManager.getInstance().getEntry("bulletbig", TextureRegion.class);
		float radius = bulletBigTexture.getRegionWidth() / (2.0f * scale.x);
		Vector2 projVel = redirection.cpy().scl(12);
		EntityType projType = avatar.getHeldBullet().getType();

		Projectile bullet = new Projectile(projType, x0, y0, radius, avatar.getBody().getUserData());
		bullet.setName("bullet");
		bullet.setDensity(HEAVY_DENSITY);
		bullet.setDrawScale(scale);
		bullet.setSensor(true);
		// bullet.setTexture(bulletBigTexture);
		if (bullet.getType() == PRESENT) {
			presentBullet = JsonAssetManager.getInstance().getEntry("projpresent", TextureRegion.class);
			bullet.setTexture(presentBullet);
		} else {
			pastBullet = JsonAssetManager.getInstance().getEntry("projpast", TextureRegion.class);
			bullet.setTexture(pastBullet);
		}
		bullet.setBullet(true);
		bullet.setGravityScale(0);
		bullet.setLinearVelocity(projVel);
		if (shifted)
			bullet.setSpace(2); // past world
		else
			bullet.setSpace(1); // present world
		addQueuedObject(bullet);
		JsonValue pew = assetDirectory.get("sounds").get("pew");
		SoundController.getInstance().play(pew.get("file").asString(), pew.get("file").asString(), false, pew.get("volume").asFloat());
	}

	/**
	 * Prints the (x,y) coordinate of the current mouse position when the 'X' key is
	 * pressed. For debug purposes.
	 */
	private void printCoordinates() {
		if (InputController.getInstance().pressedXKey()) {
			cursor = new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0);
			cursor = camera.unproject(cursor);
			cursor.scl(1/scale.x, 1/scale.y,0);
			Vector2 mousePos = new Vector2(cursor.x , cursor.y );
			System.out.println("Mouse position: " + mousePos);
		}
	}

	/**
	 * Draws an object if it is in this world
	 *
	 */
	public void drawObjectInWorld() {
		for (Obstacle obj : objects) {
			if (obj.getSpace() == 3) {
				obj.draw(canvas);
			} else if (shifted && (obj.getSpace() == 2)) { // past world
				obj.draw(canvas);
			} else if (!shifted && (obj.getSpace() == 1)) { // present world
				obj.draw(canvas);
			}
		}

		enemyController.drawEnemiesInWorld();
	}

	@Override
	public void render(float delta) {
		canvas.updateSpriteBatch();
		camera.update();
		stage.getBatch().setProjectionMatrix(stage.getCamera().combined);
		stage.getCamera().update();
		super.render(delta);
	}

	/**
	 * Draws a line indicating the direction and distance of the dash.
	 */
	public void drawIndicator(GameCanvas canvas) {
		if (!InputController.getInstance().pressedLeftMouseButton()
				&& !InputController.getInstance().pressedRightMouseButton())
			return;
		if (InputController.getInstance().pressedRightMouseButton() && !avatar.isHolding()) {
			return;
		}

		if (!avatar.canDash() && !avatar.isSticking() && !avatar.isHolding())
			return;
		// Draw dynamic dash indicator
		cursor = new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0);
		cursor = camera.unproject(cursor);
		cursor.scl(1/scale.x, 1/scale.y,0);
		Vector2 mousePos = new Vector2(cursor.x , cursor.y );
		Vector2 redirection = avatar.getPosition().cpy().sub(mousePos).nor();
		TextureRegion circle = JsonAssetManager.getInstance().getEntry("circle", TextureRegion.class);
		TextureRegion arrow = JsonAssetManager.getInstance().getEntry("arrow", TextureRegion.class);
		Vector2 avPos = avatar.getPosition();
		Vector2 mPos = mousePos;
		Vector2 startPos = avPos.cpy().scl(scale);
		mousePos = mPos.cpy().scl(scale);
		float dist = Math.min(avatar.getDashRange(), avPos.dst(mPos));
		if (!avatar.isHolding()) {
			canvas.draw(circle, Color.WHITE, circle.getRegionWidth() / 2, circle.getRegionHeight() / 2,
					avatar.getX() * scale.x, avatar.getY() * scale.y, redirection.angle() / 57, 0.0095f * scale.x * dist* 1.5f,
					0.0095f * scale.y * dist* 1.5f);
			canvas.draw(arrow, Color.WHITE, 0, arrow.getRegionHeight() / 2, avatar.getX() * scale.x, avatar.getY() * scale.y,
					(180 + redirection.angle()) / 57, 0.0075f * scale.x * dist* 1.5f, 0.0075f * scale.y * dist* 1.5f);
		} else {
			canvas.draw(arrow, Color.WHITE, 0, arrow.getRegionHeight() / 2, avatar.getX() * scale.x, avatar.getY() * scale.y,
					(180 + redirection.angle()) / 57, 0.0075f * scale.x * avatar.getDashRange()* 1.5f,
					0.0075f * scale.y * avatar.getDashRange()* 1.5f);
		}
		//canvas.end();
		// If player is holding a projectile, draw projectile indicator
		// TODO: need to fix - line is a bit off
		Vector2 projDir = startPos.cpy().sub(mousePos).scl(scale);
		// System.out.println("HOLDING: " + avatar.isHolding());*/
		TextureRegion projCircle = JsonAssetManager.getInstance().getEntry("projectile_circle", TextureRegion.class);
		TextureRegion projArrow = JsonAssetManager.getInstance().getEntry("projectile_arrow", TextureRegion.class);

		//canvas.begin();
		if (avatar.isHolding()) {
			canvas.draw(projCircle, Color.GOLD, projCircle.getRegionWidth() / 2, projCircle.getRegionHeight() / 2,
					avatar.getX() * scale.x, avatar.getY() * scale.y, redirection.angle() / 57, 0.0073f * scale.x* 1.5f,
					0.0073f * scale.y* 1.5f);
			canvas.draw(projArrow, Color.GOLD, 0, projArrow.getRegionHeight() / 2, avatar.getX() * scale.x,
					avatar.getY() * scale.y, (redirection.angle()) / 57, 0.0061f * scale.x * 1.5f, 0.0061f * scale.y* 1.5f);
		}
	}

	public void drawLives(GameCanvas canvas) {
		TextureRegion life = JsonAssetManager.getInstance().getEntry("life", TextureRegion.class);
		TextureRegion streak = JsonAssetManager.getInstance().getEntry("streak", TextureRegion.class);
		canvas.draw(streak, Color.WHITE, 0, 0, -0.8f * scale.x, canvas.getHeight() / 2 + 2 * scale.y, 9 * scale.x,
				9 * scale.y);
		for (int i = 0; i < avatar.getLives(); i++) {
			canvas.draw(life, Color.WHITE, 0, 0,
					life.getRegionWidth() * 0.002f * scale.x + (life.getRegionWidth() * 0.005f * scale.x * i),
					canvas.getHeight() - life.getRegionHeight() * 0.007f * scale.y, scale.x, scale.y);
		}
	}

	public void updateShader(boolean failedState) {

		if(!failedState){
			prev_m_rippleDistance = m_rippleRange;
			m_rippleRange = (1 - m_rippleDistance / maxRippleDistance) * 0.02f;
		}
		// write to shader
		shaderprog.begin();
		shaderprog.setUniformf("time", ticks);

		// shaderprog.setUniformf("mousePos", new Vector2(0 * scale.x /
		// sw , 0 * scale.y / sh));
		shaderprog.setUniformf("mousePos", new Vector2(avatar.getPosition().x * scale.x / sw,
				(DEFAULT_HEIGHT - avatar.getPosition().y) * scale.y / sh));
		shaderprog.setUniformf("deltax", Math.abs(delta_x / 100));
		shaderprog.setUniformf("deltay", Math.abs(delta_y / 100));
		// update ripple params
		shaderprog.setUniformf("u_rippleDistance", m_rippleDistance);
		shaderprog.setUniformf("u_rippleRange", m_rippleRange);
		shaderprog.end();

		m_rippleRange = prev_m_rippleDistance;
	}

	/**
	 * Draws the debug of an object if it is in this world
	 *
	 */
	public void drawDebugInWorld() {
		for (Obstacle obj : objects) {
			if (obj.getSpace() == 3) {
				obj.drawDebug(canvas);
			} else if (shifted && (obj.getSpace() == 2)) {
				obj.drawDebug(canvas);
			} else if (!shifted && (obj.getSpace() == 1)) {
				obj.drawDebug(canvas);
			}
		}

		enemyController.drawEnemiesDebugInWorld();
	}

	/**
	 * Draw the physics object360s to the canvas
	 *
	 * For simple worlds, this method is enough by itself. It will need to be
	 * overriden if the world needs fancy backgrounds or the like.
	 *
	 * The method draws all objects in the order that they were added.
	 *
	 * @param delta The drawing context
	 */
	public void draw(float delta) {


		// Final message
		if (complete && !failed) {
			if(GameStateManager.getInstance().lastRoom()){
				canvas.begin();
				displayFont.setColor(Color.YELLOW);
				canvas.draw(win_room,Color.WHITE, 0f,0f, (float)sw, (float) sh);
				canvas.end();
//				reset();
			}else{
				stage.addAction(Actions.sequence(Actions.fadeOut(0.3f), Actions.run(new Runnable() {
					@Override
					public void run() {
						exitNextRoom();
					}
				})));
			}
		} else if (failed) {
			canvas.begin();
			displayFont.setColor(Color.WHITE);
			canvas.drawTextCentered("FAILURE", displayFont, 0.0f);
			canvas.end();
		}

		canvas.updateSpriteBatch();
		canvas.clear();

		//VIEWPORT UPDATES
		batch.setProjectionMatrix(canvas.getViewport().getCamera().combined);
		stage.getBatch().setProjectionMatrix(stage.getCamera().combined);
		stage.getCamera().update();

		// render batch with shader
		batch.begin();
		if (rippleOn) {
			updateShader(false);
			batch.setShader(shaderprog);
		}
		if (shifted) {
			bgSprite.setRegion(pastBackgroundTexture);
		} else {
			bgSprite.setRegion(presentBackgroundTexture);
		}
		batch.draw(bgSprite, 0, 0, sw, sh);
		batch.end();



		canvas.begin();



		drawObjectInWorld();
		drawIndicator(canvas);

		if(!isTutorial){
			drawLives(canvas);
		}

		if (debug) {
			canvas.beginDebug();
			drawDebugInWorld();
			canvas.endDebug();
		}

		canvas.end();

		stage.act(Gdx.graphics.getDeltaTime());
		stage.draw();



	}

	/** Unused ContactListener method */
	public void postSolve(Contact contact, ContactImpulse impulse) {
	}

	/** Unused ContactListener method */
	public void preSolve(Contact contact, Manifold oldManifold) {
	}

	@Override
	public void resize(int width, int height) {
		//viewport UPDATES
		viewport.update(width, height);
		viewport.apply();
		camera.update();
		stage.getViewport().update(width, height);
		stage.getCamera().viewportWidth = sw;
		stage.getCamera().viewportHeight = sh;
		stage.getCamera().position.set(stage.getCamera().viewportWidth / 2, stage.getCamera().viewportHeight / 2, 0);
		stage.getCamera().update();
		super.resize(width, height);
	}

	public void exitNextRoom(){
		listener.exitScreen(this, ScreenExitCodes.EXIT_NEXT.ordinal());
	}

	/**
	 * @return avatar object
	 */
	public Avatar getAvatar() {
		return avatar;
	}

	public PooledList<Enemy> getEnemies() {
		return enemies;
	}

	/**
	 * @return goal door object
	 */
	public BoxObstacle getGoalDoor() {
		return goalDoor;
	}

	public void removeEnemy() {
		numEnemies--;
		if (numEnemies < 0) {
			numEnemies = 0;
		}
	}

	public boolean getShifted() {
		return shifted;
	}

	public void playMusic(int level) {
		String past = "past2";
		String present = "present2";
		if (level == 1) {
			past = "past1";
			present = "present1";
		} else if (level == 2) {
			past = "past2";
			present = "present2";
		} else if (level == 3) {
			past = "past3";
			present = "present3";
		} else if (level == 4) {
			past = "past4";
			present = "present4";
		}
		JsonValue pastMus = assetDirectory.get("music").get(past);
		MusicController.getInstance().play("past", pastMus.get("file").asString(), true, 0.0f);
		JsonValue presentMus = assetDirectory.get("music").get(present);
		MusicController.getInstance().play("present", presentMus.get("file").asString(), true, 1.0f);
	}

}