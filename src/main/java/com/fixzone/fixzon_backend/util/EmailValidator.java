package com.fixzone.fixzon_backend.util;

import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import java.util.Hashtable;
import java.util.Set;

public class EmailValidator {

    private static final Set<String> BLOCKED_DOMAINS = Set.of(
        "example.com", "example.org", "example.net",
        "test.com", "test.org", "test.net",
        "sample.com", "sample.org", "sample.net",
        "fake.com", "demo.com", "dummy.com", "invalid.com",
        "mailinator.com", "tempmail.com", "10minutemail.com",
        "guerrillamail.com", "trashmail.com", "yopmail.com",
        "sharklasers.com", "getairmail.com", "dispostable.com"
    );

    /**
     * Performs a real-time DNS lookup for MX (Mail Exchange) or A records
     * to confirm that the domain actually exists and is configured to receive emails.
     */
    public static boolean hasMxRecord(String domain) {
        try {
            Hashtable<String, String> env = new Hashtable<>();
            env.put("java.naming.factory.initial", "com.sun.jndi.dns.DnsContextFactory");
            env.put("com.sun.jndi.dns.timeout.initial", "3000");
            env.put("com.sun.jndi.dns.timeout.retries", "1");
            
            DirContext ictx = new InitialDirContext(env);
            Attributes attrs = ictx.getAttributes(domain, new String[] { "MX" });
            Attribute attr = attrs.get("MX");
            if (attr != null && attr.size() > 0) {
                return true;
            }
            
            // Fallback: RFC 5321 allows direct mail delivery to an address matching the A record
            attrs = ictx.getAttributes(domain, new String[] { "A" });
            attr = attrs.get("A");
            return attr != null && attr.size() > 0;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Checks that an email has valid RFC syntax, is not a known dummy/disposable domain,
     * and has an existing, active mail exchange (MX) DNS server.
     */
    public static boolean isValidRealEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }
        String clean = email.trim().toLowerCase();
        if (!clean.matches("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$")) {
            return false;
        }
        String[] parts = clean.split("@");
        if (parts.length != 2) {
            return false;
        }
        String domain = parts[1];
        if (BLOCKED_DOMAINS.contains(domain)) {
            return false;
        }
        return hasMxRecord(domain);
    }
}
