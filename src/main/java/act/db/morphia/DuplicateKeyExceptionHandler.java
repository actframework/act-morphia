package act.db.morphia;

import act.app.ActionContext;
import act.handler.builtin.controller.ActionHandlerInvoker;
import act.handler.builtin.controller.ExceptionInterceptor;
import act.view.ActConflict;
import com.mongodb.DuplicateKeyException;
import org.osgl.mvc.result.Result;

public class DuplicateKeyExceptionHandler extends ExceptionInterceptor {

    public DuplicateKeyExceptionHandler() {
        super(0, DuplicateKeyException.class);
    }

    @Override
    public boolean sessionFree() {
        return false;
    }

    @Override
    public void accept(ActionHandlerInvoker.Visitor visitor) {
        // do nothing
    }



    @Override
    protected Result internalHandle(Exception e, ActionContext actionContext) throws Exception {
        return ActConflict.create(e.getMessage());
    }
}
