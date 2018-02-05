package com.cts.corda.etf.flow;

import co.paralleluniverse.fibers.Suspendable;
import com.cts.corda.etf.contract.EtfStock;
import net.corda.core.contracts.Command;
import net.corda.core.contracts.PartyAndReference;
import net.corda.core.contracts.StateAndContract;
import net.corda.core.flows.*;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.core.utilities.OpaqueBytes;
import net.corda.core.utilities.ProgressTracker;

import java.util.stream.Collectors;


@StartableByRPC
public class EtfStockIssueFlow extends FlowLogic<SignedTransaction> {

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

    private Long guantity;
    private String etfName;

    public EtfStockIssueFlow(Long quantity, String etfName) {
        super();
        this.guantity = quantity;
        this.etfName = etfName;
    }

    @Override
    public ProgressTracker getProgressTracker() {
        return progressTracker;
    }

    @Suspendable
    @Override
    public SignedTransaction call() throws FlowException {

        System.out.print("Called SecurityIssueFlow for quantity " + guantity + " etfName " + etfName);
        getLogger().info("Called SecurityIssueFlow for quantity " + guantity + " etfName " + etfName);

        PartyAndReference issuer = this.getOurIdentity().ref(OpaqueBytes.of((etfName + guantity).getBytes()));

        EtfStock.State etfTradeState = new EtfStock.State(issuer, getOurIdentity(), etfName, guantity);

        System.out.print("etfTradeState -->> " + etfTradeState);

        final Command<EtfStock.Commands.Issue> txCommand = new Command<>(new EtfStock.Commands.Issue(), etfTradeState.getParticipants().stream().map(AbstractParty::getOwningKey).collect(Collectors.toList()));

        System.out.println("Inside EtfIssue flow BUILDING tx");
        // Step 2. build tx.
        progressTracker.setCurrentStep(BUILDING);
        final Party notary = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0);
        final TransactionBuilder txBuilder = new TransactionBuilder(notary)
                .withItems(new StateAndContract(etfTradeState, EtfStock.ETF_STOCK_CONTRACT), txCommand);

        System.out.println("Inside EtfIssue flow verify tx");
        // Stage 3. verify tx
        progressTracker.setCurrentStep(VERIFYING_TRANSACTION);

        // Verify that the transaction is valid.
        getLogger().info("Before verify TX");
        txBuilder.verify(getServiceHub());

        getLogger().info("Verified TX");


        // step 4 Sign the transaction.
        progressTracker.setCurrentStep(SIGNING);
        final SignedTransaction partSignedTx = getServiceHub().signInitialTransaction(txBuilder);
        getLogger().info("Signed TX");

        System.out.println("Inside EtfIssue flow finalize tx");

        // Stage 6. finalise tx;
        progressTracker.setCurrentStep(FINALISING_TRANSACTION);
        // Notarise and record the transaction in both parties' vaults.
        SignedTransaction notarisedTx = subFlow(new FinalityFlow(partSignedTx));
        getLogger().info("Notarised TX");
        return notarisedTx;
    }
}
