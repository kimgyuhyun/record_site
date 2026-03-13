//package com.recordsite.backend.config;
//
//import com.recordsite.backend.service.DataDragonService;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.boot.CommandLineRunner;
//import org.springframework.stereotype.Component;
//
//@Slf4j
//@Component
//@RequiredArgsConstructor
//public class DataInitRunner implements CommandLineRunner {
//
//    private final DataDragonService dataDragonService;
//
//    @Override
//    public void run(String... args) {
//        log.info("=== 챔피언 데이터 초기화 시작 ===");
//        dataDragonService.fetchAndSaveAllChampions();
//        log.info("=== 챔피언 데이터 초기화 완료 ===");
//
//        log.info("=== 아이템 데이터 초기화 시작 ===");
//        dataDragonService.fetchAndSaveAllItems();
//        log.info("=== 아이템 데이터 초기화 완료 ===");
//
//        log.info("=== 룬 데이터 초기화 시작 ===");
//        dataDragonService.fetchAndSaveAllRunes();
//        log.info("=== 룬 데이터 초기화 완료 ===");
//    }
//}
