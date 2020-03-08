package edu.cornell.gdiac.tempus.tempus.models;

import com.badlogic.gdx.math.*;
import com.badlogic.gdx.utils.*;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.physics.box2d.*;

import java.lang.reflect.*;

import edu.cornell.gdiac.tempus.GameCanvas;
import edu.cornell.gdiac.tempus.obstacle.BoxObstacle;
import edu.cornell.gdiac.util.*;

/**
 * A polygon shape representing the screen boundary
 *
 * Note that the constructor does very little.  The true initialization happens
 * by reading the JSON value.  In addition, this class overrides the drawing and
 * positioning functions to provide a tiled texture.
 */
public class Platform extends BoxObstacle {
    /** Texture information for this object */
    protected PolygonRegion region;
    /** The texture anchor upon region initialization */
    protected Vector2 anchor;

    /**
     * Create a new PlatformModel with degenerate settings
     */
    public Platform() {
        super(0,0,1,1);
        region = null;
    }

    /**
     * Initializes a PolygonRegion to support a tiled texture
     *
     * In order to keep the texture uniform, the drawing polygon position needs to
     * be absolute.  However, this can cause a problem when we want to move the
     * platform (e.g. a dynamic platform).  The purpose of the texture anchor is
     * to ensure that the texture does not move as the object moves.
     */
    private void initRegion() {
        if (texture == null) {
            return;
        }
        float[] scaled = new float[vertices.length];
        for(int ii = 0; ii < scaled.length; ii++) {
            if (ii % 2 == 0) {
                scaled[ii] = (vertices[ii] +getX())* drawScale.x;
            } else {
                scaled[ii] = (vertices[ii] +getY())* drawScale.y;
            }
        }
        short[] tris = {0,1,3,3,2,1};
        anchor = new Vector2(getX(),getY());
        region = new PolygonRegion(texture,scaled,tris);
    }

    /**
     * Reset the polygon vertices in the shape to match the dimension.
     */
    protected void resize(float width, float height) {
        super.resize(width,height);
        initRegion();
    }

    /**
     * Sets the current position for this physics body
     *
     * This method does not keep a reference to the parameter.
     *
     * @param value  the current position for this physics body
     */
    public void setPosition(Vector2 value) {
        super.setPosition(value.x,value.y);
        initRegion();
    }

    /**
     * Sets the current position for this physics body
     *
     * @param x  the x-coordinate for this physics body
     * @param y  the y-coordinate for this physics body
     */
    public void setPosition(float x, float y) {
        super.setPosition(x,y);
        initRegion();
    }

    /**
     * Sets the x-coordinate for this physics body
     *
     * @param value  the x-coordinate for this physics body
     */
    public void setX(float value) {
        super.setX(value);
        initRegion();
    }

    /**
     * Sets the y-coordinate for this physics body
     *
     * @param value  the y-coordinate for this physics body
     */
    public void setY(float value) {
        super.setY(value);
        initRegion();
    }

    /**
     * Sets the angle of rotation for this body (about the center).
     *
     * @param value  the angle of rotation for this body (in radians)
     */
    public void setAngle(float value) {
        throw new UnsupportedOperationException("Cannot rotate platforms");
    }

    /**
     * Sets the object texture for drawing purposes.
     *
     * In order for drawing to work properly, you MUST set the drawScale.
     * The drawScale converts the physics units to pixels.
     *
     * @param value  the object texture for drawing purposes.
     */
    public void setTexture(TextureRegion value) {
        super.setTexture(value);
        initRegion();
    }

    /**
     * Sets the drawing scale for this physics object
     *
     * The drawing scale is the number of pixels to draw before Box2D unit. Because
     * mass is a function of area in Box2D, we typically want the physics objects
     * to be small.  So we decouple that scale from the physics object.  However,
     * we must track the scale difference to communicate with the scene graph.
     *
     * We allow for the scaling factor to be non-uniform.
     *
     * @param x  the x-axis scale for this physics object
     * @param y  the y-axis scale for this physics object
     */
    public void setDrawScale(float x, float y) {
        super.setDrawScale(x,y);
        initRegion();
    }

    /**
     * Initializes the platform via the given JSON value
     *
     * The JSON value has been parsed and is part of a bigger level file.  However,
     * this JSON value is limited to the platform subtree
     *
     * @param json	the JSON subtree defining the dude
     */
    public void initialize(JsonValue json) {
        setName(json.name());
        float[] pos  = json.get("pos").asFloatArray();
        float[] size = json.get("size").asFloatArray();
        setPosition(pos[0],pos[1]);
        setDimension(size[0],size[1]);

        // Technically, we should do error checking here.
        // A JSON field might accidentally be missing
        setBodyType(json.get("bodytype").asString().equals("static") ? BodyDef.BodyType.StaticBody : BodyDef.BodyType.DynamicBody);
        setDensity(json.get("density").asFloat());
        setFriction(json.get("friction").asFloat());
        setRestitution(json.get("restitution").asFloat());

        // Reflection is best way to convert name to color
        Color debugColor;
        try {
            String cname = json.get("debugcolor").asString().toUpperCase();
            Field field = Class.forName("com.badlogic.gdx.graphics.Color").getField(cname);
            debugColor = new Color((Color)field.get(null));
        } catch (Exception e) {
            debugColor = null; // Not defined
        }
        int opacity = json.get("debugopacity").asInt();
        debugColor.mul(opacity/255.0f);
        setDebugColor(debugColor);

        // Now get the texture from the AssetManager singleton
        String key = json.get("texture").asString();
//        TextureRegion texture = JsonAssetManager.getInstance().getEntry(key, TextureRegion.class); //TODO : json asset manager!
        setTexture(texture);
    }

    /**
     * Draws the physics object.
     *
     * @param canvas Drawing context
     */
    public void draw(GameCanvas canvas) {
        if (region != null) {
            canvas.draw(region,Color.WHITE,0,0,(getX()-anchor.x)*drawScale.x,(getY()-anchor.y)*drawScale.y,getAngle(),1,1);
        }
    }
}