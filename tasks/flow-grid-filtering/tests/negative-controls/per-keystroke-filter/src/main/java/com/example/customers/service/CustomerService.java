package com.example.customers.service;

import java.util.List;

import com.example.customers.domain.Customer;
import com.example.customers.domain.CustomerFilter;
import com.example.customers.domain.CustomerRepository;
import com.example.customers.domain.CustomerSort;

import org.springframework.stereotype.Service;

/**
 * Application-level access to customers. The UI talks to this service, never to
 * the repository directly.
 */
@Service
public class CustomerService {

    private final CustomerRepository repository;

    public CustomerService(CustomerRepository repository) {
        this.repository = repository;
    }

    /** Returns one page of the customers matching {@code filter}. */
    public List<Customer> list(CustomerFilter filter, int offset, int limit,
            List<CustomerSort> sorts) {
        return repository.findAll(filter, offset, limit, sorts);
    }

    /** Returns the number of customers matching {@code filter}. */
    public int count(CustomerFilter filter) {
        return repository.count(filter);
    }
}
