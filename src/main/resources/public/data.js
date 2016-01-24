var imageWidth = 250;
var dataPrefix = "/data";
var actionPrefix = "/action";
var cookies = null;

function getAllImages() {
    jQuery.get(dataPrefix + "/image", function(images) {
        var imagesElement = jQuery("#images");
        imagesElement.html("");
        _.each(images.data, function(image) {
            var listItem = jQuery("<li class='imageListItem'> \
                                <input type='radio' class='image' name='radgroup' value='" + image.attributes.dzoneId + "' id='imageId" + image.attributes.dzoneId + "'> \
                                <img src='https://dz2cdn1.dzone.com/thumbnail?fid=" + image.attributes.dzoneId + "&w=" + imageWidth + "'/> \
                                </label> \
                                </li>");
            imagesElement.append(listItem);
            imagesElement.show();
        });
    });
}

jQuery("#login").click(function() {
    /*
        Disable UI while we perform the login
    */
    jQuery("#login").attr("disabled", "disabled");
    jQuery("#username").attr("disabled", "disabled");
    jQuery("#password").attr("disabled", "disabled");

    jQuery.ajax({
        method: "POST",
        url: actionPrefix + "/login",
        data: {username: jQuery("#username").val(), password: jQuery("#password").val()}
    }).done(function(myCookies) {
        cookies = myCookies;
        jQuery("#import").removeAttr("disabled");
        jQuery("#originalSource").removeAttr("disabled");
    }).error(function() {
        jQuery("#login").removeAttr("disabled");
        jQuery("#username").removeAttr("disabled");
        jQuery("#password").removeAttr("disabled");
    });
});

jQuery("#import").click(function() {

    var url = jQuery("#originalSource").val();

    if (url.trim().length == 0) {
        alert("The url is invalid");
        return;
    }

    jQuery("#import").attr("disabled", "disabled");
    jQuery("#originalSource").attr("disabled", "disabled");

    jQuery.ajax({
        url: actionPrefix + "/import",
        method: "POST",
        data: {
            awselbCookie: cookies.AWSELB,
            thCsrfCookie: cookies.TH_CSRF,
            springSecurityCookie: cookies.SPRING_SECURITY_REMEMBER_ME_COOKIE,
            jSessionIdCookie: cookies.JSESSIONID,
            url: url
        }
    }).done(function(importedPost) {
        if (importedPost.success) {


            queryDomain(url, function(domainInfo) {
                if (domainInfo.data.length == 0) {
                    alert("There was no matching information in the database for this domain");
                }

                getAuthors(domainInfo);
                getTags(domainInfo);
                getImages(domainInfo);

                jQuery("#suggestedAuthorsList").removeAttr("disabled");
                jQuery("#title").removeAttr("disabled");
                jQuery("#topics").removeAttr("disabled");
                jQuery("#submit").removeAttr("disabled");
                jQuery("#restart").removeAttr("disabled");
                jQuery("#authors").removeAttr("disabled");
                jQuery("#poster").removeAttr("disabled");

                jQuery("#edit").froalaEditor('html.set', importedPost.result.data.fullContent);
                jQuery("#title").val(importedPost.result.data.title);

                jQuery("#edit").froalaEditor('edit.on');
            });
        } else {
            importFailed();
        }
    }).error(function(){
        importFailed();
    });

});

jQuery("#restart").click(function(){
    submitSucceeded();
});

jQuery("#submit").click(function(){

    var content = jQuery("#edit").froalaEditor('html.get');
    var title = jQuery("#title").val();
    var topics = jQuery("#topics").val();
    var author = jQuery("#authors").val();
    var imageId = jQuery('.image:checked').val();
    var poster = jQuery('#poster').val();

    if (!content || content.trim().length == 0 ||
        !title || title.trim().length == 0 ||
        !topics || topics.trim().length == 0 ||
        !author || author.trim().length == 0 ||
        !poster || poster.trim().length == 0 ||
        !imageId  || imageId.trim().length == 0) {
        window.alert("Invalid content, title, author, image or topics");
        return;
    }

    jQuery("#edit").froalaEditor('edit.off');
    jQuery("#suggestedAuthorsList").attr("disabled", "disabled");
    jQuery("#title").attr("disabled", "disabled");
    jQuery("#topics").attr("disabled", "disabled");
    jQuery("#submit").attr("disabled", "disabled");
    jQuery("#restart").attr("disabled", "disabled");
    jQuery("#authors").attr("disabled", "disabled");
    jQuery("#poster").attr("disabled", "disabled");

    jQuery.ajax({
            url: actionPrefix + "/submit",
            method: "POST",
            data: {
                awselbCookie: cookies.AWSELB,
                thCsrfCookie: cookies.TH_CSRF,
                springSecurityCookie: cookies.SPRING_SECURITY_REMEMBER_ME_COOKIE,
                jSessionIdCookie: cookies.JSESSIONID,
                url: jQuery("#originalSource").val(),
                content: content,
                title: title,
                topics: topics,
                authors: author,
                image: imageId,
                poster: poster

            }
        }).done(function(importedPost) {
            if (importedPost.success) {
                submitSucceeded();
            } else {
                submitFailed();
            }
        }).error(function(){
            submitFailed();
        });
});

jQuery("body").on("click", ".authorEntry", function(event) {
    jQuery("#authors").val(jQuery(event.target).data("userid"));
});


function queryDomain(domain, success) {
    var hostname = URI(domain).hostname();
    jQuery.get(
        dataPrefix + "/mvbDomain?include=authors,tagToMvbdomains.tag.tagToImages.image&filter[mvbDomain.domain]=apievangelist.com",
        success
    );
}

function getAuthors(domainInfo) {
    var authors = jQuery("#authors");
    var authorsList = jQuery("#suggestedAuthors");
    authorsList.html("");
    var count = 0;
    _.each(domainInfo.included, function(included) {
        if (included.type == "author") {
            ++count;
            authorsList.append(jQuery("<li><a href='#' class='authorEntry' data-userid='" + included.attributes.username + "'>" + included.attributes.name + "</a></li>"));
        }
    });

    /*
        Where there is only 1 author, add it automatically
    */
    if (count == 1) {
        authors.val(jQuery(".authorEntry").data("userid"));
    }
}

function getTags(domainInfo) {
    var topics = jQuery("#topics");
    topics.val("");
    _.each(domainInfo.included, function(included) {
        if (included.type == "tag") {
            if (topics.val().trim().length != 0) {
                topics.val(topics.val() + ", ");
            }
            topics.val(topics.val() + included.attributes.name);
        }
    });
}

function getImages(domainInfo) {
    var count = 0;
    _.each(domainInfo.included, function(included) {
        if (included.type == "image") {
            ++count;
            jQuery("#imageId" + included.attributes.dzoneId).prop("checked", true);
        }
    });

    if (count != 0) {
        var images = jQuery(".imageListItem > input");
        _.each(images, function(image) {
            var imageElement = jQuery(image);
            if (imageElement.prop("checked") == false) {
                imageElement.parent().remove();
            }
        });
    }
}

function importFailed() {
    jQuery("#import").removeAttr("disabled");
    jQuery("#originalSource").removeAttr("disabled");
}

function submitFailed() {
    alert("Submission of the post failed");

    jQuery("#edit").froalaEditor('edit.on');
    jQuery("#suggestedAuthorsList").removeAttr("disabled");
    jQuery("#title").removeAttr("disabled");
    jQuery("#topics").removeAttr("disabled");
    jQuery("#submit").removeAttr("disabled");
    jQuery("#restart").removeAttr("disabled");
    jQuery("#authors").removeAttr("disabled");
    jQuery("#poster").removeAttr("disabled");
}

function submitSucceeded() {
    jQuery("#edit").froalaEditor('html.set', "<p/>");
    jQuery("#import").removeAttr("disabled");
    jQuery("#originalSource").removeAttr("disabled");
    imagesElement.html("");
    getAllImages();
}

getAllImages();


