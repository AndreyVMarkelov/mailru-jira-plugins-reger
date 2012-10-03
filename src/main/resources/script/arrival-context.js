var arrivalDialog = null;
var ajax = null;

function getAjax()
{
    if (window.ActiveXObject) {
        return new ActiveXObject("Microsoft.XMLHTTP");
    }
    else if (window.XMLHttpRequest) {
        return new XMLHttpRequest();
    } else {
        getErrorDialog("Browser does not support AJAX.");
        return null;
    }
}

function checkRegistered()
{
    ajax = getAjax();
    if (ajax != null) {
        var req = AJS.params.baseURL + "/rest/regerws/1.0/arrival-mark/check-arrival";
        ajax.open("GET", req, true);
        ajax.send(null);
        ajax.onreadystatechange = function() {
            if(ajax.readyState==4) {
                var myObject = eval('(' + ajax.responseText + ')');
                if (myObject.status == 0 || myObject.status == 3) {
                    // nothing - it's Ok
                } else if (myObject.status == 2) {
                    getErrorDialog(myObject.error).show();
                } else {
                    arrivalDialog.show();
                }
            }
        }
    }
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
    ajax = getAjax();
    if (ajax != null) {
        var req = AJS.params.baseURL + "/rest/regerws/1.0/arrival-mark/notregister";
        ajax.open("POST", req, true);
        ajax.send(null);
        ajax.onreadystatechange = function() {
            if(ajax.readyState==4) {
                arrivalDialog.hide();
                var myObject = eval('(' + ajax.responseText + ')');
                if (myObject.status == 1) {
                    getErrorDialog(myObject.error).show();
                }
            }
        }
    }
}

function registerArrival()
{
    ajax = getAjax();
    if (ajax != null) {
        var req = AJS.params.baseURL + "/rest/regerws/1.0/arrival-mark/register";
        ajax.open("POST", req, true);
        ajax.send(null);
        ajax.onreadystatechange = function() {
            if(ajax.readyState==4) {
                arrivalDialog.hide();
                var myObject = eval('(' + ajax.responseText + ')');

                if (myObject.status == 1) {
                    getErrorDialog(myObject.error).show();
                } else {
                    popup = getNotifyDialog(myObject.formattedDate);
                    popup.show();
                    countdown(8, function(){ popup.hide() });
                }
            }
        }
    }
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
