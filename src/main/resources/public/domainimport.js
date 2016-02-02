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
    _.each(lines, function(line) {
        var match = /^\s*<outline type="rss" text="(.*?)".*?xmlUrl="(.*?)".*?$/.exec(line)
        if (match) {
            var title = match[1];
            var url = match[2];

            var domainUri = URI(url);

            processDomain(domainUri, function(domainId) {
                var titleSplit = title.split(/[^A-Za-z0-9@.]/);

                _.each(titleSplit, function(titleElement) {
                    if (titleElement.trim().length !== 0) {
                        jQuery.get('https://dzone.com/services/widget/article-postV2/searchAuthors?q=' + titleElement, function(authors) {
                            if (authors.result.data.length === 1) {
                                var authorId = authors.result.data[0].id;
                                addAuthors(authors.result.data, domainId);
                            }
                        });
                    }
                });
            });
        }
    });
}