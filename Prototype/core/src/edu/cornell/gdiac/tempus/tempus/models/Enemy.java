package edu.cornell.gdiac.tempus.tempus.models;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.ObjectSet;
import edu.cornell.gdiac.tempus.GameCanvas;
import edu.cornell.gdiac.tempus.obstacle.CapsuleObstacle;
import edu.cornell.gdiac.util.JsonAssetManager;

import static edu.cornell.gdiac.tempus.tempus.models.EntityType.PAST;
import static edu.cornell.gdiac.tempus.tempus.models.EntityType.PRESENT;

public class Enemy extends CapsuleObstacle {

    // This is to fit the image to a tighter hitbox
    /** The amount to shrink the body fixture (vertically) relative to the image */
    private static final float VSHRINK = 0.0225f * 0.95f;
    /** The amount to shrink the body fixture (horizontally) relative to the image */
    private static final float HSHRINK = 0.024f * 0.7f;
    /** The density of the enemy */
    private static final float ENEMY_DENSITY = 1.0f;
    /** The factor to multiply by the input */
    private static final float FORCE = 10.0f;
    /** The amount to slow the enemy down */
    private static final float ENEMY_DAMPING = 10.0f;
    /** The maximum enemy speed */
    private static final float ENEMY_MAXSPEED = 1.0f;

    /** Height of the sensor attached to the player's feet */
    private static final float SENSOR_HEIGHT = 0.05f;
    private static final float DENSITY = 1.0F;
    private static final String LEFT_SENSOR_NAME = "EnemyLeftSensor";
    private static final String RIGHT_SENSOR_NAME = "EnemyRightSensor";
    private static final String SIGHT_SENSOR_NAME = "EnemyLineofSight";

    /** Left sensor to determine enemy position on platform*/
    private Fixture sensorFixtureLeft;
    private PolygonShape sensorShapeLeft;
    private Fixture leftFixture;
    /** Right sensor to determine enemy position on platform*/
    private Fixture sensorFixtureRight;
    private PolygonShape sensorShapeRight;
    private Fixture rightFixture;

    /** Line of sight to the avatar */
    private RayCastCallback sight;
    /** To calculate line of sight*/
    private Avatar target;

    /** Cache for internal force calculations */
    private Vector2 forceCache = new Vector2();

    /** The enemy's current movement */
    private float movement;
    /** The enemy's next movement */
    private int nextDirection;

    /** The number of frames until we can fire again */
    private int framesTillFire;
    /** How long the turret must wait until it can fire again */
    private int cooldown; // in ticks
    /** The number of frames until the turret can fire again */
    private boolean isActive;
    /** Saves whether the enemy is active or not when shifting */
    private boolean shiftedActive;
    /** The velocity of the projectile that this turret fires */
    private Vector2 projVel;
    private int limiter;

    /** Type of enemy */
    private EntityType type;

    private boolean isTurret;

    /** Texture asset for present enemy */
    private TextureRegion enemyPresentTexture;
    /** Texture asset for past enemy */
    private TextureRegion enemyPastTexture;

    // Immobile enemy (turret)
    public Enemy(
            EntityType type, float x, float y, float width, float height,
            TextureRegion texture, int cooldown, Vector2 projVel, Vector2 scale) {
        super(x,y,width*HSHRINK * scale.x,height*VSHRINK * scale.y);

        this.setTexture(texture);
        // set body space so that createBullet sets the
        // correct space for created projectiles
        if (type == PRESENT) this.setSpace(1);
        else if (type == PAST) this.setSpace(2);

        isTurret = true;
        this.type = type;
        this.cooldown = cooldown * 4;
        this.projVel = projVel;
        isActive = true;
        framesTillFire = 0;
        limiter = 4;
    }

    /**
     * Creates a turret with the provided json value
     * @param json The params for the turret
     */
    public Enemy(JsonValue json){
        super(0,0,0.5f,1.0f);
        float [] pos = json.get("pos").asFloatArray();
        float [] shrink = json.get("shrink").asFloatArray();
        TextureRegion texture = JsonAssetManager.getInstance().getEntry(json.get("texture").asString(), TextureRegion.class);
        setTexture(texture);
        setPosition(pos[0],pos[1]);
        setDimension(texture.getRegionWidth()*shrink[0],texture.getRegionHeight()*shrink[1]);
        setType(json.get("entitytype").asString().equals("present")?EntityType.PRESENT: PAST);
        setSpace(getType()==PRESENT?1:2);
        setBodyType(json.get("bodytype").asString().equals("static") ? BodyDef.BodyType.StaticBody : BodyDef.BodyType.DynamicBody);
        setDensity(json.get("density").asFloat());
        isTurret = true;
        this.cooldown = json.get("cooldown").asInt();
        float [] dir = json.get("direction").asFloatArray();
        this.projVel = new Vector2 (dir[0],dir[1]);
        isActive = true;
        shiftedActive = isActive;
        framesTillFire = this.cooldown;
        limiter = 4;
        setName("turret");
    }

    // Moving enemy
    public Enemy(
            EntityType type, float x, float y, float width, float height,
            TextureRegion texture, int cooldown, final Avatar target, Vector2 scale) {
        super(x,y,width*HSHRINK * scale.x,height*VSHRINK * scale.y);

        this.setTexture(texture);
        if (type == PRESENT) this.setSpace(1);
        else if (type == PAST) this.setSpace(2);

        this.type = type;
        this.target = target;
        this.cooldown = cooldown * 4;
        projVel = new Vector2(0,0).sub(getPosition().sub(target.getPosition()));
        isActive = true;
        framesTillFire = 0;
        movement = -1;
        nextDirection = -1;
        setFixedRotation(true);
        sight = new LineOfSight(this);
        limiter = 4;
        isTurret = false;
    }

    /**Creates a moving enemy
     *
     * @param target the target the enemy is aiming for
     * @param json the json storing enemy properties
     */
    public Enemy(final Avatar target, JsonValue json) {
        super(0,0,0.5f,1.0f);
        float [] pos = json.get("pos").asFloatArray();
        float [] shrink = json.get("shrink").asFloatArray();
        TextureRegion texture = JsonAssetManager.getInstance().getEntry(json.get("texture").asString(), TextureRegion.class);
        setTexture(texture);
        setPosition(pos[0],pos[1]);
        setDimension(texture.getRegionWidth()*shrink[0],texture.getRegionHeight()*shrink[1]);
        setType(json.get("entitytype").asString().equals("present")?EntityType.PRESENT: PAST);
        setBodyType(json.get("bodytype").asString().equals("static") ? BodyDef.BodyType.StaticBody : BodyDef.BodyType.DynamicBody);
        setSpace(getType()==PRESENT?1:2);
        setDensity(json.get("density").asFloat());
        this.target = target;
        this.cooldown = json.get("cooldown").asInt();
//        projVel = new Vector2(0,0).sub(getPosition().sub(target.getPosition()));
        isActive = false;
        shiftedActive = isActive;
        framesTillFire = 0;
        movement = -1;
        nextDirection = -1;
        setFixedRotation(true);
        sight = new LineOfSight(this);
        limiter = 4;
        isTurret = false;
        setName("enemy");
    }

    /**
     * Returns the platform fixture that the enemy's left sensor is connected to
     *
     * @return platform fixture the enemy stands on
     */
    public Fixture getLeftFixture() { return leftFixture; }

    /**
     * Sets the platform fixture the enemy's left sensor detects
     *
     * @param f the platform fixture
     */
    public void setLeftFixture(Fixture f) { leftFixture = f; }

    /**
     * Returns the platform fixture that the enemy's right sensor is connected to
     *
     * @return platform fixture the enemy stands on
     */
    public Fixture getRightFixture() { return rightFixture; }

    /**
     * Sets the platform fixture the enemy's right sensor detects
     *
     * @param f the platform fixture
     */
    public void setRightFixture(Fixture f) { rightFixture = f;}

    public boolean isTurret() { return isTurret; }

    /**
     * Returns the type of enemy.
     *
     * @return type of enemy.
     */
    public EntityType getType() { return type; }

    /**
     * Sets the entity type of enemy
     * @param t the type of enemy
     */
    public void setType (EntityType t){type = t;}

    /**
     * Sets the velocity of the bullet to aim at the target
     *
     * @param offset float of the offset from the enemy's center that bullet shoots from
     */
    public void setVelocity(float offset) {
        projVel = target.getPosition().sub(getPosition());
        projVel.y -= offset;
    }

    /**
     * Returns the velocity of the projectiles that this enemy fires.
     *
     * @return velocity of the projectiles that this enemy fires.
     */
    public Vector2 getProjVelocity() { return projVel; }

    /**
     * Sets whether the is active to fire bullets
     *
     * @param a boolean of whether the enemy is active
     */
    public void setIsActive(boolean a) {
        isActive = a;
    }

    public boolean getShiftedActive() {
        return shiftedActive;
    }

    public void setShiftedActive(boolean a) {
        shiftedActive = a;
    }

    /**
     * Returns whether or not enemy can fire.
     *
     * @return whether or not enemy can fire.
     */
    public boolean canFire() {
        if (isTurret || isActive){
            return framesTillFire <= 0;
        } else {
            return false;
            //return framesTillFire <= 0 && isActive;
        }
    }

    /**
     * Reset or cool down the enemy weapon.
     *
     * If flag is true, the enemy will cool down by one animation frame.  Otherwise
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
        if (flag){
            limiter = 1;
        }
        else {
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

    /**
     * Moves the enemy left and right
     */
    public void applyForce() {
        if (!isActive()) {
            return;
        }

        // Don't want to be moving. Damp out player motion
        if (getMovement() == 0f) {
            forceCache.set(-getDamping()*getVX(),0);
            body.applyForce(forceCache,getPosition(),true);
        }

        // Velocity too high, clamp it
        if (Math.abs(getVX()) >= getMaxSpeed()) {
            setVX(Math.signum(getVX())*getMaxSpeed());
        } else {
            forceCache.set(getMovement(),0);
            body.applyForce(forceCache,getPosition(),true);
        }
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

    public boolean activatePhysics(World world) {
        if (!super.activatePhysics(world)) {
            return false;
        }

        Vector2 sensorCenterLeft = new Vector2(-getWidth() / 2, 0);
        FixtureDef sensorDef = new FixtureDef();
        sensorDef.density = DENSITY;
        sensorDef.isSensor = true;
        sensorShapeLeft = new PolygonShape();
        sensorShapeLeft.setAsBox(SENSOR_HEIGHT / 2, getHeight() / 2.0f + SENSOR_HEIGHT, sensorCenterLeft, 0.0f);
        sensorDef.shape = sensorShapeLeft;

        sensorFixtureLeft = body.createFixture(sensorDef);
        sensorFixtureLeft.setUserData(LEFT_SENSOR_NAME);

        Vector2 sensorCenterRight = new Vector2(getWidth() / 2, 0);
        sensorShapeRight = new PolygonShape();
        sensorShapeRight.setAsBox(SENSOR_HEIGHT / 2, getHeight() / 2.0f + SENSOR_HEIGHT, sensorCenterRight, 0.0f);
        sensorDef.shape = sensorShapeRight;

        sensorFixtureRight = body.createFixture(sensorDef);
        sensorFixtureRight.setUserData(RIGHT_SENSOR_NAME);

        return true;
    }

    /**
     * Updates the enemies line of sight to check if it can see the target
     * @param world
     * @param offset offset from the enemy center to where the bullet shoots from
     */
    public void createLineOfSight(World world, float offset) {
        Vector2 shootPos = getPosition().add(0f, offset);
        world.rayCast(sight, shootPos, target.getPosition());
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
        canvas.drawPhysics(sensorShapeLeft, Color.RED,getX(),getY(),getAngle(),drawScale.x,drawScale.y);
        canvas.drawPhysics(sensorShapeRight,Color.RED,getX(),getY(),getAngle(),drawScale.x,drawScale.y);
    }

    /**
     * Draws the physics object.
     *
     * @param canvas Drawing context
     */
    public void draw(GameCanvas canvas) {
        if (texture != null) {
            canvas.draw(texture,Color.WHITE,origin.x,origin.y,getX()*drawScale.x,getY()*drawScale.y,getAngle(),0.024f * drawScale.x,0.0225f * drawScale.y);
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

        if (fixture.getBody().getUserData() instanceof Avatar) {
            enemy.setIsActive(true);
            enemy.setShiftedActive(true);
            enemy.setMovement(0);
        } else if (fixture.getBody().getUserData() instanceof Enemy ||
                fixture.getBody().getUserData() instanceof Projectile) {
            return 1;
        } else {
            enemy.setIsActive(false);
            enemy.setShiftedActive(false);
            if (enemy.getMovement() == 0) {
                enemy.setMovement(enemy.getNextDirection());
            }
        }

        return fraction;
    }
}