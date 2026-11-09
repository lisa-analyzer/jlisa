package it.unive.jlisa.springed.security.p1.defs;

public enum Rule {

    PERMIT_ALL,
    DENY_ALL,
    AUTHENTICATED,
    FULLY_AUTHENTICATED,
    REMEMBER_ME,
    ANONYMOUS,
    HAS_ROLE,
    HAS_ANY_ROLE,
    HAS_ALL_ROLES,
    HAS_AUTHORITY,
    HAS_ANY_AUTHORITY,
    HAS_ALL_AUTHORITIES,
    HAS_VARIABLE
}
