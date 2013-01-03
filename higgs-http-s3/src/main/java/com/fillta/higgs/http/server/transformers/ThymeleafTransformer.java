package com.fillta.higgs.http.server.transformers;

import com.fillta.higgs.events.ChannelMessage;
import com.fillta.higgs.http.server.HiggsEndpoint;
import com.fillta.higgs.http.server.HiggsHttpRequest;
import com.fillta.higgs.http.server.HiggsHttpResponse;
import com.fillta.higgs.http.server.HiggsResponseTransformer;
import com.fillta.higgs.http.server.config.HiggsTemplateConfig;
import com.fillta.higgs.http.server.params.HiggsFormFiles;
import com.fillta.higgs.http.server.params.HiggsFormParams;
import com.fillta.higgs.http.server.params.HiggsQueryParams;
import com.fillta.higgs.http.server.params.HiggsSession;
import com.fillta.higgs.http.server.resource.MediaType;
import com.fillta.higgs.http.server.transformers.thymeleaf.HiggsWebContext;
import com.fillta.higgs.http.server.transformers.thymeleaf.Thymeleaf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponseStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.thymeleaf.TemplateEngine;

import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;

/**
 * The following will be injected by default
 * <pre>
 * <table>
 *     <thead>
 *         <tr>
 *             <td><strong>Template Variable name</strong></td>
 *             <td><strong>Class/Type</strong></td>
 *         </tr>
 *     </thead>
 *         <tr>
 *             <td>${_query}</td>
 *             <td>{@link HiggsQueryParams}</td>
 *         </tr>
 *         <tr>
 *             <td>${_form}</td>
 *             <td>{@link HiggsFormParams}</td>
 *         </tr>
 *         <tr>
 *             <td>${_files}</td>
 *             <td>{@link HiggsFormFiles}</td>
 *         </tr>
 *         <tr>
 *             <td>${_session}</td>
 *             <td>{@link HiggsSession}</td>
 *         </tr>
 *         <tr>
 *             <td>${_cookies}</td>
 *             <td>{@link HashMap}</td>
 *         </tr>
 *         <tr>
 *             <td>${_request}</td>
 *             <td>{@link HiggsHttpRequest}</td>
 *         </tr>
 * </table>
 * </pre>
 *
 * @author Courtney Robinson <courtney@crlog.info>
 */
public class ThymeleafTransformer extends BaseTransformer {
	private Logger log = LoggerFactory.getLogger(getClass());
	protected HiggsTemplateConfig config;
	protected Thymeleaf tl;

	public ThymeleafTransformer(HiggsTemplateConfig config) {
		this.config = config;
		tl = new Thymeleaf(this.config);
	}

	@Override
	public boolean canTransform(final Object response, final HiggsHttpRequest request, final List<MediaType> mediaTypes, final HiggsEndpoint endpoint) {
		//first and foremost an endpoint must have a template annotation to even be considered
		if (!endpoint.hasTemplate()) {
			return false;
		}
		for (MediaType type : mediaTypes) {
			if (type.isCompatible(MediaType.WILDCARD_TYPE) ||
					type.isCompatible(MediaType.TEXT_HTML_TYPE) ||
					type.isCompatible(MediaType.APPLICATION_FORM_URLENCODED_TYPE) ||
					type.isCompatible(MediaType.APPLICATION_XHTML_XML_TYPE))
				return true;
		}
		return false;
	}

	@Override
	public HiggsHttpResponse transform(final Object returns, final List<MediaType> mediaTypes,
	                                   final ChannelMessage<HiggsHttpRequest> request,
	                                   final HiggsEndpoint endpoint,
	                                   final Queue<HiggsResponseTransformer> registeredTransformers) {
		HiggsHttpResponse response = new HiggsHttpResponse(request.message);

		HiggsWebContext ctx = new HiggsWebContext();
		populateContext(ctx, returns, request, endpoint);
		byte[] data = null;
		if (returns == null) {
			data = "{}".getBytes();
		} else {
			try {
				String content = tl.getTemplateEngine().process(endpoint.getTemplate(), ctx);
				data = content.getBytes(Charset.forName(config.getCharacterEncoding()));
			} catch (Throwable e) {
				log.warn("Unable to transform response to HTML using Thymeleaf transformer", e);
				response.setStatus(HttpResponseStatus.INTERNAL_SERVER_ERROR);
				return response;
			}
		}
		if (data != null) {
			response.setContent(Unpooled.wrappedBuffer(data));
			HttpHeaders.setContentLength(response, data.length);
		} else {
			return tryNextTransformer(returns, mediaTypes, request, endpoint, registeredTransformers);
		}
		return response;
	}

	private void populateContext(final HiggsWebContext ctx, final Object response,
	                             final ChannelMessage<HiggsHttpRequest> request,
	                             final HiggsEndpoint endpoint) {
		if (response instanceof Map) {

		}
	}

	public HiggsTemplateConfig getConfig() {
		return config;
	}

	/**
	 * Get the Thymeleaf template engine which can be used configured further.
	 *
	 * @return
	 */
	public TemplateEngine getTemplateEngine() {
		return tl.getTemplateEngine();
	}
}
