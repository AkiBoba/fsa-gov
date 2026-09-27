package com.example.fsa_gov.service;

import com.example.fsa_gov.dto.CertificateResponseDto;
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
        jobRunner.run(state, parsed);
        return state.getJobId();
    }

    public Optional<ImportJobState> getJob(String jobId) {
        return jobStore.get(jobId);
    }

    /**
     * Синхронный запуск. После завершения — проставляет sourceNumber = numberDoc
     * во всех найденных DTO (обычный флоу без нормализации).
     */
    public ImportJobState runSync(List<String> lines) {
        ParseResult parsed = parseLines(lines);
        ImportJobState state = jobStore.create();
        jobRunner.runSync(state, parsed);

        // Проставляем sourceNumber = numberDoc (обычный флоу)
        fillSourceNumber(state.getRfFound());
        fillSourceNumber(state.getEaeuFound());

        return state;
    }

    /**
     * Проставляет sourceNumber = numberDoc для всех DTO,
     * у которых sourceNumber ещё не заполнен.
     */
    private void fillSourceNumber(List<CertificateResponseDto> list) {
        if (list == null) return;
        for (CertificateResponseDto dto : list) {
            if (dto != null && dto.getSourceNumber() == null) {
                dto.setSourceNumber(dto.getNumberDoc());
            }
        }
    }
}