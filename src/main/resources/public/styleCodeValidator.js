var rules = [
    paasRule,
    iaasRule,
    htmlRule,
    cssRule,
    bigdataRule,
    jQueryRule,
    angularRule,
    nodejsRule,
    restfulRule,
    nosqlRule,
    sqlliteRule,
    jbossRule,
    redhatRule2,
    redhatRule
]

function paasRule(text, error, callback) {
    if (/platform\W+as\W+a\W+service/i.exec(text)) {
        callback(null, text, error + "\nUse 'Paas' not 'Platform-as-a-service'");
    } else {
        callback(null, text, error);
    }
}

function iaasRule(text, error, callback) {
    if (/infrastructure\W+as\W+a\W+service/i.exec(text)) {
        callback(null, text, error + "\nUse 'IaaS' not 'infrastructure-as-a-service'");
    } else {
        callback(null, text, error);
    }
}

function htmlRule(text, error, callback) {
    if (/html\b(?!:)/i.exec(text)) {
        callback(null, text, error + "\nUse 'HTML5' not 'HTML'");
    } else {
        callback(null, text, error);
    }
}

function cssRule(text, error, callback) {
    if (/css\b/i.exec(text)) {
        callback(null, text, error + "\nUse 'CSS3' not 'CSS'");
    } else {
        callback(null, text, error);
    }
}

function bigdataRule(text, error, callback) {
    var regex = /big data\b/ig;

    while (match = regex.exec(text)) {
        if (match[0] !== "Big Data") {
            callback(null, text, error + "\nUse 'Big Data' not 'big data'");
            return;
        }
    }

    callback(null, text, error);
}

function jQueryRule(text, error, callback) {
    var regex = /jquery/ig;

    while (match = regex.exec(text)) {
        if (match[0] !== "jQuery") {
            callback(null, text, error + "\nUse 'jQuery' not 'jquery'");
            return;
        }
    }

    callback(null, text, error);
}

function angularRule(text, error, callback) {
    if (/angular\.js/i.exec(text)) {
        callback(null, text, error + "\nUse 'AnguralJS or Angular' not 'Angular.js'");
    } else {
        callback(null, text, error);
    }
}

function nodejsRule(text, error, callback) {
    var regex = /Node\.js/ig;

    while (match = regex.exec(text)) {
        if (match[0] !== "Node.js") {
            callback(null, text, error + "\nUse 'Node.js' not 'node.js'");
            return;
        }
    }

    callback(null, text, error);
}

function restfulRule(text, error, callback) {
    var regex = /restful/ig;

    while (match = regex.exec(text)) {
        if (match[0] !== "RESTful") {
            callback(null, text, error + "\nUse 'RESTful' not 'restful'");
            return;
        }
    }

    callback(null, text, error);
}

function nosqlRule(text, error, callback) {
    var regex = /nosql/ig;

    while (match = regex.exec(text)) {
        if (match[0] !== "NoSQL") {
            callback(null, text, error + "\nUse 'NoSQL' not 'nosql'");
            return;
        }
    }

    callback(null, text, error);
}

function sqlliteRule(text, error, callback) {
    var regex = /sqlite/ig;

    while (match = regex.exec(text)) {
        if (match[0] !== "SQLite") {
            callback(null, text, error + "\nUse 'SQLite' not 'sqlite'");
            return;
        }
    }

    callback(null, text, error);
}

function jbossRule(text, error, callback) {
    var regex = /jboss/ig;

    while (match = regex.exec(text)) {
        if (match[0] !== "JBoss") {
            callback(null, text, error + "\nUse 'JBoss' not 'jboss'");
            return;
        }
    }

    callback(null, text, error);
}

function redhatRule2(text, error, callback) {
    if (/redhat/i.exec(text)) {
        callback(null, text, error + "\nUse 'Red Hat' not 'RedHat'");
    } else {
        callback(null, text, error);
    }

    callback(null, text, error);
}

function redhatRule(text, error, callback) {
    var regex = /red hat/ig;

    while (match = regex.exec(text)) {
        if (match[0] !== "Red Hat") {
            callback(null, text, error + "\nUse 'Red Hat' not 'red hat'");
            return;
        }
    }

    callback(null, text, error);
}