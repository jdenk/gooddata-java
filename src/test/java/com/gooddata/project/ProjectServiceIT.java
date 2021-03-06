package com.gooddata.project;

import com.gooddata.AbstractGoodDataIT;
import com.gooddata.GoodDataException;
import com.gooddata.collections.PageRequest;
import com.gooddata.gdc.AsyncTask;
import com.gooddata.gdc.FeatureFlag;
import com.gooddata.gdc.FeatureFlags;
import com.gooddata.gdc.TaskStatus;
import com.gooddata.gdc.UriResponse;
import org.codehaus.jackson.map.ObjectMapper;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import static com.gooddata.JsonMatchers.isJsonString;
import static com.gooddata.project.ProjectFeatureFlags.FEATURE_FLAGS_TEMPLATE;
import static com.gooddata.project.ProjectFeatureFlag.FEATURE_FLAG_TEMPLATE;
import static com.gooddata.util.ResourceUtils.readFromResource;
import static com.gooddata.util.ResourceUtils.readStringFromResource;
import static net.jadler.Jadler.onRequest;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

import java.util.Collection;
import java.util.List;
import java.util.Set;

public class ProjectServiceIT extends AbstractGoodDataIT {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final String PROJECT_ID = "PROJECT_ID";
    private static final String PROJECT_URI = "/gdc/projects/" + PROJECT_ID;

    private Project loading;
    private Project enabled;
    private Project deleted;

    @BeforeClass
    public void setUp() throws Exception {
        loading = MAPPER.readValue(readFromResource("/project/project-loading.json"), Project.class);
        enabled = MAPPER.readValue(readFromResource("/project/project.json"), Project.class);
        deleted = MAPPER.readValue(readFromResource("/project/project-deleted.json"), Project.class);
    }

    @Test
    public void shouldCreateProject() throws Exception {
        onRequest()
                .havingMethodEqualTo("POST")
                .havingPathEqualTo(Projects.URI)
                .respond()
                .withBody(MAPPER.writeValueAsString(new UriResponse(PROJECT_URI)))
                .withStatus(202)
        ;
        onRequest()
                .havingMethodEqualTo("GET")
                .havingPathEqualTo(PROJECT_URI)
            .respond()
                .withBody(MAPPER.writeValueAsString(loading))
                .withStatus(202)
            .thenRespond()
                .withBody(MAPPER.writeValueAsString(enabled))
                .withStatus(200)
        ;

        final Project project = gd.getProjectService().createProject(new Project("TITLE", "AUTH_TOKEN")).get();
        assertThat(project, is(notNullValue()));
        assertThat(project.getTitle(), is("TITLE"));
    }

    @Test(expectedExceptions = GoodDataException.class)
    public void shouldFailWhenPostFails() throws Exception {
        onRequest()
                .havingMethodEqualTo("POST")
                .havingPathEqualTo(Projects.URI)
            .respond()
                .withStatus(400)
        ;
        gd.getProjectService().createProject(new Project("TITLE", "AUTH_TOKEN")).get();
    }

    @Test(expectedExceptions = GoodDataException.class)
    public void shouldFailWhenPollFails() throws Exception {
        onRequest()
                .havingMethodEqualTo("POST")
                .havingPathEqualTo(Projects.URI)
            .respond()
                .withBody(MAPPER.writeValueAsString(new UriResponse(PROJECT_URI)))
                .withStatus(202)
        ;
        onRequest()
                .havingMethodEqualTo("GET")
                .havingPathEqualTo(PROJECT_URI)
            .respond()
                .withStatus(400)
        ;

        gd.getProjectService().createProject(new Project("TITLE", "AUTH_TOKEN")).get();
    }


    @Test(expectedExceptions = GoodDataException.class)
    public void shouldFailWhenCantCreateProject() throws Exception {
        onRequest()
                .havingMethodEqualTo("POST")
                .havingPathEqualTo(Projects.URI)
            .respond()
                .withBody(MAPPER.writeValueAsString(new UriResponse(PROJECT_URI)))
                .withStatus(202)
        ;
        onRequest()
                .havingMethodEqualTo("GET")
                .havingPathEqualTo(PROJECT_URI)
            .respond()
                .withBody(MAPPER.writeValueAsString(deleted))
                .withStatus(200)
        ;

        gd.getProjectService().createProject(new Project("TITLE", "AUTH_TOKEN")).get();
    }

    @Test
    public void shouldRemoveProject() throws Exception {
        onRequest()
                .havingMethodEqualTo("DELETE")
                .havingPathEqualTo(PROJECT_URI)
                .respond()
                .withStatus(202);

        gd.getProjectService().removeProject(enabled);
    }

    @Test
    public void shouldReturnProjectTemplates() throws Exception {
        onRequest()
                .havingPathEqualTo("/gdc/md/" + PROJECT_ID + "/templates")
            .respond()
                .withBody(readFromResource("/project/project-templates.json"));

        final Collection<ProjectTemplate> templates = gd.getProjectService().getProjectTemplates(enabled);
        assertThat(templates, is(notNullValue()));
        assertThat(templates, hasSize(1));
    }

        @Test
    public void shouldReturnAvailableValidations() throws Exception {
        onRequest()
                .havingPathEqualTo("/gdc/md/" + PROJECT_ID + "/validate")
                    .respond()
                    .withBody(readFromResource("/project/project-validationAvail.json"));

        final Set<ProjectValidationType> validations = gd.getProjectService().getAvailableProjectValidationTypes(enabled);
        assertThat(validations, notNullValue());
        assertThat(validations, hasSize(7));

    }

    @Test
    public void shouldValidateProject() throws Exception {
        final String validateUri = "/gdc/md/"  + PROJECT_ID + "/validate";
        final String task1Uri = validateUri + "/task/TASK_ID";
        final String task2Uri = validateUri + "/task/TASK_ID2";
        final String resultUri = validateUri + "/result/RESULT_ID";

        onRequest()
                .havingPathEqualTo(validateUri)
            .respond()
                .withBody(readFromResource("/project/project-validationAvail.json"));

        onRequest()
                .havingPathEqualTo(validateUri)
                .havingMethodEqualTo("POST")
                .havingBody(isJsonString("/project/project-validate.json"))
            .respond()
                .withBody(MAPPER.writeValueAsString(new AsyncTask(task1Uri)))
                .withStatus(201);

        onRequest()
                .havingPathEqualTo(task1Uri)
            .respond()
                .withBody(MAPPER.writeValueAsString(new UriResponse(task2Uri)))
                .withHeader("Location", task2Uri)
                .withStatus(303);

        onRequest()
                .havingPathEqualTo(task2Uri)
            .respond()
                .withBody(MAPPER.writeValueAsString(new AsyncTask(task2Uri)))
                .withStatus(202)
            .thenRespond()
                .withBody(MAPPER.writeValueAsString(new UriResponse(resultUri)))
                .withHeader("Location", resultUri)
                .withStatus(303);

        onRequest()
                .havingPathEqualTo(resultUri)
            .respond()
                .withBody(readFromResource("/project/project-validationResults.json"))
                .withStatus(200);

        final ProjectValidationResults validateResult = gd.getProjectService().validateProject(enabled).get();

        assertThat(validateResult, notNullValue());
    }

    @Test
    public void shouldValidateProject2() throws Exception {
        final String validateUri = "/gdc/md/" + PROJECT_ID + "/validate";
        final String task1Uri = validateUri + "/task/TASK_ID";
        final String task2Uri = validateUri + "/task/TASK_ID2";
        final String resultUri = validateUri + "/result/RESULT_ID";

        onRequest()
                .havingPathEqualTo(validateUri)
            .respond()
                .withBody(readFromResource("/project/project-validationAvail.json"));

        onRequest()
                .havingPathEqualTo(validateUri)
                .havingMethodEqualTo("POST")
                .havingBody(isJsonString("/project/project-validate.json"))
            .respond()
                .withBody(MAPPER.writeValueAsString(new AsyncTask(task1Uri)))
                .withStatus(201);

        onRequest()
                .havingPathEqualTo(task1Uri)
            .respond()
                .withBody(MAPPER.writeValueAsString(new UriResponse(task2Uri)))
                .withHeader("Location", task1Uri)
                .withStatus(303)
            .thenRespond()
                .withBody(MAPPER.writeValueAsString(new TaskStatus("RUNNING", task2Uri)))
                .withHeader("Location", task1Uri)
                .withStatus(202)
            .thenRespond()
                .withBody(MAPPER.writeValueAsString(new UriResponse(resultUri)))
                .withHeader("Location", resultUri)
                .withStatus(303)
        ;
        onRequest()
                .havingPathEqualTo(resultUri)
            .respond()
                .withBody(readFromResource("/project/project-validationResults.json"))
                .withStatus(200);

        final ProjectValidationResults validateResult = gd.getProjectService().validateProject(enabled).get();

        assertThat(validateResult, notNullValue());
    }

    @Test
    public void shouldReturnProjectRoles() {
        onRequest()
                .havingMethodEqualTo("GET")
                .havingPathEqualTo("/gdc/projects/PROJECT_ID/roles")
        .respond()
                .withBody(readFromResource("/project/project-roles.json"))
                .withStatus(200);

        onRequest()
                .havingMethodEqualTo("GET")
                .havingPathEqualTo("/gdc/projects/PROJECT_ID/roles/ROLE1")
        .respond()
                .withBody(readFromResource("/project/project-role.json"))
                .withStatus(200);

        onRequest()
                .havingMethodEqualTo("GET")
                .havingPathEqualTo("/gdc/projects/PROJECT_ID/roles/ROLE2")
        .respond()
                .withBody(readFromResource("/project/project-role2.json"))
                .withStatus(200);

        final Set<Role> roles = gd.getProjectService().getRoles(enabled);
        assertThat(roles, notNullValue());
        assertThat(roles, hasSize(2));
    }

    @Test
    public void shouldReturnProjectRoleForUri() {
        final String roleUri = "/gdc/projects/PROJECT_ID/roles/ROLE1";
        onRequest()
                .havingMethodEqualTo("GET")
                .havingPathEqualTo(roleUri)
        .respond()
                .withBody(readFromResource("/project/project-role.json"))
                .withStatus(200);

        final Role role = gd.getProjectService().getRoleByUri(roleUri);
        assertThat(role, notNullValue());
        assertThat(role.getTitle(), is("Embedded Dashboard Only"));
    }

    @Test
    public void shouldListPagedUsers() throws Exception {
        onRequest()
                .havingMethodEqualTo("GET")
                .havingPathEqualTo(Users.TEMPLATE.expand("PROJECT_ID").toString())
        .respond()
                .withBody(readFromResource("/project/project-users.json"))
                .withStatus(200);
        onRequest()
                .havingMethodEqualTo("GET")
                .havingPathEqualTo(Users.TEMPLATE.expand("PROJECT_ID").toString())
                .havingQueryStringEqualTo("offset=1&limit=1")
        .respond()
                .withBody(readFromResource("/project/project-users-empty.json"))
                .withStatus(200);

        final List<User> firstPage = gd.getProjectService().listUsers(enabled);
        assertThat(firstPage, notNullValue());
        assertThat(firstPage, hasSize(1));

        final List<User> secondPage = gd.getProjectService().listUsers(enabled, new PageRequest(firstPage.size(), 1));
        assertThat(secondPage, notNullValue());
        assertThat(secondPage, empty());
    }

    @Test
    public void shouldListAggregatedFeatureFlags() throws Exception {
        onRequest()
                .havingMethodEqualTo("GET")
                .havingPathEqualTo(FeatureFlags.AGGREGATED_FEATURE_FLAGS_TEMPLATE.expand(PROJECT_ID).toString())
                .respond()
                .withBody(readStringFromResource("/gdc/featureFlags.json"))
                .withStatus(200);

        final List<FeatureFlag> featureFlags = gd.getProjectService().listAggregatedFeatureFlags(enabled);

        assertThat(featureFlags, hasSize(2));
        assertThat(featureFlags, contains(
                new FeatureFlag("testFeature", true),
                new FeatureFlag("testFeature2", false)));
    }

    @Test
    public void shouldListProjectFeatureFlags() throws Exception {

        mockListFeatureFlagsRequest();

        final List<ProjectFeatureFlag> projectFeatureFlags = gd.getProjectService().listFeatureFlags(enabled);

        assertThat(projectFeatureFlags, contains(
                new ProjectFeatureFlag("myCoolFeature", true),
                new ProjectFeatureFlag("mySuperCoolFeature", true),
                new ProjectFeatureFlag("mySuperSecretFeature", false)));
    }

    @Test
    public void shouldGetProjectFeatureFlag() {
        final String featureFlagName = "myCoolFeature";

        mockGetFeatureFlagRequest(getFeatureFlagUri(featureFlagName), true);

        final ProjectFeatureFlag featureFlag = gd.getProjectService().getFeatureFlag(enabled, featureFlagName);

        checkFeatureFlag(featureFlag, featureFlagName, true);
    }

    @Test
    public void shouldSetProjectFeatureFlag() {
        final String featureFlagName = "myCoolFeature";

        final String featureFlagUri = getFeatureFlagUri(featureFlagName);

        mockCreateFeatureFlagRequest(featureFlagUri);

        mockGetFeatureFlagRequest(featureFlagUri, true);

        final ProjectFeatureFlag featureFlag = gd.getProjectService().createFeatureFlag(enabled,
                new ProjectFeatureFlag(featureFlagName));

        checkFeatureFlag(featureFlag, featureFlagName, true);
    }

    @Test
    public void shouldDisableExistingFeatureFlag() {

        final String featureFlagName = "myCoolFeature";

        final String featureFlagUri = getFeatureFlagUri(featureFlagName);
        mockUpdateFeatureFlagRequest(featureFlagUri);
        mockGetFeatureFlagRequest(featureFlagUri, false);

        final ProjectFeatureFlag updatedFeatureFlag = gd.getProjectService().updateFeatureFlag(
                new ProjectFeatureFlag(featureFlagName, false, new ProjectFeatureFlag.Links(featureFlagUri)));

        checkFeatureFlag(updatedFeatureFlag, featureFlagName, false);
    }

    @Test
    public void shouldDeleteExistingFeatureFlag() {

        final String featureFlagName = "myCoolFeature";

        final String featureFlagUri = getFeatureFlagUri(featureFlagName);
        onRequest()
                .havingMethodEqualTo("DELETE")
                .havingPathEqualTo(featureFlagUri)
                .respond()
                .withStatus(204);

        gd.getProjectService().deleteFeatureFlag(new ProjectFeatureFlag(featureFlagName, true,
                new ProjectFeatureFlag.Links(featureFlagUri)));
    }


    private String getFeatureFlagUri(String featureFlagName) {
        return FEATURE_FLAG_TEMPLATE.expand(PROJECT_ID, featureFlagName).toString();
    }

    private void mockListFeatureFlagsRequest() {
        onRequest()
                .havingMethodEqualTo("GET")
                .havingPathEqualTo(FEATURE_FLAGS_TEMPLATE.expand(PROJECT_ID).toString())
                .respond()
                .withBody(readStringFromResource("/project/feature-flags.json"))
                .withStatus(200);
    }

    private void mockCreateFeatureFlagRequest(String featureFlagUri) {
        onRequest()
                .havingMethodEqualTo("POST")
                .havingPathEqualTo(FEATURE_FLAGS_TEMPLATE.expand(PROJECT_ID).toString())
                .respond()
                .withHeader("Location", featureFlagUri)
                .withStatus(201);
    }

    private void mockGetFeatureFlagRequest(String featureFlagUri, boolean featureFlagValue) {
        final String jsonWithValue = readStringFromResource("/project/feature-flag.json")
                .replaceAll("\"value\"\\s*:\\s*(true|false)",
                        "\"value\" : " + featureFlagValue);
        onRequest()
                .havingMethodEqualTo("GET")
                .havingPathEqualTo(featureFlagUri)
                .respond()
                .withBody(jsonWithValue)
                .withStatus(200);
    }

    private void mockUpdateFeatureFlagRequest(String featureFlagUri) {
        onRequest()
                .havingMethodEqualTo("PUT")
                .havingPathEqualTo(featureFlagUri)
                .respond()
                .withStatus(200);
    }

    private void checkFeatureFlag(ProjectFeatureFlag featureFlag, String expectedName, boolean expectedValue) {
        assertThat(featureFlag, is(notNullValue()));
        assertThat(featureFlag.getName(), is(expectedName));
        assertThat(featureFlag.getEnabled(), is(expectedValue));
    }

}