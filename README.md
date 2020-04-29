# 基于httpserver类实现的简易http服务器

CSDN链接：https://blog.csdn.net/qq_40889820/article/details/105814895

# 一、前言

利用Java的`com.sun.net`包下的http类，实现的简易http服务器。全部代码已上传至github，链接在文末。

主要功能为：
1、只处理GET方式和POST方式的请求，其他的返回 `501 Not Implemented`
2、GET：1）如果请求路径为一个存在的html文件，返回`200 OK`和该html文件的全部内容。（对子目录下的文件同样也要能返回）；2）如果请求路径为一个文件夹，且该文件夹内有index.html文件，返回`200 OK`和index.html文件的全部内容；3）如果请求的文件不存在，或者请求的路径下无index.html文件，返回`404 Not Found`。
3、POST：1）构造一个HTTP请求，包含两个键值对：Name:姓名 和ID:学号，提交给`/Post_show`。服务器收到请求后返回`200 OK`且输出以POST方式发送的Name和ID；2）其他情况如请求路径不为`Post_show`或键的名称不为Name和ID，返回`404 Not Found`
4、使用多线程处理，具体为线程池方式实现
5、支持长选项参数：1）--ip  设置服务器端的IP地址；2）--port 设置服务器端监听的端口号；3）--number-thread 线程数目。

# 二、功能展示

这里只贴windows环境下测试的结果了。

## 1、Idea上运行

Run->Edit Configurations 设置参数

![image-20200428144026839](https://raw.githubusercontent.com/wangzhebufangqi/ImageHosting/master/img/image-20200428144026839.png)

也可以不设置，默认值就是上图中的数

点击运行

![image-20200428144144719](https://imgconvert.csdnimg.cn/aHR0cHM6Ly9yYXcuZ2l0aHVidXNlcmNvbnRlbnQuY29tL3dhbmd6aGVidWZhbmdxaS9JbWFnZUhvc3RpbmcvbWFzdGVyL2ltZy9pbWFnZS0yMDIwMDQyODE0NDE0NDcxOS5wbmc?x-oss-process=image/format,png)

这个“当前路径”设置的是：

```java
private static final String WEB_ROOT = System.getProperty("user.dir")
            + File.separator + "webroot";
```

后文有解释。在webroot文件夹内存放了一些子文件夹和html文件等。

### 1.1、浏览器输入请求

浏览器中输入

```
127.0.0.1:8888/sub/02.jpg
```

便可请求到图片`D:\Study\Java\httpserver\webroot\sub\02.jpg`

![image-20200428144426501](https://raw.githubusercontent.com/wangzhebufangqi/ImageHosting/master/img/image-20200428144426501.png)

![image-20200428144632869](https://raw.githubusercontent.com/wangzhebufangqi/ImageHosting/master/img/image-20200428144632869.png)

### 1.2、curl命令请求

curl现在是win10自带的了吧，不用安装直接能用。不过需要注意的是windows和linux下的curl使用存在区别。

![image-20200428145850223](https://imgconvert.csdnimg.cn/aHR0cHM6Ly9yYXcuZ2l0aHVidXNlcmNvbnRlbnQuY29tL3dhbmd6aGVidWZhbmdxaS9JbWFnZUhvc3RpbmcvbWFzdGVyL2ltZy9pbWFnZS0yMDIwMDQyODE0NTg1MDIyMy5wbmc?x-oss-process=image/format,png)

### 1.3、Postman请求

Postman是个很棒的工具！

![image-20200428150629364](https://imgconvert.csdnimg.cn/aHR0cHM6Ly9yYXcuZ2l0aHVidXNlcmNvbnRlbnQuY29tL3dhbmd6aGVidWZhbmdxaS9JbWFnZUhvc3RpbmcvbWFzdGVyL2ltZy9pbWFnZS0yMDIwMDQyODE1MDYyOTM2NC5wbmc?x-oss-process=image/format,png)

## 2、命令行上java -jar运行

参考[IDEA打包jar包详尽流程](https://blog.csdn.net/weixin_42089175/article/details/89113271)打成jar包，放在E盘

![image-20200428151144321](https://imgconvert.csdnimg.cn/aHR0cHM6Ly9yYXcuZ2l0aHVidXNlcmNvbnRlbnQuY29tL3dhbmd6aGVidWZhbmdxaS9JbWFnZUhvc3RpbmcvbWFzdGVyL2ltZy9pbWFnZS0yMDIwMDQyODE1MTE0NDMyMS5wbmc?x-oss-process=image/format,png)

此时将webroot和httpserver.jar放在同一目录下

![image-20200428151542441](https://imgconvert.csdnimg.cn/aHR0cHM6Ly9yYXcuZ2l0aHVidXNlcmNvbnRlbnQuY29tL3dhbmd6aGVidWZhbmdxaS9JbWFnZUhvc3RpbmcvbWFzdGVyL2ltZy9pbWFnZS0yMDIwMDQyODE1MTU0MjQ0MS5wbmc?x-oss-process=image/format,png)

请求方式和1中介绍过的类似

![image-20200428151657616](https://imgconvert.csdnimg.cn/aHR0cHM6Ly9yYXcuZ2l0aHVidXNlcmNvbnRlbnQuY29tL3dhbmd6aGVidWZhbmdxaS9JbWFnZUhvc3RpbmcvbWFzdGVyL2ltZy9pbWFnZS0yMDIwMDQyODE1MTY1NzYxNi5wbmc?x-oss-process=image/format,png)



# 三、具体实现

## 1、支持长选项参数（long options）

主要用到的是`org.apache.commons.cli`包，需要下载。

```java
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
```

如参数给出`--ip 127.0.0.1 --port 8888 --number-thread 2`，则将全局变量IP设为"127.0.0.1"，PORT设为8888，ThreadNum设为2。这些值也是默认值。

## 2、构建httpserver

```java
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
```

接收完命令行参数后，监听地址IP的端口PORT，设能同时接收ThreadNum个请求。然后设置服务器的线程池对象，这里用的是定长线程池newFixedThreadPool，最大线程数设为ThreadNum，也可以使用其他的线程池对象。

然后创建服务器监听的上下文，这里创建了两个。其实创建一个就好了，这里为了方便就创建了两个。第二个表示只要匹配到/Post_show，就会调用PostShowHandler()处理对应的请求。第一个本可以表示只要匹配到/，就会调用MyHandle()处理对应的请求，因为又设置了/Post_show的上下文监听器，所以若http请求为`127.0.0.1:8888/Post_show`的话，只会调用PostShowHandler()而不会调用MyHandler()了。

然后开启服务。

WEB_ROOT为：

```java
private static final String WEB_ROOT = System.getProperty("user.dir") + ...
```

其中，System.getProperty("user.dir")为获得程序当前路径。

## 3、处理Post请求

### 3.1、解析URI参数

```java
static class PostShowHandler implements HttpHandler {
    @Override
    public void handle(HttpExchange httpExchange) throws IOException { 
        String query = httpExchange.getRequestURI().getQuery();
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
```

要处理的POST请求形如：`127.0.0.1:8888/Post_show?Name=jsglyzcq&ID=40889820`。

首先利用getQuery()函数得到Query部分`Name=jsglyzcq&ID=40889820`，然后利用split函数分割字符串，以`&`隔开得到键值对`Name=jsglyzcq`和`ID=40889820`，再利用split函数以`=`分割得到具体的值Name和ID。

如果参数不是两个，或者参数的名称不是`Name`和`ID`等，都返回404，调用函数`NotFoundResponse`，该函数实现见3.2。

### 3.2、不满足条件

```java
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
```

比较简单，看函数名称就知道在做啥。测试如下：

![image-20200428134248810](https://imgconvert.csdnimg.cn/aHR0cHM6Ly9yYXcuZ2l0aHVidXNlcmNvbnRlbnQuY29tL3dhbmd6aGVidWZhbmdxaS9JbWFnZUhvc3RpbmcvbWFzdGVyL2ltZy9pbWFnZS0yMDIwMDQyODEzNDI0ODgxMC5wbmc?x-oss-process=image/format,png)

预览：

![image-20200428134316511](https://imgconvert.csdnimg.cn/aHR0cHM6Ly9yYXcuZ2l0aHVidXNlcmNvbnRlbnQuY29tL3dhbmd6aGVidWZhbmdxaS9JbWFnZUhvc3RpbmcvbWFzdGVyL2ltZy9pbWFnZS0yMDIwMDQyODEzNDMxNjUxMS5wbmc?x-oss-process=image/format,png)

### 3.3、满足条件

```java
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
```

和3.2差不多，就是返回内容有所更改

![image-20200428133923463](https://imgconvert.csdnimg.cn/aHR0cHM6Ly9yYXcuZ2l0aHVidXNlcmNvbnRlbnQuY29tL3dhbmd6aGVidWZhbmdxaS9JbWFnZUhvc3RpbmcvbWFzdGVyL2ltZy9pbWFnZS0yMDIwMDQyODEzMzkyMzQ2My5wbmc?x-oss-process=image/format,png)

预览：

![image-20200428134019690](https://imgconvert.csdnimg.cn/aHR0cHM6Ly9yYXcuZ2l0aHVidXNlcmNvbnRlbnQuY29tL3dhbmd6aGVidWZhbmdxaS9JbWFnZUhvc3RpbmcvbWFzdGVyL2ltZy9pbWFnZS0yMDIwMDQyODEzNDAxOTY5MC5wbmc?x-oss-process=image/format,png)

## 4、处理Get请求

### 4.1、判断何种方式请求

```java
static class MyHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange httpExchange) throws IOException {
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
```

利用getRequestMethod()函数得到是何种方式的请求。判断访问路径是否存在见4.2。

如果访问路径存在，并且访问方式为`GET`，则调用函数HandleGetRequest，见4.3；若访问方式为`POST`，也可以调用相应的函数，但我在之前已经实现过了，这里就不填了；若访问方式为除了`GET`和`POST`以外的方式，则返回501  Not IMPLEMENTED：

![image-20200428135640254](https://raw.githubusercontent.com/wangzhebufangqi/ImageHosting/master/img/image-20200428135640254.png)

预览：

![image-20200428135707110](https://raw.githubusercontent.com/wangzhebufangqi/ImageHosting/master/img/image-20200428135707110.png)

### 4.2、判断访问路径是否存在

```java
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
```

前文提到，WEB_ROOT为获取的当前程序正在运行的路径，加上requestURI得到请求的文件，利用exists()函数判断是否存在。若存在则返回false，表示不是404；若不存在则调用3.2中提过的函数NotFoundResponse()。

### 4.3、满足条件

```java
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
                NotFoundResponse(httpExchange);//这里要处理异常，为了减少篇幅删去了
            } else {//这里要处理异常，为了减少篇幅删去了
                FileSendResponse(httpExchange, filetoSend, "text/html; charset=utf-8");
            }
        } else {
            if (file.getPath().endsWith(".jpg")) {
                //这里要处理异常，为了减少篇幅删去了
                FileSendResponse(httpExchange, file, "image/jpeg");
            }else if(file.getPath().endsWith(".html")){
                //这里要处理异常，为了减少篇幅删去了
                FileSendResponse(httpExchange, file, "text/html; charset=utf-8");
            }
        }
    }
```

调用这个函数时说明访问路径是存在的。然后进一步判断访问的是一个文件夹还是文件，如果访问的是文件夹，那么查找该文件夹内是否有文件名为`index.html`，若有则调用FileSendResponse()函数发送文件，见4.4，否则报404；如果访问的是一个文件，就根据文件后缀名，设置相应的媒体类型mime。

### 4.4、发送文件

```java
private static void FileSendResponse(HttpExchange httpExchange, File file, String mime) throws Exception {
        Headers responseHeaders = httpExchange.getResponseHeaders();
        responseHeaders.set("Content-Type", mime);
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
```

将文件以字节流的形式发送出去。这里的缓冲区大小为10240。

例：

![image-20200428141322487](https://imgconvert.csdnimg.cn/aHR0cHM6Ly9yYXcuZ2l0aHVidXNlcmNvbnRlbnQuY29tL3dhbmd6aGVidWZhbmdxaS9JbWFnZUhvc3RpbmcvbWFzdGVyL2ltZy9pbWFnZS0yMDIwMDQyODE0MTMyMjQ4Ny5wbmc?x-oss-process=image/format,png)

# 四、后记

这是笔者对大三下选修的云计算实验二基础版本的实现，因为时间关系，直接选用了httpserver来实现。毕竟是两天内完成的，若文中有词不达意、错漏之处，敬请指正:fish:

代码链接：https://github.com/wangzhebufangqi/httpserver



