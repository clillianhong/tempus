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
public class Spikes extends PolygonObstacle {

    /**
     * Create a new SpikesModel with degenerate settings
     */
    public Spikes(float[] points) {
        super(points);
        region = null;
    }

    /**
     * Initializes the spikes via the given JSON value
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
        setBodyType(json.get("bodytype").asString().equals("static") ? BodyDef.BodyType.StaticBody : BodyDef.BodyType.DynamicBody);
        setDensity(json.get("density").asFloat());
        setFriction(json.get("friction").asFloat());
        setRestitution(json.get("restitution").asFloat());
        String key = json.get("texture").asString();
        TextureRegion texture = JsonAssetManager.getInstance().getEntry(key, TextureRegion.class);
        setTexture(texture);
        setSpace(json.get("space").asInt());
        setAngle(json.get("angle").asFloat());
    }

    public void draw(GameCanvas canvas) {
        if (region != null) {
            canvas.draw(texture,Color.WHITE, texture.getRegionWidth()/2, texture.getRegionHeight()/2, getX() * drawScale.x,getY() * drawScale.y, getAngle(), 0.01f * drawScale.x, 0.01f * drawScale.y);
        }
    }
}
