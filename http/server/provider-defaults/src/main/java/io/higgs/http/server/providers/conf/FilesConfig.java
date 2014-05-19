package io.higgs.http.server.providers.conf;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
public class FilesConfig {
    public boolean delete_temp_on_exit = true;
    //use default system temp dir if null
    public String temp_directory;
    //how big are the chunks when sending a file
    public int chunk_size = 8192;
    //colon separates each, comma, separates multiple extensions
    public Map<String, String> custom_mime_types = new HashMap<>();
    public int priority = -1;

    public FilesConfig() {
        //add some default mime types
        custom_mime_types.put("htm,html", "text/html");
        custom_mime_types.put("json", "application/json");
        custom_mime_types.put("xml", "application/xml");
        custom_mime_types.put("png", "image/png");
        custom_mime_types.put("css", "text/css");
        custom_mime_types.put("js", "text/javascript");
    }
}
