package com.cts.corda.etf.flow;

import co.paralleluniverse.fibers.Suspendable;
import com.cts.corda.etf.state.CommercialPaper;
import com.google.common.collect.Sets;
import lombok.extern.slf4j.Slf4j;
import net.corda.core.contracts.Command;
import net.corda.core.contracts.StateAndContract;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.flows.*;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.Party;
import net.corda.core.node.services.Vault;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.core.utilities.ProgressTracker;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static com.cts.corda.etf.state.CommercialPaper.JCP_PROGRAM_ID;


@InitiatingFlow
@StartableByRPC
@Slf4j
public class CommercialPaperMoveFlow extends FlowLogic<SignedTransaction> {

    private final Party receiverParty;

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

    private final ProgressTracker progressTracker = new ProgressTracker(GENERATING_TRANSACTION, VERIFYING_TRANSACTION, SIGNING_TRANSACTION, GATHERING_SIGS, FINALISING_TRANSACTION);

    public CommercialPaperMoveFlow(Party receiverParty) {
        this.receiverParty = receiverParty;
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

        Vault.Page<CommercialPaper.State> results = getServiceHub().getVaultService().queryBy(CommercialPaper.State.class);
        CommercialPaper.State cpState = null;
        StateAndRef<CommercialPaper.State> stateref1 = null;
        for (StateAndRef<CommercialPaper.State> stateref : results.getStates()) {
            stateref1 = stateref;
            cpState = stateref.getState().getData();
        }

        log.info("Commercialpapermove flow 1");
        List<AbstractParty> ps = new ArrayList();
        ps.add(cpState.getOwner());
        ps.add(receiverParty);

        /*final Command<CommercialPaper.Commands.Move> txCommand = new Command<>(new CommercialPaper.Commands.Move(),
                cpState.getParticipants().stream().map(AbstractParty::getOwningKey).collect(Collectors.toList()));*/
        final Command<CommercialPaper.Commands.Move> txCommand = new Command<>(new CommercialPaper.Commands.Move(),
                ps.stream().map(AbstractParty::getOwningKey).collect(Collectors.toList()));

        log.info("Commercialpapermove flow 2");
        final TransactionBuilder txBuilder = new TransactionBuilder(notary).withItems(new StateAndContract(cpState, JCP_PROGRAM_ID), stateref1, txCommand);
        log.info("Commercialpapermove flow 3");
        // Stage 2.
        progressTracker.setCurrentStep(VERIFYING_TRANSACTION);

        // Verify that the transaction is valid.
        txBuilder.verify(getServiceHub());
        log.info("Commercialpapermove flow 4");
        // Stage 3.        // Sign the transaction.
        progressTracker.setCurrentStep(SIGNING_TRANSACTION);

        final SignedTransaction partSignedTx = getServiceHub().signInitialTransaction(txBuilder);
        log.info("Commercialpapermove flow 5");
        // Stage 4.
        progressTracker.setCurrentStep(GATHERING_SIGS);
        log.info("Commercialpapermove flow receiverParty() " + receiverParty);
        FlowSession receiverSession = initiateFlow(receiverParty);

        log.info("Commercialpapermove flow initiated receiver flow receiverSession==null > " + (receiverSession == null));

        log.info("Commercialpapermove flow initiated receiver FLowInfo " + receiverSession.getCounterparty());
        // Send the state to the counterparty, and receive it back with their signature.
        final SignedTransaction fullySignedTx = subFlow(new CollectSignaturesFlow(partSignedTx,
                Sets.newHashSet(receiverSession), CollectSignaturesFlow.Companion.tracker()));
        // Stage 5.
        log.info("Commercialpapermove flow got receiver sign");
        progressTracker.setCurrentStep(FINALISING_TRANSACTION);
        return subFlow(new FinalityFlow(fullySignedTx));
    }
}


