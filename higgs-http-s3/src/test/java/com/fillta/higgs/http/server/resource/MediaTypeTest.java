package com.fillta.higgs.http.server.resource;

import org.junit.Test;

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
public class MediaTypeTest {
	//TODO ...clearly
	//the spec makes parsing media types a bit tricky with the case of quoted parameter values, these test cases
	//should fail/detect if MediaType is unable to parse any of these, valid but annoying types
	//see http://tools.ietf.org/html/rfc2046#page-29
	//and http://tools.ietf.org/html/rfc2046#page-18 (WARNING TO IMPLEMENTERS) bit
	//Content-Type: Message/Partial; number=2; total=3; id="oc=jpbe0M2Yt4s@thumper.bellcore.com"

	@Test
	public void testValueOfCommaStart() throws Exception {
		String accept = "text/xhtml;q=\"a;bc,123/abc\",text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8";
		MediaType.valueOf(accept);
	}

	@Test
	public void testValueOfMultipleCommasEnd() throws Exception {
		String accept = "text/xhtml;q=\"a;bc,123/abc\",text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8;a=\"a,b,c,d,e\"";
		MediaType.valueOf(accept);
	}

	@Test
	public void testValueOfCommasMiddle() throws Exception {
		String accept = "text/xhtml;q=\"a;bc,123/abc\",text/html,*/*;q=0.8;a=\"a,b,c,d,e\",application/xhtml+xml,application/xml;q=0.9";
		MediaType.valueOf(accept);
	}

}
