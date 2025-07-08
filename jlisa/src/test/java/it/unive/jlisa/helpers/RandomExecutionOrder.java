package it.unive.jlisa.helpers;

import org.junit.jupiter.api.MethodDescriptor;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.MethodOrdererContext;

import java.util.Collections;
import java.util.List;

public class RandomExecutionOrder implements MethodOrderer {
    @Override
    public void orderMethods(MethodOrdererContext context) {
        List<? extends MethodDescriptor> methods = context.getMethodDescriptors();
        Collections.shuffle(methods);
    }
}