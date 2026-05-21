package com.dku.voice.voice_backend.entity;
 
import jakarta.persistence.*;
import lombok.*;
 
@Entity
@Table(name = "allergen")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Allergen {
 
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
 
    @Column(name = "name_ko", nullable = false, length = 50)
    private String nameKo;
 
    @Column(name = "name_en", nullable = false, length = 50)
    private String nameEn;
}
 
