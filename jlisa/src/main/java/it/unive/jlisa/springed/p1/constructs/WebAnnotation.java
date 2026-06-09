package it.unive.jlisa.springed.p1.constructs;

import java.util.Map;

public class WebAnnotation {

	private final String httpMethod;
	private final String addressPath;
	private final Map<String, Object> params;
	private final Map<String, Object> headers;

	public WebAnnotation(
			String httpMethod,
			String addressPath,
			Map<String, Object> params,
			Map<String, Object> headers) {
		this.httpMethod = httpMethod;
		this.addressPath = addressPath;
		this.params = params;
		this.headers = headers;
	}

	public String getHttpMethod() {
		return httpMethod;
	}

	public String getAddressPath() {
		return addressPath;
	}

	public Map<String, Object> getParams() {
		return params;
	}

	public Map<String, Object> getHeaders() {
		return headers;
	}

}
