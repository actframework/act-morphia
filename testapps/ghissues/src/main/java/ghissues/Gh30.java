package ghissues;

import act.app.ActionContext;
import act.controller.annotation.UrlContext;
import act.db.*;
import act.db.morphia.MorphiaDao;
import act.db.morphia.MorphiaModel;
import act.inject.DefaultValue;
import org.mongodb.morphia.annotations.Entity;
import org.osgl.mvc.annotation.Before;
import org.osgl.mvc.annotation.GetAction;
import org.osgl.mvc.annotation.PostAction;
import org.osgl.mvc.annotation.PutAction;

import javax.inject.Inject;
import java.util.Date;

@UrlContext("30")
public class Gh30 {

    @Entity("gh30")
    public static class Gh30Model extends MorphiaModel<Gh30Model> {

        public String name;

        @CreatedAt
        public Date created;

        @LastModifiedAt
        public Date updated;

        @CreatedBy
        public String creator;

        @LastModifiedBy
        public String updator;
    }

    @Inject
    private MorphiaDao<Gh30Model> dao;

    /**
     * Setup user login context to verify the `@CreatedBy` and `@LastModifiedBy` field value
     * @param user the user specified - default value is `user`
     * @param context injected ActionContext used to setup login context
     */
    @Before
    public void setupLoginContext(@DefaultValue("user") String user, ActionContext context) {
        context.login(user);
    }

    @GetAction("{model}")
    public Gh30Model fetch(@DbBind Gh30Model model) {
        return model;
    }

    @PostAction
    public Gh30Model create(String name) {
        Gh30Model model = new Gh30Model();
        model.name = name;
        return dao.save(model);
    }

    @PutAction("{model}")
    public Gh30Model update(@DbBind Gh30Model model, String name) {
        model.name = name;
        return dao.save(model);
    }

}
