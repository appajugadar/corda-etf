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
public class SellSchemaV1 extends MappedSchema {
    public SellSchemaV1() {
        super(SellSchema.class, 1, ImmutableList.of(PersistentIOU.class));
    }

    @Entity
    @Table(name = "sell_states")
    public static class PersistentIOU extends PersistentState {

        @Column(name = "depository")
        private final String depository;
        @Column(name = "seller")
        private final String seller;
        @Column(name = "quantity")
        private final int quantity;

        @Column(name = "securityName")
        private final String securityName;

        @Column(name = "status")
        private final String status;


        @Column(name = "linear_id")
        private final UUID linearId;


        public PersistentIOU(String depository, String seller, int quantity, String securityName, String status, UUID linearId) {
            this.depository = depository;
            this.seller = seller;
            this.status = status;
            this.quantity = quantity;
            this.securityName = securityName;
            this.linearId = linearId;
        }

        // Default constructor required by hibernate.
        public PersistentIOU() {
            this.depository = null;
            this.seller = null;
            this.status = null;
            this.quantity = 0;
            this.securityName = null;
            this.linearId = null;
        }

        public String getDepository() {
            return depository;
        }

        public String getSeller() {
            return seller;
        }

        public int getQuantity() {
            return quantity;
        }

        public String getSecurityName() {
            return securityName;
        }

        public String getStatus() {
            return status;
        }

        public UUID getId() {
            return linearId;
        }
    }
}