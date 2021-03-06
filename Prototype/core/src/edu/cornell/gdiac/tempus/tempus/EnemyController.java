package edu.cornell.gdiac.tempus.tempus;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.Filter;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import edu.cornell.gdiac.tempus.GameCanvas;
import edu.cornell.gdiac.tempus.WorldController;
import edu.cornell.gdiac.tempus.obstacle.Obstacle;
import edu.cornell.gdiac.tempus.tempus.models.Avatar;
import edu.cornell.gdiac.tempus.tempus.models.Enemy;
import edu.cornell.gdiac.tempus.tempus.models.Platform;
import edu.cornell.gdiac.tempus.tempus.models.Projectile;
import edu.cornell.gdiac.util.JsonAssetManager;
import edu.cornell.gdiac.util.PooledList;
import edu.cornell.gdiac.util.SoundController;

import java.util.Random;

import static edu.cornell.gdiac.tempus.tempus.models.EntityType.PRESENT;

public class EnemyController {

    /** Offset for bullet when firing */
    private static final float BULLET_OFFSET = 1.5f;
    /** The density for a bullet */
    private static final float HEAVY_DENSITY = 10.0f;

    /** All the objects in the world. */
    private PooledList<Obstacle> objects  = new PooledList<Obstacle>();
    /** The enemy being controlled */
    private PooledList<Enemy> enemies  = new PooledList<Enemy>();

    private PooledList<Platform> platformsPast = new PooledList<Platform>();
    private PooledList<Platform> platformsPresent = new PooledList<Platform>();

    /** Target being aimed at */
    private Avatar target;
    /** World */
    private World world;
    /** Scaling of the world */
    private Vector2 scale;
    /** Present or past */
    private boolean shifted;

    private boolean playerVisible;

    /** Frames after enemy teleports */
    private float framesAfterMove;

    /** Cache for internal force calculations */
    private Vector2 forceCache = new Vector2();

    /** The reader to process JSON files */
    private JsonReader jsonReader;
    /** Assets for firing bullets */
    private JsonValue assetDirectory;

    private WorldController worldController;
    private GameCanvas canvas;

    public void setPlayerVisible(boolean b){
        playerVisible = b;
    }

    public boolean getPlayerVisible(){
        return playerVisible;
    }

    public int getEnemies() {
        int result = enemies.size();
        for (Enemy e: enemies) {
            if (e.getY() < -6){
                e.setDead();
                //e.setRespawnQueued(true);
                //e.setPosition(e.getStartPosition());
                if (e.getAi() == Enemy.EnemyType.TELEPORT){
                    if (e.getSpace() == 2) {
                        platformsPast.remove(e.getCurrPlatform());
                    } else if (e.getSpace() ==1){
                        platformsPresent.remove(e.getCurrPlatform());
                    }
                }
            }
            /*if (e.getResawnQueued()){
                if ((shifted == true && e.getSpace() == 1) ||
                        (shifted == false && e.getSpace() == 2)){
                    e.setShiftQueued(true);
                }
            }
            if ((!playerVisible && e.getY() < -6) || (e.getShiftQueued() && ((shifted == true && e.getSpace() == 2) ||
                    (shifted == false && e.getSpace() == 1)))) {
                e.setLinearVelocity(new Vector2(0,0));
                e.setPosition(e.getStartPosition());
                e.setShiftQueued(false);
                e.setRespawnQueued(false);
            }*/
            if (e.isTurret() || e.isDead()){
                result --;
            }
        }
        return result;
    }

    public void removePlat(Platform p, int space){
        if (space == 2) {
            platformsPast.remove(p);
        } else if (space ==1){
            platformsPresent.remove(p);
        }
    }

    public EnemyController(PooledList<Enemy> enemies, PooledList<Obstacle> objects, Avatar target, World world,
                           Vector2 scale, WorldController worldController, JsonValue assetDirectory) {
        this.enemies = enemies;
        for (Obstacle ob : objects) {
            this.objects.add(ob);
        }
        this.target = target;
        this.world = world;
        this.scale = scale;
        this.shifted = false;
        this.worldController = worldController;
        jsonReader = new JsonReader();
        this.assetDirectory = assetDirectory;
        canvas = worldController.getCanvas();
        framesAfterMove = 0;
        playerVisible = false;
    }

    /**
     * Resets the status of the game so that we can play again.
     *
     * This method disposes of the world and creates a new one.
     */
    public void reset() {
        playerVisible = false;
        for (Enemy e: enemies) {
            e.deactivatePhysics(world);
        }
        enemies.clear();
    }

    /**
     * Sets the cooldown for enemies when slowed down
     * @param flag whether the world is slowed down
     */
    public void slowCoolDown(boolean flag) {
        for (Enemy e: enemies) {
            if (e.getAi() != Enemy.EnemyType.TELEPORT) {
                if (flag) {
                    e.setLimiter(1);
                } else {
                    e.setLimiter(4);
                }
            }
        }
    }

    /**
     * Shifts the world
     */
    public void shift() {
        shifted = !shifted;
        for (Enemy e: enemies) {
            if (!e.isTurret() && e.getAi() != Enemy.EnemyType.TELEPORT) {
                e.coolDown(false);
            }
        }
    }

    /**
     * Moves the enemy left and right
     */
    public void applyForce(Enemy e) {
        if (!e.isActive()) {
            return;
        }

        // Don't want to be moving. Damp out player motion
        if (e.getMovement() == 0f) {
            //e.setVX(-1 * e.getVX());
            forceCache.set(-e.getDamping() * e.getVX(), 0);
            e.getBody().applyForce(forceCache,e.getPosition(),true);
        }

        // Velocity too high, clamp it
        if (Math.abs(e.getVX()) >= e.getMaxSpeed()) {
            e.setVX(Math.signum(e.getVX()) * e.getMaxSpeed());
        } else {
            forceCache.set(e.getMovement(),0);
            e.getBody().applyForce(forceCache,e.getPosition(),true);
        }
    }

    /**
     * Sets the velocity of the bullet to aim at the target
     *
     * @param offset float of the offset from the enemy's center that bullet shoots from
     */
    public void setBulletVelocity(float offset, Enemy enemy) {
        Vector2 projVel = target.getPosition().sub(enemy.getPosition());
        projVel.y -= offset;
        enemy.setProjVel(projVel.nor().scl(8));
    }

    /**
     * Set the velocity of a flying enemy
     *
     * @param enemy the enemy whose flying velocity needs to be set
     */
    public void setFlyingVelocity (Enemy enemy, Vector2 location) {
        Vector2 vel = location.cpy().sub(enemy.getPosition().cpy());
        if (vel != enemy.getFlyingVelocity()) {
            enemy.setLinearVelocity(enemy.getLinearVelocity().scl(.7f));
            enemy.setFlyingVelocity(vel.cpy());
        }
    }

    /**
     * Processes actions for the enemy during every update
     */
    public void processAction() {
        for (Enemy e: enemies) {
            if (e.isTurret()) {
                fire(e);
            } else if ((!shifted && e.getSpace() == 1) || (shifted && e.getSpace() == 2)) {
                if (e.getAi() == Enemy.EnemyType.WALK) {
                    if (e.getPlatformFixture() != null){
                        createLineOfSight(world, BULLET_OFFSET, e);
                        applyForce(e);
                        if (playerVisible) {
                            setBulletVelocity(BULLET_OFFSET, e);
                            fire(e);
                        }
                    }
                } else if (e.getAi() == Enemy.EnemyType.TELEPORT) {
                    e.coolDown(true);
                    //System.out.println("frames after move: " + framesAfterMove);
                    //System.out.println("waiting to fire" + e.getWaitToFire());
                    if (framesAfterMove >= 59 && e.getWaitToFire()) {
                        if (playerVisible) {
                            setBulletVelocity(BULLET_OFFSET, e);
                            createBullet(e);
                            e.setWaitToFire(false);
                        }
                    }
                    findPlatform(e);
                } else if (e.getAi() == Enemy.EnemyType.GUN) {
                    if(playerVisible) {
                        createLineOfSight(world, BULLET_OFFSET, e);
                        setBulletVelocity(BULLET_OFFSET, e);
                        fire(e);
                    }
                } else {
                    if (playerVisible) {
                        createLineOfSight(world, BULLET_OFFSET, e);
                        setFlyingVelocity(e, target.getPosition());
                        setBulletVelocity(BULLET_OFFSET, e);
                        fly(e);
                        fire(e);
                    } else {
//                        stopFlying(e);
                        setFlyingVelocity(e, e.getStartPosition());
                        fly(e);
                    }
                }
            }
        }
    }

    /**
     * Stops the enemy from flying
     *
     * @param e enemy that needs to be stopped
     */
    public void stopFlying(Enemy e) {
        e.setVX(e.getVX() * 0.9f);
        e.setVY(e.getVY() * 0.9f);
    }

    /**
     * Applies force to flying enemies to make them fly
     *
     * @param e enemy that is flying
     */
    public void fly(Enemy e) {
        // Velocity too high, clamp it
        if (Math.abs(e.getVX()) >= e.getMaxSpeed()) {
            e.setVX(Math.signum(e.getVX()) * e.getMaxSpeed());
        }
        if (Math.abs(e.getVY()) >= e.getMaxSpeed()) {
            e.setVY(Math.signum(e.getVY()) * e.getMaxSpeed());
        }
        forceCache.set(e.getFlyingVelocity());
        e.getBody().applyForce(forceCache, e.getPosition(), true);
    }

    /**
     * Finds an available platform for the enemy to teleport to
     * Will only teleport to a platform where it can shoot the avatar and the avatar is not currently on
     *
     * @param e enemy that is teleporting
     */
    public void findPlatform(Enemy e) {
        for (int i = 0; i < objects.size(); i++) {
            Obstacle ob = objects.get(i);
            if (ob instanceof Platform &&
                    ((!shifted && ob.getSpace() == 1) || (shifted && ob.getSpace() == 2) || ob.getSpace() == 3)) {
                Platform p = (Platform) ob;
                if (!p.getName().contains("pillar") && !p.getName().contains("tall") && !p.getName().contains("longcapsule")) {
                    e.setCheckSight(true);
                    teleportLineOfSight(p, e);
                    if (e.getTeleportTo() != null && e.canFire()
                            && e.getTeleportTo().getBody().getUserData() != target.getCurrentPlatform()
                            && e.getCurrPlatform() != e.getTeleportTo()) {
                        if (e.getSpace() == 2 && !platformsPast.contains(p) || e.getSpace() == 1 && !platformsPresent.contains(p)){
                            teleport(e, p);
                            objects.remove(i);
                            objects.add(ob);
                            return;
                        }
                    }
                }
            }
        }
    }

    /**
     * Moves the enemy to the platform and fire a bullet at the avatar
     *
     * @param e the enemy that is teleporting and firing
     * @param p the platform the enemy teleports to
     */
    public void teleport(Enemy e, Platform p) {
        JsonValue disappear = assetDirectory.get("sounds").get("teleport_disappear");
        SoundController.getInstance().play(disappear.get("file").asString(), disappear.get("file").asString(),
                false, disappear.get("volume").asFloat());
        Vector2 newPos = p.getPosition();
        newPos.y += p.getHeight() + (e.getHeight() / 2);
        newPos.x += p.getWidth() / 2;
        e.setPosition(newPos);
        if (e.getSpace() == 2) {
            platformsPast.remove(e.getCurrPlatform());
            platformsPast.add(p);
        } else if (e.getSpace() ==1){
            platformsPresent.remove(e.getCurrPlatform());
            platformsPresent.add(p);
        }
        e.setCurrPlatform(p);
        e.setTeleportTo(null);
        e.coolDown(false);
        framesAfterMove = 1;
        e.setWaitToFire(true);
    }

    /**
     * Fires a projectile at the avatar
     *
     * @param e enemy that is firing
     */
    public void fire(Enemy e) {
        if (e.canFire() && !e.getResawnQueued()) {
            createBullet(e);
        } else {
            e.coolDown(true);
        }
    }

    /**
     * Add a new bullet to the world and send it in the right direction.
     *
     * @param enemy enemy
     */
    private void createBullet(Enemy enemy) {
        float offset = BULLET_OFFSET;
        if (enemy.getAi() == Enemy.EnemyType.FLY && enemy.getSpace() == 2) {
            offset = offset / 2;
        }

        if (enemy.isTurret()){
            if (enemy.getProjVel().y < 0 && enemy.getProjVel().x == 0){
                offset = offset * -1;
            }
        }

        TextureRegion bulletBigTexture = JsonAssetManager.getInstance().getEntry("bulletbig", TextureRegion.class);
        TextureRegion presentBullet = JsonAssetManager.getInstance().getEntry("projpresent", TextureRegion.class);
        TextureRegion pastbullet = JsonAssetManager.getInstance().getEntry("projpast", TextureRegion.class);
        float radius = bulletBigTexture.getRegionWidth() / (30.0f);
        Projectile bullet = new Projectile(enemy.getType(), enemy.getX(), enemy.getY() + offset, radius,
                enemy.getBody().getUserData());

        Filter f = new Filter();
        Random random = new Random();
        f.groupIndex = (short) -random.nextInt(Short.MAX_VALUE + 1);
        for (Fixture fix: enemy.getFixtures()) {
            if (fix.getUserData() == null) {
                fix.setFilterData(f);
            }
        }
//        enemy.setFilterData(f);
        bullet.setFilterData(f);

        bullet.setName("bullet");
        bullet.setDensity(HEAVY_DENSITY);
        bullet.setDrawScale(scale);
        //bullet.setTexture(bulletBigTexture);
        bullet.setBullet(true);
        bullet.setGravityScale(0);

        //TODO: Either use later or remove
        //Differentiate between how enemy types shoot
//        if (enemy.getAi() == SPIRAL) {
//            bullet.setLinearVelocity(enemy.getProjVel());
//        } else {
//            bullet.setLinearVelocity(enemy.getProjVel());
//        }

        bullet.setLinearVelocity(enemy.getProjVel());
        bullet.setSpace(enemy.getSpace());
        worldController.addQueuedObject(bullet);
        if (bullet.getType().equals(PRESENT)){
            bullet.setTexture(presentBullet);
        } else {
            bullet.setTexture(pastbullet);
        }
        if (shifted && enemy.getSpace() == 2) { // past world
            JsonValue data = assetDirectory.get("sounds").get("pew_past");
            SoundController.getInstance().play("pew", data.get("file").asString(), false, data.get("volume").asFloat());
        } else if (!shifted && enemy.getSpace() == 1) { // present world
            JsonValue data = assetDirectory.get("sounds").get("pew_present");
            SoundController.getInstance().play("pew", data.get("file").asString(), false, data.get("volume").asFloat());
        }


        //Note: uncomment to see spiral effect
        // Change proj angle
//        if (enemy.isTurret()) { //TODO: Change condition
//            float thetaRad = (float)Math.toRadians(30);
//            enemy.changeProjAngle(thetaRad);
//        }

        // Reset the firing cooldown.
        enemy.coolDown(false);
    }

    /**
     * Updates the enemies line of sight to check if it can see the target
     * @param world
     * @param offset offset from the enemy center to where the bullet shoots from
     */
    public void createLineOfSight(World world, float offset, Enemy e) {
        e.setCheckSight(true);
        Vector2 shootPos = e.getPosition().add(0f, offset);
        TextureRegion bulletBigTexture = JsonAssetManager.getInstance().getEntry("bulletbig", TextureRegion.class);
        float radius = bulletBigTexture.getRegionWidth() / scale.x;
        world.rayCast(e.getSight(), shootPos.cpy().add(0f, -radius), target.getPosition().add(0f, -radius));
        world.rayCast(e.getSight(), shootPos.cpy().add(0f, radius), target.getPosition().add(0f, radius));
        world.rayCast(e.getSight(), shootPos.cpy().add(radius, 0f), target.getPosition().add(radius, 0f));
        world.rayCast(e.getSight(), shootPos.cpy().add(-radius, 0f), target.getPosition().add(-radius, 0f));

        if (e.getCheckSight() && playerVisible) {
            e.setIsFiring(true);
            e.setShiftedFiring(true);
            /*if (e.getAi() == Enemy.EnemyType.WALK) {
                e.setMovement(0);
            }*/
        } else {
            e.setIsFiring(false);
            e.setShiftedFiring(false);
        }
        if (e.getAi() == Enemy.EnemyType.WALK && e.getMovement() == 0) {
            e.setMovement(e.getNextDirection());
        }
    }

    /**
     * Checks the line of sight at a potential place to teleport to
     *
     * @param p platform the enemy could potentially teleport to
     * @param e enemy that is teleporting
     */
    public void teleportLineOfSight(Platform p, Enemy e) {
        e.setTeleportTo(p);
        Vector2 aim = p.getPosition();
        aim.y += e.getHeight() / 2 + p.getHeight() / 2 + BULLET_OFFSET;
        aim.x += p.getWidth() / 2;
        world.rayCast(e.getSight(), aim, target.getPosition());

        if (e.getCheckSight()) {
            e.setIsFiring(true);
            e.setShiftedFiring(true);
        } else {
            e.setTeleportTo(null);
            e.setIsFiring(false);
            e.setShiftedFiring(false);
        }
    }

    /**
     * Makes the enemies inactive if they are not in the world
     */
    public void sleepIfNotInWorld() {
        for (Enemy e: enemies) {
            if (!e.isTurret()) {
                e.setBodyType(BodyDef.BodyType.DynamicBody);
                //e.setActive(true);
                if (e.getSpace() == 3) {
                    e.setIsFiring(true);
                    e.setBodyType(BodyDef.BodyType.DynamicBody);
                } else if (!shifted && (e.getSpace() == 2)) {
                    e.setIsFiring(false);
                    e.setBodyType(BodyDef.BodyType.StaticBody);
                    //e.setActive(false);
                } else if (shifted && (e.getSpace() == 2)) {
                    e.setIsFiring(e.getShiftedFiring());
                } else if (shifted && (e.getSpace() == 1)) {
                    e.setIsFiring(false);
                    e.setBodyType(BodyDef.BodyType.StaticBody);
                    //e.setActive(false);
                } else if (!shifted && (e.getSpace() == 1)) {
                    e.setIsFiring(e.getShiftedFiring());
                }
            }
            e.setSensor(false);
            if (e.getSpace() == 3) {
                e.setSensor(false);
            } else if (!shifted && (e.getSpace() == 2)) {
                e.setSensor(true);
            } else if (shifted && (e.getSpace() == 1)) {
                e.setSensor(true);
            }
        }
    }

    //Update enemy animation state
    public void animateEnemies() {
        for (Enemy e: enemies) {
            if (e.getAi() != Enemy.EnemyType.TELEPORT) {
                e.animate(Enemy.EnemyState.NEUTRAL, true);
                e.setAnimationState(Enemy.EnemyState.NEUTRAL);
            }
        }
    }

    public void drawEnemiesInWorld() {
        for (Enemy e: enemies) {
            if (e.isDead()) {
                e.decRemovalFrames();
                if (e.getRemovalFrames() == 0) {
                    e.markRemoved(true);
                } else if ((shifted && (e.getSpace() == 2)) || (!shifted && (e.getSpace() == 1))) {
                    if (e.getRemovalFrames() > 0) {
                        if (e.getRemovalFrames() < 30) {
                            e.drawFade(canvas, e.getRemovalFrames(), 1f / 30f);
                        } else if (e.getRemovalFrames() < 45) {
                            e.drawFade(canvas, 45 - e.getRemovalFrames(), 1f / 15f);
                        } else if (e.getRemovalFrames() < 60) {
                            e.drawFade(canvas, e.getRemovalFrames() - 45, 1f / 15f);
                        }
                    }
                }
            } else if (e.getSpace() == 3) {
                e.draw(canvas);
            } else if ((shifted && (e.getSpace() == 2)) || (!shifted && (e.getSpace() == 1))) { // past world
                if (e.getAi() == Enemy.EnemyType.FLY && !playerVisible) {
                    e.setFaceDirection((e.getX() - e.getStartPosition().x < 0 ? 1 : -1));
                } else if (e.getAi() != Enemy.EnemyType.WALK) {
                    e.setFaceDirection((e.getX() - target.getX()) < 0 ? 1 : -1);
                }
                if (e.getAi() == Enemy.EnemyType.TELEPORT) {
                    if (e.getTeleportTo() != null && e.getFramesTillFire() < 60) {
                        e.animate(Enemy.EnemyState.ATTACKING, false);
                        e.setAnimationState(Enemy.EnemyState.ATTACKING);
                        e.drawFade(canvas, e.getFramesTillFire(), 1f / 60f);
                    } else if (framesAfterMove > 0 && framesAfterMove < 60) {
                        e.animate(Enemy.EnemyState.ATTACKING, false);
                        e.setAnimationState(Enemy.EnemyState.ATTACKING);
                        e.drawFade(canvas, framesAfterMove, 1f / 60f);
                        framesAfterMove += 1;
                    } else {
                        e.animate(Enemy.EnemyState.NEUTRAL, true);
                        e.setAnimationState(Enemy.EnemyState.NEUTRAL);
                        e.draw(canvas, playerVisible);
                    }
                } else {
                    e.animate(Enemy.EnemyState.NEUTRAL, true);
                    e.setAnimationState(Enemy.EnemyState.NEUTRAL);
                    e.draw(canvas, playerVisible);
                }
//            } else if (!shifted && (e.getSpace() == 1)) { // present world
//                e.draw(canvas);
            }
        }
    }

    /**
     * Draws the debug for enemies in the world
     */
    public void drawEnemiesDebugInWorld() {
        for (Enemy e: enemies) {
            if (e.getSpace() == 3) {
                e.drawDebug(canvas);
            } else if (shifted && (e.getSpace() == 2)) {
                e.drawDebug(canvas);
            } else if (!shifted && (e.getSpace() == 1)) {
                e.drawDebug(canvas);
            }
        }
    }
}
