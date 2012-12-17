package com.dimagi;

import java.util.Date;

public class Message {
    private String subject;
    private String name;
    private String email;
    private Date sentDate;

    public Message(String subject, String name, String email, Date sentDate) {
        this.subject = subject;
        this.name = name;
        this.email = email;
        this.sentDate = sentDate;
    }

    public String getSubject() {
        return subject;
    }

    public Date getSentDate() {
        return sentDate;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }
}
