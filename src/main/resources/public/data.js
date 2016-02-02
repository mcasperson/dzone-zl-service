

/*
    Perform some tasks at startup
*/
initTags();
initAuthors();
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
                                <label> \
                                <input type='radio' class='image' name='radgroup' value='" + random + "' id='imageId" + random + "'> \
                                <img src='https://dz2cdn1.dzone.com/thumbnail?fid=" + random + "&w=" + imageWidth + "'/> \
                                </label> \
                                </li>");
            imagesElement.append(listItem);
        }
    }

    /*
        Get any images we entered into the database
    */
    jQuery.get(dataPrefix + "/image", function(images) {

        _.each(images.data, function(image) {
            var listItem = jQuery("<li class='imageListItem'> \
                                <label> \
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
        xhrFields: {
            withCredentials: true
        },
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

jQuery("#restart, #restartTop").click(function(){
    submitSucceeded();
});

jQuery("#submit, #submitTop").click(function(){

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
    jQuery("#submit, #submitTop").attr("disabled", "disabled");
    jQuery("#restart, #restartTop").attr("disabled", "disabled");
    jQuery("#authors").attr("disabled", "disabled");
    jQuery("#poster").attr("disabled", "disabled");

    jQuery.ajax({
            url: actionPrefix + "/submit",
            method: "POST",
            xhrFields: {
                withCredentials: true
            },
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
    var authorEntry = jQuery(event.target);
    authors.tagsinput('add', {name: authorEntry.data('username'), id: authorEntry.data("userid")});
});

/*
    Any new authors or topics that were defined for this post will be saved in the database
*/
function saveNewAuthorsAndTags() {
        var domainUri = URI(jQuery('#originalSource').val());
        /*
            We need a copy of this object, because the authors field will be cleared,
            which in turn clears the tags that are referenced by authors.tagsinput('items').

            This is hacky, but does the job
        */
        var selectedAuthors = JSON.parse(JSON.stringify(authors.tagsinput('items')));
        var selectedTopics = topics.val().split(",");

        processDomain(domainUri, function(domainId) {
            addAuthorsToDomain(selectedAuthors, domainId);
            addTopicsToDomain(selectedTopics, domainId);
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
            authorsList.append(jQuery("<li><a href='#' class='authorEntry' data-username='" + included.attributes.name + "' data-userid='" + included.attributes.username + "'>" + included.attributes.name + "</a></li>"));
        }
    });

    /*
        Where there is only 1 author, add it automatically
    */
    if (count == 1) {
        var authorEntry = jQuery(".authorEntry");
        authors.tagsinput('add', {name: authorEntry.data('username'), id: authorEntry.data("userid")});
    }
}

function getTags(domainInfo) {
    var topics = jQuery("#topics");
    topics.val("");
    _.each(domainInfo.included, function(included) {
        if (included.type == "tag") {
            topics.tagsinput('add', {title: included.attributes.name});
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
        jQuery("#submit, #submitTop").removeAttr("disabled");
        jQuery("#restart, #restartTop").removeAttr("disabled");
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
    jQuery("#submit, #submitTop").removeAttr("disabled");
    jQuery("#restart, #restartTop").removeAttr("disabled");
    jQuery("#authors").removeAttr("disabled");
    jQuery("#poster").removeAttr("disabled");
}

function submitSucceeded(submittedPost) {
    saveNewAuthorsAndTags();

    jQuery("#edit").froalaEditor('html.set', "<p/>");
    jQuery("#edit").froalaEditor('edit.off');

    jQuery("#suggestedAuthorsList").attr("disabled", "disabled");
    jQuery("#title").attr("disabled", "disabled");
    jQuery("#submit, #submitTop").attr("disabled", "disabled");
    jQuery("#restart, #restartTop").attr("disabled", "disabled");
    jQuery("#poster").attr("disabled", "disabled");
    jQuery("#import").removeAttr("disabled");
    jQuery("#originalSource").removeAttr("disabled");

    authors.attr("disabled", "disabled");
    topics.attr("disabled", "disabled");

    topics.tagsinput('removeAll');
    authors.tagsinput('removeAll');

    var imagesElement = jQuery("#images");
    imagesElement.html("");
    getAllImages();

    window.open("https://dzone.com/articles/" + submittedPost.result.article.plug + "?preview=true");
    window.open("https://dzone.com/content/" + submittedPost.result.article.id + "/edit.html");
}


