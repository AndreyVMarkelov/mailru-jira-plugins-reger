package ru.mail.jira.plugins;

import java.util.Map;
import com.atlassian.jira.plugin.webfragment.conditions.AbstractJiraCondition;
import com.atlassian.jira.plugin.webfragment.model.JiraHelper;
import com.atlassian.plugin.PluginParseException;
import com.opensymphony.user.User;

/**
 * Check visibility "web-item" for groups "HR of department of game division"
 *   and "employees of department of game division".
 * 
 * @author Andrey Markelov
 */
@SuppressWarnings("deprecation")
public class UserIsInGameGroupCondition
    extends AbstractJiraCondition
{
    /**
     * Plug-In configuration manager.
     */
    private final ConfigurationManager configurationManager;

    /**
     * Constructor.
     */
    public UserIsInGameGroupCondition(ConfigurationManager configurationManager)
    {
        this.configurationManager = configurationManager;
    }

    @Override
    public void init(@SuppressWarnings("rawtypes") Map params)
    throws PluginParseException
    {
        super.init(params);
    }

    @Override
    public boolean shouldDisplay(User user, JiraHelper arg1)
    {
        boolean inGroup = false;

        String[] reportGroups = configurationManager.getReportGroups();
        String[] reportHrGroups = configurationManager.getHrReportGroups();

        if(user != null && reportGroups != null)
        {
            for (String group : reportGroups)
            if (user.inGroup(group))
            {
                inGroup = true;
                break;
            }
        }

        if(user != null && reportHrGroups != null)
        {
            for (String group : reportHrGroups)
            if (user.inGroup(group))
            {
                inGroup = true;
                break;
            }
        }

        return inGroup;
    }
}
