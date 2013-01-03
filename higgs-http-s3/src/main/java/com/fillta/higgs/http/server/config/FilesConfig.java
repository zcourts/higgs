package com.fillta.higgs.http.server.config;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
public class FilesConfig {
	public boolean delete_temp_on_exit = true;
	//use default system temp dir if null
	public String temp_directory = null;
	//how big are the chunks when sending a file
	public int chunk_size = 8192;
	public String public_directory = "public";
	public boolean enable_directory_listing = true;
	public boolean serve_index_file = true;
	public String index_file = "/index.html";
	//colon separates each, comma, separates multiple extensions
	public Map<String, String> custom_mime_types = new HashMap<>(); //"htm,html:text/html;json:application/json;xml:application/xml";
}
