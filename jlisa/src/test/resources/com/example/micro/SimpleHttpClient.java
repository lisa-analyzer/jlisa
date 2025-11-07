package com.example.micro;

public class SimpleHttpClient {
    private final UserController controller;

    public SimpleHttpClient(UserController controller) {
        this.controller = controller;
    }

    public String sendGet(String url) {
        int q = url.indexOf('?');
        String query = q < 0 ? "" : url.substring(q + 1);
        String name = "unknown";
        int age = -1;

        if (!query.isEmpty()) {
            String[] pairs = query.split("&");
            for (String pair : pairs) {
                int eq = pair.indexOf('=');
                if (eq < 0) continue;
                String key = pair.substring(0, eq);
                String val = pair.substring(eq + 1);
                if ("name".equals(key)) name = decode(val);
                else if ("age".equals(key)) try { age = Integer.parseInt(val); } catch (NumberFormatException ignored) {}
            }
        }

        // the path connected to @GetMapping
        return controller.getInfo(name, age);
    }

    private static String decode(String s) { return s.replace("%20", " "); }
}
