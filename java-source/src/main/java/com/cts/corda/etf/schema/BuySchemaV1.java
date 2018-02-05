package com.cts.corda.etf.schema;

import com.google.common.collect.ImmutableList;
import net.corda.core.schemas.MappedSchema;
import net.corda.core.schemas.PersistentState;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.util.UUID;

/**
 * An SecurityBuyState schema.
 */
public class BuySchemaV1 extends MappedSchema {
    public BuySchemaV1() {
        super(BuySchema.class, 1, ImmutableList.of(PersistentIOU.class));
    }

    @Entity
    @Table(name = "buy_states")
    public static class PersistentIOU extends PersistentState {
        @Column(name = "linear_id")
        private final UUID linearId;
        @Column(name = "depository")
        private String depository;
        @Column(name = "buyer")
        private String buyer;
        @Column(name = "quantity")
        private int quantity;
        @Column(name = "status")
        private String status;
        @Column(name = "securityName")
        private String securityName;


        public PersistentIOU(String depository, String buyer, int quantity, String securityName, String status, UUID linearId) {
            this.depository = depository;
            this.buyer = buyer;
            this.quantity = quantity;
            this.securityName = securityName;
            this.status = status;
            this.linearId = linearId;
        }

        // Default constructor required by hibernate.
        public PersistentIOU() {
            this.depository = null;
            this.buyer = null;
            this.quantity = 0;
            this.securityName = null;
            this.status = null;
            this.linearId = null;
        }

        public String getDepository() {
            return depository;
        }

        public void setDepository(String depository) {
            this.depository = depository;
        }

        public String getBuyer() {
            return buyer;
        }

        public void setBuyer(String buyer) {
            this.buyer = buyer;
        }

        public int getQuantity() {
            return quantity;
        }

        public void setQuantity(int quantity) {
            this.quantity = quantity;
        }

        public String getSecurityName() {
            return securityName;
        }

        public void setSecurityName(String securityName) {
            this.securityName = securityName;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public UUID getId() {
            return linearId;
        }
    }
}