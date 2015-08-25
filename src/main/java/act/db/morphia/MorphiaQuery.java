package act.db.morphia;

import act.db.Dao;
import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.query.Query;
import org.osgl.util.C;
import org.osgl.util.S;

import java.util.List;

public class MorphiaQuery<MODEL_TYPE> implements Dao.Query<MODEL_TYPE, MorphiaQuery<MODEL_TYPE>> {

    private Query<MODEL_TYPE> mq;

    public MorphiaQuery(Datastore ds, Class<MODEL_TYPE> modelType) {
        mq = ds.createQuery(modelType);
    }

    public MorphiaQuery<MODEL_TYPE> filter(String key, Object val) {
        mq.filter(key, val);
        return this;
    }

    @Override
    public MorphiaQuery<MODEL_TYPE> offset(int pos) {
        mq.offset(pos);
        return this;
    }

    @Override
    public MorphiaQuery<MODEL_TYPE> limit(int limit) {
        mq.limit(limit);
        return this;
    }

    @Override
    public MorphiaQuery<MODEL_TYPE> orderBy(String... fieldList) {
        C.List<String> spec = C.listOf(fieldList).flatMap(S.F.SPLIT);
        StringBuilder sb = S.builder();
        for (String s: spec) {
            if (s.startsWith("+")) {
                s = s.substring(1);
            }
            sb.append(s).append(",");
        }
        sb.delete(sb.length() - 1, sb.length());
        mq.order(sb.toString());
        return this;
    }

    @Override
    public MODEL_TYPE first() {
        return mq.get();
    }

    @Override
    public long count() {
        return mq.countAll();
    }

    @Override
    public Iterable<MODEL_TYPE> fetch() {
        return mq.fetch();
    }

    public List<MODEL_TYPE> fetchAsList() {
        return mq.asList();
    }

    public Query<MODEL_TYPE> morphiaQuery() {
        return mq;
    }

}
