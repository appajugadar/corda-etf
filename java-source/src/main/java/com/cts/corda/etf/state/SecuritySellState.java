package com.cts.corda.etf.state;

import com.cts.corda.etf.schema.SellSchemaV1;
import com.google.common.collect.ImmutableList;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.corda.core.contracts.LinearState;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.Party;
import net.corda.core.schemas.MappedSchema;
import net.corda.core.schemas.PersistentState;
import net.corda.core.schemas.QueryableState;

import java.util.Arrays;
import java.util.List;

@Slf4j
public class SecuritySellState implements LinearState, QueryableState {

    private final UniqueIdentifier linearId;
    @Getter
    @Setter
    private Integer quantity;
    @Getter
    @Setter
    private String securityName;
    @Getter
    @Setter
    private Party seller;
    @Getter
    @Setter
    private Party buyer;
    @Getter
    @Setter
    private Party depository;
    @Getter
    @Setter
    private String status;

    public SecuritySellState(Integer quantity, String securityName, String status,
                             Party seller,
                             Party buyer, Party depository) {
        this.quantity = quantity;
        this.securityName = securityName;
        this.status = status;
        this.seller = seller;
        this.buyer = buyer;
        this.depository = depository;
        this.linearId = new UniqueIdentifier();
    }

    public SecuritySellState(Integer quantity, String securityName, String status,
                             Party seller,
                             Party depository) {
        this.quantity = quantity;
        this.securityName = securityName;
        this.seller = seller;
        this.status = status;
        this.buyer = null;
        this.depository = depository;
        this.linearId = new UniqueIdentifier();
    }

    @Override
    public UniqueIdentifier getLinearId() {
        return linearId;
    }

    @Override
    public List<AbstractParty> getParticipants() {
        return Arrays.asList(depository, seller);
    }

    @Override
    public PersistentState generateMappedObject(MappedSchema schema) {
        if (schema instanceof SellSchemaV1) {
            return new SellSchemaV1.PersistentIOU(
                    this.depository.getName().toString(),
                    this.seller.getName().toString(),
                    this.quantity, this.securityName, this.status,
                    this.linearId.getId());
        } else {
            throw new IllegalArgumentException("Unrecognised schema $schema");
        }
    }

    @Override
    public Iterable<MappedSchema> supportedSchemas() {
        return ImmutableList.of(new SellSchemaV1());
    }

    @Override
    public String toString() {
        return String.format("%s(quantity=%s, securityName=%s seller=%s, buyer=%s, depository=%s, status=%s, linearId=%s)", getClass().getSimpleName(), quantity, securityName, seller, buyer, depository, status, linearId);
    }
}