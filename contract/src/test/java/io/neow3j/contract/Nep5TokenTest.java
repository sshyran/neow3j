package io.neow3j.contract;

import static io.neow3j.contract.ContractTestHelper.setUpWireMockForInvokeFunction;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import io.neow3j.crypto.ECKeyPair;
import io.neow3j.protocol.Neow3j;
import io.neow3j.protocol.http.HttpService;
import io.neow3j.transaction.Cosigner;
import io.neow3j.transaction.Transaction;
import io.neow3j.transaction.WitnessScope;
import io.neow3j.utils.Numeric;
import io.neow3j.wallet.Account;
import io.neow3j.wallet.Wallet;
import io.neow3j.wallet.exceptions.InsufficientFundsException;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class Nep5TokenTest {

    @Rule
    public WireMockRule wireMockRule = new WireMockRule();

    private Neow3j neow;

    @Before
    public void setUp() {
        // Configuring WireMock to use default host and port "localhost:8080".
        WireMock.configure();
        neow = Neow3j.build(new HttpService("http://localhost:8080"));
    }

    @Test
    public void transferGas() throws Exception {
        ContractTestHelper.setUpWireMockForSendRawTransaction();
        String script =
                "0200e1f5050c14c8172ea3b405bf8bfc57c33a8410116b843e13df0c14941343239213fa0e765f1027ce742f48db779a9613c00c087472616e736665720c143b7d3711c6f0ccf9b1dca903d1bfa1d896f1238c41627d5b5238";
        ContractTestHelper.setUpWireMockForCall("invokescript", "invokescript_transfer_1_gas.json",
                script, "969a77db482f74ce27105f760efa139223431394");
        // Required for fetching the token's decimals.
        setUpWireMockForInvokeFunction(
                "decimals", "invokefunction_decimals_gas.json");
        // Required for fetching the block height used for setting the validUntilBlock.
        ContractTestHelper.setUpWireMockForGetBlockCount(1000);
        // Required when checking the senders token balance.
        setUpWireMockForInvokeFunction("balanceOf",
                "invokefunction_balanceOf.json");

        Nep5Token gas = new Nep5Token(
                new ScriptHash("0x8c23f196d8a1bfd103a9dcb1f9ccf0c611377d3b"), this.neow);
        byte[] privateKey = Numeric.hexStringToByteArray(
                "e6e919577dd7b8e97805151c05ae07ff4f752654d6d8797597aca989c02c4cb3");
        Account a = Account.fromECKeyPair(ECKeyPair.create(privateKey))
                .isDefault().build();
        Wallet w = new Wallet.Builder().accounts(a).build();
        ScriptHash receiver = new ScriptHash("df133e846b1110843ac357fc8bbf05b4a32e17c8");
        Invocation i = gas.buildTransferInvocation(w, receiver, BigDecimal.ONE);

        Transaction tx = i.getTransaction();
        assertThat(tx.getNetworkFee(), is(1268390L));
        assertThat(tx.getSystemFee(), is(9007810L));
        assertThat(tx.getSender(), is(w.getDefaultAccount().getScriptHash()));
        assertThat(tx.getAttributes(), hasSize(1));
        assertThat(tx.getCosigners(), hasSize(1));
        Cosigner c = tx.getCosigners().get(0);
        assertThat(c.getScriptHash(), is(w.getDefaultAccount().getScriptHash()));
        assertThat(c.getScopes().get(0), is(WitnessScope.CALLED_BY_ENTRY));
        assertThat(tx.getWitnesses(), hasSize(1));
        assertThat(tx.getScript(), is(Numeric.hexStringToByteArray(script)));
        assertThat(tx.getWitnesses().get(0).getVerificationScript(),
                is(w.getDefaultAccount().getVerificationScript()));
    }

    @Test
    public void getName() throws IOException {
        setUpWireMockForInvokeFunction("name", "invokefunction_name.json");
        Nep5Token nep5 = new Nep5Token(NeoToken.SCRIPT_HASH, this.neow);
        assertThat(nep5.getName(), is("NEO"));
    }

    @Test
    public void getSymbol() throws IOException {
        setUpWireMockForInvokeFunction("symbol", "invokefunction_symbol.json");
        ScriptHash neo = new ScriptHash("0x9bde8f209c88dd0e7ca3bf0af0f476cdd8207789");
        Nep5Token nep5 = new Nep5Token(neo, this.neow);
        assertThat(nep5.getSymbol(), is("neo"));
    }

    @Test
    public void getDecimals() throws Exception {
        setUpWireMockForInvokeFunction("decimals", "invokefunction_decimals_gas.json");
        ScriptHash gas = new ScriptHash("0x8c23f196d8a1bfd103a9dcb1f9ccf0c611377d3b");
        Nep5Token nep5 = new Nep5Token(gas, this.neow);
        assertThat(nep5.getDecimals(), is(8));
    }

    @Test
    public void getTotalSupply() throws Exception {
        setUpWireMockForInvokeFunction("totalSupply", "invokefunction_totalSupply.json");
        ScriptHash gas = new ScriptHash("0x8c23f196d8a1bfd103a9dcb1f9ccf0c611377d3b");
        Nep5Token nep5 = new Nep5Token(gas, this.neow);
        assertThat(nep5.getTotalSupply(), is(new BigInteger("3000000000000000")));
    }

    @Test
    public void getBalanceOfAccount() throws Exception {
        ScriptHash acc = ScriptHash.fromAddress("AMRZWegpH58nwY3iSDbmbBGg3kfGH6RgRt");
        setUpWireMockForInvokeFunction("balanceOf", "invokefunction_balanceOf.json");
        ScriptHash gas = new ScriptHash("0x8c23f196d8a1bfd103a9dcb1f9ccf0c611377d3b");
        Nep5Token nep5 = new Nep5Token(gas, this.neow);
        assertThat(nep5.getBalanceOf(acc), is(new BigInteger("3000000000000000")));
    }

    @Test
    public void getBalanceOfWallet() throws Exception {
        Account a1 = Account.fromAddress("AVGpjFiocR1BdYhbYWqB6Ls6kcmzx4FWhm").isDefault().build();
        Account a2 = Account.fromAddress("Aa1rZbE1k8fXTwzaxxsPRtJYPwhDQjWRFZ").build();
        ContractTestHelper.setUpWireMockForBalanceOf(a1.getScriptHash(),
                "invokefunction_balanceOf_AVGpjFiocR1BdYhbYWqB6Ls6kcmzx4FWhm.json");
        ContractTestHelper.setUpWireMockForBalanceOf(a2.getScriptHash(),
                "invokefunction_balanceOf_Aa1rZbE1k8fXTwzaxxsPRtJYPwhDQjWRFZ.json");
        Wallet w = new Wallet.Builder().accounts(a1, a2).build();
        Nep5Token token = new Nep5Token(GasToken.SCRIPT_HASH, this.neow);
        assertThat(token.getBalanceOf(w), is(new BigInteger("411285799730")));
    }

    @Test(expected = InsufficientFundsException.class)
    public void failTransferringGasBecauseOfInsufficientBalance() throws Exception {
        ContractTestHelper.setUpWireMockForSendRawTransaction();
        // Required for fetching of system fee of the invocation.
        setUpWireMockForInvokeFunction(
                "transfer", "invokescript_transfer_1_gas.json");
        // Required for fetching the token's decimals.
        setUpWireMockForInvokeFunction(
                "decimals", "invokefunction_decimals_gas.json");
        // Required for fetching the block height used for setting the validUntilBlock.
        ContractTestHelper.setUpWireMockForGetBlockCount(1000);
        // Required for checking the senders token balance.
        ContractTestHelper.setUpWireMockForCall("invokefunction",
                "invokefunction_balanceOf_Aa1rZbE1k8fXTwzaxxsPRtJYPwhDQjWRFZ.json",
                "8c23f196d8a1bfd103a9dcb1f9ccf0c611377d3b",
                "balanceOf",
                "df133e846b1110843ac357fc8bbf05b4a32e17c8");

        Nep5Token gas = new Nep5Token(GasToken.SCRIPT_HASH, this.neow);
        byte[] privateKey = Numeric.hexStringToByteArray(
                "b4b2b579cac270125259f08a5f414e9235817e7637b9a66cfeb3b77d90c8e7f9");
        Account a = Account.fromECKeyPair(ECKeyPair.create(privateKey))
                .isDefault().build();
        Wallet w = new Wallet.Builder().accounts(a).build();
        ScriptHash receiver = new ScriptHash("df133e846b1110843ac357fc8bbf05b4a32e17c8");
        Invocation i = gas.buildTransferInvocation(w, receiver, new BigDecimal("4"));
    }
}
