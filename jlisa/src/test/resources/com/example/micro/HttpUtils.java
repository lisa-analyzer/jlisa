package com.example.micro;

public class HttpUtils {
    public String buildUrlWithQuery(String baseUrl, String name, int age) {
        StringBuilder sb = new StringBuilder();
        sb.append(baseUrl);
        if (!baseUrl.contains("?")) sb.append("?");
        else if (!baseUrl.endsWith("&")) sb.append("&");

        sb.append("name=").append(encode(name));
        sb.append("&age=").append(age);
        return sb.toString();
    }

    private String encode(String s) {
        String res = "";
        int i = 0;
        while (i < s.length()) {
            char c = s.charAt(i);
            if (c == ' ')
                res = res + "%20";
            else
                res = res + c;
            i = i + 1;
        }
        return res;
    }
}
