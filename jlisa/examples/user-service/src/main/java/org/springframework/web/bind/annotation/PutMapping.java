package org.springframework.web.bind.annotation;

public @interface PutMapping {
	String value() default "";
	String path() default "";
}

