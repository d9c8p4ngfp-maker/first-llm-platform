package com.first.gateway.infra.async;

import com.first.gateway.domain.entity.AsyncTask;

public interface TaskHandler {
    void execute(AsyncTask task) throws Exception;
    default void onSuccess(AsyncTask task) {}
    default void onFailure(AsyncTask task, Exception e) {}
    default int batchSize() { return 5; }
    TaskHandler NOOP = task -> {};
}
