var https = require('https');
var Parse = require('parse/node');
var Promise = require('promise');
var config = require('./config');

Parse.initialize(config.parse.appId, config.parse.jsKey);

var count = 100;
var className = "Contact";

var args = process.argv.slice(2);
if (args >= 1) {
  count = args[0];
}
if (args >= 2) {
  className = args[1];
}

function toTitleCase(str)
{
  return str.replace(/\w\S*/g, function(txt){return txt.charAt(0).toUpperCase() + txt.substr(1).toLowerCase();});
}

console.log('Creating ' + count + ' ' + className + ' objects');
new Promise(function(resolve, reject) {
  var options = {
    hostname: 'randomuser.me',
    path: '/api/?results=' + count
  }
  var req = https.request(options, function(res) {
    res.setEncoding('utf8');
    var data = '';
    res.on('data', function(chunk) {
      data += chunk;
    });
    res.on('end', function() {
      try {
        resolve(JSON.parse(data));
      } catch (ex) {
        reject(ex);
      }
    });
  });
  req.end();
}).then(function(json) {
  var objects = json.results.map(function(result) {
    var object = new Parse.Object(className);
    object.set('name', toTitleCase(result.user.name.first + ' ' + result.user.name.last));
    return object;
  });

  return Parse.Object.saveAll(objects);
}).then(function(result) {
  console.log('done');
}, function(error) {
  console.log('error: ' + error);
});
