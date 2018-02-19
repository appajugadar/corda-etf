package com.cts.corda.etf.flow.buy;

import co.paralleluniverse.fibers.Suspendable;
import com.cts.corda.etf.contract.BuyContract;
import com.cts.corda.etf.state.SecurityBuyState;
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

import static com.cts.corda.etf.contract.BuyContract.BUY_SECURITY_CONTRACT_ID;


@InitiatingFlow
@StartableByRPC
public class APBuyFlow extends FlowLogic<SignedTransaction> {

    private final int iouValue;
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

    public APBuyFlow(int iouValue, String securityName, Party depositoryParty) {
        this.iouValue = iouValue;
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
        Party buyer = getServiceHub().getMyInfo().getLegalIdentities().get(0);

        SecurityBuyState securityBuyState = new SecurityBuyState(iouValue, securityName, "BUY_START", buyer, depositoryParty);
        final Command<BuyContract.Commands.Create> txCommand = new Command<>(new BuyContract.Commands.Create(),
                securityBuyState.getParticipants().stream().map(AbstractParty::getOwningKey).collect(Collectors.toList()));
        final TransactionBuilder txBuilder = new TransactionBuilder(notary).withItems(new StateAndContract(securityBuyState,
                BUY_SECURITY_CONTRACT_ID), txCommand);

        // Stage 2.
        progressTracker.setCurrentStep(VERIFYING_TRANSACTION);

        // Verify that the transaction is valid.
        txBuilder.verify(getServiceHub());

        // Stage 3.        // Sign the transaction.
        progressTracker.setCurrentStep(SIGNING_TRANSACTION);
        final SignedTransaction partSignedTx = getServiceHub().signInitialTransaction(txBuilder);

        // Stage 4.
        progressTracker.setCurrentStep(GATHERING_SIGS);
        FlowSession depositorySession = initiateFlow(depositoryParty);

        // Send the state to the counterparty, and receive it back with their signature.
        final SignedTransaction fullySignedTx = subFlow(new CollectSignaturesFlow(partSignedTx,
                Sets.newHashSet(depositorySession), CollectSignaturesFlow.Companion.tracker()));
        // Stage 5.
        progressTracker.setCurrentStep(FINALISING_TRANSACTION);
        return subFlow(new FinalityFlow(fullySignedTx));
    }

    @InitiatingFlow
    public class APBuySubFlow extends FlowLogic<String> {

        private final Logger logger = LoggerFactory.getLogger(APBuySubFlow.class);
        private final FlowSession flowSession;
        private final SecurityBuyState securityBuyState;

        public APBuySubFlow(FlowSession flowSession, SecurityBuyState securityBuyState) {
            this.flowSession = flowSession;
            this.securityBuyState = securityBuyState;
            System.out.println("Inside APBuySubFlow called by " + flowSession.getCounterparty());
        }

        @Suspendable
        @Override
        public String call() throws FlowException {
            logger.info("APBuySubFlow inside call method ");
            final Party notary = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0);
            Party buyer = getServiceHub().getMyInfo().getLegalIdentities().get(0);

            // SecurityBuyState securityBuyState = new SecurityBuyState(iouValue, securityName, "BUY_MATCH", buyer, depositoryParty);
            securityBuyState.setStatus("BUY_MATCH");

            final Command<BuyContract.Commands.Create> txCommand = new Command<>(new BuyContract.Commands.Create(),
                    securityBuyState.getParticipants().stream().map(AbstractParty::getOwningKey).collect(Collectors.toList()));
            final TransactionBuilder txBuilder = new TransactionBuilder(notary).withItems(new StateAndContract(securityBuyState,
                    BUY_SECURITY_CONTRACT_ID), txCommand);

// Verify that the transaction is valid.
            txBuilder.verify(getServiceHub());

            final SignedTransaction partSignedTx = getServiceHub().signInitialTransaction(txBuilder);

            FlowSession depositorySession = initiateFlow(depositoryParty);

            FlowSession fs = initiateFlow(flowSession.getCounterparty());
            final SignedTransaction fullySignedTx = subFlow(new CollectSignaturesFlow(partSignedTx, Sets.newHashSet(depositorySession), CollectSignaturesFlow.Companion.tracker()));
            subFlow(new FinalityFlow(fullySignedTx));

            return "Success";
        }
    }
}


