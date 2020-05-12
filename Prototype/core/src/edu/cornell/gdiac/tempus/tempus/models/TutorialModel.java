package edu.cornell.gdiac.tempus.tempus.models;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import edu.cornell.gdiac.tempus.tempus.LevelController;
import edu.cornell.gdiac.tempus.tempus.TutorialController;
import edu.cornell.gdiac.util.JsonAssetManager;

import java.util.HashMap;

public class TutorialModel extends LevelModel {

    private HashMap<Integer, String[]> tutorialCards;
    private float[] map;
    private TextureRegionDrawable[] bgs;
    private TextureRegionDrawable[] dls;
<<<<<<< HEAD
    /** the current dialogue index */
    public int dialogueNum;
    /** the current background index  */
    public int bgNum;
    /** array of card indexes to stop at for displaying cutscenes */
    public float [] cutsceneStopArray;
    /** array of card indexes to start at for displaying cutscenes */
    public float [] cutsceneStartArray;
=======
>>>>>>> e9fc9033905eda29e1f3f9999c2216575bd7fc13


    public TutorialModel(int lv, boolean unlocked, boolean finished, int resume,
                         LevelController[] rms, HashMap<Integer, String[]> cards,
<<<<<<< HEAD
                         TextureRegionDrawable[] bgs, TextureRegionDrawable[] dls, float [] mapping,
                         float [] stopArray, float [] startArray) {
        super(lv, unlocked, finished, resume, rms);
        this.tutorialCards = cards;
        this.bgs = bgs;
        this.dls = dls;
        this.map = mapping;
        this.cutsceneStopArray = stopArray;
        this.cutsceneStartArray = startArray;
=======
                         TextureRegionDrawable[] bgs, TextureRegionDrawable[] dls, float [] mapping) {
        super(lv, unlocked, finished, resume, rms);
        tutorialCards = cards;
        this.bgs = bgs;
        this.dls = dls;
        this.map = mapping;
>>>>>>> e9fc9033905eda29e1f3f9999c2216575bd7fc13

    }

    @Override
    public void createLevel() {
        for(int i = 0; i<rooms.length; i++) {
            TutorialController rc = (TutorialController) rooms[i];
            rc.loadContent();
<<<<<<< HEAD
            rc.setCard(tutorialCards.get(i));
            rc.setScreenListener(listener);
            rc.setCanvas(canvas);
            if(i < cutsceneStopArray.length && cutsceneStopArray[i] != -1){
                rc.setCutScene(bgs, dls, map, (int) cutsceneStopArray[i], (int) cutsceneStartArray[i]);
            }
        }

=======
            rc.setFirst(i==0);
            rc.setCard(tutorialCards.get(i));
            rc.setScreenListener(listener);
            rc.setCanvas(canvas);
        }
        //setting cutscenes
        ((TutorialController)rooms[0]).setCutScene(bgs, dls, map);
>>>>>>> e9fc9033905eda29e1f3f9999c2216575bd7fc13
    }


}
