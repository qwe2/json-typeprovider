package com.example

import com.example.json.provider.TypeProvider

import scala.language.experimental.macros

object Hello extends App {
//  val tpe = TypeProvider("obj.json")
//  println(tpe.id)
//  println(tpe.foo)
//
//  val tpe2 = TypeProvider("array.json")
//  println(tpe2.toArray.mkString(", "))
//
  val tpe3 = TypeProvider("arrayofobj.json")
  println(tpe3.toArray.mkString(", "))
}
