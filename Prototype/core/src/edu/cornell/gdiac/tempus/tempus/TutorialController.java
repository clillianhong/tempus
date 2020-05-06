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
import com.badlogic.gdx.utils.JsonValue;
import edu.cornell.gdiac.tempus.InputController;
import edu.cornell.gdiac.tempus.tempus.models.Avatar;
import edu.cornell.gdiac.util.GameStateManager;
import edu.cornell.gdiac.util.JsonAssetManager;
import edu.cornell.gdiac.util.SoundController;

//import javax.xml.soap.Text;

public class TutorialController extends LevelController {


    private TextureRegionDrawable tutorial_card;
    private boolean first;
    Table helpCard;

    @Override
    public void render(float delta) {
        super.render(delta);
    }

    private int beginDisplay;
    private Table tutorialCard;
    private boolean isHelp;
    private int dialogueNum;
    private TextureRegionDrawable[] dialogues;
    private Table cutsceneTable;
    private Container cutCont;

    private Container<Stack> edgeContainer;
    private Stack tableStack;


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

    }

    public void setCard(TextureRegionDrawable card){
        tutorial_card = card;
    }

    public void setFirst(boolean b) {first = b;};
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
        float sw = canvas.getWidth();
        float sh = canvas.getHeight();

        // table container to center main table
        edgeContainer = new Container<Stack>();
        edgeContainer.setSize(sw, sh);
        edgeContainer.setPosition(0, 0);
        edgeContainer.fillX();
        edgeContainer.fillY();

        tableStack = new Stack();

        /*
         * START PAUSE SCREEN SETUP ---------------------
         */
        TextureRegionDrawable pauseButtonResource = new TextureRegionDrawable(
                new TextureRegion(new Texture(Gdx.files.internal("textures/gui/pausebutton.png"))));
        TextureRegionDrawable pauseBG = new TextureRegionDrawable(
                new TextureRegion(new Texture(Gdx.files.internal("textures/gui/pause_filter_50_black.png"))));
        TextureRegionDrawable pauseBox = new TextureRegionDrawable(
                new TextureRegion(new Texture(Gdx.files.internal("textures/gui/frame_pause.png"))));
        TextureRegionDrawable resumeResource = new TextureRegionDrawable(
                new TextureRegion(new Texture(Gdx.files.internal("textures/gui/pause_resume_button.png"))));
        TextureRegionDrawable restartResource = new TextureRegionDrawable(
                new TextureRegion(new Texture(Gdx.files.internal("textures/gui/pause_restart_button.png"))));
        TextureRegionDrawable exitResource = new TextureRegionDrawable(
                new TextureRegion(new Texture(Gdx.files.internal("textures/gui/pause_exit_button.png"))));


//        press_h_card = new TextureRegionDrawable(
//                new TextureRegion(new Texture(Gdx.files.internal("tutorial/helpcard.png"))));

        pauseButtonContainer = new Container<>();
        pauseButtonContainer.setBackground(pauseBG);
        pauseButtonContainer.setPosition(0, 0);
        pauseButtonContainer.fillX();
        pauseButtonContainer.fillY();

        pauseTable = new Table();
        //pauseTable.background(pauseBox);
        pauseButtonContainer.setActor(pauseTable);
        pauseButtonContainer.setVisible(false);

        Button resumeButton = new Button(resumeResource);
        resumeButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                super.clicked(event, x, y);
                unpauseGame();
            }
        });

        Button restartButton = new Button(restartResource);
        restartButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                super.clicked(event, x, y);
                reset();
                unpauseGame();
            }
        });

        Button exitButton = new Button(exitResource);
        exitButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                super.clicked(event, x, y);
                exitLevelSelect();
            }
        });
        pauseTable.add(resumeButton).width(sw / 4 / 1.5f).height(sh / 5.1f / 1.5f).center().expandX().padBottom(sh / 20);
        pauseTable.row();
        pauseTable.add(restartButton).width(sw / 4 / 1.5f).height(sh / 5.1f / 1.5f).center().expandX().padBottom(sh / 20);
        ;
        pauseTable.row();
        pauseTable.add(exitButton).width(sw / 4 / 1.5f).height(sh / 5.1f / 1.5f).expandX();

        tutorialCard = new Table();
        tutorialCard.setBackground(pauseBG);
        tutorialCard.setPosition(0, 0);
        tutorialCard.setBackground(tutorial_card);
        tutorialCard.setVisible(false);

        helpCard = new Table();

        tableStack.add(table);
        tableStack.add(pauseButtonContainer);
        tableStack.add(tutorialCard);
        helpCard.setVisible(true);
        tableStack.add(helpCard);

        /*
         * END PAUSE SCREEN SETUP---------------------
         */



        if(first){
            createCutScene(edgeContainer);
        }else{
            if(GameStateManager.getInstance().lastRoom()){
                createEndlevelUI(tableStack);
            }
            edgeContainer.setActor(tableStack);
        }

        stage.addActor(edgeContainer);
        Gdx.input.setInputProcessor(stage);
        stage.getCamera().update();
        stage.getViewport().apply();

        showTutorial();

    }

    public void createCutScene(Container edgeContainer){
        dialogues = new TextureRegionDrawable[3];
        TextureRegionDrawable storyBg = new TextureRegionDrawable(
                new TextureRegion(new Texture(Gdx.files.internal("tutorial/beginningstorybg.jpg"))));

        TextureRegionDrawable log1img = new TextureRegionDrawable(new TextureRegion(new Texture(Gdx.files.internal("tutorial/dialogue1.png"))));
        TextureRegionDrawable log2img = new TextureRegionDrawable(new TextureRegion(new Texture(Gdx.files.internal("tutorial/dialogue2.png"))));
        TextureRegionDrawable log3img = new TextureRegionDrawable(new TextureRegion(new Texture(Gdx.files.internal("tutorial/dialogue3.png"))));


        dialogues[0] = log1img;
        dialogues[1] = log2img;
        dialogues[2] = log3img;

        cutCont = new Container<>();
        cutCont.setBackground(storyBg);
        cutCont.setPosition(0, 0);
        cutCont.fillX();
        cutCont.fillY();

        cutsceneTable = new Table();
        cutsceneTable.setBackground(log1img);
        cutCont.setActor(cutsceneTable);

        Container forwardCont = new Container<>();
        forwardCont.setBackground(storyBg);
        forwardCont.setPosition(0, 0);
        forwardCont.fillX();
        forwardCont.fillY();

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
        if(dialogueNum >= 3){
            cutCont.setVisible(false);
            edgeContainer.setActor(tableStack);
        }else{
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
