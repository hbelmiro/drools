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

package org.drools.core.reteoo;

import org.drools.base.reteoo.NodeTypeEnums;
import org.drools.core.common.BetaConstraints;
import org.drools.core.common.InternalFactHandle;
import org.drools.core.common.PropagationContext;
import org.drools.core.common.ReteEvaluator;
import org.drools.core.common.TupleSets;
import org.drools.core.reteoo.builder.BuildContext;

import static org.drools.core.phreak.RuleNetworkEvaluator.doExistentialUpdatesReorderChildLeftTuple;
import static org.drools.core.phreak.TupleEvaluationUtil.flushLeftTupleIfNecessary;

public class NotNode extends BetaNode {
    private static final long serialVersionUID = 510l;

    // The reason why this is here is because forall can inject a
    //  "this == " + BASE_IDENTIFIER $__forallBaseIdentifier
    // Which we don't want to actually count in the case of forall node linking
    private boolean           emptyBetaConstraints;

    public NotNode() {

    }

    public NotNode(final int id,
                   final LeftTupleSource leftInput,
                   final ObjectSource rightInput,
                   final BetaConstraints joinNodeBinder,
                   final BuildContext context) {
        super(id,
              leftInput,
              rightInput,
              joinNodeBinder,
              context);
        this.tupleMemoryEnabled = context.isTupleMemoryEnabled();
        this.setObjectCount(leftInput.getObjectCount()); // 'not' nodes do not increase the count
        
        // The reason why this is here is because forall can inject a
        //  "this == " + BASE_IDENTIFIER $__forallBaseIdentifier
        // Which we don't want to actually count in the case of forall node linking
        emptyBetaConstraints = joinNodeBinder.getConstraints().length == 0 || context.isEmptyForAllBetaConstraints();
    }
    
    @Override
    protected void reorderRightTuple(ReteEvaluator reteEvaluator, RightTuple rightTuple) {
        doExistentialUpdatesReorderChildLeftTuple(reteEvaluator, this, rightTuple);
    }

    public boolean isEmptyBetaConstraints() {
        return emptyBetaConstraints;
    }

    public void setEmptyBetaConstraints(boolean emptyBetaConstraints) {
        this.emptyBetaConstraints = emptyBetaConstraints;
    }

    public short getType() {
        return NodeTypeEnums.NotNode;
    }
    
    public LeftTuple createPeer(LeftTuple original) {
        NotNodeLeftTuple peer = new NotNodeLeftTuple();
        peer.initPeer((LeftTuple) original, this);
        original.setPeer( peer );
        return peer;
    }    

    public LeftTuple createLeftTuple(InternalFactHandle factHandle,
                                     boolean leftTupleMemoryEnabled) {
        return new NotNodeLeftTuple(factHandle, this, leftTupleMemoryEnabled );
    }

    public LeftTuple createLeftTuple(final InternalFactHandle factHandle,
                                     final LeftTuple leftTuple,
                                     final Sink sink) {
        return new NotNodeLeftTuple(factHandle,leftTuple, sink );
    }

    public LeftTuple createLeftTuple(LeftTuple leftTuple,
                                     Sink sink,
                                     PropagationContext pctx, boolean leftTupleMemoryEnabled) {
        return new NotNodeLeftTuple(leftTuple,sink, pctx, leftTupleMemoryEnabled );
    }

    public LeftTuple createLeftTuple(LeftTuple leftTuple,
                                     RightTuple rightTuple,
                                     Sink sink) {
        return new NotNodeLeftTuple(leftTuple, rightTuple, sink );
    }   
    
    public LeftTuple createLeftTuple(LeftTuple leftTuple,
                                     RightTuple rightTuple,
                                     LeftTuple currentLeftChild,
                                     LeftTuple currentRightChild,
                                     Sink sink,
                                     boolean leftTupleMemoryEnabled) {
        return new NotNodeLeftTuple(leftTuple, rightTuple, currentLeftChild, currentRightChild, sink, leftTupleMemoryEnabled );        
    }       

    public String toString() {
        ObjectTypeNode source = getObjectTypeNode();
        return "[NotNode(" + this.getId() + ") - " + ((source != null) ? source.getObjectType() : "<source from a subnetwork>") + "]";
    }

    public void assertObject( final InternalFactHandle factHandle,
                              final PropagationContext pctx,
                              final ReteEvaluator reteEvaluator ) {
        final BetaMemory memory = getBetaMemoryFromRightInput(this, reteEvaluator);

        RightTuple rightTuple = createRightTuple( factHandle,
                                                  this,
                                                  pctx);

        rightTuple.setPropagationContext(pctx);

        boolean stagedInsertWasEmpty = memory.getStagedRightTuples().addInsert( rightTuple );

        if (memory.getAndIncCounter() == 0 && isEmptyBetaConstraints()  ) {
            // strangely we link here, this is actually just to force a network evaluation
            // The assert is then processed and the rule unlinks then.
            // This is because we need the first RightTuple to link with it's blocked
            if ( stagedInsertWasEmpty ) {
                memory.setNodeDirtyWithoutNotify();
            }

            // NotNodes can only be unlinked, if they have no variable constraints
            memory.linkNode( this, reteEvaluator );
        } else if ( stagedInsertWasEmpty ) {
            // nothing staged before, notify rule, so it can evaluate network
            memory.setNodeDirty(this, reteEvaluator);
        }

        flushLeftTupleIfNecessary( reteEvaluator, memory.getOrCreateSegmentMemory( this, reteEvaluator ), isStreamMode() );
    }

    public void retractRightTuple(final RightTuple rightTuple,
                                  final PropagationContext pctx,
                                  final ReteEvaluator reteEvaluator) {
        final BetaMemory memory = (BetaMemory) reteEvaluator.getNodeMemory( this );
        rightTuple.setPropagationContext( pctx );
        doDeleteRightTuple( rightTuple, reteEvaluator, memory );
    }

    public void doDeleteRightTuple(final RightTuple rightTuple,
                                   final ReteEvaluator reteEvaluator,
                                   final BetaMemory memory) {
        TupleSets<RightTuple> stagedRightTuples = memory.getStagedRightTuples();
        boolean stagedDeleteWasEmpty = stagedRightTuples.addDelete( rightTuple );

        if (  memory.getAndDecCounter() == 1 && isEmptyBetaConstraints()  ) {
            if ( stagedDeleteWasEmpty ) {
                memory.setNodeDirtyWithoutNotify();
            }
            // NotNodes can only be unlinked, if they have no variable constraints
            memory.linkNode( this, reteEvaluator );
        }  else if ( stagedDeleteWasEmpty ) {
            // nothing staged before, notify rule, so it can evaluate network
            memory.setNodeDirty( this, reteEvaluator );
        }

        flushLeftTupleIfNecessary( reteEvaluator, memory.getOrCreateSegmentMemory( this, reteEvaluator ), isStreamMode() );
    }

    @Override
    public void modifyRightTuple(RightTuple rightTuple, PropagationContext context, ReteEvaluator reteEvaluator) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean doRemove(RuleRemovalContext context, ReteooBuilder builder) {
        if ( !isInUse() ) {
            getLeftTupleSource().removeTupleSink( this );
            getRightInput().removeObjectSink( this );
            return true;
        }
        return false;
    }

    public boolean isLeftUpdateOptimizationAllowed() {
        return getRawConstraints().isLeftUpdateOptimizationAllowed();
    }
}
