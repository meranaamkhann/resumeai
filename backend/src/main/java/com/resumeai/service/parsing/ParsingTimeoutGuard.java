package com.resumeai.service.parsing;

import com.resumeai.exception.InvalidFileException;
import org.springframework.stereotype.Component;

import java.util.concurrent.*;

@Component
public class ParsingTimeoutGuard {

    private static final long TIMEOUT_SECONDS = 15;

    private final ExecutorService executor = Executors.newFixedThreadPool(
            Math.max(2, Runtime.getRuntime().availableProcessors()),
            r -> {
                Thread t = new Thread(r, "resume-parsing-worker");
                t.setDaemon(true);
                return t;
            }
    );

    public <T> T runWithTimeout(Callable<T> task) {
        Future<T> future = executor.submit(task);
        try {
            return future.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            future.cancel(true);
            throw new InvalidFileException(
                    "This file took too long to process and may be malformed or unusually complex. Please try a different file.");
        } catch (ExecutionException e) {
            if (e.getCause() instanceof RuntimeException re) {
                throw re;
            }
            throw new InvalidFileException("We couldn't process this file.");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new InvalidFileException("Processing was interrupted. Please try again.");
        }
    }
}

