package com.example.fsa_gov.service;

import com.example.fsa_gov.parser.CertificateListParser;
import com.example.fsa_gov.parser.dto.ParseResult;
import com.example.fsa_gov.service.job.ImportJobRunner;
import com.example.fsa_gov.service.job.ImportJobState;
import com.example.fsa_gov.service.job.ImportJobStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CertificateImportService {

    private final CertificateListParser parser;
    private final ImportJobStore jobStore;
    private final ImportJobRunner jobRunner;

    public ParseResult parseLines(List<String> lines) {
        ParseResult result = parser.parse(lines);
        log.info("Парсинг: РФ={}, ЕАЭС={}, мусор={}",
                result.getRfCertificates().size(),
                result.getEaeuCertificates().size(),
                result.getInvalidEntries().size());
        return result;
    }

    public String startImportAsync(ParseResult parsed) {
        ImportJobState state = jobStore.create();
        jobRunner.run(state, parsed); // @Async
        return state.getJobId();
    }

    public Optional<ImportJobState> getJob(String jobId) {
        return jobStore.get(jobId);
    }

    /**
     * Синхронный запуск: ждёт, пока job не перейдёт в SUCCESS/FAILED.
     */
    public ImportJobState runSync(List<String> lines) {
        ParseResult parsed = parseLines(lines);
        ImportJobState state = jobStore.create();
        jobRunner.runSync(state, parsed); // блокирующий вызов
        return state;
    }
}