package com.techshop.backend.entity;

import com.techshop.backend.enums.EmailStatus;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "email_logs")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "campaign_id")
    private EmailCampaign campaign;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false)
    private String recipientEmail;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private EmailStatus status = EmailStatus.QUEUED;

    @Column(columnDefinition = "TEXT")
    private String errorMessage;

    @Column(unique = true, nullable = false)
    private String trackingToken;

    @Builder.Default
    private boolean opened = false;
    
    private LocalDateTime openedAt;

    @Builder.Default
    private boolean clicked = false;
    
    private LocalDateTime clickedAt;

    @Builder.Default
    private boolean converted = false;
    
    private LocalDateTime convertedAt;

    private LocalDateTime sentAt;
}
