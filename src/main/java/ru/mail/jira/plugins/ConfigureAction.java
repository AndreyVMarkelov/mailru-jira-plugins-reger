package ru.mail.jira.plugins;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import com.atlassian.crowd.embedded.api.Group;
import com.atlassian.jira.security.Permissions;
import com.atlassian.jira.security.groups.GroupManager;
import com.atlassian.jira.web.action.JiraWebActionSupport;
import com.atlassian.sal.api.ApplicationProperties;

/**
 * Configure plugin settings.
 * 
 * @author andrey
 */
public class ConfigureAction
    extends JiraWebActionSupport
{
    /**
     * Serial ID.
     */
    private static final long serialVersionUID = 1324677443874546986L;

    private final ApplicationProperties applicationProperties;

    private final ConfigurationManager configurationManager;

    private final GroupManager groupManager;

    /*
     * Saved parameters
     */
    private String dbHost;
    private String dbPort;
    private String database;
    private String username;
    private String password;
    private String mailRecipient;
    private boolean isSaved = false;
    private String[] selectedReportGroups = new String[0];
    private String[] selectedHrReportGroups = new String[0];
    private List<String> savedReportGroups;
    private List<String> savedHrReportGroups;

    public ConfigureAction(
        ApplicationProperties applicationProperties,
        ConfigurationManager configurationManager,
        GroupManager groupManager)
    throws Exception
    {
        this.applicationProperties = applicationProperties;
        this.configurationManager = configurationManager;
        this.groupManager = groupManager;

        // initialize params from saved properties
        dbHost = configurationManager.getDbHost();
        dbPort = configurationManager.getDbPort();
        database = configurationManager.getDatabase();
        username = configurationManager.getUsername();
        password = configurationManager.getPassword();
        mailRecipient = configurationManager.getMailRecipient();
        selectedReportGroups = configurationManager.getReportGroups();
        selectedHrReportGroups = configurationManager.getHrReportGroups();
        savedReportGroups = selectedReportGroups == null ? null : Arrays.asList(configurationManager.getReportGroups());
        savedHrReportGroups = selectedHrReportGroups == null ? null : Arrays.asList(configurationManager.getHrReportGroups());
    }

    public boolean hasAdminPermission()
    {
        return getPermissionManager().hasPermission(Permissions.ADMINISTER, getLoggedInUser());
    }

    @Override
    protected String doExecute()
    throws Exception
    {
        configurationManager.setDbHost(dbHost);
        configurationManager.setDbPort(dbPort);
        configurationManager.setDatabase(database);
        configurationManager.setUsername(username);
        configurationManager.setPassword(password);
        configurationManager.setMailRecipient(mailRecipient);
        configurationManager.setReportGroups(selectedReportGroups);
        configurationManager.setHrReportGroups(selectedHrReportGroups);
        savedReportGroups = Arrays.asList(configurationManager.getReportGroups());
        savedHrReportGroups = Arrays.asList(configurationManager.getHrReportGroups());
        setSaved(true);

        return getRedirect("ViewRegerConfiguration!default.jspa?saved=true");
    }

    @Override
    protected void doValidation()
    {
        super.doValidation();

        if (dbHost.isEmpty())
        {
            addErrorMessage("arrival.admin.configuration.error.dbhost");
            return;
        }

        if (dbPort.isEmpty())
        {
            addErrorMessage("arrival.admin.configuration.error.dbport");
            return;
        }
        else
        {
            try
            {
                Integer.parseInt(dbPort);
            }
            catch (NumberFormatException nex)
            {
                addErrorMessage("arrival.admin.configuration.error.dbport");
                return;
            }
        }

        if (database.isEmpty())
        {
            addErrorMessage("arrival.admin.configuration.error.dbname");
            return;
        }

        if (username.isEmpty())
        {
            addErrorMessage("arrival.admin.configuration.error.dbusername");
            return;
        }

        if (password.isEmpty())
        {
            addErrorMessage("arrival.admin.configuration.error.dbpassword");
            return;
        }

        if (!RegistratorSQL.isDriverInitialized())
        {
            addErrorMessage("arrival.admin.configuration.error.jdbcdriver");
            return;
        }

        RegistratorSQL.initDataSource(dbHost, dbPort, database, username, password);
    }

    public Collection<Group> getAllGroups()
    {
        return groupManager.getAllGroups();
    }

    public String getBaseUrl()
    {
        return applicationProperties.getBaseUrl();
    }

    public String getDatabase()
    {
        return database;
    }

    public String getDbHost()
    {
        return dbHost;
    }

    public String getDbPort()
    {
        return dbPort;
    }

    public String getMailRecipient()
    {
        return mailRecipient;
    }

    public String getPassword()
    {
        return password;
    }

    public List<String> getSavedHrReportGroups()
    {
        return savedHrReportGroups;
    }

    public List<String> getSavedReportGroups()
    {
        return savedReportGroups;
    }

    public String[] getSelectedHrReportGroups()
    {
        return selectedHrReportGroups;
    }

    public String[] getSelectedReportGroups()
    {
        return selectedReportGroups;
    }

    public String getUsername()
    {
        return username;
    }

    public boolean isSaved()
    {
        return isSaved;
    }

    public void setDatabase(String database)
    {
        this.database = database;
    }

    public void setDbHost(String dbHost)
    {
        this.dbHost = dbHost;
    }

    public void setDbPort(String dbPort)
    {
        this.dbPort = dbPort;
    }

    public void setMailRecipient(String mailRecipient)
    {
        this.mailRecipient = mailRecipient;
    }

    public void setPassword(String password)
    {
        this.password = password;
    }

    public void setSaved(boolean saved)
    {
        isSaved = saved;
    }

    public void setSelectedHrReportGroups(String[] selectedHrReportGroups)
    {
        this.selectedHrReportGroups = selectedHrReportGroups;
    }

    public void setSelectedReportGroups(String[] selectedReportGroups)
    {
        this.selectedReportGroups = selectedReportGroups;
    }

    public void setUsername(String username)
    {
        this.username = username;
    }
}
