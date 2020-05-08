package edu.cornell.gdiac.tempus.tempus;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import edu.cornell.gdiac.tempus.GameCanvas;
import edu.cornell.gdiac.tempus.tempus.models.LevelModel;
import edu.cornell.gdiac.tempus.tempus.models.ScreenExitCodes;
import edu.cornell.gdiac.util.FilmStrip;
import edu.cornell.gdiac.util.GameStateManager;
import edu.cornell.gdiac.util.JsonAssetManager;
import edu.cornell.gdiac.util.ScreenListener;

public class SelectRoomMode implements Screen {

    /** LISTENERS EVENTS TO CHANGE SCREEN **/
    private void exitBack(){
        listener.exitScreen(this, ScreenExitCodes.EXIT_PREV.ordinal());
    }
    private void exitToRoom(){ listener.exitScreen(this, ScreenExitCodes.ROOM_SELECT.ordinal());}

    protected Stage stage;
    private Viewport viewport;
    private OrthographicCamera camera;

    protected Rectangle bounds;
    /** Listener that will update the player mode when we are done */
    protected ScreenListener listener;
    private boolean active;

    //TEXTURES
    /** background texture region */
    private TextureRegion backgroundTexture;

    int sw = 1920/2;
    int sh = 1080/2;

    /** Reference to the game canvas */
    protected GameCanvas canvas;
    protected Vector2 scale;
    /** Width of the game world in Box2d units */
    protected static final float DEFAULT_WIDTH  = 32.0f;
    /** Height of the game world in Box2d units */
    protected static final float DEFAULT_HEIGHT = 18.0f;

    //SCENE2D COMPONENTS
    private Table wholescreenTable;
    private Table[]  roomTables;
    private Container roomContainer;
    private Button backButton;
    private Button prevRoomButton;
    private Button nextRoomButton;
    TextureRegionDrawable prevTexOff;
    TextureRegionDrawable prevTexOn;
    TextureRegionDrawable nextTexOff;
    TextureRegionDrawable nextTexOn;

    //level info
    int levelNum;
    int numRooms;
    int numPages;
    int curPage;

    /** Font */
    private BitmapFont font;
    /** Glyph layout for on the fly generation */
    private static GlyphLayout glyphLayout;



    public SelectRoomMode(int level){
        bounds = new Rectangle(0,0,DEFAULT_WIDTH,DEFAULT_HEIGHT);
        canvas = null;
        scale = new Vector2();
        active = true;

        camera = new OrthographicCamera(sw,sh);
        viewport = new FitViewport(sw, sh, camera);
        viewport.apply();

        camera.position.set(camera.viewportWidth / 2, camera.viewportHeight / 2, 0);
        camera.update();

        stage = new Stage(viewport);

        levelNum = level;
        numRooms = GameStateManager.getInstance().getLevel(level).getRoomCount();

        font = new BitmapFont(Gdx.files.internal("fonts/carterone.fnt"));
        glyphLayout  = new GlyphLayout();

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
        camera.update();
        stage = new Stage(viewport);

    }


    @Override
    public void show() {
        Gdx.input.setInputProcessor(stage);
        final GameStateManager gameManager = GameStateManager.getInstance();

        wholescreenTable = new Table();
        wholescreenTable.setWidth(sw);
        wholescreenTable.setHeight(sh);

        numPages = numRooms % 4 == 0 ? numRooms/4 : (numRooms/4) + 1;
        roomTables = new Table[numPages];
        curPage = 0;

        float cw = sw * 0.9f;
        float ch = sh * 0.5f;

        font.getData().setScale(0.75f);
        Label.LabelStyle style = new Label.LabelStyle(font, Color.WHITE);
        FilmStrip bg = JsonAssetManager.getInstance().getEntry("level"+levelNum+"_bg", FilmStrip.class);
//        backgroundTexture = new TextureRegion(bg.getTexture());
        backgroundTexture = new TextureRegion(new Texture(Gdx.files.internal("textures/gui/roomselect/room_select.png")));
        TextureRegionDrawable overlayBG = new TextureRegionDrawable(
                new TextureRegion(new Texture(Gdx.files.internal("textures/gui/pause_filter_50_black.png"))));

        TextureRegionDrawable lockedRoom = new TextureRegionDrawable(
                new TextureRegion(new Texture(Gdx.files.internal("textures/gui/roomselect/locked_room.png"))));

//        wholescreenTable.setBackground(overlayBG);
        int wid = bg.getTexture().getWidth();
        int ht = bg.getTexture().getHeight();
        final int highestRoom = gameManager.getCurrentLevel().getHighestUnlockedRoom();

        //load all room buttons
        int row = 0;
        System.out.println("width: " + wid);
        System.out.println("height: " + ht);
        boolean finishPage = false;
        for(int page = 0; page < numPages; page++){
            //create four buttons and four labels
            roomTables[page] = new Table();
            for(int f = 0; f<4; f++){
                final int roomNum = page*4 + f;
                if(roomNum >= numRooms){
                    finishPage = true;
                    break;
                }
                TextureRegionDrawable roomPreview;
                TextureRegionDrawable roomPreviewDown;
                System.out.println("x,y: ("+ ((wid/5)*f) + ", "+((ht/4)*f+row));
                if(roomNum <= highestRoom){
                    roomPreview = new TextureRegionDrawable(new TextureRegion(bg, (wid/5)*f, (ht/4)*row, wid/5, ht/4));
                    roomPreviewDown = roomPreview;
                    roomPreviewDown.tint(Color.BLACK);
                }else{
                    roomPreview = lockedRoom;
                    roomPreviewDown = lockedRoom;
                }

                Container butCon = new Container();
                Button tempButton = new Button(roomPreview, roomPreviewDown);
                tempButton.addListener(new ClickListener(){
                    @Override
                    public void clicked(InputEvent event, float x, float y) {
                        if(roomNum <= highestRoom){
                            gameManager.setCurrentLevel(levelNum, roomNum);
                            gameManager.getCurrentLevel().setCurrentRoom(roomNum);
                            gameManager.printGameState();
                            stage.addAction(Actions.sequence(Actions.fadeOut(0.5f), Actions.run(new Runnable() {
                                @Override
                                public void run() {
                                    exitToRoom();
                                }
                            })));
                        }
                    }
                });
                butCon.setActor(tempButton);

                int pad = 20;
                roomTables[page].add(tempButton).width(cw/4-pad*2).height(ch*0.8f-pad*2).padLeft(pad).padRight(pad).padTop(pad).expand().center();
            }
            roomTables[page].row();

            row+=1;

            for(int f = 0; f<4; f++){
                final int roomNum = page*4 + f;
                if(roomNum >= numRooms){
                    finishPage = true;
                    break;
                }
                String roomLabel = "Room " + (roomNum+1);
                Label tempLabel = new Label(roomLabel, style);
                tempLabel.setAlignment(Align.center);
                roomTables[page].add(tempLabel).width(cw/4).height(ch*0.2f).center();
            }

            if(finishPage){
                break;
            }

        }

        roomContainer = new Container();
        roomContainer.setActor(roomTables[curPage]);

        prevTexOn = new TextureRegionDrawable(new TextureRegion(new Texture(Gdx.files.internal("textures/gui/roomselect/prev_arrow.png"))));
        prevTexOff = new TextureRegionDrawable(new TextureRegion(new Texture(Gdx.files.internal("textures/gui/roomselect/prev_arrow_off.png"))));

        nextTexOn = new TextureRegionDrawable(new TextureRegion(new Texture(Gdx.files.internal("textures/gui/roomselect/next_arrow.png"))));
        nextTexOff = new TextureRegionDrawable(new TextureRegion(new Texture(Gdx.files.internal("textures/gui/roomselect/next_arrow_off.png"))));

        Table overlayBackButton = new Table();
        //back button
        TextureRegionDrawable bup = new TextureRegionDrawable(new TextureRegion(new Texture(Gdx.files.internal("textures/gui/selectmode/backbutton.png"))));
        Button backButton = new Button(bup);
        backButton.addListener(new ClickListener(){
            @Override
            public void clicked(InputEvent event, float x, float y) {
                stage.addAction(Actions.sequence(Actions.fadeOut(0.5f), Actions.run(new Runnable() {
                    @Override
                    public void run() {
                        exitBack();
                    }
                })));
            }
        });
        overlayBackButton.add(backButton).width(cw/12f).height(cw/15f).expand().bottom().left();

        nextRoomButton = new Button(nextTexOn,nextTexOn,nextTexOff);
        nextRoomButton.addListener(new ClickListener(){
            @Override
            public void clicked(InputEvent event, float x, float y) {

                roomTables[curPage].act(Gdx.graphics.getDeltaTime());
                curPage = Math.min(curPage+1, numPages-1);
                roomContainer.setActor(roomTables[curPage]);
                updateArrows();
            }
        });


        prevRoomButton = new Button(prevTexOn,prevTexOn,prevTexOff);
        prevRoomButton.addListener(new ClickListener(){
            @Override
            public void clicked(InputEvent event, float x, float y) {
              //go previous
                curPage = Math.max(0,(curPage - 1));
                roomContainer.setActor(roomTables[curPage]);
                updateArrows();
            }
        });
        prevRoomButton.setChecked(true);

////        Table backCont = new Table();
//        Container backCont = new Container();
//        backCont.setActor(backButton);
//        backCont.setWidth(cw/20);
        overlayBackButton.align(Align.left);
//        backCont.align(Align.left);
        Container prevCont = new Container();
        prevCont.setActor(prevRoomButton);
        prevCont.setWidth(cw/15);
        prevCont.align(Align.right);
        Container nextCont = new Container();
        nextCont.setActor(nextRoomButton);
        nextCont.setWidth(cw/15);
        nextCont.align(Align.left);
//        backCont.add(backButton).width(cw/13f).height(cw/15f).expand().bottom().left();
        roomContainer.align(Align.center);
        wholescreenTable.setPosition(0,0);
        wholescreenTable.add(roomContainer).padLeft((sw-cw)/2).padRight((sw-cw)/2).padTop((sh-ch)/2).padBottom((sh-ch)/4).center().colspan(3);
        wholescreenTable.row().height((sh-ch)/4);
        wholescreenTable.add(overlayBackButton).width(sw/4);
        wholescreenTable.add(prevCont).width(sw/4).right();
        wholescreenTable.add(nextCont).width(sw/2).left();

        stage.addActor(wholescreenTable);
//        stage.addActor();
    }

    public void updateArrows(){
        if(curPage==numPages-1){
            nextRoomButton.setChecked(true);
        }else{
            nextRoomButton.setChecked(false);
        }
        if(curPage==0){
            prevRoomButton.setChecked(true);
        }else{
            prevRoomButton.setChecked(false);
        }

    }

    @Override
    public void render(float delta) {

        if(active){
            Gdx.gl.glClearColor(0, 0, 0, 1);
            Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);;

            stage.getBatch().setProjectionMatrix(stage.getCamera().combined);
            stage.getCamera().update();

            stage.getBatch().begin();
            stage.getBatch().draw(backgroundTexture, 0, 0, sw, sh);
            stage.getBatch().end();

            stage.act();
            stage.draw();
        }
    }

    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width, height);
        stage.getCamera().viewportWidth = sw;
        stage.getCamera().viewportHeight = sh;
        stage.getCamera().position.set(stage.getCamera().viewportWidth / 2, stage.getCamera().viewportHeight / 2, 0);
        stage.getCamera().update();
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
        stage.clear();
    }

}
