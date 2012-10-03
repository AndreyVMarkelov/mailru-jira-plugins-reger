function mainRegisterArrival(event, baseUrl)
{
    event.preventDefault();
    $.ajax({
        url: baseUrl + "/rest/regerws/1.0/reger-arrival/register",
        type: "POST",
        async: false,
        success: function(result) {
            getMainNotifyDialog("Время регистрации: " + result.formattedDate).show();
        },
        error: function(xhr, ajaxOptions, thrownError) {
            getMainErrorDialog(xhr.responseText).show();
        }
    });
}

function mainNotregisterArrival(event, baseUrl)
{
    event.preventDefault();
    $.ajax({
        url: baseUrl + "/rest/regerws/1.0/reger-arrival/notregister",
        type: "POST",
        async: false,
        success: function(result) {
            getMainNotifyDialog("Успешно").show();
        },
        error: function(xhr, ajaxOptions, thrownError) {
            getMainErrorDialog(xhr.responseText).show();
        }
    });
}

function checkLatesForm()
{
    if ($("#userinput").val() == "")
    {
        getMainErrorDialog('Неправильно введены данные: Сотрудник должен быть указан').show();
        return false;
    }

    if (/^\d{4}-\d{2}-\d{2}$/.test($("#delayDate").val()) == false) {
        getMainErrorDialog('Неправильно введены данные: Дата должна быть задана в формате гггг-мм-дд').show();
        return false;
    }

    if ($("#delayDate").val() < $("#today").val()) {
        getMainErrorDialog('Неправильно введены данные: Дата не может быть ранней, чем сегодня').show();
        return false;
    }

    if ($("#delayTime").val() && /^\d{2}:\d{2}$/.test($("#delayTime").val()) == false) {
        getMainErrorDialog('Неправильно введены данные: Время должно быть в формате чч:мм').show();
        return false;
    }

    if ($("#delayTime").val() && ($("#delayTime").val() < "10:00" || $("#delayTime").val() > "19:00")) {
        getMainErrorDialog('Неправильно введены данные: Время должно быть в диапозоне 10:00 и 19:00').show();
        return false;
    }

    return true;
}

function absentTrigger()
{
    if ($("#switchAbsent").attr('checked')) {
        $("#delayTime").attr('disabled','disabled');
    } else {
        $("#delayTime").removeAttr('disabled');
    }
}

function checkGetreport()
{
    if (/^\d{4}-\d{2}-\d{2}$/.test($("#startDate").val()) == false) {
        getMainErrorDialog('Неправильно введены данные: Дата должна быть задана в формате гггг-мм-дд').show();
        return false;
    }

    if (/^\d{4}-\d{2}-\d{2}$/.test($("#endDate").val()) == false) {
        getMainErrorDialog('Неправильно введены данные: Дата должна быть задана в формате гггг-мм-дд').show();
        return false;
    }

    return true;
}

function checkGetreportall()
{
    if (/^\d{4}-\d{2}-\d{2}$/.test($("#startDate").val()) == false) {
        getMainErrorDialog('Неправильно введены данные: Дата должна быть задана в формате гггг-мм-дд').show();
        return false;
    }

    if (/^\d{4}-\d{2}-\d{2}$/.test($("#endDate").val()) == false) {
        getMainErrorDialog('Неправильно введены данные: Дата должна быть задана в формате гггг-мм-дд').show();
        return false;
    }

    return true;
}

/*
 * Search report for user.
 */
function getreport(baseUrl)
{
    if (!checkGetreport()) {
        return false;
    }

    $.ajax({
        url: baseUrl + "/rest/regerws/1.0/itemaction/userreport",
        type: "POST",
        dataType: "json",
        data: $("#searchform").serialize(),
        error: function(xhr, ajaxOptions, thrownError) {
            getMainErrorDialog(xhr.responseText).show();
        },
        success: function(result) {
            $('#data_table > tbody').empty();
            jQuery.each(result, function(index, itemData) {
                var row = "<tr><td width='50%'>" + index + "</td><td width='50%'>" + itemData + "</td></tr>";
                $('#data_table tbody').append(row);
            });
        }
    });
}

function getMainErrorDialog(bodyText)
{
    mainErrorDialog = new AJS.Dialog({
        width:400,
        height:200,
        id:"arrival-error-dialog",
        closeOnOutsideClick: true
    });

    mainErrorDialog.addHeader("Ошибка");
    mainErrorDialog.addPanel("ErrorMainPanel", '' +
        '<html><body><div class="error-message" style="background: no-repeat scroll 10px 10px #FFCCCC; border: 1px solid #CC0000; padding: 10px 10px 10px 30px;">' +
        bodyText
        + '</div></div></body></html>',
        "error-register-panel-body");
    mainErrorDialog.addCancel("Закрыть", function(){ mainErrorDialog.hide() }, 'cancel');
    return mainErrorDialog;
}

function getMainNotifyDialog(bodyText)
{
    mainNotifyDialog = new AJS.Dialog({
        width:400,
        height:150,
        id:"arrival-error-dialog",
        closeOnOutsideClick: true
    });

    mainNotifyDialog.addHeader("Информация");
    mainNotifyDialog.addPanel("ErrorMainPanel", '' +
        '<html><body><div class="error-message" style="background: no-repeat scroll 10px 10px; border: 1px solid #0b7911; padding: 10px 10px 10px 30px;">' +
        bodyText
        + '</div></div></body></html>',
        "error-register-panel-body");
    mainNotifyDialog.addCancel("Закрыть", function() { mainNotifyDialog.hide() }, 'cancel');
    return mainNotifyDialog;
}

/*
 * Search report for all employees.
 */
function getreportall(baseUrl)
{
    if (!checkGetreportall()) {
        return false;
    }

    $.ajax({
        url: baseUrl + "/rest/regerws/1.0/itemaction/allreport",
        type: "POST",
        dataType: "json",
        data: $("#searchform").serialize(),
        error: function(xhr, ajaxOptions, thrownError) {
            getMainErrorDialog(xhr.responseText).show();
        },
        success: function(result) {
            $('#data_table > tbody').empty();

            jQuery.each(result["statMap"], function(index, itemData) {
                var mins = itemData.minutes;
                var missedHours = Math.floor(mins/60);
                var missedMins = (mins%60);
                if (missedMins < 10) {
                    missedMins = "0" + missedMins;
                }
                var middedTime = missedHours + ":" + missedMins;
                var row = "<tr><td>" + index + "</td><td>" + middedTime + "</td><td>" + itemData.days + "</td></tr>";
                $('#data_table tbody').append(row);
            });
        }
    });
}

/*
 * Save calendar changes.
 */
function save_cal(baseUrl)
{
    var checked = []
    var unchecked = []
    var year = $("#year :selected").val();
    var month = $("#month :selected").val();
    $('#day_table >tbody >tr').each(function(i, tr) {
        var flag = $(tr).find('td:eq(2) input').is(':checked');
        if (flag) {
            checked.push($(tr).find('td:eq(0)').html());
        } else {
            unchecked.push($(tr).find('td:eq(0)').html());
        }
    });

    $.ajax({
        url: baseUrl + "/rest/regerws/1.0/itemaction/savecalendar",
        type: "POST",
        dataType: "json",
        data: {"checked": checked, "unchecked": unchecked, "year": year, "month": month},
        error: function(xhr, ajaxOptions, thrownError) {
            getMainErrorDialog(xhr.responseText).show();
        },
        success: function() {
            getMainNotifyDialog("Изменения сохранены").show();
        }
    });
}

/*
 * On change calendar year.
 */
function changeYear()
{
    $("#day_table >tbody").empty();
    $("#month :first").attr("selected", "selected");
}

/*
 * On change calendar month.
 */
function changeMonth(baseUrl)
{
    var year = $("#year :selected").val();
    var month = $("#month :selected").val();

    $.ajax({
        url: baseUrl + "/rest/regerws/1.0/itemaction/getdays",
        type: "POST",
        dataType: "json",
        data: {"year": year, "month": month},
        error: function(xhr, ajaxOptions, thrownError) {
            getMainErrorDialog(xhr.responseText).show();
        },
        success: function(result) {
            $('#day_table > tbody').empty();
            jQuery.each(result, function(index, itemData) {
                var trPart;
                if (itemData.holiday != "true") {
                    trPart = "<tr bgcolor='#ffe7e7'>";
                } else {
                    trPart = "<tr>";
                }

                var chbPart;
                if (itemData.dayoff == "true") {
                    chbPart = '<input type="checkbox" id="chckHead" checked="yes"/>';
                } else {
                    chbPart = '<input type="checkbox" id="chckHead"/>';
                }

                var row = trPart + "<td width='30%'>" + itemData.day + "</td><td width='30%'>" + itemData.dayofweek + "</td><td width='30%'>" + chbPart + "</td></tr>";
                $('#day_table tbody').append(row);
            });
        }
    });
}

/*
 * Show "add "late" content.
 */
function addlates()
{
    $("#addContent").show();
}

/*
 * Register delay.
 */
function reglates(baseUrl, deltext)
{
    if (!checkLatesForm()) {
        return false;
    }

    $.ajax({
        url: baseUrl + "/rest/regerws/1.0/itemaction/registerdelay",
        type: "POST",
        dataType: "json",
        data: $("#late-data").serialize(),
        error: function(xhr, ajaxOptions, thrownError) {
            if (xhr.responseText == "dublicatdate") {
                getMainErrorDialog("Для выбранного сотрудника заявка на данный день уже существует").show();
            } else {
                getMainErrorDialog(xhr.responseText).show();
            }
        },
        success: function(result) {
            $("#delaytable tbody").empty();
            jQuery.each(result, function(index, itemData) {
                var abs;
                if (itemData.absent == "true") {
                    abs = '<input type="checkbox" disabled="disabled" checked="true"/>';
                } else {
                    abs = '<input type="checkbox" disabled="disabled"/>';
                }

                var row = "<tr><td width='20%'>" + itemData.user + "</td><td width='20%'>" + itemData.reporter + "</td><td width='20%'>" + itemData.delayDate + "</td><td width='20%'>" +
                    itemData.delayTime + "</td><td width='20%'>" + itemData.comment + "</td><td width='10%'>" + abs + "</td><td width='10%'>" +
                    "<a href='#' onclick='deleteDelay(event, \"" + baseUrl + "\", \"" + deltext + "\", " + itemData.id + ");'>" + deltext + "</a></td></tr>";
                $('#delaytable tbody').append(row);
            });
        }
    });

    $("#addContent").hide();
}

/*
 * Delete delay.
 */
function deleteDelay(event, baseUrl, deltext, id)
{
    event.preventDefault();

    $.ajax({
        url: baseUrl + "/rest/regerws/1.0/itemaction/deletedelay",
        type: "POST",
        dataType: "json",
        data: {"delayId": id},
        error: function(xhr, ajaxOptions, thrownError) {
            getMainErrorDialog(xhr.responseText).show();
        },
        success: function(result) {
            $("#delaytable tbody").empty();
            jQuery.each(result, function(index, itemData) {
                var abs;
                if (itemData.absent == "true") {
                    abs = '<input type="checkbox" disabled="disabled" checked="true"/>';
                } else {
                    abs = '<input type="checkbox" disabled="disabled"/>';
                }

                var row = "<tr><td width='20%'>" + itemData.user + "</td><td width='20%'>" + itemData.reporter + "</td><td width='20%'>" + itemData.delayDate + "</td><td width='20%'>" +
                    itemData.delayTime + "</td><td width='20%'>" + itemData.comment + "</td><td width='10%'>" + abs + "</td><td width='10%'>" +
                    "<a href='#' onclick='deleteDelay(event, \"" + baseUrl + "\", \"" + deltext + "\", " + itemData.id + ");'>" + deltext + "</a></td></tr>";
                $('#delaytable tbody').append(row);
            });
        }
    });
}

/*
 * Hide "add "late" content.
 */
function cancellates()
{
    $("#addContent").hide();
}

/*
 * CSV user report.
 */
function getcsvreport(event, baseUrl)
{
    event.preventDefault();
    var url = baseUrl + '/rest/regerws/1.0/itemaction/usercsvreport?startDate=' + $("#startDate").val() + '&endDate=' + $("#endDate").val();
    location.href = url;
}

/*
 * CSV report for all.
 */
function getcsvreportall(event, baseUrl)
{
    event.preventDefault();
    var url = baseUrl + '/rest/regerws/1.0/itemaction/allcsvreport?' + $("#searchform").serialize();
    location.href = url;
}
