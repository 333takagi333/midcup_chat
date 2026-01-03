package com.chat.ui;

import javafx.application.Platform;
import javafx.stage.Window;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

/**
 * DialogHelper - 可复用的对话框安全调用工具
 */
public final class DialogHelper {

    private DialogHelper() {}

    // Existing supplier-based async helpers
    public static void safeShowErrorAsync(Supplier<Window> ownerSupplier, String message) {
        Platform.runLater(() -> {
            try {
                Window owner = ownerSupplier != null ? ownerSupplier.get() : null;
                if (owner != null) {
                    DialogUtil.showError(owner, message);
                } else {
                    System.err.println("[DialogHelper] owner not ready for error dialog: " + message);
                    DialogUtil.showError(message);
                }
            } catch (Exception e) {
                System.err.println("[DialogHelper] showErrorAsync failed: " + e.getMessage());
            }
        });
    }

    public static void safeShowInfoAsync(Supplier<Window> ownerSupplier, String message) {
        Platform.runLater(() -> {
            try {
                Window owner = ownerSupplier != null ? ownerSupplier.get() : null;
                if (owner != null) {
                    DialogUtil.showInfo(owner, message);
                } else {
                    System.err.println("[DialogHelper] owner not ready for info dialog: " + message);
                    DialogUtil.showInfo(message);
                }
            } catch (Exception e) {
                System.err.println("[DialogHelper] showInfoAsync failed: " + e.getMessage());
            }
        });
    }

    /**
     * 同步显示确认对话框并返回用户选择（如果 owner 未就绪则返回 false 并打印错误）
     * 如果调用线程不是 JavaFX 线程，会在 JavaFX 线程上执行并等待结果。
     */
    public static boolean safeShowConfirmation(Supplier<Window> ownerSupplier, String message) {
        try {
            if (Platform.isFxApplicationThread()) {
                Window owner = ownerSupplier != null ? ownerSupplier.get() : null;
                if (owner != null) {
                    return DialogUtil.showConfirmation(owner, message);
                } else {
                    System.err.println("[DialogHelper] owner not ready for confirmation dialog: " + message);
                    return DialogUtil.showConfirmation(message);
                }
            } else {
                final CountDownLatch latch = new CountDownLatch(1);
                final AtomicBoolean result = new AtomicBoolean(false);
                Platform.runLater(() -> {
                    try {
                        Window owner = ownerSupplier != null ? ownerSupplier.get() : null;
                        if (owner != null) {
                            result.set(DialogUtil.showConfirmation(owner, message));
                        } else {
                            System.err.println("[DialogHelper] owner not ready for confirmation dialog (async): " + message);
                            result.set(DialogUtil.showConfirmation(message));
                        }
                    } catch (Exception e) {
                        System.err.println("[DialogHelper] safeShowConfirmation async failed: " + e.getMessage());
                        result.set(false);
                    } finally {
                        latch.countDown();
                    }
                });
                try {
                    latch.await();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return false;
                }
                return result.get();
            }
        } catch (Exception e) {
            System.err.println("[DialogHelper] safeShowConfirmation failed: " + e.getMessage());
            return false;
        }
    }

    // Convenience overloads that accept Window directly (used by existing callers)
    public static void showError(Window owner, String message) {
        if (owner != null) {
            safeShowErrorAsync(() -> owner, message);
        } else {
            safeShowErrorAsync(null, message);
        }
    }

    public static void showInfo(Window owner, String message) {
        if (owner != null) {
            safeShowInfoAsync(() -> owner, message);
        } else {
            safeShowInfoAsync(null, message);
        }
    }

    public static boolean showConfirmation(Window owner, String message) {
        return safeShowConfirmation(() -> owner, message);
    }
}
