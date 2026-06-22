package com.acme.audit;

import com.acme.audit.testsupport.TestData;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("AuditEventPublisher enum overloads")
class AuditEventPublisherEnumTest {

    /** Guards the process-wide {@link Audit} delegate so facade tests never race other writers. */
    static final String AUDIT_FACADE = "com.acme.audit.Audit";

    enum Sample { ORDER_CANCELLED }

    private final Capturing publisher = new Capturing();

    @Test
    @DisplayName("Publishing an enum stores its constant name as the type")
    void publishEnumStoresName() {
        publisher.publish(Sample.ORDER_CANCELLED);

        assertThat(publisher.type).isEqualTo(TestData.ORDER_CANCELLED);
        assertThat(publisher.metadata).isNull();
    }

    @Test
    @DisplayName("Publishing an enum with metadata stores the name and the metadata")
    void publishEnumWithMetadataStoresNameAndMetadata() {
        publisher.publish(Sample.ORDER_CANCELLED, "payload");

        assertThat(publisher.type).isEqualTo(TestData.ORDER_CANCELLED);
        assertThat(publisher.metadata).isEqualTo("payload");
    }

    @Test
    @DisplayName("Publishing as an actor stores the enum name and the actor")
    void publishAsEnumStoresNameAndActor() {
        publisher.publishAs(TestData.ALICE, Sample.ORDER_CANCELLED, "payload");

        assertThat(publisher.createdBy).isEqualTo(TestData.ALICE);
        assertThat(publisher.type).isEqualTo(TestData.ORDER_CANCELLED);
        assertThat(publisher.metadata).isEqualTo("payload");
    }

    @Test
    @DisplayName("Recording an enum through the Audit facade stores the name")
    @ResourceLock(AUDIT_FACADE)
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
