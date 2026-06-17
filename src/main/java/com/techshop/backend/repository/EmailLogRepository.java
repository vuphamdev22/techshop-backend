package com.techshop.backend.repository;

import com.techshop.backend.entity.EmailLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface EmailLogRepository extends JpaRepository<EmailLog, Long> {
    Optional<EmailLog> findByTrackingToken(String trackingToken);
    
    List<EmailLog> findByCampaignId(Long campaignId);
    
    @Query("SELECT COUNT(e) FROM EmailLog e WHERE e.campaign.id = :campaignId")
    long countByCampaignId(@Param("campaignId") Long campaignId);
    
    @Query("SELECT COUNT(e) FROM EmailLog e WHERE e.campaign.id = :campaignId AND e.opened = true")
    long countOpenedByCampaignId(@Param("campaignId") Long campaignId);
    
    @Query("SELECT COUNT(e) FROM EmailLog e WHERE e.campaign.id = :campaignId AND e.clicked = true")
    long countClickedByCampaignId(@Param("campaignId") Long campaignId);
    
    @Query("SELECT COUNT(e) FROM EmailLog e WHERE e.campaign.id = :campaignId AND e.converted = true")
    long countConvertedByCampaignId(@Param("campaignId") Long campaignId);
}
