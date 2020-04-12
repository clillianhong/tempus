package edu.cornell.gdiac.tempus.tempus;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import edu.cornell.gdiac.tempus.GameCanvas;
import edu.cornell.gdiac.util.JsonAssetManager;
import edu.cornell.gdiac.util.ScreenListener;

import static edu.cornell.gdiac.tempus.WorldController.EXIT_NEXT;
import static edu.cornell.gdiac.tempus.WorldController.EXIT_QUIT;


public class SelectLevelMode implements Screen {

    private class Level {

        private boolean unlocked;
        private String filePreview;
        private String fileLore;
        private String filePressUp;
        private String filePressDown;
        private String filePressLocked;
        Button button;
        TextArea textLore;

        public Level(boolean l, String preview, String lore, String buttonUp, String buttonDown, String buttonLocked, String textlore){
            unlocked = l;
            filePreview = preview;
            fileLore = lore;
            filePressUp = buttonUp;
            filePressDown = buttonDown;
            filePressLocked = buttonLocked;
            TextureRegionDrawable bup = new TextureRegionDrawable(new TextureRegion(new Texture(Gdx.files.internal(buttonUp))));
            TextureRegionDrawable bdown = new TextureRegionDrawable(new TextureRegion(new Texture(Gdx.files.internal(buttonDown))));
            button = new Button(bup, bdown);
            textLore = new TextArea(textlore, skin);
        }

        public boolean isUnlocked() {
            return unlocked;
        }

        public String getFilePreview() {
            return filePreview;
        }

        public String getFileLore() {
            return fileLore;
        }

        public void setUnlocked(boolean b){
            unlocked = b;
        }
    }

    private SpriteBatch batch;
    protected Stage stage;
    private Viewport viewport;
    private OrthographicCamera camera;
    private TextureAtlas atlas;
    protected Skin skin;
    protected Rectangle bounds;
    /** Listener that will update the player mode when we are done */
    protected ScreenListener listener;
    private boolean active;
    /** background texture region */
    private TextureRegion backgroundTexture;

    /** The index of the current level in focus*/
    private int currentLevel;
    /** All levels **/
    private Level [] levels;


    /** Reference to the game canvas */
    protected GameCanvas canvas;
    protected Vector2 scale;
    /** Width of the game world in Box2d units */
    protected static final float DEFAULT_WIDTH  = 32.0f;
    /** Height of the game world in Box2d units */
    protected static final float DEFAULT_HEIGHT = 18.0f;

    public SelectLevelMode(){
        bounds = new Rectangle(0,0,DEFAULT_WIDTH,DEFAULT_HEIGHT);
        canvas = null;
        scale = new Vector2();
        active = false;
        currentLevel = 0;
        int numLevels = 3;
        skin = new Skin(Gdx.files.internal("skins/flat_earth_skin/flat-earth-ui.json"));

        levels = new Level[numLevels];
        levels[0] = new Level(true, "textures/gui/selectmode/lv1_preview.png",
                "textures/gui/selectmode/lv1_lore.png",
                "textures/gui/selectmode/level1_up.png",
                "textures/gui/selectmode/level1_down.png",
                "textures/gui/selectmode/level1_locked.png",
                "this is level 1");
        levels[1] = new Level(false, "textures/gui/selectmode/lv1_preview.png", "textures/gui/selectmode/lv1_lore.png",
                "textures/gui/selectmode/level2_up.png",
                "textures/gui/selectmode/level2_down.png",
                "textures/gui/selectmode/level2_locked.png",
                "this is level 2"); //TODO: CHANGE TO LEVEL 2 AND 3 RESOURCES
        levels[2] = new Level(false, "textures/gui/selectmode/lv1_preview.png", "textures/gui/selectmode/lv1_lore.png",
                "textures/gui/selectmode/level3_up.png",
                "textures/gui/selectmode/level3_down.png",
                "textures/gui/selectmode/level3_locked.png",
                "this is level 3");
    }


    /* set the canvas for this mode */
    public void setCanvas(GameCanvas canvas) {
        this.canvas = canvas;
        this.scale.x = canvas.getWidth()/bounds.getWidth();
        this.scale.y = canvas.getHeight()/bounds.getHeight();
    }
    /**
     * Sets the ScreenListener for this mode
     *
     * The ScreenListener will respond to requests to quit.
     */
    public void setScreenListener(ScreenListener listener) {
        this.listener = listener;
    }


    public void createMode(){
//        atlas = new TextureAtlas("skin.atlas");

        batch = new SpriteBatch();
        camera = new OrthographicCamera();
        viewport = new FitViewport(bounds.getWidth(),bounds.getHeight(), camera);
        viewport.apply();

        camera.position.set(camera.viewportWidth / 2, camera.viewportHeight / 2, 0);
        camera.update();

        stage = new Stage(new ScreenViewport());
    }

    private void exitGame(){
        listener.exitScreen(this, EXIT_QUIT);
    }

    private void exitNext(){
        listener.exitScreen(this, EXIT_NEXT);
    }

    @Override
    public void show() {
        active = true;

        float sw = Gdx.graphics.getWidth();
        float sh = Gdx.graphics.getHeight();

        float row_height = sw / 12;
        float col_width = sh / 12;

        float cw = sw * 0.9f;
        float ch = sh * 0.8f;

        backgroundTexture = new TextureRegion(new Texture(Gdx.files.internal("textures/gui/selectmode/background.png")));

        //table container to center main table
        Container<Table> edgeContainer = new Container<Table>();
        edgeContainer.setSize(cw, ch);
        edgeContainer.setPosition((sw - cw) / 2.0f, (sh - ch) / 2.0f);
        edgeContainer.fillX();
        edgeContainer.fillY();



        //Stage should controller input:
        Gdx.input.setInputProcessor(stage);

        //outer dual table
        Table dualTable = new Table();
//        dualTable.setWidth(edgeContainer.getWidth());
        dualTable.setFillParent(true);

        Table rightTable = new Table();
        rightTable.setWidth(cw/2 - 10);

        Table levelTable = new Table();

        levelTable.row().padBottom(ch * 0.08f);
        for(Level lev : levels){
            levelTable.add(lev.button).size(cw/2*0.9f, ch/3*0.5f).expandX().fillX();
            levelTable.row().padBottom(ch * 0.08f);
        }
        levelTable.setDebug(true);

        Container<Table> levelTableContainer = new Container<>();
        levelTableContainer.setActor(levelTable);
        ScrollPane scroller = new ScrollPane(levelTableContainer);

        //back button
        TextureRegionDrawable bup = new TextureRegionDrawable(new TextureRegion(new Texture(Gdx.files.internal("textures/gui/selectmode/backbutton.png"))));
        Button backButton = new Button(bup);


        //preview panel
        Container<Image> pbContainer = new Container<>();
        pbContainer.size(sw/2f* 0.8f, sh/2f);
        Image previewBackground = new Image( new TextureRegion(new Texture(Gdx.files.internal("textures/gui/selectmode/preview_bg.png"))));
        pbContainer.setActor(previewBackground);

        Stack prevStack = new Stack();

        Container<Image> pIContainer = new Container<>();
        pIContainer.size(cw/2f* 0.5f, ch/2f * 0.6f);
        Texture prevTexture = new Texture(Gdx.files.internal(levels[currentLevel].getFilePreview()));
        Image previewImg = new Image( new TextureRegion(prevTexture));

        pIContainer.setActor(previewImg);


        prevStack.setDebug(true);
        prevStack.add(pbContainer);
        prevStack.add(pIContainer);


        //lore panel
        Container<Image> lbContainer = new Container<>();
        lbContainer.size(sw/2f*0.7f, sh/2f*0.6f);
        Image loreImage = new Image( new TextureRegion(new Texture(Gdx.files.internal(levels[currentLevel].getFileLore()))));
        lbContainer.setActor(loreImage);

        rightTable.add(prevStack);
        rightTable.row();
        rightTable.add(lbContainer);



        //add scroller to dual table
        dualTable.add(scroller).size(cw/2, ch).expandX();
        //add rightTable to dualTable
        dualTable.add(rightTable).size(cw/2, ch).expandX();
        //Create top right image preview

        edgeContainer.setActor(dualTable);
//        stage.setDebugAll(true);
        stage.addActor(edgeContainer);





        //Create buttons
//        ImageButton startButton = new ImageButton(new TextureRegionDrawable(new TextureRegion(new Texture(Gdx.files.internal("textures/gui/start_button.png")))));
////        startButton.getStyle().imageUp = new TextureRegionDrawable(new TextureRegion(new Texture(Gdx.files.internal("textures/gui/start_button.png"))));
////        startButton.getStyle().imageDown = new TextureRegionDrawable(new TextureRegion(new Texture(Gdx.files.internal("textures/gui/start_button.png"))));
//        ImageButton exitButton = new ImageButton(new TextureRegionDrawable(new TextureRegion(new Texture(Gdx.files.internal("textures/gui/quit_button.png")))));
////        exitButton.getStyle().imageUp = new TextureRegionDrawable(new TextureRegion(new Texture(Gdx.files.internal("textures/gui/quit_button.png"))));
////        exitButton.getStyle().imageDown = new TextureRegionDrawable(new TextureRegion(new Texture(Gdx.files.internal("textures/gui/quit_button.png"))));
//        ImageButton helpButton = new ImageButton(new TextureRegionDrawable(new TextureRegion(new Texture(Gdx.files.internal("textures/gui/help_button.png")))));
////        helpButton.getStyle().imageUp = new TextureRegionDrawable(new TextureRegion(new Texture(Gdx.files.internal("textures/gui/help_button.png"))));
////        helpButton.getStyle().imageDown = new TextureRegionDrawable(new TextureRegion(new Texture(Gdx.files.internal("textures/gui/help_button.png"))));
//        ImageButton aboutButton = new ImageButton(new TextureRegionDrawable(new TextureRegion(new Texture(Gdx.files.internal("textures/gui/about_button.png")))));
////        aboutButton.getStyle().imageUp = new TextureRegionDrawable(new TextureRegion(new Texture(Gdx.files.internal("textures/gui/about_button.png"))));
////        aboutButton.getStyle().imageDown = new TextureRegionDrawable(new TextureRegion(new Texture(Gdx.files.internal("textures/gui/about_button.png"))));
//
//        //Add listeners to buttons
//        startButton.addListener(new ClickListener(){
//            @Override
//            public void clicked(InputEvent event, float x, float y) {
//                exitNext();
//            }
//        });
//        exitButton.addListener(new ClickListener(){
//            @Override
//            public void clicked(InputEvent event, float x, float y) {
//                exitGame();
//            }
//        });

//        mainTable.debugAll();
//
//        //add header
//        mainTable.row();
//        mainTable.add(header).height(header.getHeight()*4).width(header.getWidth()*4).colspan(4);
//        mainTable.row();
//        mainTable.row().expandX().fillX();
//
//        //add buttons
//        mainTable.add(startButton).pad(50).expandX().fillX();
//        mainTable.add(exitButton).pad(50).expandX().fillX();
//        mainTable.add(helpButton).pad(50).expandX().fillX();
//        mainTable.add(aboutButton).pad(50).expandX().fillX();
//
//
//        tableContainer.setActor(mainTable);
//        //Add table to stage
//        stage.addActor(tableContainer);
    }

    public void update(){

    }

    @Override
    public void render(float delta) {
        if(active){
            Gdx.gl.glClearColor(.1f, .12f, .16f, 1);
            Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

            canvas.clear();
            canvas.begin();

            canvas.draw(backgroundTexture, Color.WHITE, 0, 0, canvas.getWidth(), canvas.getHeight());


            canvas.end();

            stage.act();
            stage.draw();
        }
    }

    @Override
    public void resize(int width, int height) {

    }

    @Override
    public void pause() {

    }

    @Override
    public void resume() {

    }

    @Override
    public void hide() {

    }

    @Override
    public void dispose() {

    }
}