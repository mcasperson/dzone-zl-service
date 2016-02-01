var topic = jQuery('#topic');
var addtopic = jQuery('#addtopic');
var topiclist = jQuery("#topiclist");
var fileSelect = jQuery("#file");

jQuery("#reset").click(function() {
    topiclist.html("");
    topic.val("");
});

jQuery("#submit").click(function() {
    var username = jQuery("#username").val();
    var password = jQuery("#password").val();

    if (username && password) {
        login(function(myCookies){
            uploadImage(myCookies);
        });
    }
});

/**
 * http://uncorkedstudios.com/blog/multipartformdata-file-upload-with-angularjs
 */
function uploadImage(myCookies) {

    var file = fileSelect[0].files[0];

    var fd = new FormData();
    fd.append('file', file);

    jQuery.ajax({
        type: 'post',
        url: actionPrefix + '/uploadImage?awselbCookie=' + cookies.AWSELB +
            "&thCsrfCookie=" + cookies.TH_CSRF +
            "&springSecurityCookie=" + cookies.SPRING_SECURITY_REMEMBER_ME_COOKIE +
            "&jSessionIdCookie=" +  cookies.JSESSIONID,
        data: fd,
        cache: false,
        contentType: false,
        processData: false,
        type: 'POST',
        success: function(data){
            console.log(data);
            window.open('https://dz2cdn1.dzone.com/thumbnail?fid=' + data + "&w=800");

            addImage(data, function(newImage) {
                addTopics(newImage.data.id);
            });
        }
    });
}

addtopic.click(function() {
    jQuery.get('https://dzone.com/services/internal/data/topics-search?term=' + topic.val(), function(topicdata) {
        if (topicdata.success) {
            topiclist.append(jQuery('<option value="' + topicdata.result.data[0].id + '">' +  topicdata.result.data[0].title + "</option>"));
            topic.val("");
        }
    });
});

function addTopics(imageId) {
    $("#topiclist > option").each(function() {

        var name = this.text;

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
                                data: JSON.stringify(tag),
                                contentType: "application/json",
                                dataType : 'json'
                            }).done(function(newTag) {
                                console.log(JSON.stringify(newTag));
                                addTopicToImage(imageId, newTag.data.id);
                            });
                    } else {
                        addTopicToImage(imageId, existingTags.data[0].id);
                    }
                }
        );


    });
}

function addImage(imageId, success) {

    var image =
        {
            data: {
                type: "image",
                attributes: {
                    dzoneId: imageId
                }
            }
        };

    jQuery.ajax({
            url: dataPrefix + '/image',
            method: 'POST',
            data: JSON.stringify(image),
            contentType: "application/json",
            dataType : 'json'
        }).done(function(newImage) {
            console.log(JSON.stringify(newImage));
            success(newImage);
        });
}

function addTopicToImage(imageId, newTagId) {

    var tagToImage =
        {
            data: {
                type: "tagToImage",
                relationships: {
                    image: {
                        data: {
                              type: "image",
                              id: imageId
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
            url: dataPrefix + '/tagToImage',
            method: 'POST',
            data: JSON.stringify(tagToImage),
            contentType: "application/json",
            dataType : 'json'
        }).done(function(newTagToMVBDomain) {
            console.log(JSON.stringify(newTagToMVBDomain));
        });
}