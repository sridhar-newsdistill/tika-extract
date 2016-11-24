const readline = require('readline');
const fs = require('fs');
const request = require('request');

// whether to write meta-data for Elasticsearch bulk indexing
var isBulk = process.argv.length > 2 && process.argv[2] == 'bulk'

// keys to exclude from meta: content and large uninteresting items
var notMeta = new Set(['X-TIKA:content', 'Chroma Palette PaletteEntry', 'LocalColorTable ColorTableEntry',
                    'Strip Byte Counts', 'Strip Offsets', 'X-TIKA:EXCEPTION:embedded_exception']);

// process 1st path in paths then call recursively with 1st item removed to do the rest
function extract(paths) {
  if (paths.length == 0) return;
  var path = paths[0];
  process.stderr.write(`extract: processing path ${path}...\n`);
  
  function handler(err, resp, body) {
    if (err || resp.statusCode != 200) {
      process.stderr.write(`extract error: path: ${path}, err: ${err}, statusCode: ${resp.statusCode}\n`);
    } else {
      var arr = JSON.parse(body).map(a => ({
        content: a['X-TIKA:content'], 
        meta: Object.keys(a).filter(k => !notMeta.has(k)).map(k => ({ key: k, val: a[k]}))
      }));
      
      var obj = arr[0];
      obj['path'] = path;
      if (arr.length > 1) obj['embedded'] = arr.slice(1);
      if (isBulk) process.stdout.write(JSON.stringify({ create:  { _index: 'atopp', _type: 'atopp', _id: path }}) + '\n');
      process.stdout.write(JSON.stringify(obj) + '\n');
    };
    extract(paths.slice(1));
  };
  
  fs.createReadStream(path).pipe(request.put('http://localhost:9998/rmeta/text', handler));
}

// call f with all of stdin as a string as its arg
function doWithStdin(f) {
    var buf = '';
    process.stdin.setEncoding('utf8');
    process.stdin.on('data', data => buf += data.toString());
    process.stdin.on('end', () => f(buf));
}

// call extract() with array of non-empty lines from stdin as its arg
doWithStdin(inp => extract(inp.split('\n').filter(p => p)));
