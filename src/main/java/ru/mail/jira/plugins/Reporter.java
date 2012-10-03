package ru.mail.jira.plugins;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFCellStyle;
import org.apache.poi.hssf.usermodel.HSSFFont;
import org.apache.poi.hssf.usermodel.HSSFRichTextString;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.hssf.util.HSSFColor;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.velocity.exception.VelocityException;
import ru.mail.jira.plugins.structs.DateCaclStruct;
import ru.mail.jira.plugins.structs.HrDate;
import ru.mail.jira.plugins.structs.TimeOffBean;
import com.atlassian.crowd.embedded.api.User;
import com.atlassian.jira.ComponentManager;
import com.atlassian.jira.ManagerFactory;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.mail.Email;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.security.groups.GroupManager;
import com.atlassian.jira.user.util.UserUtil;
import com.atlassian.jira.util.I18nHelper;
import com.atlassian.jira.util.json.JSONArray;
import com.atlassian.jira.util.json.JSONException;
import com.atlassian.jira.util.json.JSONObject;
import com.atlassian.mail.queue.SingleMailQueueItem;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

@Path("/itemaction")
public class Reporter
{
    public static final String UTF8_BOM = "\uFEFF";

    private static Log log = LogFactory.getLog(Reporter.class);

    public static final String CSV_SEPARATOR = ";";

    /**
     * Configuration manager.
     */
    private final ConfigurationManager configurationManager;

    /**
     * Group manager.
     */
    private final GroupManager groupManager;

    /**
     * Utility for work with JIRA users.
     */
    private final UserUtil userUtil;

    /**
     * Constructor.
     */
    public Reporter(
        ConfigurationManager configurationManager,
        UserUtil userUtil,
        GroupManager groupManager)
    {
        this.configurationManager = configurationManager;
        this.userUtil = userUtil;
        this.groupManager = groupManager;
    }

    @POST
    @Produces ({ MediaType.APPLICATION_JSON})
    @Path("/sethrdate")
    public Response setHrDate(@Context HttpServletRequest request)
    throws JSONException
    {
        JiraAuthenticationContext authCtx = ComponentManager.getInstance().getJiraAuthenticationContext();
        I18nHelper i18n = authCtx.getI18nHelper();

        if (!isUserInHR(authCtx.getLoggedInUser().getName()))
        {
            return Response.serverError().build();
        }

        String year = request.getParameter("year");
        String month = request.getParameter("month");
        String repDate = request.getParameter("repDate");
        String over = request.getParameter("over");

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

        JSONObject result = new JSONObject();

        List<HrDate> hrDates;
        try
        {
            HrDate hrDate = new HrDate();
            hrDate.setYear(Integer.parseInt(year));
            hrDate.setMonth(Integer.parseInt(month));
            hrDate.setHrDate(sdf.parse(repDate));

            RegistratorSQL reg = new RegistratorSQL(configurationManager);
            HrDate storedDate = reg.getHrDate(hrDate.getYear(), hrDate.getMonth());
            boolean s = false;
            if (storedDate != null)
            {
                if (!over.equals("true"))
                {
                    result.put("over", "over");
                    return Response.ok(result.toString()).build();
                }
                else
                {
                    s = true;
                }
            }

            if (s)
            {
                reg.deleteHrDate(hrDate);
            }
                reg.addHrDate(hrDate);
                hrDates = reg.getLastHrDates();
                Map<Integer, String> mmap = ArrivalUtils.getMonthNames();

                List<Map<String, Object>> p = new ArrayList<Map<String, Object>>();
                for (HrDate hr : hrDates)
                {
                    Map<String, Object> m = new HashMap<String, Object>();
                    m.put("month", i18n.getText(mmap.get(hr.getMonth())));
                    m.put("year", hr.getYear());
                    m.put("hrDate", hr.getHrDate());
                    p.add(m);
                }

                result.put("over", "");
                result.put("hrDates", p);
        }
        catch (Exception ex)
        {
            log.error("Reporter::setHrDate", ex);
            return Response.status(500).entity(ex.getMessage()).build();
        }

        return Response.ok(result.toString()).build();
    }

    @GET
    @Produces ({ MediaType.APPLICATION_JSON})
    @Path("/listusers")
    public Response listUsers(@Context HttpServletRequest request)
    throws JSONException
    {
        String query = request.getParameter("query");

        Set<String> suggestions = new LinkedHashSet<String>();
        Set<String> data = new LinkedHashSet<String>();

        for (String groups : configurationManager.getReportGroups())
        {
            Collection<User> users = groupManager.getUsersInGroup(groups);
            for (User user : users)
            {
                if (user.getName().contains(query))
                {
                    suggestions.add(user.getDisplayName());
                    data.add(user.getName());
                }
            }
        }

        JSONObject result = new JSONObject();
        result.put("query", query);
        result.put("suggestions", suggestions);
        result.put("data", data);

        return Response.ok(result.toString()).build();
    }

    @GET
    @Produces ({ MediaType.APPLICATION_JSON})
    @Path("/allcsvreport")
    public Response allCsvReport(@Context HttpServletRequest request)
    throws JSONException
    {
        JiraAuthenticationContext authCtx = ComponentManager.getInstance().getJiraAuthenticationContext();
        I18nHelper i18n = authCtx.getI18nHelper();

        if (!isUserInHR(authCtx.getLoggedInUser().getName()))
        {
            return Response.serverError().build();
        }

        String startDateStr = request.getParameter("startDate");
        String endDateStr = request.getParameter("endDate");
        String[] users = request.getParameterValues("users");
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

        try
        {
            RegistratorSQL reg = new RegistratorSQL(configurationManager);
            Date startDate = sdf.parse(startDateStr);
            Date endDate = sdf.parse(endDateStr);
            Map<Date, Boolean> range = reg.getCalendarRange(startDate, endDate);
            Map<String, List<Date>> res = reg.getAllArrivals(users, startDate, endDate);

            Map<String, Map<String, String>> statMap = new TreeMap<String, Map<String, String>>();
            for (String user : users)
            {
                TimeCalculator tc = new TimeCalculator(startDate, endDate, range, res.get(user));
                Map<String, String> genMap = tc.calculate().toMap();
                genMap.put("fullname", ArrivalUtils.getDisplayUser(userUtil, user));
                statMap.put(ArrivalUtils.getDisplayUser(userUtil, user), genMap);
            }

            //--> create workbook
            HSSFWorkbook wb = new HSSFWorkbook();
            HSSFSheet mainSheet = wb.createSheet();
            mainSheet.autoSizeColumn(0);

            CreationHelper factory = wb.getCreationHelper();
            //<--

            int rowCount = 0;
            //--> create excel header
            HSSFRow row = mainSheet.createRow(rowCount++);
            row.setHeightInPoints(100);

            CellStyle style = wb.createCellStyle();
            style.setFillForegroundColor(HSSFColor.GREY_25_PERCENT.index);
            style.setFillPattern(HSSFCellStyle.SOLID_FOREGROUND);
            style.setAlignment(CellStyle.ALIGN_CENTER);
            HSSFFont font = wb.createFont();
            font.setBoldweight(HSSFFont.BOLDWEIGHT_BOLD);
            style.setFont(font);
            HSSFCell cell = row.createCell(0);
            cell.setCellValue(new HSSFRichTextString(i18n.getText("arrival.report.all.column.employee")));
            cell.setCellStyle(style);

            cell = row.createCell(1);
            cell.setCellValue(factory.createRichTextString(i18n.getText("arrival.report.all.column.count.minutes")));
            cell.setCellStyle(style);

            cell = row.createCell(2);
            cell.setCellValue(factory.createRichTextString(i18n.getText("arrival.report.all.column.count.absent")));
            cell.setCellStyle(style);
            //<--

            for (Entry<String, Map<String, String>> entry : statMap.entrySet())
            {
                Long mins = Long.parseLong(entry.getValue().get("minutes"));
                Long missedMins = mins%60;
                String missedTime = (mins/60) + ":" + ((missedMins < 9) ? ("0" + missedMins) : missedMins);
                Long days = Long.parseLong(entry.getValue().get("days"));

                style = wb.createCellStyle();
                if (mins > 600 || days > 0)
                {
                    font = wb.createFont();
                    font.setColor(HSSFColor.RED.index);
                    style.setFont(font);
                }

                //-->
                row = mainSheet.createRow(rowCount++);
                cell = row.createCell(0);
                cell.setCellValue(factory.createRichTextString(entry.getKey()));
                cell.setCellStyle(style);

                cell = row.createCell(1);
                cell.setCellValue(factory.createRichTextString(missedTime));
                cell.setCellStyle(style);

                cell = row.createCell(2);
                cell.setCellValue(factory.createRichTextString(entry.getValue().get("days").toString()));
                cell.setCellStyle(style);
                //<-
            }

            for (int i = 0; i < 3; i++)
            {
                mainSheet.autoSizeColumn(i);
            }

            ByteArrayOutputStream os = new ByteArrayOutputStream();
            wb.write(os);
            os.close();

            return Response.ok(os.toByteArray())
                .header("Content-Disposition", "attachment; filename=all-report.xls")
                .type(MediaType.APPLICATION_OCTET_STREAM_TYPE)
                .build();
        }
        catch (Exception ex)
        {
            log.error("Reporter::allCsvReport", ex);
            return Response.status(500).entity(ex.getMessage()).build();
        }
    }

    @GET
    @Produces ({ MediaType.APPLICATION_JSON})
    @Path("/allpdfreport")
    public Response allPdfReport(@Context HttpServletRequest request)
    throws JSONException
    {
        JiraAuthenticationContext authCtx = ComponentManager.getInstance().getJiraAuthenticationContext();
        I18nHelper i18n = authCtx.getI18nHelper();

        if (!isUserInHR(authCtx.getLoggedInUser().getName()))
        {
            return Response.serverError().build();
        }

        String startDateStr = request.getParameter("startDate");
        String endDateStr = request.getParameter("endDate");
        String[] users = request.getParameterValues("users");

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

        Font font;
        try
        {
            BaseFont bf_embedded = BaseFont.createFont("/fonts/comic.ttf", BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
            font = new Font(bf_embedded, 12);
        }
        catch (Exception ex)
        {
            log.error("Reporter::allCsvReport", ex);
            return Response.status(500).entity(ex.getMessage()).build();
        }

        Document document = new Document(PageSize.A4, 50, 50, 50, 50);
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        try
        {
            RegistratorSQL reg = new RegistratorSQL(configurationManager);
            Date startDate = sdf.parse(startDateStr);
            Date endDate = sdf.parse(endDateStr);
            Map<Date, Boolean> range = reg.getCalendarRange(startDate, endDate);
            Map<String, List<Date>> res = reg.getAllArrivals(users, startDate, endDate);

            Map<String, Map<String, String>> statMap = new HashMap<String, Map<String, String>>();
            for (String user : users)
            {
                TimeCalculator tc = new TimeCalculator(startDate, endDate, range, res.get(user));
                Map<String, String> genMap = tc.calculate().toMap();
                genMap.put("fullname", ArrivalUtils.getDisplayUser(userUtil, user));
                statMap.put(ArrivalUtils.getDisplayUser(userUtil, user), genMap);
            }

            PdfWriter.getInstance(document, os);
            document.open();

            Paragraph masterParagraph = new Paragraph();
            masterParagraph.add(new Phrase(i18n.getText("arrival.report.paragraph.all", startDateStr, endDateStr), font));

            PdfPTable main = new PdfPTable(3);
            main.setWidthPercentage(100);
            PdfPCell c1 = new PdfPCell(new Phrase(i18n.getText("arrival.report.all.column.employee"), font));
            c1.setHorizontalAlignment(Element.ALIGN_CENTER);
            c1.setBackgroundColor(Color.LIGHT_GRAY);
            main.addCell(c1);
            c1 = new PdfPCell(new Phrase(i18n.getText("arrival.report.all.column.count.minutes"), font));
            c1.setHorizontalAlignment(Element.ALIGN_CENTER);
            c1.setBackgroundColor(Color.LIGHT_GRAY);
            main.addCell(c1);
            c1 = new PdfPCell(new Phrase(i18n.getText("arrival.report.all.column.count.absent"), font));
            c1.setHorizontalAlignment(Element.ALIGN_CENTER);
            c1.setBackgroundColor(Color.LIGHT_GRAY);
            main.addCell(c1);
            main.setHeaderRows(1);
            main.setSkipFirstHeader(false);

            for (Entry<String, Map<String, String>> entry : statMap.entrySet())
            {
                main.addCell(new Phrase(entry.getKey(), font));

                Long mins = Long.parseLong(entry.getValue().get("minutes"));
                Long missedMins = mins%60;
                String missedTime = (mins/60) + ":" + ((missedMins < 9) ? ("0" + missedMins) : missedMins);

                main.addCell(new Phrase(missedTime, font));
                main.addCell(new Phrase(entry.getValue().get("days").toString(), font));
            }

            masterParagraph.add(main);
            document.add(masterParagraph);
            document.close();
        }
        catch (Exception ex)
        {
            log.error("Reporter::allCsvReport", ex);
            return Response.status(500).entity(ex.getMessage()).build();
        }

        return Response.ok(os.toByteArray()).header("Content-Disposition", "attachment; filename=all-report.pdf").type(MediaType.APPLICATION_OCTET_STREAM_TYPE).build();
    }

    @POST
    @Produces ({ MediaType.APPLICATION_JSON})
    @Path("/allreport")
    public Response allReport(@Context HttpServletRequest request)
    throws JSONException
    {
        JiraAuthenticationContext authCtx = ComponentManager.getInstance().getJiraAuthenticationContext();
        String startDateStr = request.getParameter("startDate");
        String endDateStr = request.getParameter("endDate");
        String[] users = request.getParameterValues("users");

        if (!isUserInHR(authCtx.getLoggedInUser().getName()))
        {
            return Response.serverError().build();
        }

        try
        {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            Date startDate = sdf.parse(startDateStr);
            Date endDate = sdf.parse(endDateStr);

            RegistratorSQL reg = new RegistratorSQL(configurationManager);
            Map<Date, Boolean> range = reg.getCalendarRange(startDate, endDate);
            Map<String, List<Date>> res = reg.getAllArrivals(users, startDate, endDate);

            List<Map<String, String>> statMap = new ArrayList<Map<String, String>>();
            for (String user : users)
            {
                TimeCalculator tc = new TimeCalculator(startDate, endDate, range, res.get(user));
                Map<String, String> genMap = tc.calculate().toMap();
                genMap.put("fullname", ArrivalUtils.getDisplayUser(userUtil, user));
                statMap.add(genMap);
            }

            JSONObject result = new JSONObject();
            result.putOpt("statMap", statMap);
            return Response.ok(result.toString()).build();
        }
        catch (Exception e)
        {
            log.error("Reporter::registerDelay", e);
            return Response.status(500).entity(e.getMessage()).build();
        }
    }

    @POST
    @Produces ({ MediaType.APPLICATION_JSON})
    @Path("/deletedelay")
    public Response deleteDelay(@Context HttpServletRequest request)
    throws JSONException
    {
        String user = ComponentManager.getInstance().getJiraAuthenticationContext().getLoggedInUser().getName();
        if (!isUserInReport(user) && !isUserInHR(user))
        {
            return Response.serverError().build();
        }

        String delayId = request.getParameter("delayId");

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

        List<TimeOffBean> list;
        try
        {
            RegistratorSQL reg = new RegistratorSQL(configurationManager);
            reg.deleteDelay(Long.parseLong(delayId));
            list = reg.getTimeOffList();
        }
        catch (Exception e)
        {
            log.error("Reporter::registerDelay", e);
            return Response.status(500).entity(e.getMessage()).build();
        }

        JSONArray result = new JSONArray();
        for (TimeOffBean tob : list)
        {
            Map<String, String> map = new HashMap<String, String>();
            map.put("id", Long.toString(tob.getId()));
            map.put("user", ArrivalUtils.getDisplayUser(userUtil, tob.getUser()));
            map.put("reporter", ArrivalUtils.getDisplayUser(userUtil, tob.getReporter()));
            map.put("delayDate", sdf.format(tob.getDelayDate()));
            map.put("delayTime", tob.getDelayTime());
            map.put("absent", tob.getAbsent());
            map.put("comment", tob.getComment());
            result.put(map);
        }

        return Response.ok(result.toString()).build();
    }

    @POST
    @Produces ({ MediaType.APPLICATION_JSON})
    @Path("/getdays")
    public Response getDays(@Context HttpServletRequest request)
    throws JSONException
    {
        JiraAuthenticationContext authCtx = ComponentManager.getInstance().getJiraAuthenticationContext();
        I18nHelper i18n = authCtx.getI18nHelper();

        if (!isUserInHR(authCtx.getLoggedInUser().getName()))
        {
            return Response.serverError().build();
        }

        Integer year = Integer.valueOf(request.getParameter("year"));
        Integer month = Integer.valueOf(request.getParameter("month"));
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.YEAR, year);
        cal.set(Calendar.MONTH, month - 1);

        Map<Integer, Boolean> dayList;
        try
        {
            dayList = new RegistratorSQL(configurationManager).getDayoff(year, month);
        }
        catch (SQLException sqlex)
        {
            log.error("Reporter::getDays", sqlex);
            return Response.status(500).entity(sqlex.getMessage()).build();
        }

        JSONArray result = new JSONArray();
        int minDay = cal.getActualMinimum(Calendar.DAY_OF_MONTH);
        int maxDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH);
        while (minDay <= maxDay)
        {
            cal.set(Calendar.DAY_OF_MONTH, minDay);
            int dayOfWeek = cal.get(Calendar.DAY_OF_WEEK);
            Boolean holiday = (dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY) ? false : true;
            Boolean dayOff = holiday;
            if (dayList.containsKey(minDay))
            {
                dayOff = dayList.get(minDay);
            }

            Map<String, String> map = new HashMap<String, String>();
            map.put("day", Integer.toString(minDay));
            map.put("dayofweek", ArrivalUtils.getDayOfWeekStr(dayOfWeek, i18n));
            map.put("dayoff", dayOff.toString());
            map.put("holiday", holiday.toString());
            result.put(map);
            cal.add(Calendar.DAY_OF_MONTH, 1);
            minDay++;
        }

        return Response.ok(result.toString()).build();
    }

    @POST
    @Produces ({ MediaType.APPLICATION_JSON})
    @Path("/registerdelay")
    public Response registerDelay(@Context HttpServletRequest request)
    throws JSONException
    {
        String user = ComponentManager.getInstance().getJiraAuthenticationContext().getLoggedInUser().getName();

        if (!isUserInReport(user) && !isUserInHR(user))
        {
            return Response.serverError().build();
        }

        String delayuser = request.getParameter("delayuser");
        String dalayDate = request.getParameter("delayDate");
        String delayTime = request.getParameter("delayTime");
        String lateReason = request.getParameter("lateReason");
        String switchAbsent = (request.getParameter("switchAbsent") != null) ? Boolean.TRUE.toString() : Boolean.FALSE.toString();

        if (delayTime == null)
        {
            delayTime = "";
        }

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        SimpleDateFormat sdf2 = new SimpleDateFormat("dd.MM.yyyy");

        List<TimeOffBean> list;
        try
        {
            Date date = sdf.parse(dalayDate);
            RegistratorSQL reg = new RegistratorSQL(configurationManager);
            boolean isReged = reg.registerDelay(
                delayuser,
                user,
                date,
                delayTime,
                lateReason,
                switchAbsent);

            if (!isReged)
            {
                throw new Exception("dublicatdate");
            }

            JiraAuthenticationContext authContext = ComponentManager.getInstance().getJiraAuthenticationContext();
            sendMail(
                authContext,
                configurationManager.getMailRecipient(),
                delayuser,
                sdf2.format(date),
                delayTime,
                lateReason,
                Boolean.parseBoolean(switchAbsent));

            list = reg.getTimeOffList();
        }
        catch (Exception e)
        {
            log.error("Reporter::registerDelay", e);
            return Response.status(500).entity(e.getMessage()).build();
        }

        JSONArray result = new JSONArray();
        for (TimeOffBean tob : list)
        {
            Map<String, String> map = new HashMap<String, String>();
            map.put("id", Long.toString(tob.getId()));
            map.put("user", ArrivalUtils.getDisplayUser(userUtil, tob.getUser()));
            map.put("reporter", ArrivalUtils.getDisplayUser(userUtil, tob.getReporter()));
            map.put("delayDate", sdf.format(tob.getDelayDate()));
            map.put("delayTime", tob.getDelayTime());
            map.put("absent", tob.getAbsent());
            map.put("comment", tob.getComment());
            result.put(map);
        }

        return Response.ok(result.toString()).build();
    }

    @POST
    @Produces ({ MediaType.APPLICATION_JSON})
    @Path("/savecalendar")
    public Response saveCalendar(@Context HttpServletRequest request)
    throws JSONException
    {
        JiraAuthenticationContext authCtx = ComponentManager.getInstance().getJiraAuthenticationContext();
        if (!isUserInHR(authCtx.getLoggedInUser().getName()))
        {
            return Response.serverError().build();
        }

        String[] checked = request.getParameterValues("checked");
        String[] unchecked = request.getParameterValues("unchecked");
        String year = request.getParameter("year");
        String month = request.getParameter("month");

        try
        {
            new RegistratorSQL(configurationManager).saveCalendar(
                Integer.parseInt(year),
                Integer.parseInt(month),
                checked,
                unchecked);
        }
        catch (SQLException sqlex)
        {
            log.error("Reporter::saveCalendar", sqlex);
            return Response.status(500).entity(sqlex.getMessage()).build();
        }

        return Response.ok().build();
    }

    private void sendMail(
        JiraAuthenticationContext authContext,
        String recipient,
        String delayuser,
        String date,
        String time,
        String reason,
        boolean absentOrLate)
    {
        String user = authContext.getLoggedInUser().getDisplayName();
        Email email = new Email(recipient);
        if (absentOrLate)
        {
            email.setSubject(authContext.getI18nHelper().getText("arrival.mail.absent", ArrivalUtils.getDisplayUser(userUtil, delayuser), date));
        }
        else
        {
            email.setSubject(authContext.getI18nHelper().getText("arrival.mail.notify", ArrivalUtils.getDisplayUser(userUtil, delayuser), date, time));
        }

        try
        {
            Map<String, Object> params = new HashMap<String, Object>();
            params.put("reporter", user);
            params.put("user", ArrivalUtils.getDisplayUser(userUtil, delayuser));
            params.put("date", date);
            params.put("time", time);
            params.put("reason", reason);
            params.put("absentOrLate", absentOrLate);

            email.setBody(ComponentAccessor.getVelocityManager().getEncodedBody("templates/", "mail.vm", "UTF-8", params));
        }
        catch (VelocityException e)
        {
            log.error("Reporter::sendMail", e);
        }
        email.setMimeType("text/html");
        email.setFromName("JIRA");

        ManagerFactory.getMailQueue().addItem(new SingleMailQueueItem(email));
    }

    @GET
    @Produces ({ MediaType.APPLICATION_JSON})
    @Path("/userpdfreport")
    public Response userPdfReport(@Context HttpServletRequest request)
    throws JSONException
    {
        JiraAuthenticationContext authCtx = ComponentManager.getInstance().getJiraAuthenticationContext();
        I18nHelper i18n = authCtx.getI18nHelper();
        User user = authCtx.getLoggedInUser();

        if (!isUserInReport(user.getName()))
        {
            return Response.serverError().build();
        }

        String startDateStr = request.getParameter("startDate");
        String endDateStr = request.getParameter("endDate");

        Font font;
        try
        {
            BaseFont bf_embedded = BaseFont.createFont("/fonts/comic.ttf", BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
            font = new Font(bf_embedded, 12);
        }
        catch (Exception ex)
        {
            log.error("Reporter::userCsvReport", ex);
            return Response.status(500).entity(ex.getMessage()).build();
        }

        Document document = new Document(PageSize.A4, 50, 50, 50, 50);
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        try
        {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            Date startDate = sdf.parse(startDateStr);
            Date endDate = sdf.parse(endDateStr);

            RegistratorSQL reg = new RegistratorSQL(configurationManager);
            List<Date> list = reg.getUserArrivals(user.getName(), startDate, endDate);
            Map<Date, Boolean> range = reg.getCalendarRange(startDate, endDate);

            TimeCalculator tc = new TimeCalculator(startDate, endDate, range, list);
            List<Map<String, String>> resList = tc.caclArrivals();

            PdfWriter.getInstance(document, os);
            document.open();

            Paragraph masterParagraph = new Paragraph();
            masterParagraph.add(new Phrase(i18n.getText("arrival.report.paragraph", ArrivalUtils.getDisplayUser(userUtil, user.getName()), startDateStr, endDateStr), font));
            PdfPTable table = new PdfPTable(3);
            table.setWidthPercentage(100);
            PdfPCell c1 = new PdfPCell(new Phrase(i18n.getText("arrival.report.user.column.date"), font));
            c1.setHorizontalAlignment(Element.ALIGN_CENTER);
            c1.setBackgroundColor(Color.LIGHT_GRAY);
            table.addCell(c1);
            c1 = new PdfPCell(new Phrase(i18n.getText("arrival.report.user.column.arrival"), font));
            c1.setHorizontalAlignment(Element.ALIGN_CENTER);
            c1.setBackgroundColor(Color.LIGHT_GRAY);
            table.addCell(c1);
            c1 = new PdfPCell(new Phrase(i18n.getText("arrival.report.user.column.middestime"), font));
            c1.setHorizontalAlignment(Element.ALIGN_CENTER);
            c1.setBackgroundColor(Color.LIGHT_GRAY);
            table.addCell(c1);
            table.setHeaderRows(1);

            for (Map<String, String> map : resList)
            {
                int intValue = Integer.parseInt(map.get("daytype").substring(1), 16);
                PdfPCell cell = new PdfPCell();
                cell.setBackgroundColor(new Color(intValue));
                cell.setPhrase(new Phrase(map.get("eachdata"), font));
                table.addCell(cell);
                cell = new PdfPCell();
                cell.setBackgroundColor(new Color(intValue));
                cell.setPhrase(new Phrase(map.get("arrival"), font));
                table.addCell(cell);
                cell = new PdfPCell();
                cell.setBackgroundColor(new Color(intValue));
                cell.setPhrase(new Phrase(map.get("missedtime"), font));
                table.addCell(cell);
            }

            masterParagraph.add(table);
            document.add(masterParagraph);
            document.close();
        }
        catch (Exception e)
        {
            log.error("Reporter::userCsvReport", e);
            return Response.status(500).entity(e.getMessage()).build();
        }

        return Response.ok(os.toByteArray()).header("Content-Disposition", "attachment; filename=user-report.pdf").type(MediaType.APPLICATION_OCTET_STREAM_TYPE).build();
    }

    @GET
    @Produces ({ MediaType.APPLICATION_JSON})
    @Path("/usercsvreport")
    public Response userCsvReport(@Context HttpServletRequest request)
    throws JSONException
    {
        JiraAuthenticationContext authCtx = ComponentManager.getInstance().getJiraAuthenticationContext();
        I18nHelper i18n = authCtx.getI18nHelper();
        User user = authCtx.getLoggedInUser();

        if (!isUserInReport(user.getName()))
        {
            return Response.serverError().build();
        }

        String startDateStr = request.getParameter("startDate");
        String endDateStr = request.getParameter("endDate");

        try
        {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            Date startDate = sdf.parse(startDateStr);
            Date endDate = sdf.parse(endDateStr);

            RegistratorSQL reg = new RegistratorSQL(configurationManager);
            List<Date> list = reg.getUserArrivals(user.getName(), startDate, endDate);
            Map<Date, Boolean> range = reg.getCalendarRange(startDate, endDate);

            TimeCalculator tc = new TimeCalculator(startDate, endDate, range, list);
            List<Map<String, String>> resList = tc.caclArrivals();

            //--> create workbook
            HSSFWorkbook wb = new HSSFWorkbook();
            HSSFSheet mainSheet = wb.createSheet();
            mainSheet.autoSizeColumn(0);

            CreationHelper factory = wb.getCreationHelper();
            //<--

            int rowCount = 0;
            //--> create excel header
            HSSFRow row = mainSheet.createRow(rowCount++);
            row.setHeightInPoints(100);

            CellStyle style = wb.createCellStyle();
            style.setFillForegroundColor(HSSFColor.GREY_25_PERCENT.index);
            style.setFillPattern(HSSFCellStyle.SOLID_FOREGROUND);
            style.setAlignment(CellStyle.ALIGN_CENTER);
            HSSFFont font = wb.createFont();
            font.setBoldweight(HSSFFont.BOLDWEIGHT_BOLD);
            style.setFont(font);
            HSSFCell cell = row.createCell(0);
            cell.setCellValue(new HSSFRichTextString(i18n.getText("arrival.report.user.column.date")));
            cell.setCellStyle(style);

            cell = row.createCell(1);
            cell.setCellValue(factory.createRichTextString(i18n.getText("arrival.report.user.column.arrival")));
            cell.setCellStyle(style);

            cell = row.createCell(2);
            cell.setCellValue(factory.createRichTextString(i18n.getText("arrival.report.user.column.middestime")));
            cell.setCellStyle(style);
            //<--

            for (Map<String, String> map : resList)
            {
                style = wb.createCellStyle();

                //-->
                row = mainSheet.createRow(rowCount++);
                cell = row.createCell(0);
                cell.setCellValue(factory.createRichTextString(map.get("eachdata")));
                cell.setCellStyle(style);

                cell = row.createCell(1);
                cell.setCellValue(factory.createRichTextString(map.get("arrival")));
                cell.setCellStyle(style);

                cell = row.createCell(2);
                cell.setCellValue(factory.createRichTextString(map.get("missedtime")));
                cell.setCellStyle(style);
            }

            for (int i = 0; i < 3; i++)
            {
                mainSheet.autoSizeColumn(i);
            }

            ByteArrayOutputStream os = new ByteArrayOutputStream();
            wb.write(os);
            os.close();

            return Response.ok(os.toByteArray()).header("Content-Disposition", "attachment; filename=user-report.xls")
                .header("charset", "UTF-8").type(MediaType.APPLICATION_OCTET_STREAM_TYPE).build();
        }
        catch (Exception e)
        {
            log.error("Reporter::userCsvReport", e);
            return Response.status(500).entity(e.getMessage()).build();
        }
    }

    @POST
    @Produces ({ MediaType.APPLICATION_JSON})
    @Path("/userreport")
    public Response userReport(@Context HttpServletRequest request)
    throws JSONException
    {
        JiraAuthenticationContext authCtx = ComponentManager.getInstance().getJiraAuthenticationContext();
        if (!isUserInReport(authCtx.getLoggedInUser().getName()))
        {
            return Response.serverError().build();
        }

        String startDateStr = request.getParameter("startDate");
        String endDateStr = request.getParameter("endDate");

        try
        {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            Date startDate = sdf.parse(startDateStr);
            Date endDate = sdf.parse(endDateStr);
            String user = ComponentManager.getInstance().getJiraAuthenticationContext().getLoggedInUser().getName();

            RegistratorSQL reg = new RegistratorSQL(configurationManager);
            List<Date> list = reg.getUserArrivals(user, startDate, endDate);
            Map<Date, Boolean> range = reg.getCalendarRange(startDate, endDate);

            TimeCalculator tc = new TimeCalculator(startDate, endDate, range, list);
            DateCaclStruct dcs = tc.calculate();

            JSONObject result = new JSONObject();
            result.putOpt("sgenMap", tc.caclArrivals());
            result.putOpt("genMap", dcs.toMap());
            return Response.ok(result.toString()).build();
        }
        catch (Exception e)
        {
            log.error("Reporter::registerDelay", e);
            return Response.status(500).entity(e.getMessage()).build();
        }
    }

    private boolean isUserInHR(String user)
    {
        Collection<String> groups = groupManager.getGroupNamesForUser(user);
        String[] hrGroups = configurationManager.getHrReportGroups();

        if (groups == null || hrGroups == null)
        {
            return false;
        }

        List<String> cGroups = new ArrayList<String>(Arrays.asList(hrGroups));
        groups.retainAll(cGroups);
        return (groups.size() > 0);
    }

    private boolean isUserInReport(String user)
    {
        Collection<String> groups = groupManager.getGroupNamesForUser(user);
        String[] rGroups = configurationManager.getReportGroups();

        if (groups == null || rGroups == null)
        {
            return false;
        }

        List<String> cGroups = new ArrayList<String>(Arrays.asList(rGroups));
        groups.retainAll(cGroups);
        return (groups.size() > 0);
    }
}
