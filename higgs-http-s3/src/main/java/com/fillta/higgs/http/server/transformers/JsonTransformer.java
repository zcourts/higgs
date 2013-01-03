package com.fillta.higgs.http.server.transformers;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.introspect.VisibilityChecker;
import com.fillta.higgs.http.server.*;
import com.fillta.higgs.http.server.resource.MediaType;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.HttpHeaders;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
	public boolean canTransform(Object response, HttpRequest request) {
		for (MediaType type : request.getMediaTypes()) {
			if (type.isCompatible(MediaType.TEXT_PLAIN_TYPE) ||
					type.isCompatible(MediaType.APPLICATION_JSON_TYPE))
				return true;
		}
		return false;
	}

	@Override
	public HttpResponse transform(HttpServer server, final Object returns, HttpRequest request,
	                              Queue<ResponseTransformer> registeredTransformers) {
		HttpResponse response = new HttpResponse(request);
		byte[] data = null;
		if (returns == null) {
			data = "{}".getBytes();
		} else {
			try {
				data = mapper.writeValueAsBytes(returns);
			} catch (JsonProcessingException e) {
				log.warn("Unable to transform response to JSON", e);
				response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR);
				return response;
			}
		}
		if (data != null) {
			response.setContent(Unpooled.wrappedBuffer(data));
			HttpHeaders.setContentLength(response, data.length);
		} else {
			return tryNextTransformer(server, returns, request, registeredTransformers);
		}
		return response;
	}
}
