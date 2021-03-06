/*
 * ToDo:
 * how to show main and embedded content
 * how to show main and embedder meta-data
 * search meta-data (ui to use drop down list of keys from elesticsearch)
 */

// To convert a path stored in elasticsearch to a URL for the corresponding doc replace fileBase at the start with docUrlBase
var fileBase = 'Exemplar Unstructured Deidentified data/';
var docUrlBase = 'http://localhost/atopp/content';

var searchUrl;

function initSearch() {
  searchUrl = window.location.protocol === 'file:'
    ? 'http://localhost:9200/atopp/atopp/_search'
    : '/search'; // using nginx proxy to work around CORS issue
  $('#main').append(createSearchForm());
}

/**
 * Generic
 */

var isDefined = x => typeof x !== 'undefined';

Array.prototype.flatMap = function(f) {
  return this.map(f).flatten();
}

Array.prototype.flatten = function() {
  return Array.prototype.concat.apply([], this);
}

function debug() {
  console.log(arguments)
}

function error(elem, msg) {
  console.log('error: msg =', msg);
  elem.empty().append($('<div>').addClass('error').text(msg));
}

function doAjax(url, data, success, error, method, contentType, dataType) {
  if (!method) method = data ? 'POST' : 'GET';
  if (!contentType) contentType = 'application/json; charset=utf-8';
  if (!dataType) dataType = 'json';
  try {
    debug('doAjax: url =', url, 'data =', data);
    $.ajax({
      type: method,
      url: url,
      data: data,
      contentType: contentType,
      dataType: dataType,
      success: function(data, textStatus, jqXHR) {
        try {
          debug('doAjax success: data', data, 'textStatus', textStatus, 'jqXHR', jqXHR);
          success(data);
        } catch (e) {
          debug('doAjax success exception: e = ', e);
          error(e.message);
        }
      },
      error: function(jqXHR, textStatus, errorThrown) {
        debug('doAjax ajax error: jqXHR =', jqXHR, 'textStatus =', textStatus, 'errorThrown =', errorThrown);
        error(errorThrown);
      }
    });
  } catch (e) {
    debug('doAjax exception: e =', e);
    error(e.message);
  }
}

/**
 * Generate Table.
 * <p>
 * @param data for table
 * @param cols array of (column header text, attribute of data row to pass to handler, handler to generate cell content)
 * @return table element
 */
function genTable(data, cols) {
  var t = $('<table>');
  // column headers from 'labels'
  t.append($('<tr>').append(cols.map(col => $('<th>').addClass(col.tdClass).text(col.label))));
  // make a row from each element in 'data', 'cols' gives the properties to use and their order
  t.append(data.map(x => genRow(x, cols)));
  return t;
}

function getField(x, field) {
  var v = x;
  for (var i = 0; i < field.length && isDefined(v); i++) v = v[field[i]];
  return v;
}

function genRow(x, cols) {
  return $('<tr>').append(cols.map(col => $('<td>').addClass(col.tdClass).append(col.handler(getField(x, col.field)))));
}

// what genTable needs to know for each column
function Col(label, field, handler, tdClass) {
  this.label = label; // label for header
  this.field = field; // array of field names for data
  this.handler = isDefined(handler) ? handler : defaultColHandler; // a function mapping data item => content of table/tr/td
  this.tdClass = isDefined(tdClass) ? tdClass : field[field.length - 1];
}

// functions you can use for the above Col.handler
var defaultColHandler = v => v;
var scoreColHandler = v => v === "NaN" ? "" : v.toPrecision(4);

/**
 * Search
 */

function pathColHandler(src) {
  var textMeta = (content, meta) => [
    $('<a>').attr({class: 'text', href: '#'}).text('text').data('content', content),
    $('<a>').attr({class: 'meta', href: '#'}).text('meta').data('meta', meta)
    ];
  var name = src.path.substring(fileBase.length);
  var arr = src.embedded
    ? src.embedded.map(e => $('<div>').addClass('embeddedDoc').append($('<span>').text('+-'), textMeta(e.content, e.meta)))
    : [];
  arr.unshift($('<div>').addClass('mainDoc').append(
    $('<a>').attr({ class: 'doc', href: `${docUrlBase}/${name}`, target: '_blank'}).text(name),
    textMeta(src.content, src.meta)
  ));
  return arr;
}

var trim = t => isDefined(t) ? t.trim().replace(/(?:\n *){3,}/g, '\n\n') : "";
var textContent = t => $('<span>').addClass('initialContent').append(trim(t));
var highlightContent = arr => isDefined(arr) ? arr.map(t => $('<span>').addClass('snippet').append(trim(t))) : [];
  
var contentColHandler = x => isDefined(getField(x, ['highlight', 'content']))
  ? highlightContent(x.highlight.content)
  : textContent(getField(x, ['_source', 'content']));

var embeddedColHandler = x => isDefined(getField(x, ['highlight', 'embedded.content']))
  ? highlightContent(x.highlight['embedded.content'])
  : textContent(getField(x, ['_source', 'embedded', 'content']));

function createSearchForm() {
  debug('createSearchForm');
  var q = $('<input>').attr({type: 'text', id: 'query'});
  var searchResult = $('<div>').attr('id', 'searchResult');
  var searchForm = $('<div>').attr('id', 'searchForm').append($('<form>').append(
    $('<label>').attr({for: 'query'}).text('query'),
    q,
    $('<button>').attr({type: 'button'}).text('Search').click(ev => search(q.val(), searchResult, 10, 0))
  ));
  return [ $('<h1>').text('Search'), searchForm, searchResult ];
}

// Pages: [1] 2 3 ... 27
// show union of: 1st 3 pages, current and 2 on either side, last page; with ellipsis to indicate gaps
function pages(size, from, totalHits, searchFrom) {
  var page = Math.floor(from / size + 1);
  var last = Math.floor((totalHits + size - 1) / size);
  
  var pages = [];
  var add = p => {
    if((pages.length == 0 || p > pages[pages.length - 1]) && p >= 1 && p <= last) pages.push(p);
  }
  for (var i = 1; i <= 3; ++i) add(i);
  for (var i = page - 2; i <= page + 2; ++i) add(i);
  add(last);
  // debug('pages: ', pages);
      
  var txt = t => $('<span>').addClass('page').text(t);
  var link = t => $('<a>').attr({href: t, class: 'page'}).text(t);
  var d = $('<span>').addClass('pages').text('Pages:');
  d.append(pages.flatMap((val, idx) => {
    var e = val == page ? txt(val) : link(val);
    return idx > 0 && val > pages[idx - 1] + 1 ? [ txt('...'), e ] : [ e ];
  }));
  $('a', d).on('click', function(ev) {
    // debug('pages click: ev', ev);
    ev.preventDefault();
    var page = $(ev.target).attr('href');
    searchFrom((page - 1) * size);
  });
  return d;
}

var searchResultCols = [
  new Col('Score', ['_score'], scoreColHandler),
  new Col('Document', ['_source'], pathColHandler),
  new Col('Content', [], contentColHandler),
  new Col('Embedded', [], embeddedColHandler)
];

function search(q, elem, size, from) {
  var url = from < 1 ? `${searchUrl}?size=${size}` : `${searchUrl}?size=${size};from=${from}`;
  var q2 = q.trim();
  var q3 = q2.length === 0
    ? { match_all: {} } 
    : { multi_match: {
        query: q2,
        fields: [ 'content', 'embedded.content' ]
      } };
  var qry = {
    query: q3,
    highlight: { fields: {
      content: {},
      'embedded.content': {}
    } }
  };
  debug('search: url =', url, 'qry =', qry);
  elem.empty().append($('<img>').attr({ alt: '', src: 'loading.gif', 'class': 'loading' }));
  doAjax(
    url, 
    JSON.stringify(qry),
    data => {
      elem.empty().append(
        $('<div>').addClass('summary').append(
          pages(size, from, data.hits.total, frm => search(q, elem, size, frm)),
          $('<span>').addClass('stats').text(`${data.hits.total.toLocaleString()} hits in ${(data.took * 0.001).toFixed(3)} sec`)
        ),
        genTable(data.hits.hits, searchResultCols)
      );
      var modal = $('#myModal');
      $('a.text', elem).on('click', ev => {
        $('.modal-title', modal).text('Text Content');
        $('.modal-body', modal).empty().append($('<div>').addClass('contentPopup').text(trim($(ev.target).data('content'))));
        modal.modal('show');
      });
      $('a.meta', elem).on('click', ev => {
        $('.modal-title', modal).text('Meta-data');
        $('.modal-body', modal).empty().append($('<div>').addClass('metaPopup').append(
          genTable($(ev.target).data('meta'), [new Col('Key', ['key']), new Col('Value', ['val'])])
        ));
        modal.modal('show');
      });
    },
    msg => error(elem, msg)
  );
}

