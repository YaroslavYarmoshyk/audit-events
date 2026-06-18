package com.acme.audit;

import com.acme.audit.testsupport.TestData;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AuditEventPublisherEnumTest {

    enum Sample { ORDER_CANCELLED }

    private final Capturing publisher = new Capturing();

    @Test
    void publishEnumStoresName() {
        publisher.publish(Sample.ORDER_CANCELLED);

        assertThat(publisher.type).isEqualTo(TestData.ORDER_CANCELLED);
        assertThat(publisher.metadata).isNull();
    }

    @Test
    void publishEnumWithMetadataStoresNameAndMetadata() {
        publisher.publish(Sample.ORDER_CANCELLED, "payload");

        assertThat(publisher.type).isEqualTo(TestData.ORDER_CANCELLED);
        assertThat(publisher.metadata).isEqualTo("payload");
    }

    @Test
    void publishAsEnumStoresNameAndActor() {
        publisher.publishAs(TestData.ALICE, Sample.ORDER_CANCELLED, "payload");

        assertThat(publisher.createdBy).isEqualTo(TestData.ALICE);
        assertThat(publisher.type).isEqualTo(TestData.ORDER_CANCELLED);
        assertThat(publisher.metadata).isEqualTo("payload");
    }

    @Test
    void facadeRecordEnumStoresName() {
        try {
            Audit.setPublisher(publisher);
            Audit.record(Sample.ORDER_CANCELLED);
            assertThat(publisher.type).isEqualTo(TestData.ORDER_CANCELLED);
        } finally {
            Audit.setPublisher(null);
        }
    }

    /** Captures the resolved String arguments so the enum -> name() conversion can be asserted. */
    private static final class Capturing implements AuditEventPublisher {

        private @Nullable String type;
        private @Nullable Object metadata;
        private @Nullable String createdBy;

        @Override
        public void publish(String type, @Nullable Object metadata) {
            this.type = type;
            this.metadata = metadata;
        }

        @Override
        public void publishAs(String createdBy, String type, @Nullable Object metadata) {
            this.createdBy = createdBy;
            publish(type, metadata);
        }
    }
}
