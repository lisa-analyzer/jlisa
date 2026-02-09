package it.unive.jlisa.frontend.annotations;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents a REST endpoint extracted from Spring annotations.
 * Contains HTTP method, path, and query parameters.
 * 
 * Example:
 *   @GetMapping("/users/info")
 *   public String info(@RequestParam("name") String name, @RequestParam("age") int age)
 *   
 *   EndpointInfo(method="GET", path="/users/info", queryParams=["name", "age"])
 */
public class EndpointInfo {

	/**
	 * HTTP method (GET, POST, PUT, DELETE, PATCH)
	 */
	private final String method;

	/**
	 * Endpoint path (e.g., "/users/info" or "/api/users/{id}")
	 */
	private final String path;

	/**
	 * List of query parameter names extracted from @RequestParam annotations
	 */
	private final List<String> queryParams;

	/**
	 * Handler method signature (e.g., "UserController.info")
	 */
	private final String handler;

	/**
	 * Creates a new EndpointInfo.
	 * 
	 * @param method      HTTP method (GET, POST, etc.)
	 * @param path        endpoint path
	 * @param handler     handler method signature
	 * @param queryParams list of query parameter names
	 */
	public EndpointInfo(
			String method,
			String path,
			String handler,
			List<String> queryParams) {
		this.method = method == null ? "UNKNOWN" : method;
		this.path = path == null ? "" : path;
		this.handler = handler == null ? "" : handler;
		this.queryParams = queryParams == null 
				? Collections.emptyList() 
				: Collections.unmodifiableList(new ArrayList<>(queryParams));
	}

	/**
	 * Creates an EndpointInfo with empty query params.
	 */
	public EndpointInfo(
			String method,
			String path,
			String handler) {
		this(method, path, handler, Collections.emptyList());
	}

	public String getMethod() {
		return method;
	}

	public String getPath() {
		return path;
	}

	public List<String> getQueryParams() {
		return queryParams;
	}

	public String getHandler() {
		return handler;
	}

	/**
	 * Checks if this endpoint has any information (not empty).
	 */
	public boolean isEmpty() {
		return "UNKNOWN".equals(method) && (path == null || path.isEmpty());
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("Endpoint(");
		sb.append("method=\"").append(method).append("\"");
		if (path != null && !path.isEmpty()) {
			sb.append(", path=\"").append(path).append("\"");
		}
		if (handler != null && !handler.isEmpty()) {
			sb.append(", handler=\"").append(handler).append("\"");
		}
		if (!queryParams.isEmpty()) {
			sb.append(", queryParams=").append(queryParams);
		}
		sb.append(")");
		return sb.toString();
	}
}

