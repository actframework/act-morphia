package act.db.morphia.util;

import act.app.DbServiceManager;
import act.conf.AppConfig;
import act.db.morphia.MorphiaPlugin;
import act.db.morphia.MorphiaService;
import act.db.util._SequenceNumberGenerator;
import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;
import org.osgl.util.S;

/**
 * A utility to generate sequence number
 */
public class SequenceNumberGenerator implements _SequenceNumberGenerator {

    private static Datastore ds;

    @Override
    public void configure(AppConfig config, DbServiceManager dbManager) {
        Object conf = config.get(MorphiaPlugin.CONF_KEY_SEQ_SVC_ID);
        String serviceId = null == conf ? DbServiceManager.DEFAULT : S.string(conf);
        MorphiaService service = dbManager.dbService(serviceId);
        ds = service.ds();
    }

    @Entity("_act_seq")
    @SuppressWarnings("unused")
    static class Sequence {
        @Id
        private String _id;
        private long number;
    }

    /**
     * Returns the next number in the sequence specified . If sequence does not exists
     * then it will be created
     * @param name the sequence name
     * @return the next number in the sequence
     */
    public long next(String name) {
        UpdateOperations<Sequence> op = ds.createUpdateOperations(Sequence.class);
        op.inc("number");
        Query<Sequence> q = ds.createQuery(Sequence.class).field("_id").equal(name);
        Sequence seq = ds.findAndModify(q, op, false, true);
        return seq.number;
    }

    /**
     * Returns the current number in the sequence specified. If the sequence does not
     * exists then `-1` will be returned
     * @param name the sequence name
     * @return the current number in the sequence.
     */
    public long get(String name) {
        Sequence seq = ds.find(Sequence.class, "_id", name).get();
        return null == seq ? -1 : seq.number;
    }

}
