package act.db.morphia;

import act.app.ActionContext;
import act.app.App;
import act.job.AppJobManager;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.runner.JUnitCore;
import org.osgl.$;
import org.osgl.http.H;
import org.osgl.util.S;

import java.util.Arrays;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Ignore
public abstract class TestBase extends Assert {

    protected App app;
    protected ActionContext actionContext;
    protected AppJobManager jobManager;
    protected H.Session session;

    @Before
    public void prepare() {
        jobManager = mock(AppJobManager.class);
        app = mock(App.class);
        when(app.jobManager()).thenReturn(jobManager);
        actionContext = mock(ActionContext.class);
        session = new H.Session();
        when(actionContext.session()).thenReturn(session);
    }

    protected void eq(Object[] a1, Object[] a2) {
        yes(Arrays.equals(a1, a2));
    }

    protected void ne(Object[] a1, Object[] a2) {
        no(Arrays.equals(a1, a2));
    }

    protected void eq(Object o1, Object o2) {
        assertEquals(o1, o2);
    }

    protected void ne(Object o1, Object o2) {
        no($.eq(o1, o2));
    }

    protected void yes(Boolean expr, String msg, Object... args) {
        assertTrue(S.fmt(msg, args), expr);
    }

    protected void yes(Boolean expr) {
        assertTrue(expr);
    }

    protected void no(Boolean expr, String msg, Object... args) {
        assertFalse(S.fmt(msg, args), expr);
    }

    protected void no(Boolean expr) {
        assertFalse(expr);
    }

    protected void fail(String msg, Object... args) {
        assertFalse(S.fmt(msg, args), true);
    }

    protected static void run(Class<? extends TestBase> cls) {
        new JUnitCore().run(cls);
    }

    protected static void println(String tmpl, Object... args) {
        System.out.println(String.format(tmpl, args));
    }

}