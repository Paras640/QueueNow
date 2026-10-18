
package com.queuenow.model;

public class QueueEntry {

    private int tokenNumber;
    private String customerName;

    public QueueEntry(int tokenNumber, String customerName) {
        this.tokenNumber = tokenNumber;
        this.customerName = customerName;
    }

    public int getTokenNumber() {
        return tokenNumber;
    }

    public String getCustomerName() {
        return customerName;
    }
}
