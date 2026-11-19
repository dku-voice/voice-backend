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

    @Column(name = "name_ja", length = 100)
    private String nameJa;

    @Column(name = "name_es", length = 100)
    private String nameEs;

    @Column(name = "name_th", length = 100)
    private String nameTh;
}
 
