package com.dimagi;

import java.util.Date;

public class ParsedMessage extends MailMessage {
    private String location;

    public ParsedMessage(String subject, String name, String email, Date sentDate, String location) {
        super(subject, name, email, sentDate);
        this.location = location;
    }

    public ParsedMessage(MailMessage m, String location) {
        super(m.getSubject(), m.getName(), m.getEmail(), m.getSentDate());
        this.location = location;
    }

    public String getLocation() {
        return location;
    }
}
