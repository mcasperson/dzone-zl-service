var imageWidth = 250;
var randomImages = 250;
var randomImageStartIndex = 900000;
var randomImageRange = 100000;
var dataPrefix = "/data";
var actionPrefix = "/action";

var authors = jQuery('#authors');
var topics = jQuery('#topics');
var imageTopics = jQuery('#imagetopics');
var imageClose = jQuery('#close');
var submitImage = jQuery("#submitImage");
var username = jQuery("#username");
var password = jQuery("#password");
var loginButton = jQuery("#login");
var importButton = jQuery("#import");
var originalSource = jQuery("#originalSource");
var authors = jQuery("#authors");
var authorsList = jQuery("#suggestedAuthors");
var imagesElement = jQuery("#images");
var suggestedAuthorsList = jQuery("#suggestedAuthorsList");
var edit = jQuery("#edit");
var title = jQuery("#title");
var submitButtons = jQuery("#submit, #submitTop");
var restartButtons = jQuery("#restart, #restartTop");
var poster = jQuery("#poster");

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

    imageTopics.tagsinput({
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
    loginButton.attr("disabled", "disabled");
    username.attr("disabled", "disabled");
    password.attr("disabled", "disabled");

    localStorage.setItem('username', username.val());
    localStorage.setItem('password', password.val());

    jQuery.ajax({
        method: "POST",
        url: actionPrefix + "/login",
        xhrFields: {
            withCredentials: true
        },
        data: {username: username.val(), password: password.val()}
    }).done(function(myCookies) {
        cookies = myCookies;

        importButton.removeAttr("disabled");
        originalSource.removeAttr("disabled");

        if (success) {
            success(myCookies);
        }
    }).error(function() {
        loginButton.removeAttr("disabled");
        username.removeAttr("disabled");
        password.removeAttr("disabled");
    });
}

function processDomain(domainUri, processDomain) {
    /*
        Does this domain already exist?
    */
    jQuery.get(
        dataPrefix + "/mvbDomain?include=authors,tagToMvbdomains.tag.tagToImages.image&filter[mvbDomain.domain]=" + domainUri.hostname(),
        function(existingDomains) {
            if (existingDomains.data.length != 0) {
                /*
                    Domain does exist, so add to it
                */
                console.log("Domain " + domainUri.hostname() + " already exists, so will not create a new one");
                processDomain(existingDomains.data[0].id);
            } else {
                /*
                    Domain doesn't exist, so add it
                */
                var mvdDomain =
                    {
                      data: {
                        type: "mvbDomain",
                        attributes: {
                          domain: domainUri.hostname(),
                        }
                      }
                    };

                jQuery.ajax({
                    url: dataPrefix + '/mvbDomain',
                    method: 'POST',
                    xhrFields: {
                        withCredentials: true
                    },
                    data: JSON.stringify(mvdDomain),
                    contentType: "application/json",
                    dataType : 'json'
                }).done(function(newMvbDomain) {
                    console.log("Create a new domain for " + domainUri.hostname());
                    processDomain(newMvbDomain.data.id);
                });
            }
        }
    );
}

/*
    TODO: use async.js and fix the callback to this function
*/
function addAuthorsToDomain(authorsSplit, newMvbDomainId, callback) {
    _.each(authorsSplit, function(author) {

        var name = author.name;
        var username = author.id;

        jQuery.get(
            dataPrefix + "/author?filter[author.username]=" + username,
            function(existingUser) {

                if (existingUser.data.length == 0) {

                    var author =
                            {
                              data: {
                                type: "author",
                                attributes: {
                                  name: name,
                                  username: username
                                },
                                relationships: {
                                    mvbdomain: {
                                        data: {
                                            type: "mvbDomain",
                                            id: newMvbDomainId
                                        }
                                    }
                                }
                              }
                            };

                    jQuery.ajax({
                            url: dataPrefix + '/author',
                            method: 'POST',
                            xhrFields: {
                                withCredentials: true
                            },
                            data: JSON.stringify(author),
                            contentType: "application/json",
                            dataType : 'json'
                        }).done(function(newAuthor) {
                            console.log("Create a new author for " + username);
                            console.log(JSON.stringify(newAuthor));
                            if (callback) {
                                callback();
                            }
                        });
                } else {
                    console.log("Author " + username + " already exists, so will not create a new one");
                    if (callback) {
                        callback();
                    }
                }
            }
        )
    });
}

/*
    TODO: use async.js and add a callback to this function
*/
function addTopicsToDomain(topicSplit, newMvbDomainId) {
    _.each(topicSplit, function(name) {

        jQuery.get(
                dataPrefix + "/tag?filter[tag.name]=" + name,
                function(existingTags) {
                    if (existingTags.data.length == 0) {
                        var tag =
                                {
                                  data: {
                                    type: "tag",
                                    attributes: {
                                      name: name
                                    }
                                  }
                                };

                        jQuery.ajax({
                                url: dataPrefix + '/tag',
                                method: 'POST',
                                xhrFields: {
                                    withCredentials: true
                                },
                                data: JSON.stringify(tag),
                                contentType: "application/json",
                                dataType : 'json'
                            }).done(function(newTag) {
                                console.log(JSON.stringify(newTag));
                                addTopicToDomain(newMvbDomainId, newTag.data.id);
                            });
                    } else {
                        addTopicToDomain(newMvbDomainId, existingTags.data[0].id);
                    }
                }
        );

    });
}

function addTopicToDomain(newMvbDomainId, newTagId) {

    var tagToMVBDomain =
        {
            data: {
                type: "tagToMVBDomain",
                relationships: {
                    mvbdomain: {
                        data: {
                              type: "mvbDomain",
                              id: newMvbDomainId
                        }
                    },
                    tag: {
                        data: {
                              type: "tag",
                              id: newTagId
                        }
                    }
                }
            }
        };

    jQuery.ajax({
            url: dataPrefix + '/tagToMVBDomain',
            method: 'POST',
            xhrFields: {
                withCredentials: true
            },
            data: JSON.stringify(tagToMVBDomain),
            contentType: "application/json",
            dataType : 'json'
        }).done(function(newTagToMVBDomain) {
            console.log(JSON.stringify(newTagToMVBDomain));
        });
}