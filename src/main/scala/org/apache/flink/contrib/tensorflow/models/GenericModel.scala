package org.apache.flink.contrib.tensorflow.models

import org.apache.flink.contrib.tensorflow.examples.common.GraphBuilder
import org.apache.flink.util.Preconditions.checkState
import org.tensorflow.framework.GraphDef
import org.tensorflow.{Graph, Session}

/**
  * A generic model based on a graphdef.
  *
  * Implementation classes provide the graphdef defining the graph to use.
  */
abstract class GenericModel[Self <: GenericModel[Self]] extends RichModel[Self] {
  that: Self =>

  protected def graphDef: GraphDef

  // --- RUNTIME ---

  private var graph: Graph = _
  private var session: Session = _

  override def open(): Unit = {
    graph = GraphBuilder.fromGraphDef(graphDef)
    try {
      session = new Session(graph)
    }
    catch {
      case e: RuntimeException =>
        graph.close()
        throw e
    }
  }

  override def close(): Unit = {
    if(session != null) {
      session.close()
      session = null
    }
    if(graph != null) {
      graph.close()
      graph = null
    }
  }

  override def run[IN](input: IN)(implicit method: Signature[Self, IN]): method.OUT = {
    checkState(session != null)
    val context = new Model.RunContext {
      override def graph: Graph = that.graph
      override def session: Session = that.session
    }
    method.run(that, context, input)
  }
}
