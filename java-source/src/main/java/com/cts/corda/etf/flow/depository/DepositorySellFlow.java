package com.cts.corda.etf.flow.depository;

import co.paralleluniverse.fibers.Suspendable;
import com.cts.corda.etf.contract.BuyContract;
import com.cts.corda.etf.flow.sell.APSellFlow;
import com.cts.corda.etf.state.SecurityBuyState;
import com.cts.corda.etf.state.SecuritySellState;
import com.cts.corda.etf.util.RequestHelper;
import com.google.common.collect.Sets;
import net.corda.core.contracts.Command;
import net.corda.core.contracts.ContractState;
import net.corda.core.contracts.StateAndContract;
import net.corda.core.flows.*;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.Party;
import net.corda.core.node.services.Vault;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.core.utilities.ProgressTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.stream.Collectors;

import static net.corda.core.contracts.ContractsDSL.requireThat;


@InitiatedBy(APSellFlow.class)
@InitiatingFlow
public class DepositorySellFlow extends FlowLogic<SignedTransaction> {

    static private final Logger logger = LoggerFactory.getLogger(DepositorySellFlow.class);
    private final FlowSession flowSession;

    public DepositorySellFlow(FlowSession flowSession) {
        this.flowSession = flowSession;
        System.out.println("Inside DepositorySellFlow called by " + flowSession.getCounterparty());
    }

    @Suspendable
    @Override
    public SignedTransaction call() throws FlowException {

        logger.info("DepositoryBuyFlow inside call method ");

        //check vault for sell states and if found then return
        Vault.Page<SecurityBuyState> results = getServiceHub().getVaultService().queryBy(SecurityBuyState.class);
        List<SecurityBuyState> securityBuyStateList = RequestHelper.getUnmatchedSecurityBuyState(results.getStates());

        SecurityBuyState securityBuyState = null;

        if(!securityBuyStateList.isEmpty()){
            securityBuyState = securityBuyStateList.get(0);
        }

        logger.info("DepositoryBuyFlow flowSession " + flowSession.getCounterpartyFlowInfo());
        if (securityBuyState != null) {
            //update sell state
            securityBuyState.setSeller(flowSession.getCounterparty());
            securityBuyState.setStatus("BUY_MATCHED");

            // Obtain a reference to the notary we want to use.
            final Party notary = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0);
            final Command<BuyContract.Commands.Create> txCommand = new Command<>(new BuyContract.Commands.Create(),
                    securityBuyState.getParticipants().stream().map(AbstractParty::getOwningKey).collect(Collectors.toList()));
            final TransactionBuilder txBuilder = new TransactionBuilder(notary).withItems(new StateAndContract(securityBuyState, BuyContract.BUY_SECURITY_CONTRACT_ID), txCommand);
            // Verify that the transaction is valid.
            txBuilder.verify(getServiceHub());
            // Sign the transaction.
            final SignedTransaction partSignedTx = getServiceHub().signInitialTransaction(txBuilder);
            logger.info("securityBuyState.getBuyer() " + securityBuyState.getBuyer());
            FlowSession sellerSession = initiateFlow(securityBuyState.getBuyer());
            final SignedTransaction fullySignedTx = subFlow(new CollectSignaturesFlow(partSignedTx, Sets.newHashSet(sellerSession), CollectSignaturesFlow.Companion.tracker()));
            // Notarise and record the transaction in both parties' vaults.
            subFlow(new FinalityFlow(fullySignedTx));

        } else {
            logger.info("No buy request found ");
        }


        SignedTransaction stx = subFlow(new SignTxFlow(flowSession, SignTransactionFlow.Companion.tracker()));
        return stx;
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
                SecuritySellState iou = (SecuritySellState) output;

                require.using("I won't accept Sell with a value over 100.", iou.getQuantity() <= 100);
                return null;
            });
        }
    }
}