package com.cts.corda.etf.state;

import com.cts.corda.etf.schema.BuySchemaV1;
import com.google.common.collect.ImmutableList;
import net.corda.core.contracts.LinearState;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.Party;
import net.corda.core.schemas.MappedSchema;
import net.corda.core.schemas.PersistentState;
import net.corda.core.schemas.QueryableState;

import java.util.Arrays;
import java.util.List;

/**
 * The state object recording IOU agreements between two parties.
 * <p>
 * A state must implement [ContractState] or one of its descendants.
 */
public class SecurityBuyState implements LinearState, QueryableState {

    private UniqueIdentifier linearId;
    private Integer quantity;
    private String securityName;
    private Party seller;
    private Party buyer;
    private Party depository;
    private String status;

    /**
     * @param quantity the quantity of the IOU.
     * @param seller   the party issuing the IOU.
     * @param buyer    the party receiving and approving the IOU.
     */
    public SecurityBuyState(Integer quantity, String securityName, String status,
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

    public SecurityBuyState(Integer quantity, String securityName, String status,
                            Party buyer,
                            Party depository) {
        this.quantity = quantity;
        this.securityName = securityName;
        this.status = status;
        this.seller = null;
        this.buyer = buyer;
        this.depository = depository;
        this.linearId = new UniqueIdentifier();
    }


    public SecurityBuyState() {
        this.quantity = null;
        this.securityName = null;
        this.status = null;
        this.seller = null;
        this.buyer = null;
        this.depository = null;
        this.linearId = new UniqueIdentifier();
    }

    public String getSecurityName() {
        return securityName;
    }

    public void setSecurityName(String securityName) {
        this.securityName = securityName;
    }

    public Party getDepository() {
        return depository;
    }

    public void setDepository(Party depository) {
        this.depository = depository;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public Party getSeller() {
        return seller;
    }

    public void setSeller(Party seller) {
        this.seller = seller;
    }

    public Party getBuyer() {
        return buyer;
    }

    public void setBuyer(Party buyer) {
        this.buyer = buyer;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @Override
    public UniqueIdentifier getLinearId() {
        return linearId;
    }

    public void setLinearId(UniqueIdentifier linearId) {
        this.linearId = linearId;
    }

    @Override
    public List<AbstractParty> getParticipants() {
        return Arrays.asList(depository, buyer);
    }

    @Override
    public PersistentState generateMappedObject(MappedSchema schema) {
        if (schema instanceof BuySchemaV1) {
            return new BuySchemaV1.PersistentIOU(
                    this.depository.getName().toString(),
                    this.buyer.getName().toString(),
                    this.quantity, this.securityName, this.status,
                    this.linearId.getId());
        } else {
            throw new IllegalArgumentException("Unrecognised schema $schema");
        }
    }

    @Override
    public Iterable<MappedSchema> supportedSchemas() {
        return ImmutableList.of(new BuySchemaV1());
    }

    @Override
    public String toString() {
        return String.format("%s(iou=%s, securityName=%s seller=%s, buyer=%s, depository=%s, linearId=%s)", getClass().getSimpleName(), quantity, securityName, seller, buyer, depository, linearId);
    }
}