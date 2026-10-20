package com.queuenow.model;

import java.util.UUID;

/**
 * Data model representing an individual customer's entry in the queue.
 */
public class QueueEntry {

    private final String persistenceId = UUID.randomUUID().toString();

    /** Sequential token number assigned to the customer upon joining the queue. */
    private int tokenNumber;

    /** Name of the customer who joined the queue. */
    private String customerName;

    /** Status of the customer in queue: WAITING, SERVING, or COMPLETED. */
    private String status = "WAITING";

    /**
     * Constructs a new QueueEntry with the specified token number and customer name.
     */
    public QueueEntry(int tokenNumber, String customerName) {
        this.tokenNumber = tokenNumber;
        this.customerName = customerName;
        this.status = "WAITING";
    }

    public int getTokenNumber() {
        return tokenNumber;
    }

    public String getCustomerName() {
        return customerName;
    }

    public String persistenceId() {
        return persistenceId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
