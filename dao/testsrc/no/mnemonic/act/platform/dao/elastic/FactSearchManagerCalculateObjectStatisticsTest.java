package no.mnemonic.act.platform.dao.elastic;

import no.mnemonic.act.platform.dao.api.ObjectStatisticsCriteria;
import no.mnemonic.act.platform.dao.api.ObjectStatisticsResult;
import no.mnemonic.act.platform.dao.elastic.document.FactDocument;
import no.mnemonic.act.platform.dao.elastic.document.ObjectDocument;
import org.junit.Test;

import java.util.UUID;

import static no.mnemonic.act.platform.dao.elastic.document.DocumentTestUtils.createObjectDocument;
import static no.mnemonic.commons.utilities.collections.SetUtils.set;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class FactSearchManagerCalculateObjectStatisticsTest extends AbstractManagerTest {

  @Test
  public void testCalculateObjectStatisticsWithNoCriteria() {
    assertNotNull(getFactSearchManager().calculateObjectStatistics(null));
  }

  @Test
  public void testCalculateObjectStatisticsAccessToOnlyPublicFact() {
    FactDocument accessibleFact = indexFact(d -> d.setAccessMode(FactDocument.AccessMode.Public));
    FactDocument nonAccessibleFact1 = indexFact(d -> d.setAccessMode(FactDocument.AccessMode.RoleBased));
    FactDocument nonAccessibleFact2 = indexFact(d -> d.setAccessMode(FactDocument.AccessMode.Explicit));

    ObjectStatisticsCriteria criteria = ObjectStatisticsCriteria.builder()
            .setCurrentUserID(UUID.randomUUID())
            .addAvailableOrganizationID(UUID.randomUUID())
            .addObjectID(getFirstObjectID(accessibleFact))
            .addObjectID(getFirstObjectID(nonAccessibleFact1))
            .addObjectID(getFirstObjectID(nonAccessibleFact2))
            .build();

    assertSingleStatisticExists(criteria, getFirstObjectID(accessibleFact));
  }

  @Test
  public void testCalculateObjectStatisticsAccessToRoleBasedFactViaOrganization() {
    FactDocument accessibleFact = indexFact(d -> d.setAccessMode(FactDocument.AccessMode.RoleBased));
    FactDocument nonAccessibleFact = indexFact(d -> d.setAccessMode(FactDocument.AccessMode.Explicit));

    ObjectStatisticsCriteria criteria = ObjectStatisticsCriteria.builder()
            .setCurrentUserID(UUID.randomUUID())
            .addAvailableOrganizationID(accessibleFact.getOrganizationID())
            .addObjectID(getFirstObjectID(accessibleFact))
            .addObjectID(getFirstObjectID(nonAccessibleFact))
            .build();

    assertSingleStatisticExists(criteria, getFirstObjectID(accessibleFact));
  }

  @Test
  public void testCalculateObjectStatisticsAccessToRoleBasedFactViaACL() {
    FactDocument accessibleFact = indexFact(d -> d.setAccessMode(FactDocument.AccessMode.RoleBased));
    FactDocument nonAccessibleFact = indexFact(d -> d.setAccessMode(FactDocument.AccessMode.Explicit));

    ObjectStatisticsCriteria criteria = ObjectStatisticsCriteria.builder()
            .setCurrentUserID(first(accessibleFact.getAcl()))
            .addAvailableOrganizationID(UUID.randomUUID())
            .addObjectID(getFirstObjectID(accessibleFact))
            .addObjectID(getFirstObjectID(nonAccessibleFact))
            .build();

    assertSingleStatisticExists(criteria, getFirstObjectID(accessibleFact));
  }

  @Test
  public void testCalculateObjectStatisticsAccessToExplicitFact() {
    FactDocument accessibleFact = indexFact(d -> d.setAccessMode(FactDocument.AccessMode.Explicit));
    FactDocument nonAccessibleFact = indexFact(d -> d.setAccessMode(FactDocument.AccessMode.RoleBased));

    ObjectStatisticsCriteria criteria = ObjectStatisticsCriteria.builder()
            .setCurrentUserID(first(accessibleFact.getAcl()))
            .addAvailableOrganizationID(UUID.randomUUID())
            .addObjectID(getFirstObjectID(accessibleFact))
            .addObjectID(getFirstObjectID(nonAccessibleFact))
            .build();

    assertSingleStatisticExists(criteria, getFirstObjectID(accessibleFact));
  }

  @Test
  public void testCalculateObjectStatisticsFilterByObjectIdFromDifferentFacts() {
    ObjectDocument accessibleObject = createObjectDocument();
    ObjectDocument inaccessibleObject = createObjectDocument();
    indexFact(d -> d.setObjects(set(accessibleObject)));
    indexFact(d -> d.setObjects(set(inaccessibleObject)));

    ObjectStatisticsCriteria criteria = createObjectStatisticsCriteria(b -> b.addObjectID(accessibleObject.getId()));
    assertSingleStatisticExists(criteria, accessibleObject.getId());
  }

  @Test
  public void testCalculateObjectStatisticsFilterByObjectIdFromSameFact() {
    ObjectDocument accessibleObject = createObjectDocument();
    ObjectDocument inaccessibleObject = createObjectDocument();
    indexFact(d -> d.setObjects(set(accessibleObject, inaccessibleObject)));

    ObjectStatisticsCriteria criteria = createObjectStatisticsCriteria(b -> b.addObjectID(accessibleObject.getId()));
    assertSingleStatisticExists(criteria, accessibleObject.getId());
  }

  @Test
  public void testCalculateObjectStatisticsForMultipleObjectsFromDifferentFacts() {
    ObjectDocument object1 = createObjectDocument();
    ObjectDocument object2 = createObjectDocument();
    indexFact(d -> d.setObjects(set(object1)));
    indexFact(d -> d.setObjects(set(object2)));

    ObjectStatisticsResult result = executeCalculateObjectStatistics(object1.getId(), object2.getId());
    assertEquals(2, result.getStatisticsCount());
  }

  @Test
  public void testCalculateObjectStatisticsForMultipleObjectsFromSameFact() {
    ObjectDocument object1 = createObjectDocument();
    ObjectDocument object2 = createObjectDocument();
    indexFact(d -> d.setObjects(set(object1, object2)));

    ObjectStatisticsResult result = executeCalculateObjectStatistics(object1.getId(), object2.getId());
    assertEquals(2, result.getStatisticsCount());
  }

  @Test
  public void testCalculateObjectStatisticsForObjectWithSingleFact() {
    ObjectDocument object = createObjectDocument();
    FactDocument fact = indexFact(d -> d.setObjects(set(object)));

    ObjectStatisticsResult result = executeCalculateObjectStatistics(object.getId());
    ObjectStatisticsResult.FactStatistic statistic = getFirstStatistic(result, object.getId());
    assertEquals(1, statistic.getFactCount());
    assertEquals(fact.getTypeID(), statistic.getFactTypeID());
    assertEquals(fact.getTimestamp(), statistic.getLastAddedTimestamp());
    assertEquals(fact.getLastSeenTimestamp(), statistic.getLastSeenTimestamp());
  }

  @Test
  public void testCalculateObjectStatisticsForObjectWithMultipleFactsOfSameType() {
    UUID typeID = UUID.randomUUID();
    ObjectDocument object = createObjectDocument();
    indexFact(d -> d.setTypeID(typeID)
            .setTimestamp(22222)
            .setLastSeenTimestamp(33333)
            .setObjects(set(object))
    );
    indexFact(d -> d.setTypeID(typeID)
            .setTimestamp(11111)
            .setLastSeenTimestamp(44444)
            .setObjects(set(object))
    );

    ObjectStatisticsResult result = executeCalculateObjectStatistics(object.getId());
    ObjectStatisticsResult.FactStatistic statistic = getFirstStatistic(result, object.getId());
    assertEquals(typeID, statistic.getFactTypeID());
    assertEquals(2, statistic.getFactCount());
    assertEquals(22222, statistic.getLastAddedTimestamp());
    assertEquals(44444, statistic.getLastSeenTimestamp());
  }

  @Test
  public void testCalculateObjectStatisticsForObjectWithMultipleFactsOfDifferentTypes() {
    ObjectDocument object = createObjectDocument();
    indexFact(d -> d.setTypeID(UUID.randomUUID()).setObjects(set(object)));
    indexFact(d -> d.setTypeID(UUID.randomUUID()).setObjects(set(object)));

    ObjectStatisticsResult result = executeCalculateObjectStatistics(object.getId());
    assertEquals(1, result.getStatisticsCount());
    assertEquals(2, result.getStatistics(object.getId()).size());
  }

  private void assertSingleStatisticExists(ObjectStatisticsCriteria criteria, UUID objectID) {
    ObjectStatisticsResult result = getFactSearchManager().calculateObjectStatistics(criteria);
    assertEquals(1, result.getStatisticsCount());
    assertNotNull(result.getStatistics(objectID));
  }

  private ObjectStatisticsResult executeCalculateObjectStatistics(UUID... objectID) {
    ObjectStatisticsCriteria criteria = createObjectStatisticsCriteria(b -> b.setObjectID(set(objectID)));
    return getFactSearchManager().calculateObjectStatistics(criteria);
  }

  private ObjectStatisticsCriteria createObjectStatisticsCriteria(ObjectPreparation<ObjectStatisticsCriteria.Builder> preparation) {
    ObjectStatisticsCriteria.Builder builder = ObjectStatisticsCriteria.builder()
            .setCurrentUserID(UUID.randomUUID())
            .addAvailableOrganizationID(UUID.randomUUID());
    if (preparation != null) {
      builder = preparation.prepare(builder);
    }
    return builder.build();
  }

  private UUID getFirstObjectID(FactDocument fact) {
    return first(fact.getObjects()).getId();
  }

  private ObjectStatisticsResult.FactStatistic getFirstStatistic(ObjectStatisticsResult result, UUID objectID) {
    return first(result.getStatistics(objectID));
  }

}
