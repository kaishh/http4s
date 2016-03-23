package org.http4s
package rho

import scalaz.{\/, EitherT}
import scalaz.concurrent.Task

/** A helper for capturing the result types and status codes from routes */
sealed case class Result[
+CONTINUE,
+SWITCHINGPROTOCOLS,
+PROCESSING,

+OK,
+CREATED,
+ACCEPTED,
+NONAUTHORITATIVEINFORMATION,
+NOCONTENT,
+RESETCONTENT,
+PARTIALCONTENT,
+MULTISTATUS,
+ALREADYREPORTED,
+IMUSED,

+MULTIPLECHOICES,
+MOVEDPERMANENTLY,
+FOUND,
+SEEOTHER,
+NOTMODIFIED,
+USEPROXY,
+TEMPORARYREDIRECT,
+PERMANENTREDIRECT,

+BADREQUEST,
+UNAUTHORIZED,
+PAYMENTREQUIRED,
+FORBIDDEN,
+NOTFOUND,
+METHODNOTALLOWED,
+NOTACCEPTABLE,
+PROXYAUTHENTICATIONREQUIRED,
+REQUESTTIMEOUT,
+CONFLICT,
+GONE,
+LENGTHREQUIRED,
+PRECONDITIONFAILED,
+PAYLOADTOOLARGE,
+URITOOLONG,
+UNSUPPORTEDMEDIATYPE,
+RANGENOTSATISFIABLE,
+EXPECTATIONFAILED,
+UNPROCESSABLEENTITY,
+LOCKED,
+FAILEDDEPENDENCY,
+UPGRADEREQUIRED,
+PRECONDITIONREQUIRED,
+TOOMANYREQUESTS,
+REQUESTHEADERFIELDSTOOLARGE,

+INTERNALSERVERERROR,
+NOTIMPLEMENTED,
+BADGATEWAY,
+SERVICEUNAVAILABLE,
+GATEWAYTIMEOUT,
+HTTPVERSIONNOTSUPPORTED,
+VARIANTALSONEGOTIATES,
+INSUFFICIENTSTORAGE,
+LOOPDETECTED,
+NOTEXTENDED,
+NETWORKAUTHENTICATIONREQUIRED
](resp: Response)

object Result {

  /** Result type with completely ambiguous return types */
  type BaseResult = Result[Any,Any,Any,Any,Any,Any,Any,Any,Any,Any,Any,Any,Any,Any,Any,Any,Any,Any,Any,Any,Any,Any,Any,Any,Any,Any,Any,Any,Any,Any,Any,Any,Any,Any,Any,Any,Any,Any,Any,Any,Any,Any,Any,Any,Any,Any,Any,Any,Any,Any,Any,Any,Any,Any,Any,Any,Any]

  /** Result with no inferred return types */
  type TopResult  = Result[Nothing,Nothing,Nothing,Nothing,Nothing,Nothing,Nothing,Nothing,Nothing,Nothing,Nothing,Nothing,Nothing,Nothing,Nothing,Nothing,Nothing,Nothing,Nothing,Nothing,Nothing,Nothing,Nothing,Nothing,Nothing,Nothing,Nothing,Nothing,Nothing,Nothing,Nothing,Nothing,Nothing ,Nothing,Nothing,Nothing,Nothing,Nothing,Nothing,Nothing,Nothing,Nothing,Nothing,Nothing,Nothing,Nothing,Nothing,Nothing,Nothing,Nothing,Nothing,Nothing,Nothing,Nothing,Nothing,Nothing,Nothing]

  /** Existential result type */
  type ExResult   = Result[_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_]
}

import Result._

trait ResultSyntaxInstances {

  implicit class ResultSyntax[T >: Result.TopResult <: BaseResult](r: T) extends ResponseOps {
    override type Self = T

    def withStatus[S <% Status](status: S): Self = r.copy(resp = r.resp.copy(status = status))

    override def attemptAs[T](implicit decoder: EntityDecoder[T]): DecodeResult[T] = {
      val t: Task[DecodeFailure\/T] = r.resp.attemptAs(decoder).run
      EitherT[Task, DecodeFailure, T](t)
    }

    override def withAttribute[A](key: AttributeKey[A], value: A): Self =
      Result(r.resp.withAttribute(key, value))

    override def replaceAllHeaders(headers: Headers): Self =
      Result(r.resp.replaceAllHeaders(headers))

    override def putHeaders(headers: Header*): Self =
      Result(r.resp.putHeaders(headers:_*))

    override def filterHeaders(f: (Header) => Boolean): Self =
      Result(r.resp.filterHeaders(f))

    def withBody[T](b: T)(implicit w: EntityEncoder[T]): Task[Self] = {
      r.resp.withBody(b)(w).map(Result(_))
    }
  }

  implicit class TaskResultSyntax[T >: Result.TopResult <: BaseResult](r: Task[T]) extends ResponseOps {
    override type Self = Task[T]

    def withStatus[S <% Status](status: S): Self = r.map{ result =>
      result.copy(resp = result.resp.copy(status = status))
    }

    override def attemptAs[T](implicit decoder: EntityDecoder[T]): DecodeResult[T] = {
      val t: Task[DecodeFailure\/T] = r.flatMap { t =>
        t.resp.attemptAs(decoder).run
      }
      EitherT[Task, DecodeFailure, T](t)
    }

    override def withAttribute[A](key: AttributeKey[A], value: A): Self =
      r.map(r => Result(r.resp.withAttribute(key, value)))

    override def replaceAllHeaders(headers: Headers): Self =
      r.map(r => Result(r.resp.replaceAllHeaders(headers)))

    override def putHeaders(headers: Header*): Self =
      r.map(r => Result(r.resp.putHeaders(headers:_*)))

    override def filterHeaders(f: (Header) => Boolean): Self =
      r.map(r => Result(r.resp.filterHeaders(f)))

    def withBody[T](b: T)(implicit w: EntityEncoder[T]): Self = {
      r.flatMap(_.withBody(b)(w))
    }
  }
}