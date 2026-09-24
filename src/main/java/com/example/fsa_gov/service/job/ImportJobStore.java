package com.example.fsa_gov.service.job;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory хранилище состояний задач.
 * TTL — 1 час. Чистка — каждые 10 минут.
 */
@Slf4j
@Component
public class ImportJobStore {

    private static final Duration TTL = Duration.ofHours(1);

    private final Map<String, ImportJobState> jobs = new ConcurrentHashMap<>();

    public ImportJobState create() {
        ImportJobState state = new ImportJobState();
        state.setJobId(UUID.randomUUID().toString());
        state.setStatus(ImportJobState.JobStatus.PENDING);
        state.setCreatedAt(Instant.now());
        state.setUpdatedAt(Instant.now());
        jobs.put(state.getJobId(), state);
        return state;
    }

    public Optional<ImportJobState> get(String jobId) {
        return Optional.ofNullable(jobs.get(jobId));
    }

    public void update(ImportJobState state) {
        state.setUpdatedAt(Instant.now());
        jobs.put(state.getJobId(), state);
    }

    /** Чистка устаревших job'ов */
    @Scheduled(fixedRate = 10 * 60 * 1000)
    public void cleanup() {
        Instant threshold = Instant.now().minus(TTL);
        jobs.entrySet().removeIf(e -> e.getValue().getCreatedAt().isBefore(threshold));
        log.debug("Job store cleanup: осталось {}", jobs.size());
    }
}