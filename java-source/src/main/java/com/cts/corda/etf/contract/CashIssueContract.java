package com.cts.corda.etf.contract;

import lombok.extern.slf4j.Slf4j;
import net.corda.core.contracts.CommandData;
import net.corda.core.contracts.Contract;
import net.corda.core.contracts.TypeOnlyCommandData;
import net.corda.core.transactions.LedgerTransaction;

@Slf4j
public class CashIssueContract implements Contract {

    public static final String SELF_ISSUE_CASH_CONTRACT_ID = "com.cts.bfs.etf.corda.contract.CashIssueContract";

    @Override
    public void verify(LedgerTransaction tx) {
        log.info("Inside contract. inputs " + tx.getInputs());
        log.info("Inside contract. outputs " + tx.getOutputs());
    }

    public interface Commands extends CommandData {

        class SelfIssueCash extends TypeOnlyCommandData implements CashIssueContract.Commands {

        }


    }


}