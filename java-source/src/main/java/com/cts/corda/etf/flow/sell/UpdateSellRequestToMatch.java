package com.cts.corda.etf.flow.sell;

import co.paralleluniverse.fibers.Suspendable;
import com.cts.corda.etf.contract.SellContract;
import com.cts.corda.etf.state.SecuritySellState;
import com.google.common.collect.Sets;
import lombok.extern.slf4j.Slf4j;
import net.corda.core.contracts.Command;
import net.corda.core.contracts.StateAndContract;
import net.corda.core.flows.*;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;

import java.util.stream.Collectors;

import static com.cts.corda.etf.contract.SellContract.SELL_SECURITY_CONTRACT_ID;
import static com.cts.corda.etf.util.Constants.SELL_MATCHED;

@InitiatingFlow
@Slf4j
public class UpdateSellRequestToMatch extends FlowLogic<SignedTransaction> {
    private final SecuritySellState securitySellState;

    public UpdateSellRequestToMatch(SecuritySellState securitySellState) {
        this.securitySellState = securitySellState;
        log.info("Inside UpdateSellRequestToMatch for SellRequest called by ");
    }

    @Override
    @Suspendable
    public SignedTransaction call() throws FlowException {
        final Party notary = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0);
        log.info("Inside UpdateSellRequestToMatch for SellRequest call method ");
        securitySellState.setStatus(SELL_MATCHED);
        final Command<SellContract.Commands.Create> txCommand2 = new Command<>(new SellContract.Commands.Create(),
                securitySellState.getParticipants().stream().map(AbstractParty::getOwningKey).collect(Collectors.toList()));
        final TransactionBuilder txBuilder = new TransactionBuilder(notary).withItems(new StateAndContract(securitySellState, SELL_SECURITY_CONTRACT_ID), txCommand2);
        txBuilder.verify(getServiceHub());
        final SignedTransaction partSignedTx = getServiceHub().signInitialTransaction(txBuilder);
        FlowSession depositorySession = initiateFlow(securitySellState.getDepository());
        // Send the state to the CounterParty, and receive it back with their signature.
        final SignedTransaction fullySignedTx = subFlow(new CollectSignaturesFlow(partSignedTx, Sets.newHashSet(depositorySession), CollectSignaturesFlow.Companion.tracker()));
        return subFlow(new FinalityFlow(fullySignedTx));
    }
}