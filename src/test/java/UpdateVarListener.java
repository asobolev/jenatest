import java.util.List;

import com.hp.hpl.jena.rdf.model.*;

/**
 * Created by andrey on 22.04.15.
 */
class UpdateVarListener implements ModelChangedListener {

    private boolean changed = false;

    public void addedStatement(Statement statement) { changeHappened(); }

    public void addedStatements(Statement[] statements) { changeHappened(); }

    public void addedStatements(List<Statement> list) { changeHappened(); }

    public void addedStatements(StmtIterator stmtIterator) { changeHappened(); }

    public void addedStatements(Model model) { changeHappened(); }

    public void removedStatement(Statement statement) { changeHappened(); }

    public void removedStatements(Statement[] statements) { changeHappened(); }

    public void removedStatements(List<Statement> list) { changeHappened(); }

    public void removedStatements(StmtIterator stmtIterator) { changeHappened(); }

    public void removedStatements(Model model) { changeHappened(); }

    public void notifyEvent(Model model, Object o) { }

    public void changeHappened() {
        changed = true;
    }

    public void reset() {
        changed = false;
    }

    public boolean hasChanged() {
        return changed;
    }

}