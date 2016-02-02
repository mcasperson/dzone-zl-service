var fileSelect = jQuery("#file");
var submit = jQuery("#submit");

submit.click(function() {
    var file = fileSelect[0].files[0];
    if (!file) {
        return;
    }

    var reader = new FileReader();
    reader.onload = function(e) {
        var contents = e.target.result;
        processText(contents);
    };
    reader.readAsText(file);
});

function processText(contents) {
    var lines = contents.split("\n");

    async.eachSeries(
        lines,
        function(line, lineCallback) {
            var match = /^\s*<outline type="rss" text="(.*?)".*?htmlUrl="(.*?)".*?$/.exec(line)
            if (match) {
                var title = match[1];
                var url = match[2];

                var domainUri = URI(url);

                processDomain(domainUri, function(domainId) {
                    /*
                        Split up the title, and remove empty strings
                    */
                    var titleSplit = title.split(/[^A-Za-z0-9@.]/).filter(function(n){ return n.trim().length !== 0 }); ;
                    var authorCount = 0;

                    processTitle(domainUri, domainId, titleSplit, function(authorCount) {
                       if (authorCount != 0) {
                           async.setImmediate(function () {
                               lineCallback();
                           });
                       } else {
                            /*
                                try combining two individual words to create user names
                                if matching individual elements of the title didn't match
                                any users.
                            */
                            var combinedTitleSplit = [];
                            for (var titleSplitIndex = 0; titleSplitIndex < titleSplit.length - 1; ++titleSplitIndex) {
                                combinedTitleSplit.push(titleSplit[titleSplitIndex] + ' ' + titleSplit[titleSplitIndex + 1]);
                            }

                            processTitle(domainUri, domainId, combinedTitleSplit, function() {
                                async.setImmediate(function () {
                                    lineCallback();
                                });
                            });
                       }
                    });
                });
            } else {
                async.setImmediate(function () {
                    lineCallback();
                });
            }
        },
        function(err){
            console.log("Finished importing");
        }
    );
}

function processTitle(domainUri, domainId, titleSplit, lineCallback) {
    var authorCount = 0;

    async.eachSeries(
        titleSplit,
        function(titleElement, authorCallback) {
            if (titleElement.trim().length !== 0) {
                jQuery.get('https://dzone.com/services/widget/article-postV2/searchAuthors?q=' + titleElement, function(authors) {
                    if (authors.result.data.length === 1) {
                        ++authorCount;
                        var authorId = authors.result.data[0].id;
                        addAuthorsToDomain(authors.result.data, domainId, function() {
                            async.setImmediate(function () {
                                authorCallback();
                            });
                        });

                    } else {
                       async.setImmediate(function () {
                            authorCallback();
                        });
                    }
                });
            } else {
                async.setImmediate(function () {
                    authorCallback();
                });
            }
        },
        function(err){
           console.log("Finished processing " + authorCount + " authors for " + domainUri.hostname());
           lineCallback(authorCount);
        }
    );
}