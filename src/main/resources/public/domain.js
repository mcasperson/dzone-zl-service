var dataPrefix = "/data";

var domain = jQuery('#domain');

var user = jQuery('#user');
var adduser = jQuery('#adduser');
var userlist = jQuery("#userlist");

var topic = jQuery('#topic');
var addtopic = jQuery('#addtopic');
var topiclist = jQuery("#topiclist");

var image = jQuery('#image');
var addimage = jQuery('#addimage');
var imagelist = jQuery("#imagelist");

var submit = jQuery('#submit');
var restart = jQuery('#restart');

adduser.click(function() {
    jQuery.get('https://dzone.com/services/widget/article-postV2/searchAuthors?q=' + user.val(), function(userdata) {
        if (userdata.success) {
            userlist.append(jQuery('<option value="' + userdata.result.data[0].id + '">' +  userdata.result.data[0].name + "</option>"));
            user.val("");
        }
    });
});

addtopic.click(function() {
    jQuery.get('https://dzone.com/services/internal/data/topics-search?term=' + topic.val(), function(topicdata) {
        if (topicdata.success) {
            topiclist.append(jQuery('<option value="' + topicdata.result.data[0].id + '">' +  topicdata.result.data[0].title + "</option>"));
            topic.val("");
        }
    });
});

addimage.click(function() {
    jQuery.get('https://dzone.com/services/internal/data/topics-search?term=' + topic.val(), function(topicdata) {
        if (topicdata.success) {
            imagelist.append(jQuery('<option value="' + image.val() + '">' +  image.val() + "</option>"));
            image.val("");
        }
    });
});

submit.click(function() {
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
                          domain: domain.val(),
                        }
                      }
                    };

                jQuery.ajax({
                    url: 'http://localhost:8090' + dataPrefix + '/mvbDomain',
                    method: 'POST',
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

function processDomain(newMvbDomainId) {
    addAuthors(newMvbDomainId);
    //addTopics(newMvbDomainId);
}

function addAuthors(newMvbDomainId) {
    $("#userlist > option").each(function() {

        var author =
                {
                  data: {
                    type: "author",
                    attributes: {
                      name: this.text,
                      username: this.value
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
                data: JSON.stringify(author),
                contentType: "application/json",
                dataType : 'json'
            }).done(function(newAuthor) {
                console.log(JSON.stringify(newAuthor));
            });
    });
}

function addTopics(newMvbDomain) {
    $("#topiclist > option").each(function() {

        var tag =
                {
                  data: {
                    type: "tag",
                    attributes: {
                      name: this.text
                    }
                  }
                };

        jQuery.ajax({
                url: dataPrefix + '/tag',
                method: 'POST',
                data: JSON.stringify(tag),
                contentType: "application/json",
                dataType : 'json'
            }).done(function(newTag) {
                console.log(JSON.stringify(newTag));
                addTopicToDomain(newMvbDomain, newTag);
            });
    });
}

function addTopicToDomain(newMvbDomainId, newTag) {

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
                              id: newTag.data.id
                        }
                    }
                }
            }
        };

    jQuery.ajax({
            url: dataPrefix + '/tagToMVBDomain',
            method: 'POST',
            data: JSON.stringify(tagToMVBDomain),
            contentType: "application/json",
            dataType : 'json'
        }).done(function(newTagToMVBDomain) {
            console.log(JSON.stringify(newTagToMVBDomain));
        });
}