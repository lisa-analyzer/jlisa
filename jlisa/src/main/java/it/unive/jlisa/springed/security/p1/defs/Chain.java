package it.unive.jlisa.springed.security.p1.defs;

import java.util.ArrayList;
import java.util.List;

public class Chain {

    private final List<RequestMatcher> requestMatchers = new ArrayList<>();

    private final List<Rule> rules = new ArrayList<>();

    public Chain() {
    }

    public void insert(
            RequestMatcher matcher, Rule rule) {
        this.requestMatchers.add(matcher);
        this.rules.add(rule);
    }

    public List<Rule> getRules() {
        return this.rules;
    }

    public List<RequestMatcher> getRequestMatchers() {
        return this.requestMatchers;
    }
}
