
// keys to exclude from meta: content and large uninteresting items
var notMeta = new Set(['X-TIKA:content', 'Chroma Palette PaletteEntry', 'LocalColorTable ColorTableEntry',
                      'Strip Byte Counts', 'Strip Offsets']); // , 'X-TIKA:EXCEPTION:embedded_exception']);

function transform(json) {
  var f = meta => meta.map(x => Array.isArray(x.val) ? ({ key: x.key, val: x.val.join()}) : x);
  // process.stdout.write(`json = ${json}\n`);
  try {
    var x = JSON.parse(json);
    x.meta = x.meta ? f(x.meta) : [];
    if (x.embedded) x.embedded.forEach(e => e.meta = e.meta ? f(e.meta) : []);
    // process.stdout.write(JSON.stringify({ create:  { _index: 'atopp', _type: 'atopp', _id: x.path }}) + '\n');
    process.stdout.write(JSON.stringify(x) + '\n');
  } catch(e) {
    console.error("exception", e.message, 'json', json);
  }
}

// call f with all of stdin as a string as its arg
function doWithStdin(f) {
    var buf = '';
    process.stdin.setEncoding('utf8');
    process.stdin.on('data', data => buf += data.toString());
    process.stdin.on('end', () => f(buf));
}

doWithStdin(inp => transform(inp));
