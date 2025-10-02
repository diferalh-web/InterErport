package com.interexport.guarantees.service;

import com.interexport.guarantees.entity.Client;
import com.interexport.guarantees.exception.ClientNotFoundException;
import com.interexport.guarantees.repository.ClientRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Service for managing Client entities
 * Handles client CRUD operations, search, and business logic
 */
@Service
@Transactional
public class ClientService {

    private final ClientRepository clientRepository;

    @Autowired
    public ClientService(ClientRepository clientRepository) {
        this.clientRepository = clientRepository;
    }

    @Transactional(readOnly = true)
    public Page<Client> findAll(Pageable pageable) {
        return clientRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public Page<Client> findActiveClients(Pageable pageable) {
        return clientRepository.findByIsActiveTrue(pageable);
    }

    @Transactional(readOnly = true)
    public Optional<Client> findById(Long id) {
        return clientRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public Optional<Client> findByClientCode(String clientCode) {
        return clientRepository.findByClientCode(clientCode);
    }

    @Transactional(readOnly = true)
    public List<Client> searchByName(String name) {
        return clientRepository.findByNameContainingIgnoreCase(name);
    }

    @Transactional(readOnly = true)
    public Page<Client> searchByName(String name, Pageable pageable) {
        return clientRepository.findByNameContainingIgnoreCase(name, pageable);
    }

    @Transactional(readOnly = true)
    public Page<Client> searchByCriteria(String clientCode, String name, String countryCode, 
                                         String riskRating, Boolean isActive, Pageable pageable) {
        return clientRepository.findByCriteria(clientCode, name, countryCode, riskRating, isActive, pageable);
    }

    @Transactional
    public Client createClient(Client client) {
        // Validate unique client code
        if (clientRepository.existsByClientCode(client.getClientCode())) {
            throw new IllegalArgumentException("Client with code " + client.getClientCode() + " already exists");
        }

        // Set defaults
        if (client.getIsActive() == null) {
            client.setIsActive(true);
        }
        if (client.getRiskRating() == null) {
            client.setRiskRating("STANDARD");
        }

        return clientRepository.save(client);
    }

    @Transactional
    public Client updateClient(Long id, Client updatedClient) {
        return clientRepository.findById(id).map(existingClient -> {
            existingClient.setName(updatedClient.getName());
            existingClient.setAddress(updatedClient.getAddress());
            existingClient.setCity(updatedClient.getCity());
            existingClient.setCountryCode(updatedClient.getCountryCode());
            existingClient.setPostalCode(updatedClient.getPostalCode());
            existingClient.setPhone(updatedClient.getPhone());
            existingClient.setEmail(updatedClient.getEmail());
            existingClient.setTaxId(updatedClient.getTaxId());
            existingClient.setEntityType(updatedClient.getEntityType());
            existingClient.setIndustryCode(updatedClient.getIndustryCode());
            existingClient.setRiskRating(updatedClient.getRiskRating());
            existingClient.setCreditLimit(updatedClient.getCreditLimit());
            existingClient.setCreditCurrency(updatedClient.getCreditCurrency());
            existingClient.setIsActive(updatedClient.getIsActive());
            existingClient.setKycDate(updatedClient.getKycDate());
            existingClient.setKycReviewDate(updatedClient.getKycReviewDate());
            existingClient.setNotes(updatedClient.getNotes());

            return clientRepository.save(existingClient);
        }).orElseThrow(() -> new ClientNotFoundException("Client not found with id: " + id));
    }

    @Transactional
    public void deleteClient(Long id) {
        Client client = clientRepository.findById(id)
                .orElseThrow(() -> new ClientNotFoundException("Client not found with id: " + id));
        
        // Soft delete - just mark as inactive
        client.setIsActive(false);
        clientRepository.save(client);
    }

    @Transactional(readOnly = true)
    public List<Client> findClientsWithKycReviewDue() {
        return clientRepository.findWithKycReviewDue();
    }

    @Transactional(readOnly = true)
    public List<Client> findClientsWithUpcomingKycReview(LocalDate futureDate) {
        return clientRepository.findWithUpcomingKycReview(futureDate);
    }

    @Transactional(readOnly = true)
    public List<Client> findClientsWithUpcomingKycReview(int daysAhead) {
        LocalDate futureDate = LocalDate.now().plusDays(daysAhead);
        return clientRepository.findWithUpcomingKycReview(futureDate);
    }

    @Transactional(readOnly = true)
    public List<Client> findByCountry(String countryCode) {
        return clientRepository.findByCountryCode(countryCode);
    }

    @Transactional(readOnly = true)
    public List<Client> findByRiskRating(String riskRating) {
        return clientRepository.findByRiskRating(riskRating);
    }

    @Transactional(readOnly = true)
    public boolean existsByClientCode(String clientCode) {
        return clientRepository.existsByClientCode(clientCode);
    }

    @Transactional
    public Client activateClient(Long id) {
        return clientRepository.findById(id).map(client -> {
            client.setIsActive(true);
            return clientRepository.save(client);
        }).orElseThrow(() -> new ClientNotFoundException("Client not found with id: " + id));
    }

    @Transactional
    public Client deactivateClient(Long id) {
        return clientRepository.findById(id).map(client -> {
            client.setIsActive(false);
            return clientRepository.save(client);
        }).orElseThrow(() -> new ClientNotFoundException("Client not found with id: " + id));
    }
}
