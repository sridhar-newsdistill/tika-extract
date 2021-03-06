package au.csiro.data61.tikaExtract.ner

import scala.language.implicitConversions

import org.http4s.HttpService
import org.http4s.argonaut.{ jsonEncoder, jsonOf }
import org.http4s.dsl._
import org.http4s.server.{ Server, ServerApp }
import org.http4s.server.blaze.BlazeBuilder
import org.http4s.server.middleware.CORS

import com.typesafe.scalalogging.Logger

import argonaut.Argonaut._
import argonaut.CodecJson
import scalaz.concurrent.Task

object Main extends ServerApp {
  val log = Logger(getClass)
  
  case class Text(text: String)
  case class Lang(lang: String, prob: Float)
  case class Ner(typ: String, entity: String, sentenceIdx: Int, wordIdxFirst: Int, wordIdxLast: Int)
  
  case class Meta(key: String, `val`: String)
  case class EmbeddedIn(content: Option[String], meta: Option[List[Meta]])
  case class DocIn(content: Option[String], meta: Option[List[Meta]], path: String, embedded: Option[List[EmbeddedIn]])
  
  case class EmbeddedOut(content: Option[String], meta: Option[List[Meta]], ner: List[Ner])
  case class DocOut(content: Option[String], meta: Option[List[Meta]], path: String, ner: List[Ner], embedded: Option[List[EmbeddedOut]])

  implicit val textCodec: CodecJson[Text] = casecodec1(Text.apply, Text.unapply)("text")
  implicit val langCodec: CodecJson[Lang] = casecodec2(Lang.apply, Lang.unapply)("lang", "prob")
  implicit val nerCodec: CodecJson[Ner] = casecodec5(Ner.apply, Ner.unapply)("typ", "entity", "sentenceIdx", "wordIdxFirst", "wordIdxLast")
  implicit val metaCodec: CodecJson[Meta] = casecodec2(Meta.apply, Meta.unapply)("key", "val")
  implicit val embeddedInCodec: CodecJson[EmbeddedIn] = casecodec2(EmbeddedIn.apply, EmbeddedIn.unapply)("content", "meta")
  implicit val docIn: CodecJson[DocIn] = casecodec4(DocIn.apply, DocIn.unapply)("content", "meta", "path", "embedded")
  implicit val embeddedOutCodec: CodecJson[EmbeddedOut] = casecodec3(EmbeddedOut.apply, EmbeddedOut.unapply)("content", "meta", "ner")
  implicit val docOut: CodecJson[DocOut] = casecodec5(DocOut.apply, DocOut.unapply)("content", "meta", "path", "ner", "embedded")

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
    
    def ner(text: String): List[Ner] = {
      val ners = for {
        (s, sIdx) <- new Document(text).sentences.zipWithIndex
        (t, wIdx) <- s.nerTags.zipWithIndex if t != "O"
      } yield Ner(t, s.word(wIdx), sIdx, wIdx, wIdx)
      // "New York" gives us a "LOCATION" entry for "New" and another for "York" - merge into a "New York" entry
      ners += Ner("O", "dummy", -1, -1, -1)
      ners.foldLeft((List.empty[Ner], None:  Option[Ner])) {
        case ((lst, None), n) => (lst, Some(n))
        case ((lst, Some(p)), n) =>
          if (p.typ == n.typ && p.sentenceIdx == n.sentenceIdx && p.wordIdxLast + 1 == n.wordIdxFirst) (lst, Some(Ner(p.typ, s"${p.entity} ${n.entity}", p.sentenceIdx, p.wordIdxFirst, n.wordIdxLast)))
          else (lst :+ p, Some(n))
      }._1
    }
  }
  
  def updateMeta(meta: Option[List[Meta]], lang: Option[Lang]): Option[List[Meta]] =
    lang.map { l => 
      Meta("language", l.lang) +: Meta("languageProbability", l.prob.toString) +: meta.getOrElse(Nil)
    }.orElse(meta)
  
  def langNer(d: DocIn): DocOut = {
    val lang = d.content.flatMap { c => LangDetect.lang(c) }
    val meta = updateMeta(d.meta, lang)
    val ner = d.content.map(c => CoreNLP.ner(c)).getOrElse(Nil)
    val embedded = d.embedded.map { _.map { e =>
      val lang = e.content.flatMap { c => LangDetect.lang(c) }
      val meta = updateMeta(e.meta, lang)
      val ner = e.content.map(c => CoreNLP.ner(c)).getOrElse(Nil)
      EmbeddedOut(e.content, meta, ner)
    } }
    DocOut(d.content, meta, d.path, ner, embedded)
  }
  
  def langNerMulti(d: List[DocIn]): List[DocOut] = d.map(langNer)
  
  def langNerMultiLine(in: String): String = {
    in.split("\n").toList.map { line =>
      if (line.contains("_index")) line
      else {
        val e = line.decodeEither[DocIn]
        val in = e.right.getOrElse(throw new Exception(e.left.get))
        langNer(in).asJson.nospaces
      }
    }.mkString("\n")
  }

  val nerService = CORS.apply(HttpService {
    case r @ POST -> Root / "lang" =>
      r.as(jsonOf[Text]).flatMap { t => Ok(LangDetect.lang(t.text).asJson) }
    case r @ POST -> Root / "ner" =>
      r.as(jsonOf[Text]).flatMap { t => Ok(CoreNLP.ner(t.text).asJson) }
    case r @ POST -> Root / "langNer" =>
      r.as(jsonOf[DocIn]).flatMap { d => Ok(langNer(d).asJson) }
    case r @ POST -> Root / "langNerMulti" =>
      r.as(jsonOf[List[DocIn]]).flatMap { d => Ok(langNerMulti(d).asJson) }
    case r @ POST -> Root / "langNerMultiLine" =>
      Ok(r.body.map { bv =>
        val e = bv.decodeUtf8
        val in = e.right.getOrElse(throw new Exception(e.left.get))
        langNerMultiLine(in)
      })
  })

  override def server(args: List[String]): Task[Server] = {
    BlazeBuilder
      .bindHttp(8080, "localhost")
      .mountService(nerService, "/api")
      .start
  }
}