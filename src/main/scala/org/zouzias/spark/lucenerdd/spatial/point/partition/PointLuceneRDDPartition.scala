/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.zouzias.spark.lucenerdd.spatial.point.partition

import com.spatial4j.core.shape.{Point, Shape}
import org.apache.lucene.document.{Document, StoredField}
import org.zouzias.spark.lucenerdd.spatial.core.partition.{AbstractSpatialLuceneRDDPartition, SpatialLuceneRDDPartition}

import scala.reflect._

class PointLuceneRDDPartition[K, V]
  (private val iter: Iterator[(K, V)])
  (override implicit val kTag: ClassTag[K],
  override implicit val vTag: ClassTag[V])
  (implicit locationConversion: K => (Double, Double),
   docConversion: V => Document) extends SpatialLuceneRDDPartition[K, V](iter) {

  override protected def decorateWithLocation(doc: Document, shapes: Iterable[Shape]): Document = {

    // Potentially more than one shape in this field is supported by some
    // strategies; see the Javadoc of the SpatialStrategy impl to see.
    shapes.foreach{ case shape =>
      strategy.createIndexableFields(shape).foreach{ case field =>
        doc.add(field)
      }

      // store it too; the format is up to you
      // (assume point in this example)
      val pt: Point = shape.asInstanceOf[Point]
      doc.add(new StoredField(strategy.getFieldName(), s"${pt.getX()} ${pt.getY()}"))
    }

    doc
  }

  /**
   * Restricts the entries to those satisfying a predicate
   *
   * @param pred
   * @return
   */
  override def filter(pred: (K, V) => Boolean): AbstractSpatialLuceneRDDPartition[K, V] = {
      PointLuceneRDDPartition(iterOriginal.filter(x => pred(x._1, x._2)))
  }
}

object PointLuceneRDDPartition {

  def apply[K: ClassTag, V: ClassTag]
  (iter: Iterator[(K, V)])
  (implicit keyToPoint: K => (Double, Double),
   docConversion: V => Document): SpatialLuceneRDDPartition[K, V] = {
    new PointLuceneRDDPartition[K, V](iter)(classTag[K], classTag[V])(keyToPoint, docConversion)
  }
}

