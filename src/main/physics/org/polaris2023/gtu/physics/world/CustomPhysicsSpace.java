package org.polaris2023.gtu.physics.world;

import com.jme3.bullet.PhysicsSpace;
import com.jme3.bullet.collision.PhysicsCollisionObject;
import org.polaris2023.gtu.physics.collision.EntityCollisionManager;

/**
 * 自定义物理空间
 * <p>
 * 继承 PhysicsSpace 以覆盖接触回调方法
 */
public class CustomPhysicsSpace extends PhysicsSpace {

    /**
     * 创建自定义物理空间
     */
    public CustomPhysicsSpace(BroadphaseType broadphaseType) {
        super(broadphaseType);
    }

    @Override
    public boolean onContactConceived(long pointId, long manifoldId,
                                       PhysicsCollisionObject pcoA, PhysicsCollisionObject pcoB) {
        return EntityCollisionManager.getInstance().onContactConceived(pointId, manifoldId, pcoA, pcoB);
    }

    @Override
    public void onContactEnded(long manifoldId) {
        EntityCollisionManager.getInstance().onContactEnded(manifoldId);
    }

    @Override
    public void onContactProcessed(PhysicsCollisionObject pcoA,
                                     PhysicsCollisionObject pcoB, long pointId) {
        EntityCollisionManager.getInstance().onContactProcessed(pcoA, pcoB, pointId);
    }

    @Override
    public void onContactStarted(long manifoldId) {
        EntityCollisionManager.getInstance().onContactStarted(manifoldId);
    }
}
