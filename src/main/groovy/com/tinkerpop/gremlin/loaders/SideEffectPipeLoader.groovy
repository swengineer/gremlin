package com.tinkerpop.gremlin.loaders

import com.tinkerpop.blueprints.pgm.Edge
import com.tinkerpop.blueprints.pgm.Graph
import com.tinkerpop.blueprints.pgm.Vertex
import com.tinkerpop.gremlin.Gremlin
import com.tinkerpop.gremlin.GremlinTokens
import com.tinkerpop.gremlin.pipes.ClosureSideEffectPipe
import com.tinkerpop.gremlin.pipes.GremlinPipeline
import com.tinkerpop.gremlin.pipes.GroupCountClosurePipe
import com.tinkerpop.gremlin.pipes.TablePipe
import com.tinkerpop.gremlin.pipes.util.Table
import com.tinkerpop.pipes.IdentityPipe
import com.tinkerpop.pipes.Pipe
import com.tinkerpop.pipes.sideeffect.AggregatorPipe

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
class SideEffectPipeLoader {

    public static void load() {

        Gremlin.addStep(GremlinTokens.SIDEEFFECT);
        [Pipe, Vertex, Edge, Graph].each {
            it.metaClass.sideEffect = {final Closure closure ->
                return Gremlin.compose(delegate, new ClosureSideEffectPipe(closure));
            }
        }

        Gremlin.addStep(GremlinTokens.AGGREGATE);
        Pipe.metaClass.aggregate = {final Object... params ->
            if (params) {
                if (params.length == 2) {
                    return Gremlin.compose(delegate, new AggregatorPipe((Collection) params[0]), (Closure) params[1])
                } else {
                    if (params[0] instanceof Collection) {
                        return Gremlin.compose(delegate, new AggregatorPipe((Collection) params[0]))
                    } else {
                        return Gremlin.compose(delegate, new AggregatorPipe(new LinkedList()), (Closure) params[0])

                    }
                }
            } else {
                return Gremlin.compose(delegate, new AggregatorPipe(new LinkedList()));
            }

        }


        Gremlin.addStep(GremlinTokens.GROUPCOUNT);
        Pipe.metaClass.groupCount = {final Object... params ->
            if (params) {
                if (params.length == 2) {
                    Gremlin.compose(delegate, new GroupCountClosurePipe((Map) params[0], (Closure) params[1]));
                } else {
                    if (params[0] instanceof Map) {
                        Gremlin.compose(delegate, new GroupCountClosurePipe((Map) params[0], {it + 1l}));

                    } else {
                        Gremlin.compose(delegate, new GroupCountClosurePipe(new HashMap(), (Closure) params[0]));
                    }
                }
            } else {
                return Gremlin.compose(delegate, new GroupCountClosurePipe(new HashMap(), {it + 1l}));
            }
        }

        Gremlin.addStep(GremlinTokens.TABLE);
        Pipe.metaClass.table = {final Table table, Closure... closures ->
            Gremlin.compose(delegate, new TablePipe(table, (GremlinPipeline) delegate, closures));
        }

        Gremlin.addStep(GremlinTokens.AS)
        GremlinPipeline.metaClass.as = {final String name ->
            ((GremlinPipeline) delegate).asPipe(name);
            return delegate;
        }
        [Graph, Vertex, Edge].each {
            it.metaClass.as = {final String name ->
                final GremlinPipeline pipeline = Gremlin.compose(delegate, new IdentityPipe());
                pipeline.asPipe(name);
                return pipeline;
            }
        }
    }
}
