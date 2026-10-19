package com.queuenow.model;

/**
 * Data model representing an individual customer's entry in the queue.
 *
 * <p>Encapsulates customer details such as their sequential queue token number
 * and customer name.</p>
 */
public class QueueEntry {

    /** Sequential token number assigned to the customer upon joining the queue. */
    private int tokenNumber;

    /** Name of the customer who joined the queue. */
    private String customerName;

    /**
     * Constructs a new QueueEntry with the specified token number and customer name.
     *
     * @param tokenNumber  the unique sequential token assigned to this customer
     * @param customerName the name of the customer
     */
    public QueueEntry(int tokenNumber, String customerName) {
        this.tokenNumber = tokenNumber;
        this.customerName = customerName;
    }

    /**
     * Retrieves the sequential token number for this queue entry.
     *
     * @return the token number
     */
    public int getTokenNumber() {
        return tokenNumber;
    }

    /**
     * Retrieves the name of the customer for this queue entry.
     *
     * @return the customer name
     */
    public String getCustomerName() {
        return customerName;
    }
}
