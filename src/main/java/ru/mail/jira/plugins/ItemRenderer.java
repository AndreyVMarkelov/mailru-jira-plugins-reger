package ru.mail.jira.plugins;

import java.io.IOException;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ru.mail.jira.plugins.structs.HrDate;
import ru.mail.jira.plugins.structs.TimeOffBean;

import com.atlassian.crowd.embedded.api.User;
import com.atlassian.jira.ComponentManager;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.security.groups.GroupManager;
import com.atlassian.jira.user.util.UserUtil;
import com.atlassian.jira.util.I18nHelper;
import com.atlassian.templaterenderer.RenderingException;
import com.atlassian.templaterenderer.TemplateRenderer;

/**
 * This class renders all "arrival" plug-in web-items view.
 * 
 * @author Andrey Markelov
 */
public class ItemRenderer
    extends HttpServlet
{
    /**
     * Logger.
     */
    private static Log log = LogFactory.getLog(ItemRenderer.class);

    /**
     * Serial ID.
     */
    private static final long serialVersionUID = -6264453674382063236L;

    /**
     * Configuration manager.
     */
    private final ConfigurationManager configurationManager;

    /**
     * Group manager.
     */
    private final GroupManager groupManager;

    /**
     * Template renderer.
     */
    private final TemplateRenderer renderer;

    /**
     * Utility for work with JIRA users.
     */
    private final UserUtil userUtil;

    /**
     * Constructor.
     */
    public ItemRenderer(
        TemplateRenderer renderer,
        GroupManager groupManager,
        ConfigurationManager configurationManager,
        UserUtil userUtil)
    {
        this.renderer = renderer;
        this.groupManager = groupManager;
        this.configurationManager = configurationManager;
        this.userUtil = userUtil;
    }

    @Override
    protected void doGet(
        HttpServletRequest req,
        HttpServletResponse resp)
    throws ServletException, IOException
    {
        User user = ComponentManager.getInstance().getJiraAuthenticationContext().getLoggedInUser();
        if (user == null)
        {
            resp.sendRedirect(getBaseUrl(req));
            return;
        }

        resp.setHeader("pragma", "no-cache");
        resp.setHeader("Cache-Control", "private, no-store, no-cache, must-revalidate");
        resp.setDateHeader ("Expires", 0);

        String action = req.getParameter("action");
        if (action.equals("reportAll"))
        {
            renderReportAll(req, resp);
        }
        else if (action.equals("reportUser"))
        {
            renderReportUser(req, resp);
        }
        else if (action.equals("calendar"))
        {
            renderCalendar(req, resp);
        }
        else if (action.equals("delays"))
        {
            renderDelays(req, resp);
        }
        else if (action.equals("register"))
        {
            renderRegister(req, resp);
        }
        else
        {
            renderList(req, resp);
        }
    }

    /**
     * Return context path of JIRA.
     */
    private String getBaseUrl(HttpServletRequest req)
    {
        return (req.getScheme() + "://" + req.getServerName() + ":" +
            req.getServerPort() + req.getContextPath());
    }

    private void renderCalendar(
        HttpServletRequest req,
        HttpServletResponse resp)
    throws RenderingException, IOException
    {
        User user = ComponentManager.getInstance().getJiraAuthenticationContext().getLoggedInUser();
        if (!isUserInHR(user.getName()))
        {
            resp.sendError(500, "User is not in group");
            return;
        }

        I18nHelper i18n = ComponentManager.getInstance().getJiraAuthenticationContext().getI18nHelper();
        Calendar cal = Calendar.getInstance();
        Integer year = cal.get(Calendar.YEAR);
        Integer month = cal.get(Calendar.MONTH) + 1;

        Map<Integer, Boolean> dayList;
        try
        {
            dayList = new RegistratorSQL(configurationManager).getDayoff(year, month);
        }
        catch (SQLException e)
        {
            log.error("ItemRenderer::renderCalendar", e);
            resp.sendError(500, e.getMessage());
            return;
        }

        Map<Integer, Object> dayListMap = new TreeMap<Integer, Object>();
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
            map.put("dayofweek", ArrivalUtils.getDayOfWeekStr(dayOfWeek, i18n));
            map.put("dayoff", dayOff.toString());
            map.put("holiday", holiday.toString());
            dayListMap.put(minDay, map);
            cal.add(Calendar.DAY_OF_MONTH, 1);
            minDay++;
        }

        Map<String, Object> parms = new HashMap<String, Object>();
        parms.put("lang", req.getLocale().getLanguage());
        parms.put("baseUrl", getBaseUrl(req));
        parms.put("year", year);
        parms.put("month", month);
        parms.put("dayList", dayListMap);

        resp.setContentType("text/html;charset=utf-8");
        renderer.render("/templates/Calendar.vm", parms, resp.getWriter());
    }

    private void renderDelays(
        HttpServletRequest req,
        HttpServletResponse resp)
    throws RenderingException, IOException
    {
        JiraAuthenticationContext authContext = ComponentManager.getInstance().getJiraAuthenticationContext();
        User u = ComponentManager.getInstance().getJiraAuthenticationContext().getLoggedInUser();
        if (!isUserInReport(u.getName()) && !isUserInHR(u.getName()))
        {
            resp.sendError(500, "User is not in group");
            return;
        }

        Map<Long, Object> map = new TreeMap<Long, Object>();

        Calendar cal = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

        List<TimeOffBean> list;
        try
        {
            list = new RegistratorSQL(configurationManager).getTimeOffList();
        }
        catch (SQLException e)
        {
            resp.sendError(500, e.getMessage());
            return;
        }

        for (TimeOffBean tob : list)
        {
            Map<String, String> delayMap = new HashMap<String, String>();
            delayMap.put("user", ArrivalUtils.getDisplayUser(userUtil, tob.getUser()));
            delayMap.put("reporter", ArrivalUtils.getDisplayUser(userUtil, tob.getReporter()));
            delayMap.put("delayDate", sdf.format(tob.getDelayDate()));
            delayMap.put("delayTime", tob.getDelayTime());
            delayMap.put("absent", tob.getAbsent());
            delayMap.put("comment", tob.getComment());
            map.put(tob.getId(), delayMap);
        }

        Map<String, String> userMap = new TreeMap<String, String>();
        for (String group : configurationManager.getReportGroups())
        {
            Collection<User> users = groupManager.getUsersInGroup(group);
            for (User user : users)
            {
                userMap.put(user.getName(), user.getDisplayName());
            }
        }

        Map<String, Object> parms = new HashMap<String, Object>();
        parms.put("lang", req.getLocale().getLanguage());
        parms.put("baseUrl", getBaseUrl(req));
        parms.put("delays", map);
        parms.put("delayDateDefault", sdf.format(cal.getTime()));
        parms.put("currUser", authContext.getLoggedInUser().getName());
        parms.put("userMap", userMap);

        resp.setContentType("text/html;charset=utf-8");
        renderer.render("/templates/Delays.vm", parms, resp.getWriter());
    }

    private void renderList(
        HttpServletRequest req,
        HttpServletResponse resp)
    throws RenderingException, IOException
    {
        boolean isEmployee = false;
        boolean isHR = false;

        String user = ComponentManager.getInstance().getJiraAuthenticationContext().getLoggedInUser().getName();
        for (String group : configurationManager.getReportGroups())
        {
            if (groupManager.getGroup(group).containsUser(user))
            {
                isEmployee = true;
                break;
            }
        }

        for (String hrGroup : configurationManager.getHrReportGroups())
        {
            if (groupManager.getGroup(hrGroup).containsUser(user))
            {
                isHR = true;
                break;
            }
        }

        if (!isEmployee && !isHR)
        {
            resp.sendError(500, "User is not in group");
            return;
        }

        Map<String, Object> parms = new HashMap<String, Object>();
        parms.put("lang", req.getLocale().getLanguage());
        parms.put("baseUrl", getBaseUrl(req));
        parms.put("isEmployee", isEmployee);
        parms.put("isHR", isHR);

        resp.setContentType("text/html;charset=utf-8");
        renderer.render("/templates/ItemList.vm", parms, resp.getWriter());
    }

    private void renderRegister(
        HttpServletRequest req,
        HttpServletResponse resp)
    throws RenderingException, IOException
    {
        JiraAuthenticationContext authenticationContext = ComponentManager.getInstance().getJiraAuthenticationContext();
        User user = authenticationContext.getLoggedInUser();
        if (!isUserInReport(user.getName()))
        {
            resp.sendError(500, "User is not in group");
            return;
        }

        I18nHelper i18n = authenticationContext.getI18nHelper();

        Map<String, Object> parms = new HashMap<String, Object>();
        parms.put("lang", req.getLocale().getLanguage());
        parms.put("baseUrl", getBaseUrl(req));
        parms.put("i18n", i18n);

        Timestamp arrival;
        try
        {
            arrival = new RegistratorSQL(configurationManager).register(user.getName());
            parms.put("formattedDate", new SimpleDateFormat("yyyy-MM-dd").format(arrival.getTime()));
            parms.put("formattedTime", new SimpleDateFormat("HH:mm").format(arrival.getTime()));
            parms.put("status", 0);
        }
        catch (Exception e)
        {
            log.error("ItemRenderer::renderRegister", e);
            parms.put("error", e.getMessage());
            parms.put("status", 1);
        }

        resp.setContentType("text/html;charset=utf-8");
        renderer.render("/templates/Register.vm", parms, resp.getWriter());
    }

    private void renderReportAll(
        HttpServletRequest req,
        HttpServletResponse resp)
    throws RenderingException, IOException
    {
        User u = ComponentManager.getInstance().getJiraAuthenticationContext().getLoggedInUser();
        if (!isUserInHR(u.getName()))
        {
            resp.sendError(500, "User is not in group");
            return;
        }

        List<HrDate> hrDates;
        try
        {
            hrDates = new RegistratorSQL(configurationManager).getLastHrDates();
        }
        catch (SQLException sqlex)
        {
            resp.sendError(500, sqlex.getMessage());
            return;
        }

        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMinimum(Calendar.DAY_OF_MONTH));
        Date minDate = cal.getTime();
        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH));
        Date maxDate = cal.getTime();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        Date nowDate = new Date();

        Collection<User> users;
        Map<String, String> strUsers = new TreeMap<String, String>();
        for (String rGroup : configurationManager.getReportGroups())
        {
            users = groupManager.getUsersInGroup(rGroup);
            for (User user : users)
            {
                strUsers.put(user.getName(), user.getDisplayName());
            }
        }

        Date lasthrDate = null;
        HrDate hrDate = null;
        if (hrDates.size() > 0)
        {
            hrDate = hrDates.get(0);
            Calendar hrCal = Calendar.getInstance();
            hrCal.setTime(hrDate.getHrDate());
            hrCal.add(Calendar.DAY_OF_MONTH, 1);
            lasthrDate = hrCal.getTime();
            if (nowDate.compareTo(lasthrDate) < 0)
            {
                nowDate = lasthrDate;
            }
        }

        SortedMap<String, String> sortedData = new TreeMap<String, String>(new UserMapComparator(strUsers));
        sortedData.putAll(strUsers);

        Map<String, Object> parms = new HashMap<String, Object>();
        parms.put("lang", req.getLocale().getLanguage());
        parms.put("sdf", sdf);
        parms.put("baseUrl", getBaseUrl(req));
        parms.put("minDate", sdf.format(minDate));
        parms.put("maxDate", sdf.format(maxDate));
        parms.put("nowDate", sdf.format(nowDate));
        parms.put("users", sortedData);
        parms.put("hrDates", hrDates);
        parms.put("months", ArrivalUtils.getMonthNames());
        parms.put("hrDate", hrDate);
        parms.put("lasthrDate", lasthrDate);

        resp.setContentType("text/html;charset=utf-8");
        renderer.render("/templates/ReportAll.vm", parms, resp.getWriter());
    }

    private void renderReportUser(
        HttpServletRequest req,
        HttpServletResponse resp)
    throws RenderingException, IOException
    {
        User user = ComponentManager.getInstance().getJiraAuthenticationContext().getLoggedInUser();
        if (!isUserInReport(user.getName()))
        {
            resp.sendError(500, "User is not in group");
            return;
        }

        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMinimum(Calendar.DAY_OF_MONTH));
        Date minDate = cal.getTime();
        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH));
        Date maxDate = cal.getTime();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        Date nowDate = new Date();

        List<HrDate> hrDates;
        try
        {
            hrDates = new RegistratorSQL(configurationManager).getLastHrDates();
        }
        catch (SQLException sqlex)
        {
            resp.sendError(500, sqlex.getMessage());
            return;
        }

        Date lasthrDate = null;
        HrDate hrDate = null;
        if (hrDates.size() > 0)
        {
            hrDate = hrDates.get(0);
            Calendar hrCal = Calendar.getInstance();
            hrCal.setTime(hrDate.getHrDate());
            hrCal.add(Calendar.DAY_OF_MONTH, 1);
            lasthrDate = hrCal.getTime();
            if (nowDate.compareTo(lasthrDate) < 0)
            {
                nowDate = lasthrDate;
            }
        }

        Map<String, Object> parms = new HashMap<String, Object>();
        parms.put("lang", req.getLocale().getLanguage());
        parms.put("sdf", sdf);
        parms.put("baseUrl", getBaseUrl(req));
        parms.put("minDate", sdf.format(minDate));
        parms.put("maxDate", sdf.format(maxDate));
        parms.put("nowDate", sdf.format(nowDate));
        parms.put("hrDates", hrDates);
        parms.put("months", ArrivalUtils.getMonthNames());
        parms.put("hrDate", hrDate);
        parms.put("lasthrDate", lasthrDate);

        resp.setContentType("text/html;charset=utf-8");
        renderer.render("/templates/ReportUser.vm", parms, resp.getWriter());
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
