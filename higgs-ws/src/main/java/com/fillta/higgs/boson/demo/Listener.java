package com.fillta.higgs.boson.demo;

import com.fillta.higgs.method;

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
public class Listener {
	@method(optout = true)
	public void ignoredMethod() {
	}

	@method("name")
	public int name(int a) {
		System.out.println(a);
		return a;
	}

	@method("age")
	public int age(int a) {
		System.out.println(a);
		return a;
	}

	@method("polo")
	public PoloExample polo(PoloExample a) {
		System.out.println(a);
		return a;
	}
}
