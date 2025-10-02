package com.interexport.guarantees.service;

import com.interexport.guarantees.entity.Account;
import com.interexport.guarantees.entity.Bank;
import com.interexport.guarantees.entity.CommissionParameter;
import com.interexport.guarantees.entity.enums.GuaranteeType;
import com.interexport.guarantees.exception.ParameterNotFoundException;
import com.interexport.guarantees.repository.AccountRepository;
import com.interexport.guarantees.repository.BankRepository;
import com.interexport.guarantees.repository.CommissionParameterRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.*;

/**
 * Service for managing Parameters (F10)
 * Handles banks, accounts, commission parameters, and GL mapping
 * 
 * Business Rules:
 * - Banks must have unique BIC codes
 * - Accounts must have unique numbers within a bank
 * - Commission parameters must not overlap in effectiveness periods
 * - GL codes should follow accounting standards
 */
@Service
@Transactional
public class ParametersService {

    private final BankRepository bankRepository;
    private final AccountRepository accountRepository;
    private final CommissionParameterRepository commissionParameterRepository;

    @Autowired
    public ParametersService(
            BankRepository bankRepository,
            AccountRepository accountRepository,
            CommissionParameterRepository commissionParameterRepository) {
        this.bankRepository = bankRepository;
        this.accountRepository = accountRepository;
        this.commissionParameterRepository = commissionParameterRepository;
    }

    // === BANK MANAGEMENT ===

    @Transactional(readOnly = true)
    public Page<Bank> searchBanks(String search, Pageable pageable) {
        if (StringUtils.hasText(search)) {
            return bankRepository.searchBanks(search, pageable);
        }
        return bankRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public Optional<Bank> findBankById(Long id) {
        return bankRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public Optional<Bank> findBankByBic(String bicCode) {
        return bankRepository.findByBicCode(bicCode);
    }

    @Transactional
    public Bank createBank(Bank bank) {
        // Validate BIC uniqueness
        if (bankRepository.existsByBicCodeAndIdNot(bank.getBicCode(), null)) {
            throw new IllegalArgumentException("Bank with BIC code " + bank.getBicCode() + " already exists");
        }

        // Set defaults
        if (bank.getIsActive() == null) {
            bank.setIsActive(true);
        }
        if (bank.getIsCorrespondent() == null) {
            bank.setIsCorrespondent(false);
        }

        return bankRepository.save(bank);
    }

    @Transactional
    public Bank updateBank(Long id, Bank updatedBank) {
        Bank existingBank = bankRepository.findById(id)
                .orElseThrow(() -> new ParameterNotFoundException("Bank not found with id: " + id));

        // Validate BIC uniqueness if changed
        if (!existingBank.getBicCode().equals(updatedBank.getBicCode())) {
            if (bankRepository.existsByBicCodeAndIdNot(updatedBank.getBicCode(), id)) {
                throw new IllegalArgumentException("Bank with BIC code " + updatedBank.getBicCode() + " already exists");
            }
        }

        // Update fields
        existingBank.setName(updatedBank.getName());
        existingBank.setBicCode(updatedBank.getBicCode());
        existingBank.setAddress(updatedBank.getAddress());
        existingBank.setCity(updatedBank.getCity());
        existingBank.setCountryCode(updatedBank.getCountryCode());
        existingBank.setPhone(updatedBank.getPhone());
        existingBank.setEmail(updatedBank.getEmail());
        existingBank.setIsCorrespondent(updatedBank.getIsCorrespondent());
        existingBank.setIsActive(updatedBank.getIsActive());
        existingBank.setBankType(updatedBank.getBankType());
        existingBank.setNotes(updatedBank.getNotes());

        return bankRepository.save(existingBank);
    }

    @Transactional
    public void deleteBank(Long id) {
        Bank bank = bankRepository.findById(id)
                .orElseThrow(() -> new ParameterNotFoundException("Bank not found with id: " + id));

        // Check for dependent accounts
        List<Account> accounts = accountRepository.findByBankIdOrderByAccountName(id);
        if (!accounts.isEmpty()) {
            throw new IllegalArgumentException("Cannot delete bank with existing accounts. Please delete or reassign accounts first.");
        }

        bankRepository.delete(bank);
    }

    @Transactional(readOnly = true)
    public List<Bank> getCorrespondentBanks() {
        return bankRepository.findActiveCorrespondentBanks();
    }

    @Transactional(readOnly = true)
    public List<Bank> getBanksByCountry(String countryCode) {
        return bankRepository.findByCountryCodeOrderByName(countryCode);
    }

    // === ACCOUNT MANAGEMENT ===

    @Transactional(readOnly = true)
    public Page<Account> searchAccounts(String search, String currency, Pageable pageable) {
        if (StringUtils.hasText(search)) {
            return accountRepository.searchAccounts(search, pageable);
        } else if (StringUtils.hasText(currency)) {
            return accountRepository.findByCurrencyAndIsActiveTrueOrderByAccountName(currency)
                    .stream()
                    .collect(java.util.stream.Collectors.collectingAndThen(
                            java.util.stream.Collectors.toList(),
                            list -> new org.springframework.data.domain.PageImpl<>(list, pageable, list.size())
                    ));
        }
        return accountRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public Optional<Account> findAccountById(Long id) {
        return accountRepository.findById(id);
    }

    @Transactional
    public Account createAccount(Account account) {
        // Validate bank exists
        if (account.getBank() == null || account.getBank().getId() == null) {
            throw new IllegalArgumentException("Bank is required for account creation");
        }

        Bank bank = bankRepository.findById(account.getBank().getId())
                .orElseThrow(() -> new ParameterNotFoundException("Bank not found with id: " + account.getBank().getId()));

        // Validate account number uniqueness within bank
        if (accountRepository.existsByBankIdAndAccountNumberAndIdNot(
                bank.getId(), account.getAccountNumber(), null)) {
            throw new IllegalArgumentException("Account number " + account.getAccountNumber() + 
                    " already exists for bank " + bank.getBicCode());
        }

        // Set defaults
        if (account.getIsActive() == null) {
            account.setIsActive(true);
        }
        if (account.getIsDefault() == null) {
            account.setIsDefault(false);
        }

        account.setBank(bank);
        return accountRepository.save(account);
    }

    @Transactional
    public Account updateAccount(Long id, Account updatedAccount) {
        Account existingAccount = accountRepository.findById(id)
                .orElseThrow(() -> new ParameterNotFoundException("Account not found with id: " + id));

        // Validate account number uniqueness if changed
        if (!existingAccount.getAccountNumber().equals(updatedAccount.getAccountNumber())) {
            if (accountRepository.existsByBankIdAndAccountNumberAndIdNot(
                    existingAccount.getBank().getId(), updatedAccount.getAccountNumber(), id)) {
                throw new IllegalArgumentException("Account number " + updatedAccount.getAccountNumber() + 
                        " already exists for this bank");
            }
        }

        // Update fields
        existingAccount.setAccountNumber(updatedAccount.getAccountNumber());
        existingAccount.setAccountName(updatedAccount.getAccountName());
        existingAccount.setCurrency(updatedAccount.getCurrency());
        existingAccount.setAccountType(updatedAccount.getAccountType());
        existingAccount.setGlCode(updatedAccount.getGlCode());
        existingAccount.setGlDescription(updatedAccount.getGlDescription());
        existingAccount.setBalance(updatedAccount.getBalance());
        existingAccount.setCreditLimit(updatedAccount.getCreditLimit());
        existingAccount.setIsActive(updatedAccount.getIsActive());
        existingAccount.setIsDefault(updatedAccount.getIsDefault());
        existingAccount.setNotes(updatedAccount.getNotes());

        return accountRepository.save(existingAccount);
    }

    @Transactional
    public void deleteAccount(Long id) {
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new ParameterNotFoundException("Account not found with id: " + id));

        // Note: In production, check for dependent transactions before deletion
        
        accountRepository.delete(account);
    }

    @Transactional(readOnly = true)
    public List<Account> getAccountsByBank(Long bankId) {
        return accountRepository.findByBankIdAndIsActiveTrueOrderByAccountName(bankId);
    }

    @Transactional(readOnly = true)
    public List<Account> getAccountsByCurrency(String currency) {
        return accountRepository.findByCurrencyAndIsActiveTrueOrderByAccountName(currency);
    }

    @Transactional(readOnly = true)
    public List<Account> getLiabilityAccountsByCurrency(String currency) {
        return accountRepository.findLiabilityAccountsByCurrency(currency);
    }

    // === COMMISSION PARAMETERS ===

    @Transactional(readOnly = true)
    public Page<CommissionParameter> searchCommissionParameters(String search, GuaranteeType guaranteeType, 
                                                               String currency, Pageable pageable) {
        if (StringUtils.hasText(search)) {
            return commissionParameterRepository.searchParameters(search, pageable);
        }
        // Note: Future enhancement - add filtered search by guaranteeType and currency
        return commissionParameterRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public Optional<CommissionParameter> findCommissionParameterById(Long id) {
        return commissionParameterRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public Optional<CommissionParameter> findCommissionParameterForCriteria(
            GuaranteeType guaranteeType, String currency, Boolean isDomestic, String clientSegment) {
        
        // Try exact match first
        Optional<CommissionParameter> exactMatch = commissionParameterRepository.findActiveParameterForCriteria(
                guaranteeType, currency, isDomestic, clientSegment, LocalDate.now());
        
        if (exactMatch.isPresent()) {
            return exactMatch;
        }
        
        // Fallback to best match
        List<CommissionParameter> bestMatches = commissionParameterRepository.findBestMatchingParameters(
                guaranteeType, currency, isDomestic, clientSegment, LocalDate.now(), PageRequest.of(0, 1));
        
        return bestMatches.isEmpty() ? Optional.empty() : Optional.of(bestMatches.get(0));
    }

    @Transactional
    public CommissionParameter createCommissionParameter(CommissionParameter parameter) {
        // Validate for overlapping parameters
        if (commissionParameterRepository.existsOverlappingParameter(
                parameter.getGuaranteeType(), parameter.getCurrency(), parameter.getIsDomestic(),
                parameter.getClientSegment(), parameter.getEffectiveFrom(), parameter.getEffectiveTo(), null)) {
            throw new IllegalArgumentException("Overlapping commission parameter exists for the same criteria and date range");
        }

        // Set defaults
        if (parameter.getIsActive() == null) {
            parameter.setIsActive(true);
        }
        if (parameter.getEffectiveFrom() == null) {
            parameter.setEffectiveFrom(LocalDate.now());
        }
        if (parameter.getClientSegment() == null) {
            parameter.setClientSegment("STANDARD");
        }

        return commissionParameterRepository.save(parameter);
    }

    @Transactional
    public CommissionParameter updateCommissionParameter(Long id, CommissionParameter updatedParameter) {
        CommissionParameter existingParameter = commissionParameterRepository.findById(id)
                .orElseThrow(() -> new ParameterNotFoundException("Commission parameter not found with id: " + id));

        // Validate for overlapping parameters if key fields changed
        if (!Objects.equals(existingParameter.getGuaranteeType(), updatedParameter.getGuaranteeType()) ||
            !Objects.equals(existingParameter.getCurrency(), updatedParameter.getCurrency()) ||
            !Objects.equals(existingParameter.getIsDomestic(), updatedParameter.getIsDomestic()) ||
            !Objects.equals(existingParameter.getClientSegment(), updatedParameter.getClientSegment()) ||
            !Objects.equals(existingParameter.getEffectiveFrom(), updatedParameter.getEffectiveFrom()) ||
            !Objects.equals(existingParameter.getEffectiveTo(), updatedParameter.getEffectiveTo())) {
            
            if (commissionParameterRepository.existsOverlappingParameter(
                    updatedParameter.getGuaranteeType(), updatedParameter.getCurrency(), 
                    updatedParameter.getIsDomestic(), updatedParameter.getClientSegment(),
                    updatedParameter.getEffectiveFrom(), updatedParameter.getEffectiveTo(), id)) {
                throw new IllegalArgumentException("Overlapping commission parameter exists for the same criteria and date range");
            }
        }

        // Update fields
        existingParameter.setGuaranteeType(updatedParameter.getGuaranteeType());
        existingParameter.setCurrency(updatedParameter.getCurrency());
        existingParameter.setIsDomestic(updatedParameter.getIsDomestic());
        existingParameter.setClientSegment(updatedParameter.getClientSegment());
        existingParameter.setCommissionRate(updatedParameter.getCommissionRate());
        existingParameter.setMinimumAmount(updatedParameter.getMinimumAmount());
        existingParameter.setMaximumAmount(updatedParameter.getMaximumAmount());
        existingParameter.setDefaultInstallments(updatedParameter.getDefaultInstallments());
        existingParameter.setAllowManualDistribution(updatedParameter.getAllowManualDistribution());
        existingParameter.setCalculationBasis(updatedParameter.getCalculationBasis());
        existingParameter.setEffectiveFrom(updatedParameter.getEffectiveFrom());
        existingParameter.setEffectiveTo(updatedParameter.getEffectiveTo());
        existingParameter.setIsActive(updatedParameter.getIsActive());
        existingParameter.setNotes(updatedParameter.getNotes());

        return commissionParameterRepository.save(existingParameter);
    }

    @Transactional
    public void deleteCommissionParameter(Long id) {
        CommissionParameter parameter = commissionParameterRepository.findById(id)
                .orElseThrow(() -> new ParameterNotFoundException("Commission parameter not found with id: " + id));

        // Note: In production, check for dependent guarantees using this parameter
        
        commissionParameterRepository.delete(parameter);
    }

    // === UTILITY METHODS ===

    @Transactional(readOnly = true)
    public Map<String, Object> getParametersSummary() {
        Map<String, Object> summary = new HashMap<>();
        
        summary.put("totalBanks", bankRepository.count());
        summary.put("activeBanks", bankRepository.countActiveBanks());
        summary.put("correspondentBanks", bankRepository.countActiveCorrespondentBanks());
        
        summary.put("totalAccounts", accountRepository.count());
        summary.put("activeAccounts", accountRepository.countActiveAccounts());
        
        summary.put("totalCommissionParameters", commissionParameterRepository.count());
        summary.put("activeCommissionParameters", commissionParameterRepository.countActiveParameters());
        
        return summary;
    }

    @Transactional(readOnly = true)
    public List<String> getAllClientSegments() {
        return commissionParameterRepository.findAllActiveClientSegments();
    }

    @Transactional(readOnly = true)
    public List<String> getCurrenciesWithParameters() {
        return commissionParameterRepository.findAllActiveCurrencies();
    }

    @Transactional(readOnly = true)
    public Map<String, Object> validateConfiguration() {
        Map<String, Object> validation = new HashMap<>();
        List<String> warnings = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        // Check if we have at least one bank
        if (bankRepository.countActiveBanks() == 0) {
            errors.add("No active banks configured");
        }

        // Check if we have liability accounts for common currencies
        String[] commonCurrencies = {"USD", "EUR", "GBP"};
        for (String currency : commonCurrencies) {
            List<Account> liabilityAccounts = accountRepository.findLiabilityAccountsByCurrency(currency);
            if (liabilityAccounts.isEmpty()) {
                warnings.add("No liability accounts configured for " + currency);
            }
        }

        // Check if we have commission parameters for common guarantee types
        GuaranteeType[] commonTypes = {GuaranteeType.PERFORMANCE, GuaranteeType.ADVANCE_PAYMENT, GuaranteeType.BID_BOND};
        for (GuaranteeType type : commonTypes) {
            if (commissionParameterRepository.countActiveParametersByType(type) == 0) {
                warnings.add("No commission parameters configured for " + type);
            }
        }

        validation.put("valid", errors.isEmpty());
        validation.put("errors", errors);
        validation.put("warnings", warnings);
        
        return validation;
    }
}
