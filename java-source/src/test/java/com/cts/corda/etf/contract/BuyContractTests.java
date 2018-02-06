package com.cts.corda.etf.contract;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import net.corda.core.identity.Party;
import org.junit.After;
import org.junit.Before;

import java.security.PublicKey;

import static net.corda.testing.CoreTestUtils.*;

public class BuyContractTests {
    static private final Party miniCorp = getMINI_CORP();
    static private final Party megaCorp = getMEGA_CORP();
    static private final PublicKey[] keys = Iterables.toArray(ImmutableList.of(getMEGA_CORP_PUBKEY(), getMINI_CORP_PUBKEY()), PublicKey.class);

    @Before
    public void setup() {
        setCordappPackages("com.example.contract");
    }

    @After
    public void tearDown() {
        unsetCordappPackages();
    }
/*
    @Test
    public void transactionMustIncludeCreateCommand() {
        Integer iou = 1;
        ledger(ledgerDSL -> {
            ledgerDSL.transaction(txDSL -> {
                txDSL.output(BUY_SECURITY_CONTRACT_ID, () -> new SecurityBuyState(iou, "GLD",miniCorp, megaCorp));
                txDSL.fails();
                txDSL.command(keys, BuyContract.Commands.Create::new);
                txDSL.verifies();
                return null;
            });
            return null;
        });
    }

    @Test
    public void transactionMustHaveNoInputs() {
        Integer iou = 1;
        ledger(ledgerDSL -> {
            ledgerDSL.transaction(txDSL -> {
                txDSL.input(BUY_SECURITY_CONTRACT_ID, new SecurityBuyState(iou, "GLD",miniCorp, megaCorp));
                txDSL.output(BUY_SECURITY_CONTRACT_ID, () -> new SecurityBuyState(iou, "GLD",miniCorp, megaCorp));
                txDSL.command(keys, BuyContract.Commands.Create::new);
                txDSL.failsWith("No inputs should be consumed when issuing an IOU.");
                return null;
            });
            return null;
        });
    }

    @Test
    public void transactionMustHaveOneOutput() {
        Integer iou = 1;
        ledger(ledgerDSL -> {
            ledgerDSL.transaction(txDSL -> {
                txDSL.output(BUY_SECURITY_CONTRACT_ID, () -> new SecurityBuyState(iou, "GLD",miniCorp, megaCorp));
                txDSL.output(BUY_SECURITY_CONTRACT_ID, () -> new SecurityBuyState(iou, "GLD",miniCorp, megaCorp));
                txDSL.command(keys, BuyContract.Commands.Create::new);
                txDSL.failsWith("Only one output state should be created.");
                return null;
            });
            return null;
        });
    }

    @Test
    public void lenderMustSignTransaction() {
        Integer iou = 1;
        ledger(ledgerDSL -> {
            ledgerDSL.transaction(txDSL -> {
                txDSL.output(BUY_SECURITY_CONTRACT_ID, () -> new SecurityBuyState(iou, "GLD",miniCorp, megaCorp));
                PublicKey[] keys = new PublicKey[1];
                keys[0] = getMINI_CORP_PUBKEY();
                txDSL.command(keys, BuyContract.Commands.Create::new);
                txDSL.failsWith("All of the participants must be signers.");
                return null;
            });
            return null;
        });
    }

    @Test
    public void borrowerMustSignTransaction() {
        Integer iou = 1;
        ledger(ledgerDSL -> {
            ledgerDSL.transaction(txDSL -> {
                txDSL.output(BUY_SECURITY_CONTRACT_ID, () -> new SecurityBuyState(iou, "GLD",miniCorp, megaCorp));
                PublicKey[] keys = new PublicKey[1];
                keys[0] = getMEGA_CORP_PUBKEY();
                txDSL.command(keys, BuyContract.Commands.Create::new);
                txDSL.failsWith("All of the participants must be signers.");
                return null;
            });
            return null;
        });
    }

    @Test
    public void lenderIsNotBorrower() {
        Integer iou = 1;
        ledger(ledgerDSL -> {
            ledgerDSL.transaction(txDSL -> {
                txDSL.output(BUY_SECURITY_CONTRACT_ID, () -> new SecurityBuyState(iou, "GLD",megaCorp, megaCorp));
                PublicKey[] keys = new PublicKey[1];
                keys[0] = getMEGA_CORP_PUBKEY();
                txDSL.command(keys, BuyContract.Commands.Create::new);
                txDSL.failsWith("The lender and the borrower cannot be the same entity.");
                return null;
            });
            return null;
        });
    }

    @Test
    public void cannotCreateNegativeValueIOUs() {
        Integer iou = -1;
        ledger(ledgerDSL -> {
            ledgerDSL.transaction(txDSL -> {
                txDSL.output(BUY_SECURITY_CONTRACT_ID, () -> new SecurityBuyState(iou, "GLD",miniCorp, megaCorp));
                txDSL.command(keys, BuyContract.Commands.Create::new);
                txDSL.failsWith("The IOU's value must be non-negative.");
                return null;
            });
            return null;
        });
    }*/
}