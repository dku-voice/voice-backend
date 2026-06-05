package com.dku.voice.voice_backend.repository;

import com.dku.voice.voice_backend.entity.Menu;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface MenuRepository extends JpaRepository<Menu, Long> {

    /**
     * 전체 활성 메뉴 조회 (MenuCacheService.getAllMenus()에서 사용)
     * - status = ACTIVE인 메뉴만 조회
     * - Category, Allergen, Nutrient, MenuOption FETCH JOIN (N+1 방지)
     * - 카테고리 sortOrder 순 정렬
     */
    @Query("""
        SELECT DISTINCT m
        FROM Menu m
        JOIN FETCH m.category c
        LEFT JOIN FETCH m.menuAllergens ma
        LEFT JOIN FETCH ma.allergen
        LEFT JOIN FETCH m.menuNutrient
        LEFT JOIN FETCH m.menuOptions
        WHERE m.status = 'ACTIVE'
        ORDER BY c.sortOrder ASC
        """)
    List<Menu> findAllActiveWithDetails();

    /**
     * 카테고리별 활성 메뉴 조회 (MenuCacheService.getMenusByCategory()에서 사용)
     */
    @Query("""
        SELECT DISTINCT m
        FROM Menu m
        JOIN FETCH m.category c
        LEFT JOIN FETCH m.menuAllergens ma
        LEFT JOIN FETCH ma.allergen
        LEFT JOIN FETCH m.menuNutrient
        LEFT JOIN FETCH m.menuOptions
        WHERE m.status = 'ACTIVE'
          AND c.id = :categoryId
        ORDER BY c.sortOrder ASC
        """)
    List<Menu> findActiveByCategoryId(@Param("categoryId") Long categoryId);
}