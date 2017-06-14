package act.db.morphia;

/*-
 * #%L
 * ACT Morphia
 * %%
 * Copyright (C) 2015 - 2017 ActFramework
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import org.osgl.$;
import org.osgl.util.C;
import org.osgl.util.E;
import org.osgl.util.S;

import java.util.Map;

class ClientManager {

    public static final String DEF_HOST = "localhost";
    public static final int DEF_PORT = 27017;
    public static final String CONF_URI = "uri";
    public static final String CONF_URL = "url";
    public static final String CONF_HOST = "host";
    public static final String CONF_PORT = "port";
    public static final String CONF_USERNAME = "username";
    public static final String CONF_PASSWORD = "password";
    public static final String SCHEME = "mongodb://";

    private static final Map<MorphiaService, MongoClient> clients = C.newMap();

    public static $.T2<MongoClientURI, MongoClient> register(MorphiaService service, Map<String, String> conf) {
        if (clients.containsKey(service)) {
            throw E.invalidConfiguration("Mongo client has already been registered for service[%]", service.id());
        }
        MongoClientURI clientURI = create(conf);
        MongoClient client = new MongoClient(clientURI);
        clients.put(service, client);
        return $.T2(clientURI, client);
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

    private static MongoClientURI create(Map<String, String> conf) {
        String uri = getStr(CONF_URL, conf, null);
        if (null == uri) {
            uri = getStr(CONF_URI, conf, null);
        }
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
            S.Buffer sb = S.newBuffer(SCHEME);
            if (null != username && null != password) {
                sb.append(username).append(":").append(S.urlEncode(password)).append("@");
            }
            sb.append(host).append(":").append(port);
            uri = sb.toString();
        } else {
            if (!uri.startsWith(SCHEME)) {
                uri = SCHEME + uri;
            }
        }
        return new MongoClientURI(uri);
    }

    private static String getStr(String key, Map<String, String> conf, String def) {
        String val = conf.get(key);
        return null == val ? def : val;
    }

    private static int getInt(String key, Map<String, String> conf, int def) {
        String val = conf.get(key);
        return null == val ? def : Integer.parseInt(val);
    }
}
