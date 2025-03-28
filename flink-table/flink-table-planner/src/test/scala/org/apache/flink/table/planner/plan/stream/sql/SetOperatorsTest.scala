/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.flink.table.planner.plan.stream.sql

import org.apache.flink.table.api._
import org.apache.flink.table.planner.plan.utils.NonPojo
import org.apache.flink.table.planner.utils.TableTestBase
import org.apache.flink.table.types.AbstractDataType

import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.junit.jupiter.api.{BeforeEach, Test}

class SetOperatorsTest extends TableTestBase {

  private val util = streamTestUtil()

  @BeforeEach
  def before(): Unit = {
    util.addTableSource[(Int, Long, String)]("T1", 'a, 'b, 'c)
    util.addTableSource[(Int, Long, String)]("T2", 'd, 'e, 'f)
    util.addTableSource[(Int, Long, Int, String, Long)]("T3", 'a, 'b, 'd, 'c, 'e)
  }

  @Test
  def testUnionDifferentColumnSize(): Unit = {
    // must fail. Union inputs have different column size.
    assertThatExceptionOfType(classOf[ValidationException])
      .isThrownBy(() => util.verifyExecPlan("SELECT * FROM T1 UNION ALL SELECT * FROM T3"))
  }

  @Test
  def testUnionDifferentFieldTypes(): Unit = {
    // must fail. Union inputs have different field types.
    assertThatExceptionOfType(classOf[ValidationException])
      .isThrownBy(
        () => util.verifyExecPlan("SELECT a, b, c FROM T1 UNION ALL SELECT d, c, e FROM T3"))
  }

  @Test
  def testIntersectAll(): Unit = {
    util.verifyExecPlan("SELECT c FROM T1 INTERSECT ALL SELECT f FROM T2")
  }

  @Test
  def testIntersectDifferentFieldTypes(): Unit = {
    // must fail. Intersect inputs have different field types.
    assertThatExceptionOfType(classOf[ValidationException])
      .isThrownBy(
        () => util.verifyExecPlan("SELECT a, b, c FROM T1 INTERSECT SELECT d, c, e FROM T3"))
  }

  @Test
  def testMinusAll(): Unit = {
    util.verifyExecPlan("SELECT c FROM T1 EXCEPT ALL SELECT f FROM T2")
  }

  @Test
  def testMinusDifferentFieldTypes(): Unit = {
    // must fail. Minus inputs have different field types.
    assertThatExceptionOfType(classOf[ValidationException])
      .isThrownBy(() => util.verifyExecPlan("SELECT a, b, c FROM T1 EXCEPT SELECT d, c, e FROM T3"))
  }

  @Test
  def testIntersect(): Unit = {
    util.verifyExecPlan("SELECT c FROM T1 INTERSECT SELECT f FROM T2")
  }

  @Test
  def testIntersectLeftIsEmpty(): Unit = {
    util.verifyExecPlan("SELECT c FROM T1 WHERE 1=0 INTERSECT SELECT f FROM T2")
  }

  @Test
  def testIntersectRightIsEmpty(): Unit = {
    util.verifyExecPlan("SELECT c FROM T1 INTERSECT SELECT f FROM T2 WHERE 1=0")
  }

  @Test
  def testMinus(): Unit = {
    util.verifyExecPlan("SELECT c FROM T1 EXCEPT SELECT f FROM T2")
  }

  @Test
  def testMinusLeftIsEmpty(): Unit = {
    util.verifyExecPlan("SELECT c FROM T1 WHERE 1=0 EXCEPT SELECT f FROM T2")
  }

  @Test
  def testMinusRightIsEmpty(): Unit = {
    util.verifyExecPlan("SELECT c FROM T1 EXCEPT SELECT f FROM T2 WHERE 1=0")
  }

  @Test
  def testMinusWithNestedTypes(): Unit = {
    util.addTableSource[(Long, (Int, String), Array[Boolean])]("MyTable", 'a, 'b, 'c)
    util.verifyExecPlan("SELECT * FROM MyTable EXCEPT SELECT * FROM MyTable")
  }

  @Test
  def testUnionNullableTypes(): Unit = {
    util.addTableSource[((Int, String), (Int, String), Int)]("A", 'a, 'b, 'c)
    util.verifyExecPlan(
      "SELECT a FROM A UNION ALL SELECT CASE WHEN c > 0 THEN b ELSE NULL END FROM A")
  }

  @Test
  def testUnionAnyType(): Unit = {
    val util = batchTestUtil()
    util.addTableSource(
      "A",
      Array[AbstractDataType[_]](
        DataTypes.STRUCTURED(classOf[NonPojo]),
        DataTypes.STRUCTURED(classOf[NonPojo])),
      Array("a", "b")
    )
    util.verifyExecPlan("SELECT a FROM A UNION ALL SELECT b FROM A")
  }

  @Test
  def testIntersectWithOuterProject(): Unit = {
    util.verifyExecPlan("SELECT a FROM (SELECT a, b FROM T1 INTERSECT SELECT d, e FROM T2)")
  }
}
