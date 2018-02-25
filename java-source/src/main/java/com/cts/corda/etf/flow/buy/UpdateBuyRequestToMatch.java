package com.cts.corda.etf.flow.buy;

import co.paralleluniverse.fibers.Suspendable;
import com.cts.corda.etf.contract.BuyContract;
import com.cts.corda.etf.state.SecurityBuyState;
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

import static com.cts.corda.etf.contract.BuyContract.BUY_SECURITY_CONTRACT_ID;
import static com.cts.corda.etf.util.Constants.BUY_MATCHED;


@InitiatingFlow
@Slf4j
public class UpdateBuyRequestToMatch extends FlowLogic<SignedTransaction> {
    private final SecurityBuyState securityBuyState;

    public UpdateBuyRequestToMatch(SecurityBuyState securityBuyState) {
        this.securityBuyState = securityBuyState;
        log.info("Inside UpdateBuyRequestToMatch for BuyRequest called by ");
    }

    @Override
    @Suspendable
    public SignedTransaction call() throws FlowException {
        final Party notary = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0);
        log.info("Inside UpdateBuyRequestToMatch for BuyRequest call method ");
        securityBuyState.setStatus(BUY_MATCHED);
        final Command<BuyContract.Commands.Create> txCommand2 = new Command<>(new BuyContract.Commands.Create(),
                securityBuyState.getParticipants().stream().map(AbstractParty::getOwningKey).collect(Collectors.toList()));
        final TransactionBuilder txBuilder = new TransactionBuilder(notary).withItems(new StateAndContract(securityBuyState, BUY_SECURITY_CONTRACT_ID), txCommand2);
        txBuilder.verify(getServiceHub());
        final SignedTransaction partSignedTx = getServiceHub().signInitialTransaction(txBuilder);
        FlowSession depositorySession = initiateFlow(securityBuyState.getDepository());
        // Send the state to the CounterParty, and receive it back with their signature.
        final SignedTransaction fullySignedTx = subFlow(new CollectSignaturesFlow(partSignedTx, Sets.newHashSet(depositorySession), CollectSignaturesFlow.Companion.tracker()));
        return subFlow(new FinalityFlow(fullySignedTx));
    }
}