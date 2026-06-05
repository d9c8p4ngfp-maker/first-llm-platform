package com.first.gateway.infra.async;

import com.first.gateway.domain.entity.AsyncTask;
import com.first.gateway.repository.AsyncTaskRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@Component
public class AsyncTaskScheduler {

    private static final Logger log = LoggerFactory.getLogger(AsyncTaskScheduler.class);
    private static final Duration STALE_THRESHOLD = Duration.ofMinutes(10);

    private final AsyncTaskRepository taskRepository;
    private final Map<String, TaskHandler> handlers;

    public AsyncTaskScheduler(AsyncTaskRepository taskRepository, Map<String, TaskHandler> handlers) {
        this.taskRepository = taskRepository;
        this.handlers = handlers;
    }

    @Scheduled(fixedDelay = 10_000)
    @Transactional
    public void processTasks() {
        recoverStaleTasks();

        for (Map.Entry<String, TaskHandler> entry : handlers.entrySet()) {
            String taskType = entry.getKey();
            TaskHandler handler = entry.getValue();

            List<AsyncTask> pending = taskRepository
                .findByTaskTypeAndStatusOrderByCreatedAtAsc(taskType, "PENDING");

            pending.stream().limit(handler.batchSize()).forEach(task -> executeTask(task, handler));
        }
    }

    private void executeTask(AsyncTask task, TaskHandler handler) {
        int updated = taskRepository.claimTask(task.getId(), task.getVersion(), Instant.now());
        if (updated == 0) return;

        task = taskRepository.findById(task.getId()).orElse(null);
        if (task == null) return;

        try {
            handler.execute(task);

            task.setStatus("DONE");
            task.setFinishedAt(Instant.now());
            task.setStartedAt(null);
            task.setErrorMessage(null);
            taskRepository.save(task);
            handler.onSuccess(task);
        } catch (Exception e) {
            log.warn("Task {} failed: {}", task.getId(), e.getMessage());
            handleFailure(task, e);
            handler.onFailure(task, e);
        }
    }

    private void handleFailure(AsyncTask task, Exception e) {
        if (task.getRetryCount() < task.getMaxRetry()) {
            task.setStatus("PENDING");
            task.setRetryCount(task.getRetryCount() + 1);
        } else {
            task.setStatus("FAILED");
        }
        task.setStartedAt(null);
        task.setErrorMessage(e.getMessage());
        taskRepository.save(task);
    }

    private void recoverStaleTasks() {
        Instant cutoff = Instant.now().minus(STALE_THRESHOLD);
        List<AsyncTask> stale = taskRepository.findByStatusAndStartedAtBefore("RUNNING", cutoff);
        for (AsyncTask task : stale) {
            log.warn("Recovering stale task {}", task.getId());
            handleFailure(task, new RuntimeException("处理超时"));
            handlers.getOrDefault(task.getTaskType(), TaskHandler.NOOP)
                .onFailure(task, new RuntimeException("处理超时"));
        }
    }
}
