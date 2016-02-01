var dataPrefix = "/data";

var domain = jQuery('#domain');
var submit = jQuery('#submit');
var restartButton = jQuery('#restart');

initTags();
initAuthors();

restartButton.click(function() {
    restart();
});

submit.click(function() {
    var domainUri = URI(domain.val());

    /*
        Does this domain already exist?
    */
    jQuery.get(
        dataPrefix + "/mvbDomain?include=authors,tagToMvbdomains.tag.tagToImages.image&filter[mvbDomain.domain]=" + domain.val(),
        function(existingDomains) {
            if (existingDomains.data.length != 0) {
                /*
                    Domain does exist, so add to it
                */
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
                    processDomain(newMvbDomain.data.id);
                });
            }
        }
    );


});

function restart() {
    domain.val("");
    authors.val("");
    topic.val("");
}

function processDomain(newMvbDomainId) {
    addAuthors(newMvbDomainId);
    addTopics(newMvbDomainId);
}

function addAuthors(newMvbDomainId) {
    var authorsSplit = authors.tagsinput('items');

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
                                console.log(JSON.stringify(newAuthor));
                            });
                    }
                }
        )
    });
}

function addTopics(newMvbDomainId) {
    var topicSplit = topics.val().split(",");

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