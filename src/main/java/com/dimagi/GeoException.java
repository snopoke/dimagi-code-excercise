package com.dimagi;

import java.io.IOException;

public class GeoException extends Throwable {
    public GeoException(Exception e) {
        super(e);
    }
}
