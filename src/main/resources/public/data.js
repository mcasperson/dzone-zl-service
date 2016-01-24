var imageWidth = 250;
var dataPrefix = "/data";
var actionPrefix = "/action";

jQuery.get(dataPrefix + "/image", function(images) {
    var imagesElement = jQuery("#images");
    imagesElement.html("");
    _.each(images.data, function(image) {
        var listItem = jQuery("<li class='imageListItem'> \
                            <input type='radio' name='radgroup' value='" + image.attributes.dzoneId + "'> \
                            <img src='https://dz2cdn1.dzone.com/thumbnail?fid=" + image.attributes.dzoneId + "&w=" + imageWidth + "'/> \
                            </label> \
                            </li>");
        imagesElement.append(listItem);
        imagesElement.show();
    });
});

jQuery("#login").click(function() {
    jQuery.ajax({
        method: "POST",
        url: actionPrefix + "/login",
        data: {username: jQuery("#username").val(), password: jQuery("#password").val()}
    }).done(function(headers) {
        console.log(headers);
        jQuery("#import").removeAttr("disabled");
        jQuery("#originalSource").removeAttr("disabled");

        jQuery("#login").attr("disabled", "disabled");
        jQuery("#username").attr("disabled", "disabled");
        jQuery("#password").attr("disabled", "disabled");
    })
});

jQuery("#import").click(function() {
    jQuery("#suggestedAuthorsList").removeAttr("disabled");
    jQuery("#submit").removeAttr("disabled");
});

