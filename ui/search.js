var searchUrl;

function initSearch() {
  searchUrl = window.location.protocol === 'file:'
    ? 'http://localhost:9200/atopp/atopp/_search'
    : '/es/atopp/_search'; // using nginx proxy to work around CORS issue
  $('#main').append(createSearchForm());
}

/**
 * Generic
 */

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
  elem.append($('<div>').addClass('error').text(msg));
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
  var tr = $('<tr>');
  // column headers from 'labels'
  t.append(tr);
  for (var i = 0; i < cols.length; i++) {
    var col = cols[i];
    tr.append($('<th>').addClass(col.tdClass).text(cols[i].label));
  }
  // make a row from each element in 'data'
  // 'fields' gives the properties to use and their order
  $.each(data, function(index, x) {
    t.append(genRow(x, cols));
  });
  return t;
}

function getField(x, field) {
  var v = x;
  for (var i = 0; i < field.length; i++) {
    v = v[field[i]];
  }
  return v;
}

function genRow(x, cols) {
  var tr = $('<tr>');
  for (var i = 0; i < cols.length; i++) {
    var col = cols[i];
    tr.append($('<td>').addClass(col.tdClass).append(col.handler(getField(x, col.field))));
  }
  return tr;
}

// what genTable needs to know for each column
function Col(label, field, handler, tdClass) {
  this.label = label; // label for header
  this.field = field; // array of field names for data
  this.handler = typeof handler !== 'undefined' ? handler : defaultColHandler; // a function mapping data item => content of table/tr/td
  this.tdClass = typeof tdClass !== 'undefined' ? tdClass : field[field.length - 1];
}

// functions you can use for the above Col.handler
function defaultColHandler(v) { return v; }
function scoreColHandler(v) { return v === "NaN" ? "" : v.toPrecision(4); }
function contentColHandler(v) { return v.replace(/(?:\n *){3,}/g, '\n\n'); }

/**
 * Search
 */

function createSearchForm() {
  debug('createSearchForm');
  var q = $('<input>').attr({type: 'text', id: 'query'});
  var searchResult = $('<div>').attr('id', 'searchResult');
  var searchForm = $('<div>').attr('id', 'searchForm').append($('<form>').append(
    $('<label>').attr({for: 'query'}).text('query'),
    q,
    $('<button>').attr({type: 'button'}).text('Search').click(ev => search(q.val(), searchResult))
  ));
  return [ $('<h1>').text('Search'), searchForm, searchResult ];
}

var searchResultCols = [
  new Col('Score', ['_score'], scoreColHandler),
  new Col('Path', ['_source', 'path']),
  new Col('Content', ['_source', 'content'], contentColHandler)
];

function createSearchResults(data) {
  return [
    $('<div>').addClass('stats').text(data.hits.hits.length.toLocaleString() + ' of ' + data.hits.total.toLocaleString() + ' hits in ' + (data.took * 0.001).toFixed(3) + ' sec'),
    genTable(data.hits.hits, searchResultCols)
  ];
}

function search(q, elem) {
  var qry = { query: { multi_match: {
    query: q,
    fields: [ 'content', 'embedded:content' ]
  } } };
  debug('search: searchUrl =', searchUrl, 'qry =', qry);
  elem.empty().append($('<img>').attr({ alt: '', src: 'loading.gif', 'class': 'loading' }));
  doAjax(
    searchUrl, 
    JSON.stringify(qry),
    data => elem.empty().append(createSearchResults(data)),
    msg => error(elem, msg)
  );
}

