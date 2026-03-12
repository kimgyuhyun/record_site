//package com.recordsite.backend.service;
//
//
//import com.recordsite.backend.dto.ChampionSummaryDto;
//import com.recordsite.backend.entity.Champion;
//import com.recordsite.backend.repository.ChampionRepository;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.junit.jupiter.MockitoExtension;
//import static org.assertj.core.api.Assertions.assertThat;
//
//import java.util.List;
//
//import static org.junit.jupiter.api.Assertions.*;
//
//@ExtendWith(MockitoExtension.class)
//class ChampionServiceTest {
//    @Mock
//            ChampionRepository championRepository;
//
//    @InjectMocks
//            ChampionService championService;
//
//
//
//    @Test
//    void getEmptyChampionListTest() {
//       List<ChampionDto> championDtoList = championService.getChampionList();
//       assertThat(championDtoList).isEmpty();
//    }
//
//    @Test
//    void getChampionListTest() {
//
//    }
//
//
//
//}