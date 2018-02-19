package com.cts.corda.etf.flow.sell;

import co.paralleluniverse.fibers.Suspendable;
import com.cts.corda.etf.contract.SettlementContract;
import com.cts.corda.etf.flow.depository.DepositoryBuyFlow;
import com.cts.corda.etf.state.SecuritySellState;
import com.google.common.collect.Sets;
import net.corda.core.contracts.Command;
import net.corda.core.contracts.ContractState;
import net.corda.core.contracts.StateAndContract;
import net.corda.core.flows.*;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.core.utilities.ProgressTracker;
import net.corda.finance.flows.TwoPartyTradeFlow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static com.cts.corda.etf.contract.SettlementContract.Settlement_SECURITY_CONTRACT_ID;
import static net.corda.core.contracts.ContractsDSL.requireThat;

@InitiatingFlow
@InitiatedBy(DepositoryBuyFlow.class)
public class APSellCompletionFlow extends FlowLogic<SignedTransaction> {

    static private final Logger logger = LoggerFactory.getLogger(APSellCompletionFlow.class);
    private final FlowSession flowSession;
    private final ProgressTracker.Step SELF_ISSUING = new ProgressTracker.Step("Got session ID back, issuing and timestamping some commercial paper");
    private final ProgressTracker.Step TRADING = new ProgressTracker.Step("Starting the trade flow.") {
        @Override
        public ProgressTracker childProgressTracker() {
            return TwoPartyTradeFlow.Seller.Companion.tracker();
        }
    };

    public APSellCompletionFlow(FlowSession flowSession) {
        this.flowSession = flowSession;
        System.out.println("Inside APSellCompletionFlow called by " + flowSession.getCounterparty());
    }

    @Suspendable
    @Override
    public SignedTransaction call() throws FlowException {
        logger.info("APSellCompletionFlow inside call method ");

        SignedTransaction stx = subFlow(new APSellCompletionFlow.SignTxFlow(flowSession, SignTransactionFlow.Companion.tracker()));
        ContractState output = stx.getTx().getOutputs().get(0).getData();
        SecuritySellState newSellState = (SecuritySellState) output;

        final Party notary = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0);
        List<Party> ls = new ArrayList<>();
        ls.add(newSellState.getBuyer());
        ls.add(newSellState.getSeller());

        final Command<SettlementContract.Commands.Create> txCommand = new Command<>(new SettlementContract.Commands.Create(), ls.stream().map(AbstractParty::getOwningKey).collect(Collectors.toList()));
        final TransactionBuilder txBuilder = new TransactionBuilder(notary).withItems(new StateAndContract(newSellState, Settlement_SECURITY_CONTRACT_ID), txCommand);

        final SignedTransaction partSignedTx = getServiceHub().signInitialTransaction(txBuilder);

        logger.info("newSellState.getBuyer() " + (newSellState.getBuyer()));
        FlowSession buyerSession = initiateFlow(newSellState.getBuyer());
        logger.info("buyerSession is null " + (buyerSession == null));

        final SignedTransaction fullySignedTx = subFlow(new CollectSignaturesFlow(partSignedTx, Sets.newHashSet(buyerSession), CollectSignaturesFlow.Companion.tracker()));
        subFlow(new FinalityFlow(fullySignedTx));
        return fullySignedTx;
    }

    class SignTxFlow extends SignTransactionFlow {

        private SignTxFlow(FlowSession otherPartyFlow, ProgressTracker progressTracker) {
            super(otherPartyFlow, progressTracker);
        }

        @Override
        protected void checkTransaction(SignedTransaction stx) {
            requireThat(require -> {
                ContractState output = stx.getTx().getOutputs().get(0).getData();
                require.using("This must be an SecuritySell transaction.", output instanceof SecuritySellState);
                SecuritySellState newSellState = (SecuritySellState) output;
                logger.info("Adding new state to o/p");
                require.using("I won't accept SecurityBuy with a quantity over 100.", newSellState.getQuantity() <= 100);
                return null;
            });
        }
    }
}

