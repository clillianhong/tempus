package edu.cornell.gdiac.tempus.tempus.models;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import edu.cornell.gdiac.tempus.GameCanvas;
import edu.cornell.gdiac.tempus.obstacle.BoxObstacle;
import edu.cornell.gdiac.util.*;

public class Door extends BoxObstacle {
    /**
     * Enumeration to identify the state of the door
     */
    public enum DoorState {
        /** When door is locked */
        LOCKED,
        /** When door is unlocking */
        UNLOCKING,
        /** When door is open */
        OPEN,

    };
    // ANIMATION FIELDS
    /** Current state of the animation */
    DoorState animationState;
    /** Texture filmstrip for enemy chilling */
    private FilmStrip lockedTexture;
    /** Texture filmstrip for enemy attacking */
    private FilmStrip unlockingTexture;
    /** Texture filmstrip for enemy attacking */
    private FilmStrip openTexture;

    /** The texture filmstrip for the current animation */
    private FilmStrip currentStrip;
    /** The texture filmstrip for the neutral animation */
    private FilmStrip lockedStrip;
    /** The texture filmstrip for the attacking animation */
    private FilmStrip unlockingStrip;
    /** The texture filmstrip for the tp end animation */
    private FilmStrip openStrip;

    /** The frame rate for the animation. How many seconds should elapse
     * to move to the next frame. Lower values give a faster playback. */
//    private static float FRAME_RATE = 15;
//    /** The frame cooldown for the animation */
    private static float frame_cooldown = 15;

//    private float FRAME_RATE;
//    private float frame_cooldown;

    /** Minimize the size of the texture by the factor */
    private float minimizeScale = 1;

    private int NEXT_LEVEL;
    private Vector2 scale;
    /**
     * The goal door position
     */
    private static Vector2 GOAL_POS = new Vector2(29.5f, 15.5f);
    private boolean open;

    /** Texture for locked door */
    private TextureRegion locked_texture;


    public Door(float x, float y, float width, float height, int next_level, Vector2 scale) {
        super(x, y, 0.024f * scale.x * width, 0.0225f * scale.y * height);
        NEXT_LEVEL = next_level;
        open = false;
    }

    public Door() {
        super(0, 0, 1, 1);
        setSensor(true);
        open = false;
    }

    public void setOpen(boolean o) {
        open = o;
        }

    public boolean getOpen() {
        return open;
    }

    public void initialize(JsonValue key) {
        lockedTexture = JsonAssetManager.getInstance().getEntry("door_locked", FilmStrip.class);
        setFilmStrip(DoorState.LOCKED, lockedTexture);
        unlockingTexture = JsonAssetManager.getInstance().getEntry("door_unlocking", FilmStrip.class);
        setFilmStrip(DoorState.UNLOCKING, unlockingTexture);
        openTexture = JsonAssetManager.getInstance().getEntry("door_open", FilmStrip.class);
        setFilmStrip(DoorState.OPEN, openTexture);

        TextureRegion goalTile = JsonAssetManager.getInstance().getEntry(key.get("texture").asString(), TextureRegion.class);
        locked_texture = JsonAssetManager.getInstance().getEntry("goal_locked", TextureRegion.class);
        float[] pos = key.get("pos").asFloatArray();
        float[] size = key.get("size").asFloatArray();
        setPosition(pos[0], pos[1]);
        setDimension(size[0], size[1]);
        setBodyType(key.get("bodytype").asString().equals("static") ? BodyDef.BodyType.StaticBody : BodyDef.BodyType.DynamicBody);
        setDensity(key.get("density").asFloat());
        setFriction(key.get("friction").asFloat());
        setRestitution(key.get("restitution").asFloat());
        setTexture(goalTile);
        setSpace(key.get("space").asInt());
        setName("goal");
        setSensor(true);
        NEXT_LEVEL = key.get("nextlevel").asInt();
    }

    public void setAnimationState(DoorState s){ this.animationState = s; }

    /**
     * Sets the animation node for the given state
     *
     * @param state enumeration to identify the state
     * @param strip the animation for the given state
     */
    public void setFilmStrip(DoorState state, FilmStrip strip) {
        switch (state) {
            case LOCKED:
                lockedStrip = strip;
                break;
            case UNLOCKING:
                unlockingStrip = strip;
                break;
            case OPEN:
                openStrip = strip;
                break;
            default:
                assert false : "Invalid DoorState enumeration";
        }
    }

    /**
     * Animates the given state.
     *
     * @param state      The reference to the state
     * @param shouldLoop Whether the animation should loop
     */
    public void animate(DoorState state, boolean shouldLoop) {
        switch (state) {
            case LOCKED:
                currentStrip = lockedStrip;
                break;
            case UNLOCKING:
                currentStrip = unlockingStrip;
                break;
            case OPEN:
                currentStrip = openStrip;
                break;
            default:
                assert false : "Invalid EnemyState enumeration";
        }

        // when beginning a new state, set frame to first frame
        if (animationState != state) {
            currentStrip.setFrame(0);
            animationState = state;
        }

        // Adjust animation speed
        if (frame_cooldown > 0) {
            frame_cooldown--;
            return;
        } else
            frame_cooldown = Gdx.graphics.getFramesPerSecond()/10 ;

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
     * Draws the physics object.
     *
     * @param canvas Drawing context
     */
    public void draw(GameCanvas canvas) {
        if (currentStrip != null) {
            canvas.draw(currentStrip, Color.WHITE, origin.x, origin.y, getX() * drawScale.x, getY() * drawScale.y, getAngle(), 0.015f * drawScale.x, 0.015f * drawScale.y);
        }

//        TextureRegion locked_door = locked_texture != null ? locked_texture : texture;
//        if (texture != null) {
//            if (!open) {
//                canvas.draw(locked_door, Color.GRAY, origin.x, origin.y, getX() * drawScale.x, getY() * drawScale.y, getAngle(), 0.015f * drawScale.x, 0.015f * drawScale.y);
//            } else {
//                if (currentStrip != null) {
//                    canvas.draw(currentStrip, Color.WHITE, origin.x, origin.y, getX() * drawScale.x, getY() * drawScale.y, getAngle(), 0.015f * drawScale.x, 0.015f * drawScale.y);
//                }
////                canvas.draw(texture, Color.WHITE, origin.x, origin.y, getX() * drawScale.x, getY() * drawScale.y, getAngle(), 0.015f * drawScale.x, 0.015f * drawScale.y);
//            }
//        }
    }
}
