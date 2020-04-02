package io.neow3j.protocol.core.methods.response;

import io.neow3j.protocol.core.Response;

import java.math.BigInteger;

public class NeoGetTransactionHeight extends Response<BigInteger> {

    public BigInteger getHeight() {
        return getResult();
    }

}
