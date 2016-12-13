package au.csiro.data61.tikaExtract.ner

import scala.language.implicitConversions

import org.http4s.HttpService
import org.http4s.argonaut.{ jsonEncoder, jsonOf }
import org.http4s.dsl._
import org.http4s.server.{ Server, ServerApp }
import org.http4s.server.blaze.BlazeBuilder
import org.http4s.server.middleware.CORS

import com.typesafe.scalalogging.Logger

import argonaut.Argonaut.{ FloatDecodeJson, FloatEncodeJson, OptionEncodeJson, StringDecodeJson, StringEncodeJson, ToJsonIdentity, casecodec1, casecodec2 }
import argonaut.CodecJson
import scalaz.concurrent.Task

object Main extends ServerApp {
  val log = Logger(getClass)
  
  case class Text(text: String)
  case class Lang(lang: String, prob: Float)
  case class Ner(ner: String, typ: String)

  implicit val langCodec: CodecJson[Lang] = casecodec2(Lang.apply, Lang.unapply)("lang", "prob")
  implicit val textCodec: CodecJson[Text] = casecodec1(Text.apply, Text.unapply)("text")
  implicit val nerCodec: CodecJson[Ner] = casecodec2(Ner.apply, Ner.unapply)("ner", "typ")

  object LangDetect {
    import com.optimaize.langdetect.{ LanguageDetector, LanguageDetectorBuilder }
    import com.optimaize.langdetect.ngram.NgramExtractors
    import com.optimaize.langdetect.profiles.LanguageProfileReader
    import com.optimaize.langdetect.text.CommonTextObjectFactories
    
    val languageProfiles = new LanguageProfileReader().readAllBuiltIn
    val languageDetector: LanguageDetector = LanguageDetectorBuilder.create(NgramExtractors.standard).withProfiles(languageProfiles).build
    val textObjectFactory = CommonTextObjectFactories.forDetectingOnLargeText
    
    implicit def headOption[T](jl: java.util.List[T]): Option[T] = if (jl.isEmpty) None else Some(jl.get(0))
  
    def lang(text: String): Option[Lang] = {
      languageDetector.getProbabilities(textObjectFactory.forText(text))
        .map(l => Lang(l.getLocale.getLanguage, l.getProbability.toFloat))
    }
  }
  
  object CoreNLP {
    import edu.stanford.nlp.simple._
    import scala.collection.JavaConversions.asScalaBuffer

    def ner(text: String) = {
      (for {
        (s, sIdx) <- new Document(text).sentences.zipWithIndex
        _ = log.debug(s"s = $s")
        (t, wIdx) <- s.nerTags.zipWithIndex if t != "O"
      } yield Ner(s.word(wIdx), t)).toList
    }
  }

  val nerService = CORS.apply(HttpService {
    case r @ POST -> Root / "lang" =>
      r.as(jsonOf[Text]).flatMap(t => Ok(LangDetect.lang(t.text).asJson)
    )
    case r @ POST -> Root / "ner" =>
      r.as(jsonOf[Text]).flatMap(t => Ok(CoreNLP.ner(t.text).asJson)
    )
  })

  override def server(args: List[String]): Task[Server] = {
    BlazeBuilder
      .bindHttp(8080, "localhost")
      .mountService(nerService, "/api")
      .start
  }
}