# tika-extract

## Description
Using Apache Tika to extract text and meta-data from many types of docs and load into Elasticsearch. Just some doco and scripting.

## Apache Tika
Out-of-the-box Tika doesn't OCR images embedded in PDFs. First install OCR and JSON processor:

    sudo apt-get install tesseract-ocr jq

Then the PDFParser must be configured to use tesseract:
- using [XML configuration](https://tika.apache.org/1.13/configuring.html#Using_a_Tika_Configuration_XML_file). This says some config requires > 1.14.
- [PDFParser configuration](https://wiki.apache.org/tika/PDFParser%20%28Apache%20PDFBox%29)
- resulting [tika-config.xml](tika-config.xml)

Tika 1.15 is not yet released, so use a recent [nightly](https://repository.apache.org/content/groups/snapshots/org/apache/tika/tika-server/1.15-SNAPSHOT).

    java -jar ./tika-server-1.15-20161110.184247-36.jar --config tika-config.xml
    # issue PUT request
    curl -T some.pdf http://localhost:9998/rmeta/text | jq . > some.json

This extracts text from PDFs and image files (using tesseract), but does not OCR images embedded in PDFs. This fixes it:

    mkdir tmp
    cd tmp
    jar xvf ../tika-server-1.15-20161110.184247-36.jar
    vi org/apache/tika/parser/pdf/PDFParser.properties # set extractInlineImages true
    jar cvfm ../tika-server-1.15-nb.jar META-INF/MANIFEST.MF .
    cd ..
    java -jar ./tika-server-1.15-nb.jar
    curl -T some.pdf http://localhost:9998/rmeta/text | jq . > some.json
    
So the XML config is still not working with this version.

Tika uses these comands while processing a PDF with an embedded image:

    convert -density 300 -depth 4 -colorspace gray -filter triangle -resize 900% -rotate 0 /tmp/apache-tika-2435919404079555622.tmp /tmp/apache-tika-2435919404079555622.tmp
    tesseract /tmp/apache-tika-744996606371035197.tmp /tmp/apache-tika-2435919404079555622.tmp -l eng -psm 1 txt

The image processing with `convert` appears to produce better OCR than using the original image.

To OCR tiff and jpeg2000 images embedded in PDF:

    wget http://repo1.maven.org/maven2/com/github/jai-imageio/jai-imageio-core/1.3.1/jai-imageio-core-1.3.1.jar
    wget http://repo1.maven.org/maven2/com/github/jai-imageio/jai-imageio-jpeg2000/1.3.0/jai-imageio-jpeg2000-1.3.0.jar
    java -classpath ./jai-imageio-core-1.3.1.jar:./jai-imageio-jpeg2000-1.3.0.jar -Dsun.java2d.cmm=sun.java2d.cmm.kcms.KcmsServiceProvider -jar ./tika-server-1.15-nb.jar 

## Elasticsearch

deb package install [instructions](https://www.elastic.co/guide/en/elasticsearch/reference/5.0/deb.html),
includes how to start/stop, a test URL and where to find config/logs/data etc.
To avoid different instances on your network inadvertantly forming a cluster, set a unique `cluster.name` before starting:

    sudo vi /etc/elasticsearch/elasticsearch.yml  # set cluster.name: unusualClusterName

## Scripting
The [extract.js](extract.js) nodejs script reads file paths from stdin, HTTP PUTs the file content to tika and post processes the output to be suitable
to load into Elasticsearch's [bulk API](https://www.elastic.co/guide/en/elasticsearch/reference/5.0/docs-bulk.html).

Install [node & npm](https://nodejs.org/en/download/package-manager/#debian-and-ubuntu-based-linux-distributions) (note the Ubuntu packaged versions are very old).

Install dependencies (once only):

    npm init
    npm install

Example usage:

    ls -1 docs/* | node extract.js bulk > bulk.json
    
Note the ouput as a whole is not JSON, rather each line is a JSON object representing one document.
Passing the `bulk` parameter to the script causes it to prefix each line with a line of JSON indexing meta-data required by Elasticsearch.

