var arrivalDialog = null;

function checkRegistered()
{
    jQuery.getJSON(
        AJS.params.baseURL + "/plugins/servlet/reger/checker",
        {},
        function(json) {
            if (json.status == 0 || json.status == 3 || json.status == 4) {
                // nothing - it's Ok
            } else if (json.status == 2) {
                getErrorDialog(json.error).show();
            } else {
                arrivalDialog.show();
            }
        }
    );
}

function countdown(secondsValue, callback)
{
    var seconds=secondsValue;
    if (seconds<=-1) {
        seconds+=1;
    } else {
        seconds-=1;
    }

    AJS.$('#count-down-timer').text(seconds);
    if (seconds == 0) {
        callback();
    } else {
        setTimeout("countdown(" + seconds + "," + callback + ")",1000);
    }
}

function notregisterArrival()
{
    jQuery.ajax({
        url: AJS.params.baseURL + "/plugins/servlet/reger/checker",
        dataType : "json",
        type: "POST",
        data: {"type": "cancel"}, 
        success: function(data, textStatus) {
            arrivalDialog.hide();
            if (data.status == 1) {
                getErrorDialog(myObject.error).show();
            } else if (data.status == 4) {
                getErrorDialog("Cancel is not performed because is not logged!!").show();
                window.location = AJS.params.baseURL;
            }
        },
        error: function() {
            alert("error");
        }
    });
}

function registerArrival()
{
    jQuery.ajax({
        url: AJS.params.baseURL + "/plugins/servlet/reger/checker",
        dataType : "json",
        type: "POST",
        data: {"type": "apply"}, 
        success: function(data, textStatus) {
            arrivalDialog.hide();
            if (data.status == 1) {
                getErrorDialog(data.error).show();
            } else if (data.status == 4) {
                getErrorDialog("User is not registered because is not logged!").show();
                window.location = AJS.params.baseURL;
            } else {
                popup = getNotifyDialog(data.formattedDate);
                popup.show();
                countdown(8, function(){ popup.hide() });
            }
        },
        error: function() {
            alert("error");
        }
    });
}

function initArrivalDialog()
{
    var body = "<div id='arrival-warning'><p>" + AJS.I18n.getText("arrival.dialog.not.registered") + "</p></div>";
    arrivalDialog = new AJS.Dialog({width:450, height:250});
    arrivalDialog.addHeader(AJS.I18n.getText("arrival.gadget.register.information"));
    arrivalDialog.addPanel("MainPanel", body);
    arrivalDialog.addButton(AJS.I18n.getText("arrival.gadget.register.arrival"), registerArrival, "register-button");
    arrivalDialog.addButton(AJS.I18n.getText("arrival.gadget.notregister.arrival"), notregisterArrival, "notregister-button");
}

function getNotifyDialog(formattedTime)
{
    var popup = new AJS.Dialog({width:400, height:200, id: "confirm-dialog"});
    popup.addHeader(AJS.I18n.getText("arrival.gadget.register.information"));
    popup.addPanel(AJS.I18n.getText("arrival.dialog.registered"));
    popup.getCurrentPanel().html(AJS.I18n.getText("arrival.dialog.registration.time").replace("|UNKNOWN|", formattedTime));
    popup.addCancel(AJS.I18n.getText("common.words.close"), function(){ popup.hide() }, 'cancel');

    return popup;
}

function getErrorDialog(bodyText)
{
    errorDialog = new AJS.Dialog({
        width:400,
        height:150,
        id:"arrival-error-dialog",
        closeOnOutsideClick: true
    });

    errorDialog.addHeader(AJS.I18n.getText("arrival.gadget.register.error"));
    errorDialog.addPanel("ErrorMainPanel", '' +
        '<html><body><div class="error-message" style="background: no-repeat scroll 10px 10px #FFCCCC; border: 1px solid #CC0000; padding: 10px 10px 10px 30px;">' +
        bodyText
        + '</div></div></body></html>',
        "error-register-panel-body");
    errorDialog.addCancel(AJS.I18n.getText("common.words.close"), function(){errorDialog.hide()}, 'cancel');
    return errorDialog;
}

AJS.toInit(function() {
    initArrivalDialog();
    checkRegistered();
    setInterval("checkRegistered()", 10 * 60 * 1000);
});
