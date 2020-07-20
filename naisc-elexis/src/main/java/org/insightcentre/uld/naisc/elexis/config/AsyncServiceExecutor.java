package org.insightcentre.uld.naisc.elexis.config;

import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurerSupport;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Defines the Configuration for Async execution of linking services
 *
 * @author Suruchi Gupta
 */
@Configuration
@EnableAsync
public class AsyncServiceExecutor extends AsyncConfigurerSupport {
    /**
     * Defines the async executor and the associated characteristics
     *
     * @return executor
     */
    @Override
    public Executor getAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(20);
        executor.setThreadNamePrefix("AsyncThread");
        executor.initialize();
        return executor;
    }

    /**
     * Defines the protocol to be executed for UncaughtExceptions
     *
     * @return asyncUncaughtExceptionHandler
     */
    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return (throwable, method, objects)
                -> throwable.printStackTrace();
    }
}
