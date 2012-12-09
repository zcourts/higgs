package com.fillta.higgs.util;

import com.google.common.base.Optional;

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
public interface Function2<A, B> {
    public void call(A a, B b);
}
