package com.dimagi;

import java.util.Date;

public class LocatedMessage extends ParsedMessage {
    private Double lat;
    private Double lng;

    public LocatedMessage(String subject, String name, String email, Date sentDate, String location, Double lat, Double lng) {
        super(subject, name, email, sentDate, location);
        this.lat = lat;
        this.lng = lng;
    }

    public LocatedMessage(ParsedMessage m, Double lat, Double lng) {
        super(m, m.getLocation());
        this.lat = lat;
        this.lng = lng;
    }

    public Double getLat() {
        return lat;
    }

    public Double getLng() {
        return lng;
    }
}
