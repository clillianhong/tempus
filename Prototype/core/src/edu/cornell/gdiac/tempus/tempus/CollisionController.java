package edu.cornell.gdiac.tempus.tempus;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;
import com.badlogic.gdx.utils.ObjectMap;
import com.badlogic.gdx.utils.ObjectSet;
import edu.cornell.gdiac.tempus.InputController;
import edu.cornell.gdiac.tempus.obstacle.Obstacle;
import edu.cornell.gdiac.tempus.obstacle.SimpleObstacle;
import edu.cornell.gdiac.tempus.tempus.models.*;
import edu.cornell.gdiac.util.PooledList;

public class CollisionController implements ContactListener {
    private LevelController controller;
    private ObjectMap<Short, ObjectMap<Short, ContactListener>> listeners;
    private Avatar avatar;
//    private PooledList<Obstacle> obstacles;
//    private PooledList<Enemy> enemies;
    private float cur_normal;
    private CollisionPair prevCollisionPair;
    /**
     * Mark set to handle more sophisticated collision callbacks
     */
    protected ObjectSet<Fixture> sensorFixtures;

    /**
     * CURRENTLY UNUSED
     * helper class that uses comparable to determine if collision events are happening between the same two bodies
     */
    class CollisionPair implements Comparable {

        private Obstacle A;
        private Obstacle B;

        public CollisionPair(Obstacle a, Obstacle b) {
            A = a;
            B = b;
        }

        public CollisionPair() {
            //do nothing
        }

        public Obstacle getA() {
            return A;
        }

        public Obstacle getB() {
            return B;
        }

        @Override
        public int compareTo(Object o) {
            CollisionPair comp = (CollisionPair) o;
            if (comp.getA() == null) {
                return -1;
            }
            if ((comp.getA() == this.getA() && comp.getB() == this.getB()) ||
                    (comp.getB() == this.getA() && comp.getA() == this.getB())) {
                return 0;
            }
            return -1;
        }
    }

    /**
     * Creates instance of the collision handler
     *
     * @param w
     */
    public CollisionController(LevelController w) {
        controller = w;
        avatar = w.getAvatar();
//        obstacles = w.getObjects();
//        enemies = w.getEnemies();
        cur_normal = 0;
        listeners = new ObjectMap<Short, ObjectMap<Short, ContactListener>>();
        sensorFixtures = new ObjectSet<Fixture>();
        prevCollisionPair = new CollisionPair();
    }

    /**
     * Process all contacts in the game
     *
     * @param contact is a detected contact with a sensor
     */
    private void processContact(Contact contact) {
        Fixture fixA = contact.getFixtureA();
        Fixture fixB = contact.getFixtureB();

        Object objA = fixA.getBody().getUserData();
        Object objB = fixB.getBody().getUserData();

        //avatar-projectile
        if ((objA instanceof Avatar) && (objB instanceof Projectile)) {
            processAvatarProjectileContact(fixA, fixB);
        } else if ((objB instanceof Avatar) && (objA instanceof Projectile)) {
            processAvatarProjectileContact(fixB, fixA);
        }
        //avatar-platform
        else if ((objA instanceof Avatar) && (objB instanceof Platform)) {
            processAvatarPlatformContact(fixA, fixB);
        } else if ((objB instanceof Avatar) && (objA instanceof Platform)) {
            processAvatarPlatformContact(fixB, fixA);
        }
        //avatar-spikes
        else if ((objA instanceof Avatar) && (objB instanceof Spikes)) {
            processAvatarSpikesContact(fixA, fixB, contact);
        } else if ((objB instanceof Avatar) && (objA instanceof Spikes)) {
            processAvatarSpikesContact(fixB, fixA, contact);
        }
        //avatar-turret
        else if ((objA instanceof Avatar) && (objB instanceof Enemy)) {
            processAvatarEnemyContact(fixA, fixB, contact);
        } else if ((objB instanceof Avatar) && (objA instanceof Enemy)) {
            processAvatarEnemyContact(fixB, fixA, contact);
        }
        //avatar-door
        else if ((objA instanceof Avatar) && (objB instanceof Door)) {
            processAvatarDoorContact(fixA, fixB);
        } else if ((objB instanceof Avatar) && (objA instanceof Door)) {
            processAvatarDoorContact(fixB, fixA);
        }
        //avatar-projectile
        else if ((objA instanceof Projectile) && (objB instanceof Enemy)) {
            processProjEnemyContact(fixA, fixB);
        } else if ((objB instanceof Projectile) && (objA instanceof Enemy)) {
            processProjEnemyContact(fixB, fixA);
        }
        //TODO: model classes for platforms, projectiles
        //TODO: create delegation of all contacts
    }

    private void processAvatarPlatformContact(Fixture av, Fixture platform) {
        //TODO: avatar platform contact
    }

    private void processAvatarSpikesContact(Fixture av, Fixture spike, Contact contact) {
        Float norm_angle = contact.getWorldManifold().getNormal().angle();

        if (!norm_angle.isNaN()) {
                        /*if ((norm_angle.intValue()) == 0){
                            System.out.println("Fekwbfiewq");
                            cur_normal = (float) Math.toRadians(90);
                        } else {*/
            cur_normal = (float) Math.toRadians(norm_angle + 180);
            //}
        }
        Vector2 bounceDir = new Vector2(5, 5).setAngleRad(cur_normal);
        if (spike.getBody().getPosition().x > avatar.getX()){
            if (bounceDir.x > 0){
                bounceDir.x = -1 * bounceDir.x;
            }
        } else {
            if (bounceDir.x < 0){
                bounceDir.x = -1 * bounceDir.x;
            }
        }
        avatar.resetDashNum(1);
        avatar.setEnemyContact(true);
        avatar.setLinearVelocity(bounceDir);
    }

    private void processAvatarEnemyContact(Fixture av, Fixture turret, Contact contact) {
            Float norm_angle = contact.getWorldManifold().getNormal().angle();

            if (!norm_angle.isNaN()) {
                        /*if ((norm_angle.intValue()) == 0){
                            System.out.println("Fekwbfiewq");
                            cur_normal = (float) Math.toRadians(90);
                        } else {*/
                cur_normal = (float) Math.toRadians(norm_angle + 180);
                //}
            }
            Vector2 bounceDir = new Vector2(5, 5).setAngleRad(cur_normal);
            if (turret.getBody().getPosition().x > avatar.getX()){
                if (bounceDir.x > 0){
                    bounceDir.x = -1 * bounceDir.x;
                }
            } else {
                if (bounceDir.x < 0){
                    bounceDir.x = -1 * bounceDir.x;
                }
            }
            avatar.resetDashNum(1);
            if (!(((Enemy) turret.getBody().getUserData()).isDead())) {
                avatar.setEnemyContact(true);
            }
            avatar.setLinearVelocity(bounceDir);
            turret.getBody().setLinearVelocity(new Vector2(0, 0));
            //avatar.getBody().applyForce(new Vector2(-20, 40), avatar.getPosition(), true);
            //TODO: avatar turret contact (die)
        }

    private void processAvatarProjectileContact(Fixture av, Fixture projectile) {
        if (!avatar.isHolding() && !avatar.isSticking() && InputController.getInstance().pressedRightMouseButton()) {
            Obstacle bullet = (Obstacle) projectile.getBody().getUserData();
            if (bullet.getSpace() == 2 && controller.isShifted()) {
                avatar.setHolding(true);
                avatar.setHeldBullet((Projectile) bullet);
                removeBullet(bullet);
            } else if (bullet.getSpace() == 1 && !controller.isShifted()){
                avatar.setHolding(true);
                avatar.setHeldBullet((Projectile) bullet);
                removeBullet(bullet);
            }
            // //bullet.markRemoved(true);
            //bullet.setPosition(avatar.getPosition());
            // avatar.setHeldBullet(bullet);
//            projectile.getBody().setType(BodyDef.BodyType.StaticBody);
        } else {
            Obstacle bullet = (Obstacle) projectile.getBody().getUserData();
            if (bullet.getSpace() == 2 && controller.isShifted()) {
                    if (!bullet.equals(avatar.getHeldBullet())) {
                        avatar.removeLife();
                        //avatar.setProjectileContact(true);
                    }
                removeBullet(bullet);
            } else if (bullet.getSpace() == 1 && !controller.isShifted()){
                    if (!bullet.equals(avatar.getHeldBullet())) {
                        avatar.removeLife();
                        //avatar.setProjectileContact(true);
                    }
                removeBullet(bullet);
            }
            /*else if (avatar.isHolding() && InputController.getInstance().releasedLeftMouseButton()){
            Vector2 mousePos = InputController.getInstance().getMousePosition();
            avatar.setBodyType(BodyDef.BodyType.DynamicBody);
            avatar.setSticking(false);
            avatar.setWasSticking(false);
            avatar.setDashing(true);
            avatar.setDashStartPos(avatar.getPosition().cpy());
            avatar.setDashDistance(avatar.getDashRange());
            avatar.setDashForceDirection(mousePos.cpy().sub(avatar.getPosition()));
            Obstacle obj = avatar.getHeldBullet();
            obj.setBodyType(BodyDef.BodyType.DynamicBody);
            Vector2 bulletRedirection = avatar.getPosition().cpy().sub(mousePos).nor();
            obj.setPosition(obj.getPosition().add(bulletRedirection.cpy()));
            obj.setLinearVelocity(bulletRedirection.cpy().scl(12.0f));
            avatar.setHolding(false);
            avatar.setHeldBullet(null);
        }*/
        }
    }

    private void processAvatarDoorContact(Fixture av, Fixture door) {
        //TODO: avatar door contact
    }

    private void processProjPlatformContact(Fixture projectile, Fixture platform) {
        //TODO: platform projectile contact
    }

    private void processProjEnemyContact(Fixture projectile, Fixture enemy) {
        Body projBody = projectile.getBody();
        Projectile proj = (Projectile) projBody.getUserData();
        Body enemyBody = enemy.getBody();
        Enemy e = (Enemy) enemyBody.getUserData();
//        System.out.println("proj type: " + proj.getType());
//        System.out.println("enemy type: " + e.getType());

        // if projectile and enemy are of different worlds
        // then remove enemy
        if (proj.getType() != e.getType() && !e.getName().equals("turret")) {
            Enemy obs = (Enemy) enemyBody.getUserData();
            obs.setDead();
        }

        if (e.getBody().getUserData() != proj.getSourceData()) {
            removeBullet(proj);
        }
    }

    private void processProjProjContact(Fixture projectile1, Fixture projectile2) {
        //TODO: projectile projectile contact
    }

    /**
     * Remove a new bullet from the world.
     *
     * @param bullet the bullet to remove
     */
    public void removeBullet(Obstacle bullet) {
        bullet.markRemoved(true);
        /*
        TODO: implement AssetManager
         */
    }

    /**
     * Callback method for the start of a collision
     * <p>
     * This method is called when we first get a collision between two objects.  We use
     * this method to test if it is the "right" kind of collision.  In particular, we
     * use it to test if we made it to the win door.
     *
     * @param contact The two bodies that collided
     */
    @Override
    public void beginContact(Contact contact) {

        Fixture fix1 = contact.getFixtureA();
        Fixture fix2 = contact.getFixtureB();

        Object fd1 = fix1.getUserData();
        Object fd2 = fix2.getUserData();

        Object objA = fix1.getBody().getUserData();
        Object objB = fix2.getBody().getUserData();


        if (((objB instanceof Platform)) || ((objA instanceof Platform))) {
            //if(avatar.getCurrentPlatform() != objB && avatar.getCurrentPlatform() != objA) {
            boolean correctWorld = false;
            if (objB instanceof Platform) {
                if (((Platform) objB).getSpace() == 3) {
                    correctWorld = true;
                } else if (controller.getShifted()) {
                    if (((Platform) objB).getSpace() == 2) {
                        correctWorld = true;
                    }
                } else {
                    if (((Platform) objB).getSpace() == 1) {
                        correctWorld = true;
                    }
                }
            } else {
                if (((Platform) objA).getSpace() == 3) {
                    correctWorld = true;
                } else if (controller.getShifted()) {
                    if (((Platform) objA).getSpace() == 2) {
                        correctWorld = true;
                    }
                } else {
                    if (((Platform) objA).getSpace() == 1) {
                        correctWorld = true;
                    }
                }
            }
            if (!correctWorld && !(objA instanceof Projectile) && !(objB instanceof Projectile)) {
                return;
            }
        }
        try {
            Obstacle bd1 = (Obstacle) objA;
            Obstacle bd2 = (Obstacle) objB;

            // Test bullet collision with world
            if (bd1.getSpace() != 3 && bd2.getSpace() != 3 && bd1.getSpace() != bd2.getSpace()) {
                    return;
            }
            if (bd1.getName().equals("bullet") && bd2 != avatar) {
                if (bd2.getBody().getUserData() instanceof Enemy) {
                    processProjEnemyContact(fix1, fix2);
                } else if (!bd2.getName().equals("bullet")) {
                    removeBullet(bd1);
                }
            } else if ((bd1.getName().equals("bullet") && bd2 == avatar)) {
                processAvatarProjectileContact(fix2, fix1);
            }
            if (bd2.getName().equals("bullet") && bd1 != avatar) {
                if (bd1.getBody().getUserData() instanceof Enemy) {
                    processProjEnemyContact(fix2, fix1);
                } else if (!bd1.getName().equals("bullet")) {
                    removeBullet(bd2);
                }
            } else if (bd2.getName().equals("bullet") && bd1 == avatar) {
                processAvatarProjectileContact(fix1, fix2);
            }

            //handle platform-avatar collisions first (outside of processcontact
            if (((objA instanceof Avatar) && (objB instanceof Platform)) || ((objB instanceof Avatar) && (objA instanceof Platform))) {
                //if(avatar.getCurrentPlatform() != objB && avatar.getCurrentPlatform() != objA) {
                if (objB instanceof Platform) {
                    if (avatar.getShifted() > 0 && ((SimpleObstacle) objB).getSpace() != 3 ){
                        avatar.setSpliced(true);
                        avatar.setCurrentPlatform((Platform) objB);
                        return;
                    }
                }
                if (objA instanceof Platform) {
                    if (avatar.getShifted() > 0 && ((SimpleObstacle) objA).getSpace() != 3 ){
                        avatar.setSpliced(true);
                        avatar.setCurrentPlatform((Platform) objA);
                        return;
                    }
                }
                /*if (avatar.getShifted() > 0 && ((SimpleObstacle) objB).getSpace() != 3 && ((SimpleObstacle) objA).getSpace() != 3){
                    avatar.setSpliced(true);
                    if (objB instanceof Platform) {
                        avatar.setCurrentPlatform((Platform) objB);
                    } else {
                        avatar.setCurrentPlatform((Platform) objA);
                    }
                    return;
                }*/
                boolean latentCol = false;
                if (avatar.getStartedDashing() == 1) {
                    if (avatar.getCurrentPlatform() == objA || avatar.getCurrentPlatform() == objB) {
                        latentCol = true;
                    }
                }
                if (!latentCol) {
                    if (!avatar.isSticking()) {
                        Float norm_angle = contact.getWorldManifold().getNormal().angle();

                        if (!norm_angle.isNaN()) {
                        /*if ((norm_angle.intValue()) == 0){
                            System.out.println("Fekwbfiewq");
                            cur_normal = (float) Math.toRadians(90);
                        } else {*/
                            cur_normal = (float) Math.toRadians(norm_angle - 90);
                            //}
                        }

                    /*if (avatar.getCurrentPlatform() != null && (bd1.getName().equals("wall") || bd2.getName().equals("wall"))) {
                        if (!avatar.getCurrentPlatform().equals(objA) && !avatar.getCurrentPlatform().equals(objB)) {
                            avatar.setGrounded(true);
                            avatar.setSticking(true);
                            avatar.setNewAngle(cur_normal);
                            if (objB instanceof Platform) {
                                avatar.setCurrentPlatform((Platform) objB);
                            } else {
                                avatar.setCurrentPlatform((Platform) objA);
                            }
                        }
                    } else {*/

                        //avatar.contactPoint = contact.getWorldManifold().getPoints()[0];
                        avatar.setGrounded(true);
                        avatar.setSticking(true);
                        //System.out.println(cur_normal);
                        avatar.setNewAngle(cur_normal);
                        if (objB instanceof Platform) {
                            avatar.setCurrentPlatform((Platform) objB);
                        } else {
                            avatar.setCurrentPlatform((Platform) objA);
                        }
                        //}
                    }
                    //}
                }

            }

            if (objA instanceof Enemy || objB instanceof Enemy) {
                Enemy enemy = (objA instanceof Enemy ? (Enemy) objA : (Enemy) objB);
                if (enemy.getAi() == Enemy.EnemyType.WALK) {
                    if ((enemy != bd1 && !bd1.getName().equals("bullet")) ||
                            (enemy != bd2 && !bd2.getName().equals("bullet"))) {
                        if (enemy.getPlatformFixture() == null) {
                            enemy.setPlatformFixture(enemy == bd1 ? fix2 : fix1);
                            enemy.setNextDirection(1);
                            enemy.setMovement(1);
                        }
                        if ((fix1 != enemy.getPlatformFixture() && objA instanceof Platform) ||
                                (fix2 != enemy.getPlatformFixture() && objB instanceof Platform)) {
                            enemy.setNextDirection(-1 * enemy.getNextDirection());
                            enemy.setMovement(0);
                            enemy.setFaceDirection(-1 * enemy.getFaceDirection());
                        }
                    }
                } else if (enemy.getAi() == Enemy.EnemyType.FLY) {
                    if ((enemy != bd1 && !bd1.getName().equals("bullet")) ||
                            (enemy != bd2 && !bd2.getName().equals("bullet"))) {
                        if (fix1 == enemy.getSensorFixtureCenter() || fix2 == enemy.getSensorFixtureCenter()) {
                            enemy.setFlyAngle(contact.getWorldManifold().getNormal().angle());
                        }
                    }
                }
            }


            // Check for win condition
            if ((bd1 == avatar && bd2 == controller.getGoalDoor()) ||
                    (bd1 == controller.getGoalDoor() && bd2 == avatar)) {
                Door door = (Door) controller.getGoalDoor();
                if (door.getOpen()) {
                    controller.setComplete(true);
                } /*else {
                    avatar.resetDashNum(1);
                    avatar.setLinearVelocity(avatar.getLinearVelocity().scl(-1));
                    if (avatar.getLinearVelocity().len() < 1.0f){
                        avatar.setLinearVelocity(avatar.getLinearVelocity().nor());
                    }
                }*/
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }


    /**
     * Callback method for the start of a collision
     * <p>
     * This method is called when two objects cease to touch.  The main use of this method
     * is to determine when the characer is NOT on the ground.  This is how we prevent
     * double jumping.
     */
    @Override
    public void endContact(Contact contact) {

        Fixture fix1 = contact.getFixtureA();
        Fixture fix2 = contact.getFixtureB();

        Body body1 = fix1.getBody();
        Body body2 = fix2.getBody();

        Object fd1 = fix1.getUserData();
        Object fd2 = fix2.getUserData();

        Object bd1 = body1.getUserData();
        Object bd2 = body2.getUserData();

        if ((avatar.getSensorName().equals(fd2) && avatar != bd1) ||
                (avatar.getSensorName().equals(fd1) && avatar != bd2)) {
            sensorFixtures.remove(avatar == bd1 ? fix2 : fix1);
            if (sensorFixtures.size == 0) {
                avatar.setGrounded(false);
                avatar.setSticking(false);
            }
        }

        if (bd1 instanceof Enemy || bd2 instanceof Enemy) {
            Enemy enemy = (bd1 instanceof Enemy ? (Enemy) bd1 : (Enemy) bd2);
            if (enemy.getAi() == Enemy.EnemyType.WALK) {
                if (fix1 == enemy.getPlatformFixture() || fix2 == enemy.getPlatformFixture()) {
                    enemy.setMovement(0);
                    enemy.setNextDirection(-1 * enemy.getNextDirection());
                }
            }
        }
    }

    @Override
    public void preSolve(Contact contact, Manifold oldManifold) {

    }

    @Override
    public void postSolve(Contact contact, ContactImpulse impulse) {
        this.processContact(contact);
    }

    /**
     * Returns the collection of listeners
     *
     * @param categoryA
     * @param categoryB
     * @return
     */
    private ContactListener getListener(short categoryA, short categoryB) {
        ObjectMap<Short, ContactListener> listenerCollection = listeners.get(categoryA);
        if (listenerCollection == null) {
            return null;
        }
        return listenerCollection.get(categoryB);
    }

}
