var fileSelect = jQuery("#file");
var imagesElement = jQuery("#images");

jQuery("#submitImage").click(function() {
    var username = jQuery("#username").val();
    var password = jQuery("#password").val();

    if (username && password) {
        submitImage.attr("disabled", "disabled");
        imageClose.attr("disabled", "disabled");

        login(function(myCookies){
            uploadImage(myCookies);
        });
    }
});

function displayImageModal() {
    submitImage.removeAttr("disabled");
    imageClose.removeAttr("disabled");
}

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
            //window.open('https://dz2cdn1.dzone.com/thumbnail?fid=' + data + "&w=800");

            /*
                Add the new image to the start of the list
            */
            var listItem = jQuery("<li class='imageListItem'> \
                                <label> \
                                <input type='radio' class='image' name='radgroup' value='" + data + "' id='imageId" + data + "'> \
                                <img src='https://dz2cdn1.dzone.com/thumbnail?fid=" + data + "&w=" + imageWidth + "'/> \
                                </label> \
                                </li>");
            imagesElement.prepend(listItem);

            submitImage.removeAttr("disabled");
            imageClose.removeAttr("disabled");

            /*
                Close the modal
            */
            imageClose.click();

            /*
                Associate the image with the tags in the background
            */
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
    var topicSplit = imageTopics.val().split(",");

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