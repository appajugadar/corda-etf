package com.cts.corda.etf.flow.sell;

import co.paralleluniverse.fibers.Suspendable;
import com.cts.corda.etf.contract.SellContract;
import com.cts.corda.etf.state.SecuritySellState;
import com.google.common.collect.Sets;
import net.corda.core.contracts.Command;
import net.corda.core.contracts.StateAndContract;
import net.corda.core.flows.*;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.core.utilities.ProgressTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.stream.Collectors;

import static com.cts.corda.etf.contract.SellContract.SELL_SECURITY_CONTRACT_ID;


@InitiatingFlow
@StartableByRPC
public class APSellFlow extends FlowLogic<SignedTransaction> {

    static private final Logger logger = LoggerFactory.getLogger(APSellFlow.class);

    private final int quantity;
    private final Party depositoryParty;
    private final String securityName;

    private final ProgressTracker.Step GENERATING_TRANSACTION = new ProgressTracker.Step("Generating transaction based on new IOU.");
    private final ProgressTracker.Step VERIFYING_TRANSACTION = new ProgressTracker.Step("Verifying contract constraints.");
    private final ProgressTracker.Step SIGNING_TRANSACTION = new ProgressTracker.Step("Signing transaction with our private key.");
    private final ProgressTracker.Step GATHERING_SIGS = new ProgressTracker.Step("Gathering the counterparty's signature.") {
        @Override
        public ProgressTracker childProgressTracker() {
            return CollectSignaturesFlow.Companion.tracker();
        }
    };
    private final ProgressTracker.Step FINALISING_TRANSACTION = new ProgressTracker.Step("Obtaining notary signature and recording transaction.") {
        @Override
        public ProgressTracker childProgressTracker() {
            return FinalityFlow.Companion.tracker();
        }
    };

    // The progress tracker checkpoints each stage of the flow and outputs the specified messages when each
    // checkpoint is reached in the code. See the 'progressTracker.currentStep' expressions within the call()
    // function.
    private final ProgressTracker progressTracker = new ProgressTracker(
            GENERATING_TRANSACTION,
            VERIFYING_TRANSACTION,
            SIGNING_TRANSACTION,
            GATHERING_SIGS,
            FINALISING_TRANSACTION
    );

    public APSellFlow(int quantity, String securityName, Party depositoryParty) {
        this.quantity = quantity;
        this.securityName = securityName;
        this.depositoryParty = depositoryParty;
    }

    @Override
    public ProgressTracker getProgressTracker() {
        return progressTracker;
    }

    /**
     * The flow logic is encapsulated within the call() method.
     */
    @Suspendable
    @Override
    public SignedTransaction call() throws FlowException {
        // Obtain a reference to the notary we want to use.
        final Party notary = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0);

        // Stage 1.
        progressTracker.setCurrentStep(GENERATING_TRANSACTION);
        // Generate an unsigned transaction.
        Party seller = getServiceHub().getMyInfo().getLegalIdentities().get(0);
        SecuritySellState securitySellState = new SecuritySellState(quantity, securityName, "SELL_START", seller, depositoryParty);

        final Command<SellContract.Commands.Create> txCommand = new Command<>(new SellContract.Commands.Create(),
                securitySellState.getParticipants().stream().map(AbstractParty::getOwningKey).collect(Collectors.toList()));

        final TransactionBuilder txBuilder = new TransactionBuilder(notary).withItems(new StateAndContract(securitySellState, SELL_SECURITY_CONTRACT_ID), txCommand);

        // Stage 2.
        progressTracker.setCurrentStep(VERIFYING_TRANSACTION);

        // Verify that the transaction is valid.
        txBuilder.verify(getServiceHub());

        // Stage 3.
        progressTracker.setCurrentStep(SIGNING_TRANSACTION);
        // Sign the transaction.
        final SignedTransaction partSignedTx = getServiceHub().signInitialTransaction(txBuilder);
        logger.info("AP Sell flow initiate depo flow ");
        FlowSession depositorySession = initiateFlow(depositoryParty);
        logger.info("AP Sell flow initiated depo flow ");

        // Stage 4.
        progressTracker.setCurrentStep(GATHERING_SIGS);

        // Send the state to the counterparty, and receive it back with their signature.
        final SignedTransaction fullySignedTx = subFlow(
                new CollectSignaturesFlow(partSignedTx, Sets.newHashSet(depositorySession), CollectSignaturesFlow.Companion.tracker()));

        // Stage 5.
        progressTracker.setCurrentStep(FINALISING_TRANSACTION);


        // Notarise and record the transaction in both parties' vaults.
        subFlow(new FinalityFlow(fullySignedTx));

        return subFlow(new FinalityFlow(fullySignedTx));
    }
}


