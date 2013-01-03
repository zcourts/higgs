package com.fillta.higgs.http.server.transformers;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.introspect.VisibilityChecker;
import com.fillta.higgs.events.ChannelMessage;
import com.fillta.higgs.http.server.HiggsEndpoint;
import com.fillta.higgs.http.server.HiggsHttpRequest;
import com.fillta.higgs.http.server.HiggsHttpResponse;
import com.fillta.higgs.http.server.HiggsResponseTransformer;
import com.fillta.higgs.http.server.resource.MediaType;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponseStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Queue;

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
public class JsonTransformer extends BaseTransformer {
	private Logger log = LoggerFactory.getLogger(getClass());
	protected final ObjectMapper mapper = new ObjectMapper();

	public JsonTransformer() {
		mapper.configure(SerializationFeature.WRITE_NULL_MAP_VALUES, true);
		//auto discover fields
		VisibilityChecker visibilityChecker = mapper.getSerializationConfig().getDefaultVisibilityChecker();
		visibilityChecker.withFieldVisibility(JsonAutoDetect.Visibility.ANY);
		visibilityChecker.withGetterVisibility(JsonAutoDetect.Visibility.ANY);
		visibilityChecker.withSetterVisibility(JsonAutoDetect.Visibility.ANY);
		visibilityChecker.withCreatorVisibility(JsonAutoDetect.Visibility.ANY);
		mapper.setVisibilityChecker(visibilityChecker);
	}

	@Override
	public boolean canTransform(final Object response, final HiggsHttpRequest request, final List<MediaType> mediaTypes, final HiggsEndpoint endpoint) {
		for (MediaType type : mediaTypes) {
			if (type.isCompatible(MediaType.TEXT_PLAIN_TYPE) ||
					type.isCompatible(MediaType.APPLICATION_JSON_TYPE))
				return true;
		}
		return false;
	}

	@Override
	public HiggsHttpResponse transform(final Object returns, final List<MediaType> mediaTypes,
	                                   final ChannelMessage<HiggsHttpRequest> request,
	                                   final HiggsEndpoint endpoint, final Queue<HiggsResponseTransformer> registeredTransformers) {
		HiggsHttpResponse response = new HiggsHttpResponse(request.message);
		byte[] data = null;
		if (returns == null) {
			data = "{}".getBytes();
		} else {
			try {
				data = mapper.writeValueAsBytes(returns);
			} catch (JsonProcessingException e) {
				log.warn("Unable to transform response to JSON", e);
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
}
