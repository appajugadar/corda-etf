package com.cts.corda.etf.flow;

import co.paralleluniverse.fibers.Suspendable;
import com.cts.corda.etf.contract.SecurityStock;
import com.cts.corda.etf.state.CommercialPaper;
import net.corda.core.contracts.*;
import net.corda.core.flows.*;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.core.utilities.OpaqueBytes;
import net.corda.core.utilities.ProgressTracker;

import java.time.Instant;
import java.util.Currency;
import java.util.stream.Collectors;

import static com.cts.corda.etf.state.CommercialPaper.JCP_PROGRAM_ID;

@StartableByRPC
@InitiatingFlow
public class CommercialPaperIssueFlow extends FlowLogic<SignedTransaction> {

    private final ProgressTracker.Step INITIALISING = new ProgressTracker.Step("Performing initial steps.");
    private final ProgressTracker.Step VERIFYING_TRANSACTION = new ProgressTracker.Step("Verifying contract constraints.");
    private final ProgressTracker.Step BUILDING = new ProgressTracker.Step("Performing initial steps.");
    private final ProgressTracker.Step SIGNING = new ProgressTracker.Step("Signing transaction.");
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
    private final ProgressTracker progressTracker = new ProgressTracker(
            INITIALISING, VERIFYING_TRANSACTION, BUILDING, SIGNING, GATHERING_SIGS, FINALISING_TRANSACTION
    );

    private Amount<Currency> faceValue;
    private Instant maturityDate;

    public CommercialPaperIssueFlow(Amount<Currency> faceValue, Instant maturityDate) {
        super();
        this.faceValue = faceValue;
        this.maturityDate = maturityDate;
    }

    @Override
    public ProgressTracker getProgressTracker() {
        return progressTracker;
    }

    @Suspendable
    @Override
    public SignedTransaction call() throws FlowException {

        System.out.print("Called CommercialPaperIssueFlow for faceValue " + faceValue + " maturityDate " + maturityDate);

        PartyAndReference issuer = this.getOurIdentity().ref(OpaqueBytes.of((faceValue.toString() + maturityDate.toString()).getBytes()));
        Amount<Issued<Currency>>  amount = new Amount(faceValue.getQuantity(), new Issued<>(issuer, faceValue.getToken()));
        CommercialPaper.State cpState = new CommercialPaper.State(issuer, getOurIdentity(), amount, maturityDate);

        System.out.print("etfTradeState -->> " + cpState);

        final Command<CommercialPaper.Commands.Issue> txCommand = new Command<>(new CommercialPaper.Commands.Issue(), cpState.getParticipants().stream().map(AbstractParty::getOwningKey).collect(Collectors.toList()));

        System.out.println("Inside EtfIssue flow BUILDING tx");
        // Step 2. build tx.
        progressTracker.setCurrentStep(BUILDING);
        final Party notary = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0);
        final TransactionBuilder txBuilder = new TransactionBuilder(notary).withItems(new StateAndContract(cpState, JCP_PROGRAM_ID), txCommand);

        System.out.println("Inside EtfIssue flow verify tx");
        // Stage 3. verify tx
        progressTracker.setCurrentStep(VERIFYING_TRANSACTION);

        // Verify that the transaction is valid.
        getLogger().info("Before verify TX");
        txBuilder.verify(getServiceHub());


        // step 4 Sign the transaction.
        progressTracker.setCurrentStep(SIGNING);
        final SignedTransaction partSignedTx = getServiceHub().signInitialTransaction(txBuilder);

        System.out.println("Inside EtfIssue flow finalize tx");

        // Stage 6. finalise tx;
        progressTracker.setCurrentStep(FINALISING_TRANSACTION);

        SignedTransaction notarisedTx = subFlow(new FinalityFlow(partSignedTx));
        return notarisedTx;


    }

    @InitiatedBy(CommercialPaperIssueFlow.class)
    public static class Acceptor extends FlowLogic<SignedTransaction> {

        private final FlowSession otherPartyFlow;

        public Acceptor(FlowSession otherPartyFlow) {
            this.otherPartyFlow = otherPartyFlow;
        }

        @Suspendable
        @Override
        public SignedTransaction call() throws FlowException {
            class SignTxFlow extends SignTransactionFlow {
                private SignTxFlow(FlowSession otherPartyFlow, ProgressTracker progressTracker) {
                    super(otherPartyFlow, progressTracker);
                }

                @Override
                protected void checkTransaction(SignedTransaction stx) {
                    System.out.print("Inside check transaction for self issue etf");
                }
            }

            return subFlow(new SignTxFlow(otherPartyFlow, SignTransactionFlow.Companion.tracker()));
        }
    }

}
