jQuery("#submit").click(function() {
    var username = jQuery("#username").val();
    var password = jQuery("#password").val();

    if (username && password) {
        login(function(myCookies){
            uploadImage(myCookies);
        });
    }
});

/**
 * http://uncorkedstudios.com/blog/multipartformdata-file-upload-with-angularjs
 */
function uploadImage(myCookies) {

    var file = jQuery("#file")[0].files[0];

    var fd = new FormData();
    fd.append('file', file);

    jQuery.ajax({
        type: 'post',
        url: actionPrefix + '/uploadImage?awselbCookie=' + cookies.AWSELB +
            "&thCsrfCookie=" + cookies.TH_CSRF +
            "&springSecurityCookie=" + cookies.SPRING_SECURITY_REMEMBER_ME_COOKIE +
            "&jSessionIdCookie=" +  cookies.JSESSIONID,
        data: fd,
        cache: false,
        contentType: false,
        processData: false,
        type: 'POST',
        success: function(data){
            console.log(data);
            alert(data);
        }
    });

}