import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.spi.HttpServerProvider;
import org.apache.commons.cli.*;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;

import java.util.concurrent.*;

public class httpserver {
    private static int PORT = 8888;
    private static String IP = "127.0.0.1";
    private static int ThreadNum = 1;

//    private static final String WEB_ROOT = System.getProperty("user.dir")
//            + File.separator + "webroot";
    private static String CRLF = "\r\n";
    public static final String WEB_ROOT = System.getProperty("user.dir");

    public static void main(String[] args) {
        //System.out.println(WEB_ROOT);
        HandleOption(args);
        HttpServerProvider provider = HttpServerProvider.provider();
        HttpServer httpserver = null;
        try {
            httpserver = provider.createHttpServer(new InetSocketAddress(IP, PORT), ThreadNum);//监听端口PORT,能同时接受ThreadNum个请求
            httpserver.setExecutor(Executors.newFixedThreadPool(ThreadNum));
        } catch (Exception e) {
            e.printStackTrace();
        }

        httpserver.createContext("/", new MyHandler());
        httpserver.createContext("/Post_show", new PostShowHandler());
        httpserver.start();
        System.out.println("当前路径："+WEB_ROOT);
        System.out.println("Http Server is running...");
    }
    public httpserver(){

    }
    private static void HandleOption(String[] args) {
        CommandLineParser parser = new BasicParser();
        Options options = new Options();
        options.addOption("i", "ip", true, "Specify the server IP address");
        options.addOption("p", "port", true, "Selects which port the HTTP server listens on for incoming connections");
        options.addOption("n", "number-thread", true, "The number of threads");
        try {
            CommandLine commandLine = parser.parse(options, args);
            if (commandLine.hasOption('i')) {
                IP = commandLine.getOptionValue('i');
            }
            if (commandLine.hasOption('p')) {

                PORT = Integer.parseInt(commandLine.getOptionValue('p'));
            }
            if (commandLine.hasOption('n')) {

                ThreadNum = Integer.parseInt(commandLine.getOptionValue('n'));
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    static class MyHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange httpExchange) throws IOException {
            System.out.println(httpExchange.getRequestMethod());
            String requestMethord = httpExchange.getRequestMethod();
            try {
                if (is404NotFound(httpExchange)) {
                    System.out.println(httpExchange.getRequestURI() + " does not exist!");
                    return;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (requestMethord.equalsIgnoreCase("GET")) {
                HandleGetRequest(httpExchange);
            } else if (requestMethord.equalsIgnoreCase("POST")) {
                //
            } else {
                Headers responseHeaders = httpExchange.getResponseHeaders();
                responseHeaders.set("Content-Type", "text/html;charset=utf-8");
                OutputStream responseBody = httpExchange.getResponseBody();
                OutputStreamWriter writer = new OutputStreamWriter(responseBody, "UTF-8");
                String response = "<html><title>501 Not Implemented</title><body bgcolor=ffffff>" + CRLF +
                        "Not Implemented" + CRLF +
                        "<p>Does not implement this methord: " + requestMethord + CRLF +
                        "<hr><em>HTTP Web Server</em>" + CRLF +
                        "</body></html>";
                httpExchange.sendResponseHeaders(HttpURLConnection.HTTP_NOT_IMPLEMENTED, response.getBytes("UTF-8").length);
                writer.write(response);
                writer.close();
                responseBody.close();
            }
        }
    }

    static class PostShowHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange httpExchange) throws IOException {
            //System.out.println(httpExchange.getRequestMethod());
            String query = httpExchange.getRequestURI().getQuery();
            //System.out.println(query);
            String[] keyValues = query.split("&");
            if (keyValues.length != 2) {
                System.out.println(keyValues.length);
                try {
                    NotFoundResponse(httpExchange);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return;
            }

            String[] NamePara = keyValues[0].split("=");
            String[] IDPara = keyValues[1].split("=");
            if (!NamePara[0].equals("Name") || !IDPara[0].equals("ID") || NamePara.length != 2 || IDPara.length != 2) {
                try {
                    NotFoundResponse(httpExchange);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return;
            }
            try {
                HandlePostRequest(httpExchange, NamePara[1], IDPara[1]);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private static boolean is404NotFound(HttpExchange httpExchange) throws Exception {
        String requestURI = String.valueOf(httpExchange.getRequestURI());
        File file = new File(WEB_ROOT, requestURI);
        if (file.exists()) return false;
        else {
            System.out.println(file.getPath() + "does not exist!" + CRLF);
            NotFoundResponse(httpExchange);
            return true;
        }
    }

    private static void NotFoundResponse(HttpExchange httpExchange) throws Exception {
        Headers responseHeaders = httpExchange.getResponseHeaders();
        responseHeaders.set("Content-Type", "text/html;charset=utf-8");
        OutputStream responseBody = httpExchange.getResponseBody();
        OutputStreamWriter writer = new OutputStreamWriter(responseBody, "UTF-8");
        String response = "<html><title>404 Not Found</title><body bgcolor=ffffff>" + CRLF +
                "Not Found" + CRLF +
                "<hr><em>HTTP Web Server</em>" + CRLF +
                "</body></html>";
        httpExchange.sendResponseHeaders(HttpURLConnection.HTTP_NOT_FOUND, response.getBytes("UTF-8").length);
        writer.write(response);
        writer.close();
        responseBody.close();
    }

    private static void FileSendResponse(HttpExchange httpExchange, File file, String mime) throws Exception {
        Headers responseHeaders = httpExchange.getResponseHeaders();
        responseHeaders.set("Content-Type", mime);
        //System.out.println(file.getAbsolutePath());
        OutputStream responseBody = httpExchange.getResponseBody();
        httpExchange.sendResponseHeaders(HttpURLConnection.HTTP_OK, file.length());
        if (file.exists()) {
            //System.out.println(file.length());
            System.out.println(file.getAbsolutePath() + " Exist");
            byte[] buffer = new byte[10240];
            FileInputStream fis = new FileInputStream(file);
            int readLength;
            while ((readLength = fis.read(buffer, 0, 10240)) > 0) {
                responseBody.write(buffer, 0, readLength);
            }
        }
        responseBody.close();
    }

    private static void HandleGetRequest(HttpExchange httpExchange) {
        String requestURI = String.valueOf(httpExchange.getRequestURI());
        File file = new File(WEB_ROOT, requestURI);
        File filetoSend = null;
        boolean hasIndexHtml = false;
        if (file.isDirectory()) {//若为文件夹
            String[] files = file.list();

            for (String filename : files) {
                if (filename.equals("index.html")) {
                    filetoSend = new File(file.getPath() + File.separator + filename);
                    hasIndexHtml = true;
                    break;
                }
            }
            if (!hasIndexHtml) {
                try {
                    NotFoundResponse(httpExchange);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                try {
                    FileSendResponse(httpExchange, filetoSend, "text/html; charset=utf-8");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }


        } else {
            if (file.getPath().endsWith(".jpg")) {
                try {
                    FileSendResponse(httpExchange, file, "image/jpeg");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }else if(file.getPath().endsWith(".txt")){
                try {
                    FileSendResponse(httpExchange, file, "text/plain");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }else{
                try {
                    FileSendResponse(httpExchange, file, "text/html; charset=utf-8");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private static void HandlePostRequest(HttpExchange httpExchange, String Name, String ID) throws Exception {
        Headers responseHeaders = httpExchange.getResponseHeaders();
        responseHeaders.set("Content-Type", "text/html; charset=utf-8");
        OutputStream responseBody = httpExchange.getResponseBody();
        OutputStreamWriter writer = new OutputStreamWriter(responseBody, "UTF-8");
        String response = "<html><title>" + httpExchange.getRequestMethod() + "</title><body bgcolor=ffffff>" + CRLF +
                "Your Name: " + Name + CRLF +
                "ID: " + ID + CRLF +
                "<hr><em>HTTP Web Server</em>" + CRLF +
                "</body></html>";
        httpExchange.sendResponseHeaders(HttpURLConnection.HTTP_OK, response.getBytes("UTF-8").length);
        writer.write(response);
        writer.close();
        responseBody.close();

    }


}
