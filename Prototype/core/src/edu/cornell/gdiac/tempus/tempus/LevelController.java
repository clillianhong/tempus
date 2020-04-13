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
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g2d.freetype.FreetypeFontLoader;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.*;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import edu.cornell.gdiac.tempus.InputController;
import edu.cornell.gdiac.tempus.WorldController;
import edu.cornell.gdiac.util.JsonAssetManager;
import edu.cornell.gdiac.tempus.GameCanvas;
import edu.cornell.gdiac.tempus.obstacle.*;
import edu.cornell.gdiac.tempus.tempus.models.*;
import edu.cornell.gdiac.util.FilmStrip;
import edu.cornell.gdiac.util.PooledList;
import edu.cornell.gdiac.util.SoundController;

import static edu.cornell.gdiac.tempus.tempus.models.EntityType.PAST;
import static edu.cornell.gdiac.tempus.tempus.models.EntityType.PRESENT;

/**
 * Gameplay specific controller for the platformer game.
 *
 * You will notice that asset loading is not done with static methods this time.
 * Instance asset loading makes it easier to process our game modes in a loop,
 * which is much more scalable. However, we still want the assets themselves to
 * be static. This is the purpose of our AssetState variable; it ensures that
 * multiple instances place nicely with the static assets.
 */
public class LevelController extends WorldController {

	/** Checks if did debug */
	private boolean debug;

	/** The sound file for a bullet fire */
	private static final String PEW_FILE = "sounds/pew.mp3";

	/** The texture for walls and platforms */
	protected TextureRegion earthTile;
	/** The texture for the exit condition */
	protected TextureRegion goalTile;
	/** The font for giving messages to the player */
	protected BitmapFont displayFont;

	/** Texture asset for the big bullet */
	private TextureRegion bulletBigTexture;
	private TextureRegion presentBullet = JsonAssetManager.getInstance().getEntry("projpresent", TextureRegion.class);
	private TextureRegion pastBullet = JsonAssetManager.getInstance().getEntry("projpast", TextureRegion.class);
	/** Texture aobjects.sset for the turret */
	private TextureRegion turretTexture;
	/** Texture asset for present enemy */
	private TextureRegion enemyPresentTexture;
	/** Texture asset for past enemy */
	private TextureRegion enemyPastTexture;

	/** Texture asset for the background */
	private TextureRegion backgroundTexture;

	/** The reader to process JSON files */
	private JsonReader jsonReader;
	/** The JSON asset directory */
	private JsonValue assetDirectory;
	/** The JSON defining the level model */
	private JsonValue levelFormat;

	/** Track asset loading from all instances and subclasses */
	private AssetState platformAssetState = AssetState.EMPTY;
	/** Freeze time */
	private boolean timeFreeze;

	/**
	 * Preloads the assets for this controller.
	 *
	 * To make the game modes more for-loop friendly, we opted for nonstatic loaders
	 * this time. However, we still want the assets themselves to be static. So we
	 * have an AssetState that determines the current loading state. If the assets
	 * are already loaded, this method will do nothing.
	 * 
	 * @param manager Reference to global asset manager.
	 */
	public void preLoadContent(AssetManager manager) {
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
	private static final float DEFAULT_GRAVITY = -14.7f;
	/** The density for most physics objects */
	private static final float BASIC_DENSITY = 0.0f;
	/** The density for a bullet */
	private static final float HEAVY_DENSITY = 10.0f;
	/** Friction of most platforms */
	private static final float BASIC_FRICTION = 0.6f;
	/** The restitution for all physics objects */
	private static final float BASIC_RESTITUTION = 0.1f;
	/** Offset for bullet when firing */
	private static final float BULLET_OFFSET = 1.0f;
	/** The volume for sound effects */
	private static final float EFFECT_VOLUME = 0.8f;

	// Since these appear only once, we do not care about the magic numbers.
	// In an actual game, this information would go in a data file.
	// Wall vertices
	private static final float[][] WALLS = { { 0.0f, 0.0f, 1.0f, 0.0f, 1.0f, 18.0f, 0.0f, 18.0f },
			{ 1.0f, 18.0f, 1.0f, 17.0f, 31.0f, 17.0f, 31.0f, 18.0f },
			{ 31.0f, 18.0f, 31.0f, 0.0f, 32.0f, 0.0f, 32.0f, 18.0f } };
	/*
	 * {16.0f, 18.0f, 16.0f, 17.0f, 1.0f, 17.0f, 1.0f, 0.0f, 0.0f, 0.0f, 0.0f,
	 * 18.0f}, {32.0f, 18.0f, 32.0f, 0.0f, 31.0f, 0.0f, 31.0f, 17.0f, 16.0f, 17.0f,
	 * 16.0f, 18.0f} };
	 */

	/** The outlines of all of the platforms */
	private static final float[][] PLATFORMS = {

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
	private static final float[][] PRESENT_CAPSULES = { { 3.0f, 7.0f }, { 6.0f, 4.0f }, { 24.0f, 11.5f } };
	/** The positions of all present diamond platforms */
	private static final float[][] PRESENT_DIAMONDS = { { 1.0f, 2.0f }, { 11.0f, 7.0f } };
	/** The positions of all present rounded platforms */
	private static final float[][] PRESENT_ROUNDS = { { 11.5f, 2.0f }, { 9.5f, 13.0f } };
	/** The positions of all past capsule platforms */
	private static final float[][] PAST_CAPSULES = { { 4.5f, 1.0f }, { 14.5f, 9.0f } };
	/** The positions of all past diamond platforms */
	private static final float[][] PAST_DIAMONDS = { { 13.5f, 3.0f }, { 20.0f, 5.0f } };
	/** The positions of all past rounded platforms */
	private static final float[][] PAST_ROUNDS = { { 2.0f, 13.0f }, { 18.5f, 13.0f } };

	// Other game objects
	/** The goal door position */
	private static Vector2 GOAL_POS = new Vector2(29.5f, 15.5f);
	/** The initial position of the dude */
	private static Vector2 DUDE_POS = new Vector2(2.5f, 5.0f);
	/** The initial position of the turret */
	private static Vector2 TURRET_POS = new Vector2(8.5f, 10.0f);
	// /** The initial position of the enemy */
	// private static Vector2 ENEMY_POS = new Vector2(13.0f, 7.5f);

	// Physics objects for the game
	/** Reference to the character avatar */
	private Avatar avatar;
	/** Bucket variable for holding lives across resets */
	private int lives;
	/** Reference to the goalDoor (for collision detection) */
	private Door goalDoor;

	/** The information of all the enemies */
	private int NUMBER_ENEMIES = 2;
	private EntityType[] TYPE_ENEMIES = { PRESENT, PAST };
	private float[][] COOR_ENEMIES = { { 13.0f, 6.0f }, { 15.625f, 11.03125f } };
	private int[] CD_ENEMIES = { 80, 80 };

	/** The information of all the turrets */
	private int NUMBER_TURRETS = 2;
	private EntityType[] TYPE_TURRETS = { PRESENT, PAST };
	private float[][] COOR_TURRETS = { { TURRET_POS.x + 10.0f, TURRET_POS.y + 0.3f },
			{ TURRET_POS.x, TURRET_POS.y - 5.0f } };
	private float[][] DIR_TURRETS = { // direction of proj which the turrets shoot
			{ -3.0f, 0 }, { 0, 2.0f } };
	private int[] CD_TURRETS = { 90, 120 };

	private Enemy enemy;

	/** Whether the avatar is shifted to the other world or not */
	private boolean shifted;

	/** Collision Controller instance **/
	protected CollisionController collisionController;

	/** FILEPATH TO JSON RESOURCE FOR THE LEVEL **/
	private String json_filepath;

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
		debug = false;
		timeFreeze = false;
		lives = 0;
		json_filepath = json;
	}

	/**
	 * Resets the status of the game so that we can play again.
	 *
	 * This method disposes of the world and creates a new one.
	 */
	public void reset() {
		//Vector2 gravity = new Vector2(world.getGravity());

		for (Obstacle obj : objects) {
			obj.deactivatePhysics(world);
		}
		objects.clear();
		addQueue.clear();
		world.dispose();
		shifted = false;
		//world = new World(gravity, false);
		world.setContactListener(collisionController);
		// world.setContactListener(this);
		setComplete(false);
		setFailure(false);
		levelFormat = jsonReader.parse(Gdx.files.internal(json_filepath));
//		levelFormat = jsonReader.parse(Gdx.files.internal("jsons/test_level_editor.json"));

		populateLevel();
		timeFreeze = false;
		if (lives > 0) {
			avatar.setLives(lives);
		}
		lives = 0;
	}

	private Skin skin;
	private Stage stage;

	private Table table;
	private Button quitButton;

	private void exitGame() {
		listener.exitScreen(this, EXIT_QUIT);
	}

	private void exitBack() {
		listener.exitScreen(this, ScreenExitCodes.EXIT_PREV.ordinal());
	}


	/**
	 * Lays out the game geography.
	 */
	private void populateLevel() {

		float sw = Gdx.graphics.getWidth();
		float sh = Gdx.graphics.getHeight();

		// tester stage!
		skin = new Skin(Gdx.files.internal("jsons/uiskin.json"));
		stage = new Stage(new ScreenViewport());
		table = new Table();
		table.setWidth(stage.getWidth());
		table.align(Align.center | Align.top);
		table.setPosition(0, Gdx.graphics.getHeight());
//		TextureRegionDrawable qBuResource = new TextureRegionDrawable(new TextureRegion(new Texture(Gdx.files.internal("textures/gui/white_quit.png"))));
//		quitButton = new Button(qBuResource);
//		quitButton.addListener(new ClickListener() {
//			@Override
//			public void clicked(InputEvent event, float x, float y) {
//				exitGame();
//			}
//		});
		TextureRegionDrawable backButtonResource = new TextureRegionDrawable(new TextureRegion(new Texture(Gdx.files.internal("textures/gui/selectmode/backbutton.png"))));
		Button backButton = new Button(backButtonResource);
		backButton.addListener(new ClickListener() {
			@Override
			public void clicked(InputEvent event, float x, float y) {
				exitBack();
			}
		});
		table.add(backButton).width(sw/15f).height(sw/15f).expand().right();
		stage.addActor(table);
		Gdx.input.setInputProcessor(stage);

		//Initializes the world
		float gravity = levelFormat.getFloat("gravity");
		float[] pSize = levelFormat.get("bounds").asFloatArray();
		world = new World (new Vector2(0,gravity),false);
		bounds = new Rectangle(0,0,pSize[0],pSize[1]);
		scale.x = canvas.getWidth()/pSize[0];
		scale.y = canvas.getHeight()/pSize[1];
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
		float[] newPlatCapsule = levelFormat.get("capsuleshape").asFloatArray();
		float[] newPlatDiamond = levelFormat.get("diamondshape").asFloatArray();
		float[] newPlatRounded = levelFormat.get("roundshape").asFloatArray();
		JsonValue capsule = levelFormat.get("capsules").child();
		while (capsule!=null){
			Platform obj = new Platform (newPlatCapsule);
			obj.initialize(capsule);
			obj.setDrawScale(scale);
			addObject(obj);
			capsule = capsule.next();
		}
		//TODO: Delete
//		for (int ii = 0; ii < PRESENT_CAPSULES.length; ii++) {
//			PolygonObstacle obj;
//			obj = new Platform(newPlatCapsule, PRESENT_CAPSULES[ii][0], PRESENT_CAPSULES[ii][1]);
//			obj.setBodyType(BodyDef.BodyType.StaticBody);
//			obj.setDensity(BASIC_DENSITY);
//			obj.setFriction(BASIC_FRICTION);
//			obj.setRestitution(BASIC_RESTITUTION);
//			obj.setDrawScale(scale);
//			obj.setTexture(JsonAssetManager.getInstance().getEntry("present_capsule", TextureRegion.class));
//			obj.setName("present_capsule");
//			obj.setSpace(1);
//			addObject(obj);
//		}
		JsonValue diamond = levelFormat.get("diamonds").child();
		while (diamond!=null){
			Platform obj = new Platform (newPlatDiamond);
			obj.initialize(diamond);
			obj.setDrawScale(scale);
			addObject(obj);
			diamond = diamond.next();
		}
		//TODO:Delete
//		for (int ii = 0; ii < PRESENT_DIAMONDS.length; ii++) {
//			PolygonObstacle obj;
//			obj = new Platform(newPlatDiamond, PRESENT_DIAMONDS[ii][0], PRESENT_DIAMONDS[ii][1]);
//			obj.setBodyType(BodyDef.BodyType.StaticBody);
//			obj.setDensity(BASIC_DENSITY);
//			obj.setFriction(BASIC_FRICTION);
//			obj.setRestitution(BASIC_RESTITUTION);
//			obj.setDrawScale(scale);
//			obj.setTexture(JsonAssetManager.getInstance().getEntry("present_diamond", TextureRegion.class));
//			obj.setName("present_diamond");
//			obj.setSpace(1);
//			addObject(obj);
//		}
		JsonValue round = levelFormat.get("rounds").child();
		while (round!=null){
			Platform obj = new Platform (newPlatRounded);
			obj.initialize(round);
			obj.setDrawScale(scale);
			addObject(obj);
			round = round.next();
		}
		//TODO:Delete
//		for (int ii = 0; ii < PRESENT_ROUNDS.length; ii++) {
//			PolygonObstacle obj;
//			obj = new Platform(newPlatRounded, PRESENT_ROUNDS[ii][0], PRESENT_ROUNDS[ii][1]);
//			obj.setBodyType(BodyDef.BodyType.StaticBody);
//			obj.setDensity(BASIC_DENSITY);
//			obj.setFriction(BASIC_FRICTION);
//			obj.setRestitution(BASIC_RESTITUTION);
//			obj.setDrawScale(scale);
//			obj.setTexture(JsonAssetManager.getInstance().getEntry("present_round", TextureRegion.class));
//			obj.setName("present_round");
//			obj.setSpace(1);
//			addObject(obj);
//		}

		//TODO:Delete
//		for (int ii = 0; ii < PAST_ROUNDS.length; ii++) {
//			PolygonObstacle obj;
//			obj = new Platform(newPlatRounded, PAST_ROUNDS[ii][0], PAST_ROUNDS[ii][1]);
//			obj.setBodyType(BodyDef.BodyType.StaticBody);
//			obj.setDensity(BASIC_DENSITY);
//			obj.setFriction(BASIC_FRICTION);
//			obj.setRestitution(BASIC_RESTITUTION);
//			obj.setDrawScale(scale);
//			obj.setTexture(JsonAssetManager.getInstance().getEntry("past_round", TextureRegion.class));
//			obj.setName("past_round");
//			obj.setSpace(2);
//			addObject(obj);
//		}
		//TODO:Delete
//		for (int ii = 0; ii < PAST_DIAMONDS.length; ii++) {
//			PolygonObstacle obj;
//			obj = new Platform(newPlatDiamond, PAST_DIAMONDS[ii][0], PAST_DIAMONDS[ii][1]);
//			obj.setBodyType(BodyDef.BodyType.StaticBody);
//			obj.setDensity(BASIC_DENSITY);
//			obj.setFriction(BASIC_FRICTION);
//			obj.setRestitution(BASIC_RESTITUTION);
//			obj.setDrawScale(scale);
//			obj.setTexture(JsonAssetManager.getInstance().getEntry("past_diamond", TextureRegion.class));
//			obj.setName("past_diamond");
//			obj.setSpace(2);
//			addObject(obj);
//		}
		// TODO: Delete
//		for (int ii = 0; ii < PAST_CAPSULES.length; ii++) {
//			PolygonObstacle obj;
//			obj = new Platform(newPlatCapsule, PAST_CAPSULES[ii][0], PAST_CAPSULES[ii][1]);
//			obj.setBodyType(BodyDef.BodyType.StaticBody);
//			obj.setDensity(BASIC_DENSITY);
//			obj.setFriction(BASIC_FRICTION);
//			obj.setRestitution(BASIC_RESTITUTION);
//			obj.setDrawScale(scale);
//			obj.setTexture(JsonAssetManager.getInstance().getEntry("past_capsule", TextureRegion.class));
//			obj.setName("past_capsule");
//			obj.setSpace(2);
//			addObject(obj);
//		}

		// { 1.0f, 4.0f, 3.0f, 4.0f, 3.0f, 2.5f, 1.0f, 2.5f }, { 3.0f, 8.0f, 5.0f, 8.0f,
		// 5.0f, 7.5f, 3.0f, 7.5f },
		// { 5.5f, 4.5f, 7.5f, 4.5f, 7.5f, 5.0f, 5.5f, 5.0f }


		// Create avatar
		JsonValue json = levelFormat.get("avatar");
		avatar = new Avatar();
		avatar.initialize(json);
		avatar.setDrawScale(scale);
		addObject(avatar);

		//TODO: Delete
//		avatarTexture = JsonAssetManager.getInstance().getEntry(json.get("texture").asString(), TextureRegion.class);
//		float dwidth = avatarTexture.getRegionWidth() / scale.x;
//		float dheight = avatarTexture.getRegionHeight() / scale.y;

//		avatar = new Avatar(DUDE_POS.x, DUDE_POS.y, dwidth, dheight, scale);

//		avatar.setTexture(avatarTexture);
//		avatar.setBodyType(BodyDef.BodyType.DynamicBody);
//		avatar.setName("avatar");

		// Set film strips to animate avatar states
//		avatarStandingTexture = JsonAssetManager.getInstance().getEntry("avatarstanding", FilmStrip.class);
//		avatarCrouchingTexture = JsonAssetManager.getInstance().getEntry("avatarcrouching", FilmStrip.class);
//		avatarDashingTexture = JsonAssetManager.getInstance().getEntry("avatardashing", FilmStrip.class);
//		avatarFallingTexture = JsonAssetManager.getInstance().getEntry("avatarfalling", FilmStrip.class);
//
//		avatar.setFilmStrip(Avatar.AvatarState.STANDING, avatarStandingTexture);
//		avatar.setFilmStrip(Avatar.AvatarState.CROUCHING, avatarCrouchingTexture);
//		avatar.setFilmStrip(Avatar.AvatarState.DASHING, avatarDashingTexture);
//		avatar.setFilmStrip(Avatar.AvatarState.FALLING, avatarFallingTexture);

		// Set textures for caught projectiles
//		projPresentCaughtTexture = JsonAssetManager.getInstance().getEntry("projpresentcaught", TextureRegion.class);
//		projPastCaughtTexture = JsonAssetManager.getInstance().getEntry("projpastcaught", TextureRegion.class);
//		avatar.setCaughtProjTexture(PRESENT, projPresentCaughtTexture);
//		avatar.setCaughtProjTexture(PAST, projPastCaughtTexture);


		JsonValue enemy = levelFormat.get("enemies").child();
		while(enemy!= null){
			Enemy obj = new Enemy (avatar,enemy);
			obj.setDrawScale(scale);
			addObject(obj);
			enemy = enemy.next();
		}

		//TODO: Delete

		// enemyPresentTexture = JsonAssetManager.getInstance().getEntry("enemypresent", TextureRegion.class);
//		enemyPastTexture = JsonAssetManager.getInstance().getEntry("enemypast", TextureRegion.class);
//		for (int ii = 0; ii < NUMBER_ENEMIES; ii++) {
//			TextureRegion texture;
//			if (TYPE_ENEMIES[ii] == PRESENT)
//				texture = enemyPresentTexture;
//			else
//				texture = enemyPastTexture;
//			float dwidth = texture.getRegionWidth() / scale.x;
//			float dheight = texture.getRegionHeight() / scale.y;
//			Enemy enemy = new Enemy(TYPE_ENEMIES[ii], COOR_ENEMIES[ii][0], COOR_ENEMIES[ii][1], dwidth, dheight, texture,
//					CD_ENEMIES[ii], avatar, scale);
//			enemy.setBodyType(BodyDef.BodyType.DynamicBody);
//			enemy.setDrawScale(scale);
//			enemy.setName("enemy");
//			addObject(enemy);
//		}

		JsonValue turret = levelFormat.get("turrets").child();
		while(turret!=null){
			Enemy obj = new Enemy(turret);
			obj.setDrawScale(scale);
			addObject(obj);
			turret = turret.next();
		}
//		turretTexture = JsonAssetManager.getInstance().getEntry("turret", TextureRegion.class);
//		for (int ii = 0; ii < NUMBER_TURRETS; ii++) {
//			TextureRegion texture = turretTexture;
//			float dwidth = texture.getRegionWidth() / scale.x;
//			float dheight = texture.getRegionHeight() / scale.y;
//			Vector2 projDir = new Vector2(DIR_TURRETS[ii][0], DIR_TURRETS[ii][1]);
//			Enemy turret = new Enemy(TYPE_TURRETS[ii], COOR_TURRETS[ii][0], COOR_TURRETS[ii][1], dwidth, dheight, texture,
//					CD_TURRETS[ii], projDir, scale);
//			turret.setBodyType(BodyDef.BodyType.StaticBody);
//			turret.setDrawScale(scale);
//			turret.setName("turret");
//			addObject(turret);
//		}

		collisionController = new CollisionController(this);
		world.setContactListener(collisionController);
	}

	public PooledList<Obstacle> getObjects() { return objects;}

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
		if (!super.preUpdate(dt)) {
			return false;
		}

		if (!isFailure() && avatar.getY() < -6) {
			avatar.removeLife();
			if (avatar.getLives() > 0) {
				lives = avatar.getLives();
				reset();
				return true;
			} else {
				lives = 0;
				setFailure(true);
			}
			return false;
		}
		if (!isFailure() && avatar.getLives() <= 0){
			setFailure(true);
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
		for (Obstacle obj : objects) {

			 if (obj instanceof Enemy && !((Enemy) obj).isTurret()) {
			 	Enemy e = (Enemy) obj;
			 	if (e.getSpace() == 3) {
			 		e.setIsActive(true);
			 	} else if (!shifted && (e.getSpace()==2)) {
			 		e.setIsActive(false);
			 	} else if (shifted && (e.getSpace()==2)) {
			 		e.setIsActive(e.getShiftedActive());
			 	} else if (shifted && (e.getSpace()==1)) {
			 		e.setIsActive(false);
			 	} else if (!shifted && (e.getSpace()==1)) {
			 		e.setIsActive(e.getShiftedActive());
			 	}
			 }

			if (obj.getName().equals("bullet") || obj.getName().equals("turret")) {
				obj.setSensor(false);
				if (obj.getSpace() == 3) {
					obj.setSensor(false);
				} else if (!shifted && (obj.getSpace() == 2)) {
					obj.setSensor(true);
				} else if (shifted && (obj.getSpace() == 1)) {
					obj.setSensor(true);
				}
			} else {
				obj.setActive(true);
				if (obj.getSpace() == 3) {
					obj.setActive(true);
				} else if (!shifted && (obj.getSpace() == 2)) {
					obj.setActive(false);
				} else if (shifted && (obj.getSpace() == 1)) {
					obj.setActive(false);
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

		// test slow down time
		if (timeFreeze) {
			world.step(WORLD_STEP / 8, WORLD_VELOC, WORLD_POSIT);
			for (Obstacle o : objects) {
				if (o instanceof Enemy) {
					((Enemy) o).slowCoolDown(true);
				}
			}
		} else {
			world.step(WORLD_STEP, WORLD_VELOC, WORLD_POSIT);
			for (Obstacle o : objects) {
				if (o instanceof Enemy) {
					((Enemy) o).slowCoolDown(false);
				}
			}
		}

		int t = avatar.getStartedDashing();
		if (t > 0) {
			t = t - 1;
			avatar.setStartedDashing(t);
		}

		if (avatar.isHolding()) {
			timeFreeze = true;
			if (avatar.getBodyType() != BodyDef.BodyType.StaticBody) {
				avatar.setBodyType(BodyDef.BodyType.StaticBody);
			} else if (InputController.getInstance().releasedRightMouseButton()) {
				timeFreeze = false;
				Vector2 mousePos = InputController.getInstance().getMousePosition();
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
				avatar.setCurrentPlatform(null);
				createRedirectedProj();
				avatar.setHeldBullet(null); // NOTE: gives error if called before createRedirectedProj()

			}
		}
		if (InputController.getInstance().pressedShiftKey()) {
			shifted = !shifted;
			avatar.resetDashNum();
			avatar.setPosition(avatar.getPosition().x, avatar.getPosition().y + 0.01f);
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
			for (Obstacle o: objects) {
				if (o instanceof Enemy) {
					Enemy e = (Enemy) o;
					if (!e.isTurret()) {
						e.coolDown(false);
						e.setLeftFixture(null);
						e.setRightFixture(null);
					}
				}
			}
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
			avatar.dash(); // handles checking if dashing is possible
		}
		// if (dashAttempt && !avatar.isDashing() && avatar.isSticking()) {
		// // check valid direction
		// Vector2 mousePos = InputController.getInstance().getMousePosition();
		// avatar.setBodyType(BodyDef.BodyType.DynamicBody);
		// avatar.setSticking(false);
		// avatar.setWasSticking(false);
		// avatar.setDashing(true);
		// avatar.setDashStartPos(avatar.getPosition().cpy());
		// avatar.setDashDistance(avatar.getDashRange());
		// // avatar.setDashDistance(Math.min(avatar.getDashRange(),
		// // avatar.getPosition().dst(mousePos)));
		// avatar.setDashForceDirection(mousePos.sub(avatar.getPosition()));
		// avatar.setStartedDashing(1);
		// }

		// Process actions in object model
		avatar.setJumping(InputController.getInstance().didPrimary());
		avatar.setShooting(InputController.getInstance().didSecondary());

		// Sets which direction the avatar is facing (left or right)
		if (InputController.getInstance().pressedLeftMouseButton()) {
			Vector2 mousePos = InputController.getInstance().getMousePosition();
			Vector2 avatarPos = avatar.getPosition().cpy();
			avatar.setMovement(mousePos.x - avatarPos.x);
		}

		// Add bullet if enemy can fire
		for (Obstacle o : objects) {
			if (o instanceof Enemy) {
				Enemy e = (Enemy) o.getBody().getUserData();
				if (e.isTurret()){
					if (e.canFire()) {
						createBullet(e);
					} else
						e.coolDown(true);
				} else if ((shifted && e.getSpace() == 2) || (!shifted && e.getSpace() == 1)) {
					if (!e.isTurret()) {
						if (e.getLeftFixture() == null || e.getRightFixture() == null) {
							break;
						}
						e.createLineOfSight(world, BULLET_OFFSET);
						e.applyForce();
					}
					if (e.canFire()) {
						if (o.getName() == "enemy") {
							e.setVelocity(BULLET_OFFSET);
						}
						createBullet(e);
					} else
						e.coolDown(true);
				}
			}
		}

		avatar.applyForce();
		// enemy.applyForce();

		// Update animation state
		if (InputController.getInstance().pressedLeftMouseButton()
				|| InputController.getInstance().pressedRightMouseButton()) {
			// If either mouse button is held, set animation to be crouching
			avatar.animate(Avatar.AvatarState.CROUCHING, true);
		} else if (avatar.isSticking()) {
			// Default animation if player is stationary
			avatar.animate(Avatar.AvatarState.STANDING, false);
		} else if (avatar.isDashing()) {
			avatar.animate(Avatar.AvatarState.DASHING, false);
		} else {
			avatar.animate(Avatar.AvatarState.FALLING, false);
		}

		if (avatar.isJumping()) {
			JsonValue data = assetDirectory.get("sounds").get("jump");
			SoundController.getInstance().play("jump", data.get("file").asString(), false, data.get("volume").asFloat());
		}

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

		if (avatar.isSticking() && !avatar.getWasSticking()) {
			avatar.setDashing(false);
			avatar.getBody().setLinearVelocity(0, 0);
			avatar.getBody().setAngularVelocity(0);
			avatar.setBodyType(BodyDef.BodyType.StaticBody);
			avatar.setWasSticking(true);
			System.out.println("new angle " + avatar.getNewAngle());
			avatar.setAngle(avatar.getNewAngle());
		}
	}

	/**
	 * Add a new bullet to the world and send it in the right direction.
	 *
	 * @param enemy enemy
	 */
	private void createBullet(Enemy enemy) {
		float offset = BULLET_OFFSET;
		bulletBigTexture = JsonAssetManager.getInstance().getEntry("bulletbig", TextureRegion.class);
		presentBullet = JsonAssetManager.getInstance().getEntry("projpresent", TextureRegion.class);
		pastBullet = JsonAssetManager.getInstance().getEntry("projpast", TextureRegion.class);
		float radius = bulletBigTexture.getRegionWidth() / (30.0f);
		Projectile bullet = new Projectile(enemy.getType(), enemy.getX(), enemy.getY() + offset, radius);

		bullet.setName("bullet");
		bullet.setDensity(HEAVY_DENSITY);
		bullet.setDrawScale(scale);
		//bullet.setTexture(bulletBigTexture);
		bullet.setBullet(true);
		bullet.setGravityScale(0);
		bullet.setLinearVelocity(enemy.getProjVelocity());
		bullet.setSpace(enemy.getSpace());
		addQueuedObject(bullet);
		if (bullet.getType().equals(PRESENT)){
			bullet.setTexture(presentBullet);
		} else {
			bullet.setTexture(pastBullet);
		}
		if (shifted && enemy.getSpace() == 2) { // past world
			JsonValue data = assetDirectory.get("sounds").get("pew");
			SoundController.getInstance().play("pew", data.get("file").asString(), false, data.get("volume").asFloat());
		} else if (!shifted && enemy.getSpace() == 1) { // present world
			JsonValue data = assetDirectory.get("sounds").get("pew");
			SoundController.getInstance().play("pew", data.get("file").asString(), false, data.get("volume").asFloat());
		}

		// Reset the firing cooldown.
		enemy.coolDown(false);
	}

	/**
	 * Add a new bullet to the world and send it in the right direction.
	 */
	private void createRedirectedProj() {
		Vector2 mousePos = InputController.getInstance().getMousePosition();
		Vector2 redirection = avatar.getPosition().cpy().sub(mousePos).nor();
		float x0 = avatar.getX() + (redirection.x * avatar.getWidth());
		float y0 = avatar.getY() + (redirection.y * avatar.getHeight());
		float radius = bulletBigTexture.getRegionWidth() / (2.0f * scale.x);
		Vector2 projVel = redirection.cpy().scl(12);
		EntityType projType = avatar.getHeldBullet().getType();

		Projectile bullet = new Projectile(projType, x0, y0, radius);
		bullet.setName("bullet");
		bullet.setDensity(HEAVY_DENSITY);
		bullet.setDrawScale(scale);
		//bullet.setTexture(bulletBigTexture);
		if (bullet.getType() == PRESENT){
			bullet.setTexture(presentBullet);
		} else {
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
		SoundController.getInstance().play(pew.get("file").asString(), pew.get("file").asString(), false, EFFECT_VOLUME);
	}

	/**
	 * Prints the (x,y) coordinate of the current mouse position when the 'X' key is
	 * pressed. For debug purposes.
	 */
	private void printCoordinates() {
		if (InputController.getInstance().pressedXKey()) {
			Vector2 pos = InputController.getInstance().getMousePosition();
			System.out.println("Mouse position: " + pos);
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
	}

	/**
	 * Draws a line indicating the direction and distance of the dash.
	 */
	public void drawIndicator(GameCanvas canvas) {
		if (!InputController.getInstance().pressedLeftMouseButton()
				&& !InputController.getInstance().pressedRightMouseButton())
			return;
		// Do not draw while player is dashing or not holding a projectile
		//if (avatar.isDashing() && !avatar.isHolding())
		//	return;
		if (!avatar.canDash() && !avatar.isSticking() && !avatar.isHolding())
			return;
		// Draw dynamic dash indicator
		Vector2 mousePos = InputController.getInstance().getMousePosition();
		Vector2 redirection = avatar.getPosition().cpy().sub(mousePos).nor();
		canvas.begin();
		TextureRegion circle = JsonAssetManager.getInstance().getEntry("circle", TextureRegion.class);
		TextureRegion arrow = JsonAssetManager.getInstance().getEntry("arrow", TextureRegion.class);
		//System.out.println(scale);
		if (!avatar.isHolding()) {
			canvas.draw(circle, Color.WHITE, circle.getRegionWidth() / 2, circle.getRegionHeight() / 2, avatar.getX() * scale.x, avatar.getY() * scale.y, redirection.angle() / 57, 0.0083f * scale.x, 0.0083f * scale.y);
		}
		canvas.draw(arrow, Color.WHITE, 0, arrow.getRegionHeight()/2, avatar.getX() * scale.x, avatar.getY() * scale.y, (180 + redirection.angle())/ 57, 0.016f * scale.x , 0.016f * scale.y);
		canvas.end();
		/*Vector2 avPos = avatar.getPosition();
		Vector2 mPos = InputController.getInstance().getMousePosition();
		Vector2 startPos = avPos.cpy().scl(scale);
		mousePos = mPos.cpy().scl(scale);
		Vector2 alteredPos = mousePos.sub(startPos).nor();
		float dist = avatar.getDashRange();
		// float dist = Math.min(avatar.getDashRange(), avPos.dst(mPos));
		Vector2 endPos = alteredPos.scl(dist).scl(scale);
		endPos.add(startPos);
		//canvas.drawLine(startPos.x, startPos.y, endPos.x, endPos.y, 0, 1, 0.6f, 1);

		// If player is holding a projectile, draw projectile indicator
		// TODO: need to fix - line is a bit off
		Vector2 projDir = startPos.cpy().sub(mousePos).scl(scale);
		//System.out.println("HOLDING: " + avatar.isHolding());*/
		TextureRegion projCircle = JsonAssetManager.getInstance().getEntry("projectile_circle", TextureRegion.class);
		TextureRegion projArrow = JsonAssetManager.getInstance().getEntry("projectile_arrow", TextureRegion.class);

		canvas.begin();
		if (avatar.isHolding()) {
			canvas.draw(projCircle, Color.GOLD, projCircle.getRegionWidth()/2, projCircle.getRegionHeight()/2, avatar.getX() * scale.x, avatar.getY() * scale.y, redirection.angle()/ 57, 0.0073f * scale.x , 0.0073f * scale.y);
			canvas.draw(projArrow, Color.GOLD, 0, projArrow.getRegionHeight()/2, avatar.getX() * scale.x, avatar.getY() * scale.y, (redirection.angle())/ 57, 0.0061f * scale.x , 0.0061f * scale.y);
		}
		canvas.end();
	}

	public void drawLives(GameCanvas canvas){
		TextureRegion life = JsonAssetManager.getInstance().getEntry("life", TextureRegion.class);
		TextureRegion streak = JsonAssetManager.getInstance().getEntry("streak", TextureRegion.class);
		canvas.begin();
		canvas.draw(streak, Color.WHITE, 0, 0, -0.8f * scale.x, canvas.getHeight()/2 + 2 * scale.y, 9 * scale.x, 9 * scale.y);
		for (int i = 0; i < avatar.getLives(); i++) {
			canvas.draw(life, Color.WHITE, 0, 0, life.getRegionWidth() * 0.002f * scale.x + (life.getRegionWidth() * 0.005f * scale.x * i), canvas.getHeight() - life.getRegionHeight() * 0.007f * scale.y, scale.x, scale.y);
		}
		canvas.end();
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
		canvas.clear();
		canvas.begin();
		if (shifted) {
			// System.out.println(backgroundTexture.getRegionWidth());
			backgroundTexture = JsonAssetManager.getInstance().getEntry(levelFormat.get("past_background").asString(), TextureRegion.class);
			canvas.draw(backgroundTexture, Color.WHITE, 0, 0, canvas.getWidth(), canvas.getHeight());
		} else {
			backgroundTexture = JsonAssetManager.getInstance().getEntry(levelFormat.get("present_background").asString(), TextureRegion.class);
			canvas.draw(backgroundTexture, Color.WHITE, 0, 0, canvas.getWidth(), canvas.getHeight());
		}

		drawObjectInWorld();
		canvas.end();

		drawIndicator(canvas);
		drawLives(canvas);

		if (debug) {
			canvas.beginDebug();
			drawDebugInWorld();
			canvas.endDebug();
		}

		stage.act(Gdx.graphics.getDeltaTime());
		stage.draw();

		// Final message
		if (complete && !failed) {
			displayFont.setColor(Color.YELLOW);
			canvas.begin(); // DO NOT SCALE
			canvas.drawTextCentered("VICTORY!", displayFont, 0.0f);
			canvas.end();
		} else if (failed) {
			displayFont.setColor(Color.RED);
			canvas.begin(); // DO NOT SCALE
			canvas.drawTextCentered("FAILURE!", displayFont, 0.0f);
			canvas.end();
		}
	}

	/** Unused ContactListener method */
	public void postSolve(Contact contact, ContactImpulse impulse) {
	}

	/** Unused ContactListener method */
	public void preSolve(Contact contact, Manifold oldManifold) {
	}

	/**
	 * @return avatar object
	 */
	public Avatar getAvatar() {
		return avatar;
	}

	public Enemy getEnemy() {
		return enemy;
	}

	/**
	 * @return goal door object
	 */
	public BoxObstacle getGoalDoor() {
		return goalDoor;
	}
}