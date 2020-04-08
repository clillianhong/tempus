package edu.cornell.gdiac.tempus.tempus;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Container;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import edu.cornell.gdiac.tempus.GameCanvas;
import edu.cornell.gdiac.util.ScreenListener;

import static edu.cornell.gdiac.tempus.WorldController.EXIT_NEXT;
import static edu.cornell.gdiac.tempus.WorldController.EXIT_QUIT;

public class MainMenuMode implements Screen {
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


    /** Reference to the game canvas */
    protected GameCanvas canvas;
    protected Vector2 scale;
    /** Width of the game world in Box2d units */
    protected static final float DEFAULT_WIDTH  = 32.0f;
    /** Height of the game world in Box2d units */
    protected static final float DEFAULT_HEIGHT = 18.0f;

    public MainMenuMode(){
        bounds = new Rectangle(0,0,DEFAULT_WIDTH,DEFAULT_HEIGHT);
        canvas = null;
        scale = new Vector2();
        active = false;
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
        skin = new Skin(Gdx.files.internal("jsons/uiskin.json"));

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

        float cw = sw * 0.7f;
        float ch = sh * 0.5f;

        Container<Table> tableContainer = new Container<Table>();
        tableContainer.setSize(cw, ch);
        tableContainer.setPosition((sw - cw) / 2.0f, (sh - ch) / 2.0f);
        tableContainer.fillX();


        //Stage should controller input:
        Gdx.input.setInputProcessor(stage);

        //Create Table
        Table mainTable = new Table(skin);
//        mainTable.setPosition(Gdx.graphics.getWidth()/2f,Gdx.graphics.getHeight()/2f);

        //Set table to fill stage
        mainTable.setWidth(stage.getWidth());
//        mainTable.align(Align.center | Align.top);
//        mainTable.center();
//        mainTable.setFillParent(true);
        //Set alignment of contents in the table.
//        mainTable.top();

        //Create buttons
        TextButton startButton = new TextButton("Start", skin);
        TextButton exitButton = new TextButton("Exit", skin);
        TextButton helpButton = new TextButton("Help", skin);
        TextButton aboutButton = new TextButton("About", skin);


        //Add listeners to buttons
        startButton.addListener(new ClickListener(){
            @Override
            public void clicked(InputEvent event, float x, float y) {
               exitNext();
            }
        });
        exitButton.addListener(new ClickListener(){
            @Override
            public void clicked(InputEvent event, float x, float y) {
                exitGame();
            }
        });

//        startButton.setSize(col_width*8, row_height);
//        startButton.setTransform(true);
//        startButton.scaleBy(3f);
//
//        exitButton.setSize(col_width*8, row_height);
//        exitButton.setTransform(true);
//        exitButton.scaleBy(3f);
        mainTable.debugAll();
        mainTable.row().colspan(4).expandX().fillX();

//        mainTable.row().expandX().fillX();
        mainTable.add(startButton).pad(50).expandX().fillX();
        mainTable.add(exitButton).pad(50).expandX().fillX();
        mainTable.add(helpButton).pad(50).expandX().fillX();
        mainTable.add(aboutButton).pad(50).expandX().fillX();
//        mainTable.row().expandX().fillX();


        tableContainer.setActor(mainTable);
        //Add table to stage
        stage.addActor(tableContainer);
    }

    @Override
    public void render(float delta) {
        if(active){
            Gdx.gl.glClearColor(.1f, .12f, .16f, 1);
            Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

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
