package weather.proxy.ratelimit;

import java.time.Duration;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class RateLimiter {
    private static final Logger LOGGER = Logger.getLogger(RateLimiter.class.getName());
    
    private final Semaphore semaphore;
    private final int maxRequests;
    private final Duration period;
    private final Thread releaseThread;
    private volatile boolean running = true;
    public RateLimiter(int maxRequests, Duration period) {

        this.maxRequests = maxRequests;
        this.period = period;
        this.semaphore = new Semaphore(maxRequests);
        this.releaseThread = new Thread(() -> {
            while (running) {
                try {
                    long sleepTimeMs = period.toMillis() / maxRequests;
                    TimeUnit.MILLISECONDS.sleep(sleepTimeMs);

                    if (semaphore.availablePermits() < maxRequests) {
                        semaphore.release();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
        
        releaseThread.setDaemon(true);
        releaseThread.start();
    }

    public void acquire() {
        try {
            semaphore.acquire();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOGGER.warning("Interrupted while waiting for rate limit");
        }
    }

    public void shutdown() {
        running = false;
        releaseThread.interrupt();
    }
}