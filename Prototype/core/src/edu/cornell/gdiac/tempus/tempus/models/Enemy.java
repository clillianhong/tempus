package edu.cornell.gdiac.tempus.tempus.models;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.JsonValue;
import edu.cornell.gdiac.tempus.GameCanvas;
import edu.cornell.gdiac.tempus.obstacle.CapsuleObstacle;
import edu.cornell.gdiac.util.FilmStrip;
import edu.cornell.gdiac.util.JsonAssetManager;
import edu.cornell.gdiac.util.SoundController;

import static edu.cornell.gdiac.tempus.tempus.models.EntityType.PAST;
import static edu.cornell.gdiac.tempus.tempus.models.EntityType.PRESENT;

public class Enemy extends CapsuleObstacle {
    /**
     * Enumeration to identify the state of the enemy
     */
    public enum EnemyState {
        /** When enemy is chilling */
        NEUTRAL,
        /** When enemy is attacking */
        ATTACKING,
        /** Special state after teleporting */
        TPEND
    };

    public enum EnemyType {
        /** Moves on platform */
        WALK,
        /** Teleports to other platforms */
        TELEPORT,
        /** Still and shoots quickly */
        GUN,
        /** Flies toward target */
        FLY,
        /** Shoots in a spiral pattern */
//        SPIRAL, //TODO: implement correctly, this is currently unused
    }

    // This is to fit the image to a tighter hitbox
    /** The amount to shrink the body fixture (vertically) relative to the image */
    private static final float VSHRINK = 0.0225f * 0.95f;
    /**
     * The amount to shrink the body fixture (horizontally) relative to the image
     */
    private static final float HSHRINK = 0.024f * 0.7f;
    /** The density of the enemy */
    private static final float ENEMY_MASS = 1.0f;
    /** The factor to multiply by the input */
    private static final float FORCE = 2500.0f;
    /** The factor for flying force */
    private static final float FLY_FORCE = 15.0f;
    /** The amount to slow the enemy down */
    private static final float ENEMY_DAMPING = 10.0f;
    /** The maximum enemy speed */
    private static final float ENEMY_MAXSPEED = 1.0f;

    /** Height of the sensor attached to the player's feet */
    private static final float SENSOR_HEIGHT = 0.05f;
    private static final float DENSITY = 1.0F;
    private static final String ENEMY_CENTER_SENSOR = "EnemyCenterSensor";
    private static final String ENEMY_GROUND_SENSOR = "EnemyGroundSensor";

    private static final float INDICATOR_SCALE = 0.01f;

    // ANIMATION FIELDS
    /** Texture filmstrip for enemy chilling */
    private FilmStrip neutralTexture;
    /** Texture filmstrip for enemy attacking */
    private FilmStrip attackingTexture;
    /** Texture filmstrip for enemy after tp */
    private FilmStrip tpEndTexture;

    /** The current animation state */
    public EnemyState animationState = EnemyState.NEUTRAL;
    /** The texture filmstrip for the current animation */
    private FilmStrip currentStrip;
    /** The texture filmstrip for the neutral animation */
    private FilmStrip neutralStrip;
    /** The texture filmstrip for the attacking animation */
    private FilmStrip attackingStrip;
    /** The texture filmstrip for the tp end animation */
    private FilmStrip tpEndStrip;

    /** Minimize the size of the texture by the factor */
    private float minimizeScale = 1;

    /** The frame rate for the animation. How many seconds should elapse
     * to move to the next frame. Lower values give a faster playback. */
    private static float FRAME_RATE = 10;
    /** The frame cooldown for the animation */
    private static float frame_cooldown = FRAME_RATE;

    /** Sensor to determine enemy position on platform */
    private Fixture sensorFixtureGround;
    private PolygonShape sensorShapeGround;
    /** Fixture the enemy stands on */
    private Fixture platformFixture;
    /** Surrounding censor for flying pathfinding */
    private Fixture sensorFixtureCenter;
    private CircleShape sensorShapeCenter;

    /** Line of sight to the avatar */
    private RayCastCallback sight;
    /** To calculate line of sight */
    private Avatar target;
    /** Whether this update has checked line of sight */
    private boolean checkSight;

    /** Cache for internal force calculations */
    private Vector2 forceCache = new Vector2();

    /** The enemy's current movement */
    private float movement;
    /** The enemy's next movement */
    private int nextDirection;

    /** The number of frames until we can fire again */
    private float framesTillFire;
    /** How long the turret must wait until it can fire again */
    private int cooldown; // in ticks
    /** The number of frames until the turret can fire again */
    private boolean isFiring;
    /** Saves whether the enemy is active or not when shifting */
    private boolean shiftedActive;
    /** The velocity of the projectile that this turret fires */
    private Vector2 projVel;
    private float limiter;

    /** Type of enemy */
    private EntityType type;
    /** AI of enemy */
    private EnemyType ai;
    /** Platform the enemy teleports to */
    private Platform teleportTo;
    /** Platform currently on */
    private Platform currPlatform;
    /** Velocity of flying */
    private Vector2 flyingVelocity;
    /** Flying angle */
    private Vector2 flyNormal;
    /** Whether the enemy is close to a platform */
    private boolean nearPlatform;
    /** Platform the enemy is near */
    private Contact adjPlatform;
    /** Start position of enemy */
    private Vector2 startPosition;

    /** Whether the enemy is a turret or not */
    private boolean isTurret;
    /** Whether the enemy is waiting to fire or not */
    private boolean waitToFire;

    /** Direction the enemy faces */
    private float faceDirection;

    /** Whether the enemy has died */
    private boolean dead;
    /** Frames for enemy removal */
    private float removalFrames;

    /** Texture asset for present enemy */
    private TextureRegion enemyPresentTexture;
    /** Texture asset for past enemy */
    private TextureRegion enemyPastTexture;

    /** Texture for a passive enemy */
    private TextureRegion passiveEnemyIndicator;
    /** Texture for an active enemy */
    private TextureRegion activeEnemyIndicator;

    private Boolean queueRespawn;
    private Boolean shiftQueued;

    /**
     * Creates a turret with the provided json value
     * 
     * @param json The params for the turret
     */
    public Enemy(JsonValue json) {
        super(0, 0, 0.5f, 1.0f);
        float[] pos = json.get("pos").asFloatArray();
        float[] shrink = json.get("shrink").asFloatArray();
        TextureRegion texture = JsonAssetManager.getInstance().getEntry(json.get("texture").asString(),
                TextureRegion.class);

        // example Filmstrip extraction
        String entitytype = json.get("entitytype").asString();
        FilmStrip test = JsonAssetManager.getInstance().getEntry("turret_shooting" + "_" + entitytype, FilmStrip.class);
        setTexture(texture);

        neutralTexture = JsonAssetManager.getInstance().getEntry("turret_shooting" + "_" + entitytype, FilmStrip.class);
        setFilmStrip(EnemyState.NEUTRAL, neutralTexture);

        // Minimize turret textures
        if (entitytype.equals("present")) {
            minimizeScale = 0.4f;
//            FRAME_RATE = 10;
            FRAME_RATE = Gdx.graphics.getFramesPerSecond() < 40 ? 5 : 10;
        } else if (entitytype.equals("past")) {
            minimizeScale = 0.4f;
//            FRAME_RATE = 10;
            FRAME_RATE = Gdx.graphics.getFramesPerSecond() < 40 ? 5 : 10;
        }

        setPosition(pos[0], pos[1]);
        setDimension(texture.getRegionWidth() * shrink[0] * minimizeScale, texture.getRegionHeight() * shrink[1] * minimizeScale);
        setType(entitytype.equals("present") ? EntityType.PRESENT : PAST);
        setSpace(getType() == PRESENT ? 1 : 2);
        setBodyType(json.get("bodytype").asString().equals("static") ? BodyDef.BodyType.StaticBody
                : BodyDef.BodyType.DynamicBody);
        setDensity(json.get("density").asFloat());
        isTurret = true;
        this.cooldown = json.get("cooldown").asInt();
        float[] dir = json.get("direction").asFloatArray();
        this.projVel = new Vector2(dir[0], dir[1]);
        isFiring = true;
        shiftedActive = isFiring;
        framesTillFire = this.cooldown;
        limiter = 4;
        shiftQueued = false;
        queueRespawn = false;
        setName("turret");
    }

    /**
     * Creates a moving enemy
     *
     * @param target the target the enemy is aiming for
     * @param json   the json storing enemy properties
     */
    public Enemy(final Avatar target, JsonValue json) {
        super(0, 0, 0.5f, 1.0f);
        float[] pos = json.get("pos").asFloatArray();
        float[] shrink = json.get("shrink").asFloatArray();
//        System.out.println("TEXTURE: " + json.get("texture").asString() + "_type" + (json.get("aitype").asInt()));
        TextureRegion texture = JsonAssetManager.getInstance()
                .getEntry(json.get("texture").asString() + "_type" + (json.get("aitype").asInt()), TextureRegion.class);

        String entitytype = json.get("entitytype").asString();

        setTexture(texture);

        setPosition(pos[0], pos[1]);
        startPosition = getPosition();
        setDimension(texture.getRegionWidth() * shrink[0], texture.getRegionHeight() * shrink[1]);
        setType(json.get("entitytype").asString().equals("present") ? EntityType.PRESENT : PAST);
        setBodyType(json.get("bodytype").asString().equals("static") ? BodyDef.BodyType.StaticBody
                : BodyDef.BodyType.DynamicBody);
        setSpace(getType() == PRESENT ? 1 : 2);
        setDensity(json.get("density").asFloat());
        setMass(ENEMY_MASS);
        this.target = target;
        this.cooldown = json.get("cooldown").asInt();
        // projVel = new Vector2(0,0).sub(getPosition().sub(target.getPosition()));
        shiftedActive = isFiring;
        framesTillFire = this.cooldown;
        movement = 0;
        nextDirection = 0;
        setFixedRotation(true);
        limiter = 4;
        isTurret = false;
        faceDirection = 1f;
        removalFrames = 60;
        shiftQueued = false;
        queueRespawn = false;
//        switch (type) {
//            case PAST:
//                passiveEnemyIndicator = JsonAssetManager.getInstance().getEntry("enemy_passive_red", TextureRegion.class);
//                activeEnemyIndicator = JsonAssetManager.getInstance().getEntry("enemy_active_red", TextureRegion.class);
//                break;
//
//            case PRESENT:
//                passiveEnemyIndicator = JsonAssetManager.getInstance().getEntry("enemy_passive_blue", TextureRegion.class);
//                activeEnemyIndicator = JsonAssetManager.getInstance().getEntry("enemy_active_blue", TextureRegion.class);
//                break;
//
//        }
        passiveEnemyIndicator = JsonAssetManager.getInstance().getEntry("enemy_passive_yellow", TextureRegion.class);
        activeEnemyIndicator = JsonAssetManager.getInstance().getEntry("enemy_active_yellow", TextureRegion.class);
        switch (json.get("aitype").asInt()) {
        case 1:
            ai = EnemyType.WALK;
            if (entitytype.equals("past")) {
                FRAME_RATE = Gdx.graphics.getFramesPerSecond() < 40 ? 7.5f : 15;
            }
            neutralTexture = JsonAssetManager.getInstance().getEntry(("enemywalking" + "_" + entitytype), FilmStrip.class);
            attackingTexture = neutralTexture;
            setDensity(20);
            sight = new LineOfSight(this);
            setName("enemy");
            isFiring = false;
            setActive(false);
            break;

        case 2:
            ai = EnemyType.TELEPORT;
            FRAME_RATE =Gdx.graphics.getFramesPerSecond() < 40 ? 7.5f : 15;
            neutralTexture = JsonAssetManager.getInstance().getEntry(("enemyteleporting" + "_" + entitytype), FilmStrip.class);
            attackingTexture = JsonAssetManager.getInstance().getEntry(("enemyteleporting_activate" + "_" + entitytype), FilmStrip.class);
            tpEndTexture = JsonAssetManager.getInstance().getEntry(("enemyteleporting_deactivate" + "_" + entitytype), FilmStrip.class);
            setFilmStrip(EnemyState.TPEND, tpEndTexture);
            minimizeScale = 0.35f;
            waitToFire = true;
            sight = new LineOfSight(this);
            teleportTo = null;
            setName("teleport enemy");
            isFiring = true;
            break;

        case 3:
            ai = EnemyType.GUN;
            neutralTexture = JsonAssetManager.getInstance().getEntry(("enemyshooting" + "_" + entitytype), FilmStrip.class);
            attackingTexture = neutralTexture;
            sight = new LineOfSight(this);
            setName("gun enemy");
            isFiring = false;
            break;

        case 4:
            ai = EnemyType.FLY;
            if (entitytype.equals("present")) {
                FRAME_RATE = Gdx.graphics.getFramesPerSecond() < 40 ? 5 : 10 ;
                minimizeScale = 0.5f;
            } else if (entitytype.equals("past")) {
                FRAME_RATE = Gdx.graphics.getFramesPerSecond() < 40 ? 3.75f : 7.5f;
                minimizeScale = 0.4f;
                setDimension(2.5f, 1f);
                setMass(ENEMY_MASS/2);
                setOrientation(Orientation.HORIZONTAL);
            }
            neutralTexture = JsonAssetManager.getInstance().getEntry(("enemyflying" + "_" + entitytype), FilmStrip.class);
            attackingTexture = neutralTexture;
            sight = new LineOfSight(this);
            setName("fly enemy");
            isFiring = false;
            setGravityScale(0);
            flyingVelocity = new Vector2(0,0);
            break;
        }
        setFilmStrip(EnemyState.NEUTRAL, neutralTexture);
        setFilmStrip(EnemyState.ATTACKING, attackingTexture);
        setFilmStrip(EnemyState.TPEND, neutralTexture);
    }

    public Vector2 getStartPosition() {
        return startPosition;
    }

    public Boolean getResawnQueued(){
        return queueRespawn;
    }

    public void setRespawnQueued(boolean s){
        queueRespawn = s;
    }

    public Boolean getShiftQueued(){
        return shiftQueued;
    }

    public void setShiftQueued(boolean s){
        shiftQueued = s;
    }

    /**
     *
     *
     * Set whether the enemy is waiting to fire
     *
     * @param b boolean for if enemy is waiting to fire
     */
    public void setWaitToFire(boolean b){
        waitToFire = b;
    }

    /**
     * Return whether the enemy is waiting to fire
     *
     * @return whether enemy is waiting to fire
     */
    public boolean getWaitToFire(){
        return waitToFire;
    }

    /**
     * Return an array of the enemy's fixtures
     *
     * @return array of enemy's fixtures
     */
    public Array<Fixture> getFixtures() {
        return getBody().getFixtureList();
    }

//    public Fixture getSensorFixtureCenter() {
//        return sensorFixtureCenter;
//    }

    /**
     * Set whether the enemy controller has checked the line of sight
     *
     * @param checkSight whether line of sight has been checked
     */
    public void setCheckSight(boolean checkSight) {
        this.checkSight = checkSight;
    }

    /**
     * Return whether the enemy controller has checked the line of sight
     *
     * @return whether line of sight has been checked
     */
    public boolean getCheckSight() {
        return checkSight;
    }

    /**
     * Returns the frames for the enemy removal
     *
     * @return frames for enemy removal
     */
    public float getRemovalFrames() {
        return removalFrames;
    }

    /**
     * Decreases the frames into the removal
     */
    public void decRemovalFrames() {
        removalFrames -= 1;
    }

    /**
     * Sets the enemy as dead
     */
    public void setDead() {
        setMovement(0);
        setNextDirection(0);
        setLinearVelocity(new Vector2(0,0));
        dead = true;
        Sound deathSound = JsonAssetManager.getInstance().getEntry("enemy_death", Sound.class);
        deathSound.play();
    }



    /**
     * Returns whether the enemy is dead or not
     *
     * @return whether enemy is dead
     */
    public boolean isDead() {
        return dead;
    }

    /**
     * Set the direction the enemy faces
     *
     * @param faceDirection float of direction enemy faces
     */
    public void setFaceDirection(float faceDirection) {
        this.faceDirection = faceDirection;
    }

    /**
     * Get the direction the enemy faces
     *
     * @return direction the enemy faces
     */
    public float getFaceDirection() {
        return faceDirection;
    }

    /**
     * Sets the velocity of the flying enemy
     *
     * @param vel vector the the flying enemy velocity
     */
    public void setFlyingVelocity(Vector2 vel) {
        if (getSpace() == 1) {
            this.flyingVelocity = vel.scl(FLY_FORCE);
        } else {
            this.flyingVelocity = vel.scl(FLY_FORCE * 2);
        }
    }

    /**
     * Returns the velocity of the flying enemy
     *
     * @return velocity of flying enemy
     */
    public Vector2 getFlyingVelocity() {
        return flyingVelocity;
    }

    /**
     * Sets the platform the enemy is currently on
     *
     * @param currPlatform platform the enemy is currently on
     */
    public void setCurrPlatform(Platform currPlatform) {
        this.currPlatform = currPlatform;
    }

    /**
     * Returns the platform the enemy is currently on
     *
     * @return platform the enemy is currently on
     */
    public Platform getCurrPlatform() {
        return currPlatform;
    }

    /**
     * Sets the platform the enemy plans to teleport to
     *
     * @param platform platform the enemy plans to teleport to
     */
    public void setTeleportTo(Platform platform) {
        teleportTo = platform;
    }

    /**
     * Returns the platform the enemy is planning on teleporting to
     *
     * @return platform the enemy plans to teleport to
     */
    public Platform getTeleportTo() {
        return teleportTo;
    }

    /**
     * Returns the velocity of the projectile
     *
     * @return velocity of projectile
     */
    public Vector2 getProjVel() {
        return projVel;
    }

    /**
     * Sets the velocity of the projectile
     *
     * @param projVel vector of the velocity
     */
    public void setProjVel(Vector2 projVel) {
        this.projVel = projVel;
    }

    /**
     * Changes the angle of the projectile
     *
     * @param angle angle to move in radians
     */
    public void changeProjAngle(float angle) {
        double x = projVel.x * Math.cos(angle) - projVel.y * Math.sin(angle);
        double y = projVel.x * Math.sin(angle) + projVel.y * Math.cos(angle);
        this.projVel = new Vector2((float)x, (float)y);
    }

    /**
     * Returns the ray cast used for line of sight
     *
     * @return ray cast used for line of sight
     */
    public RayCastCallback getSight() {
        return sight;
    }


    public void setLimiter(float limiter) {
        this.limiter = limiter;
    }

    /**
     * Returns the type of ai the enemy uses
     *
     * @return ai the enemy uses
     */
    public EnemyType getAi() {
        return ai;
    }

    /**
     * Sets the fixture the enemy stands on
     *
     * @param f fixture the enemy stands on
     */
    public void setPlatformFixture(Fixture f) {
        //System.out.println("platform: " + f);
        platformFixture = f;
    }

    /**
     * Returns the fixture that the enemy stands on
     *
     * @return fixture enemy stands on
     */
    public Fixture getPlatformFixture() {
        return platformFixture;
    }

    /**
     * Returns whether the enemy is a turret or not
     *
     * @return boolean whether enemy is a turret
     */
    public boolean isTurret() {
        return isTurret;
    }

    /**
     * Returns the type of enemy.
     *
     * @return type of enemy.
     */
    public EntityType getType() {
        return type;
    }

    /**
     * Sets the entity type of enemy
     * 
     * @param t the type of enemy
     */
    public void setType(EntityType t) {
        type = t;
    }

    /**
     * Sets whether the is active to fire bullets
     *
     * @param a boolean of whether the enemy is active
     */
    public void setIsFiring(boolean a) {
        isFiring = a;
    }

    /**
     * Returns whether the enemy is active to fire projectiles
     *
     * @return whether enemy is active to fire projectiles
     */
    public boolean isFiring() {
        return isFiring;
    }

    /**
     * Remembers through shift whether an enemy is firing
     *
     * @return boolean whether the enemy is meant to be firing
     */
    public boolean getShiftedFiring() {
        return shiftedActive;
    }

    /**
     * Sets whether the enemy is firing to remember after shift
     *
     * @param a boolean whether the enemy is firing
     */
    public void setShiftedFiring(boolean a) {
        shiftedActive = a;
    }

    /**
     * Returns whether or not enemy can fire.
     *
     * @return whether or not enemy can fire.
     */
    public boolean canFire() {
        if ((isTurret || isFiring) && !dead) {
            return framesTillFire <= 0;
        } else {
            return false;
            // return framesTillFire <= 0 && isActive;
        }
    }

    public float getFramesTillFire() {
        return framesTillFire;
    }

    /**
     * Reset or cool down the enemy weapon.
     *
     * If flag is true, the enemy will cool down by one animation frame. Otherwise
     * it will reset to its maximum cooldown.
     *
     * @param flag whether to cooldown or reset
     */
    public void coolDown(boolean flag) {
        if (flag && framesTillFire > 0) {
            framesTillFire = framesTillFire - limiter;
        } else if (!flag) {
            framesTillFire = cooldown;
        }
    }

    public void slowCoolDown(boolean flag) {
        if (flag) {
            limiter = 0.5f;
        } else {
            limiter = 4;
        }
    }

    /**
     * Returns the amount of sideways movement.
     *
     * -1 = left, 1 = right, 0 = still
     *
     * @return the amount of sideways movement.
     */
    public float getMovement() {
        return movement * FORCE;
    }

    /**
     * Sets the amount of sideways movement.
     *
     * -1 = left, 1 = right, 0 = still
     *
     * @param movement is the direction to set
     */
    public void setMovement(int movement) {
        this.movement = movement;
    }

    /**
     * Returns how hard the brakes are applied to get the enemy to stop moving
     *
     * @return how hard the brakes are applied to get the enemy to stop moving
     */
    public float getDamping() {
        return ENEMY_DAMPING;
    }

    /**
     * Returns the next direction the enemy moves
     *
     * @return the next direction the enemy moves
     */
    public int getNextDirection() {
        return nextDirection;
    }

    public void setNextDirection(int d) {
        nextDirection = d;
    }

    /**
     * Returns the upper limit on dude left-right movement.
     *
     * This does NOT apply to vertical movement.
     *
     * @return the upper limit on dude left-right movement.
     */
    public float getMaxSpeed() {
        return ENEMY_MAXSPEED;
    }

    public String getEnemyCenterSensorName() {
        return ENEMY_CENTER_SENSOR;
    }

    public boolean activatePhysics(World world) {
        if (!super.activatePhysics(world)) {
            return false;
        }

        if (getAi() == EnemyType.WALK) {
            Vector2 sensorGround = new Vector2(0, -getHeight() / 2);
            FixtureDef sensorDef = new FixtureDef();
            sensorDef.density = DENSITY;
            sensorDef.isSensor = true;
            sensorShapeGround = new PolygonShape();
            sensorShapeGround.setAsBox(getWidth() / 4, SENSOR_HEIGHT, sensorGround, 0f);
            sensorDef.shape = sensorShapeGround;

            sensorFixtureGround = body.createFixture(sensorDef);
            sensorFixtureGround.setUserData(ENEMY_GROUND_SENSOR);
        }
        if(getAi() == EnemyType.FLY){

            Vector2 sensorSky = new Vector2(0, 0);
//            sensorShapeGround = new PolygonShape();
//            sensorShapeGround.setAsBox(getWidth() / 4, SENSOR_HEIGHT, sensorSky, 0f);
//
            FixtureDef sensorDef = new FixtureDef();
            sensorDef.density = 0;
            sensorShapeCenter = new CircleShape();
            if (getSpace() == 1) {
                sensorShapeCenter.setRadius(1.25f * getWidth());
            } else {
                sensorShapeCenter.setRadius(0.4f * getWidth());
            }
            sensorShapeCenter.setPosition(sensorSky);
            sensorDef.shape = sensorShapeCenter;
            sensorDef.isSensor = true;
            sensorDef.filter.groupIndex = -1;
//
            sensorFixtureCenter = body.createFixture(sensorDef);
            sensorFixtureCenter.setUserData(ENEMY_CENTER_SENSOR);
        }

        return true;
    }

    /**
     * Sets the animation node for the given state
     *
     * @param state enumeration to identify the state
     * @param strip the animation for the given state
     */
    public void setFilmStrip(EnemyState state, FilmStrip strip) {
        switch (state) {
        case NEUTRAL:
            neutralStrip = strip;
            break;
        case ATTACKING:
            attackingStrip = strip;
            break;
        case TPEND:
            tpEndStrip = strip;
            break;
        default:
            assert false : "Invalid EnemyState enumeration";
        }
    }

    public void setAnimationState(EnemyState s){
        this.animationState = s;
    }

    /**
     * Animates the given state.
     *
     * @param state      The reference to the rocket burner
     * @param shouldLoop Whether the animation should loop
     */
    public void animate(EnemyState state, boolean shouldLoop) {
        switch (state) {
        case NEUTRAL:
            currentStrip = neutralStrip;
            break;
        case ATTACKING:
            currentStrip = attackingStrip;
            break;
        case TPEND:
            currentStrip = tpEndStrip;
            break;
        default:
            assert false : "Invalid EnemyState enumeration";
        }

        // when beginning a new state, set frame to first frame
        if (animationState != state) {
            currentStrip.setFrame(0);
        }

        // Adjust animation speed
        if (frame_cooldown > 0) {
            frame_cooldown--;
            return;
        } else
            frame_cooldown = FRAME_RATE;

        // Manage current frame to draw
//        System.out.println("curr frame: " + currentStrip.getFrame());
//        System.out.println("size: " + currentStrip.getSize());

        if (currentStrip.getFrame() < currentStrip.getSize() - 1) {
            currentStrip.setFrame(currentStrip.getFrame() + 1);
        } else {
            if (shouldLoop)
                currentStrip.setFrame(0); // loop animation
            else
                return; // play animation once
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
        if (getAi() == EnemyType.WALK) {
            canvas.drawPhysics(sensorShapeGround, Color.RED, getX(), getY(), getAngle(), drawScale.x, drawScale.y);
            // canvas.drawPhysics(sensorShapeLeft, Color.RED, getX(), getY(), getAngle(),
            // drawScale.x, drawScale.y);
            // canvas.drawPhysics(sensorShapeRight, Color.RED, getX(), getY(), getAngle(),
            // drawScale.x, drawScale.y);
        } else if (getAi() == EnemyType.FLY) {
            canvas.drawPhysics(sensorShapeCenter, Color.RED, getX(), getY(), drawScale.x, drawScale.y);
        }
    }

    /**
     * Draws the physics object.
     *
     * @param canvas Drawing context
     */
    public void draw(GameCanvas canvas, boolean playerVisible) {

        if (!isTurret) {
            float dist = getHeight() * drawScale.y;
            switch (ai) {
                case FLY:
                    if (space == 1) {
                        dist = getHeight() * 1.25f * drawScale.y;
                    } else {
                        dist = getHeight() * 2f * drawScale.y;
                    }
                    break;

                case TELEPORT:
                    dist = getWidth() * drawScale.x;
                    break;
            }
            float offset = type.equals(PAST) ? -3f : 0;
            float fullDist = target.getPosition().dst(getPosition());
            float ratio = dist / fullDist;
            float newX = ((target.getX() - getX()) * ratio) + (getX() * drawScale.x);
            float newY = ((target.getY() - getY()) * ratio) + (getY() * drawScale.y);
            Vector2 drawLoc = new Vector2(newX, newY);
            float rotation = getPosition().sub(target.getPosition()).nor().angle() / 57 - 1.6f;
            TextureRegion indicator = activeEnemyIndicator;

            if ((getAi() == EnemyType.WALK && !isFiring) || !playerVisible) {
                rotation = getAngle();
                indicator = passiveEnemyIndicator;
                drawLoc = new Vector2(getX() * drawScale.x, getY() * drawScale.y);
                drawLoc.add(offset, dist);
            }

            canvas.draw(indicator, Color.WHITE, origin.x, origin.y, drawLoc.x, drawLoc.y, rotation,
                    drawScale.x * INDICATOR_SCALE, drawScale.y * INDICATOR_SCALE);
        }

        if(getAi() == EnemyType.FLY) {
            float faceOffset = 0;
            if(getSpace() == 1) {
                faceOffset = 1.5f * getWidth() * faceDirection;
            } else {
                faceOffset = 0.5f * getWidth() * faceDirection;
            }

            canvas.draw(currentStrip, Color.WHITE, origin.x, origin.y,  (getX() - faceOffset) * drawScale.x, (getY()- 0.6f * getHeight()) * drawScale.y,
                    getAngle(), 0.018f * minimizeScale * drawScale.x * faceDirection , 0.0169f * minimizeScale * drawScale.y);
        } else if (getAi() == EnemyType.TELEPORT){
            float faceOffset = 0.7f* getWidth() * faceDirection;

            canvas.draw(currentStrip, Color.WHITE, origin.x, origin.y,  (getX() - faceOffset) * drawScale.x, (getY()- 0.2f * getHeight()) * drawScale.y,
                    getAngle(), 0.024f * minimizeScale * drawScale.x * faceDirection , 0.0225f * minimizeScale * drawScale.y);
        }
        else {
            // Draw enemy filmstrip
            if (currentStrip != null) {
                canvas.draw(currentStrip, Color.WHITE, origin.x, origin.y, getX() * drawScale.x, getY() * drawScale.y,
                        getAngle(), 0.024f * minimizeScale * drawScale.x * faceDirection, 0.0225f * minimizeScale * drawScale.y);
            }
        }

    }

    /**
     * Draws the enemy fading in or out
     *
     * @param canvas Drawing canvas
     * @param frames How many frames into the fade the enemy is
     * @param mul The multiplier for how fast the enemy is fading
     */
    public void drawFade(GameCanvas canvas, float frames, float mul) {
        if(getAi() == EnemyType.FLY){
            float faceOffset = 0;
            if(getSpace() == 1) {
                faceOffset = 1.5f * getWidth() * faceDirection;
            } else {
                faceOffset = 0.5f * getWidth() * faceDirection;
            }

            canvas.draw(currentStrip, new Color(1,1,1, mul * frames), origin.x, origin.y,
                    (getX() - faceOffset) * drawScale.x, (getY()- 0.6f * getHeight()) * drawScale.y, getAngle(),
                    0.018f * minimizeScale * drawScale.x * faceDirection, 0.0169f * minimizeScale * drawScale.y);

        } else if (getAi() == EnemyType.TELEPORT){
            float faceOffset = 0.7f * getWidth() * faceDirection;

            canvas.draw(currentStrip, new Color(1,1,1, mul * frames), origin.x, origin.y,  (getX() - faceOffset) * drawScale.x, (getY()- 0.2f * getHeight()) * drawScale.y,
                    getAngle(), 0.024f * minimizeScale * drawScale.x * faceDirection , 0.0225f * minimizeScale * drawScale.y);
        } else{
            if (currentStrip != null) {
                canvas.draw(currentStrip, new Color(1,1,1, mul * frames), origin.x, origin.y,
                        getX() * drawScale.x, getY() * drawScale.y, getAngle(),
                        0.024f * minimizeScale * drawScale.x * faceDirection, 0.0225f * minimizeScale * drawScale.y);
            }
        }
    }
}

class LineOfSight implements RayCastCallback {

    private Enemy enemy;
    private Vector2 point;
    private Vector2 normal;

    public LineOfSight(Enemy enemy) {
        this.enemy = enemy;
    }

    public float reportRayFixture(Fixture fixture, Vector2 point, Vector2 normal, float fraction) {

        this.point = point;
        this.normal = normal;

        if (fixture.getBody().getUserData() instanceof Projectile || fixture.getBody().getUserData() == enemy ||
                (fixture.getBody().getUserData() instanceof Platform &&
                        ((Platform) fixture.getBody().getUserData()).getSpace() != enemy.getSpace() &&
                        ((Platform) fixture.getBody().getUserData()).getSpace() != 3) ||
                (fixture.getBody().getUserData() instanceof Spikes &&
                        (((Spikes) fixture.getBody().getUserData()).getSpace() != enemy.getSpace() &&
                                ((Spikes) fixture.getBody().getUserData()).getSpace() != 3))) {
            return 1;
        } else if (!(fixture.getBody().getUserData() instanceof Avatar)){
            enemy.setCheckSight(false);
        }

        return fraction;
    }
}