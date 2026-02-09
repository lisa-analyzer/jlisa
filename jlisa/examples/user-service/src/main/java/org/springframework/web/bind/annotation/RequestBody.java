package org.springframework.web.bind.annotation;

public @interface RequestBody {
	boolean required() default true;
}

