package org.springframework.web.bind.annotation;

public @interface DeleteMapping {
	String value() default "";
	String path() default "";
}

