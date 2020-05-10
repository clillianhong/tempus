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


    public TutorialModel(int lv, boolean unlocked, boolean finished, int resume,
                         LevelController[] rms, HashMap<Integer, String[]> cards,
                         TextureRegionDrawable[] bgs, TextureRegionDrawable[] dls, float [] mapping) {
        super(lv, unlocked, finished, resume, rms);
        tutorialCards = cards;
        this.bgs = bgs;
        this.dls = dls;
        this.map = mapping;

    }

    @Override
    public void createLevel() {
        for(int i = 0; i<rooms.length; i++) {
            TutorialController rc = (TutorialController) rooms[i];
            rc.loadContent();
            rc.setFirst(i==0);
            rc.setCard(tutorialCards.get(i));
            rc.setScreenListener(listener);
            rc.setCanvas(canvas);
        }
        //setting cutscenes
        ((TutorialController)rooms[0]).setCutScene(bgs, dls, map);
    }


}
