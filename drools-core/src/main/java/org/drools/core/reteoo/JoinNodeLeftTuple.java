/*
 * Copyright 2010 Red Hat, Inc. and/or its affiliates.
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

import org.drools.core.common.InternalFactHandle;
import org.drools.core.common.PropagationContext;

public class JoinNodeLeftTuple extends LeftTuple {

    private static final long serialVersionUID = 540l;

    public JoinNodeLeftTuple() {
        // constructor needed for serialisation
    }

    // ------------------------------------------------------------
    // Constructors
    // ------------------------------------------------------------
    public JoinNodeLeftTuple(final InternalFactHandle factHandle,
                             final Sink sink,
                             final boolean leftTupleMemoryEnabled) {
        super(factHandle, sink, leftTupleMemoryEnabled);
    }

    public JoinNodeLeftTuple(final InternalFactHandle factHandle,
                             final LeftTuple leftTuple,
                             final Sink sink) {
        super( factHandle, leftTuple, sink );
    }

    public JoinNodeLeftTuple(final LeftTuple leftTuple,
                             final Sink sink,
                             final PropagationContext pctx,
                             final boolean leftTupleMemoryEnabled) {
        super(leftTuple, sink, pctx, leftTupleMemoryEnabled);
    }

    public JoinNodeLeftTuple(final LeftTuple leftTuple,
                             final RightTuple rightTuple,
                             final Sink sink) {
        super(leftTuple, rightTuple, sink);
    }

    public JoinNodeLeftTuple(final LeftTuple leftTuple,
                             final RightTuple rightTuple,
                             final Sink sink,
                             final boolean leftTupleMemoryEnabled) {
        this(leftTuple,
             rightTuple,
             null,
             null,
             sink,
             leftTupleMemoryEnabled);
    }

    public JoinNodeLeftTuple(final LeftTuple leftTuple,
                             final RightTuple rightTuple,
                             final LeftTuple currentLeftChild,
                             final LeftTuple currentRightChild,
                             final Sink sink,
                             final boolean leftTupleMemoryEnabled) {
        super(leftTuple,
              rightTuple,
              currentLeftChild,
              currentRightChild,
              sink,
              leftTupleMemoryEnabled);
    }
}
