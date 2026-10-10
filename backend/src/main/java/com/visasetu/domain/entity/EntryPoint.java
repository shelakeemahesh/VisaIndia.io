package com.visasetu.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "entry_points")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EntryPoint {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(length = 20)
    private String code;

    @Column(nullable = false, length = 50)
    private String type; // AIRPORT, SEAPORT, LAND_ICP

    @Column(nullable = false, length = 100)
    private String state;

    @Column(name = "is_evisa_entry", nullable = false)
    @Builder.Default
    private Boolean isEvisaEntry = true;

    @Column(name = "is_evisa_exit", nullable = false)
    @Builder.Default
    private Boolean isEvisaExit = true;

    @Column(name = "is_voa_allowed", nullable = false)
    @Builder.Default
    private Boolean isVoaAllowed = false;

    @Column(name = "official_source_url", nullable = false, length = 500)
    private String officialSourceUrl;
}
