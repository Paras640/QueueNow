
package com.queuenow.service;

import com.queuenow.model.QueueEntry;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class QueueService {

    private final List<QueueEntry> queue = new ArrayList<>();
    private int nextTokenNumber = 1;

    public QueueEntry joinQueue(String customerName) {
        QueueEntry entry = new QueueEntry(
                nextTokenNumber,
                customerName
        );

        queue.add(entry);
        nextTokenNumber++;

        return entry;
    }

    public List<QueueEntry> getQueue() {
        return new ArrayList<>(queue);
    }
}
