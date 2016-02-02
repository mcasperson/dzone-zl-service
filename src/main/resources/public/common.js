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

function addAuthors(authorsSplit, newMvbDomainId, callback) {
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
                            callback();
                        });
                } else {
                    console.log("Author " + username + " already exists, so will not create a new one");
                    callback();
                }
            }
        )
    });
}

function addTopics(topicSplit, newMvbDomainId) {
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