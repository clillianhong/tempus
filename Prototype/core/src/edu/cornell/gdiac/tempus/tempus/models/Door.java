package edu.cornell.gdiac.tempus.tempus.models;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.utils.JsonValue;
import edu.cornell.gdiac.tempus.GameCanvas;
import edu.cornell.gdiac.tempus.obstacle.BoxObstacle;
import edu.cornell.gdiac.util.*;

public class Door extends BoxObstacle {

    private int NEXT_LEVEL;
    private Vector2 scale;
    /** The goal door position */
    private static Vector2 GOAL_POS = new Vector2(29.5f,15.5f);


    public Door(float x, float y, float width, float height, int next_level) {
        super(x, y, width, height);
        NEXT_LEVEL = next_level;
    }
    public Door() {
        super(0,0,1,1);
        setSensor(true);
    }
    public void initialize(JsonValue key){
        TextureRegion goalTile = JsonAssetManager.getInstance().getEntry(key.get("texture").asString(), TextureRegion.class);
        float [] pos = key.get("pos").asFloatArray();
        float [] size = key.get("size").asFloatArray();
        setPosition(pos[0],pos[1]);
        setDimension(size[0],size[1]);
        setBodyType(key.get("bodytype").asString().equals("static")? BodyDef.BodyType.StaticBody : BodyDef.BodyType.DynamicBody);
        setDensity(key.get("density").asFloat());
        setFriction(key.get("friction").asFloat());
        setRestitution(key.get("restitution").asFloat());
        setTexture(goalTile);
        setName("goal");
        NEXT_LEVEL = key.get("nextlevel").asInt();
    }

    /**
     * Draws the physics object.
     *
     * @param canvas Drawing context
     */
    public void draw(GameCanvas canvas) {
        if (texture != null) {
            canvas.draw(texture,Color.WHITE,origin.x,origin.y,getX()*drawScale.x,getY()*drawScale.y,getAngle(),1,1);
        }
    }

//    public void initialize(){
//        setName
//    }
}
