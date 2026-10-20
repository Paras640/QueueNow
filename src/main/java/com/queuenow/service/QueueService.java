package com.queuenow.service;

import com.queuenow.model.QueueEntry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service component managing queue entries, turn progress, and MongoDB persistence.
 */
@Service
public class QueueService {

    private final MongoTemplate mongoTemplate;

    /** Internal in-memory list to store queue entries in arrival order (FIFO). */
    private final List<QueueEntry> queue = new ArrayList<>();

    /** Sequential counter for issuing token numbers, starting at 1. */
    private int nextTokenNumber = 1;

    /** Current token number being served at the counter (0 means idle / no one currently being called). */
    private int currentServingToken = 0;

    /** List of customer feedback submissions. */
    private final List<Map<String, Object>> feedbackList = new ArrayList<>();

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(QueueService.class);

    public QueueService(@Autowired(required = false) MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
        if (mongoTemplate != null) {
            log.info(">>> [MongoDB] Connected successfully! MongoTemplate initialized: db='{}'", mongoTemplate.getDb().getName());
        } else {
            log.error(">>> [MongoDB] WARNING: MongoTemplate is NULL! Check Spring Boot MongoDB starter & configuration.");
        }
    }

    /**
     * Adds a new customer to the queue, assigns a sequential token, and records in MongoDB.
     */
    public QueueEntry joinQueue(String customerName) {
        QueueEntry entry = new QueueEntry(
                nextTokenNumber,
                customerName
        );

        queue.add(entry);
        nextTokenNumber++;

        // Persist to MongoDB Atlas if connected
        if (mongoTemplate != null) {
            try {
                Map<String, Object> doc = new HashMap<>();
                doc.put("_id", entry.persistenceId());
                doc.put("tokenNumber", entry.getTokenNumber());
                doc.put("customerName", entry.getCustomerName());
                doc.put("status", "WAITING");
                doc.put("createdAt", new Date());
                mongoTemplate.save(doc, "tokens");
                log.info(">>> [MongoDB] Saved token #{} for '{}' to collection 'tokens' in DB '{}'", entry.getTokenNumber(), entry.getCustomerName(), mongoTemplate.getDb().getName());
            } catch (Exception e) {
                log.error(">>> [MongoDB] FAILED to save token #{} for '{}' to MongoDB: {}", entry.getTokenNumber(), entry.getCustomerName(), e.getMessage(), e);
            }
        } else {
            log.warn(">>> [MongoDB] Cannot persist token #{} - mongoTemplate is NULL!", entry.getTokenNumber());
        }

        return entry;
    }

    /**
     * Retrieves the current snapshot of all customers in the queue.
     */
    public List<QueueEntry> getQueue() {
        return new ArrayList<>(queue);
    }

    /**
     * Retrieves the current token being served (0 if none).
     */
    public int getCurrentServingToken() {
        return currentServingToken;
    }

    public boolean hasWaitingCustomers() {
        return queue.stream().anyMatch(e -> "WAITING".equalsIgnoreCase(e.getStatus()));
    }

    public boolean isCurrentlyServing() {
        return queue.stream().anyMatch(e -> "SERVING".equalsIgnoreCase(e.getStatus()));
    }

    /**
     * Advances to the next customer:
     * 1. Marks previous SERVING customer as COMPLETED (triggering their feedback form automatically).
     * 2. Sets next WAITING customer to SERVING (alerting their phone).
     */
    
public synchronized int callNextToken() {
    // Do not automatically complete the current customer.
    // Admin must click Finish before calling the next customer.
    for (QueueEntry entry : queue) {
        if ("SERVING".equalsIgnoreCase(entry.getStatus())) {
            return entry.getTokenNumber();
        }
    }

    // Find the first waiting customer.
    for (QueueEntry entry : queue) {
        if ("WAITING".equalsIgnoreCase(entry.getStatus())) {
            entry.setStatus("SERVING");
            currentServingToken = entry.getTokenNumber();
            updateMongoStatus(entry, "SERVING");
            return currentServingToken;
        }
    }

    currentServingToken = 0;
    return 0;
}


    /**
     * Finishes the current customer's service:
     * Marks them as COMPLETED immediately (triggering their feedback form)
     * and sets counter to idle without calling a non-existent token.
     */
    public synchronized boolean finishCurrentToken() {
        boolean completedAny = false;
        for (QueueEntry entry : queue) {
            if ("SERVING".equalsIgnoreCase(entry.getStatus())) {
                entry.setStatus("COMPLETED");
                updateMongoStatus(entry, "COMPLETED");
                completedAny = true;
            }
        }
        currentServingToken = 0;
        return completedAny;
    }

    private void updateMongoStatus(QueueEntry entry, String status) {
        if (mongoTemplate != null) {
            try {
                org.springframework.data.mongodb.core.query.Query query =
                        new org.springframework.data.mongodb.core.query.Query(
                                org.springframework.data.mongodb.core.query.Criteria.where("_id").is(entry.persistenceId())
                        );
                org.springframework.data.mongodb.core.query.Update update =
                        new org.springframework.data.mongodb.core.query.Update().set("status", status);
                var result = mongoTemplate.updateFirst(query, update, "tokens");
                if (result.getMatchedCount() == 0) {
                    log.error(">>> [MongoDB] No document found to update status for token #{}", entry.getTokenNumber());
                    return;
                }
                log.info(">>> [MongoDB] Updated status to '{}' for token #{} in DB '{}'", status, entry.getTokenNumber(), mongoTemplate.getDb().getName());
            } catch (Exception e) {
                log.error(">>> [MongoDB] FAILED to update status for token #{}: {}", entry.getTokenNumber(), e.getMessage(), e);
            }
        }
    }

    /**
     * Saves a feedback submission into MongoDB Atlas and in-memory list.
     */
    public Map<String, Object> recordFeedback(int tokenNumber, String customerName, int rating, String comments) {
        Map<String, Object> fb = new HashMap<>();
        fb.put("tokenNumber", tokenNumber);
        fb.put("customerName", customerName);
        fb.put("rating", rating);
        fb.put("comments", comments);
        fb.put("submittedAt", new Date());

        feedbackList.add(fb);

        if (mongoTemplate != null) {
            try {
                mongoTemplate.save(fb, "feedback");
                log.info(">>> [MongoDB] Saved feedback for token #{} to collection 'feedback'", tokenNumber);
            } catch (Exception e) {
                log.error(">>> [MongoDB] FAILED to save feedback for token #{}: {}", tokenNumber, e.getMessage(), e);
            }
        }
        return fb;
    }

    public List<Map<String, Object>> getFeedbackList() {
        return new ArrayList<>(feedbackList);
    }

    /**
     * Inspects live MongoDB Atlas connection and fetches documents directly from collections.
     */
    public Map<String, Object> getMongoStatus() {
        Map<String, Object> result = new HashMap<>();
        if (mongoTemplate == null) {
            result.put("connected", false);
            result.put("message", "MongoTemplate bean is NULL");
            return result;
        }
        try {
            result.put("connected", true);
            result.put("database", mongoTemplate.getDb().getName());
            List<Map> tokens = mongoTemplate.findAll(Map.class, "tokens");
            List<Map> feedback = mongoTemplate.findAll(Map.class, "feedback");
            result.put("tokensCount", tokens.size());
            result.put("feedbackCount", feedback.size());
            result.put("tokens", tokens);
            result.put("feedback", feedback);
            return result;
        } catch (Exception e) {
            result.put("connected", false);
            result.put("error", e.getMessage());
            return result;
        }
    }

    /**
     * Resets the entire queue back to token #1 for a fresh queue session.
     */
    public synchronized void resetQueue() {
        queue.clear();
        nextTokenNumber = 1;
        currentServingToken = 0;
    }
}
