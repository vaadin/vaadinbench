package com.example.customers.ui;

import java.util.List;

import com.example.customers.domain.Customer;
import com.example.customers.domain.CustomerSort;
import com.example.customers.service.CustomerService;

import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.provider.QuerySortOrder;
import com.vaadin.flow.data.provider.SortDirection;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

/**
 * Lists customers in a lazily loaded {@link Grid}.
 *
 * <p>The grid never holds the whole data set: it asks the backend for one page
 * at a time and asks separately for the total row count.
 */
@Route("")
@PageTitle("Customers")
public class CustomerListView extends VerticalLayout {

    private final CustomerService customerService;
    private final Grid<Customer> grid = new Grid<>(Customer.class, false);

    public CustomerListView(CustomerService customerService) {
        this.customerService = customerService;

        setSizeFull();
        add(new H2("Customers"), grid);

        configureGrid();
    }

    private void configureGrid() {
        grid.addColumn(Customer::firstName).setHeader("First name")
                .setKey("firstName").setSortProperty("firstName");
        grid.addColumn(Customer::lastName).setHeader("Last name")
                .setKey("lastName").setSortProperty("lastName");
        grid.addColumn(Customer::email).setHeader("Email")
                .setKey("email").setSortProperty("email");
        grid.addColumn(Customer::country).setHeader("Country")
                .setKey("country").setSortProperty("country");
        grid.addColumn(customer -> customer.status().name()).setHeader("Status")
                .setKey("status").setSortProperty("status");
        grid.setSizeFull();

        grid.setItems(
                query -> customerService
                        .list(query.getOffset(), query.getLimit(), toSorts(query.getSortOrders()))
                        .stream(),
                query -> customerService.count());
    }

    private static List<CustomerSort> toSorts(List<QuerySortOrder> sortOrders) {
        return sortOrders.stream()
                .map(order -> new CustomerSort(
                        order.getSorted(), order.getDirection() == SortDirection.ASCENDING))
                .toList();
    }
}
