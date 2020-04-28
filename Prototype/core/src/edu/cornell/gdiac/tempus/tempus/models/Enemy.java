package edu.cornell.gdiac.tempus.tempus.models;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;
import com.badlogic.gdx.utils.JsonValue;
import edu.cornell.gdiac.tempus.GameCanvas;
import edu.cornell.gdiac.tempus.obstacle.CapsuleObstacle;
import edu.cornell.gdiac.util.FilmStrip;
import edu.cornell.gdiac.util.JsonAssetManager;

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
        ATTACKING
    };

    public enum EnemyType {
        /** Moves on platform */
        WALK,
        /** Teleports to other platforms */
        TELEPORT,
        /** Still and shoots quickly */
        GUN,
        /** Flies toward target */
        FLY
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
    private static final float FORCE = 125.0f;
    /** The amount to slow the enemy down */
    private static final float ENEMY_DAMPING = 10.0f;
    /** The maximum enemy speed */
    private static final float ENEMY_MAXSPEED = 1.0f;

    /** Height of the sensor attached to the player's feet */
    private static final float SENSOR_HEIGHT = 0.05f;
    private static final float DENSITY = 1.0F;
    private static final String GROUND_SENSOR_NAME = "EnemyGroundSensor";
    private static final String CENTER_SENSOR_NAME = "EnemyCenterSensor";

    // ANIMATION FIELDS
    /** Texture filmstrip for avatar dashing */
    private FilmStrip neutralTexture;
    /** Texture filmstrip for avatar falling */
    private FilmStrip attackingTexture;

    /** The texture filmstrip for the current animation */
    private FilmStrip currentStrip;
    /** The texture filmstrip for the neutral animation */
    private FilmStrip neutralStrip;
    /** The texture filmstrip for the attacking animation */
    private FilmStrip attackingStrip;

    /** The frame rate for the animation */
    private static final float FRAME_RATE = 10;
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
    private Float flyAngle;

    private boolean isTurret;

    /** Texture asset for present enemy */
    private TextureRegion enemyPresentTexture;
    /** Texture asset for past enemy */
    private TextureRegion enemyPastTexture;

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

        // TODO 1: set filmstrips
        // neutralTexture =
        // JsonAssetManager.getInstance().getEntry(json.get("texture").asString(),
        // FilmStrip.class);
        // setFilmStrip(EnemyState.NEUTRAL, neutralTexture);

        // example Filmstrip extraction
        String entitytype = json.get("entitytype").asString();
        FilmStrip test = JsonAssetManager.getInstance().getEntry("turret_shooting" + "_" + entitytype, FilmStrip.class);
        setTexture(texture);

        neutralTexture = JsonAssetManager.getInstance().getEntry("turret_shooting" + "_" + entitytype, FilmStrip.class);
        setFilmStrip(EnemyState.NEUTRAL, neutralTexture);

        setPosition(pos[0], pos[1]);
        setDimension(texture.getRegionWidth() * shrink[0], texture.getRegionHeight() * shrink[1]);
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
        System.out.println("TEXTURE: " + json.get("texture").asString() + "_type" + (json.get("aitype").asInt()));
        TextureRegion texture = JsonAssetManager.getInstance()
                .getEntry(json.get("texture").asString() + "_type" + (json.get("aitype").asInt()), TextureRegion.class);

        String entitytype = json.get("entitytype").asString();

        // example filmstrip extraction
        FilmStrip test = JsonAssetManager.getInstance().getEntry(("enemywalking" + "_" + entitytype), FilmStrip.class);
        test = JsonAssetManager.getInstance().getEntry(("enemyshooting" + "_" + entitytype), FilmStrip.class);
        test = JsonAssetManager.getInstance().getEntry(("enemyflying" + "_" + entitytype), FilmStrip.class);
        test = JsonAssetManager.getInstance().getEntry(("enemyteleporting" + "_" + entitytype), FilmStrip.class);

        setTexture(texture);

        setPosition(pos[0], pos[1]);
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
        switch (json.get("aitype").asInt()) {
        case 1:
            ai = EnemyType.WALK;
            neutralTexture = JsonAssetManager.getInstance().getEntry(("enemywalking" + "_" + entitytype), FilmStrip.class);
            break;

        case 2:
            ai = EnemyType.TELEPORT;
            neutralTexture = JsonAssetManager.getInstance().getEntry(("enemyteleporting" + "_" + entitytype), FilmStrip.class);
            break;

        case 3:
            ai = EnemyType.GUN;
            neutralTexture = JsonAssetManager.getInstance().getEntry(("enemyshooting" + "_" + entitytype), FilmStrip.class);
//            System.out.println("type: " + entitytype);
            break;

        case 4:
            ai = EnemyType.FLY;
            neutralTexture = JsonAssetManager.getInstance().getEntry(("enemyflying" + "_" + entitytype), FilmStrip.class);
            break;
        }
        System.out.println("type: " + entitytype);
        setFilmStrip(EnemyState.NEUTRAL, neutralTexture);
        setFilmStrip(EnemyState.ATTACKING, neutralTexture); //TODO : CHANGE LATER

        if (ai.equals(EnemyType.WALK)) {
            sight = new LineOfSight(this);
            setName("enemy");
            isFiring = false;
            setActive(false);
        } else if (ai.equals(EnemyType.TELEPORT)) {
            sight = new TeleportLineOfSight(this);
            teleportTo = null;
            setName("teleport enemy");
            isFiring = true;
        } else if (ai.equals(EnemyType.GUN)) {
            sight = new LineOfSight(this);
            setName("gun enemy");
            isFiring = false;
        } else {
            sight = new LineOfSight(this);
            setName("fly enemy");
            isFiring = false;
            setGravityScale(0);
        }
    }

    public void setFlyAngle(Float angle) {
        flyAngle = angle;
    }

    public Float getFlyAngle() {
        return flyAngle;
    }

    public Fixture getSensorFixtureCenter() {
        return sensorFixtureCenter;
    }

    public void setFlyingVelocity(Vector2 vel) {
        this.flyingVelocity = vel.scl(FORCE);
    }

    public Vector2 getFlyingVelocity() {
        return flyingVelocity;
    }

    public void setCurrPlatform(Platform currPlatform) {
        this.currPlatform = currPlatform;
    }

    public Platform getCurrPlatform() {
        return currPlatform;
    }

    public void setTeleportTo(Platform platform) {
        teleportTo = platform;
    }

    public Platform getTeleportTo() {
        return teleportTo;
    }

    public Vector2 getProjVel() {
        return projVel;
    }

    public void setProjVel(Vector2 projVel) {
        this.projVel = projVel;
    }

    public RayCastCallback getSight() {
        return sight;
    }

    public void setLimiter(float limiter) {
        this.limiter = limiter;
    }

    public EnemyType getAi() {
        return ai;
    }

    public void setPlatformFixture(Fixture f) {
        platformFixture = f;
    }

    public Fixture getPlatformFixture() {
        return platformFixture;
    }

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

    public boolean getShiftedFiring() {
        return shiftedActive;
    }

    public void setShiftedFiring(boolean a) {
        shiftedActive = a;
    }

    /**
     * Returns whether or not enemy can fire.
     *
     * @return whether or not enemy can fire.
     */
    public boolean canFire() {
        if (isTurret || isFiring) {
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

    public String getGroundSensorName() {
        return GROUND_SENSOR_NAME;
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
            sensorFixtureGround.setUserData(GROUND_SENSOR_NAME);
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
        default:
            assert false : "Invalid EnemyState enumeration";
        }
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
        default:
            assert false : "Invalid EnemyState enumeration";
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
    public void draw(GameCanvas canvas) {
        // TODO 2: uncomment this after [TODO 1] has been done
        // Draw enemy filmstrip
         if (currentStrip != null) {
         canvas.draw(currentStrip, Color.WHITE, origin.x, origin.y,
         getX() * drawScale.x, getY() * drawScale.y, getAngle(),
         0.024f * drawScale.x, 0.0225f * drawScale.y);
         }

        // Old draw texture method
//        if (texture != null) {
//            canvas.draw(texture, Color.WHITE, origin.x, origin.y, getX() * drawScale.x, getY() * drawScale.y,
//                    getAngle(), 0.024f * drawScale.x, 0.0225f * drawScale.y);
//        }
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

        if (fixture.getBody().getUserData() instanceof Avatar) {
            enemy.setIsFiring(true);
            enemy.setShiftedFiring(true);
            if (enemy.getAi() == Enemy.EnemyType.WALK) {
                enemy.setMovement(0);
            }
        } else if (fixture.getBody().getUserData() instanceof Projectile || fixture.getBody().getUserData() == enemy) {
            return 1;
        } else {
            enemy.setIsFiring(false);
            enemy.setShiftedFiring(false);
            if (enemy.getAi() == Enemy.EnemyType.WALK && enemy.getMovement() == 0) {
                enemy.setMovement(enemy.getNextDirection());
            }
        }

        return fraction;
    }
}

class TeleportLineOfSight implements RayCastCallback {

    private Enemy enemy;
    private Vector2 point;
    private Vector2 normal;

    public TeleportLineOfSight(Enemy enemy) {
        this.enemy = enemy;
    }

    @Override
    public float reportRayFixture(Fixture fixture, Vector2 point, Vector2 normal, float fraction) {
        this.point = point;
        this.normal = normal;

        if (fixture.getBody().getUserData() instanceof Avatar) {
            enemy.setIsFiring(true);
            enemy.setShiftedFiring(true);
        } else if (fixture.getBody().getUserData() instanceof Projectile || fixture.getBody().getUserData() == enemy) {
            return 1;
        } else {
            enemy.setTeleportTo(null);
            enemy.setIsFiring(false);
            enemy.setShiftedFiring(false);
        }

        return fraction;
    }
}