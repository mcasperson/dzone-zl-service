var dataPrefix = "/data";
var actionPrefix = "/action";

var cookies = null;

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
        xhrFields: {
            withCredentials: true
        },
        data: {username: jQuery("#username").val(), password: jQuery("#password").val()}
    }).done(function(myCookies) {
        cookies = myCookies;

        jQuery("#import").removeAttr("disabled");
        jQuery("#originalSource").removeAttr("disabled");

        if (success) {
            success(myCookies);
        }
    }).error(function() {
        jQuery("#login").removeAttr("disabled");
        jQuery("#username").removeAttr("disabled");
        jQuery("#password").removeAttr("disabled");
    });
}