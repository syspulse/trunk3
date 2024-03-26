package io.syspulse.haas.core

import scala.jdk.CollectionConverters._
import scala.util.control.NoStackTrace

class RetryException(msg: String) extends RuntimeException(msg) with NoStackTrace