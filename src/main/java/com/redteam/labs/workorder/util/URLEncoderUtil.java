package com.redteam.labs.workorder.util;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

public class URLEncoderUtil
{
    public static String encodeURIComponent(String value) {
        try {
            return URLEncoder.encode(value, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("UTF-8 encoding not supported", e);
        }
    }
}
