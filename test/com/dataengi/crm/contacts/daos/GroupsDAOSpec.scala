package com.dataengi.crm.contacts.daos

import com.dataengi.crm.common.context.CRMApplication
import org.joda.time.DateTime
import play.api.test.PlaySpecification
import com.dataengi.crm.common.extensions.awaits._
import com.dataengi.crm.common.context.types._
import com.dataengi.crm.contacts.daos.arbitraries.ContactsArbitrary
import com.dataengi.crm.contacts.daos.errors.{GroupsDAOError, GroupsDAOErrors}
import com.dataengi.crm.contacts.models.Group
import com.dataengi.crm.common.extensions.arbitrary._

import org.scalacheck.Gen
import org.specs2.runner.SpecificationsFinder

class GroupsDAOSpec extends PlaySpecification with CRMApplication with ContactsArbitrary {

  sequential

  lazy val groupsDAO: GroupsDAO = application.injector.instanceOf[GroupsDAO]
  lazy val TestGroup: Group     = Group("TestGroup" + Gen.alphaStr.sample.get, 0, new DateTime().getMillis)

  "GroupsDAO" should {

    "add group and get by id" in {
      val addGroupResult = groupsDAO.add(TestGroup).await()
      addGroupResult.isRight === true
      val id                 = addGroupResult.value
      val getGroupByIdResult = groupsDAO.get(id).await()
      getGroupByIdResult.isRight === true
      getGroupByIdResult.value === TestGroup.copy(id = Some(id))
      val getGroupByIdOptionResult = groupsDAO.getOption(id).await()
      getGroupByIdOptionResult.isRight === true
      getGroupByIdOptionResult.value === Some(TestGroup.copy(id = Some(id)))
    }

    "add and delete" in {
      val addGroupResult = groupsDAO.add(TestGroup).await()
      addGroupResult.isRight === true
      val id                 = addGroupResult.value
      val getGroupByIdResult = groupsDAO.get(id).await()
      getGroupByIdResult.isRight === true
      getGroupByIdResult.value === TestGroup.copy(id = Some(id))
      val deleteGroupResult = groupsDAO.delete(id).await()
      deleteGroupResult.isRight === true
    }

    "update group" in {
      val addGroupResult = groupsDAO.add(TestGroup).await()
      addGroupResult.isRight === true
      val id                = addGroupResult.value
      val updatedGroup      = TestGroup.copy(id = Some(id), createDate = new DateTime().getMillis, name = "UpdatedName")
      val updateGroupResult = groupsDAO.update(updatedGroup).await()
      updateGroupResult.isRight === true
      val getGroupByIdResult = groupsDAO.get(id).await()
      getGroupByIdResult.isRight === true
      getGroupByIdResult.value === updatedGroup
    }

    "Update group - negative" in {
      val updateGroupResult = groupsDAO.update(Group("new", 55777, 65, None)).await()
      updateGroupResult.isLeft === true
      updateGroupResult must beLeft(GroupsDAOErrors.GroupIdIsEmpty)
    }

    "list of companies" in {
      val getGroupByIdResult = groupsDAO.all.await()
      getGroupByIdResult.isRight === true
    }
    "find group by name" in {
      val findGroupByNameResult = groupsDAO.findByName(TestGroup.name).await()
      findGroupByNameResult.isRight === true
      findGroupByNameResult.value.isDefined === true
      val group = findGroupByNameResult.value.get
      group.copy(id = None) === TestGroup
    }

    "add and get groups" in {
      val groups          = groupArbitrary.arbitrary.list(10)
      val addGroupsResult = groupsDAO.add(groups).await()
      addGroupsResult.isRight === true
      val groupIds: List[Long] = addGroupsResult.value
      val getGroupsByIdsResult = groupsDAO.get(groupIds).await()
      getGroupsByIdsResult.isRight === true
      ignoreIds(groups) === ignoreIds(getGroupsByIdsResult.value)
    }

    "find groups by contacts book id" in {
      val testContactsBookId: Long = Gen.Choose.chooseLong.choose(0, Int.MaxValue).value
      val groups             = groupArbitrary.arbitrary.list(10).map(_.copy(contactsBookId = testContactsBookId))
      val addGroupsResult    = groupsDAO.add(groups).await()
      addGroupsResult.isRight === true
      val getGroupsByContactsBookIdResult = groupsDAO.findByContactsBookId(testContactsBookId).await()
      getGroupsByContactsBookIdResult.isRight === true
      ignoreIds(groups).toSet === ignoreIds(getGroupsByContactsBookIdResult.value).toSet
    }

  }

  def ignoreId(group: Group) = group.copy(id = None)

  def ignoreIds(group: List[Group]) = group.map(ignoreId)

}
