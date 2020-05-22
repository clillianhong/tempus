package edu.cornell.gdiac.tempus.tempus;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.JsonValue;
import edu.cornell.gdiac.tempus.tempus.models.*;
import edu.cornell.gdiac.util.JsonAssetManager;

public class EndGameController extends LevelController{

    /**
     * Creates and initialize a new instance of the platformer game
     * <p>
     * The game has default gravity and other settings
     *
     * @param json
     */
    public EndGameController(String json) {
        super(json);
        this.bounds.height = DEFAULT_HEIGHT * 3;
    }

    @Override
    public void reset() {
        super.reset();
        levelFormat = jsonReader.parse(Gdx.files.internal("jsons/levels/endgame.json"));

    }

    @Override
    public void render(float delta) {
        canvas.updateSpriteBatch();
        updateCamera(sh, 2*sh);
        stage.getCamera().update();
        super.render(delta);
    }

    @Override
    public void draw(float delta) {
        super.draw(delta);
        if(complete && !failed && !drawEndRoom){

        }
    }

    @Override
    protected void populateLevel() {
        // tester stage!
        skin = new Skin(Gdx.files.internal("jsons/uiskin.json"));
        stage = new Stage(viewport);
        stage.setViewport(hudViewport);
        Gdx.input.setInputProcessor(stage);//BUGGY CHANGE?
        table = new Table();
        table.setWidth(stage.getWidth());
        table.align(Align.center | Align.top);
        table.setPosition(0, sh);

        //initialize backgrounds
        pastBackgroundTexture = JsonAssetManager.getInstance().getEntry(levelFormat.get("past_background").asString(),
                TextureRegion.class);
        presentBackgroundTexture = JsonAssetManager.getInstance().getEntry(levelFormat.get("present_background").asString(),
                TextureRegion.class);
        bgSprite = new Sprite(presentBackgroundTexture);

//		win_room = new TextureRegion(new Texture(Gdx.files.local("textures/background/blackscreen.png")));
        createUI();

        // Initializes the world
        float gravity = levelFormat.getFloat("gravity");
        float[] pSize = levelFormat.get("bounds").asFloatArray();
        world = new World(new Vector2(0, gravity), false);
        bounds = new Rectangle(0, 0, pSize[0], pSize[1]);
        scale.x = canvas.getWidth() / DEFAULT_WIDTH;
        scale.y = canvas.getHeight() / DEFAULT_HEIGHT;
        // Add level goal
        goalDoor = new Door();
        goalDoor.initialize(levelFormat.get("door"));

        goalDoor.setDrawScale(scale);
        addObject(goalDoor);

//		earthTile = JsonAssetManager.getInstance().getEntry("earth", TextureRegion.class);

        float[] newPlatCapsule = {0.5f, 1.1f, 0.6f, 1.1f, 2.4f, 1.1f, 2.6f, 1.1f, 2.6f, 0.6f, 2.0f, 0.3f, 1.1f, 0.3f, 0.5f, 0.6f};
        float[] newPlatDiamond = {0.4f, 1.8f, 0.5f, 1.8f, 2.0f, 1.8f, 2.2f, 1.8f, 1.4f, 0.1f};
        float[] newPlatRounded = {0.4f, 1.4f, 0.8f, 1.7f, 2.1f, 1.7f, 2.4f, 1.4f, 2.3f, 0.8f, 1.7f, 0.3f, 1.1f, 0.3f};
        float[] newSpikes = {0.3f, -0.6f, 0.0f, -0.2f, -0.6f, 0.0f, -0.5f, 0.4f, 0.0f, 0.6f, 0.4f, -0.2f, 0.6f, -0.3f};
        float[] newPlatLongcapsule = {0.5f, 1.1f, 0.6f, 1.1f, 4.7f, 1.1f, 4.9f, 1.1f, 4.9f, 0.6f, 4.3f, 0.3f, 3.4f, 0.3f,
                2.7f, 0.5f, 2.0f, 0.3f, 1.1f, 0.3f, 0.5f, 0.6f};
        float[] newPlatTall = {0.4f, 3.9f, 0.5f, 3.9f, 1.6f, 3.9f, 1.7f, 3.9f, 1.1f, 0.5f};
        float[] newPlatPillar = {1.2f, 4.0f, 1.3f, 4.0f, 2.0f, 4.0f, 2.1f, 4.0f, 2.1f, 1.0f, 1.2f, 1.0f};


        JsonValue capsule = levelFormat.get("capsules").child();
        while (capsule != null) {
            Platform obj = new Platform(newPlatCapsule);
            obj.initialize(capsule);
            obj.setDrawScale(scale);
            addObject(obj);
            capsule = capsule.next();
        }

        JsonValue longcapsule = levelFormat.get("longcapsules").child();
        while (longcapsule != null) {
            Platform obj = new Platform(newPlatLongcapsule);
            obj.initialize(longcapsule);
            obj.setDrawScale(scale);
            addObject(obj);
            longcapsule = longcapsule.next();
        }

        JsonValue pillar = levelFormat.get("pillars").child();
        while (pillar != null) {
            Platform obj = new Platform(newPlatPillar);
            obj.initialize(pillar);
            obj.setDrawScale(scale);
            addObject(obj);
            pillar = pillar.next();
        }

        JsonValue tall = levelFormat.get("talls").child();
        while (tall != null) {
            Platform obj = new Platform(newPlatTall);
            obj.initialize(tall);
            obj.setDrawScale(scale);
            addObject(obj);
            tall = tall.next();
        }
        JsonValue diamond = levelFormat.get("diamonds").child();
        while (diamond != null) {
            Platform obj = new Platform(newPlatDiamond);
            obj.initialize(diamond);
            obj.setDrawScale(scale);
            addObject(obj);
            diamond = diamond.next();
        }
        JsonValue round = levelFormat.get("rounds").child();
        while (round != null) {
            Platform obj = new Platform(newPlatRounded);
            obj.initialize(round);
            obj.setDrawScale(scale);
            addObject(obj);
            round = round.next();
        }
        JsonValue spikes = levelFormat.get("spikes").child();
        while (spikes != null) {
            Spikes obj = new Spikes(newSpikes);
            obj.initialize(spikes);
            obj.setDrawScale(scale);
            addObject(obj);
            spikes = spikes.next();
        }
        // Create avatar
        JsonValue json = levelFormat.get("avatar");
        avatar = new Avatar();
        avatar.setCanvas(camera);
        avatar.setDrawScale(scale);
        //avatar.setScale(scale);
        avatar.initialize(json);
        addObject(avatar);
        float[] pos = json.get("pos").asFloatArray();
        avatarStart = new Vector2(pos[0], pos[1]);

        JsonValue enemy = levelFormat.get("enemies").child();
        while (enemy != null) {
            Enemy obj = new Enemy(avatar, enemy);
            obj.setDrawScale(scale);
            addEnemy(obj);
            numEnemies++;
            enemy = enemy.next();
        }

        JsonValue turret = levelFormat.get("turrets").child();
        while (turret != null) {
            Enemy obj = new Enemy(turret);
            obj.setDrawScale(scale);
            addEnemy(obj);
            turret = turret.next();
        }

        collisionController = new CollisionController(this);
        enemyController = new EnemyController(enemies, objects, avatar, world, scale, this, assetDirectory);
        world.setContactListener(collisionController);

    }

    public void updateCamera(float bottomCiel, float topFloor){
        Vector3 screenAvatarPos =new Vector3(camera.position.x, camera.position.y + (avatar.getY()*scale.y - camera.position.y) * 0.08f, 0);

        if(screenAvatarPos.y < bottomCiel/2) {
            camera.position.y = camera.position.y + ( bottomCiel/2 - camera.position.y) * 0.02f;
        }
        else if(screenAvatarPos.y > topFloor){
            camera.position.y = camera.position.y + ( topFloor - camera.position.y) * 0.02f;
        }
        else{
            camera.position.set(screenAvatarPos);
        }
//        gameCam.unproject(new Vector3(avatar.getX(), avatar.getY(), 0));
        camera.update();
    }
}
