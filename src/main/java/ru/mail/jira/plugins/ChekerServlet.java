package ru.mail.jira.plugins;

import java.io.IOException;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import ru.mail.jira.plugins.structs.TimePair;
import com.atlassian.crowd.embedded.api.User;
import com.atlassian.jira.ComponentManager;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.security.groups.GroupManager;
import com.atlassian.jira.util.json.JSONException;
import com.atlassian.jira.util.json.JSONObject;

public class ChekerServlet
    extends HttpServlet
{
    /**
     * Unique ID.
     */
    private static final long serialVersionUID = 2418034821343773322L;

    /**
     * Logger.
     */
    private static Log log = LogFactory.getLog(ChekerServlet.class);

    /**
     * Group manager.
     */
    private final GroupManager groupManager;

    /**
     * Configuration manager.
     */
    private final ConfigurationManager configurationManager;

    /**
     * Constructor.
     */
    public ChekerServlet(
        GroupManager groupManager,
        ConfigurationManager configurationManager)
    {
        this.groupManager = groupManager;
        this.configurationManager = configurationManager;
    }

    private boolean checkUserGroups(User user)
    {
        if (user == null)
        {
            return false;
        }

        String[] arrSavedGroups = configurationManager.getReportGroups();
        Collection<String> cUserGroups = groupManager.getGroupNamesForUser(user.getName());
        if (arrSavedGroups == null || cUserGroups == null)
        {
            return false;
        }

        Collection <String> userGroups = new HashSet<String>(cUserGroups);
        Collection <String> savedGroups = new HashSet<String>(Arrays.asList(arrSavedGroups));

        userGroups.retainAll(savedGroups);
        return userGroups.size() > 0;
    }

    @Override
    protected void doGet(
        HttpServletRequest req,
        HttpServletResponse resp)
    throws ServletException, IOException
    {
        resp.setHeader("Expires", "0");
        // Set standard HTTP/1.1 no-cache headers.
        resp.setHeader("Cache-Control", "no-store, no-cache, must-revalidate");
        // Set IE extended HTTP/1.1 no-cache headers (use addHeader).
        resp.addHeader("Cache-Control", "post-check=0, pre-check=0");
        // Set standard HTTP/1.0 no-cache header.
        resp.setHeader("Pragma", "no-cache");

        JSONObject result = new JSONObject();

        JiraAuthenticationContext authenticationContext = ComponentManager.getInstance().getJiraAuthenticationContext();
        User user = authenticationContext.getLoggedInUser();

        try
        {
            if (user == null)
            {
                result.put("status", 4);
                resp.getWriter().write(result.toString());
                resp.getWriter().flush();
                return;
            }

            if (checkUserGroups(user))
            {
                TimePair timePair = new RegistratorSQL(configurationManager).getRegistrationTime(user.getName());
                if (timePair.isCTime() || timePair.isTimeOn())
                {
                    result.put("status", 0);
                }
                else
                {
                    result.put("status", 1);
                }
            }
            else
            {
                result.put("status", 3);
            }
        }
        catch (Exception e)
        {
            log.error("ChekerServlet::doGet", e);
            try
            {
                result.put("status", 2);
                result.put("error", e.getMessage());
            }
            catch (JSONException jsonex)
            {
                log.error("ChekerServlet::doGet", jsonex);
                resp.sendError(500, "JSON error");
                return;
            }
        }

        resp.getWriter().write(result.toString());
        resp.getWriter().flush();
    }

    @Override
    protected void doPost(
        HttpServletRequest req,
        HttpServletResponse resp)
    throws ServletException, IOException
    {
        String param = req.getParameter("type");
        if (param != null && param.length() > 0)
        {
            if (param.equals("cancel"))
            {
                try
                {
                    resp.getWriter().write(notRegister());
                    resp.getWriter().flush();
                }
                catch (JSONException e)
                {
                    resp.sendError(500, "JSON error");
                    return;
                }
            }
            else
            {
                try
                {
                    resp.getWriter().write(doRegister());
                    resp.getWriter().flush();
                }
                catch (JSONException e)
                {
                    resp.sendError(500, "JSON error");
                    return;
                }
            }
        }
        else
        {
            resp.sendError(404);
        }
    }

    public String doRegister()
    throws JSONException
    {
        JSONObject result = new JSONObject();

        JiraAuthenticationContext authenticationContext = ComponentManager.getInstance().getJiraAuthenticationContext();
        User user = authenticationContext.getLoggedInUser();

        if (user == null)
        {
            result.put("status", 4);
            return result.toString();
        }

        if (!checkUserGroups(user))
        {
            result.put("status", 3);
        }
        else
        {
            try
            {
                Timestamp arrival = new RegistratorSQL(configurationManager).register(user.getName());
                result.put("formattedDate", new SimpleDateFormat("yyyy-MM-dd HH:mm").format(arrival.getTime()));
                result.put("status", 0);
            }
            catch (Exception e)
            {
                log.error("", e);
                result.put("error", e.getMessage());
                result.put("status", 1);
            }
        }

        return result.toString();
    }

    public String notRegister()
    throws JSONException
    {
        JSONObject result = new JSONObject();

        JiraAuthenticationContext authenticationContext = ComponentManager.getInstance().getJiraAuthenticationContext();
        User user = authenticationContext.getLoggedInUser();

        if (user == null)
        {
            result.put("status", 4);
            return result.toString();
        }

        if (!checkUserGroups(user))
        {
            result.put("status", 3);
        }
        else
        {
            try
            {
                new RegistratorSQL(configurationManager).addCancelTime(user.getName());
                result.put("status", 0);
            }
            catch (SQLException e)
            {
                log.error("SQL processing exception", e);
                result.put("error", e.toString());
                result.put("status", 1);
            }
        }

        return result.toString();
    }
}
