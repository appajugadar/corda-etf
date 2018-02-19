package com.cts.corda.etf.flow;

import co.paralleluniverse.fibers.Suspendable;
import com.cts.corda.etf.contract.SecurityStock;
import com.google.common.collect.Sets;
import net.corda.core.contracts.Command;
import net.corda.core.contracts.PartyAndReference;
import net.corda.core.contracts.StateAndContract;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.flows.*;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.core.utilities.OpaqueBytes;
import net.corda.core.utilities.ProgressTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.stream.Collectors;

import static com.cts.corda.etf.contract.SecurityStock.SECURITY_STOCK_CONTRACT;

@InitiatingFlow
@InitiatedBy(APBuyCompletionFlow.class)
public class ApSellSettleFlow extends FlowLogic<SignedTransaction> {

    static private final Logger logger = LoggerFactory.getLogger(ApSellSettleFlow.class);
    private final FlowSession flowSession;

    public ApSellSettleFlow(FlowSession flowSession) {
        this.flowSession = flowSession;
        System.out.println("Inside ApSellSettleFlow called by " + flowSession.getCounterparty());
    }

    @Suspendable
    @Override
    public SignedTransaction call() throws FlowException {
        System.out.println("Inside ApSellSettleFlow call " + flowSession.getCounterparty());

        List<StateAndRef<SecurityStock.State>> etfTradeStatesQueryResp = getServiceHub().getVaultService().queryBy(SecurityStock.State.class).getStates();
        SecurityStock.State securityState = null;
        for (StateAndRef<SecurityStock.State> stateAndRef : etfTradeStatesQueryResp) {
            securityState = stateAndRef.getState().getData();
        }
        final Party notary = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0);

        //
        PartyAndReference issuer1 = this.getOurIdentity().ref(OpaqueBytes.of((securityState.getSecurityName() + securityState.getQuantity()).getBytes()));
        SecurityStock.State etfTradeState1 = new SecurityStock.State(issuer1, getOurIdentity(), securityState.getSecurityName(), -securityState.getQuantity());
        final Command<SecurityStock.Commands.Issue> txCommand1 = new Command<>(new SecurityStock.Commands.Issue(),
                securityState.getParticipants().stream().map(AbstractParty::getOwningKey).collect(Collectors.toList()));
        final TransactionBuilder txBuilder1 = new TransactionBuilder(notary).withItems(new StateAndContract(etfTradeState1, SECURITY_STOCK_CONTRACT), txCommand1);
        txBuilder1.verify(getServiceHub());
        final SignedTransaction partSignedTx1 = getServiceHub().signInitialTransaction(txBuilder1);
        subFlow(new FinalityFlow(partSignedTx1));
        //

        PartyAndReference issuer = this.getOurIdentity().ref(OpaqueBytes.of((securityState.getSecurityName() + securityState.getQuantity()).getBytes()));
        SecurityStock.State etfTradeState = new SecurityStock.State(issuer, flowSession.getCounterparty(), securityState.getSecurityName(), securityState.getQuantity());
        final Command<SecurityStock.Commands.Issue> txCommand = new Command<>(new SecurityStock.Commands.Issue(),
                securityState.getParticipants().stream().map(AbstractParty::getOwningKey).collect(Collectors.toList()));
        final TransactionBuilder txBuilder = new TransactionBuilder(notary).withItems(new StateAndContract(etfTradeState, SECURITY_STOCK_CONTRACT), txCommand);
        // Verify that the transaction is valid.
        getLogger().info("Before verify ApSellSettleFlow");
        txBuilder.verify(getServiceHub());
        final SignedTransaction partSignedTx = getServiceHub().signInitialTransaction(txBuilder);
        //FlowSession fs = initiateFlow(flowSession.getCounterparty());
        final SignedTransaction fullySignedTx = subFlow(new CollectSignaturesFlow(partSignedTx, Sets.newHashSet(flowSession), CollectSignaturesFlow.Companion.tracker()));

        System.out.println("Inside EtfIssue flow finalize tx11");
        SignedTransaction fullySignedTx1 = subFlow(new FinalityFlow(fullySignedTx));
        return fullySignedTx1;
    }

    //  @InitiatedBy(ApSellSettleFlow.class)
    class SignTxFlow extends SignTransactionFlow {
        private SignTxFlow(FlowSession otherPartyFlow) {
            super(otherPartyFlow, null);
            logger.info("ApSellSettleFlow SignTxFlow  ");
        }

        private SignTxFlow(FlowSession otherPartyFlow, ProgressTracker progressTracker) {
            super(otherPartyFlow, progressTracker);
            logger.info("ApSellSettleFlow SignTxFlow  ");
        }

        @Override
        protected void checkTransaction(SignedTransaction stx) {
            logger.info("ApSellSettleFlow SignTxFlow checkTransaction ");
        }
    }
}

