package com.fillta.higgs.http.server.config;

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
public class ServerConfig {
	/**
	 * If true then a new instance of resource classes are  created for every request the class matches
	 * if false then a single instance is used to service every request.
	 * useful for concurrency issues
	 */
	public boolean instance_per_request = true;
	/**
	 * the port the server binds to
	 */
	public int port = 8080;
	public boolean add_thymeleaf_transformer = true;
	public boolean add_json_transformer = true;
	public boolean add_default_error_transformer = true;
	public TemplateConfig template_config = new TemplateConfig();
	public String session_path = "/";
	//ignored if null
	public String session_domain;
	//7 days in milliseconds
	public long session_max_age = 604800000;
	public boolean session_http_only;
	//ignored if null
	public String session_ports;
	public FilesConfig files = new FilesConfig();
	public String default_error_template = "error/default";
	public boolean add_default_injector = true;
	public boolean add_default_resource_filter = true;
	public boolean add_static_resource_filter = true;
}
