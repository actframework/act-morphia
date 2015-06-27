package act.db.morphia;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import org.osgl.util.C;
import org.osgl.util.E;
import org.osgl.util.S;

import java.util.Map;

public class ClientManager {

    public static final String DEF_HOST = "localhost";
    public static final int DEF_PORT = 27017;
    public static final String CONF_URI = "uri";
    public static final String CONF_HOST = "host";
    public static final String CONF_PORT = "port";
    public static final String CONF_USERNAME = "username";
    public static final String CONF_PASSWORD = "password";
    public static final String SCHEME = "mongodb://";

    private static final Map<MorphiaService, MongoClient> clients = C.newMap();

    public static MongoClient register(MorphiaService service, Map<String, Object> conf) {
        if (clients.containsKey(service)) {
            throw E.invalidConfiguration("Mongo client has already been registered for service[%]", service.id());
        }
        MongoClient client = create(conf);
        clients.put(service, client);
        return client;
    }

    public static MongoClient get(MorphiaService service) {
        return clients.get(service);
    }

    public static void release(MorphiaService service) {
        MongoClient client = clients.remove(service);
        if (null != client) {
            client.close();
        }
    }

    private static MongoClient create(Map<String, Object> conf) {
        String uri = getStr(CONF_URI, conf, null);
        if (null == uri) {
            String host = getStr(CONF_HOST, conf, DEF_HOST);
            int port;
            if (!host.contains(":")) {
                port = getInt(CONF_PORT, conf, DEF_PORT);
            } else {
                port = Integer.parseInt(S.after(host, ":"));
                host = S.before(host, ":");
            }
            String username = getStr(CONF_USERNAME, conf, null);
            String password = null != username ? getStr(CONF_PASSWORD, conf, null) : null;
            StringBuilder sb = S.builder().append(SCHEME);
            if (null != username && null != password) {
                sb.append(username).append(":").append(password).append("@");
            }
            sb.append(host).append(":").append(port);
            uri = sb.toString();
        } else {
            if (!uri.startsWith(SCHEME)) {
                uri = SCHEME + uri;
            }
        }
        MongoClientURI mongoClientURI = new MongoClientURI(uri);
        return new MongoClient(mongoClientURI);
    }

    private static String getStr(String key, Map<String, Object> conf, String def) {
        Object val = conf.get(key);
        return null == val ? def : val.toString();
    }

    private static int getInt(String key, Map<String, Object> conf, int def) {
        Object val = conf.get(key);
        return null == val ? def : Integer.parseInt(val.toString());
    }
}
