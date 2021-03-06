package edu.cornell.gdiac.tempus.tempus.models;

import com.badlogic.gdx.math.*;
import com.badlogic.gdx.utils.*;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.physics.box2d.*;

import java.lang.reflect.*;

import edu.cornell.gdiac.tempus.GameCanvas;
import edu.cornell.gdiac.tempus.obstacle.BoxObstacle;
import edu.cornell.gdiac.tempus.obstacle.PolygonObstacle;
import edu.cornell.gdiac.util.*;

/**
 * A polygon shape representing the screen boundary
 *
 * Note that the constructor does very little. The true initialization happens
 * by reading the JSON value. In addition, this class overrides the drawing and
 * positioning functions to provide a tiled texture.
 */
public class Platform extends PolygonObstacle {
    /** Texture information for this object */
    protected PolygonRegion region;
    /** The texture anchor upon region initialization */
    protected Vector2 anchor;
    /** The shape for debug mode */
    protected PolygonShape shape;
    /** The debug color for debug mode */
    protected Color debugColor;

    private String key;

    /**
     * Create a new PlatformModel with degenerate settings
     */
    public Platform(float[] points, float cx, float cy) {
        super(points, cx, cy);
        shape = new PolygonShape();
        region = null;
        key = null;
    }

    public Platform(float[] points) {
        super(points);
        shape = new PolygonShape();
        region = null;
        key = null;
    }

    /**
     * Initializes a PolygonRegion to support a tiled texture
     *
     * In order to keep the texture uniform, the drawing polygon position needs to
     * be absolute. However, this can cause a problem when we want to move the
     * platform (e.g. a dynamic platform). The purpose of the texture anchor is to
     * ensure that the texture does not move as the object moves.
     */
    private void initRegion() {
        if (texture == null) {
            return;
        }
        float[] scaled = new float[vertices.length];
        for (int ii = 0; ii < scaled.length; ii++) {
            if (ii % 2 == 0) {
                scaled[ii] = (vertices[ii]  + getX()) * drawScale.x;
            } else {
                scaled[ii] = (vertices[ii] + getY()) * drawScale.y;
            }
        }
        short[] tris = { 0, 1, 3, 3, 2, 1 };
        anchor = new Vector2(getX(), getY());
        region = new PolygonRegion(texture, scaled, tris);
    }

    /**
     * Reset the polygon vertices in the shape to match the dimension.
     */
    protected void resize(float width, float height) {
        super.resize(width, height);
        initRegion();
    }

    /**
     * Sets the current position for this physics body
     *
     * This method does not keep a reference to the parameter.
     *
     * @param value the current position for this physics body
     */
    public void setPosition(Vector2 value) {
        super.setPosition(value.x, value.y);
        initRegion();
    }

    /**
     * Gets the current position for this physics body
     *
     * @return the current position for this physics body
     */
    public Vector2 getPosition () { return super.getPosition(); }

    /**
     * Sets the current position for this physics body
     *
     * @param x the x-coordinate for this physics body
     * @param y the y-coordinate for this physics body
     */
    public void setPosition(float x, float y) {
        super.setPosition(x, y);
        initRegion();
    }

    /**
     * Sets the x-coordinate for this physics body
     *
     * @param value the x-coordinate for this physics body
     */
    public void setX(float value) {
        super.setX(value);
        initRegion();
    }

    /**
     * Sets the y-coordinate for this physics body
     *
     * @param value the y-coordinate for this physics body
     */
    public void setY(float value) {
        super.setY(value);
        initRegion();
    }

    /**
     * Sets the angle of rotation for this body (about the center).
     *
     * @param value the angle of rotation for this body (in radians)
     */
    public void setAngle(float value) {
        throw new UnsupportedOperationException("Cannot rotate platforms");
    }

    /**
     * Sets the object texture for drawing purposes.
     *
     * In order for drawing to work properly, you MUST set the drawScale. The
     * drawScale converts the physics units to pixels.
     *
     * @param value the object texture for drawing purposes.
     */
    public void setTexture(TextureRegion value) {
        super.setTexture(value);
        initRegion();
    }

    /**
     * Sets the drawing scale for this physics object
     *
     * The drawing scale is the number of pixels to draw before Box2D unit. Because
     * mass is a function of area in Box2D, we typically want the physics objects to
     * be small. So we decouple that scale from the physics object. However, we must
     * track the scale difference to communicate with the scene graph.
     *
     * We allow for the scaling factor to be non-uniform.
     *
     * @param x the x-axis scale for this physics object
     * @param y the y-axis scale for this physics object
     */
    public void setDrawScale(float x, float y) {
        super.setDrawScale(x, y);
        initRegion();
    }

    /**
     * Initializes the platform via the given JSON value
     *
     * The JSON value has been parsed and is part of a bigger level file. However,
     * this JSON value is limited to the platform subtree
     *
     * @param json the JSON subtree defining the dude
     */
    public void initialize(JsonValue json) {
        setName(json.get("name").asString());
        float[] pos =json.get("pos").asFloatArray();
        setPosition(pos[0],pos[1]);
        getFilterData().groupIndex = -1;
        setBodyType(json.get("bodytype").asString().equals("static") ? BodyDef.BodyType.StaticBody : BodyDef.BodyType.DynamicBody);
        setDensity(json.get("density").asFloat());
        setFriction(json.get("friction").asFloat());
        setRestitution(json.get("restitution").asFloat());
        key = json.get("texture").asString();
        TextureRegion texture = JsonAssetManager.getInstance().getEntry(key, TextureRegion.class);
        setTexture(texture);
        setSpace(json.get("space").asInt());
    }

    public void draw(GameCanvas canvas) {
        if (region != null) {
             canvas.draw(texture,Color.WHITE, 0, 0, getX() * drawScale.x,getY() * drawScale.y, getAngle(), 0.008f * drawScale.x, 0.0075f * drawScale.y);
        }
    }

    public void shift(boolean shifted) {
        if (shifted){
            TextureRegion texture = JsonAssetManager.getInstance().getEntry(key + "_past", TextureRegion.class);
            setTexture(texture);
        } else {
            TextureRegion texture = JsonAssetManager.getInstance().getEntry(key, TextureRegion.class);
            setTexture(texture);
        }
    }
}