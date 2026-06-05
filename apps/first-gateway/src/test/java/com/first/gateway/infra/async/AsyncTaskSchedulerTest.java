package com.first.gateway.infra.async;

import com.first.gateway.domain.entity.AsyncTask;
import com.first.gateway.repository.AsyncTaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AsyncTaskSchedulerTest {

    @Mock private AsyncTaskRepository taskRepository;
    @Mock private TaskHandler handler;

    private AsyncTaskScheduler scheduler;

    @BeforeEach
    void setUp() {
        when(handler.batchSize()).thenReturn(5);
        scheduler = new AsyncTaskScheduler(taskRepository, Map.of("DOC_INDEX", handler));
    }

    @Test
    void shouldClaimAndExecutePendingTask() throws Exception {
        AsyncTask task = newTask(1L, "DOC_INDEX", "PENDING", 0, 3);
        task.setRefId(100L);

        when(taskRepository.findByTaskTypeAndStatusOrderByCreatedAtAsc("DOC_INDEX", "PENDING"))
                .thenReturn(List.of(task));
        when(taskRepository.claimTask(eq(1L), eq(0), any())).thenReturn(1);
        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));

        scheduler.processTasks();

        // Scheduler sets status to DONE before calling onSuccess
        assertThat(task.getStatus()).isEqualTo("DONE");
        assertThat(task.getFinishedAt()).isNotNull();
        assertThat(task.getErrorMessage()).isNull();

        verify(handler).execute(task);
        verify(handler).onSuccess(task);
    }

    @Test
    void shouldNotExecuteWhenClaimReturnsZero() throws Exception {
        AsyncTask task = newTask(1L, "DOC_INDEX", "PENDING", 0, 3);

        when(taskRepository.findByTaskTypeAndStatusOrderByCreatedAtAsc("DOC_INDEX", "PENDING"))
                .thenReturn(List.of(task));
        when(taskRepository.claimTask(eq(1L), eq(0), any())).thenReturn(0); // claimed by another instance

        scheduler.processTasks();

        verify(handler, never()).execute(task);
        verify(handler, never()).onSuccess(task);
        verify(handler, never()).onFailure(any(), any());
    }

    @Test
    void shouldRetryOnFailure() throws Exception {
        AsyncTask task = newTask(1L, "DOC_INDEX", "PENDING", 0, 3);
        task.setRefId(100L);

        when(taskRepository.findByTaskTypeAndStatusOrderByCreatedAtAsc("DOC_INDEX", "PENDING"))
                .thenReturn(List.of(task));
        when(taskRepository.claimTask(eq(1L), eq(0), any())).thenReturn(1);
        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));
        doThrow(new RuntimeException("test error")).when(handler).execute(task);

        scheduler.processTasks();

        // Retry count should be incremented, status reset to PENDING
        assertThat(task.getStatus()).isEqualTo("PENDING");
        assertThat(task.getRetryCount()).isEqualTo(1);
        assertThat(task.getErrorMessage()).isEqualTo("test error");

        verify(handler).onFailure(eq(task), any(RuntimeException.class));
    }

    @Test
    void shouldMarkFailedWhenRetryExhausted() throws Exception {
        AsyncTask task = newTask(1L, "DOC_INDEX", "PENDING", 3, 3);
        task.setRefId(100L);

        when(taskRepository.findByTaskTypeAndStatusOrderByCreatedAtAsc("DOC_INDEX", "PENDING"))
                .thenReturn(List.of(task));
        when(taskRepository.claimTask(eq(1L), eq(3), any())).thenReturn(1);
        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));
        doThrow(new RuntimeException("fatal error")).when(handler).execute(task);

        scheduler.processTasks();

        assertThat(task.getStatus()).isEqualTo("FAILED");
        assertThat(task.getRetryCount()).isEqualTo(3);
        assertThat(task.getErrorMessage()).isEqualTo("fatal error");

        verify(handler).onFailure(eq(task), any(RuntimeException.class));
    }

    @Test
    void shouldRecoverStaleRunningTasks() {
        AsyncTask stale = newTask(1L, "DOC_INDEX", "RUNNING", 0, 3);
        stale.setStartedAt(Instant.now().minus(Duration.ofMinutes(15)));

        when(taskRepository.findByTaskTypeAndStatusOrderByCreatedAtAsc("DOC_INDEX", "PENDING"))
                .thenReturn(List.of());
        when(taskRepository.findByStatusAndStartedAtBefore(eq("RUNNING"), any()))
                .thenReturn(List.of(stale));

        scheduler.processTasks();

        // Stale task should be reset to PENDING with incremented retry
        assertThat(stale.getStatus()).isEqualTo("PENDING");
        assertThat(stale.getRetryCount()).isEqualTo(1);
        assertThat(stale.getErrorMessage()).contains("超时");

        verify(handler).onFailure(eq(stale), any(RuntimeException.class));
    }

    @Test
    void shouldSkipStaleTask_whenNoHandlerRegistered() {
        AsyncTask stale = newTask(1L, "UNKNOWN_TYPE", "RUNNING", 0, 3);
        stale.setStartedAt(Instant.now().minus(Duration.ofMinutes(15)));

        when(taskRepository.findByTaskTypeAndStatusOrderByCreatedAtAsc("DOC_INDEX", "PENDING"))
                .thenReturn(List.of());
        when(taskRepository.findByStatusAndStartedAtBefore(eq("RUNNING"), any()))
                .thenReturn(List.of(stale));

        scheduler.processTasks();

        // Task should still be recovered (status reset) but NOOP handler used
        assertThat(stale.getStatus()).isEqualTo("PENDING");
        assertThat(stale.getRetryCount()).isEqualTo(1);
        verify(handler, never()).onFailure(any(), any());
    }

    @Test
    void shouldLimitBatchSize() throws Exception {
        when(handler.batchSize()).thenReturn(2);

        AsyncTask task1 = newTask(1L, "DOC_INDEX", "PENDING", 0, 3);
        AsyncTask task2 = newTask(2L, "DOC_INDEX", "PENDING", 0, 3);
        AsyncTask task3 = newTask(3L, "DOC_INDEX", "PENDING", 0, 3);

        when(taskRepository.findByTaskTypeAndStatusOrderByCreatedAtAsc("DOC_INDEX", "PENDING"))
                .thenReturn(List.of(task1, task2, task3));
        when(taskRepository.claimTask(eq(1L), eq(0), any())).thenReturn(1);
        when(taskRepository.claimTask(eq(2L), eq(0), any())).thenReturn(1);
        when(taskRepository.findById(1L)).thenReturn(Optional.of(task1));
        when(taskRepository.findById(2L)).thenReturn(Optional.of(task2));

        scheduler.processTasks();

        // Only first 2 should be executed (batchSize=2)
        verify(handler).execute(task1);
        verify(handler).execute(task2);
        verify(handler, never()).execute(task3);
    }

    @Test
    void shouldClearStartedAtOnCompletion() throws Exception {
        AsyncTask task = newTask(1L, "DOC_INDEX", "PENDING", 0, 3);
        task.setStartedAt(Instant.now().minus(Duration.ofMinutes(1)));

        when(taskRepository.findByTaskTypeAndStatusOrderByCreatedAtAsc("DOC_INDEX", "PENDING"))
                .thenReturn(List.of(task));
        when(taskRepository.claimTask(eq(1L), eq(0), any())).thenReturn(1);
        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));

        scheduler.processTasks();

        assertThat(task.getStatus()).isEqualTo("DONE");
        assertThat(task.getStartedAt()).isNull();
        assertThat(task.getFinishedAt()).isNotNull();
    }

    @Test
    void shouldClearStartedAtOnFailure() throws Exception {
        AsyncTask task = newTask(1L, "DOC_INDEX", "PENDING", 0, 3);
        task.setStartedAt(Instant.now().minus(Duration.ofMinutes(1)));

        when(taskRepository.findByTaskTypeAndStatusOrderByCreatedAtAsc("DOC_INDEX", "PENDING"))
                .thenReturn(List.of(task));
        when(taskRepository.claimTask(eq(1L), eq(0), any())).thenReturn(1);
        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));
        doThrow(new RuntimeException("error")).when(handler).execute(task);

        scheduler.processTasks();

        assertThat(task.getStartedAt()).isNull();
    }

    private static AsyncTask newTask(Long id, String type, String status, int retryCount, int maxRetry) {
        AsyncTask task = new AsyncTask();
        task.setId(id);
        task.setTaskType(type);
        task.setStatus(status);
        task.setRetryCount(retryCount);
        task.setMaxRetry(maxRetry);
        task.setVersion(retryCount); // version typically tracks retry count in tests
        return task;
    }
}
