package com.interexport.guarantees.repository;

import com.interexport.guarantees.entity.GuaranteeTemplate;
import com.interexport.guarantees.entity.enums.GuaranteeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for GuaranteeTemplate entities
 * Supports F2 - Template engine for guarantee text generation
 * SIMPLIFIED VERSION - Complex queries disabled until SWIFT integration
 */
@Repository
public interface GuaranteeTemplateRepository extends JpaRepository<GuaranteeTemplate, Long> {

    /**
     * Find all active templates ordered by type and language
     */
    @Query("SELECT t FROM GuaranteeTemplate t WHERE t.isActive = true ORDER BY t.guaranteeType, t.language")
    List<GuaranteeTemplate> findAllActiveTemplatesOrderByTypeAndLanguage();

    /**
     * Find active templates by guarantee type
     */
    @Query("SELECT t FROM GuaranteeTemplate t WHERE t.guaranteeType = :guaranteeType AND t.isActive = true ORDER BY t.language, t.priority DESC")
    List<GuaranteeTemplate> findActiveTemplatesByType(@Param("guaranteeType") GuaranteeType guaranteeType);

    /**
     * Find active templates by guarantee type and language
     */
    @Query("SELECT t FROM GuaranteeTemplate t WHERE t.guaranteeType = :guaranteeType AND t.language = :language AND t.isActive = true ORDER BY t.priority DESC")
    List<GuaranteeTemplate> findActiveTemplatesByTypeAndLanguage(
            @Param("guaranteeType") GuaranteeType guaranteeType,
            @Param("language") String language);

    /**
     * Find template by guarantee type and language - SIMPLIFIED
     * SWIFT field parameters removed for now
     */
    @Query("SELECT t FROM GuaranteeTemplate t WHERE " +
           "t.guaranteeType = :guaranteeType AND " +
           "t.language = :language AND " +
           "t.isActive = true " +
           "ORDER BY t.priority DESC, t.createdDate DESC")
    List<GuaranteeTemplate> findBestMatchingTemplate(
            @Param("guaranteeType") GuaranteeType guaranteeType,
            @Param("language") String language);

    /**
     * Find default template for guarantee type and language
     */
    @Query("SELECT t FROM GuaranteeTemplate t WHERE " +
           "t.guaranteeType = :guaranteeType AND " +
           "t.language = :language AND " +
           "t.isActive = true AND " +
           "t.isDefault = true " +
           "ORDER BY t.priority DESC")
    Optional<GuaranteeTemplate> findDefaultTemplate(
            @Param("guaranteeType") GuaranteeType guaranteeType,
            @Param("language") String language);

    /**
     * Find template by ID and ensure it's active
     */
    @Query("SELECT t FROM GuaranteeTemplate t WHERE t.id = :id AND t.isActive = true")
    Optional<GuaranteeTemplate> findActiveTemplateById(@Param("id") Long id);
}