var dataPrefix = "/data";

jQuery.get(dataPrefix + "/image", function(images) {
    var imagesElement = jQuery("#images");
    imagesElement.html("");
    _.each(images.data, function(image) {
        var listItem = jQuery("<li class='imageListItem'> \
                            <input type='radio' name='radgroup' value='" + image.attributes.dzoneId + "'> \
                            <img src='https://dz2cdn1.dzone.com/thumbnail?fid=" + image.attributes.dzoneId + "&w=370'/> \
                            </label> \
                            </li>");
        imagesElement.append(listItem);
    });
});