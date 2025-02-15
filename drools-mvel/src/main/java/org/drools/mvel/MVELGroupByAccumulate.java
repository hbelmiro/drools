package org.drools.mvel;

import org.drools.base.base.ValueResolver;
import org.drools.base.reteoo.AccumulateContextEntry;
import org.drools.base.reteoo.BaseTuple;
import org.drools.base.rule.Accumulate;
import org.drools.base.rule.Declaration;
import org.drools.base.rule.accessor.Accumulator;
import org.drools.base.rule.accessor.FieldValue;
import org.drools.base.rule.accessor.ReturnValueExpression;
import org.drools.base.rule.accessor.Wireable;
import org.drools.core.common.InternalFactHandle;
import org.drools.core.common.ReteEvaluator;
import org.drools.core.reteoo.AccumulateNode;
import org.drools.core.reteoo.EvalNodeLeftTuple;
import org.drools.core.reteoo.LeftTuple;
import org.drools.core.reteoo.Tuple;
import org.drools.core.util.index.TupleList;
import org.kie.api.runtime.rule.FactHandle;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

public class MVELGroupByAccumulate extends Accumulate {
    private Accumulate innerAccumulate;
    private Declaration[] groupingDeclarations;
    private ReturnValueExpression groupingFunction;

    private boolean isMvel;

    public MVELGroupByAccumulate() { }

    public MVELGroupByAccumulate( Accumulate innerAccumulate, Declaration[] groupingDeclarations, ReturnValueExpression groupingFunction, boolean isMvel ) {
        super(innerAccumulate.getSource(), innerAccumulate.getRequiredDeclarations());
        this.innerAccumulate = innerAccumulate;
        this.groupingDeclarations = groupingDeclarations;
        this.groupingFunction = groupingFunction;
        this.isMvel = isMvel;
    }

    public Accumulate getInnerAccumulate() {
        return innerAccumulate;
    }

    private Object getKey( Tuple tuple, FactHandle handle, ReteEvaluator reteEvaluator ) {
        try {
            Tuple keyTuple = isMvel? tuple : new EvalNodeLeftTuple((InternalFactHandle) handle, (LeftTuple) tuple, tuple
                    .getTupleSink());
            FieldValue out = groupingFunction.evaluate(handle, keyTuple, groupingDeclarations,
                    getInnerDeclarationCache(), reteEvaluator, groupingFunction.createContext());
            return out.getValue();
        } catch (Exception e) {
            throw new RuntimeException("Grouping function threw an exception", e);
        }
    }

    @Override
    public void readExternal( ObjectInput in) throws IOException,
            ClassNotFoundException {
        super.readExternal(in);
        this.innerAccumulate = (Accumulate) in.readObject();
        this.groupingDeclarations = (Declaration[]) in.readObject();
        this.groupingFunction = (ReturnValueExpression) in.readObject();
        this.isMvel = in.readBoolean();
    }

    @Override
    public void writeExternal( ObjectOutput out) throws IOException {
        super.writeExternal(out);
        out.writeObject(innerAccumulate);
        out.writeObject(groupingDeclarations);
        out.writeObject(groupingFunction);
        out.writeBoolean(isMvel);
    }

    @Override
    public Accumulator[] getAccumulators() {
        return innerAccumulate.getAccumulators();
    }

    @Override
    public Object createFunctionContext() {
        return innerAccumulate.createFunctionContext();
    }

    @Override
    public Object init(Object workingMemoryContext, Object accContext,
            Object funcContext, BaseTuple leftTuple, ValueResolver valueResolver) {
        // do nothing here, it's done when the group is first created
        return funcContext;
    }

    @Override
    public Object accumulate(Object workingMemoryContext, Object context,
            BaseTuple match, FactHandle handle, ValueResolver valueResolver) {
        AccumulateNode.GroupByContext groupByContext = (AccumulateNode.GroupByContext) context;
        TupleList<AccumulateContextEntry> tupleList = groupByContext.getGroup(workingMemoryContext, innerAccumulate,
                (Tuple) match, getKey( (Tuple) match, handle, (ReteEvaluator) valueResolver), (ReteEvaluator) valueResolver);

        return accumulate(workingMemoryContext, match, handle, groupByContext, tupleList, valueResolver);
    }

    @Override
    public Object accumulate(Object workingMemoryContext, BaseTuple match, FactHandle handle,
            Object groupByContext, Object tupleList, ValueResolver valueResolver) {
        TupleList<AccumulateContextEntry> list = (TupleList<AccumulateContextEntry>) tupleList;
        ((AccumulateNode.GroupByContext)groupByContext).moveToPropagateTupleList( list);
        return innerAccumulate.accumulate(workingMemoryContext, list.getContext(), match, handle, valueResolver);
    }

    @Override
    public boolean tryReverse(Object workingMemoryContext, Object context, BaseTuple leftTuple, FactHandle handle,
            BaseTuple match, ValueResolver valueResolver) {
        Tuple tupleMatch = (Tuple) match;
        TupleList<AccumulateContextEntry> memory = tupleMatch.getMemory();
        AccumulateContextEntry entry = memory.getContext();
        boolean reversed = innerAccumulate.tryReverse(workingMemoryContext, entry, leftTuple, handle, match, valueResolver);

        if (reversed) {
            AccumulateNode.GroupByContext groupByContext = (AccumulateNode.GroupByContext) context;
            groupByContext.moveToPropagateTupleList( tupleMatch.getMemory() );

            memory.remove( tupleMatch );
            if ( memory.isEmpty() ) {
                groupByContext.removeGroup( entry.getKey() );
                memory.getContext().setEmpty( true );
            }
        }

        return reversed;
    }

    @Override
    public Object getResult( Object workingMemoryContext, Object context, BaseTuple leftTuple, ValueResolver valueResolver ) {
        AccumulateContextEntry entry = (AccumulateContextEntry) context;
        return entry.isEmpty() ? null : innerAccumulate.getResult(workingMemoryContext, context, leftTuple, valueResolver);
    }

    @Override
    public boolean supportsReverse() {
        return innerAccumulate.supportsReverse();
    }

    @Override
    public Accumulate clone() {
        return new MVELGroupByAccumulate( innerAccumulate.clone(), groupingDeclarations, groupingFunction, isMvel );
    }

    @Override
    public Object createWorkingMemoryContext() {
        return innerAccumulate.createWorkingMemoryContext();
    }

    @Override
    public boolean isMultiFunction() {
        return innerAccumulate.isMultiFunction();
    }

    @Override
    public void replaceAccumulatorDeclaration( Declaration declaration, Declaration resolved ) {
        innerAccumulate.replaceAccumulatorDeclaration(declaration, resolved);
    }

    @Override
    public boolean isGroupBy() {
        return true;
    }

    public class GroupingFunctionWirer implements Wireable {
        @Override
        public void wire(Object object) {
            ReturnValueExpression expression = (ReturnValueExpression) object;
            groupingFunction = expression;
            for ( Accumulate clone : cloned ) {
                ((MVELGroupByAccumulate)clone).groupingFunction = expression;
            }
        }
    }
}
