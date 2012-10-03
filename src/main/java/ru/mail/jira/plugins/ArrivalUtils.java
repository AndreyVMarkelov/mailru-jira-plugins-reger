package ru.mail.jira.plugins;

import com.atlassian.crowd.embedded.api.User;
import com.atlassian.jira.ComponentManager;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.user.util.UserUtil;
import com.atlassian.jira.util.I18nHelper;
import com.atlassian.plugin.webresource.WebResourceManager;
import java.util.*;

/**
 * @author amarkelov
 */
public class ArrivalUtils
{
    public static String getDayOfWeekStr(int dayOfWeek, I18nHelper i18n)
    {
        switch (dayOfWeek)
        {
            case Calendar.MONDAY:
                return i18n.getText("arrival.calendar.day.monday");
            case Calendar.TUESDAY:
                return i18n.getText("arrival.calendar.day.tuesday");
            case Calendar.WEDNESDAY:
                return i18n.getText("arrival.calendar.day.wednesday");
            case Calendar.THURSDAY:
                return i18n.getText("arrival.calendar.day.thursday");
            case Calendar.FRIDAY:
                return i18n.getText("arrival.calendar.day.friday");
            case Calendar.SATURDAY:
                return i18n.getText("arrival.calendar.day.saturday");
            default:
                return i18n.getText("arrival.calendar.day.sunday");
        }
    }

    public static Map<Integer, String> getMonthNames()
    {
        Map<Integer, String> res = new HashMap<Integer, String>();
        res.put(0, "arrival.calendar.january");
        res.put(1, "arrival.calendar.febrary");
        res.put(2, "arrival.calendar.march");
        res.put(3, "arrival.calendar.april");
        res.put(4, "arrival.calendar.may");
        res.put(5, "arrival.calendar.june");
        res.put(6, "arrival.calendar.july");
        res.put(7, "arrival.calendar.august");
        res.put(8, "arrival.calendar.september");
        res.put(9, "arrival.calendar.october");
        res.put(10, "arrival.calendar.november");
        res.put(11, "arrival.calendar.december");

        return res;
    }

    public static String convertMissedMinutes(long missedTime)
    {
        if (missedTime <= 0)
        {
            return "";
        }

        long hours = missedTime/60;
        long minutes = missedTime%60;

        String hoursStr = Long.toString(hours);
        if (hours < 10)
        {
            hoursStr = "0" + hoursStr;
        }

        String minutesStr = Long.toString(minutes);
        if (minutes < 10)
        {
            minutesStr = "0" + minutesStr;
        }

        return hoursStr + ":" + minutesStr; 
    }

    /**
     * Return display name of user if user exists in JIRA.
     */
    public static String getDisplayUser(UserUtil userUtil, String user)
    {
        User userObj = userUtil.getUserObject(user);
        if (userObj != null)
        {
            return userObj.getDisplayName();
        }
        else
        {
            return user;
        }
    }

    public static Calendar getMonday()
    {
        Calendar now = Calendar.getInstance();
        int weekday = now.get(Calendar.DAY_OF_WEEK);
        if (weekday != Calendar.MONDAY)
        {
            // calculate how much to add
            // the 2 is the difference between Saturday and Monday
            int days = (Calendar.SATURDAY - weekday + 2) % 7 - 7;
            now.add(Calendar.DAY_OF_YEAR, days);
        }

        return now;
    }

    public static Map<String, Object> getParams(JiraAuthenticationContext authenticationContext)
    {
        WebResourceManager webResourceManager = ComponentManager.getInstance().getWebResourceManager();

        Map<String, Object> params = new HashMap<String, Object>();
        User user = ComponentManager.getInstance().getJiraAuthenticationContext().getLoggedInUser();
        params.put("user", user.getName());
        params.put("today", new Date());
        params.put("i18n", authenticationContext.getI18nHelper());
        params.put("webResourceManager", webResourceManager);
        params.put("weekDays", getWeekDays());

        return params;
    }

    public static ArrayList<Date> getWeekDays()
    {
        ArrayList<Date> weekdays = new ArrayList<Date>();
        Calendar weekday = getMonday();

        for (int count=0; count<7; count++)
        {
            weekdays.add(weekday.getTime());
            weekday.add(Calendar.DAY_OF_YEAR, 1);
        }

        return weekdays;
    }
}
