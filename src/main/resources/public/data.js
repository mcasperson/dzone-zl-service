var imageWidth = 250;
var dataPrefix = "/data";
var actionPrefix = "/action";
var cookies = null;
var randomImages = 250;
var randomImageStartIndex = 900000;
var randomImageRange = 100000;

/*
    Perform some tasks at startup
*/
getAllImages();
populateImportUrl();
quickLoad();

function getParameterByName(name) {
    name = name.replace(/[\[]/, "\\[").replace(/[\]]/, "\\]");
    var regex = new RegExp("[\\?&]" + name + "=([^&#]*)"),
        results = regex.exec(location.search);
    return results === null ? "" : decodeURIComponent(results[1].replace(/\+/g, " "));
}

/*
    We can skip the login if we have the cookies ready to go
*/
function quickLoad() {
    var username = jQuery("#username").val();
    var password = jQuery("#password").val();

    if (username && password) {
        login(function(){
            /*
                If we have a import url, start the process
            */
            if (jQuery('#originalSource').val()) {
                doImport();
            }
        });
    }
}

function populateImportUrl() {
    var importUrl = getParameterByName('importUrl');
    if (importUrl) {
        jQuery('#originalSource').val(importUrl);
    }
}

function getAllImages() {
    var imagesElement = jQuery("#images");
    imagesElement.html("");

    /*
        Randomly select 100 images from some of the more recent posts
    */
    for (var count = 0; count < randomImages; ++count) {
        var random = Math.floor(Math.random() * randomImageRange + randomImageStartIndex);
        if (jQuery("#imageId" + random).length == 0) {
            var listItem = jQuery("<li class='imageListItem'> \
                                <input type='radio' class='image' name='radgroup' value='" + random + "' id='imageId" + random + "'> \
                                <img src='https://dz2cdn1.dzone.com/thumbnail?fid=" + random + "&w=" + imageWidth + "'/> \
                                </label> \
                                </li>");
            imagesElement.append(listItem);
            imagesElement.show();
        }
    }

    /*
        Get any images we entered into the database
    */
    jQuery.get(dataPrefix + "/image", function(images) {

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

function doImport() {
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
        if (importedPost.success &&
            importedPost.result.data.htmlContent &&
            importedPost.result.data.htmlContent.trim().length != 0) {

            importSucceeded(url, importedPost.result.data.htmlContent, importedPost.result.data.title);

        } else {
            importFailed(url);
        }
    }).error(function(){
        importFailed(url);
    });
}

jQuery("#login").click(function() {
    login();
});

jQuery("#import").click(doImport);

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
                submitSucceeded(importedPost);
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
        data: {username: jQuery("#username").val(), password: jQuery("#password").val()}
    }).done(function(myCookies) {
        cookies = myCookies;

        jQuery("#import").removeAttr("disabled");
        jQuery("#originalSource").removeAttr("disabled");

        if (success) {
            success();
        }
    }).error(function() {
        jQuery("#login").removeAttr("disabled");
        jQuery("#username").removeAttr("disabled");
        jQuery("#password").removeAttr("disabled");
    });
}

function queryDomain(domain, success) {
    var hostname = URI(domain).hostname();
    jQuery.get(
        dataPrefix + "/mvbDomain?include=authors,tagToMvbdomains.tag.tagToImages.image&filter[mvbDomain.domain]=" + hostname,
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
    var imagesElement = jQuery("#images");
    var count = 0;
    _.each(domainInfo.included, function(included) {
        if (included.type == "image") {
            ++count;
            var image = jQuery("#imageId" + included.attributes.dzoneId);
            /*
                Select the image
            */
            image.prop("checked", true);
            /*
                Move it to the start of the list
            */
            image.parent().remove();
            imagesElement.prepend(image.parent());
        }
    });

    /*if (count != 0) {
        var images = jQuery(".imageListItem > input");
        _.each(images, function(image) {
            var imageElement = jQuery(image);
            if (imageElement.prop("checked") == false) {
                imageElement.parent().remove();
            }
        });
    }*/
}

function importSucceeded(url, content, title) {
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

        jQuery("#edit").froalaEditor('html.set', content);
        jQuery("#title").val(title);

        jQuery("#edit").froalaEditor('edit.on');
    });
}

function importFailed(url) {
    var continueImport = confirm("Import process failed. Do you wish to continue?");
    if (!continueImport) {
        jQuery("#import").removeAttr("disabled");
        jQuery("#originalSource").removeAttr("disabled");
    } else {
        importSucceeded(url, '');
    }
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

function submitSucceeded(submittedPost) {
    jQuery("#edit").froalaEditor('html.set', "<p/>");
    jQuery("#edit").froalaEditor('edit.off');

    jQuery("#suggestedAuthorsList").attr("disabled", "disabled");
    jQuery("#title").attr("disabled", "disabled");
    jQuery("#topics").attr("disabled", "disabled");
    jQuery("#submit").attr("disabled", "disabled");
    jQuery("#restart").attr("disabled", "disabled");
    jQuery("#authors").attr("disabled", "disabled");
    jQuery("#poster").attr("disabled", "disabled");

    jQuery("#import").removeAttr("disabled");
    jQuery("#originalSource").removeAttr("disabled");

    var imagesElement = jQuery("#images");
    imagesElement.html("");
    getAllImages();

    window.open("https://dzone.com/articles/" + submittedPost.result.article.plug + "?preview=true");
    window.open("https://dzone.com/content/" + submittedPost.result.article.id + "/edit.html");
}


