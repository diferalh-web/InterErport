package com.interexport.guarantees.service;

import com.interexport.guarantees.entity.GuaranteeTemplate;
import com.interexport.guarantees.entity.enums.GuaranteeType;
import com.interexport.guarantees.repository.GuaranteeTemplateRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

/**
 * Service to initialize default guarantee templates on application startup
 * Implements F2 - Template Engine with sample templates
 */
@Service
@Order(2) // Run after basic data initialization
public class TemplateDataInitializationService implements ApplicationRunner {

    private final GuaranteeTemplateRepository templateRepository;

    @Autowired
    public TemplateDataInitializationService(GuaranteeTemplateRepository templateRepository) {
        this.templateRepository = templateRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) throws Exception {
        if (templateRepository.count() == 0) {
            initializeDefaultTemplates();
            System.out.println("✅ Initialized default guarantee templates");
        } else {
            System.out.println("ℹ️ Template data already exists, skipping initialization");
        }
    }

    private void initializeDefaultTemplates() {
        List<GuaranteeTemplate> defaultTemplates = Arrays.asList(
            createPerformanceGuaranteeTemplate(),
            createAdvancePaymentGuaranteeTemplate(),
            createBidBondTemplate(),
            createMaintenanceGuaranteeTemplate(),
            createCustomsGuaranteeTemplate(),
            createWarrantyGuaranteeTemplate(),
            
            // Spanish templates
            createPerformanceGuaranteeTemplateSpanish(),
            createAdvancePaymentGuaranteeTemplateSpanish(),
            createBidBondTemplateSpanish()
        );

        templateRepository.saveAll(defaultTemplates);
    }

    /**
     * Performance Guarantee Template - English
     */
    private GuaranteeTemplate createPerformanceGuaranteeTemplate() {
        GuaranteeTemplate template = new GuaranteeTemplate();
        template.setName("Standard Performance Guarantee");
        template.setGuaranteeType(GuaranteeType.PERFORMANCE);
        template.setLanguage("EN");
        template.setIsDefault(true);
        template.setPriority(100);
        template.setIsDomestic(null); // Applicable to both
        template.setDescription("Standard performance guarantee template for international and domestic use");
        
        template.setTemplateContent(
            "PERFORMANCE GUARANTEE\n\n" +
            "Guarantee Reference: {{GUARANTEE_REFERENCE}}\n" +
            "Issue Date: {{ISSUE_DATE}}\n" +
            "Expiry Date: {{EXPIRY_DATE}}\n\n" +
            "TO: {{BENEFICIARY_NAME}}\n" +
            "    {{BENEFICIARY_ADDRESS}}\n\n" +
            "DEAR SIRS,\n\n" +
            "We hereby irrevocably undertake to pay to you any amount or amounts not exceeding in total " +
            "{{CURRENCY}} {{GUARANTEE_AMOUNT}} ({{GUARANTEE_AMOUNT_WORDS}}) upon receipt by us of your first " +
            "written demand accompanied by your written statement that the applicant has failed to fulfill " +
            "their contractual obligations under the underlying contract.\n\n" +
            "Your demand must indicate in what respect the applicant has failed to fulfill their obligations.\n\n" +
            "This guarantee shall expire on {{EXPIRY_DATE}} or upon receipt by us of your written release, " +
            "whichever occurs first.\n\n" +
            "This guarantee is subject to the Uniform Rules for Demand Guarantees (URDG 758) of the " +
            "International Chamber of Commerce.\n\n" +
            "INTEREXPORT BANK\n" +
            "Guarantees Department\n\n" +
            "{{#IS_DOMESTIC}}" +
            "This is a domestic guarantee issued in accordance with local banking regulations." +
            "{{/IS_DOMESTIC}}"
        );
        
        template.setRequiredVariables(
            "GUARANTEE_REFERENCE,GUARANTEE_AMOUNT,GUARANTEE_AMOUNT_WORDS,CURRENCY," +
            "ISSUE_DATE,EXPIRY_DATE,BENEFICIARY_NAME"
        );
        
        template.setOptionalVariables("BENEFICIARY_ADDRESS,IS_DOMESTIC");
        template.setIsActive(true);
        
        return template;
    }

    /**
     * Advance Payment Guarantee Template - English
     */
    private GuaranteeTemplate createAdvancePaymentGuaranteeTemplate() {
        GuaranteeTemplate template = new GuaranteeTemplate();
        template.setName("Standard Advance Payment Guarantee");
        template.setGuaranteeType(GuaranteeType.ADVANCE_PAYMENT);
        template.setLanguage("EN");
        template.setIsDefault(true);
        template.setPriority(100);
        template.setDescription("Standard advance payment guarantee template");
        
        template.setTemplateContent(
            "ADVANCE PAYMENT GUARANTEE\n\n" +
            "Guarantee Reference: {{GUARANTEE_REFERENCE}}\n" +
            "Issue Date: {{ISSUE_DATE}}\n" +
            "Expiry Date: {{EXPIRY_DATE}}\n\n" +
            "TO: {{BENEFICIARY_NAME}}\n" +
            "    {{BENEFICIARY_ADDRESS}}\n\n" +
            "DEAR SIRS,\n\n" +
            "We refer to the contract between yourselves as buyers and {{APPLICANT_NAME}} as sellers.\n\n" +
            "We hereby irrevocably undertake to pay to you any amount or amounts not exceeding in total " +
            "{{CURRENCY}} {{GUARANTEE_AMOUNT}} ({{GUARANTEE_AMOUNT_WORDS}}) upon receipt by us of your first " +
            "written demand accompanied by your written statement that:\n\n" +
            "1. You have paid an advance payment to the sellers under the contract, and\n" +
            "2. The sellers have failed to fulfill their contractual obligations and you are therefore " +
            "entitled to repayment of the advance payment.\n\n" +
            "The amount of this guarantee shall be progressively reduced by the amount of any partial " +
            "shipments made under the contract when evidenced by presentation to us of copies of clean " +
            "on board bills of lading or equivalent transport documents.\n\n" +
            "This guarantee shall expire on {{EXPIRY_DATE}} or upon receipt by us of your written release, " +
            "whichever occurs first.\n\n" +
            "This guarantee is subject to the Uniform Rules for Demand Guarantees (URDG 758) of the " +
            "International Chamber of Commerce.\n\n" +
            "INTEREXPORT BANK\n" +
            "Guarantees Department"
        );
        
        template.setRequiredVariables(
            "GUARANTEE_REFERENCE,GUARANTEE_AMOUNT,GUARANTEE_AMOUNT_WORDS,CURRENCY," +
            "ISSUE_DATE,EXPIRY_DATE,BENEFICIARY_NAME,APPLICANT_NAME"
        );
        
        template.setOptionalVariables("BENEFICIARY_ADDRESS");
        template.setIsActive(true);
        
        return template;
    }

    /**
     * Bid Bond Template - English
     */
    private GuaranteeTemplate createBidBondTemplate() {
        GuaranteeTemplate template = new GuaranteeTemplate();
        template.setName("Standard Bid Bond");
        template.setGuaranteeType(GuaranteeType.BID_BOND);
        template.setLanguage("EN");
        template.setIsDefault(true);
        template.setPriority(100);
        template.setDescription("Standard bid bond template for tenders and proposals");
        
        template.setTemplateContent(
            "BID BOND\n\n" +
            "Bond Reference: {{GUARANTEE_REFERENCE}}\n" +
            "Issue Date: {{ISSUE_DATE}}\n" +
            "Expiry Date: {{EXPIRY_DATE}}\n\n" +
            "TO: {{BENEFICIARY_NAME}}\n" +
            "    {{BENEFICIARY_ADDRESS}}\n\n" +
            "DEAR SIRS,\n\n" +
            "We have been informed that {{APPLICANT_NAME}} (hereinafter called 'the Contractor') " +
            "intends to submit their tender dated {{TENDER_DATE}} for {{PROJECT_DESCRIPTION}} " +
            "(hereinafter called 'the Tender').\n\n" +
            "Furthermore, we understand that, according to your conditions, tenders must be supported " +
            "by a bid bond.\n\n" +
            "At the request of the Contractor, we hereby irrevocably undertake to pay you any amount " +
            "not exceeding {{CURRENCY}} {{GUARANTEE_AMOUNT}} ({{GUARANTEE_AMOUNT_WORDS}}) upon receipt " +
            "by us of your first written demand accompanied by a written statement that:\n\n" +
            "- The Contractor has withdrawn their tender during the period of tender validity, or\n" +
            "- The Contractor, having been notified of the acceptance of their tender by you during " +
            "the period of tender validity, has failed to enter into a contract in accordance with " +
            "their tender, or\n" +
            "- The Contractor has failed to provide the required performance security.\n\n" +
            "This bond shall expire on {{EXPIRY_DATE}} or upon receipt by us of your written release, " +
            "whichever occurs first.\n\n" +
            "This bond is subject to the Uniform Rules for Demand Guarantees (URDG 758) of the " +
            "International Chamber of Commerce.\n\n" +
            "INTEREXPORT BANK\n" +
            "Guarantees Department"
        );
        
        template.setRequiredVariables(
            "GUARANTEE_REFERENCE,GUARANTEE_AMOUNT,GUARANTEE_AMOUNT_WORDS,CURRENCY," +
            "ISSUE_DATE,EXPIRY_DATE,BENEFICIARY_NAME,APPLICANT_NAME"
        );
        
        template.setOptionalVariables("BENEFICIARY_ADDRESS,TENDER_DATE,PROJECT_DESCRIPTION");
        template.setIsActive(true);
        
        return template;
    }

    /**
     * Maintenance Guarantee Template - English
     */
    private GuaranteeTemplate createMaintenanceGuaranteeTemplate() {
        GuaranteeTemplate template = new GuaranteeTemplate();
        template.setName("Standard Maintenance Guarantee");
        template.setGuaranteeType(GuaranteeType.WARRANTY);
        template.setLanguage("EN");
        template.setIsDefault(true);
        template.setPriority(100);
        template.setDescription("Standard maintenance/warranty guarantee template");
        
        template.setTemplateContent(
            "MAINTENANCE GUARANTEE\n\n" +
            "Guarantee Reference: {{GUARANTEE_REFERENCE}}\n" +
            "Issue Date: {{ISSUE_DATE}}\n" +
            "Expiry Date: {{EXPIRY_DATE}}\n\n" +
            "TO: {{BENEFICIARY_NAME}}\n" +
            "    {{BENEFICIARY_ADDRESS}}\n\n" +
            "DEAR SIRS,\n\n" +
            "We refer to the contract for {{PROJECT_DESCRIPTION}} between yourselves as employer and " +
            "{{APPLICANT_NAME}} as contractor.\n\n" +
            "We hereby irrevocably undertake to pay to you any amount or amounts not exceeding in total " +
            "{{CURRENCY}} {{GUARANTEE_AMOUNT}} ({{GUARANTEE_AMOUNT_WORDS}}) upon receipt by us of your first " +
            "written demand accompanied by your written statement that the contractor has failed to remedy " +
            "defects in the works or has failed to carry out maintenance of the works in accordance with " +
            "the contract during the maintenance period.\n\n" +
            "This guarantee covers the maintenance period from {{MAINTENANCE_START_DATE}} to {{EXPIRY_DATE}}.\n\n" +
            "This guarantee shall expire on {{EXPIRY_DATE}} or upon receipt by us of your written release, " +
            "whichever occurs first.\n\n" +
            "This guarantee is subject to the Uniform Rules for Demand Guarantees (URDG 758) of the " +
            "International Chamber of Commerce.\n\n" +
            "INTEREXPORT BANK\n" +
            "Guarantees Department"
        );
        
        template.setRequiredVariables(
            "GUARANTEE_REFERENCE,GUARANTEE_AMOUNT,GUARANTEE_AMOUNT_WORDS,CURRENCY," +
            "ISSUE_DATE,EXPIRY_DATE,BENEFICIARY_NAME,APPLICANT_NAME"
        );
        
        template.setOptionalVariables("BENEFICIARY_ADDRESS,PROJECT_DESCRIPTION,MAINTENANCE_START_DATE");
        template.setIsActive(true);
        
        return template;
    }

    /**
     * Customs Guarantee Template - English
     */
    private GuaranteeTemplate createCustomsGuaranteeTemplate() {
        GuaranteeTemplate template = new GuaranteeTemplate();
        template.setName("Standard Customs Guarantee");
        template.setGuaranteeType(GuaranteeType.CUSTOMS);
        template.setLanguage("EN");
        template.setIsDefault(true);
        template.setPriority(100);
        template.setDescription("Standard customs guarantee for import/export duties");
        
        template.setTemplateContent(
            "CUSTOMS GUARANTEE\n\n" +
            "Guarantee Reference: {{GUARANTEE_REFERENCE}}\n" +
            "Issue Date: {{ISSUE_DATE}}\n" +
            "Expiry Date: {{EXPIRY_DATE}}\n\n" +
            "TO: {{CUSTOMS_AUTHORITY}}\n" +
            "    {{BENEFICIARY_ADDRESS}}\n\n" +
            "DEAR SIRS,\n\n" +
            "We refer to the customs declaration(s) to be submitted by {{APPLICANT_NAME}} " +
            "for the importation/exportation of goods.\n\n" +
            "At the request of the declarant, we hereby irrevocably undertake to pay to you any amount " +
            "or amounts not exceeding in total {{CURRENCY}} {{GUARANTEE_AMOUNT}} ({{GUARANTEE_AMOUNT_WORDS}}) " +
            "in respect of customs duties, taxes, and other charges which may become due in connection " +
            "with the above-mentioned customs operations.\n\n" +
            "This guarantee covers customs declaration reference: {{CUSTOMS_REFERENCE}}\n\n" +
            "This guarantee shall expire on {{EXPIRY_DATE}} or upon receipt by us of your written " +
            "confirmation that all obligations have been fulfilled, whichever occurs first.\n\n" +
            "This guarantee is governed by local customs regulations and banking practices.\n\n" +
            "INTEREXPORT BANK\n" +
            "Guarantees Department"
        );
        
        template.setRequiredVariables(
            "GUARANTEE_REFERENCE,GUARANTEE_AMOUNT,GUARANTEE_AMOUNT_WORDS,CURRENCY," +
            "ISSUE_DATE,EXPIRY_DATE,CUSTOMS_AUTHORITY,APPLICANT_NAME"
        );
        
        template.setOptionalVariables("BENEFICIARY_ADDRESS,CUSTOMS_REFERENCE");
        template.setIsActive(true);
        
        return template;
    }

    /**
     * Warranty Guarantee Template - English
     */
    private GuaranteeTemplate createWarrantyGuaranteeTemplate() {
        GuaranteeTemplate template = new GuaranteeTemplate();
        template.setName("Standard Warranty Guarantee");
        template.setGuaranteeType(GuaranteeType.WARRANTY);
        template.setLanguage("EN");
        template.setIsDefault(true);
        template.setPriority(100);
        template.setDescription("Standard warranty guarantee for equipment and services");
        
        template.setTemplateContent(
            "WARRANTY GUARANTEE\n\n" +
            "Guarantee Reference: {{GUARANTEE_REFERENCE}}\n" +
            "Issue Date: {{ISSUE_DATE}}\n" +
            "Expiry Date: {{EXPIRY_DATE}}\n\n" +
            "TO: {{BENEFICIARY_NAME}}\n" +
            "    {{BENEFICIARY_ADDRESS}}\n\n" +
            "DEAR SIRS,\n\n" +
            "We refer to the supply contract for {{EQUIPMENT_DESCRIPTION}} between yourselves as buyer " +
            "and {{APPLICANT_NAME}} as supplier, dated {{CONTRACT_DATE}}.\n\n" +
            "We hereby irrevocably undertake to pay to you any amount or amounts not exceeding in total " +
            "{{CURRENCY}} {{GUARANTEE_AMOUNT}} ({{GUARANTEE_AMOUNT_WORDS}}) upon receipt by us of your first " +
            "written demand accompanied by your written statement that the supplier has failed to remedy " +
            "defects in the supplied equipment or has failed to fulfill warranty obligations in accordance " +
            "with the contract.\n\n" +
            "This guarantee covers the warranty period from {{WARRANTY_START_DATE}} to {{EXPIRY_DATE}}.\n\n" +
            "Your demand must be accompanied by evidence of the defect and your request to the supplier " +
            "to remedy the defect.\n\n" +
            "This guarantee shall expire on {{EXPIRY_DATE}} or upon receipt by us of your written release, " +
            "whichever occurs first.\n\n" +
            "This guarantee is subject to the Uniform Rules for Demand Guarantees (URDG 758) of the " +
            "International Chamber of Commerce.\n\n" +
            "INTEREXPORT BANK\n" +
            "Guarantees Department"
        );
        
        template.setRequiredVariables(
            "GUARANTEE_REFERENCE,GUARANTEE_AMOUNT,GUARANTEE_AMOUNT_WORDS,CURRENCY," +
            "ISSUE_DATE,EXPIRY_DATE,BENEFICIARY_NAME,APPLICANT_NAME"
        );
        
        template.setOptionalVariables(
            "BENEFICIARY_ADDRESS,EQUIPMENT_DESCRIPTION,CONTRACT_DATE,WARRANTY_START_DATE"
        );
        template.setIsActive(true);
        
        return template;
    }

    // Spanish Templates

    /**
     * Performance Guarantee Template - Spanish
     */
    private GuaranteeTemplate createPerformanceGuaranteeTemplateSpanish() {
        GuaranteeTemplate template = new GuaranteeTemplate();
        template.setName("Garantía de Cumplimiento Estándar");
        template.setGuaranteeType(GuaranteeType.PERFORMANCE);
        template.setLanguage("ES");
        template.setIsDefault(true);
        template.setPriority(100);
        template.setDescription("Plantilla estándar de garantía de cumplimiento");
        
        template.setTemplateContent(
            "GARANTÍA DE CUMPLIMIENTO\n\n" +
            "Referencia de Garantía: {{GUARANTEE_REFERENCE}}\n" +
            "Fecha de Emisión: {{ISSUE_DATE}}\n" +
            "Fecha de Vencimiento: {{EXPIRY_DATE}}\n\n" +
            "PARA: {{BENEFICIARY_NAME}}\n" +
            "       {{BENEFICIARY_ADDRESS}}\n\n" +
            "ESTIMADOS SEÑORES:\n\n" +
            "Por medio de la presente nos comprometemos irrevocablemente a pagar cualquier cantidad o " +
            "cantidades que no excedan en total {{CURRENCY}} {{GUARANTEE_AMOUNT}} ({{GUARANTEE_AMOUNT_WORDS}}) " +
            "al recibo de su primera demanda escrita acompañada de su declaración escrita de que el " +
            "solicitante ha fallado en cumplir con sus obligaciones contractuales bajo el contrato subyacente.\n\n" +
            "Su demanda debe indicar en qué aspectos el solicitante ha fallado en cumplir con sus obligaciones.\n\n" +
            "Esta garantía vencerá el {{EXPIRY_DATE}} o al recibo de su liberación escrita, lo que ocurra primero.\n\n" +
            "Esta garantía está sujeta a las Reglas Uniformes para Garantías a Demanda (URDG 758) de la " +
            "Cámara de Comercio Internacional.\n\n" +
            "BANCO INTEREXPORT\n" +
            "Departamento de Garantías"
        );
        
        template.setRequiredVariables(
            "GUARANTEE_REFERENCE,GUARANTEE_AMOUNT,GUARANTEE_AMOUNT_WORDS,CURRENCY," +
            "ISSUE_DATE,EXPIRY_DATE,BENEFICIARY_NAME"
        );
        
        template.setOptionalVariables("BENEFICIARY_ADDRESS");
        template.setIsActive(true);
        
        return template;
    }

    /**
     * Advance Payment Guarantee Template - Spanish
     */
    private GuaranteeTemplate createAdvancePaymentGuaranteeTemplateSpanish() {
        GuaranteeTemplate template = new GuaranteeTemplate();
        template.setName("Garantía de Anticipo Estándar");
        template.setGuaranteeType(GuaranteeType.ADVANCE_PAYMENT);
        template.setLanguage("ES");
        template.setIsDefault(true);
        template.setPriority(100);
        template.setDescription("Plantilla estándar de garantía de anticipo");
        
        template.setTemplateContent(
            "GARANTÍA DE ANTICIPO\n\n" +
            "Referencia de Garantía: {{GUARANTEE_REFERENCE}}\n" +
            "Fecha de Emisión: {{ISSUE_DATE}}\n" +
            "Fecha de Vencimiento: {{EXPIRY_DATE}}\n\n" +
            "PARA: {{BENEFICIARY_NAME}}\n" +
            "       {{BENEFICIARY_ADDRESS}}\n\n" +
            "ESTIMADOS SEÑORES:\n\n" +
            "Nos referimos al contrato entre ustedes como compradores y {{APPLICANT_NAME}} como vendedores.\n\n" +
            "Por medio de la presente nos comprometemos irrevocablemente a pagar cualquier cantidad o " +
            "cantidades que no excedan en total {{CURRENCY}} {{GUARANTEE_AMOUNT}} ({{GUARANTEE_AMOUNT_WORDS}}) " +
            "al recibo de su primera demanda escrita acompañada de su declaración escrita de que:\n\n" +
            "1. Ustedes han pagado un anticipo a los vendedores bajo el contrato, y\n" +
            "2. Los vendedores han fallado en cumplir con sus obligaciones contractuales y por lo tanto " +
            "ustedes tienen derecho al reembolso del anticipo.\n\n" +
            "Esta garantía vencerá el {{EXPIRY_DATE}} o al recibo de su liberación escrita, lo que ocurra primero.\n\n" +
            "Esta garantía está sujeta a las Reglas Uniformes para Garantías a Demanda (URDG 758) de la " +
            "Cámara de Comercio Internacional.\n\n" +
            "BANCO INTEREXPORT\n" +
            "Departamento de Garantías"
        );
        
        template.setRequiredVariables(
            "GUARANTEE_REFERENCE,GUARANTEE_AMOUNT,GUARANTEE_AMOUNT_WORDS,CURRENCY," +
            "ISSUE_DATE,EXPIRY_DATE,BENEFICIARY_NAME,APPLICANT_NAME"
        );
        
        template.setOptionalVariables("BENEFICIARY_ADDRESS");
        template.setIsActive(true);
        
        return template;
    }

    /**
     * Bid Bond Template - Spanish
     */
    private GuaranteeTemplate createBidBondTemplateSpanish() {
        GuaranteeTemplate template = new GuaranteeTemplate();
        template.setName("Garantía de Oferta Estándar");
        template.setGuaranteeType(GuaranteeType.BID_BOND);
        template.setLanguage("ES");
        template.setIsDefault(true);
        template.setPriority(100);
        template.setDescription("Plantilla estándar de garantía de oferta para licitaciones");
        
        template.setTemplateContent(
            "GARANTÍA DE OFERTA\n\n" +
            "Referencia de Garantía: {{GUARANTEE_REFERENCE}}\n" +
            "Fecha de Emisión: {{ISSUE_DATE}}\n" +
            "Fecha de Vencimiento: {{EXPIRY_DATE}}\n\n" +
            "PARA: {{BENEFICIARY_NAME}}\n" +
            "       {{BENEFICIARY_ADDRESS}}\n\n" +
            "ESTIMADOS SEÑORES:\n\n" +
            "Hemos sido informados de que {{APPLICANT_NAME}} (en adelante llamado 'el Contratista') " +
            "tiene la intención de presentar su oferta fechada {{TENDER_DATE}} para {{PROJECT_DESCRIPTION}}.\n\n" +
            "Por medio de la presente nos comprometemos irrevocablemente a pagar cualquier cantidad " +
            "que no exceda {{CURRENCY}} {{GUARANTEE_AMOUNT}} ({{GUARANTEE_AMOUNT_WORDS}}) al recibo " +
            "de su primera demanda escrita acompañada de una declaración escrita de que:\n\n" +
            "- El Contratista ha retirado su oferta durante el período de validez de la oferta, o\n" +
            "- El Contratista, habiendo sido notificado de la aceptación de su oferta, ha fallado en " +
            "celebrar un contrato de acuerdo con su oferta.\n\n" +
            "Esta garantía vencerá el {{EXPIRY_DATE}} o al recibo de su liberación escrita, lo que ocurra primero.\n\n" +
            "BANCO INTEREXPORT\n" +
            "Departamento de Garantías"
        );
        
        template.setRequiredVariables(
            "GUARANTEE_REFERENCE,GUARANTEE_AMOUNT,GUARANTEE_AMOUNT_WORDS,CURRENCY," +
            "ISSUE_DATE,EXPIRY_DATE,BENEFICIARY_NAME,APPLICANT_NAME"
        );
        
        template.setOptionalVariables("BENEFICIARY_ADDRESS,TENDER_DATE,PROJECT_DESCRIPTION");
        template.setIsActive(true);
        
        return template;
    }
}
