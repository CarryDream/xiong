package http;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.sf.json.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.ParseException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContexts;
import org.apache.http.ssl.TrustStrategy;
import org.apache.http.util.CharsetUtils;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.servlet.http.HttpServletRequest;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class HttpUtil {

    private static Logger logger = Logger.getLogger(HttpUtil.class);

    /**
     * @param reqURL      请求地址
     * @param data        请求参数应该是 name1=value1&name2=value2 的形式
     * @param contentType
     * @return
     */
    public static String sendPost(String reqURL, String data, String contentType) {
        logger.info("Request url : " + reqURL);
        logger.debug("Request data : " + data);
        CloseableHttpClient httpClient = HttpClients.createDefault();
        HttpPost method = new HttpPost(reqURL);
        RequestConfig requestConfig = RequestConfig.custom().setSocketTimeout(90000).setConnectTimeout(30000).build();// 设置请求和传输超时时间
        method.setConfig(requestConfig);
        StringEntity entity = new StringEntity(data, "UTF-8");//
        entity.setContentEncoding("UTF-8");
        entity.setContentType(contentType);
        method.setEntity(entity);
        try {
            HttpResponse resp = httpClient.execute(method);
            logger.info("Send request to service !");
            HttpEntity HttpResp = resp.getEntity();
            String respData = EntityUtils.toString(HttpResp);
            logger.info("Get resp date : " + respData);
            return respData;
        } catch (Exception e) {
            logger.error("Send error! The message :" + e.getMessage());
            logger.error("请求数据 ：" + data);
            return null;
        } finally {
            method.releaseConnection();
        }
    }

    public static JSONObject sendHttp(String reqURL, String data) {
        logger.debug("reqUrl : " + reqURL);
        logger.debug("data : " + data);
        JSONObject jsonObject;
        CloseableHttpClient httpClient;
        if (org.apache.commons.lang.StringUtils.isNotBlank(reqURL) && reqURL.startsWith("https")) {
            boolean ifVerifySslCert = false;
            logger.debug("connecting https URL.");
            logger.debug("read config if_verify_ssl_cert:" + ifVerifySslCert);
            if (!ifVerifySslCert) {
                // 不验证 ssl 证书
                // 没有服务器证书，采用自定义 信任机制
                try {
                    SSLContext sslContext = SSLContexts.custom().loadTrustMaterial(null, new TrustStrategy() {
                        @Override
                        public boolean isTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                            return true;// 信任所有
                        }
                    }).build();
                    SSLConnectionSocketFactory factory = new SSLConnectionSocketFactory(sslContext,
                            new HostnameVerifier() {
                                @Override
                                public boolean verify(String s, SSLSession sslSession) {
                                    return true;
                                }
                            });
                    httpClient = HttpClients.custom().setSSLSocketFactory(factory).build();
                } catch (Exception e) {
                    logger.error("ssl config error!", e);
                    httpClient = HttpClients.createDefault();
                }
            } else {
                logger.debug("需要验证 ssl 证书.");
                logger.debug("暂未实现加载自定义 ssl 证书.");
                httpClient = HttpClients.createDefault();
            }
        } else {
            httpClient = HttpClients.createDefault();
        }
        HttpPost method = new HttpPost(reqURL);

        StringEntity entity = new StringEntity(data, "utf-8");//
        entity.setContentEncoding("UTF-8");
        entity.setContentType("application/json");
        method.setEntity(entity);

        try {
            HttpResponse resp = httpClient.execute(method);
            logger.info("sendHttp -- Recv Protocal !");
            HttpEntity HttpResp = resp.getEntity();
            String charset = EntityUtils.toString(HttpResp, "utf-8");
            logger.debug("received result:" + charset);
            jsonObject = JSONObject.fromObject(charset);

        } catch (Exception e) {
            logger.error("sendHttp -- Send error !,The message :"
                    + e.getMessage());
            return null;
        } finally {
            method.releaseConnection();
        }
        return jsonObject;
    }

    /**
     * @param reqURL 请求参数应该是 name1=value1&name2=value2 的形式
     * @return
     */
    public static String sendGet(String reqURL) {
        try {
            CloseableHttpClient httpClient = HttpClients.createDefault();
            HttpGet httpGet = new HttpGet(reqURL);
            HttpResponse resp = httpClient.execute(httpGet);
            String respData = EntityUtils.toString(resp.getEntity());
            logger.info("Get resp date : " + respData);
            return respData;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static JsonObject sendHttps(String reqURL, String data, boolean ifVerifySslCert) {
        logger.debug("reqUrl : " + reqURL);
        logger.debug("data : " + data);
        JsonObject jsonObject;
        CloseableHttpClient httpClient;
        if (StringUtils.isNotBlank(reqURL) && reqURL.startsWith("https")) {
            logger.debug("connecting https URL.");
            logger.debug("read config if_verify_ssl_cert:" + ifVerifySslCert);
            if (!ifVerifySslCert) {
                // 不验证 ssl 证书
                // 没有服务器证书，采用自定义 信任机制
                try {
                    SSLContext sslContext = SSLContexts.custom().loadTrustMaterial(null, new TrustStrategy() {
                        @Override
                        public boolean isTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                            return true;// 信任所有
                        }
                    }).build();
                    SSLConnectionSocketFactory factory = new SSLConnectionSocketFactory(sslContext,
                            new HostnameVerifier() {
                                @Override
                                public boolean verify(String s, SSLSession sslSession) {
                                    return true;
                                }
                            });
                    httpClient = HttpClients.custom().setSSLSocketFactory(factory).build();
                } catch (Exception e) {
                    logger.error("ssl config error!", e);
                    httpClient = HttpClients.createDefault();
                }
            } else {
                logger.debug("需要验证 ssl 证书.");
                logger.debug("暂未实现加载自定义 ssl 证书.");
                httpClient = HttpClients.createDefault();
            }
        } else {
            httpClient = HttpClients.createDefault();
        }
        HttpPost method = new HttpPost(reqURL);

        StringEntity entity = new StringEntity(data, "utf-8");//
        entity.setContentEncoding("UTF-8");
        entity.setContentType("application/json");
        method.setEntity(entity);

        try {
            HttpResponse resp = httpClient.execute(method);
            logger.info("sendHttp -- Recv Protocal !");
            HttpEntity HttpResp = resp.getEntity();
            String charset = EntityUtils.toString(HttpResp, "utf-8");
            logger.debug("received result:" + charset);
            jsonObject = new JsonParser().parse(charset).getAsJsonObject();
        } catch (Exception e) {
            logger.error("sendHttp -- Send error !,The message :" + e.getMessage());
            return null;
        } finally {
            method.releaseConnection();
        }
        return jsonObject;
    }

    /**
     * 上传文件
     *
     * @throws ParseException
     * @throws IOException
     */
    public static void uploadFile() throws ParseException, IOException {
        CloseableHttpClient httpClient = HttpClients.createDefault();
        try {
            // 要上传的文件的路径
            String filePath = new String("C:\\Users\\zz\\Desktop\\diqu.png");
            // 把一个普通参数和文件上传给下面这个地址 是一个servlet
            HttpPost httpPost = new HttpPost("http://11.2.2.223:8081/auth_center/upload");
            // 把文件转换成流对象FileBody
            File file = new File(filePath);
            FileBody bin = new FileBody(file);
            /*
             * StringBody uploadFileName = new StringBody("my.png",
             * ContentType.create("text/plain", Consts.UTF_8));
             */
            // 以浏览器兼容模式运行，防止文件名乱码。
            HttpEntity reqEntity = MultipartEntityBuilder.create().setMode(HttpMultipartMode.BROWSER_COMPATIBLE)
                    .addPart("uploadFile", bin) // uploadFile对应服务端类的同名属性<File类型>
                    // .addPart("uploadFileName", uploadFileName)//
                    // uploadFileName对应服务端类的同名属性<String类型>
                    .setCharset(CharsetUtils.get("UTF-8")).build();

            httpPost.setEntity(reqEntity);

            System.out.println("发起请求的页面地址 " + httpPost.getRequestLine());
            // 发起请求 并返回请求的响应
            CloseableHttpResponse response = httpClient.execute(httpPost);
            try {
                // 打印响应状态
                System.out.println(response.getStatusLine());
                // 获取响应对象
                HttpEntity resEntity = response.getEntity();
                if (resEntity != null) {
                    // 打印响应长度
                    System.out.println("Response content length: " + resEntity.getContentLength());
                    // 打印响应内容
                    System.out.println(EntityUtils.toString(resEntity, Charset.forName("UTF-8")));
                }
                // 销毁
                EntityUtils.consume(resEntity);
            } finally {
                response.close();
            }
        } finally {
            httpClient.close();
        }
    }

    /**
     * 下载文件
     *
     * @param url          http://www.xxx.com/img/333.jpg
     * @param destFileName xxx.jpg/xxx.png/xxx.txt
     * @throws ClientProtocolException
     * @throws IOException
     */
    public static void downloadFile(String url, String destFileName) throws ClientProtocolException, IOException {
        // 生成一个httpclient对象
        CloseableHttpClient httpclient = HttpClients.createDefault();
        HttpGet httpget = new HttpGet(url);
        HttpResponse response = httpclient.execute(httpget);

        HttpEntity entity = response.getEntity();
        InputStream in = entity.getContent();
        File file = new File(destFileName);
        FileOutputStream fout = new FileOutputStream(file);
        int l = -1;
        byte[] tmp = new byte[1024];
        while ((l = in.read(tmp)) != -1) {
            fout.write(tmp, 0, l); // 注意这里如果用OutputStream.write(buff)的话，图片会失真，大家可以试试
        }
        fout.flush();
        fout.close();
        // 关闭低层流。
        in.close();
    }

    public static String getRequestJsonString(HttpServletRequest request) {
        String submitMehtod = request.getMethod();
        // GET
        if (submitMehtod.equals("GET")) {
            try {
                return new String(request.getQueryString().getBytes(
                        "iso-8859-1"), "utf-8").replaceAll("%22", "\"");
            } catch (UnsupportedEncodingException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            // POST
        } else {
            try {
                return getRequestPostStr(request);
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        return "";
    }

    protected static byte[] getRequestPostBytes(HttpServletRequest request)
            throws IOException {
        int contentLength = request.getContentLength();
        if (contentLength < 0) {
            return null;
        }
        byte buffer[] = new byte[contentLength];
        for (int i = 0; i < contentLength; ) {

            int readlen = request.getInputStream().read(buffer, i,
                    contentLength - i);
            if (readlen == -1) {
                break;
            }
            i += readlen;
        }
        return buffer;
    }

    protected static String getRequestPostStr(HttpServletRequest request)
            throws IOException {
        byte buffer[] = getRequestPostBytes(request);
        String charEncoding = request.getCharacterEncoding();
        if (charEncoding == null) {
            charEncoding = "UTF-8";
        }
        return new String(buffer, charEncoding);
    }

    public static String getParameterMap(HttpServletRequest request) {
        Map map = request.getParameterMap();
        String text = "";
        if (map != null) {
            Set set = map.entrySet();
            Iterator iterator = set.iterator();
            while (iterator.hasNext()) {
                Map.Entry entry = (Map.Entry) iterator.next();
                if (entry.getValue() instanceof String[]) {
                    logger.info("==A==entry的key： " + entry.getKey());
                    String key = (String) entry.getKey();
                    if (key != null && !"id".equals(key) && key.startsWith("[")
                            && key.endsWith("]")) {
                        text = (String) entry.getKey();
                        break;
                    }
                    String[] values = (String[]) entry.getValue();
                    for (int i = 0; i < values.length; i++) {
                        logger.info("==B==entry的value: " + values[i]);
                        key += "=" + values[i];
                    }
                    if (key.startsWith("[") && key.endsWith("]")) {
                        text = (String) entry.getKey();
                        break;
                    }
                } else if (entry.getValue() instanceof String) {
                    logger.info("==========entry的key： " + entry.getKey());
                    logger.info("==========entry的value: " + entry.getValue());
                }
            }
        }
        return text;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static String getParamNames(HttpServletRequest request) {
        Map map = new HashMap();
        Enumeration paramNames = request.getParameterNames();
        while (paramNames.hasMoreElements()) {
            String paramName = (String) paramNames.nextElement();

            String[] paramValues = request.getParameterValues(paramName);
            if (paramValues.length == 1) {
                String paramValue = paramValues[0];
                if (paramValue.length() != 0) {
                    map.put(paramName, paramValue);
                }
            }
        }

        Set<Map.Entry<String, String>> set = map.entrySet();
        String text = "";
        for (Map.Entry entry : set) {
            logger.info(entry.getKey() + ":" + entry.getValue());
            text += entry.getKey() + ":" + entry.getValue();
            logger.info("text------->" + text);
        }
        if (text.startsWith("[") && text.endsWith("]")) {
            return text;
        }
        return "";
    }

    public void Security() {
        System.setProperty("javax.net.ssl.trustStore", "D:/ssl/ssl.keystore");
        System.setProperty("javax.net.ssl.trustStorePassword", "111111");
        System.setProperty("javax.net.ssl.keyStore", "D:/ssl/ssl.keystore");
        System.setProperty("javax.net.ssl.keyStorePassword", "111111");
        // System.setProperty("https.protocols", "SSLv3");
    }

    /**
     * 发送HTTPs请求,需要加载SSL安全证书
     *
     * @param reqURL
     * @param data
     * @return
     */
    public String sendHttps(String reqURL, String data) {
        Security();
        String respData = null;
        CloseableHttpClient httpClient = HttpClients.createDefault();
        HttpPost method = new HttpPost(reqURL);
        StringEntity entity = new StringEntity(data, "utf-8");//
        entity.setContentEncoding("UTF-8");
        entity.setContentType("application/json");
        method.setEntity(entity);
        try {
            HttpResponse resp = httpClient.execute(method);
            logger.info("sendHttps -- Recv Protocal !");
            HttpEntity HttpResp = resp.getEntity();
            respData = EntityUtils.toString(HttpResp);
            logger.info("sendHttps -- The respData : " + respData);
        } catch (Exception e) {
            logger.error("sendHttps -- Send error !,The message :"
                    + e.getMessage());
            e.printStackTrace();
        } finally {
            // 锟酵凤拷锟斤拷锟斤拷
            method.releaseConnection();
        }
        return respData;
    }

    public String getReqString(HttpServletRequest request) {
        logger.info("---------------getReqString-------------");
        StringBuffer jb = new StringBuffer();
        String line = null;
        try {
            BufferedReader reader = request.getReader();
            while ((line = reader.readLine()) != null)
                jb.append(line);
            logger.debug("req data : " + jb.toString());
            return jb.toString();
        } catch (Exception e) {
            // crash and burn
            // throw new IOException("Error parsing JSON request string");
            logger.error("ToString is error . error : " + e.getMessage());
        }
        return "";
    }

}
