package com.example.json.provider

import java.net.{URI, URL}

import scala.language.experimental.macros
import scala.reflect.macros.whitebox
import org.json4s._
import org.json4s.jackson.JsonMethods._

import scala.annotation.StaticAnnotation

class body(tree: Any) extends StaticAnnotation


class TypeProviderMacros(val c: whitebox.Context) {
  import c.universe._

  def extractArrayValue(values: Array[JValue])(i: Int): Tree = {
    jsonToTpe(values(i)).getOrElse(EmptyTree)
  }

  def bail(msg: String): Nothing = {
    c.abort(c.enclosingPosition, msg)
  }

  def error(msg: String): Unit = {
    c.error(c.enclosingPosition, msg)
  }

  def jsonToTpe(value: JValue): Option[Tree] = value match {
    case JNothing => None
    case JNull => None
    case JString(s) => Some(q"$s")
    case JDouble(d) => Some(q"$d")
    case JDecimal(d) => Some(q"scala.BigDecimal(${d.toString})")
    case JInt(i) => Some(q"scala.BigInt(${i.toByteArray})")
    case JLong(l) => Some(q"$l")
    case JBool(b) => Some(q"$b")
    case JArray(arr) =>
      val arrTree = arr.flatMap(jsonToTpe)

      val clsName = c.freshName[TypeName](TypeName("harraycls"))
      val hArray =
        q"""
           class $clsName {
             @_root_.com.example.json.provider.body(scala.Array[Any](..$arrTree))
             def apply(i: Int): Any = macro _root_.com.example.json.provider.DelegatedMacros.arrApply_impl

             @_root_.com.example.json.provider.body(scala.Array[Any](..$arrTree))
             def toArray: scala.Array[Any] = macro _root_.com.example.json.provider.DelegatedMacros.selectField_impl
           }
           new $clsName {}
         """

      Some(hArray)
    case JSet(set) => Some(q"scala.Set(..${set.flatMap(jsonToTpe)})")
    case JObject(fields) =>
      val fs = fields.flatMap { case (k, v) =>
        jsonToTpe(v).map(v => q"""
          @_root_.com.example.json.provider.body($v) def ${TermName(k)}: Any =
            macro _root_.com.example.json.provider.DelegatedMacros.selectField_impl""")
      }

      val clsName = c.freshName[TypeName](TypeName("jsoncls"))
      Some(q"""
         class $clsName {
          ..$fs
         }
         new $clsName {}
       """)
  }

  def provider_impl(path: c.Expr[String]): c.Tree = {
    import c.universe._

    val Literal(Constant(jsonPath: String)) = path.tree

    val jsonStream = this.getClass.getClassLoader.getResource(jsonPath).openStream()
//    val jsonStream = new URI(jsonPath).toURL.openStream()
    val parsed = parse(jsonStream)

    val ret = jsonToTpe(parsed).getOrElse(EmptyTree)
//    bail(show(ret))

    ret
  }
}

object DelegatedMacros {

  def arrApply_impl(c: whitebox.Context)(i: c.Expr[Int]): c.Tree = {
    import c.universe._

    def bail(msg: String): Nothing = {
      c.abort(c.enclosingPosition, msg)
    }

    def error(msg: String): Unit = {
      c.error(c.enclosingPosition, msg)
    }

    val arrValue = selectField_impl(c)
    val arrElems = arrValue match {
      case q"scala.Array.apply[$tpe](..$elems)($cls)" => elems
      case _ => bail("arr needs to be an array of constants")
    }
    val idx = i.tree match {
      case Literal(Constant(ix: Int)) => ix
      case _ => bail(s"i needs to be a constant Int, got ${showRaw(i.tree)}")
    }
//    error("" + show(arrValue))
//    bail("" + show(arrElems(idx)))
    val ret = c.untypecheck(arrElems(idx))
//    bail(show(ret))
    ret
  }

  def selectField_impl(c: whitebox.Context) : c.Tree = {
    c.macroApplication.symbol.annotations.filter(
      _.tree.tpe <:< c.typeOf[body]
    ).head.tree.children.tail.head
  }
}

object TypeProvider {
  def apply(path: String): Any = macro TypeProviderMacros.provider_impl
}
