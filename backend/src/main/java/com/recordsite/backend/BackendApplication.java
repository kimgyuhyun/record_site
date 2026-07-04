package com.recordsite.backend;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.TimeZone;

@SpringBootApplication
@EnableScheduling // 매치망 크롤러(SummonerCrawlService)의 @Scheduled 활성화
public class BackendApplication {

	// 서버가 어느 지역/타임존에 배포되든 저장·기준 시각을 UTC 로 통일한다.
	// LocalDateTime.now() 가 항상 UTC 벽시계를 쓰게 해, 로컬 개발(KST)과 운영(UTC)의 시각이 어긋나지 않게 한다.
	// (직렬화 시 오프셋 'Z' 를 붙이는 건 JacksonTimeConfig 가 담당.)
	@PostConstruct
	public void initDefaultTimeZoneToUtc() {
		TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
	}

	public static void main(String[] args) {
		SpringApplication.run(BackendApplication.class, args);
	}

}
