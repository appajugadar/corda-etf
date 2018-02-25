package com.cts.corda.etf.flow.commercialpaper;

import co.paralleluniverse.fibers.Suspendable;
import com.cts.corda.etf.state.CommercialPaper;
import com.google.common.collect.Sets;
import lombok.extern.slf4j.Slf4j;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.flows.*;
import net.corda.core.identity.Party;
import net.corda.core.node.services.Vault;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.core.utilities.ProgressTracker;


@InitiatingFlow
@StartableByRPC
@Slf4j
public class CommercialPaperMoveFlow extends FlowLogic<SignedTransaction> {

    private final Party receiverParty;

    private final ProgressTracker.Step GENERATING_TRANSACTION = new ProgressTracker.Step("Generating transaction.");
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

    private final ProgressTracker progressTracker = new ProgressTracker(GENERATING_TRANSACTION, VERIFYING_TRANSACTION, SIGNING_TRANSACTION, GATHERING_SIGS, FINALISING_TRANSACTION);

    public CommercialPaperMoveFlow(Party receiverParty) {
        this.receiverParty = receiverParty;
    }

    @Override
    public ProgressTracker getProgressTracker() {
        return progressTracker;
    }

    @Suspendable
    @Override
    public SignedTransaction call() throws FlowException {
        // Stage 1.
        progressTracker.setCurrentStep(GENERATING_TRANSACTION);
        // Generate an unsigned transaction.
        Vault.Page<CommercialPaper.State> results = getServiceHub().getVaultService().queryBy(CommercialPaper.State.class);
        StateAndRef<CommercialPaper.State> commercialPaperStateRef = null;
        if (results != null && results.getStates().size() > 0) {
            commercialPaperStateRef = results.getStates().get(0);
        }

        final TransactionBuilder txBuilder = new TransactionBuilder(getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0));
        CommercialPaper.generateMove(txBuilder, commercialPaperStateRef, receiverParty);
        progressTracker.setCurrentStep(VERIFYING_TRANSACTION);
        txBuilder.verify(getServiceHub());
        progressTracker.setCurrentStep(SIGNING_TRANSACTION);
        final SignedTransaction partSignedTx = getServiceHub().signInitialTransaction(txBuilder);
        progressTracker.setCurrentStep(GATHERING_SIGS);
        FlowSession receiverSession = initiateFlow(receiverParty);
        final SignedTransaction fullySignedTx = subFlow(new CollectSignaturesFlow(partSignedTx, Sets.newHashSet(receiverSession), CollectSignaturesFlow.Companion.tracker()));
        progressTracker.setCurrentStep(FINALISING_TRANSACTION);
        return subFlow(new FinalityFlow(fullySignedTx));
    }
}


