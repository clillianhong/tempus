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

import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;
import edu.cornell.gdiac.tempus.GameCanvas;
import edu.cornell.gdiac.tempus.InputController;
import edu.cornell.gdiac.tempus.WorldController;
import edu.cornell.gdiac.tempus.obstacle.*;
import edu.cornell.gdiac.tempus.tempus.models.*;
import edu.cornell.gdiac.util.PooledList;
import edu.cornell.gdiac.util.SoundController;

import java.util.Iterator;
import java.util.Vector;

import static edu.cornell.gdiac.tempus.tempus.models.EntityType.PAST;
import static edu.cornell.gdiac.tempus.tempus.models.EntityType.PRESENT;

/**
 * Gameplay specific controller for the platformer game.  
 *
 * You will notice that asset loading is not done with static methods this time.  
 * Instance asset loading makes it easier to process our game modes in a loop, which 
 * is much more scalable. However, we still want the assets themselves to be static.
 * This is the purpose of our AssetState variable; it ensures that multiple instances
 * place nicely with the static assets.
 */
public class PrototypeController extends WorldController {
	/** The texture file for the character avatar (no animation) */
	private static final String DUDE_FILE  = "enemies/dude.png";
	/** The texture file for the spinning barrier */
	private static final String BARRIER_FILE = "enemies/barrier.png";

	/** The texture file for the bullet */
	private static final String BULLET_FILE  = "enemies/bullet.png";
	/** The texture file for the big bullet */
	private static final String BULLET_BIG_FILE  = "enemies/bulletbig.png";

	/** The texture file for the turret */
	private static final String TURRET_FILE  = "enemies/turret.png";
	/** The texture file for the background */
	private static final String BACKGROUND_FILE = "shared/ice.png";

	/** The texture file for present enemy */
	private static final String ENEMY_PRESENT_FILE  = "enemies/enemy_present.png";
	/** The texture file for past enemy */
	private static final String ENEMY_PAST_FILE  = "enemies/enemy_past.png";

	/** Checks if did debug */
	private boolean debug;
	
	/** The sound file for a jump */
	private static final String JUMP_FILE = "enemies/jump.mp3";
	/** The sound file for a bullet fire */
	private static final String PEW_FILE = "enemies/pew.mp3";
	/** The sound file for a bullet collision */
	private static final String POP_FILE = "enemies/plop.mp3";

	/** Texture asset for character avatar */
	private TextureRegion avatarTexture;
	/** Texture asset for the spinning barrier */
	private TextureRegion barrierTexture;
	/** Texture asset for the bullet */
	private TextureRegion bulletTexture;
	/** Texture asset for the big bullet */
	private TextureRegion bulletBigTexture;
	/** Texture asset for the bridge plank */
	private TextureRegion bridgeTexture;
	/** Texture asset for the turret */
	private TextureRegion turretTexture;
	/** Texture asset for present enemy */
	private TextureRegion enemyPresentTexture;
	/** Texture asset for past enemy */
	private TextureRegion enemyPastTexture;

	/** Texture asset for the background */
	private TextureRegion backgroundTexture;
	
	/** Track asset loading from all instances and subclasses */
	private AssetState platformAssetState = AssetState.EMPTY;
	
	/**
	 * Preloads the assets for this controller.
	 *
	 * To make the game modes more for-loop friendly, we opted for nonstatic loaders
	 * this time.  However, we still want the assets themselves to be static.  So
	 * we have an AssetState that determines the current loading state.  If the
	 * assets are already loaded, this method will do nothing.
	 * 
	 * @param manager Reference to global asset manager.
	 */	
	public void preLoadContent(AssetManager manager) {
		if (platformAssetState != AssetState.EMPTY) {
			return;
		}

		// Images
		platformAssetState = AssetState.LOADING;
		manager.load(DUDE_FILE, Texture.class);
		assets.add(DUDE_FILE);
		manager.load(BARRIER_FILE, Texture.class);
		assets.add(BARRIER_FILE);
		manager.load(BULLET_FILE, Texture.class);
		assets.add(BULLET_FILE);
		manager.load(BULLET_BIG_FILE, Texture.class);
		assets.add(BULLET_BIG_FILE);
		manager.load(TURRET_FILE, Texture.class);
		assets.add(TURRET_FILE);
		manager.load(ENEMY_PRESENT_FILE, Texture.class);
		assets.add(ENEMY_PRESENT_FILE);
		manager.load(ENEMY_PAST_FILE, Texture.class);
		assets.add(ENEMY_PAST_FILE);

		//background files
		manager.load(BACKGROUND_FILE, Texture.class);
		assets.add(BACKGROUND_FILE);

		// Sounds
		manager.load(JUMP_FILE, Sound.class);
		assets.add(JUMP_FILE);
		manager.load(PEW_FILE, Sound.class);
		assets.add(PEW_FILE);
		manager.load(POP_FILE, Sound.class);
		assets.add(POP_FILE);
		
		super.preLoadContent(manager);
	}

	/**
	 * Load the assets for this controller.
	 *
	 * To make the game modes more for-loop friendly, we opted for nonstatic loaders
	 * this time.  However, we still want the assets themselves to be static.  So
	 * we have an AssetState that determines the current loading state.  If the
	 * assets are already loaded, this method will do nothing.
	 * 
	 * @param manager Reference to global asset manager.
	 */
	public void loadContent(AssetManager manager) {
		if (platformAssetState != AssetState.LOADING) {
			return;
		}
		
		avatarTexture = createTexture(manager,DUDE_FILE,false);
		barrierTexture = createTexture(manager,BARRIER_FILE,false);
		bulletTexture = createTexture(manager,BULLET_FILE,false);
		bulletBigTexture = createTexture(manager,BULLET_BIG_FILE,false);
		turretTexture = createTexture(manager,TURRET_FILE,false);
		backgroundTexture = createTexture(manager,BACKGROUND_FILE, false);
		enemyPresentTexture = createTexture(manager,ENEMY_PRESENT_FILE,false);
		enemyPastTexture = createTexture(manager,ENEMY_PAST_FILE,false);

		SoundController sounds = SoundController.getInstance();
		sounds.allocate(manager, JUMP_FILE);
		sounds.allocate(manager, PEW_FILE);
		sounds.allocate(manager, POP_FILE);
		super.loadContent(manager);
		platformAssetState = AssetState.COMPLETE;
	}
	
	// Physics constants for initialization
	/** The new heavier gravity for this world (so it is not so floaty) */
	private static final float  DEFAULT_GRAVITY = -14.7f;
	/** The density for most physics objects */
	private static final float  BASIC_DENSITY = 0.0f;
	/** The density for a bullet */
	private static final float  HEAVY_DENSITY = 10.0f;
	/** Friction of most platforms */
	private static final float  BASIC_FRICTION = 0.6f;
	/** The restitution for all physics objects */
	private static final float  BASIC_RESTITUTION = 0.1f;
	/** Offset for bullet when firing */
	private static final float  BULLET_OFFSET = 1.0f;
	/** The volume for sound effects */
	private static final float EFFECT_VOLUME = 0.8f;

	// Since these appear only once, we do not care about the magic numbers.
	// In an actual game, this information would go in a data file.
	// Wall vertices
	private static final float[][] WALLS = { 
			  								{16.0f, 18.0f, 16.0f, 17.0f,  1.0f, 17.0f,
			  								  1.0f,  0.0f,  0.0f,  0.0f,  0.0f, 18.0f},
			  								{32.0f, 18.0f, 32.0f,  0.0f, 31.0f,  0.0f,
			  							     31.0f, 17.0f, 16.0f, 17.0f, 16.0f, 18.0f}
											};
	
	/** The outlines of all of the platforms */
	private static float dudex = 2.5f;
	private static float dudey = 5.0f;
	private static final float[][] PLATFORMS = {

			{1.0f, 4.0f, 3.0f, 4.0f, 3.0f, 2.5f, 1.0f, 2.5f},
			{3.0f, 8.0f, 5.0f, 8.0f, 5.0f, 7.5f, 3.0f, 7.5f},
			{5.5f, 4.5f, 7.5f, 4.5f, 7.5f, 5.0f, 5.5f, 5.0f}, //downwards diagonal
			{9.0f, 6.0f, 9.0f, 7.5f, 9.5f, 7.5f, 9.5f, 6.0f},
			{7.0f, 9.0f, 7.0f, 10.5f, 7.5f, 10.5f, 7.5f, 9.0f},
			{9.0f, 11.5f, 9.0f, 13.0f, 9.5f, 13.0f, 9.5f, 11.5f},
			{7.0f, 15.5f, 7.5f, 15.5f, 7.5f, 14.0f, 7.0f, 14.0f},
			{11.0f, 15.5f, 11.5f, 15.5f, 11.5f, 14.0f, 11.0f, 14.0f},
			{12.0f, 6.5f, 13.5f, 6.5f, 13.5f, 6.0f, 12.0f, 6.0f},
			{15.0f, 8.0f, 15.0f, 8.5f, 16.5f, 8.5f, 16.5f, 8.0f},
			{18.0f, 5.5f, 18.0f, 6.0f, 19.5f, 6.0f, 19.5f, 5.5f},
			{21.0f, 8.0f, 21.0f, 8.5f, 22.5f, 8.5f, 22.5f, 8.0f},
			{24.0f, 5.5f, 24.0f, 6.0f, 25.5f, 6.0f, 25.5f, 5.5f},
			{25.5f, 10.0f, 25.5f, 11.5f, 26.0f, 11.5f, 26.0f, 10.0f},
			{23.5f, 13.0f, 23.5f, 14.5f, 24.0f, 14.5f, 24.0f, 13.0f},
			{26.5f, 13.0f, 26.5f, 14.0f, 31.0f, 14.0f, 31.0f, 13.0f}
	};

	// Other game objects
	/** The goal door position */
	private static Vector2 GOAL_POS = new Vector2(29.5f,15.5f);
	/** The initial position of the dude */
	private static Vector2 DUDE_POS = new Vector2(dudex, dudey);
	/** The initial position of the turret */
	private static Vector2 TURRET_POS = new Vector2(8.5f, 10.0f);
	/** The initial position of the enemy */
	private static Vector2 ENEMY_POS = new Vector2(13.0f, 7.5f);

	// Physics objects for the game
	/** Reference to the character avatar */
	private Avatar avatar;
	/** Reference to the goalDoor (for collision detection) */
	private Door goalDoor;

	private Enemy turret;
	private Enemy turret2;
	private Enemy enemy;

	/** Whether the avatar is shifted to the other world or not */
	private boolean shifted;


	/** Collision Controller instance **/
	protected CollisionController collisionController;

	/**
	 * Creates and initialize a new instance of the platformer game
	 *
	 * The game has default gravity and other settings
	 */
	public PrototypeController() {
		super(DEFAULT_WIDTH,DEFAULT_HEIGHT,DEFAULT_GRAVITY);
		setDebug(false);
		setComplete(false);
		setFailure(false);

		shifted = false;
		debug = false;
	}
	
	/**
	 * Resets the status of the game so that we can play again.
	 *
	 * This method disposes of the world and creates a new one.
	 */
	public void reset() {
		Vector2 gravity = new Vector2(world.getGravity() );
		
		for(Obstacle obj : objects) {
			obj.deactivatePhysics(world);
		}
		objects.clear();
		addQueue.clear();
		world.dispose();
		shifted = false;
		world = new World(gravity,false);
		world.setContactListener(collisionController);
//		world.setContactListener(this);
		setComplete(false);
		setFailure(false);
		populateLevel();
	}

	/**
	 * Lays out the game geography.
	 */
	private void populateLevel() {
		// Add level goal
		float dwidth  = goalTile.getRegionWidth()/scale.x;
		float dheight = goalTile.getRegionHeight()/scale.y;
		goalDoor = new Door(GOAL_POS.x,GOAL_POS.y,dwidth,dheight,0);
		goalDoor.setBodyType(BodyDef.BodyType.StaticBody);
		goalDoor.setDensity(0.0f);
		goalDoor.setFriction(0.0f);
		goalDoor.setRestitution(0.0f);
		goalDoor.setSensor(true);
		goalDoor.setDrawScale(scale);
		goalDoor.setTexture(goalTile);
		goalDoor.setName("goal");
		addObject(goalDoor);

	    String wname = "wall";
	    for (int ii = 0; ii < WALLS.length; ii++) {
	        PolygonObstacle obj;
	    	obj = new PolygonObstacle(WALLS[ii], 0, 0);
			obj.setBodyType(BodyDef.BodyType.StaticBody);
			obj.setDensity(BASIC_DENSITY);
			obj.setFriction(BASIC_FRICTION);
			obj.setRestitution(BASIC_RESTITUTION);
			obj.setDrawScale(scale);
			obj.setTexture(earthTile);
			obj.setName(wname);
			addObject(obj);
	    }
	    
	    String pname = "platform";
	    for (int ii = 0; ii < PLATFORMS.length; ii++) {
	        PolygonObstacle obj;
	    	obj = new Platform(PLATFORMS[ii], 0, 0);
			obj.setBodyType(BodyDef.BodyType.StaticBody);
			obj.setDensity(BASIC_DENSITY);
			obj.setFriction(BASIC_FRICTION);
			obj.setRestitution(BASIC_RESTITUTION);
			obj.setDrawScale(scale);
			obj.setTexture(earthTile);
			obj.setName(pname);
			if (ii <= PLATFORMS.length/2){
				obj.setSpace(1);
			}
			if (ii > PLATFORMS.length/2){
				obj.setSpace(2);
			}
			addObject(obj);
	    }

		// Create dude
		dwidth  = avatarTexture.getRegionWidth()/scale.x;
		dheight = avatarTexture.getRegionHeight()/scale.y;
		avatar = new Avatar(DUDE_POS.x, DUDE_POS.y, dwidth, dheight, AvatarOrientation.OR_UP);
		avatar.setDrawScale(scale);
		avatar.setTexture(avatarTexture);
		avatar.setBodyType(BodyDef.BodyType.DynamicBody);
		avatar.setName("avatar");
		addObject(avatar);

		// Create turret in past world
		dwidth  = turretTexture.getRegionWidth()/scale.x;
		dheight = turretTexture.getRegionHeight()/scale.y;
		Vector2 projVel = new Vector2(0, 2.0f);
		turret = new Enemy(PAST, TURRET_POS.x,TURRET_POS.y - 5.0f, dwidth, dheight, 120, projVel);
		turret.setDrawScale(scale);
		turret.setTexture(turretTexture);
		turret.setBodyType(BodyDef.BodyType.StaticBody);
		turret.setSpace(2);
		turret.setName("turret1");
		addObject(turret);

		// Create turret in present world
		projVel = new Vector2(-3.0f, 0);
		turret2 = new Enemy(PRESENT, TURRET_POS.x + 10.0f,TURRET_POS.y, dwidth, dheight, 60, projVel);
		turret2.setDrawScale(scale);
		turret2.setTexture(turretTexture);
		turret2.setBodyType(BodyDef.BodyType.StaticBody);
		turret2.setSpace(1);
		turret2.setName("turret2");
		addObject(turret2);

		// Create enemy in present world
		dwidth  = enemyPresentTexture.getRegionWidth()/scale.x;
		dheight = enemyPresentTexture.getRegionHeight()/scale.y;
		enemy = new Enemy(PRESENT, ENEMY_POS.x, ENEMY_POS.y, dwidth, dheight,
				enemyPresentTexture, 60, avatar);
		enemy.setDrawScale(scale);
		enemy.setName("enemyPresent1");
		addObject(enemy);

		// Create enemy in past world
		dwidth  = enemyPastTexture.getRegionWidth()/scale.x;
		dheight = enemyPastTexture.getRegionHeight()/scale.y;
		Enemy enemyPast1 = new Enemy(PAST, 15.625f,11.03125f,
				dwidth, dheight, 60, new Vector2(0,0));
		enemyPast1.setDrawScale(scale);
		enemyPast1.setTexture(enemyPastTexture);
		enemyPast1.setBodyType(BodyDef.BodyType.DynamicBody);
		enemyPast1.setSpace(2);
		enemyPast1.setName("enemyPast1");
		addObject(enemyPast1);

		collisionController = new CollisionController(this);
		world.setContactListener(collisionController);
	}
	
	/**
	 * Returns whether to process the update loop
	 *
	 * At the start of the update loop, we check if it is time
	 * to switch to a new game mode.  If not, the update proceeds
	 * normally.
	 *
	 * @param dt Number of seconds since last animation frame
	 * 
	 * @return whether to process the update loop
	 */
	public boolean preUpdate(float dt) {
		if (!super.preUpdate(dt)) {
			return false;
		}

		if (!isFailure() && avatar.getY() < -1) {
			setFailure(true);
			return false;
		}

		enemy.createLineOfSight(world);
		
		return true;
	}


	/**
	 * Makes the object sleep if it is not in this world
	 *
	 */
	public void sleepIfNotInWorld(){
		for(Obstacle obj : objects) {
			obj.setActive(true);
			if (obj.getSpace() == 3){
				obj.setActive(true);
			}
			else if (!shifted && (obj.getSpace()==2)) {obj.setActive(false);}
			else if (shifted && (obj.getSpace()==1)) {obj.setActive(false);}
		}
	}
	/**
	 * The core gameplay loop of this world.
	 *
	 * This method contains the specific update code for this mini-game. It does
	 * not handle collisions, as those are managed by the parent class WorldController.
	 * This method is called after input is read, but before collisions are resolved.
	 * The very last thing that it should do is apply forces to the appropriate objects.
	 *
	 * @param dt Number of seconds since last animation frame
	 */
	public void update(float dt) {
		if (avatar.isHolding()) {
			if (avatar.getBodyType() != BodyDef.BodyType.StaticBody) {
				avatar.setBodyType(BodyDef.BodyType.StaticBody);
			} else if (InputController.getInstance().releasedLeftMouseButton()){
				Vector2 mousePos = InputController.getInstance().getMousePosition();
				avatar.setBodyType(BodyDef.BodyType.DynamicBody);
				avatar.setSticking(false);
				avatar.setWasSticking(false);
				avatar.setDashing(true);
				avatar.setDashStartPos(avatar.getPosition().cpy());
				avatar.setDashDistance(avatar.getDashRange());
				//avatar.setDashDistance(Math.min(avatar.getDashRange(), avatar.getPosition().dst(mousePos)));
				avatar.setDashForceDirection(mousePos.cpy().sub(avatar.getPosition()));
				avatar.setHolding(false);
				Vector2 redirection = avatar.getPosition().cpy().sub(mousePos).nor();
				createBullet(
						new Enemy(avatar.getHeldBullet().getType(),
						avatar.getX() + (redirection.x * avatar.getWidth()),
						avatar.getY()  - BULLET_OFFSET + (redirection.y * avatar.getHeight()),
						1, 1, 0, redirection.cpy().scl(12)));
			}
		}
		if (InputController.getInstance().pressedShiftKey()){
			shifted = !shifted;
		}
		//Check if the platform is in this world or other world. If in the other world, make the platform sleep.
		sleepIfNotInWorld();
		if(InputController.getInstance().didDebug()){
			debug = !debug;
		}

		//prototype: Dash
		boolean dashAttempt = InputController.getInstance().releasedLeftMouseButton();
		if(dashAttempt && !avatar.isDashing() && avatar.isSticking()){
			//check valid direction
			Vector2 mousePos = InputController.getInstance().getMousePosition();
				avatar.setBodyType(BodyDef.BodyType.DynamicBody);
				avatar.setSticking(false);
				avatar.setWasSticking(false);
				avatar.setDashing(true);
				avatar.setDashStartPos(avatar.getPosition().cpy());
				avatar.setDashDistance(avatar.getDashRange());
				avatar.setDashDistance(Math.min(avatar.getDashRange(), avatar.getPosition().dst(mousePos)));
				avatar.setDashForceDirection(mousePos.sub(avatar.getPosition()));
		}

		// Process actions in object model
		avatar.setMovement(InputController.getInstance().getHorizontal() *avatar.getForce());
		avatar.setJumping(InputController.getInstance().didPrimary());
		avatar.setShooting(InputController.getInstance().didSecondary());
		
		// Add a bullet if we fire
		if (turret.canFire()) {
			createBullet(turret);
		} else {
			turret.coolDown(true);
		}

		if (turret2.canFire()) {
			createBullet(turret2);
		} else {
			turret2.coolDown(true);
		}

		if (enemy.canFire()) {
			enemy.setVelocity();
			createBullet(enemy);
		} else {
			enemy.coolDown(true);
		}
		
		avatar.applyForce();
//		enemy.applyForce();

	    if (avatar.isJumping()) {
	        SoundController.getInstance().play(JUMP_FILE,JUMP_FILE,false,EFFECT_VOLUME);
	    }
		
	    // If we use sound, we must remember this.
	    SoundController.getInstance().update();

	    // Print location of the mouse position when 'X' key is pressed
		// so we can know where to spawn enemies for testing purposes.
		printCoordinates();
	}

	/**

	 * Add a new bullet to the world and send it in the direction specified by
	 * the turret it originated from.
	 * Processes physics
	 *
	 * Once the update phase is over, but before we draw, we are ready to handle
	 * physics.  The primary method is the step() method in world.  This implementation
	 * works for all applications and should not need to be overwritten.
	 *
	 * If the avatar is sticking, sets the avatar's velocity to zero and makes the avatar's
	 * body type static so that it sticks.
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
		float radius = bulletBigTexture.getRegionWidth()/(2.0f*scale.x);
		Projectile bullet = new Projectile(enemy.getType(), enemy.getX(), enemy.getY()+offset, radius);
		
	    bullet.setName("bullet");
		bullet.setDensity(HEAVY_DENSITY);
	    bullet.setDrawScale(scale);
	    bullet.setTexture(bulletBigTexture);
	    bullet.setBullet(true);
	    bullet.setGravityScale(0);
		bullet.setLinearVelocity(enemy.getProjVelocity());
		bullet.setSpace(enemy.getSpace());
		addQueuedObject(bullet);
		
		SoundController.getInstance().play(PEW_FILE, PEW_FILE, false, EFFECT_VOLUME);

		// Reset the firing cooldown.
		enemy.coolDown(false);
	}

	/**
	 * Prints the (x,y) coordinate of the current mouse position
	 * when the 'X' key is pressed. For debug purposes.
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
	public void drawObjectInWorld (){
		for(Obstacle obj : objects) {
			if (obj.getSpace() == 3) {
				obj.draw(canvas);
			} else if (shifted && (obj.getSpace() == 2)) { //past world
				obj.draw(canvas);
			} else if (!shifted && (obj.getSpace() == 1)) { //present world
				obj.draw(canvas);
			}
		}
	}

	/**
	 * Draws a line indicating the direction and distance of the dash.
	 */
	public void drawIndicator(GameCanvas canvas) {
		if (avatar.isDashing()) return; // do not draw while player is dashing
		Vector2 avPos = avatar.getPosition();
		Vector2 mPos = InputController.getInstance().getMousePosition();
		Vector2 startPos = avPos.cpy().scl(scale);
		Vector2 mousePos = mPos.cpy().scl(scale);
		Vector2 alteredPos = mousePos.sub(startPos).nor();
		float dist = Math.min(avatar.getDashRange(), avPos.dst(mPos));
		Vector2 endPos = alteredPos.scl(dist).scl(scale);
		endPos.add(startPos);
		canvas.drawIndicator(startPos.x, startPos.y, endPos.x, endPos.y);
	}

	/**
	 * Draws the debug of an object if it is in this world
	 *
	 */
	public void drawDebugInWorld(){
		for(Obstacle obj : objects) {
			if (obj.getSpace() == 3){
				obj.drawDebug(canvas);
			}
			else if (shifted && (obj.getSpace()==2)) {obj.drawDebug(canvas);}
			else if (!shifted && (obj.getSpace()==1)) {obj.drawDebug(canvas);}
		}
	}
	/**
	 * Draw the physics objects to the canvas
	 *
	 * For simple worlds, this method is enough by itself.  It will need
	 * to be overriden if the world needs fancy backgrounds or the like.
	 *
	 * The method draws all objects in the order that they were added.
	 *
	 * @param delta The drawing context
	 */
	public void draw(float delta) {
		canvas.clear();

		canvas.begin();
		if(shifted) {
			canvas.draw(backgroundTexture, Color.PINK, 0, 0,
					backgroundTexture.getRegionWidth(), backgroundTexture.getRegionHeight());
		}else{
			canvas.draw(backgroundTexture, 0, 0);
		}
		drawObjectInWorld ();
		canvas.end();

		drawIndicator(canvas);

		if (debug) {
			canvas.beginDebug();
			drawDebugInWorld();
			canvas.endDebug();
		}
	}



	/** Unused ContactListener method */
	public void postSolve(Contact contact, ContactImpulse impulse) {}
	/** Unused ContactListener method */
	public void preSolve(Contact contact, Manifold oldManifold) {}

	/**
	 * @return avatar object
	 */
	public Avatar getAvatar() { return avatar; }

	public Enemy getEnemy() { return enemy; }

	/**
	 * @return goal door object
	 */
	public BoxObstacle getGoalDoor() { return goalDoor; }
}