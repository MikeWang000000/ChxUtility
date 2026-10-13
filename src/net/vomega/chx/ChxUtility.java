/*
 * Decompiled with CFR 0.152.
 */
package net.vomega.chx;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.io.StringReader;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import net.vomega.chx.ChxAccount;
import net.vomega.chx.ChxCourse;
import net.vomega.chx.ChxResource;
import net.vomega.chx.ChxSection;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class ChxUtility
implements Serializable {
    private transient HttpClient client;
    private HashMap<String, String> chxData = new HashMap();
    private Map<String, List<String>> cookieHeader = new HashMap<String, List<String>>();
    private ChxAccount account;
    private static final long serialVersionUID = 24269464L;
    private static final String USERAGENT = "Mozilla/5.0 (iPhone; CPU iPhone OS 13_3_1 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Mobile/15E148 ChaoXingStudy/ChaoXingStudy_3_4.3.2_ios_phone_201911291130_27 (@Kalimdor)_17365069475625398395";
    private static final String BOUNDARY = "d41d8cd98f00b204e9800998ecf8427e";
    private static final String BROWSER_UA = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36";
    private static final String VIDEO_REFERER = "https://mooc1.chaoxing.com/ananas/modules/video/index.html?v=2025-0725-1842";
    private transient HashMap<String, JsonObject> mArgCache = new HashMap<String, JsonObject>();
    private transient HashMap<String, Long> mArgCacheTime = new HashMap<String, Long>();

    public ChxUtility() {
        CookieHandler.setDefault(new CookieManager(null, CookiePolicy.ACCEPT_ALL));
        this.client = HttpClient.newBuilder().cookieHandler(CookieHandler.getDefault()).proxy(ProxySelector.of(null)).connectTimeout(Duration.ofSeconds(3L)).build();
    }

    private static String md5(String context) {
        try {
            MessageDigest m = MessageDigest.getInstance("MD5");
            m.update(context.getBytes());
            byte[] md5bytes = m.digest();
            StringBuffer buf = new StringBuffer("");
            int offset = 0;
            while (offset < md5bytes.length) {
                int i = md5bytes[offset];
                if (i < 0) {
                    i += 256;
                }
                if (i < 16) {
                    buf.append("0");
                }
                buf.append(Integer.toHexString(i));
                ++offset;
            }
            return buf.toString();
        }
        catch (NoSuchAlgorithmException e) {
            return "";
        }
    }

    private static String getInfEnc(long mTime, String mToken) {
        String mEncryptStr = "token=" + mToken + "&_time=" + mTime + "&DESKey=Z(AfY@XS";
        return ChxUtility.md5(mEncryptStr);
    }

    private static String getEnc(ChxResource resource, int playTime) {
        String clazzId = resource.course.clazzid;
        String userId = resource.account.userid;
        String jobId = resource.jobid;
        String objectId = resource.objectid;
        String durSec = resource.duration;
        String playMs = String.valueOf(playTime * 1000);
        String durMs = String.valueOf(Integer.parseInt(resource.duration) * 1000);
        String encText = "[" + clazzId + "][" + userId + "][" + jobId + "][" + objectId + "][" + playMs + "][d_yHJ!$pdA~5][" + durMs + "][0_" + durSec + "]";
        return ChxUtility.md5(encText);
    }

    public ChxAccount login(String username, String password) throws IOException, InterruptedException {
        ChxAccount account = new ChxAccount();
        account.username = username;
        account.password = password;
        long mTime = new Date().getTime();
        String mToken = "4faa8662c59590c6f43ae9fe5b002b42";
        String mInfEnc = ChxUtility.getInfEnc(mTime, mToken);
        String postText = ("--{bound}\r\nContent-Disposition: form-data; name=\"uname\"\r\nContent-Type: text/plain; charset=UTF-8\r\nContent-Transfer-Encoding: 8bit\r\n\r\n" + username + "\r\n" + "--{bound}\r\n" + "Content-Disposition: form-data; name=\"code\"\r\n" + "Content-Type: text/plain; charset=UTF-8\r\n" + "Content-Transfer-Encoding: 8bit\r\n" + "\r\n" + password + "\r\n" + "--{bound}\r\n" + "Content-Disposition: form-data; name=\"loginType\"\r\n" + "Content-Type: text/plain; charset=UTF-8\r\n" + "Content-Transfer-Encoding: 8bit\r\n" + "\r\n" + "1\r\n" + "--{bound}\r\n" + "Content-Disposition: form-data; name=\"roleSelect\"\r\n" + "Content-Type: text/plain; charset=UTF-8\r\n" + "Content-Transfer-Encoding: 8bit\r\n" + "\r\n" + "true\r\n" + "--{bound}--").replaceAll("\\{bound\\}", BOUNDARY);
        HttpRequest request = HttpRequest.newBuilder().POST(HttpRequest.BodyPublishers.ofString(postText)).uri(URI.create("https://passport2.chaoxing.com/xxt/loginregisternew?token=" + mToken + "&_time=" + mTime + "&inf_enc=" + mInfEnc)).setHeader("Accept-Encoding", "plain").setHeader("Content-Type", "multipart/form-data; boundary=d41d8cd98f00b204e9800998ecf8427e").setHeader("Accept-Language", "zh-CN,en-US;q=0.8").setHeader("User-Agent", USERAGENT).build();
        HttpResponse<String> response = this.client.send(request, HttpResponse.BodyHandlers.ofString());
        JsonObject object = ChxUtility.parseObject(response);
        ChxUtility.log("登录接口响应: HTTP " + response.statusCode() + " " + ChxUtility.shortBody(response));
        JsonElement statusElement = object.get("status");
        boolean isLogin = statusElement != null && statusElement.isJsonPrimitive() && statusElement.getAsBoolean();
        if (isLogin) {
            this.account = account;
            this.fillMissingAccountInfo(object, account);
            try {
                ChxAccount loaded = this.loadAccountInfo(account);
                if (loaded != null) {
                    account = loaded;
                }
            }
            catch (IOException e) {
                ChxUtility.log("登录成功，但账号信息接口不可用（已忽略并继续）：" + e);
            }
            this.account = account;
            return account;
        }
        return null;
    }

    static final String logPath = System.getProperty("user.home") + File.separator + "chx-debug.log";

    static void log(String message) {
        String line = "[chx " + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "] " + message;
        System.err.println(line);
        try {
            OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(logPath, true), java.nio.charset.StandardCharsets.UTF_8);
            writer.write(line + System.lineSeparator());
            writer.close();
        }
        catch (IOException iOException) {
            // ignore
        }
    }

    private static String shortBody(HttpResponse<String> response) {
        String resBody = response.body();
        if (resBody == null) {
            return "(空)";
        }
        String head = resBody.replaceAll("\\s+", " ").trim();
        if (head.length() > 200) {
            head = head.substring(0, 200) + "...";
        }
        return head;
    }

    private static JsonObject parseObject(HttpResponse<String> response) throws IOException {
        String resBody = response.body();
        try {
            return JsonParser.parseString(resBody).getAsJsonObject();
        }
        catch (RuntimeException e) {
            throw new IOException("服务器返回的不是 JSON (HTTP " + response.statusCode() + " " + response.uri() + ")：" + ChxUtility.shortBody(response), e);
        }
    }

    private static void logDump(String tag, HttpResponse<String> response) {
        String text = response.body() == null ? "(空)" : response.body();
        ChxUtility.log(tag + " -> HTTP " + response.statusCode() + " 长度=" + text.length());
        ChxUtility.log(tag + " 原文: " + (text.length() > 1500 ? text.substring(0, 1500) + " ...[截断,共" + text.length() + " 字]" : text));
    }

    private static void dumpToFile(String fileName, String text) {
        if (text == null || text.isEmpty()) {
            return;
        }
        try {
            FileOutputStream fos = new FileOutputStream(new File(System.getProperty("user.home"), fileName));
            fos.write(text.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            fos.close();
        }
        catch (IOException iOException) {
            // ignore
        }
    }

    private static String extractObjectAfter(String text, String marker) {
        int start = text.indexOf(marker);
        if (start < 0) {
            return null;
        }
        start = text.indexOf(123, start);
        if (start < 0) {
            return null;
        }
        int depth = 0;
        boolean inString = false;
        boolean escaped = false;
        int i = start;
        while (i < text.length()) {
            char c = text.charAt(i);
            if (inString) {
                if (escaped) {
                    escaped = false;
                }
                else if (c == '\\') {
                    escaped = true;
                }
                else if (c == '"') {
                    inString = false;
                }
            }
            else if (c == '"') {
                inString = true;
            }
            else if (c == '{') {
                ++depth;
            }
            else if (c == '}') {
                --depth;
                if (depth == 0) {
                    return text.substring(start, i + 1);
                }
            }
            ++i;
        }
        return null;
    }

    private JsonObject loadCardData(ChxSection section) {
        JsonObject cached;
        if (section == null || section.course == null || section.nodeid == null) {
            return null;
        }
        if ((cached = this.mArgCache.get(section.nodeid)) != null) {
            Long time = this.mArgCacheTime.get(section.nodeid);
            if (time != null && System.currentTimeMillis() - time < 300000L) {
                return cached;
            }
        }
        try {
            String entry = "https://mooc1.chaoxing.com/mooc-ans/mycourse/studentstudy?chapterId=" + section.nodeid + "&courseId=" + section.course.courseid + "&clazzid=" + section.course.clazzid + "&cpi=" + section.course.urlcpi;
            HttpRequest entryRequest = HttpRequest.newBuilder().GET().uri(URI.create(entry)).setHeader("Accept-Encoding", "plain").setHeader("Accept-Language", "zh-CN,en-US;q=0.8").setHeader("User-Agent", BROWSER_UA).build();
            this.client.send(entryRequest, HttpResponse.BodyHandlers.ofString());
            String cardsUrl = "https://mooc1.chaoxing.com/mooc-ans/knowledge/cards?clazzid=" + section.course.clazzid + "&courseid=" + section.course.courseid + "&knowledgeid=" + section.nodeid + "&ut=s&cpi=" + section.course.urlcpi + "&v=2025-0424-1038-3&mooc2=1&num=0";
            HttpRequest cardsRequest = HttpRequest.newBuilder().GET().uri(URI.create(cardsUrl)).setHeader("Accept-Encoding", "plain").setHeader("Accept-Language", "zh-CN,en-US;q=0.8").setHeader("User-Agent", BROWSER_UA).setHeader("Referer", entry).build();
            HttpResponse<String> response = this.client.send(cardsRequest, HttpResponse.BodyHandlers.ofString());
            String html = response.body() == null ? "" : response.body();
            ChxUtility.log("mooc-ans/knowledge/cards -> HTTP " + response.statusCode() + " 长度=" + html.length());
            String mArg = ChxUtility.extractObjectAfter(html, "mArg = {");
            if (mArg == null) {
                mArg = ChxUtility.extractObjectAfter(html, "mArg={");
            }
            if (mArg == null) {
                ChxUtility.log("任务点页面里没有 mArg，拿不到上报 token（只能退回旧参数）");
                return null;
            }
            JsonObject object = JsonParser.parseString(mArg).getAsJsonObject();
            this.mArgCache.put(section.nodeid, object);
            this.mArgCacheTime.put(section.nodeid, System.currentTimeMillis());
            return object;
        }
        catch (IOException | InterruptedException e) {
            ChxUtility.log("拉取任务点页面失败：" + e);
            return null;
        }
        catch (RuntimeException e) {
            ChxUtility.log("解析任务点页面失败：" + e);
            return null;
        }
    }

    private void fillTokensFromCards(ChxResource resource) {
        JsonObject object = this.loadCardData(resource.section);
        if (object == null) {
            return;
        }
        try {
            JsonObject defaults;
            if (object.has("defaults") && object.get("defaults").isJsonObject() && resource.account != null) {
                defaults = object.getAsJsonObject("defaults");
                if (defaults.has("userid")) {
                    String pageUid = defaults.get("userid").getAsString();
                    if (pageUid != null && !pageUid.isEmpty() && !pageUid.equals(resource.account.userid)) {
                        ChxUtility.log("按任务点页面修正 userid: " + resource.account.userid + " -> " + pageUid);
                        resource.account.userid = pageUid;
                    }
                }
                if (defaults.has("fid")) {
                    resource.account.schoolid = defaults.get("fid").getAsString();
                }
            }
            JsonElement attachmentsElement = object.get("attachments");
            if (attachmentsElement == null || !attachmentsElement.isJsonArray()) {
                return;
            }
            for (JsonElement element : attachmentsElement.getAsJsonArray()) {
                String objectId;
                if (element == null || !element.isJsonObject()) continue;
                JsonObject attachment = element.getAsJsonObject();
                objectId = attachment.has("objectId") ? attachment.get("objectId").getAsString() : (attachment.has("objectid") ? attachment.get("objectid").getAsString() : null);
                if (objectId == null || !objectId.equals(resource.objectid)) continue;
                if (attachment.has("otherInfo")) {
                    resource.otherInfo = attachment.get("otherInfo").getAsString();
                }
                if (attachment.has("attDurationEnc")) {
                    resource.attDurationEnc = attachment.get("attDurationEnc").getAsString();
                }
                if (attachment.has("videoFaceCaptureEnc")) {
                    resource.videoFaceCaptureEnc = attachment.get("videoFaceCaptureEnc").getAsString();
                }
                if (resource.otherInfo != null) {
                    if (resource.otherInfo.contains("-rt_d")) {
                        resource.rt = "0.9";
                    }
                    else if (resource.otherInfo.contains("-rt_1")) {
                        resource.rt = "1";
                    }
                }
                ChxUtility.log("已取到上报 token: objectId=" + objectId + " otherInfo长度=" + (resource.otherInfo == null ? 0 : resource.otherInfo.length()) + " attDurationEnc=" + (resource.attDurationEnc != null) + " faceEnc=" + (resource.videoFaceCaptureEnc != null) + " rt=" + resource.rt);
                return;
            }
            ChxUtility.log("任务点页面里没有 objectId=" + resource.objectid + " 的附件项");
        }
        catch (RuntimeException e) {
            ChxUtility.log("读取上报 token 失败：" + e);
        }
    }

    private static String cookieValue(String name) {
        try {
            CookieHandler handler = CookieHandler.getDefault();
            if (handler == null) {
                return null;
            }
            Map<String, List<String>> headers = handler.get(URI.create("https://passport2.chaoxing.com/"), new HashMap<String, List<String>>());
            List<String> cookies = headers.get("Cookie");
            if (cookies == null) {
                return null;
            }
            for (String cookie : cookies) {
                for (String item : cookie.split(";")) {
                    String[] pair = item.trim().split("=", 2);
                    if (pair.length == 2 && pair[0].equalsIgnoreCase(name)) {
                        return pair[1];
                    }
                }
            }
        }
        catch (IOException iOException) {
            // ignore
        }
        return null;
    }

    private static String deepFind(JsonElement element, String... keys) {
        if (element == null || element.isJsonNull()) {
            return null;
        }
        if (element.isJsonObject()) {
            JsonObject object = element.getAsJsonObject();
            for (String key : keys) {
                JsonElement value = object.get(key);
                if (value != null && value.isJsonPrimitive()) {
                    return value.getAsString();
                }
            }
            for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
                String found = ChxUtility.deepFind(entry.getValue(), keys);
                if (found != null) {
                    return found;
                }
            }
        }
        else if (element.isJsonArray()) {
            for (JsonElement child : element.getAsJsonArray()) {
                String found = ChxUtility.deepFind(child, keys);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private void fillMissingAccountInfo(JsonElement source, ChxAccount account) {
        if (account.userid == null || account.userid.isEmpty()) {
            String uid = ChxUtility.cookieValue("_uid");
            if (uid == null || uid.isEmpty()) {
                uid = ChxUtility.cookieValue("UID");
            }
            if (uid != null && !uid.isEmpty()) {
                account.userid = uid;
                ChxUtility.log("已从 Cookie 推断 userid=" + uid);
            }
        }
        if (account.schoolid == null || account.schoolid.isEmpty()) {
            String fid = ChxUtility.cookieValue("fid");
            if (fid != null && !fid.isEmpty()) {
                account.schoolid = fid;
                ChxUtility.log("已从 Cookie 推断 schoolid=" + fid);
            }
        }
        if ((account.userid == null || account.userid.isEmpty()) && source != null) {
            String uid = ChxUtility.deepFind(source, "uid", "userid", "userId", "puid");
            if (uid != null && !uid.isEmpty()) {
                account.userid = uid;
                ChxUtility.log("已从接口响应推断 userid=" + uid);
            }
        }
        if ((account.schoolid == null || account.schoolid.isEmpty()) && source != null) {
            String schoolid = ChxUtility.deepFind(source, "schoolid", "schoolId", "fid");
            if (schoolid != null && !schoolid.isEmpty()) {
                account.schoolid = schoolid;
                ChxUtility.log("已从接口响应推断 schoolid=" + schoolid);
            }
        }
        if (account.realname == null || account.realname.isEmpty()) {
            if (source != null) {
                String realname = ChxUtility.deepFind(source, "realname", "realName", "name");
                if (realname != null && !realname.isEmpty()) {
                    account.realname = realname;
                }
            }
        }
    }

    private ChxAccount loadAccountInfo(ChxAccount account) throws IOException, InterruptedException {
        String[] candidates = new String[]{"https://passport2.chaoxing.com/passport2islogin", "https://passport2-api.chaoxing.com/passport2islogin", "https://mooc1-api.chaoxing.com/passport2islogin", "https://sso.chaoxing.com/apis/login/userLogin4Uname.do"};
        for (String url : candidates) {
            try {
                HttpRequest request = HttpRequest.newBuilder().GET().uri(URI.create(url)).setHeader("Accept-Encoding", "plain").setHeader("Accept-Language", "zh-CN,en-US;q=0.8").setHeader("User-Agent", USERAGENT).build();
                HttpResponse<String> response = this.client.send(request, HttpResponse.BodyHandlers.ofString());
                JsonObject object;
                try {
                    object = ChxUtility.parseObject(response);
                }
                catch (IOException e) {
                    ChxUtility.log("账号信息接口不可用: " + url + " -> HTTP " + response.statusCode() + " " + ChxUtility.shortBody(response));
                    continue;
                }
                JsonElement login = object.get("login");
                if (login == null || !login.isJsonPrimitive()) {
                    JsonElement msg = object.get("msg");
                    if (msg != null && msg.isJsonObject()) {
                        JsonObject info = msg.getAsJsonObject();
                        if (account.userid == null || account.userid.isEmpty()) {
                            if (info.has("uid")) {
                                account.userid = info.get("uid").getAsString();
                            }
                            else if (info.has("puid")) {
                                account.userid = info.get("puid").getAsString();
                            }
                        }
                        if (info.has("fid")) {
                            account.schoolid = info.get("fid").getAsString();
                        }
                        if (info.has("name")) {
                            account.realname = info.get("name").getAsString();
                        }
                        if (info.has("realname")) {
                            account.realname = info.get("realname").getAsString();
                        }
                        ChxUtility.log("账号信息（sso msg）加载成功: " + url + " -> " + account);
                        return account;
                    }
                    ChxUtility.log("账号信息接口字段不符: " + url + " -> " + ChxUtility.shortBody(response));
                    continue;
                }
                if (!login.getAsBoolean()) {
                    ChxUtility.log("账号信息接口报告未登录: " + url);
                    return null;
                }
                if (object.has("realname")) {
                    account.realname = object.get("realname").getAsString();
                }
                if (object.has("uid")) {
                    account.userid = object.get("uid").getAsString();
                }
                if (object.has("schoolid")) {
                    account.schoolid = object.get("schoolid").getAsString();
                }
                ChxUtility.log("账号信息加载成功: " + url + " -> " + account);
                return account;
            }
            catch (IOException e) {
                ChxUtility.log("账号信息接口请求失败: " + url + " -> " + e);
            }
        }
        throw new IOException("无法获取账号信息：所有候选接口均不可用（详见 " + ChxUtility.logPath + "）");
    }

    public boolean checkLogin() throws IOException, InterruptedException {
        try {
            return this.loadAccountInfo(this.account) != null;
        }
        catch (IOException e) {
            ChxUtility.log("会话检查失败（账号信息接口不可用），改为使用保存的账号重新登录：" + e);
            return false;
        }
    }

    public ChxAccount reLogin() throws IOException, InterruptedException {
        return this.reLogin(this.account);
    }

    public ChxAccount reLogin(ChxAccount account) throws IOException, InterruptedException {
        return this.login(account.username, account.password);
    }

    public ChxAccount getAccount() {
        return this.account;
    }

    public ChxCourse[] getCourses() throws IOException, InterruptedException {
        return this.getCourses(this.account);
    }

    public ChxCourse[] getCourses(ChxAccount account) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder().GET().uri(URI.create("https://mooc1-api.chaoxing.com/mycourse?rss=1&mcode=")).setHeader("Accept-Encoding", "plain").setHeader("Accept-Language", "zh-CN,en-US;q=0.8").setHeader("User-Agent", USERAGENT).build();
        HttpResponse<String> response = this.client.send(request, HttpResponse.BodyHandlers.ofString());
        ChxUtility.logDump("mycourse", response);
        JsonObject object = ChxUtility.parseObject(response);
        this.fillMissingAccountInfo(object, account);
        JsonElement channelElement = object.get("channelList");
        if (channelElement == null || !channelElement.isJsonArray()) {
            throw new IOException("课程列表接口返回异常：" + ChxUtility.shortBody(response));
        }
        JsonArray channelList = channelElement.getAsJsonArray();
        int channelLen = channelList.size();
        ChxCourse[] courseList = new ChxCourse[channelLen];
        int i = 0;
        while (i < channelLen) {
            ChxCourse course = new ChxCourse(account);
            JsonObject channel = channelList.get(i).getAsJsonObject();
            try {
                JsonObject curCourse = channel.get("content").getAsJsonObject().get("course").getAsJsonObject();
                course.name = curCourse.get("data").getAsJsonArray().get(0).getAsJsonObject().get("name").getAsString();
                course.teacher = curCourse.get("data").getAsJsonArray().get(0).getAsJsonObject().get("teacherfactor").getAsString();
                course.courseid = curCourse.get("data").getAsJsonArray().get(0).getAsJsonObject().get("id").getAsString();
                course.urlcpi = channel.get("cpi").getAsString();
                course.clazzid = channel.get("content").getAsJsonObject().get("id").getAsString();
                course.supported = true;
            }
            catch (IndexOutOfBoundsException | NullPointerException e) {
                course.supported = false;
            }
            courseList[i] = course;
            ++i;
        }
        return courseList;
    }

    public ChxSection[] getSections(ChxCourse course) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder().GET().uri(URI.create("https://mooc1-api.chaoxing.com/gas/clazz?id=" + course.clazzid + "&fields=id,bbsid,classscore,allowdownload,isstart,chatid,name,state,isthirdaq,information,discuss,visiblescore,begindate,course.fields(id,infocontent,name,objectid,classscore,bulletformat,imageurl,privately,teacherfactor,unfinishedJobcount,jobcount,state,knowledge.fields(id,name,indexOrder,parentnodeid,status,layer,label,begintime,attachment.fields(id,type,objectid,extension,name).type(video)))&view=json")).setHeader("Accept-Encoding", "plain").setHeader("Accept-Language", "zh-CN,en-US;q=0.8").setHeader("User-Agent", USERAGENT).build();
        HttpResponse<String> response = this.client.send(request, HttpResponse.BodyHandlers.ofString());
        String resBody = response.body();
        JsonElement element = JsonParser.parseString(resBody);
        JsonObject object = element.getAsJsonObject();
        JsonArray knowledgeArray = object.get("data").getAsJsonArray().get(0).getAsJsonObject().get("course").getAsJsonObject().get("data").getAsJsonArray().get(0).getAsJsonObject().get("knowledge").getAsJsonObject().get("data").getAsJsonArray();
        ArrayList<ChxSection> arr = new ArrayList<ChxSection>();
        int knowledgeLen = knowledgeArray.size();
        int primaryorder = 1;
        while (true) {
            int parentnodeid = -1;
            int i = 0;
            while (i < knowledgeLen) {
                JsonObject knowledgeJson = knowledgeArray.get(i).getAsJsonObject();
                if (knowledgeJson.get("parentnodeid").getAsInt() == 0 && knowledgeJson.get("indexorder").getAsInt() == primaryorder) {
                    parentnodeid = knowledgeJson.get("id").getAsInt();
                    break;
                }
                ++i;
            }
            if (parentnodeid == -1) break;
            int secondaryorder = 1;
            while (true) {
                ChxSection section = null;
                int j = 0;
                while (j < knowledgeLen) {
                    JsonObject knowledgeJson = knowledgeArray.get(j).getAsJsonObject();
                    if (knowledgeJson.get("parentnodeid").getAsInt() == parentnodeid && knowledgeJson.get("indexorder").getAsInt() == secondaryorder) {
                        section = new ChxSection(course);
                        section.label = knowledgeJson.get("label").getAsString();
                        section.name = knowledgeJson.get("name").getAsString();
                        section.nodeid = String.valueOf(knowledgeJson.get("id").getAsInt());
                        section.supported = true;
                        break;
                    }
                    ++j;
                }
                if (section == null) break;
                arr.add(section);
                ++secondaryorder;
            }
            ++primaryorder;
        }
        ChxSection[] sections = arr.toArray(new ChxSection[arr.size()]);
        return sections;
    }

    public ChxResource[] getResources(ChxSection section) throws IOException, InterruptedException {
        String description;
        HttpRequest request = HttpRequest.newBuilder().GET().uri(URI.create("https://mooc1-api.chaoxing.com/gas/knowledge?id=" + section.nodeid + "&courseid=" + section.course.courseid + "&fields=begintime,clickcount,createtime,description,indexorder,jobUnfinishedCount,jobcount,jobfinishcount,label,lastmodifytime,layer,listPosition,name,openlock,parentnodeid,status,id,card.fields(cardIndex,cardorder,description,knowledgeTitile,knowledgeid,theme,title,id).contentcard(all)&view=json")).setHeader("Accept-Encoding", "plain").setHeader("Accept-Language", "zh-CN,en-US;q=0.8").setHeader("User-Agent", USERAGENT).build();
        HttpResponse<String> response = this.client.send(request, HttpResponse.BodyHandlers.ofString());
        ChxUtility.logDump("gas/knowledge", response);
        JsonObject object = ChxUtility.parseObject(response);
        try {
            description = object.get("data").getAsJsonArray().get(0).getAsJsonObject().get("card").getAsJsonObject().get("data").getAsJsonArray().get(0).getAsJsonObject().get("description").getAsString();
        }
        catch (IndexOutOfBoundsException | NullPointerException e) {
            ChxUtility.log("gas/knowledge 结构不符合预期（取不到 card.description），顶层字段=" + object.keySet() + "，异常=" + e);
            return new ChxResource[0];
        }
        ChxUtility.log("课时 description 前 1200 字：" + (description.length() > 1200 ? description.substring(0, 1200) : description));

        ArrayList<String> dataStrings = new ArrayList<String>();
        NodeList nlist = null;
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            String xml = "<body>" + description.replace("&nbsp", " ") + "</body>";
            Document document = builder.parse(new InputSource(new StringReader(xml)));
            document.getDocumentElement().normalize();
            nlist = document.getElementsByTagName("iframe");
            int i = 0;
            while (i < nlist.getLength()) {
                dataStrings.add(((Element)nlist.item(i)).getAttribute("data"));
                ++i;
            }
            ChxUtility.log("按 XML 解析出的 iframe 数量=" + nlist.getLength());
        }
        catch (ParserConfigurationException | SAXException e) {
            ChxUtility.log("description 不是合法 XML，改用正则提取：" + e);
        }
        if (dataStrings.isEmpty()) {
            Matcher dataMatcher = Pattern.compile("data\\s*=\\s*(['\"])(.*?)\\1", 32).matcher(description);
            while (dataMatcher.find()) {
                String candidate = dataMatcher.group(2);
                if (candidate.indexOf("objectid") >= 0 || candidate.indexOf("objectId") >= 0 || candidate.indexOf("jobid") >= 0 || candidate.indexOf("jobId") >= 0) {
                    dataStrings.add(candidate);
                }
            }
            ChxUtility.log("正则提取到的 data 属性数量=" + dataStrings.size());
        }
        if (dataStrings.isEmpty()) {
            Matcher jsonMatcher = Pattern.compile("\\{[^{}]*\"(?:objectid|objectId|jobid|jobId)\"[^{}]*\\}").matcher(description);
            while (jsonMatcher.find()) {
                dataStrings.add(jsonMatcher.group());
            }
            ChxUtility.log("正则提取到的 JSON 片段数量=" + dataStrings.size());
        }
        if (dataStrings.isEmpty()) {
            ChxUtility.log("没有提取到任何资料，description 全文（最多 4000 字）：" + (description.length() > 4000 ? description.substring(0, 4000) + " ...[截断]" : description));
        }

        ArrayList<ChxResource> arr = new ArrayList<ChxResource>();
        int i = 0;
        while (i < dataStrings.size()) {
            String dataStr = dataStrings.get(i).replace("&quot;", "\"").replace("&#34;", "\"").replace("&apos;", "'").replace("&#39;", "'").replace("&amp;", "&");
            ChxUtility.log("资料候选[" + i + "] data=" + (dataStr.length() > 400 ? dataStr.substring(0, 400) : dataStr));
            JsonObject dataJson;
            try {
                dataJson = JsonParser.parseString(dataStr).getAsJsonObject();
            }
            catch (RuntimeException e) {
                ChxUtility.log("资料候选[" + i + "] 不是合法 JSON，已跳过：" + e);
                ++i;
                continue;
            }
            ChxResource resource = new ChxResource(section);
            if (dataJson.has("name")) {
                resource.name = dataJson.get("name").getAsString();
            }
            if (dataJson.has("jobid")) {
                resource.jobid = dataJson.get("jobid").getAsString();
            }
            if (dataJson.has("jobId")) {
                resource.jobid = dataJson.get("jobId").getAsString();
            }
            if (dataJson.has("objectid")) {
                resource.objectid = dataJson.get("objectid").getAsString();
            }
            if (dataJson.has("objectId")) {
                resource.objectid = dataJson.get("objectId").getAsString();
            }
            resource.supported = dataJson.has("name") && resource.jobid != null && resource.objectid != null;
            ChxUtility.log("资料[" + i + "] name=" + resource.name + " jobid=" + resource.jobid + " objectid=" + resource.objectid + " supported=" + resource.supported);
            arr.add(resource);
            ++i;
        }
        return arr.toArray(new ChxResource[arr.size()]);
    }

    public ChxResource loadResourceInfo(ChxResource resource) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder().GET().uri(URI.create("https://mooc1-api.chaoxing.com/ananas/status/" + resource.objectid + "?k=" + resource.account.schoolid + "&flag=normal&_dc=" + new Date().getTime())).setHeader("Accept-Encoding", "plain").setHeader("Accept-Language", "zh-CN,en-US;q=0.8").setHeader("User-Agent", USERAGENT).build();
        HttpResponse<String> response = this.client.send(request, HttpResponse.BodyHandlers.ofString());
        ChxUtility.logDump("ananas/status", response);
        JsonObject dataJson = ChxUtility.parseObject(response);
        if (dataJson.has("dtoken")) {
            resource.dtoken = dataJson.get("dtoken").getAsString();
        }
        if (dataJson.has("duration")) {
            resource.duration = dataJson.get("duration").getAsString();
        }
        if (dataJson.has("download")) {
            resource.downloadurl = dataJson.get("download").getAsString();
        }
        request = HttpRequest.newBuilder().GET().uri(URI.create("https://mooc1-api.chaoxing.com/knowledge/cards?clazzid=" + resource.course.clazzid + "&courseid=" + resource.course.courseid + "&knowledgeid=" + resource.section.nodeid + "&num=" + (resource.dtoken == null || resource.dtoken.isEmpty() ? "0" : "2") + "&isPhone=1&control=true")).setHeader("Accept-Encoding", "plain").setHeader("Accept-Language", "zh-CN,en-US;q=0.8").setHeader("User-Agent", USERAGENT).build();
        response = this.client.send(request, HttpResponse.BodyHandlers.ofString());
        ChxUtility.logDump("knowledge/cards", response);
        String resBody = response.body();
        ChxUtility.dumpToFile("chx-debug-cards.html", resBody);
        try {
            Matcher m = Pattern.compile("window.AttachmentSetting *=(.*?);\\n", 8).matcher(resBody);
            if (m.find()) {
                JsonObject object = JsonParser.parseString(m.group(1)).getAsJsonObject();
                ChxUtility.log("AttachmentSetting 顶层字段=" + object.keySet());
                JsonElement attachmentsElement = object.get("attachments");
                if (attachmentsElement != null && attachmentsElement.isJsonArray()) {
                    JsonArray attachments = attachmentsElement.getAsJsonArray();
                    int attachmentsLen = attachments.size();
                    int i = 0;
                    while (i < attachmentsLen) {
                        JsonElement attachmentElement = attachments.get(i);
                        if (attachmentElement != null && attachmentElement.isJsonObject()) {
                            JsonObject attachment = attachmentElement.getAsJsonObject();
                            if (attachment.has("jtoken") && attachment.has("jobid") && attachment.get("jobid").getAsString().equals(resource.jobid)) {
                                resource.jtoken = attachment.get("jtoken").getAsString();
                            }
                        }
                        ++i;
                    }
                }
                else {
                    ChxUtility.log("AttachmentSetting 里没有 attachments 数组（页面结构变了，跳过 jtoken 提取，视频挂机不受影响）");
                }
            }
            else {
                ChxUtility.log("knowledge/cards 里没有 window.AttachmentSetting（页面结构变了，跳过 jtoken 提取，视频挂机不受影响）");
            }
        }
        catch (RuntimeException runtimeException) {
            ChxUtility.log("解析 AttachmentSetting 时出错（已忽略，视频挂机不受影响）：" + runtimeException);
        }
        this.fillTokensFromCards(resource);
        ChxUtility.log("loadResourceInfo 结束：dtoken=" + resource.dtoken + " jtoken=" + resource.jtoken + " duration=" + resource.duration + " download=" + resource.downloadurl);
        return resource;
    }

    public void enterSection(ChxSection section) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder().GET().uri(URI.create("https://mooc1-api.chaoxing.com/job/submitstudy?node=" + section.nodeid + "&userid=" + section.account.userid + "&clazzid=" + section.course.clazzid + "&courseid=" + section.course.courseid + "&view=json")).setHeader("Accept-Encoding", "plain").setHeader("Accept-Language", "zh-CN,en-US;q=0.8").setHeader("User-Agent", USERAGENT).build();
        this.client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    public boolean playResource(ChxResource resource, int playTime) throws IOException, InterruptedException {
        if ((resource.dtoken == null || resource.dtoken.isEmpty()) && (resource.jtoken == null || resource.jtoken.isEmpty())) {
            this.loadResourceInfo(resource);
        }
        if (resource.duration != null && !resource.duration.isEmpty()) {
            if (resource.otherInfo == null) {
                this.fillTokensFromCards(resource);
            }
            String chxEnc = ChxUtility.getEnc(resource, playTime);
            String otherInfo = resource.otherInfo != null ? resource.otherInfo : "nodeId_" + resource.section.nodeid;
            String baseUrl = "https://mooc1.chaoxing.com/mooc-ans/multimedia/log/a/" + resource.course.urlcpi + "/" + resource.dtoken + "?clazzId=" + resource.course.clazzid + "&playingTime=" + playTime + "&duration=" + resource.duration + "&clipTime=0_" + resource.duration + "&objectId=" + resource.objectid + "&otherInfo=" + otherInfo + "&courseId=" + resource.course.courseid + "&jobid=" + resource.jobid + "&userid=" + resource.account.userid + "&isdrag=3&view=pc&enc=" + chxEnc + "&dtype=Video";
            if (resource.attDurationEnc != null) {
                baseUrl = baseUrl + "&attDuration=" + resource.duration + "&attDurationEnc=" + resource.attDurationEnc;
            }
            if (resource.videoFaceCaptureEnc != null) {
                baseUrl = baseUrl + "&videoFaceCaptureEnc=" + resource.videoFaceCaptureEnc;
            }
            String[] rtList = resource.rt != null && resource.rt.equals("1") ? new String[]{"1", "0.9"} : new String[]{"0.9", "1"};
            HttpResponse<String> response = null;
            String resBody = "";
            int i = 0;
            while (i < rtList.length) {
                String url = baseUrl + "&rt=" + rtList[i] + "&_t=" + new Date().getTime();
                HttpRequest request = HttpRequest.newBuilder().GET().uri(URI.create(url)).setHeader("Accept-Encoding", "plain").setHeader("Accept-Language", "zh-CN,en-US;q=0.8").setHeader("User-Agent", BROWSER_UA).setHeader("Referer", VIDEO_REFERER).build();
                response = this.client.send(request, HttpResponse.BodyHandlers.ofString());
                resBody = response.body() == null ? "" : response.body();
                if (response.statusCode() == 403) {
                    ChxUtility.log("multimedia/log 403（rt=" + rtList[i] + "），换一个 rt 重试");
                    ++i;
                    continue;
                }
                break;
            }
            try {
                JsonElement element = JsonParser.parseString(resBody);
                JsonObject dataJson = element.getAsJsonObject();
                JsonElement isPassed = dataJson.get("isPassed");
                if (isPassed == null || !isPassed.isJsonPrimitive()) {
                    ChxUtility.log("multimedia/log 返回里没有 isPassed：HTTP " + (response == null ? -1 : response.statusCode()) + " " + (resBody.length() > 300 ? resBody.substring(0, 300) : resBody));
                    return false;
                }
                return isPassed.getAsBoolean();
            }
            catch (Exception e) {
                ChxUtility.log("multimedia/log 解析失败：" + e + " 原文：" + (resBody.length() > 300 ? resBody.substring(0, 300) : resBody));
                return false;
            }
        }
        HttpRequest request = HttpRequest.newBuilder().GET().uri(URI.create("https://mooc1-api.chaoxing.com/ananas/job/document?jobid=" + resource.jobid + "&knowledgeid=" + resource.section.nodeid + "&courseid=" + resource.course.courseid + "&clazzid=" + resource.course.clazzid + "&jtoken=" + resource.jtoken + "&_dc=" + new Date().getTime())).setHeader("Accept-Encoding", "plain").setHeader("Accept-Language", "zh-CN,en-US;q=0.8").setHeader("User-Agent", USERAGENT).build();
        HttpResponse<String> response = this.client.send(request, HttpResponse.BodyHandlers.ofString());
        String resBody = response.body();
        JsonElement element = JsonParser.parseString(resBody);
        JsonObject dataJson = element.getAsJsonObject();
        return dataJson.get("status").getAsBoolean();
    }

    public boolean saveTo(String path) {
        try {
            CookieHandler cookie = CookieHandler.getDefault();
            this.cookieHeader = cookie.get(URI.create("http://chaoxing.com/"), this.cookieHeader);
            FileOutputStream fos = new FileOutputStream(path);
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(this);
            oos.close();
            return true;
        }
        catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean loadFrom(String path) {
        try {
            FileInputStream fis = new FileInputStream(path);
            ObjectInputStream ois = new ObjectInputStream(fis);
            ChxUtility loaded = (ChxUtility)ois.readObject();
            ois.close();
            CookieHandler cookie = CookieHandler.getDefault();
            this.cookieHeader.put("Set-Cookie", loaded.cookieHeader.get("Cookie"));
            cookie.put(URI.create("http://chaoxing.com/"), this.cookieHeader);
            this.cookieHeader.remove("Set-Cookie");
            this.chxData = loaded.chxData;
            this.account = loaded.account;
            return true;
        }
        catch (IOException | ClassNotFoundException e) {
            return false;
        }
    }
}
