package com.example.fsa_gov.dto.async;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AsyncFindDocRequest {
    private List<FindDocItem> items;
}