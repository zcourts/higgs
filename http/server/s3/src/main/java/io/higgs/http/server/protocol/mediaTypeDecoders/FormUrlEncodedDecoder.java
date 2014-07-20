package io.higgs.http.server.protocol.mediaTypeDecoders;

import io.higgs.core.reflect.dependency.DependencyProvider;
import io.higgs.http.server.HttpRequest;
import io.higgs.http.server.HttpStatus;
import io.higgs.http.server.params.HttpFile;
import io.higgs.http.server.protocol.MediaTypeDecoder;
import io.higgs.http.server.resource.MediaType;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.multipart.DefaultHttpDataFactory;
import io.netty.handler.codec.http.multipart.FileUpload;
import io.netty.handler.codec.http.multipart.HttpDataFactory;
import io.netty.handler.codec.http.multipart.HttpPostRequestDecoder;
import io.netty.handler.codec.http.multipart.InterfaceHttpData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.WebApplicationException;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
public class FormUrlEncodedDecoder implements MediaTypeDecoder {
    protected static HttpDataFactory factory = new DefaultHttpDataFactory(DefaultHttpDataFactory.MINSIZE); //Disk
    protected final HttpRequest request;
    protected HttpPostRequestDecoder decoder;
    protected Logger log = LoggerFactory.getLogger(getClass());

    public FormUrlEncodedDecoder(HttpRequest request) {
        this.request = request;
        try {
            decoder = new HttpPostRequestDecoder(factory, request);
        } catch (HttpPostRequestDecoder.ErrorDataDecoderException e1) {
            log.warn("Unable to decode data", e1);
            throw new WebApplicationException(HttpStatus.BAD_REQUEST.code());
        } catch (HttpPostRequestDecoder.IncompatibleDataDecoderException e) {
            log.warn("Incompatible request type", e);
            throw new WebApplicationException(HttpStatus.BAD_REQUEST.code());
        }
        request.setMultipart(decoder.isMultipart());
    }

    @Override
    public boolean canDecode(List<MediaType> mediaType) {
        if (mediaType == null || mediaType.size() == 0) {
            return true; //by default if no media type is provided assume form url encoding
        }
        for (MediaType m : mediaType) {
            if (MediaType.APPLICATION_FORM_URLENCODED_TYPE.isCompatible(m)
                    || MediaType.MULTIPART_FORM_DATA_TYPE.isCompatible(m)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void offer(HttpContent chunk) {
        try {
            decoder.offer(chunk);
        } catch (HttpPostRequestDecoder.ErrorDataDecoderException e1) {
            log.warn("Unable to decode HTTP chunk", e1);
            throw new WebApplicationException(HttpStatus.BAD_REQUEST.code());
        }
        try {
            while (decoder.hasNext()) {
                InterfaceHttpData data = decoder.next();
                if (data != null) {
                    writeHttpData(data);
                }
            }
        } catch (HttpPostRequestDecoder.EndOfDataDecoderException e1) {
            // end
        }
    }

    private void writeHttpData(InterfaceHttpData data) {
        //check if is file upload or attribute, attributes go into form params and file uploads to file params
        if (data.getHttpDataType() == InterfaceHttpData.HttpDataType.Attribute) {
            io.netty.handler.codec.http.multipart.Attribute field =
                    (io.netty.handler.codec.http.multipart.Attribute) data;
            try {
                //the [] detection adds support for PHPism where name[] is used on radio buttons
                //add form param
                String name = field.getName();
                int idx = name.indexOf("[");
                if (idx != -1 && name.endsWith("]")) {
                    String realName = name.substring(0, idx);
                    String fieldName = name.substring(idx + 1).replace(']', ' ').trim();
                    if (request.getFormParam().get(realName) == null) {
                        request.getFormParam().put(realName, new HashMap<String, String>());
                    }
                    ((HashMap<String, String>) request.getFormParam().get(realName)).put(fieldName, field.getValue());
                } else {
                    request.addFormField(name, field.getValue());
                }
            } catch (IOException e) {
                log.warn(String.format("unable to extract form field's value, field name = %s", field.getName()));
            }
        } else {
            if (data instanceof FileUpload) {
                //add form file
                request.addFormFile(new HttpFile((FileUpload) data));
            } else {
                log.warn(String.format("Unknown form type encountered Class: %s,data type:%s,name:%s",
                        data.getClass().getName(), data.getHttpDataType().name(), data.getName()));
            }
        }
    }

    @Override
    public void finished(ChannelHandlerContext ctx) {
        //entire message/request received
        List<InterfaceHttpData> data;
        try {
            data = decoder.getBodyHttpDatas();
        } catch (HttpPostRequestDecoder.NotEnoughDataDecoderException e1) {
            log.warn("Not enough data to decode", e1);
            throw new WebApplicationException(HttpStatus.BAD_REQUEST.code());
        }
        //called when all data is received, go over request data and separate form fields from files
        for (InterfaceHttpData httpData : data) {
            writeHttpData(httpData);
        }
    }

    @Override
    public DependencyProvider provider() {
        return DependencyProvider.from();
    }
}
