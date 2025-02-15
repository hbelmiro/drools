/*
 * Copyright 2005 Red Hat, Inc. and/or its affiliates.
 *
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
 */

package org.drools.base.rule;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.drools.base.base.ValueResolver;
import org.drools.base.rule.accessor.CompiledInvoker;
import org.drools.base.rule.accessor.Evaluator;
import org.drools.base.rule.accessor.PredicateExpression;
import org.drools.base.rule.accessor.Wireable;
import org.drools.base.reteoo.BaseTuple;
import org.drools.base.rule.accessor.ReadAccessor;
import org.kie.api.runtime.rule.FactHandle;

/**
 * A predicate can be written as a top level constraint or be nested
 * inside inside a field constraint (and as so, must implement the
 * Restriction interface).
 */
public class PredicateConstraint extends MutableTypeConstraint
    implements
        Wireable,
    Externalizable {

    private static final long          serialVersionUID   = 510l;

    private PredicateExpression expression;

    private Declaration[]              requiredDeclarations;

    private Declaration[]              previousDeclarations;

    private Declaration[]              localDeclarations;

    private List<PredicateConstraint>  cloned             = Collections.emptyList();

    private static final Declaration[] EMPTY_DECLARATIONS = new Declaration[0];

    public PredicateConstraint() {
        this( null );
    }

    public PredicateConstraint(final PredicateExpression evaluator) {
        this( evaluator,
              null,
              null );
    }

    public PredicateConstraint(final Declaration[] previousDeclarations,
                               final Declaration[] localDeclarations) {
        this( null,
              previousDeclarations,
              localDeclarations );
    }

    public PredicateConstraint(final PredicateExpression expression,
                               final Declaration[] previousDeclarations,
                               final Declaration[] localDeclarations ) {

        this.expression = expression;

        if ( previousDeclarations == null ) {
            this.previousDeclarations = PredicateConstraint.EMPTY_DECLARATIONS;
        } else {
            this.previousDeclarations = previousDeclarations;
        }

        if ( localDeclarations == null ) {
            this.localDeclarations = PredicateConstraint.EMPTY_DECLARATIONS;
        } else {
            this.localDeclarations = localDeclarations;
        }

        this.requiredDeclarations = new Declaration[this.previousDeclarations.length + this.localDeclarations.length];
        System.arraycopy( this.previousDeclarations,
                          0,
                          this.requiredDeclarations,
                          0,
                          this.previousDeclarations.length );
        System.arraycopy( this.localDeclarations,
                          0,
                          this.requiredDeclarations,
                          this.previousDeclarations.length,
                          this.localDeclarations.length );
    }

    @SuppressWarnings("unchecked")
    public void readExternal(ObjectInput in) throws IOException,
                                            ClassNotFoundException {
        super.readExternal( in );
        this.expression = (PredicateExpression) in.readObject();
        this.requiredDeclarations = (Declaration[]) in.readObject();
        this.previousDeclarations = (Declaration[]) in.readObject();
        this.localDeclarations = (Declaration[]) in.readObject();
        this.cloned = (List<PredicateConstraint>) in.readObject();
    }

    public void writeExternal(ObjectOutput out) throws IOException {
        super.writeExternal( out );
        if ( CompiledInvoker.isCompiledInvoker(this.expression) ) {
            out.writeObject( null );
        } else {
            out.writeObject( this.expression );
        }
        out.writeObject( this.requiredDeclarations );
        out.writeObject( this.previousDeclarations );
        out.writeObject( this.localDeclarations );
        out.writeObject( this.cloned );
    }

    public Declaration[] getRequiredDeclarations() {
        return this.requiredDeclarations;
    }

    public void replaceDeclaration(Declaration oldDecl,
                                   Declaration newDecl) {
        for ( int i = 0; i < this.requiredDeclarations.length; i++ ) {
            if ( this.requiredDeclarations[i].equals( oldDecl ) ) {
                this.requiredDeclarations[i] = newDecl;
            }
        }
        for ( int i = 0; i < this.previousDeclarations.length; i++ ) {
            if ( this.previousDeclarations[i].equals( oldDecl ) ) {
                this.previousDeclarations[i] = newDecl;
            }
        }
        for ( int i = 0; i < this.localDeclarations.length; i++ ) {
            if ( this.localDeclarations[i].equals( oldDecl ) ) {
                this.localDeclarations[i] = newDecl;
            }
        }
    }

    public void wire(Object object) {
        setPredicateExpression( (PredicateExpression) object );
        for ( PredicateConstraint clone : this.cloned ) {
            clone.wire( object );
        }
    }

    public void setPredicateExpression(final PredicateExpression expression) {
        this.expression = expression;
    }

    public PredicateExpression getPredicateExpression() {
        return this.expression;
    }
    
    public boolean isTemporal() {
        return false;
    }

    public String toString() {
        return "[PredicateConstraint previousDeclarations=" + Arrays.toString(this.previousDeclarations) +
                " localDeclarations=" + Arrays.toString(this.localDeclarations) + "]";
    }

    public int hashCode() {
        return this.expression != null ? this.expression.hashCode() : 0;
    }

    public boolean equals(final Object object) {
        if ( object == this ) {
            return true;
        }

        if ( object == null || object.getClass() != PredicateConstraint.class ) {
            return false;
        }

        final PredicateConstraint other = (PredicateConstraint) object;

        if ( this.previousDeclarations.length != other.previousDeclarations.length ) {
            return false;
        }

        if ( this.localDeclarations.length != other.localDeclarations.length ) {
            return false;
        }

        for ( int i = 0, length = this.previousDeclarations.length; i < length; i++ ) {
            if (this.previousDeclarations[i].getTupleIndex() != other.previousDeclarations[i].getTupleIndex() ) {
                return false;
            }

            if ( !this.previousDeclarations[i].getExtractor().equals( other.previousDeclarations[i].getExtractor() ) ) {
                return false;
            }
        }

        for ( int i = 0, length = this.localDeclarations.length; i < length; i++ ) {
            if (this.localDeclarations[i].getTupleIndex() != other.localDeclarations[i].getTupleIndex() ) {
                return false;
            }

            if ( !this.localDeclarations[i].getExtractor().equals( other.localDeclarations[i].getExtractor() ) ) {
                return false;
            }
        }

        return this.expression.equals( other.expression );
    }

    public ContextEntry createContextEntry() {
        PredicateContextEntry ctx = new PredicateContextEntry();
        ctx.dialectContext = this.expression.createContext();
        return ctx;
    }

    public boolean isAllowed(final FactHandle handle,
                             final ValueResolver valueResolver) {
        try {
            return this.expression.evaluate( handle,
                                             null,
                                             this.previousDeclarations,
                                             this.localDeclarations,
                                             valueResolver,
                                             null ); //((PredicateContextEntry) ctx).dialectContext );
        } catch ( final Exception e ) {
            throw new RuntimeException( "Exception executing predicate " + this.expression,
                                         e );
        }
    }

    public boolean isAllowed(ReadAccessor extractor,
                             final FactHandle handle,
                             final ValueResolver valueResolver,
                             ContextEntry context) {
        throw new UnsupportedOperationException( "Method not supported. Please contact development team." );
    }

    public boolean isAllowedCachedLeft(final ContextEntry context,
                                       final FactHandle handle) {
        try {
            final PredicateContextEntry ctx = (PredicateContextEntry) context;
            return this.expression.evaluate( handle,
                                             ctx.tuple,
                                             this.previousDeclarations,
                                             this.localDeclarations,
                                             ctx.valueResolver,
                                             ctx.dialectContext );
        } catch ( final Exception e ) {
            throw new RuntimeException( "Exception executing predicate " + this.expression,
                                        e );
        }
    }

    public boolean isAllowedCachedRight(final BaseTuple tuple,
                                        final ContextEntry context) {
        try {
            final PredicateContextEntry ctx = (PredicateContextEntry) context;
            return this.expression.evaluate( ctx.rightHandle,
                                             tuple,
                                             this.previousDeclarations,
                                             this.localDeclarations,
                                             ctx.valueResolver,
                                             ctx.dialectContext );
        } catch ( final Exception e ) {
            throw new RuntimeException( "Exception executing predicate " + this.expression,
                                        e );
        }
    }

    public PredicateConstraint clone() {
        Declaration[] previous = new Declaration[this.previousDeclarations.length];
        for ( int i = 0; i < previous.length; i++ ) {
            previous[i] = this.previousDeclarations[i].clone();
        }

        Declaration[] local = new Declaration[this.localDeclarations.length];
        for ( int i = 0; i < local.length; i++ ) {
            local[i] = this.localDeclarations[i].clone();
        }

        PredicateConstraint clone = new PredicateConstraint( this.expression,
                                                             previous,
                                                             local );

        if ( this.cloned == Collections.EMPTY_LIST ) {
            this.cloned = new ArrayList<>( 1 );
        }

        this.cloned.add( clone );

        return clone;

    }

    public static class PredicateContextEntry
        implements
        ContextEntry {

        private static final long    serialVersionUID = 510l;

        public BaseTuple                 tuple;
        public FactHandle    rightHandle;
        public ValueResolver valueResolver;

        public Object                dialectContext;

        private ContextEntry         entry;

        public PredicateContextEntry() {
        }

        public void readExternal(ObjectInput in) throws IOException,
                                                ClassNotFoundException {
            tuple = (BaseTuple) in.readObject();
            rightHandle = (FactHandle) in.readObject();
            valueResolver = (ValueResolver) in.readObject();
            dialectContext = in.readObject();
            entry = (ContextEntry) in.readObject();
        }

        public void writeExternal(ObjectOutput out) throws IOException {
            out.writeObject( tuple );
            out.writeObject( rightHandle );
            out.writeObject( valueResolver );
            out.writeObject( dialectContext );
            out.writeObject( entry );
        }

        public ContextEntry getNext() {
            return this.entry;
        }

        public void setNext(final ContextEntry entry) {
            this.entry = entry;
        }

        public void updateFromFactHandle(final ValueResolver valueResolver,
                                         final FactHandle handle) {
            this.valueResolver = valueResolver;
            this.rightHandle = handle;
        }

        public void updateFromTuple(final ValueResolver valueResolver,
                                    final BaseTuple tuple) {
            this.valueResolver = valueResolver;
            this.tuple = tuple;
        }

        public void resetTuple() {
            this.tuple = null;
        }

        public void resetFactHandle() {
            this.rightHandle = null;
        }
    }

    public Evaluator getEvaluator() {
        return null;
    }
}
