package com.queuenow.service;

import com.queuenow.model.QueueEntry;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Service component managing in-memory queue entries and token assignments.
 *
 * <p>Maintains an ordered list of customer queue entries and handles the issuance
 * of sequential token numbers as customers join the queue.</p>
 */
@Service
public class QueueService {

    /** Internal in-memory list to store queue entries in the order they joined (FIFO). */
    private final List<QueueEntry> queue = new ArrayList<>();

    /** Sequential counter for issuing token numbers, starting at 1. */
    private int nextTokenNumber = 1;

    /**
     * Adds a new customer to the queue and assigns them the next sequential token number.
     *
     * @param customerName the name of the customer joining the queue
     * @return the newly created {@link QueueEntry} containing the assigned token and customer name
     */
    public QueueEntry joinQueue(String customerName) {
        // Instantiate a new queue entry using the current token number and customer name
        QueueEntry entry = new QueueEntry(
                nextTokenNumber,
                customerName
        );

        // Add the entry to the end of the queue
        queue.add(entry);

        // Increment the counter so the subsequent entrant receives the next token number
        nextTokenNumber++;

        // Return the created entry back to the caller
        return entry;
    }

    /**
     * Retrieves the current snapshot of all customers in the queue.
     *
     * @return a new list containing all current {@link QueueEntry} objects in FIFO order
     */
    public List<QueueEntry> getQueue() {
        // Return a defensive shallow copy to protect the internal list from direct external mutation
        return new ArrayList<>(queue);
    }
}
