package com.example.fsa_gov.controller;

import com.example.fsa_gov.parser.dto.ParseResult;
import com.example.fsa_gov.service.CertificateImportService;
import com.example.fsa_gov.service.NormalizeCheckService;
import com.example.fsa_gov.service.job.ImportJobState;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/certificates/import")
@Tag(name = "Импорт сертификатов")
public class CertificateImportController {

    private final CertificateImportService importService;
    private final NormalizeCheckService normalizeCheckService;

    @PostMapping("/parse")
    @Operation(summary = "Распарсить список без проверки")
    public ResponseEntity<ParseResult> parse(@RequestBody List<String> lines) {
        return ResponseEntity.ok(importService.parseLines(lines));
    }

    @PostMapping("/start-async")
    @Operation(summary = "Запустить проверку асинхронно (возвращает jobId)")
    public ResponseEntity<Map<String, String>> startAsync(@RequestBody List<String> lines) {
        ParseResult parsed = importService.parseLines(lines);
        String jobId = importService.startImportAsync(parsed);
        return ResponseEntity.ok(Map.of("jobId", jobId));
    }

    @GetMapping("/job/{jobId}")
    @Operation(summary = "Статус и результат задачи")
    public ResponseEntity<ImportJobState> getJob(@PathVariable String jobId) {
        return importService.getJob(jobId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * СИНХРОННЫЙ импорт: ждёт завершения и сразу возвращает готовый результат.
     * Удобно для быстрой проверки списка номеров.
     */
    @PostMapping("/run-sync")
    @Operation(summary = "Синхронная проверка списка (ждёт завершения)")
    public ResponseEntity<ImportJobState> runSync(@RequestBody List<String> lines) {
        return ResponseEntity.ok(importService.runSync(lines));
    }


        /**
         * Диагностический endpoint: сравнить результаты импорта «как есть»
         * и после нормализации номеров.
         *
         * Возвращает сводку:
         *  - сколько найдено до нормализации;
         *  - сколько найдено после;
         *  - список изменённых номеров (было → стало);
         *  - полные списки найденных / не найденных по каждому прогону.
         */
        @PostMapping("/normalize-check")
        @Operation(
                summary = "Проверить нормализацию на списке номеров",
                description = """
                    Прогоняет один и тот же список дважды:
                    1) как есть;
                    2) после нормализации (латиница → кириллица, чистка разделителей).

                    Возвращает сравнение: сколько номеров нашлось до и после,
                    какие номера были изменены, полные списки найденных/не найденных.
                    """
        )
        public ResponseEntity<Map<String, Object>> normalizeCheck(
                @RequestBody List<String> lines) {
            return ResponseEntity.ok(normalizeCheckService.check(lines));
        }
}