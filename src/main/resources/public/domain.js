var dataPrefix = "/data";

var domain = jQuery('#domain');
var submit = jQuery('#submit');
var restartButton = jQuery('#restart');

initTags();
initAuthors();

restartButton.click(function() {
    restart();
});

submit.click(function() {
    var domainUri = URI(domain.val());
    processDomain(domainUri, processDomain);
});

function restart() {
    domain.val("");
    authors.val("");
    topic.val("");
}

function processDomain(newMvbDomainId) {
    addAuthors(authors.tagsinput('items'), newMvbDomainId);
    addTopics(topics.val().split(","), newMvbDomainId);
}





