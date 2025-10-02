package com.interexport.guarantees.cqrs.command;

import com.interexport.guarantees.cqrs.event.GuaranteeCreatedEvent;
import com.interexport.guarantees.entity.GuaranteeContract;
import com.interexport.guarantees.entity.enums.GuaranteeStatus;
import com.interexport.guarantees.entity.enums.GuaranteeType;
import com.interexport.guarantees.repository.GuaranteeContractRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Command Handler for Guarantee operations
 * Handles write operations and publishes events
 */
@Component
public class GuaranteeCommandHandler {
    
    private final GuaranteeContractRepository guaranteeRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    
    @Autowired
    public GuaranteeCommandHandler(GuaranteeContractRepository guaranteeRepository, 
                                 KafkaTemplate<String, Object> kafkaTemplate) {
        this.guaranteeRepository = guaranteeRepository;
        this.kafkaTemplate = kafkaTemplate;
    }
    
    @Transactional
    public String handle(CreateGuaranteeCommand command) {
        // Business logic validation
        validateCommand(command);
        
        // Create guarantee entity
        GuaranteeContract guarantee = new GuaranteeContract();
        guarantee.setReference(command.getReference());
        guarantee.setGuaranteeType(GuaranteeType.valueOf(command.getGuaranteeType()));
        guarantee.setAmount(command.getAmount());
        guarantee.setCurrency(command.getCurrency());
        guarantee.setIssueDate(command.getIssueDate());
        guarantee.setExpiryDate(command.getExpiryDate());
        guarantee.setBeneficiaryName(command.getBeneficiaryName());
        guarantee.setApplicantId(command.getApplicantId());
        guarantee.setGuaranteeText(command.getGuaranteeText());
        guarantee.setLanguage(command.getLanguage());
        guarantee.setStatus(GuaranteeStatus.DRAFT);
        guarantee.setCreatedDate(LocalDateTime.now());
        guarantee.setLastModifiedDate(LocalDateTime.now());
        
        // Save to command database
        GuaranteeContract savedGuarantee = guaranteeRepository.save(guarantee);
        
        // Publish event to Kafka
        GuaranteeCreatedEvent event = new GuaranteeCreatedEvent(
            savedGuarantee.getId().toString(),
            savedGuarantee.getReference(),
            savedGuarantee.getGuaranteeType().name(),
            savedGuarantee.getAmount(),
            savedGuarantee.getCurrency(),
            savedGuarantee.getIssueDate(),
            savedGuarantee.getExpiryDate(),
            savedGuarantee.getBeneficiaryName(),
            savedGuarantee.getApplicantId(),
            savedGuarantee.getGuaranteeText(),
            savedGuarantee.getLanguage(),
            savedGuarantee.getStatus().name(),
            savedGuarantee.getCreatedDate()
        );
        
        kafkaTemplate.send("guarantee-created", savedGuarantee.getId().toString(), event);
        
        return savedGuarantee.getId().toString();
    }
    
    private void validateCommand(CreateGuaranteeCommand command) {
        if (command.getReference() == null || command.getReference().trim().isEmpty()) {
            throw new IllegalArgumentException("Guarantee reference is required");
        }
        if (command.getAmount() == null || command.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Guarantee amount must be positive");
        }
        if (command.getExpiryDate() != null && command.getIssueDate() != null && 
            command.getExpiryDate().isBefore(command.getIssueDate())) {
            throw new IllegalArgumentException("Expiry date cannot be before issue date");
        }
    }
}
