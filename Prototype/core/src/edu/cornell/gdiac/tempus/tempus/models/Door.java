package edu.cornell.gdiac.tempus.tempus.models;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import edu.cornell.gdiac.tempus.GameCanvas;
import edu.cornell.gdiac.tempus.obstacle.BoxObstacle;

public class Door extends BoxObstacle {

    private int NEXT_LEVEL;
    private TextureRegion goalTile;
    private Vector2 scale;
    /** The goal door position */
    private static Vector2 GOAL_POS = new Vector2(29.5f,15.5f);


    public Door(float x, float y, float width, float height, int next_level, Vector2 scale) {
        super(x, y,0.024f *scale.x *  width , 0.0225f * scale.y *  height);
        NEXT_LEVEL = next_level;
    }
    public Door() {
        super(0,0,1,1);
    }
    public void initialize(){

    }

    /**
     * Draws the physics object.
     *
     * @param canvas Drawing context
     */
    public void draw(GameCanvas canvas) {
        if (texture != null) {
            canvas.draw(texture,Color.WHITE,origin.x,origin.y,getX()*drawScale.x,getY()*drawScale.y,getAngle(),0.03f * drawScale.x,0.03f * drawScale.y);
        }
    }

//    public void initialize(){
//        setName
//    }
}
