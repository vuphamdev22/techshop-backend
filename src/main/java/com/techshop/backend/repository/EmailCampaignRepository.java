package com.techshop.backend.repository;

import com.techshop.backend.entity.EmailCampaign;
import com.techshop.backend.enums.CampaignStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface EmailCampaignRepository extends JpaRepository<EmailCampaign, Long> {
    List<EmailCampaign> findByStatusAndScheduledAtBefore(CampaignStatus status, LocalDateTime time);
}
