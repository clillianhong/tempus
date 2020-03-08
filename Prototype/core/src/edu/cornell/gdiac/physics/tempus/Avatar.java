package edu.cornell.gdiac.physics.tempus;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.physics.box2d.FixtureDef;
import com.badlogic.gdx.physics.box2d.PolygonShape;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.utils.Array;
import edu.cornell.gdiac.physics.GameCanvas;
import edu.cornell.gdiac.physics.obstacle.CapsuleObstacle;
import com.badlogic.gdx.math.*;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.physics.box2d.*;

import edu.cornell.gdiac.physics.*;
import edu.cornell.gdiac.physics.obstacle.*;


/**
 * Player avatar for the plaform game.
 *
 * Note that this class returns to static loading.  That is because there are
 * no other subclasses that we might loop through.
 */
public class Avatar extends CapsuleObstacle {
    // Physics constants
    /** The density of the character */
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
    /** Identifier to allow us to track the sensor in ContactListener */
    private static final String SENSOR_NAME = "DudeGroundSensor";
    //added for prototype
    /** Distance for dashing across screen */
    private static final float DASH_RANGE = 4;
    /** Dash force multiplier */
    private static float dashForce = 1000;
    /** Array containing orientation vectors based on the enums for orientation */
    private static final Vector2 [] orients =
            {new Vector2(0,1), new Vector2(0,-1), new Vector2(-1,0), new Vector2(1,0)};
    private static final String LEFT_SENSOR_NAME = "DudeLeftSensor";
    private static final String RIGHT_SENSOR_NAME = "DudeRightSensor";
    private static final String TOP_SENSOR_NAME = "DudeTopSensor";

    // This is to fit the image to a tigher hitbox
    /** The amount to shrink the body fixture (vertically) relative to the image */
    private static final float VSHRINK = 0.95f;
    /** The amount to shrink the body fixture (horizontally) relative to the image */
    private static final float HSHRINK = 0.7f;
    /** The amount to shrink the sensor fixture (horizontally) relative to the image */
    private static final float SSHRINK = 0.6f;

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
    /** Ground sensor to represent our feet */
    private Fixture sensorFixture;
    private PolygonShape sensorShape;
    /** Left sensor to determine sticking on the left side */
    private Fixture sensorFixtureLeft;
    private PolygonShape sensorShapeLeft;
    /** Right sensor to determine sticking on the left side */
    private Fixture sensorFixtureRight;
    private PolygonShape sensorShapeRight;
    /** Top sensor to determine sticking on the top */
    private Fixture sensorFixtureTop;
    private PolygonShape sensorShapeTop;

    //added for prototype
    /** Whether we are actively dashing */
    private boolean isDashing;
    /** Whether the initial dash force has been applied */
    private boolean hasDashed;
    /** Whether we are actively sticking */
    private boolean isSticking;
    /** Whether already stuck */
    private boolean wasSticking;
    /** Whether we are actively holding */
    private boolean isHolding;
    /** the current orientation of the player */
    private AvatarOrientation orientation;
    /** the dash distance of the player (max is DASH_RANGE) */
    private float dashDistance;
    /** the dash starting position */
    private Vector2 dashStartPos;
    /** the dash direction */
    private Vector2 dashDirection;
    /** the bullet the character is currently holding */
    private Obstacle heldBullet;

    /** Cache for internal force calculations */
    private Vector2 forceCache = new Vector2();

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
        movement = value;
        // Change facing if appropriate
        if (movement < 0) {
            faceRight = false;
        } else if (movement > 0) {
            faceRight = true;
        }
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
        return !isDashing && isGrounded;
    }

    /**
     * Returns the avatar orientation enum
     *
     * @return true if the dude is actively dashing.
     */
    public Vector2 getAvatarOrientation() {
        return orients[orientation.ordinal()];
    }

    /**
     * Returns the max dash range
     *
     * @return max dash range
     */
    public float getDashRange(){ return DASH_RANGE; }

    /**
     * Sets avatar orientation
     */
    public void setAvatarOrientation(AvatarOrientation or) {
        orientation = or;
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

    public void setHeldBullet(Obstacle bullet) {heldBullet = bullet;}
    public Obstacle getHeldBullet() {return heldBullet; }

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
    public Avatar(float x, float y, float width, float height, AvatarOrientation or) {
        super(x,y,width*HSHRINK,height*VSHRINK);
        setDensity(DENSITY);
//        setFriction(FRICTION);  /// HE WILL STICK TO WALLS IF YOU FORGET
        setFixedRotation(true);

        // Gameplay attributes
        isGrounded = false;
        isShooting = false;
        isJumping = false;
        faceRight = true;

        //prototype added
        isDashing = false;
        orientation = or;
        isSticking = false;
        dashDistance = DASH_RANGE;
        dashStartPos = new Vector2(x,y);

        shootCooldown = 0;
        jumpCooldown = 0;
        setName("dude");
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

        // Ground Sensor
        // -------------
        // We only allow the dude to jump when he's on the ground.
        // Double jumping is not allowed.
        //
        // To determine whether or not the dude is on the ground,
        // we create a thin sensor under his feet, which reports
        // collisions with the world but has no collision response.
        Vector2 sensorCenter = new Vector2(0, -getHeight() / 2);
        FixtureDef sensorDef = new FixtureDef();
        sensorDef.density = DENSITY;
        sensorDef.isSensor = true;
        sensorShape = new PolygonShape();
        sensorShape.setAsBox(getWidth() / 2.0f - 2.0f * SENSOR_HEIGHT, SENSOR_HEIGHT, sensorCenter, 0.0f);
        sensorDef.shape = sensorShape;

        sensorFixture = body.createFixture(sensorDef);
        sensorFixture.setUserData(getSensorName());

        // To determine whether the body collides on the left side
        Vector2 sensorCenterLeft = new Vector2(-getWidth() / 2, 0);
        sensorShapeLeft = new PolygonShape();
        sensorShapeLeft.setAsBox(SENSOR_HEIGHT, getHeight() / 2.0f - 2.0f * SENSOR_HEIGHT, sensorCenterLeft, 0.0f);
        sensorDef.shape = sensorShapeLeft;

        sensorFixtureLeft = body.createFixture(sensorDef);
        sensorFixtureLeft.setUserData(getLeftSensorName());

        // To determine whether the body collides on the right side
        Vector2 sensorCenterRight = new Vector2(getWidth() / 2, 0);
        sensorShapeRight = new PolygonShape();
        sensorShapeRight.setAsBox(SENSOR_HEIGHT, getHeight() / 2.0f - 2.0f * SENSOR_HEIGHT, sensorCenterRight, 0.0f);
        sensorDef.shape = sensorShapeRight;

        sensorFixtureRight = body.createFixture(sensorDef);
        sensorFixtureRight.setUserData(getRightSensorName());

        // To determine whether the body collides on the right side
        Vector2 sensorCenterTop = new Vector2(0, getHeight() / 2);
        sensorShapeTop = new PolygonShape();
        sensorShapeTop.setAsBox(getWidth() / 2.0f - 2.0f * SENSOR_HEIGHT, SENSOR_HEIGHT, sensorCenterTop, 0.0f);
        sensorDef.shape = sensorShapeTop;

        sensorFixtureTop = body.createFixture(sensorDef);
        sensorFixtureTop.setUserData(getTopSensorName());

        return true;
    }

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
            System.out.println("APPLYING FORCE");
            //linearly interpolate dashForce
            System.out.println("dash direction raw: " + dashDirection);
            System.out.println("dash direction norm: " + dashDirection.nor());
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
        // Apply cooldowns

        //check if dash must end
        if(isDashing){
            if(getPosition().dst(getDashStartPos()) > getDashDistance()){
                System.out.println("DASHED TOO FAR");
                setDashing(false);
                setLinearVelocity(new Vector2(0,0));
            }
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
     * Draws the physics object.
     *
     * @param canvas Drawing context
     */
    public void draw(GameCanvas canvas) {
        float effect = faceRight ? 1.0f : -1.0f;
        canvas.draw(texture, Color.WHITE,origin.x,origin.y,getX()*drawScale.x,getY()*drawScale.y,getAngle(),effect,1.0f);
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
        canvas.drawPhysics(sensorShape,Color.RED,getX(),getY(),getAngle(),drawScale.x,drawScale.y);
        canvas.drawPhysics(sensorShapeLeft,Color.RED,getX(),getY(),getAngle(),drawScale.x,drawScale.y);
        canvas.drawPhysics(sensorShapeRight,Color.RED,getX(),getY(),getAngle(),drawScale.x,drawScale.y);
        canvas.drawPhysics(sensorShapeTop,Color.RED,getX(),getY(),getAngle(),drawScale.x,drawScale.y);
    }
}