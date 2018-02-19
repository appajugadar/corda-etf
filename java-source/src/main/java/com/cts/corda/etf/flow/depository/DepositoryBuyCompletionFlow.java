package com.cts.corda.etf.flow.depository;

import co.paralleluniverse.fibers.Suspendable;
import com.cts.corda.etf.flow.buy.APBuyFlow;
import com.cts.corda.etf.state.SecurityBuyState;
import com.cts.corda.etf.state.SecuritySellState;
import net.corda.core.contracts.ContractState;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.flows.*;
import net.corda.core.node.services.Vault;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.utilities.ProgressTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static net.corda.core.contracts.ContractsDSL.requireThat;


@InitiatedBy(APBuyFlow.APBuySubFlow.class)
@InitiatingFlow
public class DepositoryBuyCompletionFlow extends FlowLogic<SignedTransaction> {

    static private final Logger logger = LoggerFactory.getLogger(DepositoryBuyCompletionFlow.class);
    private final FlowSession flowSession;

    public DepositoryBuyCompletionFlow(FlowSession flowSession) {
        this.flowSession = flowSession;
        System.out.println("Inside DepositoryBuyCompletionFlow called by " + flowSession.getCounterparty());
    }

    @Suspendable
    @Override
    public SignedTransaction call() throws FlowException {

        logger.info("DepositoryBuyFlow inside call method ");

        //check vault for sell states and if found then return
        Vault.Page<SecuritySellState> results = getServiceHub().getVaultService().queryBy(SecuritySellState.class);
        List<StateAndRef<SecuritySellState>> ref = results.getStates();
        SecuritySellState securitySellState = null;

        for (StateAndRef<SecuritySellState> stateref : ref) {
            securitySellState = stateref.getState().getData();
        }

        logger.info("DepositoryBuyFlow flowSession " + flowSession.getCounterpartyFlowInfo());
        logger.info("Sending back sign to APBuy Sub Flow " + securitySellState);

        if (securitySellState != null && securitySellState.getStatus().equals("SELL_MATCHED")) {
            SignedTransaction tx = subFlow(new SignTxFlow(flowSession, SignTransactionFlow.Companion.tracker()));
            return tx;
        } else {
            return null;
        }
        /*SignedTransaction tx = subFlow(new SignTxFlow(flowSession, SignTransactionFlow.Companion.tracker()));
        return tx;*/
    }

    class SignTxFlow extends SignTransactionFlow {

        private SignTxFlow(FlowSession otherPartyFlow, ProgressTracker progressTracker) {
            super(otherPartyFlow, progressTracker);
        }

        @Override
        protected void checkTransaction(SignedTransaction stx) {
            requireThat(require -> {
                ContractState output = stx.getTx().getOutputs().get(0).getData();
                require.using("This must be an SecurityBuy transaction.", output instanceof SecurityBuyState);
                return null;
            });
        }
    }
}
