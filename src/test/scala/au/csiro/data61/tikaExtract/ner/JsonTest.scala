package au.csiro.data61.tikaExtract.ner

import com.typesafe.scalalogging.Logger
import org.scalatest.FlatSpec
import org.scalatest.Matchers
import Main._
import org.http4s.argonaut.{ jsonEncoder, jsonOf }
import com.typesafe.scalalogging.Logger

import argonaut.Argonaut._
import argonaut.CodecJson

class JsonTest extends FlatSpec with Matchers {
  val log = Logger(getClass)
  
  "DocIn" should "De/serialize" in {
    val d = DocIn(Some("content"), List(Meta("key1", "val1"), Meta("key2", "val2")), "path", Some(List(
      EmbeddedIn(None, List(Meta("key3", "val3"), Meta("key4", "val4"))), 
      EmbeddedIn(Some("embedded content2"), List(Meta("key5", "val5"), Meta("key6", "val6")))
    )))
    log.debug(s"json = ${d.asJson}")
    
    val json = List(
      """{
        "path": "path",
        "meta": []
      }""",
      
      """{
  "content": "\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\nPowerPoint Presentation\n\n\t \tAUMENTO \tAUMENTO \t\tIMPACTO\n\t \tREAL\tCALCULADO\t\taumentos no \n\t \t \t \t \taplicados y N/C\n\t \t \t \t \t \n\t\t\t\t\t\n\tHasta el 18 de enero de 2011\t   1,135,470 \t                  1,240,000 \t\t                (104,530)\n\tLo que queda por facutar de periodo 2012\t      600,817 \t                           736,285 \t\t                (135,468)\n\t\t\t\t\t\n\tTotal\t   1,736,287 \t                  1,976,285 \t \t                (239,998)\n\t\t\t\t\t\n\tPROPUESTA INICIAL PRESENTADA A SOCIOS\t\t                  2,200,000 \t\t\n\t\t\t\t\t\n\tImpacto por el re-cálculo inicial (estimado)\t \t                    (223,715)\t**\t \n\t\t\t\t\t\n\t\t\t\t\t\n\t** Este monto puede ser menos negativo, ya que está pendiente aplicar el aumento a Mossfon UK y Dubai sobre las  sociedades de BVI y Bahamas II periodo\n\t\t\t\t\n\t\t\t\t\t\n\nEl mayor impacto que debe darse es de unos USD463,713. Por tanto, se debe lograr un aumento en la facturación de USD1,7K\nAUMENTO EN LAS ANUALIDADES 2012\nDe los USD104,530 de aumentos no aplicados y N/C aplicadas para reversar aumentos, el 81% (i.e USD84,765), está distribuido por canastas así:\n\nHK                          24,536                   23%\nJSY/IOM               18,931                   18%\nUK                          18,038                   17%\nBRA                        12,238                   12%\nPMA                      11,022                   11%\n\t\nHK fue la más  alta, pero no muy lejos de JSY/IOM y UK.  \n\n\n\n\n",
  "meta": [
    {
      "key": "Application-Name",
      "val": "Microsoft Office PowerPoint"
    },
    {
      "key": "Application-Version",
      "val": "14.0000"
    },
    {
      "key": "Author",
      "val": "Marisabel Robles - Accounting"
    },
    {
      "key": "Content-Type",
      "val": "application/vnd.openxmlformats-officedocument.presentationml.presentation"
    },
    {
      "key": "Creation-Date",
      "val": "2012-01-23T19:39:48Z"
    },
    {
      "key": "Last-Author",
      "val": "Sutinen, Alison"
    },
    {
      "key": "Last-Modified",
      "val": "2016-10-25T02:28:08Z"
    },
    {
      "key": "Last-Save-Date",
      "val": "2016-10-25T02:28:08Z"
    },
    {
      "key": "Paragraph-Count",
      "val": "50"
    },
    {
      "key": "Presentation-Format",
      "val": "On-screen Show (4:3)"
    },
    {
      "key": "Revision-Number",
      "val": "5"
    },
    {
      "key": "Slide-Count",
      "val": "1"
    },
    {
      "key": "Total-Time",
      "val": "34"
    },
    {
      "key": "Word-Count",
      "val": "156"
    },
    {
      "key": "X-Parsed-By",
      "val": "org.apache.tika.parser.DefaultParser, org.apache.tika.parser.microsoft.ooxml.OOXMLParser"
    },
    {
      "key": "X-TIKA:parse_time_millis",
      "val": "15114"
    },
    {
      "key": "cp:revision",
      "val": "5"
    },
    {
      "key": "creator",
      "val": "Marisabel Robles - Accounting"
    },
    {
      "key": "date",
      "val": "2016-10-25T02:28:08Z"
    },
    {
      "key": "dc:creator",
      "val": "Marisabel Robles - Accounting"
    },
    {
      "key": "dc:title",
      "val": "PowerPoint Presentation"
    },
    {
      "key": "dcterms:created",
      "val": "2012-01-23T19:39:48Z"
    },
    {
      "key": "dcterms:modified",
      "val": "2016-10-25T02:28:08Z"
    },
    {
      "key": "extended-properties:AppVersion",
      "val": "14.0000"
    },
    {
      "key": "extended-properties:Application",
      "val": "Microsoft Office PowerPoint"
    },
    {
      "key": "extended-properties:PresentationFormat",
      "val": "On-screen Show (4:3)"
    },
    {
      "key": "extended-properties:TotalTime",
      "val": "34"
    },
    {
      "key": "meta:author",
      "val": "Marisabel Robles - Accounting"
    },
    {
      "key": "meta:creation-date",
      "val": "2012-01-23T19:39:48Z"
    },
    {
      "key": "meta:last-author",
      "val": "Sutinen, Alison"
    },
    {
      "key": "meta:paragraph-count",
      "val": "50"
    },
    {
      "key": "meta:save-date",
      "val": "2016-10-25T02:28:08Z"
    },
    {
      "key": "meta:slide-count",
      "val": "1"
    },
    {
      "key": "meta:word-count",
      "val": "156"
    },
    {
      "key": "modified",
      "val": "2016-10-25T02:28:08Z"
    },
    {
      "key": "title",
      "val": "PowerPoint Presentation"
    },
    {
      "key": "xmpTPg:NPages",
      "val": "1"
    }
  ],
  "path": "Exemplar Unstructured Deidentified data/AAA.pptx",
  "embedded": [
    {
      "meta": [
        {
          "key": "Chroma BlackIsZero",
          "val": "true"
        },
        {
          "key": "Chroma ColorSpaceType",
          "val": "RGB"
        },
        {
          "key": "Chroma Gamma",
          "val": "0.45455"
        },
        {
          "key": "Chroma NumChannels",
          "val": "4"
        },
        {
          "key": "Compression CompressionTypeName",
          "val": "deflate"
        },
        {
          "key": "Compression Lossless",
          "val": "true"
        },
        {
          "key": "Compression NumProgressiveScans",
          "val": "1"
        },
        {
          "key": "Content-Type",
          "val": "image/png"
        },
        {
          "key": "Data BitsPerSample",
          "val": "8 8 8 8"
        },
        {
          "key": "Data PlanarConfiguration",
          "val": "PixelInterleaved"
        },
        {
          "key": "Data SampleFormat",
          "val": "UnsignedIntegral"
        },
        {
          "key": "Dimension HorizontalPixelSize",
          "val": "0.115460105"
        },
        {
          "key": "Dimension ImageOrientation",
          "val": "Normal"
        },
        {
          "key": "Dimension PixelAspectRatio",
          "val": "1.0"
        },
        {
          "key": "Dimension VerticalPixelSize",
          "val": "0.115460105"
        },
        {
          "key": "IHDR",
          "val": "width=97, height=97, bitDepth=8, colorType=RGBAlpha, compressionMethod=deflate, filterMethod=adaptive, interlaceMethod=none"
        },
        {
          "key": "Transparency Alpha",
          "val": "nonpremultipled"
        },
        {
          "key": "X-Parsed-By",
          "val": "org.apache.tika.parser.DefaultParser, org.apache.tika.parser.ocr.TesseractOCRParser, org.apache.tika.parser.image.ImageParser"
        },
        {
          "key": "X-TIKA:embedded_resource_path",
          "val": "/image1.png"
        },
        {
          "key": "X-TIKA:parse_time_millis",
          "val": "1582"
        },
        {
          "key": "embeddedRelationshipId",
          "val": "slide1_rId2"
        },
        {
          "key": "gAMA",
          "val": "45455"
        },
        {
          "key": "height",
          "val": "97"
        },
        {
          "key": "pHYs",
          "val": "pixelsPerUnitXAxis=8661, pixelsPerUnitYAxis=8661, unitSpecifier=meter"
        },
        {
          "key": "resourceName",
          "val": "image1.png"
        },
        {
          "key": "sRGB",
          "val": "Perceptual"
        },
        {
          "key": "tiff:BitsPerSample",
          "val": "8 8 8 8"
        },
        {
          "key": "tiff:ImageLength",
          "val": "97"
        },
        {
          "key": "tiff:ImageWidth",
          "val": "97"
        },
        {
          "key": "width",
          "val": "97"
        }
      ]
    },
    {
      "content": "\n\n\n\n\n\n\n\n\n/docProps/thumbnail.jpeg\n\nI. 8.9..1'll ('l ' -l‘Jvl\n\nS aw Isl\n‘H an ‘\nK INN v0\n‘ MAI. 1.0;“\n‘3 8‘ v:\n\n:‘.\"0' ’vaI-I Suit-.00.:01 -'§830-8\nr4318 r9319! n r. 09.05 r)\n.99. .038 .35.» .nd 30.931’38' 8.3 .08 .10 3395.95... 0\n\nl \"\noi! I!’ voba’o.o!ll'-l.ll'l\",\"V’91'0I3\n\nH {1.1.3.1.}\n\n\"“4 892.00’l01 .I“?\n\n\"H ”'0 1\"“ a... 1\".\"\nav\" \"an can no“.l.|.3 I...\n\n \n\n \n\nVA «an 00331336;\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n/docProps/thumbnail.jpeg\n\n",
      "meta": [
        {
          "key": "Component 1",
          "val": "Y component: Quantization table 0, Sampling factors 2 horiz/2 vert"
        },
        {
          "key": "Component 2",
          "val": "Cb component: Quantization table 1, Sampling factors 1 horiz/1 vert"
        },
        {
          "key": "Component 3",
          "val": "Cr component: Quantization table 1, Sampling factors 1 horiz/1 vert"
        },
        {
          "key": "Compression Type",
          "val": "Baseline"
        },
        {
          "key": "Content-Type",
          "val": "image/jpeg"
        },
        {
          "key": "Data Precision",
          "val": "8 bits"
        },
        {
          "key": "File Modified Date",
          "val": "Thu Dec 08 11:34:31 +11:00 2016"
        },
        {
          "key": "File Name",
          "val": "apache-tika-2678118197485568482.tmp"
        },
        {
          "key": "File Size",
          "val": "41373 bytes"
        },
        {
          "key": "Image Height",
          "val": "192 pixels"
        },
        {
          "key": "Image Width",
          "val": "256 pixels"
        },
        {
          "key": "Number of Components",
          "val": "3"
        },
        {
          "key": "Resolution Units",
          "val": "inch"
        },
        {
          "key": "Thumbnail Height Pixels",
          "val": "0"
        },
        {
          "key": "Thumbnail Width Pixels",
          "val": "0"
        },
        {
          "key": "X Resolution",
          "val": "96 dots"
        },
        {
          "key": "X-Parsed-By",
          "val": "org.apache.tika.parser.DefaultParser, org.apache.tika.parser.ocr.TesseractOCRParser, org.apache.tika.parser.jpeg.JpegParser"
        },
        {
          "key": "X-TIKA:embedded_resource_path",
          "val": "/thumbnail.jpeg"
        },
        {
          "key": "X-TIKA:parse_time_millis",
          "val": "9249"
        },
        {
          "key": "Y Resolution",
          "val": "96 dots"
        },
        {
          "key": "dc:title",
          "val": "/docProps/thumbnail.jpeg"
        },
        {
          "key": "embeddedRelationshipId",
          "val": "/docProps/thumbnail.jpeg"
        },
        {
          "key": "resourceName",
          "val": "/docProps/thumbnail.jpeg"
        },
        {
          "key": "tiff:BitsPerSample",
          "val": "8"
        },
        {
          "key": "tiff:ImageLength",
          "val": "192"
        },
        {
          "key": "tiff:ImageWidth",
          "val": "256"
        },
        {
          "key": "title",
          "val": "/docProps/thumbnail.jpeg"
        }
      ]
    }
  ]
}
"""
      )
    for (j <- json) log.debug(s"docIn = ${j.decodeEither[DocIn]}")
  }
  
}