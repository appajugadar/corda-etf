package com.cts.corda.etf.flow;

import com.cts.corda.etf.flow.depository.DepositoryBuyFlow;
import net.corda.node.internal.StartedNode;
import net.corda.testing.node.MockNetwork;
import net.corda.testing.node.MockNetwork.BasketOfNodes;
import net.corda.testing.node.MockNetwork.MockNode;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.ExpectedException;

import static net.corda.testing.CoreTestUtils.setCordappPackages;
import static net.corda.testing.CoreTestUtils.unsetCordappPackages;

public class IOUFlowTests {
    @Rule
    public final ExpectedException exception = ExpectedException.none();
    private MockNetwork network;
    private StartedNode<MockNode> a;
    private StartedNode<MockNode> b;

    @Before
    public void setup() {
        setCordappPackages("com.example.contract");
        network = new MockNetwork();
        BasketOfNodes nodes = network.createSomeNodes(2);
        a = nodes.getPartyNodes().get(0);
        b = nodes.getPartyNodes().get(1);
        // For real nodes this happens automatically, but we have to manually register the flow for tests.
        for (StartedNode<MockNode> node : nodes.getPartyNodes()) {
            node.registerInitiatedFlow(DepositoryBuyFlow.class);
        }
        network.runNetwork();
    }

    @After
    public void tearDown() {
        unsetCordappPackages();
        network.stopNodes();
    }
/*
    @Test
    public void flowRejectsInvalidIOUs() throws Exception {
        // The BuyContract specifies that IOUs cannot have negative values.
        APBuyFlow flow = new APBuyFlow(-1, "GLD",b.getInfo().getLegalIdentities().get(0));
        CordaFuture<SignedTransaction> future = a.getServices().startFlow(flow).getResultFuture();
        network.runNetwork();

        // The BuyContract specifies that IOUs cannot have negative values.
        exception.expectCause(instanceOf(TransactionVerificationException.class));
        future.get();
    }

    @Test
    public void signedTransactionReturnedByTheFlowIsSignedByTheInitiator() throws Exception {
        APBuyFlow flow = new APBuyFlow(1,"GLD", b.getInfo().getLegalIdentities().get(0));
        CordaFuture<SignedTransaction> future = a.getServices().startFlow(flow).getResultFuture();
        network.runNetwork();

        SignedTransaction signedTx = future.get();
        signedTx.verifySignaturesExcept(b.getInfo().getLegalIdentities().get(0).getOwningKey());
    }

    @Test
    public void signedTransactionReturnedByTheFlowIsSignedByTheAcceptor() throws Exception {
        APBuyFlow flow = new APBuyFlow(1, "GLD",b.getInfo().getLegalIdentities().get(0));
        CordaFuture<SignedTransaction> future = a.getServices().startFlow(flow).getResultFuture();
        network.runNetwork();

        SignedTransaction signedTx = future.get();
        signedTx.verifySignaturesExcept(a.getInfo().getLegalIdentities().get(0).getOwningKey());
    }

    @Test
    public void flowRecordsATransactionInBothPartiesTransactionStorages() throws Exception {
        APBuyFlow flow = new APBuyFlow(1, "GLD",b.getInfo().getLegalIdentities().get(0));
        CordaFuture<SignedTransaction> future = a.getServices().startFlow(flow).getResultFuture();
        network.runNetwork();
        SignedTransaction signedTx = future.get();

        // We check the recorded transaction in both vaults.
        for (StartedNode<MockNode> node : ImmutableList.of(a, b)) {
            assertEquals(signedTx, node.getServices().getValidatedTransactions().getTransaction(signedTx.getId()));
        }
    }

    @Test
    public void recordedTransactionHasNoInputsAndASingleOutputTheInputIOU() throws Exception {
        Integer iouValue = 1;
        APBuyFlow flow = new APBuyFlow(iouValue, "GLD",b.getInfo().getLegalIdentities().get(0));
        CordaFuture<SignedTransaction> future = a.getServices().startFlow(flow).getResultFuture();
        network.runNetwork();
        SignedTransaction signedTx = future.get();

        // We check the recorded transaction in both vaults.
        for (StartedNode<MockNode> node : ImmutableList.of(a, b)) {
            SignedTransaction recordedTx = node.getServices().getValidatedTransactions().getTransaction(signedTx.getId());
            List<TransactionState<ContractState>> txOutputs = recordedTx.getTx().getOutputs();
            assert (txOutputs.size() == 1);

            SecurityBuyState recordedState = (SecurityBuyState) txOutputs.get(0).getData();
            assertEquals(recordedState.getQuantity(), iouValue);
            assertEquals(recordedState.getSeller(), a.getInfo().getLegalIdentities().get(0));
            assertEquals(recordedState.getSeller(), b.getInfo().getLegalIdentities().get(0));
        }
    }

    @Test
    public void flowRecordsTheCorrectIOUInBothPartiesVaults() throws Exception {
        Integer iouValue = 1;
        APBuyFlow flow = new APBuyFlow(1, "GLD",b.getInfo().getLegalIdentities().get(0));
        CordaFuture<SignedTransaction> future = a.getServices().startFlow(flow).getResultFuture();
        network.runNetwork();
        future.get();

        // We check the recorded IOU in both vaults.
        for (StartedNode<MockNode> node : ImmutableList.of(a, b)) {
            node.getDatabase().transaction(it -> {
                    List<StateAndRef<SecurityBuyState>> ious = node.getServices().getVaultService().queryBy(SecurityBuyState.class).getStates();
                    assertEquals(1, ious.size());
                    SecurityBuyState recordedState = ious.get(0).getState().getData();
                    assertEquals(recordedState.getQuantity(), iouValue);
                    assertEquals(recordedState.getSeller(), a.getInfo().getLegalIdentities().get(0));
                    assertEquals(recordedState.getSeller(), b.getInfo().getLegalIdentities().get(0));
                    return null;
            });
        }
    }*/
}