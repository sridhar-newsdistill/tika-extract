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

This extracts text from PDFs and image files (using tesseract), but does not OCR images embedded in PDFs.
So the XML config is still not working with this version.
To create a new Tika executable with this config set by default and with additional non-apache libs to support TIFF and JPEG200:

    mkdir lib
    cd lib
    wget http://repo1.maven.org/maven2/com/github/jai-imageio/jai-imageio-core/1.3.1/jai-imageio-core-1.3.1.jar
    wget http://repo1.maven.org/maven2/com/github/jai-imageio/jai-imageio-jpeg2000/1.3.0/jai-imageio-jpeg2000-1.3.0.jar
    mkdir ../tmp
    cd ../tmp
    jar xvf ../tika-server-1.15-20161110.184247-36.jar
    jar xvf ../lib/jai-imageio-core-1.3.1.jar
    jar xvf ../lib/jai-imageio-jpeg2000-1.3.0.jar
    vi org/apache/tika/parser/pdf/PDFParser.properties # set extractInlineImages true
    jar cvfm ../tika-server-1.15-nb.jar META-INF/MANIFEST.MF .
    cd ..
    java -jar ./tika-server-1.15-nb.jar
    curl -T some.pdf http://localhost:9998/rmeta/text | jq . > some.json
    
Run Tika and process docs (results end up in a `done` directory):

    java -jar tika-server-1.15-nb.jar &> tika.log &
    ls -1 docs/* | ./proc.sh firstrun
    
Tika uses these comands while processing a PDF with an embedded image:

    convert -density 300 -depth 4 -colorspace gray -filter triangle -resize 900% -rotate 0 /tmp/apache-tika-2435919404079555622.tmp /tmp/apache-tika-2435919404079555622.tmp
    tesseract /tmp/apache-tika-744996606371035197.tmp /tmp/apache-tika-2435919404079555622.tmp -l eng -psm 1 txt

The image processing with `convert` appears to produce better OCR than using the original image.

Tika is still not successfully OCRing TIF embedded in PDF. From tika.log:

    Dec 08, 2016 11:38:07 AM org.apache.pdfbox.tools.imageio.ImageIOUtil writeImage
    SEVERE: No ImageWriter found for 'tif' format
    Dec 08, 2016 11:38:07 AM org.apache.pdfbox.tools.imageio.ImageIOUtil writeImage
    SEVERE: Supported formats: JPG jpg bmp BMP gif GIF WBMP png PNG jpeg wbmp JPEG 

## Scripting

### nodejs
The `extract.js` nodejs script reads file paths from stdin, HTTP PUTs the file content to tika and post processes the output to be suitable
to load into Elasticsearch's [bulk API](https://www.elastic.co/guide/en/elasticsearch/reference/5.0/docs-bulk.html).

Install [node & npm](https://nodejs.org/en/download/package-manager/#debian-and-ubuntu-based-linux-distributions) (note the Ubuntu packaged versions are very old).

Install dependencies (once only):

    npm init
    npm install

Example usage:

    ls -1 docs/* | node extract.js bulk > bulk.json
    
Note the ouput as a whole is not JSON, rather each line is a JSON object representing one document.
Passing the `bulk` parameter to the script causes it to prefix each line with a line of JSON indexing meta-data required by Elasticsearch.

The `transform.js` nodejs script is a cut down version of `extract.js` that only performs the post processing for a single result from tika.

### bash
The `proc.sh` bash script is a replacement for `extract.js`. Example usage:

    ls -1 docs/* | ./proc.sh firstrun

With the `firstrun` parameter it:

- deletes and recreates directories `todo`, `inprogress` and `done`
- creates `todo/bunch-???` files, each (but the last) containing a fixed number of file paths from stdin
- creates a fixed number of subprocesses, each of which loops attempting to move one of these files from `todo` to `inprogress` and if that succeeds processes it
  by sending each file to tika and post processing the result with `transform.js` and finally moving the `bunch-???` file from `inprogress` to `done`.

    ./proc.sh
    
Without the `firstrun` parameter it continues a previous run by moving `inprogress` files to `todo` and then processes `todo` as above.

## Elasticsearch

### Using deb package (need admin rights to access logs etc.)
deb package install [instructions](https://www.elastic.co/guide/en/elasticsearch/reference/5.0/deb.html),
includes how to start/stop, a test URL and where to find config/logs/data etc.
To avoid different instances on your network inadvertantly forming a cluster, set a unique `cluster.name` before starting:

    sudo vi /etc/elasticsearch/elasticsearch.yml  # set cluster.name: unusualClusterName

### Using tarball

    # configure
    vi elasticsearch-5.0.2/config/elasticsearch.yml  # set cluster.name: unusualClusterName
    
    # run
    ./elasticsearch-5.0.2/bin/elasticsearch >& elasticsearch.log &

### indexing

    for i in done/bunch-00*.json; do
      curl -s -X POST localhost:9200/_bulk --data-binary "@$i"
      echo
    done
    
### custom mapping

    # manually modify the mapping that was automatically generated from the data
    curl -s localhost:9200/atopp?pretty > index.json
    vi index.json
    
    # recreate index with the new mapping
    curl -s -X DELETE localhost:9200/atopp
    curl -s -X PUT localhost:9200/atopp --data-binary "@index.json"
    
    # reload the data by repeating the "indexing" step above

### searching

    # using GET
    curl -s 'localhost:9200/atopp/atopp/_search?pretty;q=content:Mossack'
    curl -s 'localhost:9200/atopp/atopp/_search?pretty;q=embedded.content:company'
    
    # using POST
    curl -s 'localhost:9200/atopp/atopp/_search?pretty' --data-binary '
    {
        "query" : {
            "multi_match" : { "query" : "Mossack", "fields": [ "content", "embedded:content" ] }
        }
    }'

### CORS issue

Elasticsearch CORS support has not been attempted because it hasn't worked in the past. Instead we're using nginx as a proxy.

    sudo cp ui/nginx-atopp /etc/nginx/sites-available/atopp
    cd /etc/nginx/sites-enabled
    rm *                                    # disable all sites
    sudo ln -s ../sites-available/atopp .   # enable atopp site
