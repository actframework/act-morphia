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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import act.app.ActionContext;
import act.app.App;
import act.app.data.StringValueResolverManager;
import act.event.EventBus;
import act.job.AppJobManager;
import org.junit.Before;
import org.junit.Ignore;
import org.osgl.http.H;
import osgl.ut.TestBase;

import java.lang.reflect.Field;

@Ignore
public abstract class MorphiaTestBase extends TestBase {

    protected App app;
    protected ActionContext actionContext;
    protected AppJobManager jobManager;
    protected H.Session session;
    protected EventBus eventBus;

    @Before
    public void prepare() throws Exception {
        jobManager = mock(AppJobManager.class);
        eventBus = mock(EventBus.class);
        app = mock(App.class);
        when(app.jobManager()).thenReturn(jobManager);
        when(app.eventBus()).thenReturn(eventBus);
        when(app.resolverManager()).thenReturn(mock(StringValueResolverManager.class));
        actionContext = mock(ActionContext.class);
        session = new H.Session();
        when(actionContext.session()).thenReturn(session);
        Field f = App.class.getDeclaredField("INST");
        f.setAccessible(true);
        f.set(null, app);
    }

    protected void eq(Integer n, Long l) {
        assertEquals(n.intValue(), l.intValue());
    }

}
