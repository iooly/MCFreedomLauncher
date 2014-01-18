package net.minecraft.launcher.updater;

import java.util.concurrent.FutureTask;
import org.apache.logging.log4j.LogManager;
import java.util.concurrent.Callable;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.CancellationException;
import java.util.concurrent.Future;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import org.apache.logging.log4j.Logger;
import java.util.concurrent.ThreadPoolExecutor;

public class ExceptionalThreadPoolExecutor extends ThreadPoolExecutor
{
    private static final Logger LOGGER;
    
    public ExceptionalThreadPoolExecutor(final int corePoolSize, final int maximumPoolSize, final long keepAliveTime, final TimeUnit unit) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, new LinkedBlockingQueue<Runnable>());
    }
    
    @Override
    protected void afterExecute(final Runnable r, Throwable t) {
        super.afterExecute(r, t);
        if (t == null && r instanceof Future) {
            try {
                final Future<?> future = (Future<?>)r;
                if (future.isDone()) {
                    future.get();
                }
            }
            catch (CancellationException ce) {
                t = ce;
            }
            catch (ExecutionException ee) {
                t = ee.getCause();
            }
            catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            }
        }
    }
    
    @Override
    protected <T> RunnableFuture<T> newTaskFor(final Runnable runnable, final T value) {
        return new ExceptionalFutureTask<T>(runnable, value);
    }
    
    @Override
    protected <T> RunnableFuture<T> newTaskFor(final Callable<T> callable) {
        return new ExceptionalFutureTask<T>(callable);
    }
    
    static {
        LOGGER = LogManager.getLogger();
    }
    
    public class ExceptionalFutureTask<T> extends FutureTask<T>
    {
        public ExceptionalFutureTask(final Callable<T> callable) {
            super(callable);
        }
        
        public ExceptionalFutureTask(final Runnable runnable, final T result) {
            super(runnable, result);
        }
        
        @Override
        protected void done() {
            try {
                this.get();
            }
            catch (Throwable t) {
                ExceptionalThreadPoolExecutor.LOGGER.error("Unhandled exception in executor " + this, t);
            }
        }
    }
}
