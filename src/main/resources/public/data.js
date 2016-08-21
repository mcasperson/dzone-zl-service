
var processingCount = 0;

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

function addBackgroundProcessing() {
     ++processingCount;
     if (processingCount == 1) {
        window.onbeforeunload = function() {
            return "Some background processes are still running. Please wait for them to finish before closing the window";
        };
        spinner.show();
    }
}

function removeBackgroundProcessing() {
    --processingCount;
    processingCount = processingCount < 0 ? 0 : processingCount;

    if (processingCount === 0) {
        window.onbeforeunload = null;
        spinner.hide();
    }
}

/*
    Add the topic to the database when it is added.
    NOTE - this is disabled now that we use Alchemy to load new topics every time
*/
/*topics.on('itemAdded', function(event) {
    addBackgroundProcessing();

    var domainUri = URI(originalSource.val());
    var selectedTopics = topics.val().split(",");

    processDomain(domainUri, function(domainId) {
          addTopicsToDomain(selectedTopics, domainId, function() {
             removeBackgroundProcessing();
         });
    });
});*/

/*
    Add the author to the database when it is added
*/
authors.on('itemAdded', function(event) {
    addBackgroundProcessing();

    var domainUri = URI(originalSource.val());
    /*
        We need a copy of this object, because the authors field will be cleared,
        which in turn clears the tags that are referenced by authors.tagsinput('items').

        This is hacky, but does the job
    */
    var selectedAuthors = JSON.parse(JSON.stringify(authors.tagsinput('items')));

    processDomain(domainUri, function(domainId) {
         addAuthorsToDomain(selectedAuthors, domainId, function() {
             removeBackgroundProcessing();
             queryDomain(domainUri, function(mvdDomain) {
                 getAuthors(mvdDomain);
              });
         });
    });
});

/*
    Add the user to the posters table when the tag is added
*/
poster.on('itemAdded', function(event) {
  addBackgroundProcessing();

  var tag = event.item;

  var posterEntity =
      {
        data: {
          type: "poster",
          attributes: {
            username: tag.id,
            name: tag.name
          }
        }
      };

  jQuery.ajax({
      url: dataPrefix + '/poster',
      method: 'POST',
      xhrFields: {
          withCredentials: true
      },
      data: JSON.stringify(posterEntity),
      contentType: "application/json",
      dataType : 'json'
  }).done(function(newPoster) {
      console.log("Create a new poster " + newPoster);
      removeBackgroundProcessing();
      getPosters();
  }).error(function() {
    /*
        This is expected, as we can't save the same poster twice
    */
    removeBackgroundProcessing();
  });
});

loginButton.click(function() {
    login();
});

importButton.click(doImport);

restartButtons.click(function(){
    submitSucceeded();
});

submitButtons.click(function(){
    validateContent();
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

jQuery("#topicsInputParent > .bootstrap-tagsinput").on(
    'click',
    '.tag',
    function(event) {
        var topicName = event.target.textContent;

        var topicItems = topics.tagsinput('items');

        _.each(topicItems, function(topic) {
            if (topic && topic.title === topicName) {
                topics.tagsinput('remove', topic);
            }
        });
    }
);

jQuery("#postersInputParent  > .bootstrap-tagsinput").on(
    'click',
    '.tag',
    function(event) {
        var posterName = event.target.textContent;

        var posterItems = poster.tagsinput('items');

        var matchingPosters = _.filter(posterItems, function(poster) {
            return poster.name === posterName;
        });

        if (matchingPosters.length != 0) {
            window.open('https://dzone.com/users/' + matchingPosters[0].id + "/");
        }
    }
);

jQuery("#authorsInputParent > .bootstrap-tagsinput").on(
    'click',
    '.tag',
    function(event) {
        var authorName = event.target.textContent;

        var authorsItems = authors.tagsinput('items');

        var matchingAuthors = _.filter(authorsItems, function(author) {
            return author.name === authorName;
        });

        if (matchingAuthors.length != 0) {
            window.open('https://dzone.com/users/' + matchingAuthors[0].id + "/");
        }
    }
);

ignoreErrors.click(function() {
    submitArticle();
});

function submitArticle() {
    var content = edit.froalaEditor('html.get');
    var titleContent = title.val();
    var tldrContent = tldr.val();
    var topicsContent = topics.val();
    var author = authors.val();
    var imageId = jQuery('.image:checked').val();
    var posterContent = poster.val();
    var waitDays = daysBeforePublishing.val();
    var email = emailWhenPublishing.val();
    var citeAuthorContent = citeAuthor.val();
    var zoneContent = zone.val();

    if (!content || content.trim().length == 0) {
        window.alert("Invalid content");
        return;
    } else if (!titleContent || titleContent.trim().length == 0) {
        window.alert("Invalid title");
        return;
    }

    if (!tldrContent || tldrContent.trim().length == 0) {
        window.alert("Invalid tldr");
        return;
    }

    if(!topicsContent || topicsContent.trim().length == 0) {
        window.alert("Invalid topics");
        return;
    }

    if(!author || author.trim().length == 0) {
        window.alert("Invalid author");
        return;
    }

    if (!posterContent || posterContent.trim().length == 0) {
        window.alert("Invalid poster");
        return;
    }

    if (!imageId || imageId.trim().length == 0) {
        window.alert("No image was selected.");
        return;
    }

    if (waitDays.trim.length !== 0 && isNaN(parseInt(waitDays))) {
        window.alert("Invalid days to wait before publishing");
        return;
    }

    /*
     Add some instructions at the top of the content
     */
    if (waitDays) {
        content = "<h1>Do not publish until " + moment().add(waitDays, "d").format("Do MMMM YYYY") + "</h1>" + content;
    }

    if (email) {
        content = "<h1>Email <a href='mailto:" + email + "'>" + email + " when publishing</h1>"  + content;
    }

    if (zoneContent) {
        content = "<h1>Suggested zone: " + zoneContent + "</h1>"  + content;
    }

    if (citeAuthorContent) {
        content += "<p>Original article by " + citeAuthorContent + "</p>";
    }

    edit.froalaEditor('edit.off');
    suggestedAuthorsList.attr("disabled", "disabled");
    posterList.attr("disabled", "disabled");
    title.attr("disabled", "disabled");
    tldr.attr("disabled", "disabled");
    zone.attr("disabled", "disabled");
    zone.val("");
    suggestedZone.html("");
    topics.attr("disabled", "disabled");
    submitButtons.attr("disabled", "disabled");
    restartButtons.attr("disabled", "disabled");
    authors.attr("disabled", "disabled");
    poster.attr("disabled", "disabled");
    emailWhenPublishing.attr("disabled", "disabled");
    daysBeforePublishing.attr("disabled", "disabled");
    citeAuthor.attr("disabled", "disabled");

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
            poster: posterContent,
            tldr: tldrContent

        }
    }).done(function(importedPost) {
        if (importedPost.success) {
            submitSucceeded(importedPost);
        } else {
            submitFailed(importedPost);
        }
    }).error(function(){
        submitFailed();
    });
}

function validateContent() {
    var contentHtml = edit.froalaEditor('html.get');

    $.ajax({
        type: "POST",
        url: "/action/htmlToText",
        data: contentHtml,
        dataType: "text",
        contentType: "text/plain",
        success: function(text) {
            var newRules = rules.slice( 0 );
            newRules.unshift(function(callback) {
                callback(null, text, "");
            });

            async.waterfall(
                newRules,
                function(err, text, error) {
                    if (error !== "") {
                        styleGuideViolations.html(error.replace(/\n/g, "<br/>"));
                        styleGuideViolationsModal.modal();
                    } else {
                        submitArticle();
                    }
                }
            );
        },
        error: function() {
            submitArticle();
        }
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
    if (domainInfo && domainInfo.included) {
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
}

function getTags(domainInfo, url) {
    topics.val("");

    /*
        Use a machine learning API to extract the keywords
     */
    jQuery.get(actionPrefix + "/getKeywords?url=" + encodeURIComponent(url), function(dzoneTopics) {

        _.each(dzoneTopics, function(topic) {
            topics.tagsinput('add', {title: topic});
        });


    }).error(
        /*
            Fall back to loading them from the database
         */
        function() {
            getTagsFromDB(domainInfo)
        }
    )
}

function getTagsFromDB(domainInfo) {
    if (domainInfo && domainInfo.included) {
        _.each(domainInfo.included, function(included) {
            if (included.type == "tag") {
                topics.tagsinput('add', {title: included.attributes.name});
            }
        });
    }
}

function getImages(domainInfo) {
    if (domainInfo && domainInfo.included) {
        _.each(domainInfo.included, function(included) {
            if (included.type == "image") {

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
}

function titleCaps(title) {
    var small = "(a|an|and|as|at|but|by|en|for|if|in|it|its|nor|me|of|on|or|so|the|to|up|yet|v[.]?|via|vs[.]?)";
    var punct = "([!\"#$%&'()*+,./:;<=>?@[\\\\\\]^_`{|}~-]*)";
    var parts = [], split = /[:.;?!] |(?: |^)["Ò]/g, index = 0;
    title = lower(title);
    while (true) {
        var m = split.exec(title);
        parts.push( title.substring(index, m ? m.index : title.length)
            .replace(/\b([A-Za-z][a-z.'Õ]*)\b/g, function(all){
                return /[A-Za-z]\.[A-Za-z]/.test(all) ? all : upper(all);
            })
            .replace(RegExp("\\b" + small + "\\b", "ig"), lower)
            .replace(RegExp("^" + punct + small + "\\b", "ig"), function(all, punct, word){
                return punct + upper(word);
            })
            .replace(RegExp("\\b" + small + punct + "$", "ig"), upper));

        index = split.lastIndex;

        if ( m ) parts.push( m[0] );
        else break;
    }

    return parts.join("").replace(/ V(s?)\. /ig, " v$1. ")
        .replace(/(['Õ])S\b/ig, "$1s")
        .replace(/\b(AT&T|Q&A)\b/ig, function(all){
            return all.toUpperCase();
        });
}

function lower(word){
    return word.toLowerCase();
}

function upper(word){
    return word.substr(0,1).toUpperCase() + word.substr(1);
}

function importSucceeded(url, content, articleTitle) {

    title.val(titleCaps(articleTitle));

    edit.froalaEditor('html.set', content);
    /*
     Once the editor has made it's own modifications, reset the images
     */
    edit.froalaEditor('html.set', setImagesToBreakText( edit.froalaEditor('html.get')));

    edit.froalaEditor('edit.on');

    jQuery.get(
        dataPrefix + "/article?filter[article.source]=" + encodeURIComponent(originalSource.val()),
        function(sourceUrls) {
            if (sourceUrls.data.length !== 0) {
                alert("WARNING!!! The article your are importing has already been processed, and if you proceed" +
                " you will most likely import duplicated content.");
            }
        }
    );

    queryDomain(url, function(domainInfo) {
        if (domainInfo.data.length == 0) {
            alert("There was no matching information in the database for this domain");
        }

        getAuthors(domainInfo);
        getTags(domainInfo, url);
        getImages(domainInfo);

        /*
            We can't do this with an empty domain
        */
        if (domainInfo && domainInfo.data && domainInfo.data.length !== 0) {
            daysBeforePublishing.val(domainInfo.data[0].attributes.daysBeforePublishing);
            emailWhenPublishing.val(domainInfo.data[0].attributes.emailWhenPublishing);
            emailWhenPublishing.val(domainInfo.data[0].attributes.emailWhenPublishing);
        }

        emailWhenPublishing.removeAttr("disabled");
        daysBeforePublishing.removeAttr("disabled");
        suggestedAuthorsList.removeAttr("disabled");
        posterList.removeAttr("disabled");
        title.removeAttr("disabled");
        tldr.removeAttr("disabled");
        zone.removeAttr("disabled");
        topics.removeAttr("disabled");
        submitButtons.removeAttr("disabled");
        restartButtons.removeAttr("disabled");
        authors.removeAttr("disabled");
        poster.removeAttr("disabled");
        citeAuthor.removeAttr("disabled");
    });

    classifyContent(articleTitle + " " + content);
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

function submitFailed(data) {
    /*
        Attempt to provide some meaningful feedback
    */
    if (data && data.errors) {
        if (data.errors.topics) {
           alert(data.errors.topics);
        } else {
            alert(JSON.stringify(data.errors));
        }
    } else {
        alert("Submission of the post failed");
    }

    edit.froalaEditor('edit.on');
    suggestedAuthorsList.removeAttr("disabled");
    posterList.removeAttr("disabled");
    title.removeAttr("disabled");
    tldr.removeAttr("disabled");
    zone.removeAttr("disabled");
    topics.removeAttr("disabled");
    submitButtons.removeAttr("disabled");
    restartButtons.removeAttr("disabled");
    authors.removeAttr("disabled");
    poster.removeAttr("disabled");
    emailWhenPublishing.removeAttr("disabled");
    daysBeforePublishing.removeAttr("disabled");
}

function submitSucceeded(submittedPost) {

    edit.froalaEditor('html.set', "<p/>");
    edit.froalaEditor('edit.off');

    suggestedAuthorsList.attr("disabled", "disabled");
    posterList.attr("disabled", "disabled");
    title.attr("disabled", "disabled");
    tldr.attr("disabled", "disabled");
    zone.attr("disabled", "disabled");
    zone.val("");
    suggestedZone.html("");
    submitButtons.attr("disabled", "disabled");
    restartButtons.attr("disabled", "disabled");
    poster.attr("disabled", "disabled");
    importButton.removeAttr("disabled");
    originalSource.removeAttr("disabled");

    authors.attr("disabled", "disabled");
    topics.attr("disabled", "disabled");

    emailWhenPublishing.attr("disabled", "disabled");
    daysBeforePublishing.attr("disabled", "disabled");

    topics.tagsinput('removeAll');
    authors.tagsinput('removeAll');

    var imagesElement = jQuery("#images");
    imagesElement.html("");
    getAllImages();

    if (submittedPost) {
        window.open("https://dzone.com/articles/" + submittedPost.result.article.plug + "?preview=true");
        window.open("https://dzone.com/content/" + submittedPost.result.article.id + "/edit.html");
    }
}

/**
/**
 * Inline images have the class fr-dii. Line break images have the class fr-dib
 */
function setImagesToBreakText(content) {
    return content.replace(/(<img(?:.*?)class=".*?)(fr-dii)"/g, "$1fr-dib\"");
}

/**
 * Open up pixabay using the current tags as a search criteria
 */
function openImageSearch() {

    /*
        Pixabay only searches on about 9 phrases
     */
    var maxSearchTerms = 5;

    var topicsContent = topics.val();
    if (!topicsContent.trim()) {
        alert("No topics selected!");
        return;
    }
	
	var topicsSplit = topicsContent.split(",");
	var topicsSearch = "";

	for (var i = 0; i < Math.min(topicsSplit.length, maxSearchTerms); ++i) {
		if (topicsSearch != "") {
			topicsSearch += " or ";
		}

        var myTopicSplit = topicsSplit[i];

        if (myTopicSplit.indexOf(" ") !== -1) {
            topicsSearch += "\"" + topicsSplit[i] + "\"";
        } else {
            topicsSearch += topicsSplit[i];
        }


	}

    window.open("https://pixabay.com/en/photos/?image_type=&cat=&min_width=&min_height=&q=" + encodeURIComponent(topicsSearch));
}


