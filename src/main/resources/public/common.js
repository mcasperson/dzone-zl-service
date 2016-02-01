var dataPrefix = "/data";
var actionPrefix = "/action";

var authors = jQuery('#authors');
var topics = jQuery('#topics');

var cookies = null;

function initTags() {
    var tagNames = new Bloodhound({
      datumTokenizer: function (datum) {
          return Bloodhound.tokenizers.whitespace(datum.title);
      },
      queryTokenizer: Bloodhound.tokenizers.whitespace,
      remote: {
        url: 'https://dzone.com/services/internal/data/topics-search?term=%QUERY',
        wildcard: '%QUERY',
        filter: function(topics) {
          return topics.result.data;
        }
      }
    });
    tagNames.initialize();

    topics.tagsinput({
      itemValue: 'title',
      itemText: 'title',
      typeaheadjs: {
        name: 'topic',
        displayKey: 'title',
        source: tagNames.ttAdapter()
      }
    });
}

function initAuthors() {
    var authorNames = new Bloodhound({
      datumTokenizer: function (datum) {
          return Bloodhound.tokenizers.whitespace(datum.title);
      },
      queryTokenizer: Bloodhound.tokenizers.whitespace,
      remote: {
        url: 'https://dzone.com/services/widget/article-postV2/searchAuthors?q=%QUERY',
        wildcard: '%QUERY',
        filter: function(authors) {
          return authors.result.data;
        }
      }
    });
    authorNames.initialize();

    authors.tagsinput({
      itemValue: 'id',
      itemText: 'name',
      typeaheadjs: {
        name: 'author',
        displayKey: 'name',
        source: authorNames.ttAdapter()
      }
    });
}

function login(success) {
    /*
        Disable UI while we perform the login
    */
    jQuery("#login").attr("disabled", "disabled");
    jQuery("#username").attr("disabled", "disabled");
    jQuery("#password").attr("disabled", "disabled");

    jQuery.ajax({
        method: "POST",
        url: actionPrefix + "/login",
        xhrFields: {
            withCredentials: true
        },
        data: {username: jQuery("#username").val(), password: jQuery("#password").val()}
    }).done(function(myCookies) {
        cookies = myCookies;

        jQuery("#import").removeAttr("disabled");
        jQuery("#originalSource").removeAttr("disabled");

        if (success) {
            success(myCookies);
        }
    }).error(function() {
        jQuery("#login").removeAttr("disabled");
        jQuery("#username").removeAttr("disabled");
        jQuery("#password").removeAttr("disabled");
    });
}