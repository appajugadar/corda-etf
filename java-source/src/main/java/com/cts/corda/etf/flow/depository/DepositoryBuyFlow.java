package com.cts.corda.etf.flow.depository;

import co.paralleluniverse.fibers.Suspendable;
import com.cts.corda.etf.contract.SellContract;
import com.cts.corda.etf.flow.buy.APBuyFlow;
import com.cts.corda.etf.state.SecurityBuyState;
import com.cts.corda.etf.state.SecuritySellState;
import com.cts.corda.etf.util.RequestHelper;
import com.google.common.collect.Sets;
import lombok.extern.slf4j.Slf4j;
import net.corda.core.contracts.Command;
import net.corda.core.contracts.ContractState;
import net.corda.core.contracts.StateAndContract;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.flows.*;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.Party;
import net.corda.core.node.services.Vault;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.core.utilities.ProgressTracker;

import java.util.List;
import java.util.stream.Collectors;

import static com.cts.corda.etf.contract.SellContract.SELL_SECURITY_CONTRACT_ID;
import static com.cts.corda.etf.util.Constants.BUY_MATCHED;
import static com.cts.corda.etf.util.Constants.SELL_MATCHED;
import static net.corda.core.contracts.ContractsDSL.requireThat;


@InitiatedBy(APBuyFlow.class)
@InitiatingFlow
@Slf4j
public class DepositoryBuyFlow extends FlowLogic<SignedTransaction> {

    private final FlowSession flowSession;

    public DepositoryBuyFlow(FlowSession flowSession) {
        this.flowSession = flowSession;
        log.info("Inside DepositoryBuyFlow called by " + flowSession.getCounterparty());
    }

    @Suspendable
    @Override
    public SignedTransaction call() throws FlowException {
        log.info("DepositoryBuyFlow inside call method ");
        //check vault for sell states and if found then return
        Vault.Page<SecuritySellState> results = getServiceHub().getVaultService().queryBy(SecuritySellState.class);
        List<SecuritySellState> securitySellStateList = RequestHelper.getUnmatchedSecuritySellState(results.getStates());

        SecuritySellState securitySellState = null;
        if (!securitySellStateList.isEmpty()) {
            securitySellState = securitySellStateList.get(0);
        }

        log.info("DepositoryBuyFlow flowSession " + flowSession.getCounterpartyFlowInfo());

        if (securitySellState != null) {
            //update sell state
            securitySellState.setBuyer(flowSession.getCounterparty());
            securitySellState.setStatus(SELL_MATCHED);
            // Obtain a reference to the notary we want to use.
            final Party notary = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0);
            final Command<SellContract.Commands.Create> txCommand = new Command<>(new SellContract.Commands.Create(),
                    securitySellState.getParticipants().stream().map(AbstractParty::getOwningKey).collect(Collectors.toList()));
            final TransactionBuilder txBuilder = new TransactionBuilder(notary).withItems(new StateAndContract(securitySellState, SELL_SECURITY_CONTRACT_ID), txCommand);
            // Verify that the transaction is valid.
            txBuilder.verify(getServiceHub());
            // Sign the transaction.
            final SignedTransaction partSignedTx = getServiceHub().signInitialTransaction(txBuilder);
            FlowSession sellerSession = initiateFlow(securitySellState.getSeller());
            final SignedTransaction fullySignedTx = subFlow(new CollectSignaturesFlow(partSignedTx, Sets.newHashSet(sellerSession), CollectSignaturesFlow.Companion.tracker()));
            // Notarise and record the transaction in both parties' vaults.
            subFlow(new FinalityFlow(fullySignedTx));
        } else {
            //  flowSession.send(new SecurityBuyState());
        }

        SignedTransaction tx = subFlow(new SignTxFlow(flowSession, SignTransactionFlow.Companion.tracker()));
        return tx;
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
