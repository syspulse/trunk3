package io.haas.intercept.server

import scala.collection.immutable
import io.jvm.uuid._

import io.haas.intercept.Script

final case class Scripts(data: immutable.Seq[Script],total:Option[Long]=None)

final case class ScriptCreateReq(id:String, src:String)
final case class ScriptUpdateReq(id:String, src:String)

final case class ScriptRes(scripts: Seq[Script])
