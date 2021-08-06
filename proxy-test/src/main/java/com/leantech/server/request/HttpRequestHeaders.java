package com.leantech.server.request;

public class HttpRequestHeaders implements Header {
    private String request;
    private HttpRequestMethodType requestMethodEnum;
    private String host;
    private Integer port;

    public HttpRequestHeaders(String request) {
        super();
        this.request = request;
        this.parse();
    }

    private void parse() {
        // find host
        String key = new String("\nhost:");
        String newLine = new String("\n");

        int indexOfHost = this.request.toLowerCase().indexOf(key.toLowerCase());
        String host = this.request.substring(indexOfHost + key.length());
        int indexOfFinal = host.indexOf(newLine);
        host = host.substring(0, indexOfFinal).trim();
        int indexOfDots = host.indexOf(new String(":"));

        if (indexOfDots != -1) {
            this.host = host.substring(0, indexOfDots);
        } else {
            this.host = host;
        }

        // find request method
        if (this.request.toLowerCase().startsWith(HttpRequestMethodType.GET.toString().toLowerCase())) {
            this.requestMethodEnum = HttpRequestMethodType.GET;
        } else if (this.request.toLowerCase().startsWith(HttpRequestMethodType.POST.toString().toLowerCase())) {
            this.requestMethodEnum = HttpRequestMethodType.POST;
        } else if (this.request.toLowerCase().startsWith(HttpRequestMethodType.CONNECT.toString().toLowerCase())) {
            this.requestMethodEnum = HttpRequestMethodType.CONNECT;
        }

        // find port
        boolean start = false;
        StringBuilder portBuilder = new StringBuilder();
        for (char c : this.request.toCharArray()) {
            if (c == '\n') {
                break;
            }

            if (c == ':') {
                start = true;
                continue;
            }

            if (start) {
                try {
                    Integer.parseInt(new Character(c).toString());
                    portBuilder.append(c);
                } catch (Exception e) {
                    start = false;
                }
            }
        }

        if (portBuilder.toString().isEmpty()) {
            this.port = 80;
        } else {
            try {
                int portCandidate = Integer.valueOf(portBuilder.toString());
                int indexHostPort = this.request.indexOf(this.getHost() + new String(":") + portCandidate);
                if (indexHostPort != -1) {
                    this.port = portCandidate;
                } else {
                    this.port = 80;
                }
            } catch (Exception e) {
                this.port = 80;
            }
        }
    }

    @Override
    public String toString() {
        return this.depureRequest();
    }

    public String getHost() {
        return this.host;
    }

    public HttpRequestMethodType getRequestMethodEnum() {
        return this.requestMethodEnum;
    }

    public Integer getPort() {
        return this.port;
    }

    public String depureRequest() {
        if (this.requestMethodEnum == HttpRequestMethodType.GET
                || this.requestMethodEnum == HttpRequestMethodType.POST) {
            String toFind = new String("http://") + this.getHost();
            int index = this.request.indexOf(toFind + new String(":") + this.getPort());

            if (index != -1) {
                return this.request.replaceFirst(toFind + new String(":") + this.getPort(), new String(""));
            }

            return this.request.replaceFirst(toFind, new String(""));
        } else {
            return this.request;
        }
    }
}
