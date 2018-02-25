package com.cts.corda.etf.flow.sell;

import co.paralleluniverse.fibers.Suspendable;
import com.cts.corda.etf.contract.SecurityStock;
import com.google.common.collect.Sets;
import lombok.extern.slf4j.Slf4j;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.flows.*;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;

@InitiatingFlow
@Slf4j
public class MoveSecurityFlow extends FlowLogic<SignedTransaction> {
    private final StateAndRef stateAndRef;
    private final Party receiverParty;
    public MoveSecurityFlow(StateAndRef stateAndRef, Party receiverParty) {
        this.stateAndRef = stateAndRef;
        this.receiverParty = receiverParty;
        log.info("Inside MoveSecurityFlow for SellRequest called by ");
    }

    @Override
    @Suspendable
    public SignedTransaction call() throws FlowException {
        log.info("Inside MoveSecurityFlow for SellRequest call method ");
        final TransactionBuilder txBuilder = new TransactionBuilder(getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0));
        SecurityStock.generateMove(txBuilder, stateAndRef, receiverParty);
        txBuilder.verify(getServiceHub());
        final SignedTransaction partSignedTx = getServiceHub().signInitialTransaction(txBuilder);
        FlowSession receiverSession = initiateFlow(receiverParty);
        final SignedTransaction fullySignedTx = subFlow(new CollectSignaturesFlow(partSignedTx, Sets.newHashSet(receiverSession), CollectSignaturesFlow.Companion.tracker()));
        SignedTransaction fullySignedTx1 = subFlow(new FinalityFlow(fullySignedTx));
        return fullySignedTx1;
    }
}

