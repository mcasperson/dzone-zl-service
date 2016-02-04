

/*
    Perform some tasks at startup
*/
initTags();
initAuthors();
getPosters();
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
    /*
        Restore credentials from local storage
    */
    username.val(localStorage.getItem('username'));
    password.val(localStorage.getItem('password'));

    if (username.val() && password.val()) {
        login(function(){
            /*
                If we have a import url, start the process
            */
            if (originalSource.val()) {
                doImport();
            }
        });
    }
}

function populateImportUrl() {
    var importUrl = getParameterByName('importUrl');
    if (importUrl) {
        originalSource.val(importUrl);
    }
}

function getAllImages() {
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
    var url = originalSource.val();

    if (url.trim().length == 0) {
        alert("The url is invalid");
        return;
    }

    importButton.attr("disabled", "disabled");
    originalSource.attr("disabled", "disabled");

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
        if (importedPost.content &&
            importedPost.content.trim().length != 0) {

            importSucceeded(url, importedPost.content, importedPost.title);

        } else {
            importFailed(url);
        }
    }).error(function(){
        importFailed(url);
    });
}

loginButton.click(function() {
    login();
});

importButton.click(doImport);

restartButtons.click(function(){
    submitSucceeded();
});

submitButtons.click(function(){

    var content = edit.froalaEditor('html.get');
    var titleContent = title.val();
    var topicsContent = topics.val();
    var author = authors.val();
    var imageId = jQuery('.image:checked').val();
    var posterContent = poster.val();

    if (!content || content.trim().length == 0 ||
        !titleContent || titleContent.trim().length == 0 ||
        !topicsContent || topicsContent.trim().length == 0 ||
        !author || author.trim().length == 0 ||
        !posterContent || posterContent.trim().length == 0 ||
        !imageId  || imageId.trim().length == 0) {
        window.alert("Invalid content, title, author, image or topics");
        return;
    }

    edit.froalaEditor('edit.off');
    suggestedAuthorsList.attr("disabled", "disabled");
    posterList.attr("disabled", "disabled");
    title.attr("disabled", "disabled");
    topics.attr("disabled", "disabled");
    submitButtons.attr("disabled", "disabled");
    restartButtons.attr("disabled", "disabled");
    authors.attr("disabled", "disabled");
    poster.attr("disabled", "disabled");

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
                url: originalSource.val(),
                content: content,
                title: titleContent,
                topics: topicsContent,
                authors: author,
                image: imageId,
                poster: posterContent

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

jQuery("body").on("click", ".posterEntry", function(event) {
    var posterEntry = jQuery(event.target);
    poster.tagsinput('removeAll');
    poster.tagsinput('add', {name: posterEntry.data('username'), id: posterEntry.data("userid")});

    localStorage.setItem('poster', posterEntry.data("userid"));
});

jQuery("#authorsInputParent > .bootstrap-tagsinput").on('click', '.tag', function(event) {
    var authorName = event.target.textContent;

    var authorsItems = authors.tagsinput('items');

    var matchingAuthors = _.filter(authorsItems, function(author) {
        return author.name === authorName;
    });

    if (matchingAuthors.length != 0) {
        window.open('https://dzone.com/users/' + matchingAuthors[0].id + "/");
    }
});


/*
    Any new authors or topics that were defined for this post will be saved in the database
*/
function saveNewAuthorsAndTags() {
        window.onbeforeunload = function() {
            return "Some background processes are still running. Please wait for them to finish before closing the window";
        };
        spinner.show();

        var domainUri = URI(originalSource.val());
        /*
            We need a copy of this object, because the authors field will be cleared,
            which in turn clears the tags that are referenced by authors.tagsinput('items').

            This is hacky, but does the job
        */
        var selectedAuthors = JSON.parse(JSON.stringify(authors.tagsinput('items')));
        var selectedTopics = topics.val().split(",");

        processDomain(domainUri, function(domainId) {
            sync.parallel([
                function(callback){
                    addAuthorsToDomain(selectedAuthors, domainId, function() {
                        callback(null, null);
                    });

                },
                function(callback){
                    addTopicsToDomain(selectedTopics, domainId, function() {
                        callback(null, null);
                    });
                }
            ],
            // optional callback
            function(err, results) {
                window.onbeforeunload = null;
                spinner.hide();
            });
        });
}

function queryDomain(domain, success) {
    var hostname = URI(domain).hostname();
    jQuery.get(
        dataPrefix + "/mvbDomain?include=authors,tagToMvbdomains.tag.tagToImages.image&filter[mvbDomain.domain]=" + hostname,
        success
    );
}

function getPosters() {
    posters.html("");

    var savedPoster = localStorage.getItem('poster');

    jQuery.get(dataPrefix + "/poster", function(postersEntities) {
        _.each(postersEntities.data, function(posterEntity) {
            posters.append(jQuery(
                "<li><a href='#' class='posterEntry' data-username='" + posterEntity.attributes.name + "' data-userid='" + posterEntity.attributes.username + "'>" +
                posterEntity.attributes.name +
                "</a></li>"
            ));

            /*
                Non strict equality here is on purpose
            */
            if (savedPoster == posterEntity.attributes.username)  {
                poster.tagsinput('add', {name: posterEntity.attributes.name, id: posterEntity.attributes.username});
            }
        });
    });
}

function getAuthors(domainInfo) {
    authorsList.html("");
    var count = 0;
    _.each(domainInfo.included, function(included) {
        if (included.type == "author") {
            ++count;
            authorsList.append(jQuery(
                "<li><a href='#' class='authorEntry' data-username='" + included.attributes.name + "' data-userid='" + included.attributes.username + "'>" +
                included.attributes.name +
                "</a></li>"
            ));
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
    topics.val("");
    _.each(domainInfo.included, function(included) {
        if (included.type == "tag") {
            topics.tagsinput('add', {title: included.attributes.name});
        }
    });
}

function getImages(domainInfo) {
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

function importSucceeded(url, content, articleTitle) {
    queryDomain(url, function(domainInfo) {
        if (domainInfo.data.length == 0) {
            alert("There was no matching information in the database for this domain");
        }

        getAuthors(domainInfo);
        getTags(domainInfo);
        getImages(domainInfo);

        suggestedAuthorsList.removeAttr("disabled");
        posterList.removeAttr("disabled");
        title.removeAttr("disabled");
        topics.removeAttr("disabled");
        submitButtons.removeAttr("disabled");
        restartButtons.removeAttr("disabled");
        authors.removeAttr("disabled");
        poster.removeAttr("disabled");

        edit.froalaEditor('html.set', content);
        /*
            Once the editor has made it's own modifications, reset the images
        */
        edit.froalaEditor('html.set', setImagesToBreakText( edit.froalaEditor('html.get')));
        title.val(articleTitle);

        edit.froalaEditor('edit.on');
    });
}

function importFailed(url) {
    var continueImport = confirm("Import process failed. Do you wish to continue?");
    if (!continueImport) {
        importButton.removeAttr("disabled");
        originalSource.removeAttr("disabled");
    } else {
        importSucceeded(url, '');
    }
}

function submitFailed() {
    alert("Submission of the post failed");

    edit.froalaEditor('edit.on');
    suggestedAuthorsList.removeAttr("disabled");
    posterList.removeAttr("disabled");
    title.removeAttr("disabled");
    topics.removeAttr("disabled");
    submitButtons.removeAttr("disabled");
    restartButtons.removeAttr("disabled");
    authors.removeAttr("disabled");
    poster.removeAttr("disabled");
}

function submitSucceeded(submittedPost) {
    saveNewAuthorsAndTags();

    edit.froalaEditor('html.set', "<p/>");
    edit.froalaEditor('edit.off');

    suggestedAuthorsList.attr("disabled", "disabled");
    posterList.attr("disabled", "disabled");
    title.attr("disabled", "disabled");
    submitButtons.attr("disabled", "disabled");
    restartButtons.attr("disabled", "disabled");
    poster.attr("disabled", "disabled");
    importButton.removeAttr("disabled");
    originalSource.removeAttr("disabled");

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

/**
 * Inline images have the class fr-dii. Line break images have the class fr-dib
 */
function setImagesToBreakText(content) {
    return content.replace(/(<img\s+class=".*?)(fr-dii)"/g, "$1fr-dib\"");
}


