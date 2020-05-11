package edu.cornell.gdiac.tempus.tempus;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.JsonValue;
import edu.cornell.gdiac.tempus.InputController;
import edu.cornell.gdiac.tempus.tempus.models.Avatar;
import edu.cornell.gdiac.util.GameStateManager;
import edu.cornell.gdiac.util.JsonAssetManager;
import edu.cornell.gdiac.util.SoundController;

import java.util.HashMap;

//import javax.xml.soap.Text;

public class TutorialController extends LevelController {


    private String[] instructions;
    private boolean first;
    Table helpCard;

    @Override
    public void render(float delta) {
        super.render(delta);
    }

    private Table tutorialCard;
    private boolean isHelp;
    /** the current dialogue index */
    private int dialogueNum;
    /** the current background index  */
    private int bgNum;
    /** All cutscene dialogues */
    private TextureRegionDrawable[] dialogues;
    /** All cutscene backgrounds */
    private TextureRegionDrawable[] backgrounds;
    /** Mapping of cutscene index to the appropriate dialogue index **/
    private float[] dlMap;
    private Table cutsceneTable;
    private Container cutCont;


    /**
     * Creates and initialize a new instance of the tutorial level
     *
     * The game has default gravity and other settings
     *
     * @param json
     */
    public TutorialController(String json) {
        super(json);
        isHelp = false;
        //beginDisplay = 150;
        isTutorial = true;
        dialogueNum = 0;
        inputReady = false;

    }

    public void setCard(String[] card){
        instructions = card;
    }

    @Override
    public void reset() {
        dialogueNum = 0;
        isHelp = false;
        isTutorial = true;
        super.reset();
    }

    public void setFirst(boolean b) {first = b;};

    public void setCutScene(TextureRegionDrawable [] bgs, TextureRegionDrawable [] dls, float [] map){
        backgrounds = bgs;
        dialogues = dls;
        dlMap = map;
    }
    @Override
    public void preLoadContent() {
        super.preLoadContent();
    }

    @Override
    public void loadContent() {
        super.loadContent();
    }

    @Override
    public void resize(int width, int height) {

        super.resize(width, height);
    }

    @Override
    public void createUI() {
        float cw = canvas.getWidth()*0.8f;
        float ch = canvas.getHeight()*0.9f;

        super.createUI();

        tutorialCard = new Table();
        tutorialCard.setWidth(cw);
        tutorialCard.setHeight(ch);
//        tutorialCard.setPosition((canvas.getWidth()-cw)/2, (canvas.getHeight()-ch)/2);
        tutorialCard.setVisible(false);

        float label_height = ch/9f;
        TextureRegionDrawable overlay = new TextureRegionDrawable(overlayDark);

        if(instructions.length==1){ //make single card
            Label cardLabel = new Label(instructions[0], style);
            cardLabel.setWrap(true);
            cardLabel.setAlignment(Align.center);
            Container labCont = new Container();
            labCont.setActor(cardLabel);
            labCont.setSize(cw, label_height);

            Table bgOverlay = new Table();
            bgOverlay.setSize(cw, label_height);
            bgOverlay.setBackground(overlay);

            Stack st = new Stack();
            st.add(bgOverlay);
            st.add(cardLabel);

            tutorialCard.row().padTop(ch-label_height);
            tutorialCard.add(st).expandX();

        }else if(instructions.length==2){ //make double card
            Label cardLabel = new Label(instructions[0], style);
            cardLabel.setWrap(true);
            cardLabel.setAlignment(Align.center);

            Container labCont = new Container();
            labCont.setActor(cardLabel);
            labCont.setSize(cw, label_height);

            Table bgOverlay = new Table();
            bgOverlay.setSize(cw, label_height);
            bgOverlay.setBackground(overlay);

            Stack st = new Stack();
            st.add(bgOverlay);
            st.add(cardLabel);

            Label cardLabel2 = new Label(instructions[1], style);
            cardLabel2.setWrap(true);
            cardLabel2.setAlignment(Align.center);

            Container labCont2 = new Container();
            labCont2.setActor(cardLabel2);
            labCont2.setSize(cw, label_height);

            Table bgOverlay2 = new Table();
            bgOverlay2.setSize(cw, label_height);
            bgOverlay2.setBackground(overlay);

            Stack st2 = new Stack();
            st2.add(bgOverlay2);
            st2.add(cardLabel2);

            tutorialCard.add(st).expandX();
            tutorialCard.row().padTop(ch-label_height*2);
            tutorialCard.add(st2).expandX();
        }


//        helpCard = new Table();

//        tableStack.add(table);
//        tableStack.add(pauseButtonContainer);
        tableStack.add(tutorialCard);
//        helpCard.setVisible(true);
//        tableStack.add(helpCard);

        /*
         * END PAUSE SCREEN SETUP---------------------
         */



        if(first){
            createCutScene(edgeContainer);
        }

//        else{
//            if(GameStateManager.getInstance().lastRoom()){
//                createEndlevelUI(tableStack);
//            }
//            edgeContainer.setActor(tableStack);
//        }

        stage.addActor(edgeContainer);
//        Gdx.input.setInputProcessor(stage);
//        stage.getCamera().update();
//        stage.getViewport().apply();

        showTutorial();

    }

    public void createCutScene(Container edgeContainer){

        dialogueNum = 0;
        bgNum = 0;
//
//        TextureRegionDrawable storyBg = new TextureRegionDrawable(
//                new TextureRegion(new Texture(Gdx.files.internal("tutorial/beginningstorybg.jpg"))));

//        dialogues = new TextureRegionDrawable[3];
//
//        TextureRegionDrawable log1img = new TextureRegionDrawable(new TextureRegion(new Texture(Gdx.files.internal("tutorial/dialogue1.png"))));
//        TextureRegionDrawable log2img = new TextureRegionDrawable(new TextureRegion(new Texture(Gdx.files.internal("tutorial/dialogue2.png"))));
//        TextureRegionDrawable log3img = new TextureRegionDrawable(new TextureRegion(new Texture(Gdx.files.internal("tutorial/dialogue3.png"))));
//
//        dialogues[0] = log1img;
//        dialogues[1] = log2img;
//        dialogues[2] = log3img;

        cutCont = new Container<>();
        cutCont.setBackground(backgrounds[bgNum]);
        cutCont.setPosition(0, 0);
        cutCont.fillX();
        cutCont.fillY();

        cutsceneTable = new Table();
        cutsceneTable.setBackground(dialogues[dialogueNum]);
        cutCont.setActor(cutsceneTable);

        Table overlayBackButton = new Table();
        TextureRegionDrawable bup = new TextureRegionDrawable(new TextureRegion(new Texture(Gdx.files.internal("tutorial/forwardbutton.png"))));
        Button backButton = new Button(bup);
        backButton.addListener(new ClickListener(){
            @Override
            public void clicked(InputEvent event, float x, float y) {
                nextDialogue();
            }
        });
        overlayBackButton.add(backButton).width(sw/20f).height(sw/26f).expand().bottom().right().padBottom(40).padRight(60);

        Stack big = new Stack();

        big.add(cutCont);
        big.add(overlayBackButton);
        edgeContainer.setActor(big);

    }

    public void nextDialogue(){
        dialogueNum++;
        if(dialogueNum >= dialogues.length){
            cutCont.setVisible(false);
            edgeContainer.setActor(tableStack);
            inputReady = true;
        }else{
            if(dlMap[dialogueNum] != bgNum){
                bgNum = (int) dlMap[dialogueNum];
                cutCont.setBackground(backgrounds[bgNum]);
            }
            cutsceneTable.setBackground(dialogues[dialogueNum]);
        }
    }


    public void showTutorial(){
        tutorialCard.setVisible(true);
    }

    public void unshowTutorial(){
        tutorialCard.setVisible(false);
    }

    @Override
    public boolean preUpdate(float dt) {
        failed = false;

        if(first && dialogueNum < 3){
            return false;
        }

        if(drawEndRoom){
            unshowTutorial();
        }
        if(InputController.getInstance().didHelp()){
            isHelp = !isHelp;
        }
        return super.preUpdate(dt);
    }

    @Override
    public void update(float dt) {

//        if (isHelp) {
//            showTutorial();
//        } else {
//            unshowTutorial();
//        }

        avatar.setLives(5);

        super.update(dt);

    }



}
