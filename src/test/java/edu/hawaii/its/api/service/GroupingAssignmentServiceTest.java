package edu.hawaii.its.api.service;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import edu.hawaii.its.api.configuration.SpringBootWebApplication;
import edu.hawaii.its.api.repository.GroupRepository;
import edu.hawaii.its.api.repository.GroupingRepository;
import edu.hawaii.its.api.repository.MembershipRepository;
import edu.hawaii.its.api.repository.PersonRepository;
import edu.hawaii.its.api.type.AdminListsHolder;
import edu.hawaii.its.api.type.Group;
import edu.hawaii.its.api.type.Grouping;
import edu.hawaii.its.api.type.Person;

import edu.internet2.middleware.grouperClient.ws.beans.WsGetMembersResult;
import edu.internet2.middleware.grouperClient.ws.beans.WsGetMembersResults;
import edu.internet2.middleware.grouperClient.ws.beans.WsGroup;
import edu.internet2.middleware.grouperClient.ws.beans.WsSubject;
import edu.internet2.middleware.grouperClient.ws.beans.WsSubjectLookup;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

@ActiveProfiles("localTest")
@RunWith(SpringRunner.class)
@SpringBootTest(classes = { SpringBootWebApplication.class })
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class GroupingAssignmentServiceTest {

    @Value("${groupings.api.grouping_admins}")
    private String GROUPING_ADMINS;

    @Value("${groupings.api.grouping_apps}")
    private String GROUPING_APPS;

    @Value("${groupings.api.test.username}")
    private String USERNAME;

    @Value("${groupings.api.test.name}")
    private String NAME;

    @Value("${groupings.api.test.uhuuid}")
    private String UHUUID;

    @Value("${groupings.api.person_attributes.uhuuid}")
    private String UHUUID_KEY;

    @Value("${groupings.api.person_attributes.username}")
    private String UID_KEY;

    @Value("${groupings.api.person_attributes.first_name}")
    private String FIRST_NAME_KEY;

    @Value("${groupings.api.person_attributes.last_name}")
    private String LAST_NAME_KEY;

    @Value("${groupings.api.person_attributes.composite_name}")
    private String COMPOSITE_NAME_KEY;

    @Value("${groupings.api.insufficient_privileges}")
    private String INSUFFICIENT_PRIVILEGES;

    @Value("${groupings.api.stale_subject_id}")
    private String STALE_SUBJECT_ID;

    private static final String PATH_ROOT = "path:to:grouping";

    private static final String BASIS = ":basis";

    private static final String GROUPING_0_PATH = PATH_ROOT + 0;
    private static final String GROUPING_1_PATH = PATH_ROOT + 1;
    private static final String GROUPING_3_PATH = PATH_ROOT + 3;
    private static final String GROUPING_3_BASIS_PATH = GROUPING_3_PATH + BASIS;

    private static final String ADMIN_USER = "admin";
    private static final Person ADMIN_PERSON = new Person(ADMIN_USER, ADMIN_USER, ADMIN_USER);
    private List<Person> admins = new ArrayList<>();
    private Group adminGroup = new Group();

    private static final String APP_USER = "app";
    private static final Person APP_PERSON = new Person(APP_USER, APP_USER, APP_USER);
    private List<Person> apps = new ArrayList<>();
    private Group appGroup = new Group();

    private List<Person> users = new ArrayList<>();
    private List<WsSubjectLookup> lookups = new ArrayList<>();

    @Autowired
    private GroupingRepository groupingRepository;

    @Autowired
    private GroupRepository groupRepository;

    @Autowired
    private PersonRepository personRepository;

    @Autowired
    private MembershipRepository membershipRepository;

    @Autowired
    private GroupingAssignmentService groupingAssignmentService;

    @Autowired
    private MembershipService membershipService;

    @Autowired
    private GrouperFactoryService grouperFS;

    @Autowired
    private DatabaseSetupService databaseSetupService;

    @Before
    public void setup() {
        databaseSetupService.initialize(users, lookups, admins, adminGroup, appGroup);

    }

    @Test
    public void construction() {
        //autowired
        assertNotNull(groupingAssignmentService);
    }

    @Test
    public void getGroupingTest() {
        try {
            groupingAssignmentService.getGrouping(GROUPING_0_PATH, users.get(1).getUsername());
        } catch (AccessDeniedException ade) {
            assertThat(INSUFFICIENT_PRIVILEGES, is(ade.getMessage()));
        }
        Grouping groupingOwner = groupingAssignmentService.getGrouping(GROUPING_0_PATH, users.get(0).getUsername());
        Grouping groupingAdmin = groupingAssignmentService.getGrouping(GROUPING_0_PATH, ADMIN_USER);

        assertTrue(groupingOwner.getComposite().getNames().contains(users.get(0).getName()));
        assertTrue(groupingOwner.getComposite().getUsernames().contains(users.get(0).getUsername()));
        assertTrue(groupingOwner.getComposite().getUhUuids().contains(users.get(0).getUhUuid()));
        assertTrue(groupingOwner.getInclude().getNames().contains(users.get(5).getName()));
        assertTrue(groupingOwner.getExclude().getNames().contains(users.get(2).getName()));
        assertTrue(groupingOwner.getBasis().getNames().contains(users.get(4).getName()));
        assertTrue(groupingOwner.getOwners().getNames().contains(users.get(0).getName()));

        assertTrue(groupingAdmin.getComposite().getNames().contains(users.get(0).getName()));
        assertTrue(groupingAdmin.getComposite().getUsernames().contains(users.get(0).getUsername()));
        assertTrue(groupingAdmin.getComposite().getUhUuids().contains(users.get(0).getUhUuid()));
        assertTrue(groupingAdmin.getInclude().getNames().contains(users.get(5).getName()));
        assertTrue(groupingAdmin.getExclude().getNames().contains(users.get(2).getName()));
        assertTrue(groupingAdmin.getBasis().getNames().contains(users.get(4).getName()));
        assertTrue(groupingAdmin.getOwners().getNames().contains(users.get(0).getName()));
    }

    @Test
    public void getPaginatedGroupingTest() {

        try {
            Grouping groupingRandom = groupingAssignmentService
                    .getPaginatedGrouping(GROUPING_0_PATH, users.get(1).getUsername(), 1, 4, "name", true);
            fail("Shouldn't be here.");
        } catch (AccessDeniedException ade) {
            assertThat(ade.getMessage(), equalTo(INSUFFICIENT_PRIVILEGES));
        }

        Grouping groupingOwner = groupingAssignmentService
                .getPaginatedGrouping(GROUPING_0_PATH, users.get(0).getUsername(), 1, 4, "name", true);
        Grouping groupingAdmin = groupingAssignmentService
                .getPaginatedGrouping(GROUPING_0_PATH, ADMIN_USER, 1, 4, "name", false);
        Grouping groupingNull = groupingAssignmentService
                .getPaginatedGrouping(GROUPING_0_PATH, users.get(0).getUsername(), null, null, null, null);

        //        assertThat(groupingRandom.getComposite().getMembers().size(), equalTo(0));
        //        assertThat(groupingRandom.getBasis().getMembers().size(), equalTo(0));
        //        assertThat(groupingRandom.getInclude().getMembers().size(), equalTo(0));
        //        assertThat(groupingRandom.getExclude().getMembers().size(), equalTo(0));
        //        assertThat(groupingRandom.getOwners().getMembers().size(), equalTo(0));

        assertTrue(groupingOwner.getComposite().getNames().contains(users.get(0).getName()));
        assertTrue(groupingOwner.getComposite().getUsernames().contains(users.get(0).getUsername()));
        assertTrue(groupingOwner.getComposite().getUhUuids().contains(users.get(0).getUhUuid()));
        assertFalse(groupingOwner.getComposite().getNames().contains(users.get(7).getName()));
        assertFalse(groupingOwner.getComposite().getUsernames().contains(users.get(7).getUsername()));
        assertFalse(groupingOwner.getComposite().getUhUuids().contains(users.get(7).getUhUuid()));

        assertTrue(groupingAdmin.getComposite().getNames().contains(users.get(7).getName()));
        assertTrue(groupingAdmin.getComposite().getUsernames().contains(users.get(7).getUsername()));
        assertTrue(groupingAdmin.getComposite().getUhUuids().contains(users.get(7).getUhUuid()));
        assertFalse(groupingAdmin.getComposite().getNames().contains(users.get(0).getName()));
        assertFalse(groupingAdmin.getComposite().getUsernames().contains(users.get(0).getUsername()));
        assertFalse(groupingAdmin.getComposite().getUhUuids().contains(users.get(0).getUhUuid()));

        assertTrue(groupingNull.getComposite().getNames().contains(users.get(0).getName()));
        assertTrue(groupingNull.getComposite().getUsernames().contains(users.get(0).getUsername()));
        assertTrue(groupingNull.getComposite().getUhUuids().contains(users.get(0).getUhUuid()));
        assertTrue(groupingNull.getComposite().getNames().contains(users.get(7).getName()));
        assertTrue(groupingNull.getComposite().getUsernames().contains(users.get(7).getUsername()));
        assertTrue(groupingNull.getComposite().getUhUuids().contains(users.get(7).getUhUuid()));
    }

    @Test
    public void groupingsInTest() {

        Iterable<Group> groupsIn = groupRepository.findByMembersUsername(users.get(6).getUsername());
        List<String> groupPaths = new ArrayList<>();
        List<String> supposedGroupings = new ArrayList<>();

        for (Group group : groupsIn) {
            groupPaths.add(group.getPath());
        }
        supposedGroupings
                .addAll(groupPaths.stream().filter(groupPath -> groupPath.matches("[a-zA-Z0-9:]*grouping[0-9]*"))
                        .collect(Collectors.toList()));

        List<Grouping> groupingsIn = groupingAssignmentService.groupingsIn(groupPaths);
        List<String> groupingPaths = groupingsIn.stream().map(Grouping::getPath).collect(Collectors.toList());

        for (String path : supposedGroupings) {
            assertTrue(groupingPaths.contains(path));
        }
        for (Grouping grouping : groupingsIn) {
            assertTrue(supposedGroupings.contains(grouping.getPath()));
        }
    }

    @Test
    public void getOptInGroupsTest() {
        List<List<String>> optInPathsLists = new ArrayList<List<String>>();
        for (int i = 0; i < 6; i++) {
            optInPathsLists.add(new ArrayList<>(
                    groupingAssignmentService.getOptInGroups(users.get(0).getUsername(), users.get(i).getUsername())));
        }
        List<String> assumedPaths = Arrays.asList(GROUPING_0_PATH, GROUPING_1_PATH, GROUPING_3_PATH);

        for (List<String> list : optInPathsLists) {
            for (String path : list) {
                assertTrue(assumedPaths.contains(path));
            }
        }
        List<String> optInPaths =
                groupingAssignmentService.getOptInGroups(users.get(0).getUsername(), users.get(1).getUsername());
        assertNotNull(optInPaths);
        Set<String> pathMap = new HashSet<>();
        for (String path : optInPaths) {
            // Check for duplicate paths.
            assertTrue(pathMap.add(path));
        }
    }

    @Test
    public void getOptOutGroupsTest() {
        List<String> optOutPaths =
                groupingAssignmentService.getOptOutGroups(users.get(0).getUsername(), users.get(1).getUsername());
        assertNotNull(optOutPaths);
        Set<String> pathMap = new HashSet<>();
        for (String path : optOutPaths) {
            // Check for duplicate paths.
            assertTrue(pathMap.add(path));
        }
    }

    @Test
    public void groupingsOwnedTest() {
        Iterable<Group> groupsIn = groupRepository.findByMembersUsername(users.get(0).getUsername());
        List<String> groupPaths = new ArrayList<>();

        for (Group group : groupsIn) {
            groupPaths.add(group.getPath());
        }

        List<Grouping> groupingsOwned = groupingAssignmentService.groupingsOwned(groupPaths);

        for (int i = 0; i < groupingsOwned.size(); i++) {
            assertTrue(groupingsOwned.get(i).getPath().equals(PATH_ROOT + i));
        }
    }

    @Test
    public void adminListsTest() {
        AdminListsHolder adminListsHolder = groupingAssignmentService.adminLists(ADMIN_USER);

        assertThat(5, is(adminListsHolder.getAllGroupingPaths().size()));
        assertThat(1, is(adminListsHolder.getAdminGroup().getMembers().size()));

        try {
            groupingAssignmentService.adminLists(users.get(1).getUsername());
        } catch (AccessDeniedException ade) {
            assertThat(INSUFFICIENT_PRIVILEGES, is(ade.getMessage()));
        }
    }

    /////////////////////////////////////////////////////
    // non-mocked tests//////////////////////////////////
    /////////////////////////////////////////////////////

    @Test
    public void extractGroupPaths() {
        List<String> groupNames = groupingAssignmentService.extractGroupPaths(null);
        assertThat(groupNames.size(), is(0));

        List<WsGroup> groups = new ArrayList<>();
        final int size = 300;

        for (int i = 0; i < size; i++) {
            WsGroup w = new WsGroup();
            w.setName("testName_" + i);
            groups.add(w);
        }
        assertThat(groups.size(), is(size));

        groupNames = groupingAssignmentService.extractGroupPaths(groups);
        for (int i = 0; i < size; i++) {
            assertTrue(groupNames.contains("testName_" + i));
        }
        assertThat(groupNames.size(), is(size));

        // Create some duplicates.
        groups = new ArrayList<>();
        for (int j = 0; j < 3; j++) {
            for (int i = 0; i < size; i++) {
                WsGroup w = new WsGroup();
                w.setName("testName_" + i);
                groups.add(w);
            }
        }
        assertThat(groups.size(), is(size * 3));

        // Duplicates should not be in groupNames list.
        groupNames = groupingAssignmentService.extractGroupPaths(groups);
        assertThat(groupNames.size(), is(size));
        for (int i = 0; i < size; i++) {
            assertTrue(groupNames.contains("testName_" + i));
        }
    }

    @Test
    public void makeGroupsTest() {

        WsGetMembersResults getMembersResults = new WsGetMembersResults();
        String[] attributeNames =
                new String[] { UID_KEY, UHUUID_KEY, LAST_NAME_KEY, COMPOSITE_NAME_KEY, FIRST_NAME_KEY };

        // We create an array here because getMembersResults.setResults() only takes an array
        WsGetMembersResult[] getMembersResult = new WsGetMembersResult[1];
        WsGetMembersResult subGetMembersResult = new WsGetMembersResult();

        WsGroup wsGroup = new WsGroup();
        wsGroup.setName(GROUPING_0_PATH);

        WsSubject[] list = new WsSubject[3];
        for (int i = 0; i < 3; i++) {
            list[i] = new WsSubject();
            list[i].setName("testSubject_" + i);
            list[i].setId("testSubject_uuid_" + i);
            // Attribute values need to match names in order (uuid is set seperately, so it can be blank here
            list[i].setAttributeValues(new String[] { "testSubject_username_" + i, "", "", "testSubject_" + i, "" });
        }

        subGetMembersResult.setWsSubjects(list);
        subGetMembersResult.setWsGroup(wsGroup);
        getMembersResult[0] = subGetMembersResult;

        getMembersResults.setResults(getMembersResult);
        getMembersResults.setSubjectAttributeNames(attributeNames);

        Map<String, Group> groups = groupingAssignmentService.makeGroups(getMembersResults);

        assertThat(groups.size(), not(equalTo(0)));
        Group resultGroup = groups.get(GROUPING_0_PATH);

        for (int i = 0; i < resultGroup.getMembers().size(); i++) {
            assertTrue(resultGroup.getMembers().get(i).getName().equals("testSubject_" + i));
            assertTrue(resultGroup.getNames().contains("testSubject_" + i));
            assertTrue(resultGroup.getMembers().get(i).getUhUuid().equals("testSubject_uuid_" + i));
            assertTrue(resultGroup.getUhUuids().contains("testSubject_uuid_" + i));
            assertTrue(resultGroup.getMembers().get(i).getUsername().equals("testSubject_username_" + i));
            assertTrue(resultGroup.getUsernames().contains("testSubject_username_" + i));
        }
    }

    @Test
    public void makePersonTest() {
        String name = "name";
        String id = "uuid";
        String identifier = "username";
        String[] attributeNames =
                new String[] { UID_KEY, UHUUID_KEY, LAST_NAME_KEY, COMPOSITE_NAME_KEY, FIRST_NAME_KEY };
        String[] attributeValues = new String[] { identifier, id, null, name, null };

        WsSubject subject = new WsSubject();
        subject.setName(name);
        subject.setId(id);
        subject.setAttributeValues(attributeValues);

        Person person = groupingAssignmentService.makePerson(subject, attributeNames);

        assertTrue(person.getName().equals(name));
        assertTrue(person.getUhUuid().equals(id));
        assertTrue(person.getUsername().equals(identifier));

        assertNotNull(groupingAssignmentService.makePerson(new WsSubject(), new String[] {}));
    }

    //todo Finish this test for setGroupingAttributes
    @Test
    public void setGroupingAttributesTest() {
        Grouping grouping = new Grouping();
        grouping = groupingAssignmentService.getGrouping(GROUPING_3_PATH, users.get(0).getUsername());
    }

    @Test
    public void makeGroupsNullValuesTest() {

        WsGetMembersResults getMembersResults = new WsGetMembersResults();
        String[] attributeNames =
                new String[] { UID_KEY, UHUUID_KEY, LAST_NAME_KEY, COMPOSITE_NAME_KEY, FIRST_NAME_KEY };

        // We create an array here because getMembersResults.setResults() only takes an array
        // nullSubject... will have a null Subject Array while testSubject... will have 2 subjects with test attributes
        WsGetMembersResult[] getMembersResult = new WsGetMembersResult[2];
        WsGetMembersResult nullSubjectArrayGetMembersResult = new WsGetMembersResult();
        WsGetMembersResult testSubjectsGetMembersResult = new WsGetMembersResult();

        WsGroup wsGroup0 = new WsGroup();
        wsGroup0.setName(GROUPING_0_PATH);
        nullSubjectArrayGetMembersResult.setWsGroup(wsGroup0);

        WsGroup wsGroup1 = new WsGroup();
        wsGroup1.setName(GROUPING_3_BASIS_PATH);
        testSubjectsGetMembersResult.setWsGroup(wsGroup1);

        // Set up subjects: one null, one with STALE_SUBJECT_ID
        WsSubject[] list = new WsSubject[2];
        list[1] = new WsSubject();
        list[1].setName("iDontExistAnymoreName");
        list[1].setId("iDontExistAnymoreUHUUID");
        list[1].setAttributeValues(new String[] { "iDontExistAnymoreUsername", "", "", "iDontExistAnymoreName", "" });
        list[1].setSourceId(STALE_SUBJECT_ID);

        // Push to array and set results
        nullSubjectArrayGetMembersResult.setWsSubjects(null);
        testSubjectsGetMembersResult.setWsSubjects(list);
        getMembersResult[0] = nullSubjectArrayGetMembersResult;
        getMembersResult[1] = testSubjectsGetMembersResult;
        getMembersResults.setResults(getMembersResult);
        getMembersResults.setSubjectAttributeNames(attributeNames);

        Map<String, Group> groups = groupingAssignmentService.makeGroups(getMembersResults);

        // Only one result should've created a group (the non-null subject array one)
        // The second group should have only one member because it had a null subject
        assertThat(groups.size(), equalTo(1));
        assertThat(groups.get(GROUPING_3_BASIS_PATH).getMembers().size(), equalTo(1));

        // Second group
        // Check if stale subject id produces the right username restructure
        List<Person> group1Members = groups.get(GROUPING_3_BASIS_PATH).getMembers();
        assertThat(group1Members.get(0).getUsername(), equalTo("User Not Available."));
    }
}