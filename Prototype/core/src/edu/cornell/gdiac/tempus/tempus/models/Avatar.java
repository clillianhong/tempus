package edu.cornell.gdiac.tempus.tempus.models;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.box2d.*;
import com.badlogic.gdx.utils.JsonValue;
import edu.cornell.gdiac.tempus.GameCanvas;
import edu.cornell.gdiac.tempus.InputController;
import edu.cornell.gdiac.tempus.obstacle.CapsuleObstacle;
import edu.cornell.gdiac.util.FilmStrip;
import edu.cornell.gdiac.util.JsonAssetManager;

import static edu.cornell.gdiac.tempus.tempus.models.EntityType.PAST;
import static edu.cornell.gdiac.tempus.tempus.models.EntityType.PRESENT;


/**
 * Player avatar for the plaform game.
 *
 * Note that this class returns to static loading.  That is because there are
 * no other subclasses that we might loop through.
 */
public class Avatar extends CapsuleObstacle {
    /**
     * Enumeration to identify the state of the avatar
     */
    public enum AvatarState {
        /** When avatar is on a platform */
        STANDING,
        /** When avatar is crouching before a dash */
        CROUCHING,
        /** When avatar is dashing in air */
        DASHING,
        /** When avatar is falling in air */
        FALLING,
        /** When the avatar is dead */
        DEAD
    };

    // Physics constants
    /** The density of the avatar */
    private static final float DENSITY = 1.0f;

    /** The factor to multiply by the input */
    private static final float FORCE = 20.0f;
    /** The amount to slow the character down */
    private static final float DAMPING = 10.0f;
    /** The dude is a slippery one */
    private static final float FRICTION = 0.0f;
    /** The maximum character speed */
    private static final float MAXSPEED = 100.0f;
    /** The impulse for the character jump */
    private static final float JUMP_IMPULSE = 5.5f;
    /** Cooldown (in animation frames) for jumping */
    private static final int JUMP_COOLDOWN = 30;
    /** Cooldown (in animation frames) for shooting */
    private static final int SHOOT_COOLDOWN = 40;
    /** Height of the sensor attached to the player's feet */
    private static final float SENSOR_HEIGHT = 0.05f;
    /** Max number of dashes afforded to player **/
    private static final int maxDashes = 2;
    /** Identifier to allow us to track the sensor in ContactListener */
    private static final String SENSOR_NAME = "DudeGroundSensor";
    //added for prototype
    /** Distance for dashing across screen */
    private static final float DASH_RANGE = 2.5f;
    /** Dash force multiplier */
    private static float dashForce = 1000;
    /** Array containing orientation vectors based on the enums for orientation */
    private static final Vector2 [] orients =
            {new Vector2(0,1), new Vector2(0,-1), new Vector2(-1,0), new Vector2(1,0)};
    private static final String LEFT_SENSOR_NAME = "DudeLeftSensor";
    private static final String RIGHT_SENSOR_NAME = "DudeRightSensor";
    private static final String TOP_SENSOR_NAME = "DudeTopSensor";
    private static final String CORE_SENSOR_NAME = "DudeCenterSensor";

    // This is to fit the image to a tigher hitbox
    /** The amount to shrink the body fixture (vertically) relative to the image */
    private static final float VSHRINK = 0.0225f * 0.5f;
    /** The amount to shrink the body fixture (horizontally) relative to the image */
    private static final float HSHRINK = 0.024f * 0.9f;
    /** The amount to shrink the sensor fixture (horizontally) relative to the image */
    private static final float SSHRINK = 0.6f;

    /** The current immortality frame */
    private int immortality;
    private GameCanvas canvas;
    /** The threshold for immortality */
    private static final int threshold = 120;
    private int shifted;
    private boolean spliced;
    public float width;
    public float height;
    private float density;
    private int dashCounter;

    private boolean wasDamaged;
    private boolean hitByProjctile;
    private int projectileTicks;
    /** number of frames since avatar took damage from enemy contact */
    private int enemyTicks;
    /** The current lives the avatar has **/
    private int lives;
    /** The current state of the avatar **/
    private AvatarState state;

    public AvatarState animationState;
    /** The avatar of the character at the end of the dash*/
    private Vector2 endDashVelocity;
    /** Number of dashes left*/
    private int numDashes;
    /** The current horizontal movement of the character */
    private float   movement;
    /** Which direction is the character facing */
    private boolean faceRight;
    /** How long until we can jump again */
    private int jumpCooldown;
    /** Whether we are actively jumping */
    private boolean isJumping;
    /** How long until we can shoot again */
    private int shootCooldown;
    /** Whether our feet are on the ground */
    private boolean isGrounded;
    /** Whether we are actively shooting */
    private boolean isShooting;

//    /** Ground sensor to represent our feet */
//    private Fixture sensorFixture;
//    private PolygonShape sensorShape;
//    /** Left sensor to determine sticking on the left side */
    private Fixture sensorFixtureLeft;
    private PolygonShape sensorShapeLeft;
//    /** Right sensor to determine sticking on the right side */
    private Fixture sensorFixtureRight;
    private PolygonShape sensorShapeRight;
//    /** Top sensor to determine sticking on the top */
    private Fixture sensorFixtureTop;
    private PolygonShape sensorShapeTop;
//    /** Core sensor for line of sight */
//    private Fixture sensorFixtureCore;
//    private PolygonShape sensorShapeCore;

    private boolean enemyContact;
    /** how long ago the character started dashing */
    private int startedDashing;
    /** Whether we are actively dashing */
    private boolean isDashing;
    /** Whether the initial dash force has been applied */
    private boolean hasDashed;
    /** Whether we are actively sticking */
    private boolean isSticking;
    /** Whether already stuck */
    private boolean wasSticking;
    /** Whether we are actively holding */
    public boolean isHolding;
    /** the current orientation of the player */
    private float newAngle;
    /** the dash distance of the player (max is DASH_RANGE) */
    private float dashDistance;
    /** the dash starting position */
    private Vector2 dashStartPos;
    /** the dash direction */
    private Vector2 dashDirection;
    /** the bullet the character is currently holding */
    private Projectile heldBullet;
    /** the platform the character was most recently on */
    private Platform currentPlat;

    /** Cache for internal force calculations */
    private Vector2 forceCache = new Vector2();

    /** Texture filmstrip for avatar standing */
    private FilmStrip avatarStandingTexture;
    /** Texture filmstrip for avatar dashing */
    private FilmStrip avatarCrouchingTexture;
    /** Texture filmstrip for avatar dashing */
    private FilmStrip avatarDashingTexture;
    /** Texture filmstrip for avatar falling */
    private FilmStrip avatarFallingTexture;

    // ANIMATION FIELDS
    /** The texture filmstrip for the current animation */
    private FilmStrip currentStrip;
    /** The texture filmstrip for the standing animation */
    private FilmStrip standingStrip;
    /** The texture filmstrip for the crouching animation */
    private FilmStrip crouchingStrip;
    /** The texture filmstrip for the dashing animation */
    private FilmStrip dashingStrip;
    /** The texture filmstrip for the falling animation */
    private FilmStrip fallingStrip;

    /** The frame rate for the animation */
    private static final float FRAME_RATE = 6;
    /** The frame cooldown for the animation */
    private static float frame_cooldown = FRAME_RATE;

    // CAUGHT PROJECTILE TEXTURES
    /** The texture for the caught projectile of type present */
    private TextureRegion projPresentCaughtTexture;
    /** The texture for the caught projectile of type past */
    private TextureRegion projPastCaughtTexture;

    public void setDashCounter( int n) {
        dashCounter = n;
    }

    public int getShifted(){
        return shifted;
    }

    public void setShifted(int s){
        shifted = s;
    }

    public void setSpliced(boolean s){
        spliced = s;
    }

    /** returns true if the avatar touched an enemy recently */
    public boolean getEnemyContact() {return enemyContact;}

    /** sets whether the avatar touched an enemy recently */
    public void setEnemyContact(boolean e) {enemyContact = e;}

    public void setProjectileContact(boolean p) {hitByProjctile = p;}
    /**
     * Returns left/right movement of this character.
     *
     * This is the result of input times dude force.
     *
     * @return left/right movement of this character.
     */
    public float getMovement() {
        return movement;
    }

    /**
     * Sets left/right movement of this character.
     *
     * This is the result of input times dude force.
     *
     * @param value left/right movement of this character.
     */
    public void setMovement(float value) {
//         Change facing if appropriate
        if (value < 0) {
            faceRight = false;
        } else if (value > 0) {
            faceRight = true;
        }
    }

    public void setAnimationState(AvatarState s){
        this.animationState = s;
    }

    /**
     * Returns true if the dude is actively firing.
     *
     * @return true if the dude is actively firing.
     */
    public boolean isShooting() {
        return isShooting && shootCooldown <= 0;
    }

    /**
     * Returns true if the dude is actively dashing.
     *
     * @return true if the dude is actively dashing.
     */
    public boolean isDashing() {
        return isDashing;
    }

    private Vector3 cursor;
    /** Enacts a dash
     *
     * @return true if there the player did a dash (still has dashes left) **/
    public boolean dash() {
        if(isSticking){
            this.setDashing(false);
            numDashes = maxDashes;
        }
        boolean candash = canDash();
        if(candash){
            cursor = new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0);
            cursor = camera.unproject(cursor);
            cursor.scl(1/scale.x, 1/scale.y,0);
            Vector2 mousePos = new Vector2(cursor.x , cursor.y );
//            Vector2 mousePos = canvas.getViewport().unproject(InputController.getInstance().getMousePosition());
            this.setBodyType(BodyDef.BodyType.DynamicBody);
            this.setSticking(false);
            this.setWasSticking(false);
            this.setDashing(true);
            this.setDashStartPos(this.getPosition().cpy());
            this.setDashDistance(Math.min(this.getDashRange(), mousePos.cpy().sub(this.getPosition()).len()));
            this.setDashForceDirection(mousePos.cpy().sub(this.getPosition()));
            System.out.println("dash angle: " + mousePos.cpy().sub(this.getPosition()).angleRad());
            System.out.println("avatar angle: " + getAngle());
            //this.setStartedDashing(2);
            if (Math.abs(mousePos.cpy().sub(this.getPosition()).angleRad() + Math.PI / 2 - getAngle()) > Math.PI / 2.5f) {
                this.setDimension(width / 4f, height / 4f);
                this.setDensity(density * 16f);
            }
            //dashCounter = 10;
            numDashes--;
        }
        return candash;
    }

    /**
     * Sets whether or not avatar is dashing.
     *
     */
    public void setDashing(boolean d) {
        isDashing = d;
        if(d) {hasDashed = false;}
    }

    /**
     * Returns true if the dude is actively dashing.
     *
     * @return true if the dude is actively dashing.
     */
    public boolean canDash() {
        return (numDashes > 0);
    }


    /**
     * Resets the dash number to max dashes.
     *
     * @return true if the dude is actively dashing.
     */
    public void resetDashNum(int n) {
        if (n == -1) {
            numDashes = maxDashes;
        } else {
            numDashes = n;
        }
    }

    private OrthographicCamera camera;
    public void setCanvas(OrthographicCamera canv){
        camera = canv;
    }
    /**
     * Returns the avatar orientation enum
     *
     * @return true if the dude is actively dashing.
     */
    public float getNewAngle() {
        return newAngle;
    }

    /**
     * Returns the max dash range
     *
     * @return max dash range
     */
    public float getDashRange(){ return DASH_RANGE; }

    /**
     * Returns the current number of lives the avatar has
     *
     * @return lives
     */
    public int getLives() {
        return lives;
    }

    /** sets the number of lives the avatar has */
    public void setLives(int lives){
        this.lives = lives;
    }

    /**
     * Decrements a life and returns whether the avatar is still alive
     *
     * @return true if lives > 0, else false
     */
    public boolean removeLife() {
        if(!isImmortal()){
            this.lives = lives - 1;
            resetImmortality();
        }
        return lives > 0;
    }

    /**
     * Returns dash distance
     *
     * @return dash distance
     */
    public float getDashDistance() {
        return dashDistance;
    }

    /**
     * Sets the dash distance
     *
     */
    public void setDashDistance(float dist) {
        dashDistance = dist;
    }

    /** sets how long ago the character started dashing */
    public void setStartedDashing(int s) { startedDashing = s;}

    /** returns how long ago the character started dashing */
    public int getStartedDashing() { return startedDashing; }

    /** sets the last platform the character was on */
    public void setCurrentPlatform(Platform p) { currentPlat = p; }

    /** returns the last platform the character was on */
    public Platform getCurrentPlatform() { return currentPlat; }
    
    /**
     * Returns true if the dude is actively dashing.
     *
     * @return true if the dude is actively dashing.
     */
    public Vector2 getDashStartPos() {
        return dashStartPos;
    }

    /**
     * Sets whether or not avatar is dashing.
     *
     */
    public void setDashStartPos(Vector2 pos) {
        dashStartPos = pos;
    }

    /**
     *  Sets the dash direction
     */
    public void setDashForceDirection(Vector2 dpos){
        dashDirection = dpos;
    }

    /**
     *
     * @return true if the Avatar is dead
     */
    public boolean isDead(){
        return (lives < 1) || (state == AvatarState.DEAD);
    }
    /**
     * Sets whether the dude is actively firing.
     *
     * @param value whether the dude is actively firing.
     */
    public void setShooting(boolean value) {
        isShooting = value;
    }

    /**
     * Returns true if the dude is actively jumping.
     *
     * @return true if the dude is actively jumping.
     */
    public boolean isJumping() {
        return isJumping && isGrounded && jumpCooldown <= 0;
    }

    /**
     * Sets whether the dude is actively jumping.
     *
     * @param value whether the dude is actively jumping.
     */
    public void setJumping(boolean value) {
        isJumping = value;
    }

    /**
     * Returns true if the dude is on the ground.
     *
     * @return true if the dude is on the ground.
     */
    public boolean isGrounded() {
        return isGrounded;
    }

    /**
     * Returns true if the dude is sticking.
     *
     * @return true if the dude is sticking
     */
    public boolean isSticking() {
        return isSticking;
    }

    /**
     * Returns true if the dude is sticking.
     *
     * @return true if the dude is sticking
     */
    public void setSticking(boolean s) {
        isSticking = s;
    }

    /**
     * Returns true if the dude is holding.
     *
     * @return true if the dude is holding
     */
    public boolean isHolding() {return isHolding;}

    /**
     * Returns true if the dude is holding.
     *
     */
    public void setHolding(boolean s) { isHolding = s;}

    /**
     * Sets whether the avatar is already sticking
     *
     * @param s whether the avatar was sticking
     */
    public void setWasSticking(boolean s) {
        wasSticking = s;
    }

    /**
     * Returns whether the avatar is already sticking
     *
     * @return whether the avatar is already sticking
     */
    public boolean getWasSticking() {
        return wasSticking;
    }

    /**
     * Sets whether the dude is on the ground.
     *
     * @param value whether the dude is on the ground.
     */
    public void setGrounded(boolean value) {
        isGrounded = value;
        isSticking = value;
    }

    /**
     * Sets new angle value out of sync
     *
     * @param value the angle to update body angle to after the world step
     */
    public void setNewAngle(float value) {
        newAngle = value;
    }

    /**
     * Returns how much force to apply to get the dude moving
     *
     * Multiply this by the input to get the movement value.
     *
     * @return how much force to apply to get the dude moving
     */
    public float getForce() {
        return FORCE;
    }

    /**
     * Returns ow hard the brakes are applied to get a dude to stop moving
     *
     * @return ow hard the brakes are applied to get a dude to stop moving
     */
    public float getDamping() {
        return DAMPING;
    }

    /**
     * Returns the upper limit on dude left-right movement.
     *
     * This does NOT apply to vertical movement.
     *
     * @return the upper limit on dude left-right movement.
     */
    public float getMaxSpeed() {
        return MAXSPEED;
    }

    /**
     * Returns the name of the ground sensor
     *
     * This is used by ContactListener
     *
     * @return the name of the ground sensor
     */
    public String getSensorName() {
        return SENSOR_NAME;
    }

    /** Returns the name of the left sensor
     *
     * This is used by ContactListener
     *
     * @return the name of the left sensor
     */
    public String getLeftSensorName() {
        return LEFT_SENSOR_NAME;
    }

    /** Returns the name of the right sensor
     *
     * This is used by ContactListener
     *
     * @return the name of the right sensor
     */
    public String getRightSensorName() {
        return RIGHT_SENSOR_NAME;
    }

    /**
     * Returns the name of the top sensor
     *
     * @return the name of the top sensor
     */
    public String getTopSensorName() {
        return TOP_SENSOR_NAME;
    }

    public String getCoreSensorName() {
        return CORE_SENSOR_NAME;
    }

    /**
     * Returns the height/width of the sensor
     *
     * @return the height/width of the sensor
     */
    public float getSensorHeight() {
        return SENSOR_HEIGHT;
    }

    /**
     * Returns true if this character is facing right
     *
     * @return true if this character is facing right
     */
    public boolean isFacingRight() {
        return faceRight;
    }

    /**
     * Sets the held projectile of the player
     *
     * @param bullet projectile to set held bullet
     */
    public void setHeldBullet(Projectile bullet) { heldBullet = bullet; }

    /**
     * Returns the currently held projectile of the player
     *
     * @return the currently held projectile
     */
    public Projectile getHeldBullet() { return heldBullet; }

    /**
     * Returns the current immortality frame of the player
     * @return the current immortality frame
     */
    public int getImmmortality(){return immortality;}

    /**
     * Decrements the immortality frame
     */
    public void  decImmortality(){
        if (immortality <= 0) {
            immortality = 0;
        }
        immortality -- ;
    }

    /**
     * Returns whether the player is immortal
     * @return whether the player is immortal
     */
    public boolean isImmortal(){
        return immortality > 0;
    }

    /**
     * Resets the immortality when the player is damaged while not being immortal.
     */
    public void resetImmortality(){
        immortality = threshold;
    }


    public void resetDashes(){
        numDashes = maxDashes;
    }

    public void decDash(){
        numDashes--;
    }
    /**
     * Creates a new dude avatar with degenerate settings.
     */
    public Avatar (){
        super(0,0,1.0f,0.5f);
//        setFriction(FRICTION);  /// HE WILL STICK TO WALLS IF YOU FORGET
        setFixedRotation(true);

        // Gameplay attributes
        lives = 5;
        state = AvatarState.STANDING;
        animationState = state;
        isGrounded = false;
        isShooting = false;
        isJumping = false;
        faceRight = true;
        isDashing = false;
        newAngle = 0;
        isSticking = false;
        dashDistance = DASH_RANGE;
        startedDashing = 0;
        numDashes = maxDashes;
        endDashVelocity = new Vector2(0f,0f);
        enemyContact = false;
        shootCooldown = 0;
        jumpCooldown = 0;
        isHolding = false;
        wasDamaged = false;
        shifted = 0;
        spliced = false;
        dashCounter = 0;
    }

    private Vector2 scale;
    /**
     * Creates a new dude avatar at the given position.
     *
     * The size is expressed in physics units NOT pixels.  In order for
     * drawing to work properly, you MUST set the drawScale. The drawScale
     * converts the physics units to pixels.
     *
     * @param x  		Initial x position of the avatar center
     * @param y  		Initial y position of the avatar center
     * @param width		The object width in physics units
     * @param height	The object width in physics units
     */
    public Avatar(float x, float y, float width, float height, Vector2 scale) {
        super(x,y, width*HSHRINK * scale.x,height*VSHRINK * scale.y);
        setDensity(DENSITY);
//        setFriction(FRICTION);  /// HE WILL STICK TO WALLS IF YOU FORGET
        setFixedRotation(true);
        this.scale = scale;
        // Gameplay attributes
        lives = 3;
        state = AvatarState.STANDING;
        animationState = state;
        isGrounded = false;
        isShooting = false;
        isJumping = false;
        faceRight = true;
        isDashing = false;
        newAngle = 0;
        isSticking = false;
        dashDistance = DASH_RANGE;
        dashStartPos = new Vector2(x,y);
        startedDashing = 0;
        canvas = null;
        numDashes = maxDashes;
        endDashVelocity = new Vector2(0f,0f);
        enemyTicks = 0;
        projectileTicks = 0;
        shootCooldown = 0;
        jumpCooldown = 0;
        setName("dude");
        isHolding = false;
        wasDamaged = false;
        enemyContact = false;
        spliced = false;
    }

    /**
     * Initializes the avatar via the given JSON value
     *
     * The JSON value has been parsed and is part of a bigger level file.  However,
     * this JSON value is limited to the dude subtree
     *
     * @param json	the JSON subtree defining the dude
     */
    public void initialize(JsonValue json) {
        float [] shrink = json.get("shrink").asFloatArray();
        float [] pos = json.get("pos").asFloatArray();
        TextureRegion avatarTexture = JsonAssetManager.getInstance().getEntry(json.get("texture").asString(), TextureRegion.class);
        setDashStartPos(new Vector2 (pos[0],pos[1]));
        float dwidth = avatarTexture.getRegionWidth();
		float dheight = avatarTexture.getRegionHeight();
		setDimension(dwidth*shrink[0],dheight*shrink[1] * 1.5f);
		width = dwidth*shrink[0];
		height = dheight*shrink[1] * 1.5f;
        setPosition(pos[0],pos[1]);
		setTexture(avatarTexture);
        setDensity(json.get("density").asFloat());
        density = getDensity();
        setBodyType(json.get("bodytype").asString().equals("static") ? BodyDef.BodyType.StaticBody : BodyDef.BodyType.DynamicBody);

        avatarStandingTexture = JsonAssetManager.getInstance().getEntry(json.get("avatarstanding").asString(), FilmStrip.class);
        avatarCrouchingTexture = JsonAssetManager.getInstance().getEntry(json.get("avatarcrouching").asString(), FilmStrip.class);
        avatarDashingTexture = JsonAssetManager.getInstance().getEntry(json.get("avatardashing").asString(), FilmStrip.class);
        avatarFallingTexture = JsonAssetManager.getInstance().getEntry(json.get("avatarfalling").asString(), FilmStrip.class);

        setFilmStrip(Avatar.AvatarState.STANDING, avatarStandingTexture);
        setFilmStrip(Avatar.AvatarState.CROUCHING, avatarCrouchingTexture);
        setFilmStrip(Avatar.AvatarState.DASHING, avatarDashingTexture);
        setFilmStrip(Avatar.AvatarState.FALLING, avatarFallingTexture);

        projPresentCaughtTexture = JsonAssetManager.getInstance().getEntry("projpresentcaught", TextureRegion.class);
        projPastCaughtTexture = JsonAssetManager.getInstance().getEntry("projpastcaught", TextureRegion.class);
        setCaughtProjTexture(PRESENT, projPresentCaughtTexture);
        setCaughtProjTexture(PAST, projPastCaughtTexture);
		setName(json.name());
    }

    /**
     * Creates the physics Body(s) for this object, adding them to the world.
     *
     * This method overrides the base method to keep your ship from spinning.
     *
     * @param world Box2D world to store body
     *
     * @return true if object allocation succeeded
     */
    public boolean activatePhysics(World world) {
        // create the box from our superclass
        if (!super.activatePhysics(world)) {
            return false;
        }
        FixtureDef sensorDef = new FixtureDef();
        sensorDef.density = 0;

        Vector2 sensorCenterTop = new Vector2(0, getHeight()/ 2);
        sensorShapeTop = new PolygonShape();
        sensorShapeTop.setAsBox(3 * getWidth()/ 4, getHeight() / 2, sensorCenterTop, 0.0f);
        sensorDef.shape = sensorShapeTop;
        sensorDef.isSensor = true;
        sensorDef.filter.groupIndex = -1;
        sensorFixtureTop = body.createFixture(sensorDef);
        sensorFixtureTop.setUserData(getTopSensorName());
        return true;
    }



    /*public void wingSensorsActive(boolean active){
        if (active) {
            // Ground Sensor
            // -------------
            // We only allow the dude to jump when he's on the ground.
            // Double jumping is not allowed.
            //
            // To determine whether or not the dude is on the ground,
            // we create a thin sensor under his feet, which reports
            // collisions with the world but has no collision response.
//        Vector2 sensorCenter = new Vector2(0, -getHeight() / 2);
            FixtureDef sensorDef = new FixtureDef();
            sensorDef.density = DENSITY;
            sensorDef.isSensor = true;
//        sensorShape = new PolygonShape();
//        sensorShape.setAsBox(getWidth() / 4.0f - 2.0f * SENSOR_HEIGHT, SENSOR_HEIGHT, sensorCenter, 0.0f);
//        sensorDef.shape = sensorShape;
//
//        sensorFixture = body.createFixture(sensorDef);
//        sensorFixture.setUserData(getSensorName());

            // To determine whether the body collides on the left side
            Vector2 sensorCenterLeft = new Vector2(-getWidth() / 2, 0);
            sensorShapeLeft = new PolygonShape();
            sensorShapeLeft.setAsBox(SENSOR_HEIGHT, getHeight() / 5.0f - 2.0f * SENSOR_HEIGHT, sensorCenterLeft, 0.0f);
            sensorDef.shape = sensorShapeLeft;

            sensorFixtureLeft = body.createFixture(sensorDef);
            sensorFixtureLeft.setUserData(getLeftSensorName());


//
//        // To determine whether the body collides on the right side
            Vector2 sensorCenterRight = new Vector2(getWidth() / 2, 0);
            sensorShapeRight = new PolygonShape();
            sensorShapeRight.setAsBox(SENSOR_HEIGHT, getHeight() / 5.0f - 2.0f * SENSOR_HEIGHT, sensorCenterRight, 0.0f);
            sensorDef.shape = sensorShapeRight;

            sensorFixtureRight = body.createFixture(sensorDef);
            sensorFixtureRight.setUserData(getRightSensorName());
//
//        // To determine whether the body collides on the top side
//        Vector2 sensorCenterTop = new Vector2(0, getHeight() / 2);
//        sensorShapeTop = new PolygonShape();
//        sensorShapeTop.setAsBox(getWidth() / 4.0f - 2.0f * SENSOR_HEIGHT, SENSOR_HEIGHT, sensorCenterTop, 0.0f);
//        sensorDef.shape = sensorShapeTop;
//
//        sensorFixtureTop = body.createFixture(sensorDef);
//        sensorFixtureTop.setUserData(getTopSensorName());
//
//        Vector2 sensorCenterCore = new Vector2(0, 0);
//        sensorShapeCore = new PolygonShape();
//        sensorShapeCore.setAsBox(SENSOR_HEIGHT * 4, SENSOR_HEIGHT * 4, sensorCenterCore, 0.0f);
//        sensorDef.shape = sensorShapeCore;
//
//        sensorFixtureCore = body.createFixture(sensorDef);
//        sensorFixtureCore.setUserData(getCoreSensorName());
        }
    }*/
//    public Fixture getSensorFixtureCore() { return sensorFixtureCore; }

    /**
     * Applies the force to the body of this dude
     *
     * This method should be called after the force attribute is set.
     */
    public void applyForce() {
        if (!isActive()) {
            return;
        }

//        // Don't want to be moving. Damp out player motion
//        if (getMovement() == 0f) {
//            forceCache.set(-getDamping()*getVX(),0);
//            body.applyForce(forceCache,getPosition(),true);
//        }

        //apply dash force only ONCE per dash
        if(isDashing && !hasDashed){
//            System.out.println("APPLYING FORCE");
            //linearly interpolate dashForce
//            System.out.println("dash direction raw: " + dashDirection);
//            System.out.println("dash direction norm: " + dashDirection.nor());
            forceCache.set(dashDirection.nor().scl(dashForce));
            body.applyForce(forceCache,getPosition(), true);
            hasDashed = true;

        }

        // Velocity too high, clamp it
        if (Math.abs(getVX()) >= getMaxSpeed()) {
            setVX(Math.signum(getVX())*getMaxSpeed());
        } else {
            forceCache.set(getMovement(),0);
            body.applyForce(forceCache,getPosition(),true);
        }

//        // Jump!
//        if (isJumping()) {
//            forceCache.set(0, JUMP_IMPULSE);
//            body.applyLinearImpulse(forceCache,getPosition(),true);
//        }
    }


    /**
     * Updates the object's physics state (NOT GAME LOGIC).
     *
     * We use this method to reset cooldowns.
     *
     * @param dt Number of seconds since last animation frame
     */
    public void update(float dt) {
        //System.out.println(lives);
        wingsActive();
        if (spliced){
//            System.out.println("spliced!");
            setLinearVelocity(new Vector2(0,0));
            if (currentPlat.getName().contains("capsule")) {
                setPosition(currentPlat.getPosition().cpy().add(new Vector2(getWidth() * 3 / 2, getHeight() * 3)));

            } else if (!(currentPlat.getName().contains("tall") || currentPlat.getName().contains("pillar"))) {
                setPosition(currentPlat.getPosition().cpy().add(new Vector2(getWidth() * 3 / 2, getHeight() * 4)));
            } else
            {
                setPosition(currentPlat.getPosition().cpy().add(new Vector2(getWidth() * 3 / 2, getHeight() * 6)));
            }
            spliced = false;
        }
        // Apply cooldowns
        /*if (hitByProjctile && isHolding){
                removeLife();
                hitByProjctile = false;
        }
        if (hitByProjctile && projectileTicks == 0) {
            projectileTicks = 20;
        }
        if (projectileTicks > 0) {
            projectileTicks--;
        }
        if (projectileTicks == 1) {
            if (!isHolding()) {
                removeLife();
            }
            projectileTicks = 0;
            hitByProjctile = false;
        }
        if(!isSticking){
            setAngle(0);
        }*/
        //check if dash must end
        if(isDashing) {
            //setCurrentPlatform(null);
            float dist = getPosition().dst(getDashStartPos());
            if (dist > getDashDistance()) {
//                System.out.println("DASHED TOO FAR");
                setDashing(false);
                setAngle(0);
                //System.out.println("VELOCITY: " + getLinearVelocity());
                endDashVelocity = getPosition();
//                this.setLinearVelocity(new Vector2(0,0));
//                System.out.println(getDashDistance());
                setLinearVelocity(getLinearVelocity().cpy().nor().scl(getDashDistance() * 3, getDashDistance() * 4));
//                setLinearVelocity(new Vector2(0,0));
            }
        } else {
            setDimension(width, height);
            setDensity(density);
        }

        if (dashCounter > 1) {
            dashCounter--;
        } else if (dashCounter == 1){
            dashCounter = 0;
            setDimension(width, height);
            setDensity(density);
        }

        if (isJumping()) {
            jumpCooldown = JUMP_COOLDOWN;
        } else {
            jumpCooldown = Math.max(0, jumpCooldown - 1);
        }

        if (isShooting()) {
            shootCooldown = SHOOT_COOLDOWN;
        } else {
            shootCooldown = Math.max(0, shootCooldown - 1);
        }

        super.update(dt);
    }

    /**
     * Sets the animation node for the given state
     *
     * @param state enumeration to identify the state
     * @param strip the animation for the given state
     */
    public void setFilmStrip(AvatarState state, FilmStrip strip) {
        switch (state) {
            case STANDING:
                standingStrip = strip;
                break;
            case CROUCHING:
                crouchingStrip = strip;
                break;
            case DASHING:
                dashingStrip = strip;
                break;
            case FALLING:
                fallingStrip = strip;
                break;
            default:
                assert false : "Invalid AvatarState enumeration";
        }
    }

    /**
     * Animates the given state.
     *
     * @param state The reference to the rocket burner
     * @param shouldLoop Whether the animation should loop
     */
    public void animate(AvatarState state, boolean shouldLoop) {
        switch (state) {
            case STANDING:
                currentStrip = standingStrip;
                break;
            case CROUCHING:
                currentStrip = crouchingStrip;
                break;
            case DASHING:
                currentStrip = dashingStrip;
                break;
            case FALLING:
                currentStrip = fallingStrip;
                break;
            default:
                assert false : "Invalid AvatarState enumeration";
        }

        // when beginning a new state, set frame to first frame
        if (animationState != state) {
            currentStrip.setFrame(0);
        }

        // Adjust animation speed
        if (frame_cooldown > 0) {
            frame_cooldown--;
            return;
        } else frame_cooldown = FRAME_RATE;

        // Manage current frame to draw
        if (currentStrip.getFrame() < currentStrip.getSize()-1) {
            currentStrip.setFrame(currentStrip.getFrame() + 1);
        } else {
            if (shouldLoop) currentStrip.setFrame(0); // loop animation
            else return; // play animation once
        }
    }

    /**
     * Sets the texture for the given projectile type
     *
     * @param type the type of projectile
     * @param texture the texture for the given projectile type
     */
    public void setCaughtProjTexture(EntityType type, TextureRegion texture) {
        switch (type) {
            case PRESENT:
                projPresentCaughtTexture = texture;
                break;
            case PAST:
                projPastCaughtTexture = texture;
                break;
            default:
                assert false : "Invalid projectile type";
        }
    }

    /**
     * Draws the physics object.
     *
     * @param canvas Drawing context
     */
    public void draw(GameCanvas canvas) {
        // Note: Restrict angle to the top horizontal because
        // flipping the avatar when they are sticking
        // below a platform looks off
        float faceDirection = 1.0f;
        if (getAngle() > -0.3 && getAngle() < 0.3) {
            faceDirection = faceRight ? 1.0f : -1.0f;
        }
        float angle = getAngle();
        if (!isSticking){
            angle = 0;
        }
        // Draw avatar body
        if (currentStrip != null) {
            if(isImmortal()) { //If the player is immortal, make the player blink.
                if (getImmmortality()%20<10) {
                    canvas.draw(currentStrip, (new Color(1, 1, 1, 0.5f)), origin.x + 84f, origin.y + 60f,
                            getX() * drawScale.x, getY() * drawScale.y, angle,
                            0.02f * drawScale.x * faceDirection, 0.01875f * drawScale.y);
                }
                else {
                    canvas.draw(currentStrip, (new Color(1, 1, 1, 1f)), origin.x + 84f, origin.y + 60f,
                            getX() * drawScale.x, getY() * drawScale.y, angle,
                            0.02f * drawScale.x * faceDirection, 0.01875f * drawScale.y);
                }
            }
            else{
                canvas.draw(currentStrip, Color.WHITE, origin.x + 84f, origin.y + 60f,
                        getX() * drawScale.x, getY() * drawScale.y, angle,
                        0.02f * drawScale.x * faceDirection, 0.01875f * drawScale.y);
            }
        }

        // If player is holding a projectile then draw the held projectile
        // Caught projectile should be drawn at the center of the player's horns
        // TODO: fix rotation of caught projectile
        if (heldBullet != null) {
            EntityType projType = heldBullet.getType();
            switch (projType) {
                case PRESENT:
                    canvas.draw(projPresentCaughtTexture, Color.WHITE,origin.x + 10,origin.y,
                            getX()*drawScale.x + 10,getY()*drawScale.y, angle,0.005f * drawScale.x,0.005f * drawScale.y);
                    break;
                case PAST:
                    canvas.draw(projPastCaughtTexture, Color.WHITE,origin.x,origin.y,
                            getX()*drawScale.x,getY()*drawScale.y, angle,0.005f * drawScale.x,0.005f * drawScale.y);
                    break;
                default:
                    assert false : "Invalid projectile type";
            }
        }
    }

    /**
     * Draws the outline of the physics body.
     *
     * This method can be helpful for understanding issues with collisions.
     *
     * @param canvas Drawing context
     */
    public void drawDebug(GameCanvas canvas) {
        super.drawDebug(canvas);
//       canvas.drawPhysics(sensorShape,Color.RED,getX(),getY(),getAngle(),drawScale.x,drawScale.y);
        /*if (sensorFixtureRight != null){
            canvas.drawPhysics((PolygonShape) sensorFixtureRight.getShape(),Color.RED,getX(),getY(),getAngle(),drawScale.x,drawScale.y);
        }
        if (sensorFixtureLeft != null) {
            canvas.drawPhysics((PolygonShape) sensorFixtureLeft.getShape(), Color.RED, getX(), getY(), getAngle(), drawScale.x, drawScale.y);
        }*/
        canvas.drawPhysics(sensorShapeTop,Color.RED,getX(),getY(),getAngle(),drawScale.x,drawScale.y);
    }

    public boolean wingsActive() {
        return (animationState == AvatarState.CROUCHING || animationState == AvatarState.FALLING);
    }
}