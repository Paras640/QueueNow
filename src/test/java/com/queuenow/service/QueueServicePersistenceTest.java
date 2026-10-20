package com.queuenow.service;

import com.mongodb.client.MongoDatabase;
import com.mongodb.client.result.UpdateResult;
import com.queuenow.model.QueueEntry;
import org.bson.Document;
import org.junit.jupiter.api.Test;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class QueueServicePersistenceTest {

    @Test
    void queueStatusesUpdateTheUniqueDocumentRatherThanReusedTokenNumbers() {
        MongoTemplate mongoTemplate = mock(MongoTemplate.class);
        MongoDatabase database = mock(MongoDatabase.class);
        when(mongoTemplate.getDb()).thenReturn(database);
        when(database.getName()).thenReturn("queuenow");
        when(mongoTemplate.updateFirst(any(Query.class), any(), eq("tokens")))
                .thenReturn(UpdateResult.acknowledged(1, 1L, null));

        QueueService service = new QueueService(mongoTemplate);
        QueueEntry firstEntry = service.joinQueue("Alex");
        QueueService nextQueueSession = new QueueService(mongoTemplate);
        QueueEntry reusedTokenEntry = nextQueueSession.joinQueue("Sam");

        assertEquals(firstEntry.getTokenNumber(), reusedTokenEntry.getTokenNumber());
        assertNotEquals(firstEntry.persistenceId(), reusedTokenEntry.persistenceId());

        nextQueueSession.callNextToken();
        nextQueueSession.finishCurrentToken();

        var queryCaptor = org.mockito.ArgumentCaptor.forClass(Query.class);
        verify(mongoTemplate, org.mockito.Mockito.times(2))
                .updateFirst(queryCaptor.capture(), any(), eq("tokens"));

        for (Query query : queryCaptor.getAllValues()) {
            Document filter = query.getQueryObject();
            assertEquals(reusedTokenEntry.persistenceId(), filter.getString("_id"));
        }
    }

}
