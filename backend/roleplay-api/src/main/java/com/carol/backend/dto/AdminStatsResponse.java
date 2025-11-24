package com.carol.backend.dto;

import lombok.Data;
import java.util.List;

@Data
public class AdminStatsResponse {
    private Long userCount;
    private Long characterCount;
    private Long knowledgeCount;
    private List<CharacterKnowledgeStat> knowledgeDistribution;

    @Data
    public static class CharacterKnowledgeStat {
        private String characterName;
        private Long count;
    }
}
