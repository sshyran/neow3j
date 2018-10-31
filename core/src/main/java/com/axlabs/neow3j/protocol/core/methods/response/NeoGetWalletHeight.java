package com.axlabs.neow3j.protocol.core.methods.response;

import com.axlabs.neow3j.protocol.core.Response;

import java.math.BigInteger;

public class NeoGetWalletHeight extends Response<BigInteger> {

    public BigInteger getHeight() {
        return getResult();
    }

}
