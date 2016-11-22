# tika-extract

## Description
Using Apache tika to extract text and meta-data from many types of docs. Just some doco and nodejs scripts, nothing special.

## Setup
Out-of-the-box tika doesn't OCR images embedded in PDFs. First

    sudo apt-get install tesseract-ocr

Then the PDFParser must be configured to use tesseract:
- using [XML configuration](https://tika.apache.org/1.13/configuring.html#Using_a_Tika_Configuration_XML_file)
- [PDFParser configuration](https://wiki.apache.org/tika/PDFParser%20%28Apache%20PDFBox%29)




needs > 1.14 and 1.15 not out yet, so use
https://repository.apache.org/content/groups/snapshots/org/apache/tika/tika-server/1.15-SNAPSHOT/tika-server-1.15-20161110.184247-36.jar
or
https://repository.apache.org/content/groups/snapshots/org/apache/tika/tika-app/1.15-SNAPSHOT/tika-app-1.15-20161110.184012-36.jar

cd ~/sw/atopp/tika
java -jar ./tika-server-1.15-20161110.184247-36.jar --config tika-config.xml
