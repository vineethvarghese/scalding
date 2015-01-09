/*
 Copyright 2014 Twitter, Inc.

 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

 http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
 */
package com.twitter.scalding.macros.impl.ordbufs

import scala.language.experimental.macros
import scala.reflect.macros.Context

import com.twitter.scalding._
import java.nio.ByteBuffer
import com.twitter.scalding.typed.OrderedBufferable

object PrimitiveOrderedBuf {
  def dispatch(c: Context): PartialFunction[c.Type, TreeOrderedBuf[c.type]] = {
    case tpe if tpe =:= c.universe.typeOf[Short] => PrimitiveOrderedBuf(c)(tpe, "getShort", "putShort")
    case tpe if tpe =:= c.universe.typeOf[Byte] => PrimitiveOrderedBuf(c)(tpe, "get", "put")
    case tpe if tpe =:= c.universe.typeOf[Char] => PrimitiveOrderedBuf(c)(tpe, "getChar", "putChar")
    case tpe if tpe =:= c.universe.typeOf[Int] => PrimitiveOrderedBuf(c)(tpe, "getInt", "putInt")
    case tpe if tpe =:= c.universe.typeOf[Long] => PrimitiveOrderedBuf(c)(tpe, "getLong", "putLong")
    case tpe if tpe =:= c.universe.typeOf[Float] => PrimitiveOrderedBuf(c)(tpe, "getFloat", "putFloat")
    case tpe if tpe =:= c.universe.typeOf[Double] => PrimitiveOrderedBuf(c)(tpe, "getDouble", "putDouble")
  }

  def apply(c: Context)(outerType: c.Type, bbGetterStr: String, bbPutterStr: String): TreeOrderedBuf[c.type] = {
    val bbGetter = c.universe.newTermName(bbGetterStr)
    val bbPutter = c.universe.newTermName(bbPutterStr)
    import c.universe._

    def freshT = newTermName(c.fresh(s"freshTerm"))

    val elementA = freshT
    val elementB = freshT
    val tmpRawVal = freshT
    val binaryCompare = q"""
    val $tmpRawVal = $elementA.${bbGetter}.compare($elementB.${bbGetter})
    if($tmpRawVal != 0)
        return $tmpRawVal
    0
    """

    val hashVal = freshT
    val hashFn = q"$hashVal.hashCode"

    val getVal = freshT
    val getFn = q"$getVal.$bbGetter"

    val putBBInput = freshT
    val putBBdataInput = freshT
    val putFn = q"$putBBInput.$bbPutter($putBBdataInput)"

    val compareInputA = freshT
    val compareInputB = freshT
    val cmpTmpVal = freshT
    val compareFn = q"""
      val $cmpTmpVal = $compareInputA.compare($compareInputB)
      if($cmpTmpVal != 0)
        return $cmpTmpVal
      0
    """

    new TreeOrderedBuf[c.type] {
      override val ctx: c.type = c
      override val tpe = outerType
      override val compareBinary = (elementA, elementB, binaryCompare)
      override val hash = (hashVal, hashFn)
      override val put = (putBBInput, putBBdataInput, putFn)
      override val get = (getVal, getFn)
      override val compare = (compareInputA, compareInputB, compareFn)
    }
  }
}
