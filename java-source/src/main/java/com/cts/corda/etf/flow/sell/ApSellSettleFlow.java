package com.cts.corda.etf.flow.sell;

import co.paralleluniverse.fibers.Suspendable;
import com.cts.corda.etf.contract.SecurityStock;
import com.cts.corda.etf.contract.SellContract;
import com.cts.corda.etf.flow.buy.APBuyCompletionFlow;
import com.cts.corda.etf.state.SecuritySellState;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.stream.Collectors;

import static com.cts.corda.etf.contract.SecurityStock.SECURITY_STOCK_CONTRACT;
import static com.cts.corda.etf.contract.SellContract.SELL_SECURITY_CONTRACT_ID;

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


        //UPDATE sell request as matched
        //check vault for sell states and if found then return
        List<StateAndRef<SecuritySellState>> ref = getServiceHub().getVaultService().queryBy(SecuritySellState.class).getStates();
        SecuritySellState securitySellState = null;

        for (StateAndRef<SecuritySellState> stateref : ref) {
            securitySellState = stateref.getState().getData();
            if (securitySellState.getStatus().equals("SELL_MATCHED")) {

            }
        }

        if (securitySellState != null) {
            //update sell state
            securitySellState.setBuyer(flowSession.getCounterparty());
            securitySellState.setStatus("SELL_MATCHED");
            final Command<SellContract.Commands.Create> txCommand2 = new Command<>(new SellContract.Commands.Create(),
                    securitySellState.getParticipants().stream().map(AbstractParty::getOwningKey).collect(Collectors.toList()));
            final TransactionBuilder txBuilder2 = new TransactionBuilder(notary).withItems(new StateAndContract(securitySellState, SELL_SECURITY_CONTRACT_ID), txCommand2);
            txBuilder2.verify(getServiceHub());
            final SignedTransaction partSignedTx2 = getServiceHub().signInitialTransaction(txBuilder2);

            FlowSession depositorySession = initiateFlow(securitySellState.getDepository());
            logger.info("AP Sell flow initiated depo flow ");
            // Send the state to the counterparty, and receive it back with their signature.
            final SignedTransaction fullySignedTx2 = subFlow(new CollectSignaturesFlow(partSignedTx2, Sets.newHashSet(depositorySession), CollectSignaturesFlow.Companion.tracker()));
            subFlow(new FinalityFlow(fullySignedTx2));

            //Report to regulator
            subFlow(new ReportToRegulatorFlow(fullySignedTx2));
        }


        return fullySignedTx1;
    }


    @InitiatingFlow
    public class ReportToRegulatorFlow extends FlowLogic<String> {
        private final SignedTransaction fullySignedTx;

        public ReportToRegulatorFlow(SignedTransaction fullySignedTx) {
            this.fullySignedTx = fullySignedTx;
            System.out.println("Inside ReportToRegulatorFlow for SellRequest called by ");
        }

        @Override
        @Suspendable
        public String call() throws FlowException {
            System.out.println("Inside ReportToRegulatorFlow for SellRequest call method ");
            Party regulator = (Party) getServiceHub().getIdentityService().partiesFromName("Regulator", true).toArray()[0];
            FlowSession session = initiateFlow(regulator);
            subFlow(new SendTransactionFlow(session, fullySignedTx));
            return "Success";
        }
    }
}

