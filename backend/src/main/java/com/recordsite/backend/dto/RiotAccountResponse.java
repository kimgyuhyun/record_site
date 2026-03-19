package com.recordsite.backend.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class RiotAccountResponse {
    private String puuid;
    private String gameName;
    private String tagLine;
}
