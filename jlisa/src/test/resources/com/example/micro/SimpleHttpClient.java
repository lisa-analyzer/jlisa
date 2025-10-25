package com.example.micro;

public class SimpleHttpClient {

    private final UserController controller;

    public SimpleHttpClient(UserController controller) {
        this.controller = controller;
    }

    public String sendGet(String url) {
        String name = "";
        int age = 0;

        int qIdx = url.indexOf('?');
        if (qIdx >= 0) {
            String qs = url.substring(qIdx + 1);
            int i = 0;
            while (i < qs.length()) {
                int amp = qs.indexOf('&', i);
                String pair;
                if (amp == -1) {
                    pair = qs.substring(i);
                    i = qs.length();
                } else {
                    pair = qs.substring(i, amp);
                    i = amp + 1;
                }

                int eq = pair.indexOf('=');
                if (eq > 0) {
                    String key = pair.substring(0, eq);
                    String val = pair.substring(eq + 1);
                    if ("name".equals(key)) {
                        name = val;
                    } else if ("age".equals(key)) {

                        int tmp = 0;
                        int j = 0;
                        boolean ok = true;
                        while (j < val.length()) {
                            char c = val.charAt(j);
                            if (c >= '0' && c <= '9') {
                                tmp = tmp * 10 + (c - '0');
                            } else {
                                ok = false;
                                break;
                            }
                            j = j + 1;
                        }
                        if (ok) age = tmp;
                    }
                }
            }
        }

        return controller.getInfo(name, age);
    }
}
