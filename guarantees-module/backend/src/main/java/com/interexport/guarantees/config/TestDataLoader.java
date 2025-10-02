package com.interexport.guarantees.config;

import com.interexport.guarantees.entity.*;
import com.interexport.guarantees.entity.enums.*;
import com.interexport.guarantees.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Test data loader for Guarantees Module POC
 * Creates comprehensive sample data for demonstration purposes
 */
@Component
public class TestDataLoader implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(TestDataLoader.class);
    
    @Autowired
    private ClientRepository clientRepository;
    
    @Autowired
    private FxRateRepository fxRateRepository;
    
    @Autowired
    private GuaranteeContractRepository guaranteeRepository;
    
    @Autowired
    private FeeItemRepository feeItemRepository;
    
    @Autowired
    private BankRepository bankRepository;
    
    @Autowired
    private AccountRepository accountRepository;
    
    @Autowired
    private CommissionParameterRepository commissionParameterRepository;
    
    @Autowired
    private AmendmentRepository amendmentRepository;
    
    @Autowired
    private ClaimRepository claimRepository;
    
    private final Random random = new Random();

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        if (guaranteeRepository.count() == 0) {
            log.info("Loading test data...");
            
            // Load test data in order (dependencies first)
            List<Bank> banks = createBanks();
            List<Account> accounts = createAccounts(banks);
            List<CommissionParameter> commissionParams = createCommissionParameters();
            List<Client> clients = createClients();
            createFxRates();
            List<GuaranteeContract> guarantees = createGuaranteeContracts(clients);
            createFeeItems(guarantees);
            List<Amendment> amendments = createAmendments(guarantees);
            List<Claim> claims = createClaims(guarantees);
            
            log.info("Test data loading completed:");
            log.info("- {} banks created", banks.size());
            log.info("- {} accounts created", accounts.size());
            log.info("- {} commission parameters created", commissionParams.size());
            log.info("- {} clients created", clients.size());
            log.info("- {} FX rates created", fxRateRepository.count());
            log.info("- {} guarantees created", guarantees.size());
            log.info("- {} fee items created", feeItemRepository.count());
            log.info("- {} amendments created", amendments.size());
            log.info("- {} claims created", claims.size());
        }
    }

    private List<Client> createClients() {
        List<Client> clients = new ArrayList<>();
        
        // Create diverse client base
        String[][] clientData = {
            {"ACME001", "ACME Corporation", "john.smith@acme.com", "+1-555-0101", "123 Business Ave, New York, NY", "US"},
            {"GLOB002", "Global Trade Ltd", "m.garcia@globaltrade.com", "+1-555-0102", "456 Commerce St, Miami, FL", "US"},
            {"TECH003", "TechStart Inc", "d.chen@techstart.com", "+1-555-0103", "789 Innovation Way, San Francisco, CA", "US"},
            {"EURO004", "European Imports SA", "p.dubois@eurimports.com", "+33-1-4567-8901", "10 Rue de Commerce, Paris", "FR"},
            {"ASIA005", "Asian Manufacturing", "h.tanaka@asianmfg.com", "+81-3-1234-5678", "Tokyo Business Center", "JP"},
            {"LATN006", "Latin Export Co", "c.rodriguez@latexport.com", "+52-55-9876-5432", "Av. Reforma 100, Mexico City", "MX"},
            {"NORD007", "Nordic Trading AB", "l.anderson@nordic.se", "+46-8-555-1234", "Storgatan 15, Stockholm", "SE"},
            {"MIDL008", "Middle East Trading", "a.rashid@metrading.ae", "+971-4-123-4567", "Dubai International City", "AE"},
            {"AUST009", "Australian Exports", "s.wilson@ausexports.com.au", "+61-2-9876-5432", "Harbor Bridge Plaza, Sydney", "AU"},
            {"CANA010", "Canadian Resources", "r.macdonald@canresources.ca", "+1-416-555-7890", "Bay Street Tower, Toronto", "CA"},
            {"BRAZ011", "Brazilian Commodities", "i.santos@brazcommodities.com.br", "+55-11-9999-8888", "Av. Paulista 1000, São Paulo", "BR"},
            {"INDI012", "Indian Textiles Ltd", "r.patel@indiantextiles.in", "+91-22-7777-6666", "Mumbai Trade Center", "IN"},
            {"GERM013", "German Engineering", "k.mueller@germaneng.de", "+49-89-555-4321", "Maximilianstrasse 10, Munich", "DE"},
            {"ITAL014", "Italian Fashion House", "m.romano@italianfashion.it", "+39-02-1234-5678", "Via Montenapoleone, Milan", "IT"},
            {"SAFR015", "South African Mining", "n.mandela@samining.co.za", "+27-11-987-6543", "Johannesburg Mining District", "ZA"},
            {"RUSS016", "Russian Energy Corp", "v.petrov@rusenergy.ru", "+7-495-123-4567", "Red Square Business Center, Moscow", "RU"},
            {"KORE017", "Korean Electronics", "k.jongun@korelectronics.kr", "+82-2-555-1111", "Gangnam Business District, Seoul", "KR"},
            {"THAI018", "Thai Agriculture", "s.jaidee@thaiagriculture.th", "+66-2-333-4444", "Bangkok Trade Plaza", "TH"},
            {"EGYP019", "Egyptian Cotton Co", "m.hassan@egyptcotton.com.eg", "+20-2-555-7777", "Cairo Trade Center", "EG"},
            {"NIGE020", "Nigerian Oil & Gas", "c.okafor@nigerianpetrol.ng", "+234-1-888-9999", "Victoria Island, Lagos", "NG"}
        };

        for (String[] data : clientData) {
            Client client = new Client();
            client.setClientCode(data[0]);
            client.setName(data[1]);
            client.setEmail(data[2]);
            client.setPhone(data[3]);
            client.setAddress(data[4]);
            client.setCountryCode(data[5]);
            client.setIsActive(true);
            client.setRiskRating("A");
            client.setEntityType("COMPANY");
            client.setKycDate(LocalDate.now().minusDays(random.nextInt(365)));
            clients.add(client);
        }

        List<Client> savedClients = clientRepository.saveAll(clients);
        log.info("Saved {} clients, first client ID: {}", savedClients.size(), 
                 savedClients.isEmpty() ? "none" : savedClients.get(0).getId());
        return savedClients;
    }


    private void createFxRates() {
        String[][] rateData = {
            {"EUR", "1.0850", "ECB"},
            {"GBP", "1.2650", "BLOOMBERG"},
            {"JPY", "0.0067", "BLOOMBERG"},
            {"CAD", "0.7380", "BLOOMBERG"},
            {"CHF", "1.1020", "ECB"},
            {"AUD", "0.6540", "BLOOMBERG"},
            {"SEK", "0.0920", "ECB"},
            {"NOK", "0.0940", "MANUAL"},
            {"DKK", "0.1456", "ECB"},
            {"SGD", "0.7420", "BLOOMBERG"},
            {"HKD", "0.1280", "MANUAL"},
            {"CNY", "0.1380", "MANUAL"},
            {"INR", "0.0120", "MANUAL"},
            {"KRW", "0.000750", "MANUAL"},
            {"THB", "0.0275", "MANUAL"},
            {"MYR", "0.2130", "MANUAL"},
            {"IDR", "0.0000650", "MANUAL"},
            {"PHP", "0.0178", "MANUAL"},
            {"MXN", "0.0580", "MANUAL"},
            {"BRL", "0.2010", "MANUAL"},
            {"ARS", "0.00112", "MANUAL"},
            {"CLP", "0.00105", "MANUAL"},
            {"ZAR", "0.0530", "MANUAL"},
            {"RUB", "0.0108", "MANUAL"},
            {"PLN", "0.2450", "ECB"},
            {"CZK", "0.0435", "ECB"},
            {"HUF", "0.00275", "ECB"},
            {"TRY", "0.0340", "MANUAL"},
            {"EGP", "0.0325", "MANUAL"},
            {"NGN", "0.00065", "MANUAL"}
        };

        for (String[] rate : rateData) {
            FxRate fxRate = new FxRate();
            fxRate.setBaseCurrency(rate[0]);
            fxRate.setTargetCurrency("USD");
            fxRate.setRate(new BigDecimal(rate[1]));
            fxRate.setProvider(FxRateProvider.valueOf(rate[2]));
            fxRate.setEffectiveDate(LocalDate.now());
            fxRate.setIsActive(true);
            fxRateRepository.save(fxRate);
        }
    }

    private List<GuaranteeContract> createGuaranteeContracts(List<Client> clients) {
        List<GuaranteeContract> guarantees = new ArrayList<>();
        
        // Create diverse guarantees
        String[] currencies = {"USD", "EUR", "GBP", "JPY", "CAD", "AUD", "CHF", "SEK", "SGD", "HKD"};
        GuaranteeType[] types = GuaranteeType.values();
        GuaranteeStatus[] statuses = GuaranteeStatus.values();
        String[] beneficiaries = {
            "Ministry of Public Works", "Department of Transportation", "Central Bank Authority",
            "Port Authority", "Municipal Government", "State Construction Board",
            "Federal Trade Commission", "Export Development Bank", "Infrastructure Agency",
            "Energy Regulatory Authority", "Telecommunications Board", "Environmental Protection Agency",
            "Healthcare Authority", "Education Ministry", "Agriculture Department",
            "Mining Regulatory Board", "Tourism Development Authority", "Industrial Development Corp",
            "National Housing Authority", "Water Resources Commission"
        };

        for (int i = 0; i < 5; i++) { // Start with fewer guarantees for testing
            GuaranteeContract guarantee = new GuaranteeContract();
            
            Client client = clients.get(random.nextInt(clients.size()));
            
            log.info("Creating guarantee {} with client ID: {}", i+1, client.getId());
            
            // Generate realistic amounts based on guarantee type
            GuaranteeType type = types[random.nextInt(types.length)];
            BigDecimal baseAmount;
            switch (type) {
                case PERFORMANCE:
                    baseAmount = new BigDecimal(50000 + random.nextInt(950000)); // 50K-1M
                    break;
                case ADVANCE_PAYMENT:
                    baseAmount = new BigDecimal(25000 + random.nextInt(475000)); // 25K-500K
                    break;
                case BID_BOND:
                    baseAmount = new BigDecimal(10000 + random.nextInt(240000)); // 10K-250K
                    break;
                case WARRANTY:
                    baseAmount = new BigDecimal(15000 + random.nextInt(335000)); // 15K-350K
                    break;
                case PAYMENT:
                    baseAmount = new BigDecimal(30000 + random.nextInt(470000)); // 30K-500K
                    break;
                case CUSTOMS:
                    baseAmount = new BigDecimal(5000 + random.nextInt(95000)); // 5K-100K
                    break;
                case OTHER:
                default:
                    baseAmount = new BigDecimal(20000 + random.nextInt(280000)); // 20K-300K
                    break;
            }

            String currency = currencies[random.nextInt(currencies.length)];
            
            guarantee.setReference(generateReference(i + 1));
            guarantee.setGuaranteeType(type);
            guarantee.setAmount(baseAmount);
            guarantee.setCurrency(currency);
            guarantee.setIssueDate(LocalDate.now().minusDays(random.nextInt(180)));
            guarantee.setExpiryDate(guarantee.getIssueDate().plusMonths(6 + random.nextInt(18))); // 6-24 months
            Long clientId = client.getId();
            log.info("Setting applicantId: {} for guarantee {}", clientId, i+1);
            guarantee.setApplicantId(clientId);
            guarantee.setBeneficiaryName(beneficiaries[random.nextInt(beneficiaries.length)]);
            guarantee.setStatus(statuses[random.nextInt(statuses.length)]);
            guarantee.setIsDomestic(random.nextBoolean());
            guarantee.setLanguage(guarantee.getIsDomestic() ? "EN" : "BI");
            
            // Note: Additional fields like purpose, undertakingText may not exist in current entity
            
            // Calculate exchange rate if not USD
            if (!currency.equals("USD")) {
                guarantee.setExchangeRate(getExchangeRateForCurrency(currency));
                guarantee.setBaseAmount(guarantee.getAmount().multiply(guarantee.getExchangeRate()));
            } else {
                guarantee.setExchangeRate(BigDecimal.ONE);
                guarantee.setBaseAmount(guarantee.getAmount());
            }

            guarantees.add(guarantee);
        }

        return guaranteeRepository.saveAll(guarantees);
    }

    private void createFeeItems(List<GuaranteeContract> guarantees) {
        for (GuaranteeContract guarantee : guarantees) {
            // Create commission fees for each guarantee
            int installments = 1 + random.nextInt(4); // 1-4 installments
            
            // Calculate total commission (1.0% to 3.5% based on type and risk)
            BigDecimal commissionRate;
            switch (guarantee.getGuaranteeType()) {
                case PERFORMANCE:
                    commissionRate = new BigDecimal("1.5");
                    break;
                case ADVANCE_PAYMENT:
                    commissionRate = new BigDecimal("2.0");
                    break;
                case BID_BOND:
                    commissionRate = new BigDecimal("1.0");
                    break;
                case WARRANTY:
                    commissionRate = new BigDecimal("1.75");
                    break;
                case PAYMENT:
                    commissionRate = new BigDecimal("2.25");
                    break;
                case CUSTOMS:
                    commissionRate = new BigDecimal("1.25");
                    break;
                case OTHER:
                default:
                    commissionRate = new BigDecimal("1.8");
                    break;
            }
            
            BigDecimal totalCommission = guarantee.getAmount()
                .multiply(commissionRate)
                .divide(BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_UP);
            
            BigDecimal installmentAmount = totalCommission.divide(
                BigDecimal.valueOf(installments), 2, java.math.RoundingMode.HALF_UP);
            
            for (int i = 0; i < installments; i++) {
                FeeItem feeItem = new FeeItem();
                feeItem.setGuarantee(guarantee);
                feeItem.setFeeType("COMMISSION");
                feeItem.setDescription(String.format("Commission Fee - Installment %d of %d", i + 1, installments));
                feeItem.setAmount(i == installments - 1 ? 
                    totalCommission.subtract(installmentAmount.multiply(BigDecimal.valueOf(i))) : 
                    installmentAmount);
                feeItem.setCurrency(guarantee.getCurrency());
                feeItem.setBaseAmount(feeItem.getAmount().multiply(guarantee.getExchangeRate()));
                feeItem.setExchangeRate(guarantee.getExchangeRate());
                feeItem.setDueDate(guarantee.getIssueDate().plusMonths(i * 3L));
                feeItem.setIsPaid(random.nextBoolean() && i < installments - 1); // Some paid, last usually unpaid
                feeItem.setIsCalculated(true);
                
                feeItemRepository.save(feeItem);
            }
        }
    }

    private String generateReference(int sequence) {
        String year = String.valueOf(LocalDate.now().getYear());
        return String.format("GT-%s-%06d", year, sequence);
    }

    private String generatePurpose(GuaranteeType type) {
        switch (type) {
            case PERFORMANCE:
                return "Performance guarantee for construction project completion";
            case ADVANCE_PAYMENT:
                return "Advance payment guarantee for project financing";
            case BID_BOND:
                return "Bid bond guarantee for tender participation";
            case WARRANTY:
                return "Warranty guarantee for post-completion warranty";
            case PAYMENT:
                return "Payment guarantee for commercial transactions";
            case CUSTOMS:
                return "Customs guarantee for import/export procedures";
            case OTHER:
            default:
                return "General commercial guarantee";
        }
    }

    private String generateUndertakingText(GuaranteeType type) {
        return "We hereby unconditionally and irrevocably guarantee payment of the guaranteed amount upon first written demand from the beneficiary, without requiring proof or reasons for the demand.";
    }

    private String generateSpecialConditions() {
        String[] conditions = {
            "Automatic extension clause applies",
            "Reduction allowed upon milestone completion",
            "Counter-guarantee required from parent company",
            "Multiple draw-downs permitted",
            "Standby letter of credit backing required"
        };
        return conditions[random.nextInt(conditions.length)];
    }

    private BigDecimal getExchangeRateForCurrency(String currency) {
        // Simplified - return a default rate based on currency
        switch (currency) {
            case "EUR":
                return new BigDecimal("1.0850");
            case "GBP":
                return new BigDecimal("1.2650");
            case "JPY":
                return new BigDecimal("0.0067");
            case "CAD":
                return new BigDecimal("0.7380");
            case "CHF":
                return new BigDecimal("1.1020");
            case "AUD":
                return new BigDecimal("0.6540");
            case "SEK":
                return new BigDecimal("0.0920");
            case "SGD":
                return new BigDecimal("0.7420");
            case "HKD":
                return new BigDecimal("0.1280");
            default:
                return BigDecimal.ONE;
        }
    }

    // === NEW METHODS FOR COMPREHENSIVE TEST DATA ===

    private List<Bank> createBanks() {
        List<Bank> banks = new ArrayList<>();
        
        // Major international correspondent banks
        String[][] bankData = {
            {"CHASUS33", "JPMorgan Chase Bank", "270 Park Avenue, New York, NY", "New York", "US", "+1-212-270-6000", "correspondent@chase.com", "true", "CORRESPONDENT"},
            {"CITIUS33", "Citibank N.A.", "388 Greenwich Street, New York, NY", "New York", "US", "+1-212-559-1000", "trade@citi.com", "true", "CORRESPONDENT"},
            {"BOFAUS3N", "Bank of America", "100 North Tryon Street, Charlotte, NC", "Charlotte", "US", "+1-704-386-5681", "global@bofa.com", "true", "CORRESPONDENT"},
            {"DEUTDEFF", "Deutsche Bank AG", "Taunusanlage 12, Frankfurt", "Frankfurt", "DE", "+49-69-910-00", "trade@db.com", "true", "CORRESPONDENT"},
            {"HSBCGB2L", "HSBC Bank plc", "8 Canada Square, London", "London", "GB", "+44-20-7991-8888", "trade@hsbc.co.uk", "true", "CORRESPONDENT"},
            {"BNPAFRPP", "BNP Paribas", "16 Boulevard des Italiens, Paris", "Paris", "FR", "+33-1-40-14-45-46", "trade@bnpparibas.com", "true", "CORRESPONDENT"},
            {"UBSWCHZH", "UBS Switzerland AG", "Bahnhofstrasse 45, Zurich", "Zurich", "CH", "+41-44-234-1111", "trade@ubs.com", "true", "CORRESPONDENT"},
            {"ICRAITRR", "UniCredit S.p.A.", "Piazza Gae Aulenti, Milano", "Milano", "IT", "+39-02-8862-1", "trade@unicredit.it", "true", "CORRESPONDENT"},
            {"SMBCJPJT", "Sumitomo Mitsui Banking Corp", "1-1-2 Yurakucho, Tokyo", "Tokyo", "JP", "+81-3-3501-1111", "trade@smbc.co.jp", "true", "CORRESPONDENT"},
            {"ICBKCNBJ", "Industrial and Commercial Bank", "55 Fuxingmennei Dajie, Beijing", "Beijing", "CN", "+86-10-6610-6114", "trade@icbc.com.cn", "true", "CORRESPONDENT"},
            {"IEXPUS33", "InterExport Main Branch", "1 Financial Plaza, Miami, FL", "Miami", "US", "+1-305-555-0001", "main@interexport.com", "false", "INTERNAL"},
            {"IEXPUS34", "InterExport New York Branch", "200 West Street, New York, NY", "New York", "US", "+1-212-555-0002", "newyork@interexport.com", "false", "INTERNAL"}
        };

        for (String[] data : bankData) {
            Bank bank = new Bank();
            bank.setBicCode(data[0]);
            bank.setName(data[1]);
            bank.setAddress(data[2]);
            bank.setCity(data[3]);
            bank.setCountryCode(data[4]);
            bank.setPhone(data[5]);
            bank.setEmail(data[6]);
            bank.setIsCorrespondent(Boolean.parseBoolean(data[7]));
            bank.setBankType(Bank.BankType.valueOf(data[8]));
            bank.setIsActive(true);
            bank.setNotes("Test bank created for POC demonstration");
            banks.add(bank);
        }

        return bankRepository.saveAll(banks);
    }

    private List<Account> createAccounts(List<Bank> banks) {
        List<Account> accounts = new ArrayList<>();
        
        // Create liability accounts for each major currency
        String[] currencies = {"USD", "EUR", "GBP", "JPY", "CHF", "CAD", "AUD"};
        String[] glCodes = {"2100-USD", "2100-EUR", "2100-GBP", "2100-JPY", "2100-CHF", "2100-CAD", "2100-AUD"};
        
        Bank mainBank = banks.stream().filter(b -> "IEXPUS33".equals(b.getBicCode())).findFirst().orElse(banks.get(0));
        
        for (int i = 0; i < currencies.length; i++) {
            Account account = new Account();
            account.setAccountNumber("LIA" + currencies[i] + "001");
            account.setAccountName("Guarantee Liability - " + currencies[i]);
            account.setCurrency(currencies[i]);
            account.setAccountType(Account.AccountType.LIABILITY);
            account.setGlCode(glCodes[i]);
            account.setGlDescription("Liability account for " + currencies[i] + " guarantees");
            account.setBalance(new BigDecimal("1000000.00"));
            account.setCreditLimit(new BigDecimal("50000000.00"));
            account.setBank(mainBank);
            account.setIsActive(true);
            account.setIsDefault(i == 0); // USD as default
            account.setNotes("Main liability account for guarantee operations");
            accounts.add(account);
        }

        // Create nostro accounts with correspondent banks
        for (Bank bank : banks) {
            if (bank.getIsCorrespondent()) {
                Account nostroAccount = new Account();
                nostroAccount.setAccountNumber("NOSTRO" + bank.getBicCode().substring(0, 6));
                nostroAccount.setAccountName("Nostro Account - " + bank.getName());
                nostroAccount.setCurrency("USD");
                nostroAccount.setAccountType(Account.AccountType.NOSTRO);
                nostroAccount.setGlCode("1200-" + bank.getCountryCode());
                nostroAccount.setGlDescription("Nostro account with " + bank.getName());
                nostroAccount.setBalance(new BigDecimal("500000.00"));
                nostroAccount.setBank(bank);
                nostroAccount.setIsActive(true);
                nostroAccount.setNotes("Correspondent bank account for international operations");
                accounts.add(nostroAccount);
            }
        }

        return accountRepository.saveAll(accounts);
    }

    private List<CommissionParameter> createCommissionParameters() {
        List<CommissionParameter> parameters = new ArrayList<>();
        
        // Commission rates by guarantee type and currency
        Object[][] commissionData = {
            // {GuaranteeType, Currency, IsDomestic, ClientSegment, Rate, MinAmount, DefaultInstallments}
            {GuaranteeType.PERFORMANCE, "USD", true, "STANDARD", 0.0125, 500.0, 1},
            {GuaranteeType.PERFORMANCE, "USD", false, "STANDARD", 0.0150, 1000.0, 1},
            {GuaranteeType.PERFORMANCE, "EUR", true, "STANDARD", 0.0120, 450.0, 1},
            {GuaranteeType.PERFORMANCE, "EUR", false, "STANDARD", 0.0145, 900.0, 1},
            {GuaranteeType.ADVANCE_PAYMENT, "USD", true, "STANDARD", 0.0100, 300.0, 2},
            {GuaranteeType.ADVANCE_PAYMENT, "USD", false, "STANDARD", 0.0125, 600.0, 2},
            {GuaranteeType.BID_BOND, "USD", true, "STANDARD", 0.0075, 250.0, 1},
            {GuaranteeType.BID_BOND, "USD", false, "STANDARD", 0.0100, 500.0, 1},
            {GuaranteeType.WARRANTY, "USD", true, "STANDARD", 0.0150, 400.0, 4},
            {GuaranteeType.OTHER, "USD", true, "STANDARD", 0.0110, 350.0, 3},
            {GuaranteeType.CUSTOMS, "USD", true, "STANDARD", 0.0085, 200.0, 1},
            {GuaranteeType.PAYMENT, "USD", true, "STANDARD", 0.0130, 500.0, 1},
            // Premium rates for VIP clients
            {GuaranteeType.PERFORMANCE, "USD", true, "VIP", 0.0100, 400.0, 1},
            {GuaranteeType.PERFORMANCE, "USD", false, "VIP", 0.0120, 800.0, 1},
            {GuaranteeType.ADVANCE_PAYMENT, "USD", true, "VIP", 0.0080, 250.0, 2}
        };

        for (Object[] data : commissionData) {
            CommissionParameter param = new CommissionParameter();
            param.setGuaranteeType((GuaranteeType) data[0]);
            param.setCurrency((String) data[1]);
            param.setIsDomestic((Boolean) data[2]);
            param.setClientSegment((String) data[3]);
            param.setCommissionRate(new BigDecimal(data[4].toString()));
            param.setMinimumAmount(new BigDecimal(data[5].toString()));
            param.setDefaultInstallments((Integer) data[6]);
            param.setAllowManualDistribution(true);
            param.setCalculationBasis(CommissionParameter.CalculationBasis.FULL_AMOUNT);
            param.setIsActive(true);
            param.setEffectiveFrom(LocalDate.now().minusDays(30));
            param.setNotes("POC commission parameters - " + data[0] + " " + data[1]);
            parameters.add(param);
        }

        return commissionParameterRepository.saveAll(parameters);
    }

    private List<Amendment> createAmendments(List<GuaranteeContract> guarantees) {
        List<Amendment> amendments = new ArrayList<>();
        
        // Create amendments for some guarantees
        for (int i = 0; i < Math.min(guarantees.size(), 15); i++) {
            GuaranteeContract guarantee = guarantees.get(i);
            
            if (random.nextBoolean() && guarantee.getStatus() == com.interexport.guarantees.entity.enums.GuaranteeStatus.APPROVED) {
                Amendment amendment = new Amendment();
                amendment.setGuarantee(guarantee);
                amendment.setAmendmentReference("AMD-" + guarantee.getReference().substring(3) + "-01");
                amendment.setAmendmentType(AmendmentType.values()[random.nextInt(AmendmentType.values().length)]);
                amendment.setDescription(generateAmendmentDescription());
                amendment.setReason(generateAmendmentReason());
                amendment.setChangesJson(generateAmendmentChanges(guarantee));
                amendment.setRequiresConsent(random.nextBoolean());
                
                // Set status based on amendment type and requirements
                if (amendment.getRequiresConsent()) {
                    if (random.nextDouble() > 0.7) {
                        amendment.setStatus(com.interexport.guarantees.entity.enums.GuaranteeStatus.APPROVED);
                        amendment.setConsentReceivedDate(LocalDateTime.now().minusDays(random.nextInt(10)));
                        amendment.setProcessedDate(LocalDateTime.now().minusDays(random.nextInt(5)));
                        amendment.setProcessedBy("system_approval");
                    } else {
                        amendment.setStatus(com.interexport.guarantees.entity.enums.GuaranteeStatus.SUBMITTED);
                    }
                } else {
                    amendment.setStatus(com.interexport.guarantees.entity.enums.GuaranteeStatus.APPROVED);
                    amendment.setProcessedDate(LocalDateTime.now().minusDays(random.nextInt(3)));
                    amendment.setProcessedBy("auto_approval");
                }
                
                amendment.setSubmittedDate(LocalDateTime.now().minusDays(random.nextInt(20) + 5));
                amendments.add(amendment);
            }
        }
        
        return amendmentRepository.saveAll(amendments);
    }

    private List<Claim> createClaims(List<GuaranteeContract> guarantees) {
        List<Claim> claims = new ArrayList<>();
        
        // Create claims for some guarantees
        for (int i = 0; i < Math.min(guarantees.size(), 10); i++) {
            GuaranteeContract guarantee = guarantees.get(i);
            
            if (random.nextDouble() > 0.7 && guarantee.getStatus() == com.interexport.guarantees.entity.enums.GuaranteeStatus.APPROVED) {
                Claim claim = new Claim();
                claim.setGuarantee(guarantee);
                claim.setClaimReference("CLM-" + guarantee.getReference().substring(3) + "-01");
                claim.setAmount(guarantee.getAmount().multiply(new BigDecimal(0.3 + random.nextDouble() * 0.7)));
                claim.setCurrency(guarantee.getCurrency());
                claim.setClaimDate(LocalDate.now().minusDays(random.nextInt(30) + 5));
                claim.setClaimReason(generateClaimReason());
                claim.setBeneficiaryContact(guarantee.getBeneficiaryName() + " Claims Dept");
                claim.setDocumentsSubmitted(random.nextBoolean() ? "Original contract, performance certificate, legal notice" : "Pending submission of supporting documents");
                claim.setProcessingDeadline(LocalDate.now().plusDays(random.nextInt(15) + 5));
                claim.setRequiresSpecialApproval(claim.getAmount().compareTo(new BigDecimal("100000")) > 0);
                
                // Set claim status randomly
                ClaimStatus[] statuses = {ClaimStatus.REQUESTED, ClaimStatus.UNDER_REVIEW, ClaimStatus.APPROVED, ClaimStatus.PENDING_DOCUMENTS, ClaimStatus.REJECTED, ClaimStatus.SETTLED};
                ClaimStatus status = statuses[random.nextInt(statuses.length)];
                claim.setStatus(status);
                
                // Set additional fields based on status
                switch (status) {
                    case APPROVED:
                        claim.setApprovedDate(LocalDateTime.now().minusDays(random.nextInt(10)));
                        claim.setApprovedBy("claims_officer_" + (random.nextInt(3) + 1));
                        break;
                    case REJECTED:
                        claim.setRejectedDate(LocalDateTime.now().minusDays(random.nextInt(5)));
                        claim.setRejectedBy("claims_manager");
                        claim.setRejectionReason("Insufficient supporting documentation provided");
                        break;
                    case SETTLED:
                        claim.setApprovedDate(LocalDateTime.now().minusDays(random.nextInt(15) + 5));
                        claim.setApprovedBy("claims_officer_1");
                        claim.setPaymentDate(LocalDateTime.now().minusDays(random.nextInt(5)));
                        claim.setPaymentReference("PAY-" + System.currentTimeMillis());
                        break;
                    case PENDING_DOCUMENTS:
                        claim.setMissingDocuments("Original guarantee document, beneficiary authorization letter");
                        break;
                }
                
                claim.setProcessingNotes("POC test claim - Status: " + status);
                claims.add(claim);
            }
        }
        
        return claimRepository.saveAll(claims);
    }

    private String generateAmendmentDescription() {
        String[] descriptions = {
            "Extension of guarantee expiry date by 90 days",
            "Increase in guarantee amount by 25%",
            "Change in beneficiary name and address",
            "Amendment to underlying contract terms",
            "Reduction in guarantee amount as per milestone completion",
            "Text amendment to include additional conditions",
            "Currency change from USD to EUR",
            "Partial release of guarantee amount"
        };
        return descriptions[random.nextInt(descriptions.length)];
    }

    private String generateAmendmentReason() {
        String[] reasons = {
            "Project timeline extended due to regulatory approvals",
            "Contract value increased per addendum",
            "Beneficiary corporate restructuring",
            "Underlying contract amendments required",
            "Milestone achievements verified",
            "Additional terms negotiated by parties",
            "Currency hedging requirements",
            "Partial project completion certified"
        };
        return reasons[random.nextInt(reasons.length)];
    }

    private String generateAmendmentChanges(GuaranteeContract guarantee) {
        // Create a JSON representation of changes
        return String.format("{\"original_expiry\":\"%s\",\"new_expiry\":\"%s\",\"reason\":\"Contract extension approved\",\"change_type\":\"date_extension\"}", 
                guarantee.getExpiryDate(), guarantee.getExpiryDate().plusDays(90));
    }

    private String generateClaimReason() {
        String[] reasons = {
            "Non-performance of contractual obligations by applicant",
            "Breach of payment terms in underlying contract",
            "Failure to deliver goods as per contract specifications",
            "Default on milestone completion deadlines",
            "Quality issues with delivered services",
            "Abandonment of project by contractor",
            "Force majeure event preventing performance",
            "Insolvency of applicant company"
        };
        return reasons[random.nextInt(reasons.length)];
    }
}
