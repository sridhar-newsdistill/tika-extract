
// keys to exclude from meta: content and large uninteresting items
var notMeta = new Set(['X-TIKA:content', 'Chroma Palette PaletteEntry', 'LocalColorTable ColorTableEntry',
                      'Strip Byte Counts', 'Strip Offsets']); // , 'X-TIKA:EXCEPTION:embedded_exception']);

function transform(json, path) {
  var arr = JSON.parse(json).map(a => ({
    content: a['X-TIKA:content'], 
    meta: Object.keys(a).filter(k => !notMeta.has(k)).map(k => ({ key: k, val: a[k]}))
  }));
      
  var obj = arr[0];
  obj['path'] = path;
  if (arr.length > 1) obj['embedded'] = arr.slice(1);
  process.stdout.write(JSON.stringify({ create:  { _index: 'atopp', _type: 'atopp', _id: path }}) + '\n');
  process.stdout.write(JSON.stringify(obj) + '\n');
}

// call f with all of stdin as a string as its arg
function doWithStdin(f) {
    var buf = '';
    process.stdin.setEncoding('utf8');
    process.stdin.on('data', data => buf += data.toString());
    process.stdin.on('end', () => f(buf));
}

doWithStdin(inp => transform(inp, process.argv[2]));
