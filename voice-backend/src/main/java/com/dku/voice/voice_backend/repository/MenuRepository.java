package com.dku.voice.voice_backend.repository;

import com.dku.voice.voice_backend.entity.Menu;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MenuRepository extends JpaRepository<Menu, Long> {

    /**
     * 활성화된 메뉴 전체 조회.
     * - allergens, allergen (N+1 방지용 fetch join)
     * - nutrient는 LEFT JOIN (영양소 없는 메뉴도 포함)
     * - Master-Slave 환경에서는 @Transactional(readOnly=true) 를 Service에 적용
     */
    @Query("""
        SELECT DISTINCT m
        FROM Menu m
        JOIN FETCH m.category c
        LEFT JOIN FETCH m.allergens ma
        LEFT JOIN FETCH ma.allergen
        LEFT JOIN FETCH m.nutrient
        WHERE m.isActive = true
          AND c.isActive = true
        ORDER BY c.sortOrder ASC, m.sortOrder ASC
        """)
    List<Menu> findAllActiveWithDetails();

    /**
     * 카테고리 ID로 활성화된 메뉴 조회.
     */
    @Query("""
        SELECT DISTINCT m
        FROM Menu m
        JOIN FETCH m.category c
        LEFT JOIN FETCH m.allergens ma
        LEFT JOIN FETCH ma.allergen
        LEFT JOIN FETCH m.nutrient
        WHERE m.isActive = true
          AND c.isActive = true
          AND c.id = :categoryId
        ORDER BY m.sortOrder ASC
        """)
    List<Menu> findActiveByCategoryId(@Param("categoryId") Long categoryId);

    /** 단건 상세 조회 (RAG 동기화 API 등에서 활용) */
    @Query("""
        SELECT m
        FROM Menu m
        JOIN FETCH m.category
        LEFT JOIN FETCH m.allergens ma
        LEFT JOIN FETCH ma.allergen
        LEFT JOIN FETCH m.nutrient
        WHERE m.id = :id
        """)
    Optional<Menu> findByIdWithDetails(@Param("id") Long id);
}