package act.db.morphia.util;

import act.db.DbBind;
import act.db.morphia.MorphiaAdaptiveRecord;
import act.db.morphia.MorphiaDao;
import org.osgl.http.H;
import org.osgl.mvc.annotation.Action;
import org.osgl.mvc.annotation.DeleteAction;
import org.osgl.mvc.annotation.GetAction;
import org.osgl.mvc.annotation.PostAction;

import java.util.Map;

import static act.controller.Controller.Util.notFoundIfNull;

public abstract class MorphiaAdaptiveRecordRestfulServiceBase<MODEL_TYPE extends MorphiaAdaptiveRecord<MODEL_TYPE>> extends MorphiaDao<MODEL_TYPE> {
    @GetAction
    public Iterable<MODEL_TYPE> list() {
        return findAll();
    }

    @GetAction("{id}")
    public MODEL_TYPE get(@DbBind("id") MODEL_TYPE model) {
        return model;
    }

    @PostAction
    public MODEL_TYPE create(MODEL_TYPE model) {
        return save(model);
    }

    @Action(value = "{id}", methods = {H.Method.PATCH, H.Method.PUT})
    public MODEL_TYPE update(@DbBind("id") MODEL_TYPE model, Map<String, Object> data) {
        notFoundIfNull(model);
        return save(model.mergeValues(data));
    }

    @DeleteAction("{id}")
    public void delete(String id) {
        deleteById(id);
    }

}
