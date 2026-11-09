package it.unive.jlisa.springed.security.p1;

import it.unive.jlisa.springed.security.p1.defs.Chain;

import java.util.ArrayList;
import java.util.List;

public class SecurityChains {

    private final List<Chain> chains = new ArrayList<>();

    public SecurityChains() {
    }

    public List<Chain> getChains() {
        return this.chains;
    }

    public void insert(
            Chain chain) {
        this.chains.add(chain);
    }
}
