/*
 * Copyright 2018-2020 47 Degrees, LLC. <http://www.47deg.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package higherkindness.skeuomorph.protobuf

import cats.effect.IO
import higherkindness.skeuomorph.mu.MuF
import higherkindness.skeuomorph.protobuf.ProtobufF._
import higherkindness.skeuomorph.protobuf.ParseProto._
import higherkindness.skeuomorph.mu.CompressionType
import org.scalacheck.{Arbitrary, Gen}
import org.specs2.{ScalaCheck, Specification}
import higherkindness.droste.data.Mu
import higherkindness.droste.data.Mu._
import scala.meta._

class ProtobufProtocolSpec extends Specification with ScalaCheck {

  val currentDirectory: String                  = new java.io.File(".").getCanonicalPath
  val root                                      = "/src/test/scala/higherkindness/skeuomorph/protobuf"
  val path                                      = currentDirectory + s"$root/service"
  val importRoot                                = Some(currentDirectory + root)
  val source                                    = ProtoSource(s"book.proto", path, importRoot)
  val protobufProtocol: Protocol[Mu[ProtobufF]] = parseProto[IO, Mu[ProtobufF]].parse(source).unsafeRunSync()

  implicit val arbCompressionType: Arbitrary[CompressionType] = Arbitrary {
    Gen.oneOf(CompressionType.Identity, CompressionType.Gzip)
  }

  def is = s2"""
  Protobuf Protocol

  It should be possible to generate Scala code for a Mu protocol from a Proto file. $codegenProtobufProtocol
  """

  def codegenProtobufProtocol = prop { (ct: CompressionType, useIdiom: Boolean) =>
    val parseProtocol: Protocol[Mu[ProtobufF]] => higherkindness.skeuomorph.mu.Protocol[Mu[MuF]] = {
      p: Protocol[Mu[ProtobufF]] =>
        higherkindness.skeuomorph.mu.Protocol.fromProtobufProto(ct, useIdiom)(p)
    }

    val streamCtor: (Type, Type) => Type.Apply = {
      case (f: Type, a: Type) => t"Stream[$f, $a]"
    }

    val codegen: higherkindness.skeuomorph.mu.Protocol[Mu[MuF]] => Pkg = {
      p: higherkindness.skeuomorph.mu.Protocol[Mu[MuF]] =>
        higherkindness.skeuomorph.mu.codegen.protocol(p, streamCtor).right.get
    }

    val actual = (parseProtocol andThen codegen)(protobufProtocol)

    val expected = expectation(ct, useIdiom).parse[Source].get.children.head.asInstanceOf[Pkg]

    import scala.meta.contrib._
    actual.isEqual(expected) :| s"""
      |Actual output:
      |$actual
      |
      |
      |Expected output:
      |$expected"
      """.stripMargin
  }

  def expectation(compressionType: CompressionType, useIdiomaticEndpoints: Boolean): String = {

    val serviceParams: String = "Protobuf" +
      (if (compressionType == CompressionType.Gzip) ", Gzip" else ", Identity") +
      (if (useIdiomaticEndpoints) ", namespace = Some(\"com.acme\"), methodNameStyle = Capitalize" else "")

    s"""package com.acme
      |
      |import _root_.higherkindness.mu.rpc.protocol._
      |
      |object book {
      |
      |@message final case class Book(
      |  @_root_.pbdirect.pbIndex(1) isbn: _root_.scala.Long,
      |  @_root_.pbdirect.pbIndex(2) title: _root_.java.lang.String,
      |  @_root_.pbdirect.pbIndex(3) author: _root_.scala.List[_root_.com.acme.author.Author],
      |  @_root_.pbdirect.pbIndex(9) binding_type: _root_.scala.Option[_root_.com.acme.book.BindingType],
      |  @_root_.pbdirect.pbIndex(10) rating: _root_.scala.Option[_root_.com.acme.rating.Rating],
      |  @_root_.pbdirect.pbIndex(11) `private`: _root_.scala.Boolean,
      |  @_root_.pbdirect.pbIndex(16) `type`: _root_.scala.Option[_root_.com.acme.book.`type`],
      |  @_root_.pbdirect.pbIndex(17) nearest_copy: _root_.scala.Option[_root_.com.acme.book.BookStore.Location]
      |)
      |@message final case class `type`(
      |  @_root_.pbdirect.pbIndex(1) foo: _root_.scala.Long,
      |  @_root_.pbdirect.pbIndex(2) thing: _root_.scala.Option[_root_.com.acme.`hyphenated-name`.Thing]
      |)
      |@message final case class GetBookRequest(
      |  @_root_.pbdirect.pbIndex(1) isbn: _root_.scala.Long
      |)
      |@message final case class GetBookViaAuthor(
      |  @_root_.pbdirect.pbIndex(1) author: _root_.scala.Option[_root_.com.acme.author.Author]
      |)
      |@message final case class BookStore(
      |  @_root_.pbdirect.pbIndex(1) name: _root_.java.lang.String,
      |  @_root_.pbdirect.pbIndex(2) books: _root_.scala.Predef.Map[_root_.scala.Long, _root_.java.lang.String],
      |  @_root_.pbdirect.pbIndex(3) genres: _root_.scala.List[_root_.com.acme.book.Genre],
      |  @_root_.pbdirect.pbIndex(4,5,6,7) payment_method: _root_.scala.Option[_root_.shapeless.:+:[_root_.scala.Long, _root_.shapeless.:+:[_root_.scala.Int, _root_.shapeless.:+:[_root_.java.lang.String, _root_.shapeless.:+:[_root_.com.acme.book.Book, _root_.shapeless.CNil]]]]],
      |  @_root_.pbdirect.pbIndex(8,9) either: _root_.scala.Option[_root_.scala.Either[_root_.scala.Long, _root_.scala.Int]],
      |  @_root_.pbdirect.pbIndex(10) location: _root_.scala.Option[_root_.com.acme.book.BookStore.Location],
      |  @_root_.pbdirect.pbIndex(11) coffee_quality: _root_.scala.Option[_root_.com.acme.book.BookStore.CoffeeQuality]
      |)
      |object BookStore {
      |  @message final case class Location(
      |    @_root_.pbdirect.pbIndex(1) town: _root_.java.lang.String,
      |    @_root_.pbdirect.pbIndex(2) country: _root_.scala.Option[_root_.com.acme.book.BookStore.Location.Country]
      |  )
      |  object Location {
      |    @message final case class Country(
      |      @_root_.pbdirect.pbIndex(1) name: _root_.java.lang.String,
      |      @_root_.pbdirect.pbIndex(2) iso_code: _root_.java.lang.String
      |    )
      |  }
      |  sealed abstract class CoffeeQuality(val value: _root_.scala.Int) extends _root_.enumeratum.values.IntEnumEntry
      |  object CoffeeQuality extends _root_.enumeratum.values.IntEnum[CoffeeQuality] {
      |    case object DELICIOUS extends CoffeeQuality(0)
      |    case object DRINKABLE extends CoffeeQuality(1)
      |
      |    val values = findValues
      |  }
      |}
      |
      |sealed abstract class Genre(val value: _root_.scala.Int) extends _root_.enumeratum.values.IntEnumEntry
      |object Genre extends _root_.enumeratum.values.IntEnum[Genre] {
      |  case object UNKNOWN extends Genre(0)
      |  case object SCIENCE_FICTION extends Genre(1)
      |  case object POETRY extends Genre(2)
      |
      |  val values = findValues
      |}
      |
      |sealed abstract class BindingType(val value: _root_.scala.Int) extends _root_.enumeratum.values.IntEnumEntry
      |object BindingType extends _root_.enumeratum.values.IntEnum[BindingType] {
      |  case object HARDCOVER extends BindingType(0)
      |  case object PAPERBACK extends BindingType(5)
      |
      |  val values = findValues
      |}
      |
      |@service($serviceParams) trait BookService[F[_]] {
      |  def GetBook(req: _root_.com.acme.book.GetBookRequest): F[_root_.com.acme.book.Book]
      |  def GetBooksViaAuthor(req: _root_.com.acme.book.GetBookViaAuthor): Stream[F, _root_.com.acme.book.Book]
      |  def GetGreatestBook(req: Stream[F, _root_.com.acme.book.GetBookRequest]): F[_root_.com.acme.book.Book]
      |  def GetBooks(req: Stream[F, _root_.com.acme.book.GetBookRequest]): Stream[F, _root_.com.acme.book.Book]
      |  def GetRatingOfAuthor(req: _root_.com.acme.author.Author): F[_root_.com.acme.rating.Rating]
      |}
      |
      |}""".stripMargin
  }

  implicit class StringOps(self: String) {
    def clean: String = self.replaceAll("\\s", "")
  }

}
