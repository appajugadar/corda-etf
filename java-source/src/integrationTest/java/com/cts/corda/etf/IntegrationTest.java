package com.cts.corda.etf;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import net.corda.core.concurrent.CordaFuture;
import net.corda.core.identity.CordaX500Name;
import net.corda.core.identity.Party;
import net.corda.node.services.transactions.SimpleNotaryService;
import net.corda.nodeapi.internal.ServiceInfo;
import net.corda.testing.driver.DriverParameters;
import net.corda.testing.driver.NodeHandle;
import net.corda.testing.driver.NodeParameters;
import net.corda.testing.driver.WebserverHandle;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.util.List;
import java.util.Set;

import static net.corda.core.crypto.CryptoUtils.entropyToKeyPair;
import static net.corda.testing.TestConstants.getDUMMY_NOTARY;
import static net.corda.testing.driver.Driver.driver;

public class IntegrationTest {

    private static final Logger log = LoggerFactory.getLogger(IntegrationTest.class);

    @Before
    public void setUp() {
        //CoreTestUtils.setCordappPackages( "net.corda.core.contracts","net.corda.finance.contracts");
        //CoreTestUtils.setCordappPackages( "net.corda.finance.contracts.asset.Cash");
        /*driver(true,ImmutableList.of("net.corda.core.contracts","net.corda.finance.contracts"),true){
        }*/
        //Driver.driver()
    }

    @Test
    public void runDriverTest() {
        Party notary = getDUMMY_NOTARY();

        Party ap1 = new Party(new CordaX500Name("AP1", "London", "GB"), entropyToKeyPair(BigInteger.valueOf(40)).getPublic());

        Party ap2 = new Party(new CordaX500Name("AP2", "London", "GB"), entropyToKeyPair(BigInteger.valueOf(50)).getPublic());

        Party depository = new Party(new CordaX500Name("DEPOSITORY", "London", "GB"), entropyToKeyPair(BigInteger.valueOf(70)).getPublic());

        Party regulator = new Party(new CordaX500Name("Regulator", "London", "GB"), entropyToKeyPair(BigInteger.valueOf(90)).getPublic());


        Set<ServiceInfo> notaryServices = ImmutableSet.of(new ServiceInfo(SimpleNotaryService.Companion.getType(), null));

        driver(new DriverParameters().setIsDebug(true).setStartNodesInProcess(true).setExtraCordappPackagesToScan(
                ImmutableList.of("com.example.contract", "net.corda.finance.contracts.asset", "net.corda.finance.contracts")),
                dsl -> {
                    // This starts three nodes simultaneously with startNode, which returns a future that completes when the node
                    // has completed startup. Then these are all resolved with getOrThrow which returns the NodeHandle list.
                    List<CordaFuture<NodeHandle>> handles = ImmutableList.of(
                            dsl.startNode(new NodeParameters().setProvidedName(notary.getName()).setAdvertisedServices(notaryServices)),
                            dsl.startNode(new NodeParameters().setProvidedName(ap1.getName())),
                            dsl.startNode(new NodeParameters().setProvidedName(ap2.getName())),
                            dsl.startNode(new NodeParameters().setProvidedName(depository.getName())),
                            dsl.startNode(new NodeParameters().setProvidedName(regulator.getName())
                            )
                    );

                    try {
                        NodeHandle notaryHandle = handles.get(0).get();
                        NodeHandle ap1Handle = handles.get(1).get();
                        NodeHandle ap2Handle = handles.get(2).get();
                        NodeHandle depositoryHandle = handles.get(3).get();
                        NodeHandle regulatorHandle = handles.get(4).get();

                        // This test will call via the RPC proxy to find a party of another node to verify that the nodes have
                        // started and can communicate. This is a very basic test, in practice tests would be starting flows,
                        // and verifying the states in the vault and other important metrics to ensure that your CorDapp is working
                        // as intended.

                        CordaFuture<WebserverHandle> ap1Handle1 = dsl.startWebserver(ap1Handle);
                        CordaFuture<WebserverHandle> ap2Handle1 = dsl.startWebserver(ap2Handle);
                        CordaFuture<WebserverHandle> depositoryHandle1 = dsl.startWebserver(depositoryHandle);
                        CordaFuture<WebserverHandle> regulatorHandle1 = dsl.startWebserver(regulatorHandle);

                        log.info("ap1Handle address: " + ap1Handle1.get().getListenAddress());
                        log.info("ap2Handle address: " + ap2Handle1.get().getListenAddress());
                        log.info("depositoryHandle address: " + depositoryHandle1.get().getListenAddress());
                        log.info("regulatorHandle address: " + regulatorHandle1.get().getListenAddress());


                        dsl.waitForAllNodesToFinish();

                        Assert.assertEquals(notaryHandle.getRpc().wellKnownPartyFromX500Name(ap1.getName()).getName(), ap1.getName());
                        Assert.assertEquals(notaryHandle.getRpc().wellKnownPartyFromX500Name(ap2.getName()).getName(), ap2.getName());
                        Assert.assertEquals(notaryHandle.getRpc().wellKnownPartyFromX500Name(depository.getName()).getName(), depository.getName());
                        Assert.assertEquals(notaryHandle.getRpc().wellKnownPartyFromX500Name(regulator.getName()).getName(), regulator.getName());
                        Assert.assertEquals(notaryHandle.getRpc().wellKnownPartyFromX500Name(notary.getName()).getName(), notary.getName());
                    } catch (Exception e) {
                        throw new RuntimeException("Caught exception during test", e);
                    }

                    return null;
                });
    }
}