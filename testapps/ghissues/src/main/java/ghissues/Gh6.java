package ghissues;

import act.app.ActionContext;
import act.controller.annotation.UrlContext;
import act.db.*;
import act.db.morphia.MorphiaDao;
import act.db.morphia.MorphiaModel;
import act.inject.DefaultValue;
import act.job.OnAppStart;
import act.test.NotFixture;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Property;
import org.osgl.mvc.annotation.Before;
import org.osgl.mvc.annotation.GetAction;
import org.osgl.mvc.annotation.PostAction;
import org.osgl.mvc.annotation.PutAction;

import javax.inject.Inject;
import java.util.List;

@UrlContext("6")
public class Gh6 {

    @Entity("gh6")
    @NotFixture
    public static class Gh6Model extends MorphiaModel<Gh6Model> {
        @Property("login_name")
        public String loginName;

        public Gh6Model(String loginName) {
            this.loginName = loginName;
        }
    }

    @Inject
    private MorphiaDao<Gh6Model> dao;

    @OnAppStart
    public void initData() {
        dao.deleteAll();
        dao.save(new Gh6Model("a"));
        dao.save(new Gh6Model("c"));
        dao.save(new Gh6Model("b"));
    }

    @GetAction("by_login_name")
    public Iterable<Gh6Model> list() {
        return dao.q().order("login_name");
    }

    @GetAction("by_loginName")
    public Iterable<Gh6Model> list2() {
        return dao.q().order("loginName");
    }

}
