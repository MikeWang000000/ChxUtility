package net.vomega.chx;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
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

import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class ChxUtility implements Serializable {
    private transient HttpClient client;
    private HashMap<String, String> chxData = new HashMap<>();
    private Map<String, List<String>> cookieHeader = new HashMap<>();
    private ChxAccount account;

    private static final long serialVersionUID = 24269464L;
    private static final String USERAGENT = "Mozilla/5.0 (iPhone; CPU iPhone OS 13_3_1 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Mobile/15E148 ChaoXingStudy/ChaoXingStudy_3_4.3.2_ios_phone_201911291130_27 (@Kalimdor)_17365069475625398395";
    private static final String BOUNDARY = "d41d8cd98f00b204e9800998ecf8427e";

    public ChxUtility() {
        CookieHandler.setDefault(new CookieManager(null, CookiePolicy.ACCEPT_ALL));
        this.client = HttpClient.newBuilder()
                .cookieHandler(CookieHandler.getDefault())
                .proxy(ProxySelector.of(null)) // ignore proxy settings
                .connectTimeout(Duration.ofSeconds(3))
                .build();
    }

    public static TreePath getPath(TreeNode treeNode) {
        // Licsber - 2020-04-02
        List<Object> nodes = new ArrayList<Object>();
        if (treeNode != null) {
            nodes.add(treeNode);
            treeNode = treeNode.getParent();
            while (treeNode != null) {
                nodes.add(0, treeNode);
                treeNode = treeNode.getParent();
            }
        }

        return nodes.isEmpty() ? null : new TreePath(nodes.toArray());
    }

    private static String md5(String context) {
        try {
            int i;
            MessageDigest m = MessageDigest.getInstance("MD5");
            m.update(context.getBytes());
            byte[] md5bytes = m.digest();
            StringBuffer buf = new StringBuffer("");
            for (int offset = 0; offset < md5bytes.length; offset++) {
                i = md5bytes[offset];
                if (i < 0)
                    i += 256;
                if (i < 16)
                    buf.append("0");
                buf.append(Integer.toHexString(i));
            }
            return buf.toString();
        } catch (NoSuchAlgorithmException e) {
            return "";
        }
    }

    private static String getInfEnc(long mTime, String mToken) {
        String mEncryptStr = "token=" + mToken + "&_time=" + mTime + "&DESKey=Z(AfY@XS";
        return md5(mEncryptStr);
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
        return md5(encText);
    }

    public ChxAccount login(String username, String password) throws IOException, InterruptedException {

        ChxAccount account = new ChxAccount();
        account.username = username;
        account.password = password;

        long mTime = new Date().getTime();
        String mToken = "4faa8662c59590c6f43ae9fe5b002b42";
        String mInfEnc = getInfEnc(mTime, mToken);

        String postText = (
                "--{bound}\r\n" +
                        "Content-Disposition: form-data; name=\"uname\"\r\n" +
                        "Content-Type: text/plain; charset=UTF-8\r\n" +
                        "Content-Transfer-Encoding: 8bit\r\n" +
                        "\r\n" +
                        username + "\r\n" +
                        "--{bound}\r\n" +
                        "Content-Disposition: form-data; name=\"code\"\r\n" +
                        "Content-Type: text/plain; charset=UTF-8\r\n" +
                        "Content-Transfer-Encoding: 8bit\r\n" +
                        "\r\n" +
                        password + "\r\n" +
                        "--{bound}\r\n" +
                        "Content-Disposition: form-data; name=\"loginType\"\r\n" +
                        "Content-Type: text/plain; charset=UTF-8\r\n" +
                        "Content-Transfer-Encoding: 8bit\r\n" +
                        "\r\n" +
                        "1\r\n" +
                        "--{bound}\r\n" +
                        "Content-Disposition: form-data; name=\"roleSelect\"\r\n" +
                        "Content-Type: text/plain; charset=UTF-8\r\n" +
                        "Content-Transfer-Encoding: 8bit\r\n" +
                        "\r\n" +
                        "true\r\n" +
                        "--{bound}--"
        ).replaceAll("\\{bound\\}", BOUNDARY);

        HttpRequest request = HttpRequest.newBuilder()
                .POST(HttpRequest.BodyPublishers.ofString(postText))
                .uri(URI.create("http://passport2.chaoxing.com/xxt/loginregisternew?token=" + mToken + "&_time=" + mTime + "&inf_enc=" + mInfEnc))
                .setHeader("Accept-Encoding", "plain")
                .setHeader("Content-Type", "multipart/form-data; boundary=" + BOUNDARY)
                .setHeader("Accept-Language", "zh-CN,en-US;q=0.8")
                .setHeader("User-Agent", USERAGENT)
                .build();

        HttpResponse<String> response = this.client.send(request, HttpResponse.BodyHandlers.ofString());

        String resBody = response.body();

        JsonElement element = JsonParser.parseString(resBody);
        JsonObject object = element.getAsJsonObject();
        boolean isLogin = object.get("status").getAsBoolean();

        if (isLogin) {
            account = loadAccountInfo(account);
            this.account = account;

            return account;
        } else {
            return null;
        }
    }

    private ChxAccount loadAccountInfo(ChxAccount account) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .GET()
                .uri(URI.create("http://www.chaoxing.com/passport2islogin"))
                .setHeader("Accept-Encoding", "plain")
                .setHeader("Accept-Language", "zh-CN,en-US;q=0.8")
                .setHeader("User-Agent", USERAGENT)
                .build();

        HttpResponse<String> response = this.client.send(request, HttpResponse.BodyHandlers.ofString());

        String resBody = response.body();

        JsonElement element = JsonParser.parseString(resBody);
        JsonObject object = element.getAsJsonObject();
        boolean isLogin = object.get("login").getAsBoolean();

        if (isLogin) {
            account.realname = object.get("realname").getAsString();
            account.userid = object.get("uid").getAsString();
            account.schoolid = object.get("schoolid").getAsString();

            return account;
        } else {
            return null;
        }
    }

    public boolean checkLogin() throws IOException, InterruptedException {
        if (loadAccountInfo(this.account) != null)
            return true;
        else
            return false;
    }

    public ChxAccount reLogin() throws IOException, InterruptedException {
        return reLogin(this.account);
    }

    public ChxAccount reLogin(ChxAccount account) throws IOException, InterruptedException {
        return login(account.username, account.password);
    }

    public ChxAccount getAccount() {
        return this.account;
    }

    public ChxCourse[] getCourses() throws IOException, InterruptedException {
        return getCourses(this.account);
    }

    public ChxCourse[] getCourses(ChxAccount account) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .GET()
                .uri(URI.create("http://mooc1-api.chaoxing.com/mycourse?rss=1&mcode="))
                .setHeader("Accept-Encoding", "plain")
                .setHeader("Accept-Language", "zh-CN,en-US;q=0.8")
                .setHeader("User-Agent", USERAGENT)
                .build();

        HttpResponse<String> response = this.client.send(request, HttpResponse.BodyHandlers.ofString());

        String resBody = response.body();

        JsonElement element = JsonParser.parseString(resBody);
        JsonObject object = element.getAsJsonObject();
        JsonArray channelList = object.get("channelList").getAsJsonArray();
        int channelLen = channelList.size();

        ChxCourse[] courseList = new ChxCourse[channelLen];
        for (int i = 0; i < channelLen; i++) {
            ChxCourse course = new ChxCourse(account);
            JsonObject channel = channelList.get(i).getAsJsonObject();
            try {
                JsonObject curCourse = channel
                        .get("content").getAsJsonObject()
                        .get("course").getAsJsonObject();
                course.name = curCourse
                        .get("data").getAsJsonArray()
                        .get(0).getAsJsonObject()
                        .get("name").getAsString();
                course.teacher = curCourse
                        .get("data").getAsJsonArray()
                        .get(0).getAsJsonObject()
                        .get("teacherfactor").getAsString();
                course.courseid = curCourse
                        .get("data").getAsJsonArray()
                        .get(0).getAsJsonObject()
                        .get("id").getAsString();
                course.urlcpi = channel
                        .get("cpi").getAsString();
                course.clazzid = channel
                        .get("content").getAsJsonObject()
                        .get("id").getAsString();
                course.supported = true;

            } catch (NullPointerException | IndexOutOfBoundsException e) {
                course.supported = false;
            }
            courseList[i] = course;
        }
        return courseList;
    }

    public ChxSection[] getSections(ChxCourse course) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .GET()
                .uri(URI.create("http://mooc1-api.chaoxing.com/gas/clazz?id=" + course.clazzid + "&fields=id,bbsid,classscore,allowdownload,isstart,chatid,name,state,isthirdaq,information,discuss,visiblescore,begindate,course.fields(id,infocontent,name,objectid,classscore,bulletformat,imageurl,privately,teacherfactor,unfinishedJobcount,jobcount,state,knowledge.fields(id,name,indexOrder,parentnodeid,status,layer,label,begintime,attachment.fields(id,type,objectid,extension,name).type(video)))&view=json"))
                .setHeader("Accept-Encoding", "plain")
                .setHeader("Accept-Language", "zh-CN,en-US;q=0.8")
                .setHeader("User-Agent", USERAGENT)
                .build();

        HttpResponse<String> response = this.client.send(request, HttpResponse.BodyHandlers.ofString());

        String resBody = response.body();

        JsonElement element = JsonParser.parseString(resBody);
        JsonObject object = element.getAsJsonObject();
        JsonArray knowledgeArray = object
                .get("data").getAsJsonArray()
                .get(0).getAsJsonObject()
                .get("course").getAsJsonObject()
                .get("data").getAsJsonArray()
                .get(0).getAsJsonObject()
                .get("knowledge").getAsJsonObject()
                .get("data").getAsJsonArray();

        ArrayList<ChxSection> arr = new ArrayList<>();
        int knowledgeLen = knowledgeArray.size();

        // The order of data is awful. Do some sorting.
        for (int primaryorder = 1; ; primaryorder++) {
            // find the primary section by order
            int parentnodeid = -1;
            // go through all the list
            for (int i = 0; i < knowledgeLen; i++) {
                JsonObject knowledgeJson = knowledgeArray.get(i).getAsJsonObject();
                if (knowledgeJson.get("parentnodeid").getAsInt() == 0 &&
                        knowledgeJson.get("indexorder").getAsInt() == primaryorder) {
                    parentnodeid = knowledgeJson.get("id").getAsInt();
                    break;
                }
            }
            // if not found, end the loop
            if (parentnodeid == -1) break;

            // primary section found, search for the secondary section
            for (int secondaryorder = 1; ; secondaryorder++) {
                ChxSection section = null;
                // go through all the list
                for (int j = 0; j < knowledgeLen; j++) {
                    JsonObject knowledgeJson = knowledgeArray.get(j).getAsJsonObject();
                    if (knowledgeJson.get("parentnodeid").getAsInt() == parentnodeid &&
                            knowledgeJson.get("indexorder").getAsInt() == secondaryorder) {
                        // primary section found, create ChxSection
                        section = new ChxSection(course);
                        section.label = knowledgeJson.get("label").getAsString();
                        section.name = knowledgeJson.get("name").getAsString();
                        section.nodeid = String.valueOf(knowledgeJson.get("id").getAsInt());
                        section.supported = true;
                        break;
                    }
                }
                // primary section not found, end this loop
                if (section == null) break;
                // add to array
                arr.add(section);
            }
        }
        ChxSection[] sections = arr.toArray(new ChxSection[arr.size()]);
        return sections;
    }

    public ChxResource[] getResources(ChxSection section) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .GET()
                .uri(URI.create("http://mooc1-api.chaoxing.com/gas/knowledge?id=" + section.nodeid + "&courseid=" + section.course.courseid + "&fields=begintime,clickcount,createtime,description,indexorder,jobUnfinishedCount,jobcount,jobfinishcount,label,lastmodifytime,layer,listPosition,name,openlock,parentnodeid,status,id,card.fields(cardIndex,cardorder,description,knowledgeTitile,knowledgeid,theme,title,id).contentcard(all)&view=json"))
                .setHeader("Accept-Encoding", "plain")
                .setHeader("Accept-Language", "zh-CN,en-US;q=0.8")
                .setHeader("User-Agent", USERAGENT)
                .build();

        HttpResponse<String> response = this.client.send(request, HttpResponse.BodyHandlers.ofString());

        String resBody = response.body();
        JsonElement element = JsonParser.parseString(resBody);
        JsonObject object = element.getAsJsonObject();
        String description;
        try {
            description = object
                    .get("data").getAsJsonArray()
                    .get(0).getAsJsonObject()
                    .get("card").getAsJsonObject()
                    .get("data").getAsJsonArray()
                    .get(0).getAsJsonObject()
                    .get("description").getAsString();
        } catch (NullPointerException | IndexOutOfBoundsException e) {
            return new ChxResource[0];
        }

        NodeList nlist;
        try {
            // parse HTML element as XML
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            String xml = "<body>" + description.replace("&nbsp", " ") + "</body>";
            Document document = builder.parse(new ByteArrayInputStream(xml.getBytes()));
            document.getDocumentElement().normalize();
            nlist = document.getElementsByTagName("iframe");
        } catch (ParserConfigurationException | SAXException e) {
            return new ChxResource[0];
        }

        int nlen = nlist.getLength();
        ChxResource[] resources = new ChxResource[nlen];
        for (int i = 0; i < nlen; i++) {
            String dataStr = ((Element) nlist.item(i)).getAttribute("data");

            JsonObject dataJson = JsonParser.parseString(dataStr).getAsJsonObject();
            ChxResource resource = new ChxResource(section);

            if (dataJson.has("name"))
                resource.name = dataJson.get("name").getAsString();
            if (dataJson.has("jobid"))
                resource.jobid = dataJson.get("jobid").getAsString();
            if (dataJson.has("objectid"))
                resource.objectid = dataJson.get("objectid").getAsString();

            if (dataJson.has("name") && dataJson.has("jobid") && dataJson.has("objectid"))
                resource.supported = true;
            else
                resource.supported = false;

            resources[i] = resource;
        }
        return resources;
    }

    public ChxResource loadResourceInfo(ChxResource resource) throws IOException, InterruptedException {
        // Step 1
        HttpRequest request = HttpRequest.newBuilder()
                .GET()
                .uri(URI.create("http://mooc1-api.chaoxing.com/ananas/status/" + resource.objectid + "?k=" + resource.account.schoolid + "&flag=normal&_dc=" + new Date().getTime()))
                .setHeader("Accept-Encoding", "plain")
                .setHeader("Accept-Language", "zh-CN,en-US;q=0.8")
                .setHeader("User-Agent", USERAGENT)
                .build();

        HttpResponse<String> response = this.client.send(request, HttpResponse.BodyHandlers.ofString());

        String resBody = response.body();
        JsonElement element = JsonParser.parseString(resBody);
        JsonObject dataJson = element.getAsJsonObject();

        if (dataJson.has("dtoken"))
            resource.dtoken = dataJson.get("dtoken").getAsString();
        if (dataJson.has("duration"))
            resource.duration = dataJson.get("duration").getAsString();
        if (dataJson.has("download"))
            resource.downloadurl = dataJson.get("download").getAsString();

        // Step 2 (get jtoken)
        request = HttpRequest.newBuilder()
                .GET()
                .uri(URI.create("http://mooc1-api.chaoxing.com/knowledge/cards?clazzid=" + resource.course.clazzid + "&courseid=" + resource.course.courseid + "&knowledgeid=" + resource.section.nodeid + "&num=" + ((resource.dtoken == null || resource.dtoken.isEmpty()) ? "0" : "2") + "&isPhone=1&control=true"))
                .setHeader("Accept-Encoding", "plain")
                .setHeader("Accept-Language", "zh-CN,en-US;q=0.8")
                .setHeader("User-Agent", USERAGENT)
                .build();

        response = this.client.send(request, HttpResponse.BodyHandlers.ofString());
        resBody = response.body();

        Matcher m = Pattern.compile("window.AttachmentSetting *=(.*?);\\n", Pattern.MULTILINE).matcher(resBody);
        if (m.find()) {
            try {
                String dataStr = m.group(1);

                JsonObject object = JsonParser.parseString(dataStr).getAsJsonObject();
                JsonArray attachments = object.get("attachments").getAsJsonArray();
                int attachmentsLen = attachments.size();
                for (int i = 0; i < attachmentsLen; i++) {
                    JsonObject attachment = attachments.get(i).getAsJsonObject();
                    if (attachment.has("jtoken") && attachment.has("jobid") && attachment.get("jobid").getAsString().equals(resource.jobid))
                        resource.jtoken = attachment.get("jtoken").getAsString();
                }
            } catch (IllegalStateException e) {
                // just ignore it
            }
        }
        return resource;
    }

    public void enterSection(ChxSection section) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .GET()
                .uri(URI.create("http://mooc1-api.chaoxing.com/job/submitstudy?node=" + section.nodeid + "&userid=" + section.account.userid + "&clazzid=" + section.course.clazzid + "&courseid=" + section.course.courseid + "&view=json"))
                .setHeader("Accept-Encoding", "plain")
                .setHeader("Accept-Language", "zh-CN,en-US;q=0.8")
                .setHeader("User-Agent", USERAGENT)
                .build();

        this.client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    public boolean playResource(ChxResource resource, int playTime) throws IOException, InterruptedException {
        // if no info, load it.
        if ((resource.dtoken == null || resource.dtoken.isEmpty()) && (resource.jtoken == null || resource.jtoken.isEmpty()))
            loadResourceInfo(resource);

        if (resource.duration != null && !resource.duration.isEmpty()) {
            String chxEnc = getEnc(resource, playTime);
            HttpRequest request = HttpRequest.newBuilder()
                    .GET()
                    .uri(URI.create("http://mooc1-api.chaoxing.com/multimedia/log/a/" + resource.course.urlcpi + "/" + resource.dtoken + "?otherInfo=nodeId_" + resource.section.nodeid + "&playingTime=" + playTime + "&duration=" + resource.duration + "&akid=null&jobid=" + resource.jobid + "&clipTime=0_" + resource.duration + "&clazzId=" + resource.course.clazzid + "&objectId=" + resource.objectid + "&userid=" + resource.account.userid + "&isdrag=2&enc=" + chxEnc + "&dtype=Video&view=json"))
                    .setHeader("Accept-Encoding", "plain")
                    .setHeader("Accept-Language", "zh-CN,en-US;q=0.8")
                    .setHeader("User-Agent", USERAGENT)
                    .build();

            HttpResponse<String> response = this.client.send(request, HttpResponse.BodyHandlers.ofString());

            String resBody = response.body();
            try {
                JsonElement element = JsonParser.parseString(resBody);
                JsonObject dataJson = element.getAsJsonObject();

                return dataJson.get("isPassed").getAsBoolean();
            } catch (Exception e) {
                return false;
            }
        } else {
            HttpRequest request = HttpRequest.newBuilder()
                    .GET()
                    .uri(URI.create("https://mooc1-api.chaoxing.com/ananas/job/document?jobid=" + resource.jobid + "&knowledgeid=" + resource.section.nodeid + "&courseid=" + resource.course.courseid + "&clazzid=" + resource.course.clazzid + "&jtoken=" + resource.jtoken + "&_dc=" + new Date().getTime()))
                    .setHeader("Accept-Encoding", "plain")
                    .setHeader("Accept-Language", "zh-CN,en-US;q=0.8")
                    .setHeader("User-Agent", USERAGENT)
                    .build();

            HttpResponse<String> response = this.client.send(request, HttpResponse.BodyHandlers.ofString());

            String resBody = response.body();

            JsonElement element = JsonParser.parseString(resBody);
            JsonObject dataJson = element.getAsJsonObject();

            return dataJson.get("status").getAsBoolean();
        }
    }

    public boolean saveTo(String path) {
        try {
            // Save Cookies
            CookieHandler cookie = CookieHandler.getDefault();
            this.cookieHeader = cookie.get(URI.create("http://chaoxing.com/"), this.cookieHeader);

            FileOutputStream fos = new FileOutputStream(path);
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(this);
            oos.close();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean loadFrom(String path) {
        try {
            FileInputStream fis = new FileInputStream(path);
            ObjectInputStream ois = new ObjectInputStream(fis);
            ChxUtility loaded = (ChxUtility) ois.readObject();
            ois.close();

            // Load Cookies
            CookieHandler cookie = CookieHandler.getDefault();
            this.cookieHeader.put("Set-Cookie", loaded.cookieHeader.get("Cookie"));
            cookie.put(URI.create("http://chaoxing.com/"), this.cookieHeader);
            this.cookieHeader.remove("Set-Cookie");

            this.chxData = loaded.chxData;
            this.account = loaded.account;

            return true;
        } catch (ClassNotFoundException | IOException e) {
            return false;
        }
    }
}