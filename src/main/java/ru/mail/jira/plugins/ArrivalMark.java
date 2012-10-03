package ru.mail.jira.plugins;

import java.io.IOException;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;

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
import org.apache.velocity.exception.VelocityException;
import ru.mail.jira.plugins.structs.TimePair;
import com.atlassian.crowd.embedded.api.User;
import com.atlassian.jira.ComponentManager;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.security.groups.GroupManager;
import com.atlassian.jira.util.json.JSONException;
import com.atlassian.jira.util.json.JSONObject;

/**
 * @author grundic
 */
@Path("/arrival-mark")
public class ArrivalMark
{
    private static Log log = LogFactory.getLog(ArrivalMark.class);

    private final GroupManager groupManager;

    private final ConfigurationManager configurationManager;

    public ArrivalMark(
        ConfigurationManager configurationManager,
        GroupManager groupManager)
    {
        this.configurationManager = configurationManager;
        this.groupManager = groupManager;
    }

    @GET
    @Produces ({ MediaType.APPLICATION_JSON})
    @Path("/check-arrival")
    public Response checkArrived(@Context HttpServletRequest req)
    throws IOException
    {
        JiraAuthenticationContext authenticationContext = ComponentManager.getInstance().getJiraAuthenticationContext();
        User user = authenticationContext.getLoggedInUser();

        JSONObject result = new JSONObject();

        TimePair timePair;
        try
        {
            if (checkUserGroups(authenticationContext))
            {
                timePair = new RegistratorSQL(configurationManager).getRegistrationTime(user.getName());
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
            log.error("ArrivalMark::checkArrived", e);
            try
            {
                result.put("status", 2);
                result.put("error", e.getMessage());
            }
            catch (JSONException jsonex)
            {
                log.error("ArrivalMark::checkArrived", jsonex);
                return Response.serverError().build();
            }
        }

        return Response.ok(result.toString()).header("'Cache-Control", "no-cache, must-revalidate").header("Pragma", "no-cache").header("Expires", "0").build();
    }

    private boolean checkUserGroups(JiraAuthenticationContext authenticationContext)
    {
        if (authenticationContext.getLoggedInUser() == null)
        {
            return false;
        }

        String[] arrSavedGroups = configurationManager.getReportGroups();
        Collection<String> cUserGroups = groupManager.getGroupNamesForUser(authenticationContext.getLoggedInUser().getName());
        if (arrSavedGroups == null || cUserGroups == null)
        {
            return false;
        }

        Collection <String> userGroups = new HashSet<String>(cUserGroups);
        Collection <String> savedGroups = new HashSet<String>(Arrays.asList(arrSavedGroups));

        userGroups.retainAll(savedGroups);
        return userGroups.size() > 0;
    }

    /**
     * Return context path of JIRA.
     */
    private String getBaseUrl(HttpServletRequest req)
    {
        return (req.getScheme() + "://" + req.getServerName() + ":" +
            req.getServerPort() + req.getContextPath());
    }

    @GET
    @Produces ({ MediaType.APPLICATION_JSON})
    @Path("/arrivals")
    public Response getArrivals(@Context HttpServletRequest req)
    throws JSONException
    {
        JiraAuthenticationContext authenticationContext = ComponentManager.getInstance().getJiraAuthenticationContext();
        User user = authenticationContext.getLoggedInUser();

        Map<String, Object> params = ArrivalUtils.getParams(authenticationContext);
        params.put("i18n", authenticationContext.getI18nHelper());
        params.put("baseUrl", getBaseUrl(req));

        if (checkUserGroups(authenticationContext))
        {
            RegistratorSQL reg = new RegistratorSQL(configurationManager);
            try
            {
                TimePair timePair = reg.getRegistrationTime(user.getName());
                if (timePair.isTimeOn() || timePair.isCTime())
                {
                    params.put("arrivals", reg.getWeekReport(user.getName()));
                    params.put("header_fmt", new SimpleDateFormat("d/MMM [EEE]"));
                    params.put("arrival_fmt", new SimpleDateFormat("HH:mm:ss"));
                    params.put("view", "0");
                }
                else
                {
                    params.put("arrivals", reg.getWeekReport(user.getName()));
                    params.put("header_fmt", new SimpleDateFormat("d/MMM [EEE]"));
                    params.put("arrival_fmt", new SimpleDateFormat("HH:mm:ss"));
                    params.put("view", "1");
                }
            }
            catch (SQLException e)
            {
                log.error("SQL processing exception", e);
                params.put("error", e.toString());
                params.put("view", "2");
            }
        }
        else
        {
            params.put("view", "3");
        }

        try
        {
            return Response.ok(new HTMLRepresentation(ComponentAccessor.getVelocityManager().getBody("templates/", "arrival-gadget.vm", params))).build();
        }
        catch (VelocityException e)
        {
            log.error("velocity exception", e);
            return Response.serverError().build();
        }
    }

    @POST
    @Produces ({ MediaType.APPLICATION_JSON})
    @Path ("/notregister")
    public Response notRegister()
    throws JSONException
    {
        JiraAuthenticationContext authenticationContext = ComponentManager.getInstance().getJiraAuthenticationContext();
        User user = authenticationContext.getLoggedInUser();

        if (!checkUserGroups(authenticationContext))
        {
            return Response.serverError().build();
        }

        JSONObject result = new JSONObject();
        result.put("i18n", authenticationContext.getI18nHelper());

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

        return Response.ok(result.toString()).build();
    }

    @POST
    @Produces ({ MediaType.APPLICATION_JSON})
    @Path("/register")
    public Response doRegister()
    throws JSONException
    {
        JiraAuthenticationContext authenticationContext = ComponentManager.getInstance().getJiraAuthenticationContext();
        User user = authenticationContext.getLoggedInUser();

        if (!checkUserGroups(authenticationContext))
        {
            return Response.serverError().build();
        }

        JSONObject result = new JSONObject();

        Timestamp arrival;
        try
        {
            arrival = new RegistratorSQL(configurationManager).register(user.getName());
            result.put("formattedDate", new SimpleDateFormat("yyyy-MM-dd HH:mm").format(arrival.getTime()));
            result.put("status", 0);
        }
        catch (Exception e)
        {
            log.error("", e);
            result.put("error", e.getMessage());
            result.put("status", 1);
        }

        return Response.ok(result.toString()).header("'Cache-Control", "no-cache, must-revalidate").header("Pragma", "no-cache").header("Expires", "0").build();
    }
}
