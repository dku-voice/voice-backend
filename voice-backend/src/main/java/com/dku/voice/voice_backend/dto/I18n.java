package com.dku.voice.voice_backend.dto;

/** 번역이 없으면 영어 이름으로 대체한다 (화면에 null이 나가지 않게). */
public final class I18n {

    private I18n() {}

    public static String or(String value, String fallback) {
        return (value == null || value.isBlank()) ? fallback : value;
    }
}