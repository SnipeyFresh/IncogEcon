package com.snipeyfresh.incogshop.util;

import com.snipeyfresh.incogshop.IncogShopPlugin;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

public final class AsyncIoManager {
    private final IncogShopPlugin plugin;
    private final ExecutorService executor;
    private final AtomicBoolean closed = new AtomicBoolean(false);

    public AsyncIoManager(IncogShopPlugin plugin) {
        this.plugin = plugin;
        this.executor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "IncogEcon-IO");
            t.setDaemon(true);
            return t;
        });
    }

    public void submit(String label, Runnable task) {
        if (task == null) return;
        if (closed.get()) {
            runSafely(label, task);
            return;
        }
        try {
            executor.execute(() -> runSafely(label, task));
        } catch (RejectedExecutionException ex) {
            runSafely(label, task);
        }
    }

    public void writeTextAtomic(Path target, String contents, String label) {
        submit(label, () -> {
            try {
                Path parent = target.getParent();
                if (parent != null) Files.createDirectories(parent);
                Path temp = target.resolveSibling(target.getFileName().toString() + ".tmp");
                Files.writeString(temp, contents, StandardCharsets.UTF_8,
                        StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
                try {
                    Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
                } catch (IOException atomicFailure) {
                    Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING);
                }
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        });
    }

    public void appendText(Path target, String contents, String label) {
        if (contents == null || contents.isEmpty()) return;
        submit(label, () -> {
            try {
                Path parent = target.getParent();
                if (parent != null) Files.createDirectories(parent);
                Files.writeString(target, contents, StandardCharsets.UTF_8,
                        StandardOpenOption.CREATE, StandardOpenOption.APPEND, StandardOpenOption.WRITE);
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        });
    }

    public boolean flush(long timeoutMillis) {
        if (closed.get()) return true;
        try {
            Future<?> barrier = executor.submit(() -> {});
            barrier.get(Math.max(1L, timeoutMillis), TimeUnit.MILLISECONDS);
            return true;
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return false;
        } catch (ExecutionException | TimeoutException | RejectedExecutionException ex) {
            plugin.getLogger().warning("Timed out waiting for IncogEcon disk writes: " + ex.getClass().getSimpleName());
            return false;
        }
    }

    public void shutdownAndFlush(long timeoutMillis) {
        if (!closed.compareAndSet(false, true)) return;
        executor.shutdown();
        try {
            if (!executor.awaitTermination(Math.max(1L, timeoutMillis), TimeUnit.MILLISECONDS)) {
                plugin.getLogger().warning("IncogEcon disk writer did not finish in time; forcing shutdown.");
                executor.shutdownNow();
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            executor.shutdownNow();
        }
    }

    private void runSafely(String label, Runnable task) {
        try {
            task.run();
        } catch (Throwable ex) {
            Throwable root = ex;
            while (root.getCause() != null) root = root.getCause();
            plugin.getLogger().severe("Async I/O failure (" + label + "): "
                    + root.getClass().getSimpleName() + ": " + String.valueOf(root.getMessage()));
        }
    }
}
