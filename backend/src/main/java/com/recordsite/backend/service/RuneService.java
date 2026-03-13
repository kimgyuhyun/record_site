package com.recordsite.backend.service;

import com.recordsite.backend.dto.RuneDto;
import com.recordsite.backend.dto.RunePathDto;
import com.recordsite.backend.entity.Rune;
import com.recordsite.backend.entity.RunePath;
import com.recordsite.backend.repository.RunePathRepository;
import com.recordsite.backend.repository.RuneRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RuneService {

    private final RunePathRepository runePathRepository;
    private final RuneRepository runeRepository;


    public List<RunePathDto> findAllRunePathList() {
        List<RunePathDto> runePathDtoList = new ArrayList<>();
        List<RunePath> runePathList = runePathRepository.findAll();
        for (RunePath path : runePathList) {
            runePathDtoList.add(RunePathDto.from(path));
        }
        return runePathDtoList;
    }

    public List<RuneDto> findAllRuneList() {
        List<RuneDto> runeDtoList = new ArrayList<>();
        List<Rune> runeList = runeRepository.findAll();

        for (Rune rune : runeList) {
            runeDtoList.add(RuneDto.from(rune));
        }

        return runeDtoList;
    }
}
