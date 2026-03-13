package com.recordsite.backend.dto;

import com.recordsite.backend.entity.Rune;
import com.recordsite.backend.entity.RunePath;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RuneDtoTest {

    @Test
    void CopyPathKeyFromRunePathTest() {

        RunePath runePath = new RunePath();
        runePath.setPathKey(1);

        Rune rune = new Rune();
        rune.setPath(runePath);
        rune.setRuneNameKor("감전");

        RuneDto runeDto = RuneDto.from(rune);

        assertEquals(1, runeDto.getPathKey());
        assertEquals("감전", runeDto.getRuneNameKor());
    }

}