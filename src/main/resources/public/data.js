var imageWidth = 250;
var dataPrefix = "/data";
var actionPrefix = "/action";
var cookies = null;

jQuery.get(dataPrefix + "/image", function(images) {
    var imagesElement = jQuery("#images");
    imagesElement.html("");
    _.each(images.data, function(image) {
        var listItem = jQuery("<li class='imageListItem'> \
                            <input type='radio' name='radgroup' value='" + image.attributes.dzoneId + "' id='imageId" + image.attributes.dzoneId + "'> \
                            <img src='https://dz2cdn1.dzone.com/thumbnail?fid=" + image.attributes.dzoneId + "&w=" + imageWidth + "'/> \
                            </label> \
                            </li>");
        imagesElement.append(listItem);
        imagesElement.show();
    });
});

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
            url: jQuery("#originalSource").val()
        }
    }).done(function(importedPost) {
        if (importedPost.success) {
            jQuery("#suggestedAuthorsList").removeAttr("disabled");
            jQuery("#title").removeAttr("disabled");
            jQuery("#topics").removeAttr("disabled");
            jQuery("#submit").removeAttr("disabled");
            jQuery("#authors").removeAttr("disabled");

            jQuery("#edit").froalaEditor('html.set', importedPost.result.data.fullContent);
            jQuery("#title").val(importedPost.result.data.title);

            jQuery("#edit").froalaEditor('edit.on');
        } else {
            importFailed();
        }
    }).error(function(){
        importFailed();
    });

});

jQuery("#submit").click(function(){

    var content = jQuery("#edit").froalaEditor('html.get');
    var title = jQuery("#title").val();


    if (content.trim().length == 0 || title.trim().length == 0) {
        window.alert("Invalid content or title");
        return;
    }

    jQuery("#edit").froalaEditor('edit.off');
    jQuery("#suggestedAuthorsList").attr("disabled", "disabled");
    jQuery("#title").attr("disabled", "disabled");
    jQuery("#topics").attr("disabled", "disabled");
    jQuery("#submit").attr("disabled", "disabled");
    jQuery("#authors").attr("disabled", "disabled");

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
                title: title

            }
        }).done(function(importedPost) {
            if (importedPost.success) {
                jQuery("#edit").froalaEditor('html.set', "<p/>");
                jQuery("#import").removeAttr("disabled");
                jQuery("#originalSource").removeAttr("disabled");
            } else {
                submitFailed();
            }
        }).error(function(){
            submitFailed();
        });
});

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
    jQuery("#authors").removeAttr("disabled");
}


