package org.polaris2023.gtu.physics.world;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 物理暂停管理器
 * <p>
 * 当游戏暂停时（如按ESC打开菜单），暂停所有物理计算
 * <p>
 * <b>暂停场景：</b>
 * <ul>
 *   <li>单人游戏按ESC暂停</li>
 *   <li>窗口失去焦点（可配置）</li>
 *   <li>服务器tick暂停</li>
 * </ul>
 */
public class PhysicsPauseManager {

    private static final PhysicsPauseManager INSTANCE = new PhysicsPauseManager();

    public static PhysicsPauseManager getInstance() {
        return INSTANCE;
    }

    // ==================== 暂停状态 ====================

    /**
     * 是否暂停
     */
    private final AtomicBoolean paused = new AtomicBoolean(false);

    /**
     * 暂停原因
     */
    private volatile String pauseReason = null;

    /**
     * 暂停锁
     */
    private final ReentrantLock pauseLock = new ReentrantLock();

    /**
     * 暂停条件
     */
    private final Condition pauseCondition = pauseLock.newCondition();

    /**
     * 暂停开始时间
     */
    private volatile long pauseStartTime = 0;

    // ==================== 统计 ====================

    private volatile long totalPausedTime = 0;
    private volatile int pauseCount = 0;

    private PhysicsPauseManager() {
    }

    // ==================== 暂停控制 ====================

    /**
     * 暂停物理系统
     *
     * @param reason 暂停原因
     */
    public void pause(String reason) {
        if (paused.compareAndSet(false, true)) {
            pauseReason = reason;
            pauseStartTime = System.currentTimeMillis();
            pauseCount++;
        }
    }

    /**
     * 恢复物理系统
     */
    public void resume() {
        if (paused.compareAndSet(true, false)) {
            // 计算暂停时长
            if (pauseStartTime > 0) {
                totalPausedTime += System.currentTimeMillis() - pauseStartTime;
                pauseStartTime = 0;
            }
            pauseReason = null;

            // 唤醒等待的线程
            pauseLock.lock();
            try {
                pauseCondition.signalAll();
            } finally {
                pauseLock.unlock();
            }
        }
    }

    /**
     * 切换暂停状态
     *
     * @param paused 是否暂停
     * @param reason 暂停原因（恢复时可为null）
     */
    public void setPaused(boolean paused, String reason) {
        if (paused) {
            this.pause(reason);
        } else {
            this.resume();
        }
    }

    // ==================== 状态查询 ====================

    /**
     * 是否暂停
     */
    public boolean isPaused() {
        return paused.get();
    }

    /**
     * 获取暂停原因
     */
    public String getPauseReason() {
        return pauseReason;
    }

    /**
     * 获取暂停次数
     */
    public int getPauseCount() {
        return pauseCount;
    }

    /**
     * 获取总暂停时间（毫秒）
     */
    public long getTotalPausedTime() {
        return totalPausedTime;
    }

    /**
     * 获取当前暂停时长（毫秒）
     */
    public long getCurrentPauseDuration() {
        if (paused.get() && pauseStartTime > 0) {
            return System.currentTimeMillis() - pauseStartTime;
        }
        return 0;
    }

    // ==================== 线程等待 ====================

    /**
     * 等待直到恢复（用于工作线程）
     *
     * @param timeoutMs 超时时间（毫秒），0表示无限等待
     * @return 是否正常唤醒（false表示超时）
     */
    public boolean awaitResume(long timeoutMs) {
        if (!paused.get()) {
            return true;
        }

        pauseLock.lock();
        try {
            if (timeoutMs > 0) {
                return pauseCondition.awaitNanos(timeoutMs * 1_000_000) > 0;
            } else {
                pauseCondition.await();
                return true;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        } finally {
            pauseLock.unlock();
        }
    }

    /**
     * 如果暂停则等待恢复
     */
    public void waitIfPaused() {
        while (paused.get()) {
            awaitResume(100);
        }
    }

    // ==================== 重置 ====================

    /**
     * 重置统计
     */
    public void resetStats() {
        totalPausedTime = 0;
        pauseCount = 0;
    }

    /**
     * 强制恢复（用于异常情况）
     */
    public void forceResume() {
        paused.set(false);
        pauseReason = null;
        pauseStartTime = 0;

        pauseLock.lock();
        try {
            pauseCondition.signalAll();
        } finally {
            pauseLock.unlock();
        }
    }
}
