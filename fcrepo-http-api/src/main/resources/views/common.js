$(document).ready(function() {
  $("#new_id").attr("placeholder", "(auto-generated identifier)");
});

function addChild()
{
    var id = $("#new_id").val().trim();

    var mixin = $("#new_mixin").val();

    var newURI = $('#main').attr('resource') + "/" + id;

    var postURI = newURI;

    if ( mixin != '' ) {
        postURI = postURI + "?mixin=" + mixin;
    }

    if (mixin == "fedora:datastream") {
        var update_file = document.getElementById("datastream_payload").files[0];
        var reader = new FileReader();
        var xhr = new XMLHttpRequest();
        xhr.onreadystatechange = function() {
            if (xhr.readyState == 4) {
                if (xhr.status == 201) {
                    var loc = xhr.getResponseHeader('Location');
                    var link = xhr.getResponseHeader('Link');
                    if (link != null && link.match('rel="describedby"')) {
                        var str = link.match(/<[^>]+>/)[0];
                        window.location = str.slice(1, str.length - 1);
                    } else if (loc != null) {
                        window.location = loc;
                    } else {
                        window.location.reload();
                    }
                } else {
                    ajaxErrorHandler(xhr, "", "Error creating datastream");
                }
            }
        };

        if (id == "") {
            xhr.open( "POST", newURI );
        } else {
            xhr.open( "PUT", newURI );
        }

        xhr.setRequestHeader("Content-type", update_file.type || "application/octet-stream");
        reader.onload = function(e) {
            var result = e.target.result;
            var data = new Uint8Array(result.length);
            for (var i = 0; i < result.length; i++) {
                data[i] = (result.charCodeAt(i) & 0xff);
            }
            xhr.send(data.buffer);
        };
        reader.readAsBinaryString(update_file);
    } else {
      $.ajax({
        type: id == "" ? "POST" : "PUT",
        url: postURI,
        success: function(data, textStatus, request) { window.location = request.getResponseHeader('Location') || postURI }
      }).fail( ajaxErrorHandler);
    }

    return false;
}

function sendImport() {
    var mixin = $("#import_format").val();
    var postURI = $('#main').attr('resource') + "/fcr:import?format=" + mixin;

    var update_file = document.getElementById("import_file").files[0];
    var reader = new FileReader();
    var xhr = new XMLHttpRequest();
    xhr.onreadystatechange = function() {
        if (xhr.readyState == 4) {

            if (xhr.status == 201) {
                window.location.reload();
            } else {
                ajaxErrorHandler(xhr, "", "Error importing content");
            }
        }
    }

    xhr.open( "POST", postURI );

    xhr.setRequestHeader("Content-type", update_file.type || "application/octet-stream");
    reader.onload = function(e) {
        var result = e.target.result;
        var data = new Uint8Array(result.length);
        for (var i = 0; i < result.length; i++) {
            data[i] = (result.charCodeAt(i) & 0xff);
        }
        xhr.send(data.buffer);
    };
    reader.readAsBinaryString(update_file);

    return false;

}

$(function() {
    $('#new_mixin').change(function() {
        if($('#new_mixin').val() == "fedora:datastream") {
            $('#datastream_payload_container').show();
        } else {
            $('#datastream_payload_container').hide();
        }
    });

    $('#action_create').submit(addChild);
    $('#action_sparql_update').submit(sendSparqlUpdate);
    $('#action_register_namespace').submit(registerNamespace);
    $('#action_delete').submit(deleteItem);
    $('#action_create_transaction').submit(submitAndFollowLocation);
    $('#action_rollback_transaction').submit(submitAndRedirectToBase);
    $('#action_commit_transaction').submit(submitAndRedirectToBase);
    $('#action_import').submit(sendImport);
    $('#action_cnd_update').submit(sendCndUpdate);
    $('#action_sparql_select').submit(sendSparqlQuery);
    $('#action_revert').submit(patchAndReload);
    $('#action_remove_version').submit(removeVersion);

    var ldpContains = $('#childList li').length;
    $('#badge').text(ldpContains);
    $('a[property][href*="' + location.host + '"],#childList a,.breadcrumb a').click(checkIfNonRdfResource);

});

function checkIfNonRdfResource(e) {

    var url = this.href;

    $.ajax({type: "HEAD", url: url}).success(function(data, status, xhr) {
        var headers = xhr.getResponseHeader("Link").split(", ");

        var types = $.grep(headers, function(h) {
            return h.match(/rel="type"/);
        });

        if ($.grep(types, function(h) { return h.match(/NonRDFSource/)}).length > 0 ){
            var description = $.grep(headers, function(h) { return h.match(/rel="describedby"/)});

            if (description.length > 0) {
                location.href = description[0].substr(1, description[0].indexOf(">") - 1);
                return;
            }
        }

        location.href = url;
    });
    e.preventDefault();
    return false;
}


function submitAndFollowLocation() {
    var $form = $(this);

    var postURI = $form.attr('action');

    $.post(postURI, "some-data-to-make-chrome-happy", function(data, textStatus, request) {
        window.location = request.getResponseHeader('Location');
    }).fail( ajaxErrorHandler);

    return false;
}

function removeVersion() {
    var $form = $(this);

    var xhr = new XMLHttpRequest();
    xhr.onreadystatechange = function() {
        if (xhr.readyState == 4) {
            if (xhr.status == 204 ) {
                window.location = $form.attr('data-redirect-after-submit');
            } else {
                ajaxErrorHandler(xhr, "", "Error removing version");
            }
        }
    }
    xhr.open( "DELETE", $form.attr('action') );
    xhr.send(null);

    return false;
}
function patchAndReload() {
    var $form = $(this);
    var patchURI = $form.attr('action');

    $.ajax({url: patchURI, type: "PATCH", data: "", success: function(data, textStatus, request) {
        window.location = $form.attr('data-redirect-after-submit');
    }, error: ajaxErrorHandler});

    return false;
}

function submitAndRedirectToBase() {
    var $form = $(this);


    var postURI = $form.attr('action');

    $.post(postURI, "some-data-to-make-chrome-happy", function(data, textStatus, request) {
        window.location = $form.attr('data-redirect-after-submit');
    }).fail( ajaxErrorHandler);

    return false;
}


function registerNamespace() {
    var postURI = $('#main').attr('resource');


    var query = "INSERT { <" + $('#namespace_uri').val() + "> <http://purl.org/vocab/vann/preferredNamespacePrefix> \"" + $('#namespace_prefix').val() + "\"} WHERE {}";


    $.ajax({url: postURI, type: "POST", contentType: "application/sparql-update", data: query, success: function(data, textStatus, request) {
        window.location.reload(true);
    }}).fail( ajaxErrorHandler);

    return false;
}


function sendSparqlQuery() {
    var $form = $(this);
    var postURI = $form.attr('action');

    $.ajax({url: postURI, type: "POST", contentType: "application/sparql-query", data: $("#sparql_select_query").val(), success: function(data, textStatus, request) {

        $('#errorLabel').text("RESULT");
        $('#errorText').html("<pre></pre>");
        $('#errorText pre').text(request.responseText);
        $('#errorModal').modal('show');
    }, error: ajaxErrorHandler});

    return false;
}

function sendSparqlUpdate() {
    var postURI = $('#main').attr('resource');


    $.ajax({url: postURI, type: "PATCH", contentType: "application/sparql-update", data: $("#sparql_update_query").val(), success: function(data, textStatus, request) {
        window.location.reload(true);
    }, error: ajaxErrorHandler});

    return false;
}

function sendCndUpdate() {
    var postURI = $('#main').attr('resource');


    $.ajax({url: postURI, type: "POST", contentType: "text/cnd", data: $("#cnd_update_query").val(), success: function(data, textStatus, request) {
        window.location.reload(true);
    }, error: ajaxErrorHandler});

    return false;
}

function deleteItem()
{
    var uri = $('#main').attr('resource');
    var arr = uri.toString().split("/");
    arr.pop();
    var newURI = arr.join("/");

    $.ajax({url: uri, type: "DELETE", success: function() {
        window.location = newURI;
    }}).fail( ajaxErrorHandler);
    return false;
}

function updateFile()
{
    var update_file = document.getElementById("update_file").files[0];
    var url = window.location.replace("fcr:metadata", "");
    var reader = new FileReader();
    var xhr = new XMLHttpRequest();
    xhr.onreadystatechange = function() {
        if (xhr.readyState == 4) {

            if (xhr.status == 204 || xhr.status == 201) {
                window.location.reload(true);
            } else {
                ajaxErrorHandler(xhr, "", "Error creating datastream");
            }
        }
    }
    xhr.open( "PUT", url );
    xhr.setRequestHeader("Content-type", update_file.type);
    reader.onload = function(e) {
        var result = e.target.result;
        var data = new Uint8Array(result.length);
        for (var i = 0; i < result.length; i++) {
            data[i] = (result.charCodeAt(i) & 0xff);
        }
        xhr.send(data.buffer);
    };
    reader.readAsBinaryString(update_file);
}

function updateAccessRoles()
{
    var update_json = document.getElementById("rbacl_json").value;
    var url = window.location + "/fcr:accessroles";
    var xhr = new XMLHttpRequest();
    xhr.onreadystatechange = function() {
        if (xhr.readyState == 4) {
            if (xhr.status == 204 || xhr.status == 201) {
                window.location.reload(true);
            } else {
                ajaxErrorHandler(xhr, "", "Error creating datastream");
            }
        }
    }
    xhr.open( "POST", url );
    xhr.setRequestHeader("Content-type", "application/json");
    xhr.send(update_json);
}

function ajaxErrorHandler(xhr, textStatus, errorThrown) {
    $('#errorLabel').text(errorThrown);
    $('#errorText').text(xhr.responseText);
    $('#errorModal').modal('show');

}
