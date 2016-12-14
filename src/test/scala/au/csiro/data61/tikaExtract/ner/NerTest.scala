package au.csiro.data61.tikaExtract.ner

import com.typesafe.scalalogging.Logger
import org.scalatest.FlatSpec
import org.scalatest.Matchers
import Main._

class NerTest extends FlatSpec with Matchers {
  val log = Logger(getClass)
  
  "ner" should "find New York" in {
    val ners = CoreNLP.ner("Harry went to live in New York. NATO bombed Italy with Christmas cards.")
    log.debug(s"ners = $ners")
    ners should be(List(Ner("PERSON", "Harry" ,0,0,0), Ner("LOCATION", "New York" ,0,5,6), Ner("ORGANIZATION", "NATO", 1,0,0), Ner("LOCATION", "Italy", 1,2,2), Ner("DATE", "Christmas", 1,4,4)))
  }
 }