package com.dku.voice.voice_backend;

import java.util.TimeZone;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class VoiceBackendApplication {

	public static void main(String[] args) {
		// 서버(컨테이너) 시간대와 관계없이 한국 시간 기준으로 동작하도록 고정
		TimeZone.setDefault(TimeZone.getTimeZone("Asia/Seoul"));
		SpringApplication.run(VoiceBackendApplication.class, args);
	}

}