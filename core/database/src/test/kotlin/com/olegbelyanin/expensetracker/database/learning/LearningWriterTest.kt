package com.olegbelyanin.expensetracker.database.learning

import com.olegbelyanin.expensetracker.categorization.CategoryVector
import com.olegbelyanin.expensetracker.categorization.TextNormalizer
import com.olegbelyanin.expensetracker.database.dao.KeywordDao
import com.olegbelyanin.expensetracker.database.dao.LearningDao
import com.olegbelyanin.expensetracker.database.entities.CategoryTransitionEntity
import com.olegbelyanin.expensetracker.database.entities.CategoryTransitionKeywordEntity
import com.olegbelyanin.expensetracker.database.entities.ExactCategoryRuleEntity
import com.olegbelyanin.expensetracker.database.entities.KeywordCategoryStatEntity
import com.olegbelyanin.expensetracker.database.entities.NameCategoryContextEntity
import com.olegbelyanin.expensetracker.database.entities.NameCategoryContextKeywordEntity
import com.olegbelyanin.expensetracker.model.CategoryAssignmentSource
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LearningWriterTest {
    private val normalizer = TextNormalizer()
    private val fingerprint = LearningFingerprint("латт", categoryId = 2, locationId = 10)

    @Test
    fun acceptedAutosuggestionWritesExampleAndContextButNotExactRule() = runTest {
        val learning = FakeLearningDao()
        val keywords = FakeKeywordDao()
        applyAccepted(learning, keywords, CategoryAssignmentSource.PROBABILISTIC)

        val example = learning.findExampleByExpenseId("e1")!!
        assertEquals(LearningPlanner.AUTO_ACCEPTED, example.feedbackType)
        assertEquals("латт", example.normalizedName)
        assertEquals(2L, example.categoryId)
        assertEquals(2L, example.proposedCategoryId)
        assertEquals(10L, example.locationId)

        val context = learning.findNameContext("латт")!!
        assertEquals(2L, context.categoryId)
        assertEquals(LearningPlanner.AUTO_ACCEPTED, context.source)
        assertEquals(listOf(keywords.requireId("word", "латт")), learning.nameContextKeywordIds("латт"))
        assertEquals(listOf(keywords.requireId("word", "латт")), learning.exampleKeywordIds(example.id))

        val keywordStat = learning.findKeywordStat(
            keywords.requireId("word", "латт"),
            2,
            LearningPlanner.SOURCE_USER,
        )
        assertEquals(1, keywordStat?.observationCount)
        assertEquals(1, learning.findLocationStat(10, 2, LearningPlanner.SOURCE_USER)?.observationCount)
        assertNull(learning.findExactRule("латт"))
        assertTrue(learning.transitions.isEmpty())
    }

    @Test
    fun acceptedFallbackWritesExampleAndContextButNotExactRule() = runTest {
        val learning = FakeLearningDao()
        applyAccepted(learning, FakeKeywordDao(), CategoryAssignmentSource.FALLBACK)
        assertEquals(LearningPlanner.AUTO_ACCEPTED, learning.findExampleByExpenseId("e1")?.feedbackType)
        assertEquals(LearningPlanner.AUTO_ACCEPTED, learning.findNameContext("латт")?.source)
        assertNull(learning.findExactRule("латт"))
    }

    @Test
    fun acceptedAutosuggestionDoesNotReplaceExistingExactRule() = runTest {
        val learning = FakeLearningDao()
        learning.upsertExactRule(
            ExactCategoryRuleEntity(
                normalizedName = "латт",
                categoryId = 2,
                source = LearningPlanner.SOURCE_SEED,
                createdAt = 1,
                updatedAt = 1,
            ),
        )
        applyAccepted(learning, FakeKeywordDao(), CategoryAssignmentSource.PROBABILISTIC)
        val rule = learning.findExactRule("латт")!!
        assertEquals(LearningPlanner.SOURCE_SEED, rule.source)
        assertEquals(1L, rule.updatedAt)
        assertEquals(LearningPlanner.AUTO_ACCEPTED, learning.findNameContext("латт")?.source)
    }

    @Test
    fun acceptedAutosuggestionReplacesSeedNameContext() = runTest {
        val learning = FakeLearningDao()
        learning.upsertNameContext(
            NameCategoryContextEntity(
                normalizedName = "латт",
                categoryId = 9,
                source = LearningPlanner.SOURCE_SEED,
                updatedAt = 1,
            ),
        )
        applyAccepted(learning, FakeKeywordDao(), CategoryAssignmentSource.PROBABILISTIC)
        val context = learning.findNameContext("латт")!!
        assertEquals(2L, context.categoryId)
        assertEquals(LearningPlanner.AUTO_ACCEPTED, context.source)
        assertEquals(1_000L, context.updatedAt)
    }

    @Test
    fun explicitChoiceAlsoWritesExactRule() = runTest {
        val learning = FakeLearningDao()
        val keywords = FakeKeywordDao()
        val plan = LearningPlanner.plan(
            hadExpense = false,
            previous = null,
            next = fingerprint,
            source = CategoryAssignmentSource.EXPLICIT,
            interactive = true,
            proposedCategoryId = 2,
        )
        writer(learning, keywords).apply(
            expenseId = "e1",
            normalizedName = "латт",
            rawName = "Латте",
            categoryId = 2,
            locationId = 10,
            proposedCategoryId = 2,
            plan = plan,
            now = 1_000L,
        )
        assertEquals(LearningPlanner.EXPLICIT, learning.findExampleByExpenseId("e1")?.feedbackType)
        assertEquals(LearningPlanner.EXPLICIT, learning.findNameContext("латт")?.source)
        assertEquals(LearningPlanner.EXPLICIT, learning.findExactRule("латт")?.source)
    }

    @Test
    fun emptyNormalizedNameSkipsContextAndExactRule() = runTest {
        val learning = FakeLearningDao()
        val plan = LearningPlanner.plan(
            hadExpense = false,
            previous = null,
            next = fingerprint.copy(normalizedName = ""),
            source = CategoryAssignmentSource.PROBABILISTIC,
            interactive = true,
        )
        writer(learning, FakeKeywordDao()).apply(
            expenseId = "e1",
            normalizedName = "",
            rawName = "!!!",
            categoryId = 2,
            locationId = null,
            proposedCategoryId = 2,
            plan = plan,
            now = 1_000L,
        )
        assertEquals("", learning.findExampleByExpenseId("e1")?.normalizedName)
        assertNull(learning.findNameContext(""))
        assertNull(learning.findExactRule(""))
    }

    @Test
    fun importPlanWritesNeitherExampleNorContextNorRule() = runTest {
        val learning = FakeLearningDao()
        val plan = LearningPlanner.plan(
            hadExpense = false,
            previous = null,
            next = fingerprint,
            source = CategoryAssignmentSource.PROBABILISTIC,
            interactive = false,
        )
        writer(learning, FakeKeywordDao()).apply(
            expenseId = "e1",
            normalizedName = "латт",
            rawName = "Латте",
            categoryId = 2,
            locationId = 10,
            proposedCategoryId = 2,
            plan = plan,
            now = 1_000L,
        )
        assertNull(learning.findExampleByExpenseId("e1"))
        assertNull(learning.findNameContext("латт"))
        assertNull(learning.findExactRule("латт"))
    }

    @Test
    fun rememberedCountIgnoresAcceptedAndSeedRules() = runTest {
        val learning = FakeLearningDao()
        applyAccepted(learning, FakeKeywordDao(), CategoryAssignmentSource.PROBABILISTIC)
        assertEquals(0L, learning.observeUserExactRuleCount().first())
        learning.upsertExactRule(
            ExactCategoryRuleEntity("латт", 2, LearningPlanner.SOURCE_SEED, createdAt = 1, updatedAt = 1),
        )
        assertEquals(0L, learning.observeUserExactRuleCount().first())
    }

    @Test
    fun explicitSaveIncrementsRememberedRuleCount() = runTest {
        val learning = FakeLearningDao()
        writer(learning, FakeKeywordDao()).apply(
            expenseId = "e1",
            normalizedName = "латт",
            rawName = "Латте",
            categoryId = 2,
            locationId = 10,
            proposedCategoryId = 2,
            plan = LearningPlanner.plan(
                hadExpense = false,
                previous = null,
                next = fingerprint,
                source = CategoryAssignmentSource.EXPLICIT,
                interactive = true,
                proposedCategoryId = 2,
            ),
            now = 1_000L,
        )
        assertEquals(1L, learning.observeUserExactRuleCount().first())
    }

    @Test
    fun clearingHistoryDetachesExampleAndKeepsRules() = runTest {
        val learning = FakeLearningDao()
        val keywords = FakeKeywordDao()
        writer(learning, keywords).apply(
            expenseId = "e1",
            normalizedName = "латт",
            rawName = "Латте",
            categoryId = 2,
            locationId = 10,
            proposedCategoryId = 4,
            plan = LearningPlanner.plan(
                hadExpense = false,
                previous = null,
                next = fingerprint,
                source = CategoryAssignmentSource.EXPLICIT,
                interactive = true,
                proposedCategoryId = 4,
            ),
            now = 1_000L,
        )
        val example = learning.findExampleByExpenseId("e1")!!
        learning.detachExamplesFromExpenses()

        assertNull(learning.findExampleByExpenseId("e1"))
        val detached = learning.findExampleById(example.id)!!
        assertNull(detached.expenseId)
        assertEquals(2L, detached.categoryId)
        assertEquals(LearningPlanner.CORRECTION, detached.feedbackType)
        assertEquals(2L, learning.findNameContext("латт")?.categoryId)
        assertEquals(2L, learning.findExactRule("латт")?.categoryId)
        assertEquals(
            1,
            learning.findKeywordStat(
                keywords.requireId("word", "латт"),
                2,
                LearningPlanner.SOURCE_USER,
            )?.observationCount,
        )
    }

    @Test
    fun recalcDoesNotOverwriteExistingExample() = runTest {
        val learning = FakeLearningDao()
        applyAccepted(learning, FakeKeywordDao(), CategoryAssignmentSource.PROBABILISTIC)
        val before = learning.findExampleByExpenseId("e1")!!
        val plan = LearningPlanner.plan(
            hadExpense = true,
            previous = fingerprint,
            next = fingerprint.copy(categoryId = 4),
            source = CategoryAssignmentSource.PROBABILISTIC,
            interactive = false,
        )
        writer(learning, FakeKeywordDao()).apply(
            expenseId = "e1",
            normalizedName = "латт",
            rawName = "Латте",
            categoryId = 4,
            locationId = 10,
            proposedCategoryId = 4,
            plan = plan,
            now = 2_000L,
        )
        val after = learning.findExampleByExpenseId("e1")!!
        assertEquals(before.id, after.id)
        assertEquals(2L, after.categoryId)
        assertEquals(LearningPlanner.AUTO_ACCEPTED, after.feedbackType)
        assertEquals(2L, learning.findNameContext("латт")?.categoryId)
        assertNull(learning.findExactRule("латт"))
        assertTrue(learning.transitions.isEmpty())
    }

    @Test
    fun dentistFullyMovesButDoctorDoesNot() = runTest {
        val learning = FakeLearningDao()
        val keywords = FakeKeywordDao()
        val doctor = keywords.require("word", "врач")
        val dentist = keywords.require("word", "стоматолог")
        seedHealthContexts(learning, doctor, dentist)
        correctDoctorDentist(learning, keywords)

        val transition = learning.transitions.single()
        assertEquals(HEALTH, transition.fromCategoryId)
        assertEquals(STOMATOLOGY, transition.toCategoryId)
        assertNull(transition.closedAt)
        assertNotNull(learning.findActiveByKeyword(dentist))
        assertNull(learning.findActiveByKeyword(doctor))
        assertEquals(1L, learning.countActiveKeywords(transition.id))
    }

    @Test
    fun missingContextsDoNotActivateKeyword() = runTest {
        val learning = FakeLearningDao()
        val keywords = FakeKeywordDao()
        correctDoctorDentist(learning, keywords)
        val transition = learning.transitions.single()
        assertEquals(0L, learning.countActiveKeywords(transition.id))
        assertEquals(1_000L, transition.closedAt)
    }

    @Test
    fun newCorrectionReplacesPreviousActiveKeyword() = runTest {
        val learning = FakeLearningDao()
        val keywords = FakeKeywordDao()
        val dentist = keywords.require("word", "стоматолог")
        learning.upsertNameContext(
            NameCategoryContextEntity("стоматолог", HEALTH, LearningPlanner.SOURCE_SEED, 1),
        )
        learning.insertNameContextKeyword(NameCategoryContextKeywordEntity("стоматолог", dentist))
        learning.upsertKeywordStat(KeywordCategoryStatEntity(dentist, HEALTH, CategoryVector.SOURCE_SEED, 50))
        val oldId = "old-transit"
        learning.insertTransition(CategoryTransitionEntity(oldId, 9, HEALTH, createdAt = 1))
        learning.upsertTransitionKeyword(CategoryTransitionKeywordEntity(oldId, dentist, active = true))

        val plan = LearningPlanner.plan(
            hadExpense = false,
            previous = null,
            next = LearningFingerprint("стоматолог", STOMATOLOGY, null),
            source = CategoryAssignmentSource.EXPLICIT,
            interactive = true,
            proposedCategoryId = HEALTH,
        )
        writer(learning, keywords).apply(
            expenseId = "e2",
            normalizedName = "стоматолог",
            rawName = "стоматолог",
            categoryId = STOMATOLOGY,
            locationId = null,
            proposedCategoryId = HEALTH,
            plan = plan,
            now = 1_000L,
            activeCategoryIds = setOf(HEALTH, STOMATOLOGY, OTHER),
        )
        val replacement = learning.transitions.single { it.id != oldId }
        assertEquals(1_000L, learning.findTransition(oldId)?.closedAt)
        assertEquals(replacement.id, learning.findActiveByKeyword(dentist)?.transitionId)
        assertEquals(HEALTH, replacement.fromCategoryId)
        assertEquals(STOMATOLOGY, replacement.toCategoryId)
    }

    @Test
    fun baseProbabilityDeactivatesOneKeywordWithoutClosingSiblings() = runTest {
        val learning = FakeLearningDao()
        val keywords = FakeKeywordDao()
        val dentist = keywords.require("word", "стоматолог")
        val xray = keywords.require("word", "рентген")
        val transitionId = "t-siblings"
        learning.insertTransition(CategoryTransitionEntity(transitionId, HEALTH, STOMATOLOGY, createdAt = 1))
        learning.upsertTransitionKeyword(CategoryTransitionKeywordEntity(transitionId, dentist, active = true))
        learning.upsertTransitionKeyword(CategoryTransitionKeywordEntity(transitionId, xray, active = true))
        learning.upsertKeywordStat(
            KeywordCategoryStatEntity(dentist, HEALTH, CategoryVector.SOURCE_SEED, 1),
        )
        learning.upsertKeywordStat(
            KeywordCategoryStatEntity(dentist, STOMATOLOGY, LearningPlanner.SOURCE_USER, 20),
        )
        learning.upsertKeywordStat(
            KeywordCategoryStatEntity(xray, HEALTH, CategoryVector.SOURCE_SEED, 50),
        )

        val plan = LearningPlanner.plan(
            hadExpense = false,
            previous = null,
            next = LearningFingerprint("стоматолог", STOMATOLOGY, null),
            source = CategoryAssignmentSource.PROBABILISTIC,
            interactive = true,
            proposedCategoryId = STOMATOLOGY,
        )
        writer(learning, keywords).apply(
            expenseId = "e3",
            normalizedName = "стоматолог",
            rawName = "стоматолог",
            categoryId = STOMATOLOGY,
            locationId = null,
            proposedCategoryId = STOMATOLOGY,
            plan = plan,
            now = 2_000L,
            activeCategoryIds = setOf(HEALTH, STOMATOLOGY, OTHER),
        )
        assertNull(learning.findActiveByKeyword(dentist))
        assertEquals(transitionId, learning.findActiveByKeyword(xray)?.transitionId)
        assertNull(learning.findTransition(transitionId)?.closedAt)
    }

    @Test
    fun acceptedSuggestionDoesNotCreateTransition() = runTest {
        val learning = FakeLearningDao()
        applyAccepted(learning, FakeKeywordDao(), CategoryAssignmentSource.PROBABILISTIC)
        assertTrue(learning.transitions.isEmpty())
    }

    private suspend fun seedHealthContexts(learning: FakeLearningDao, doctor: Long, dentist: Long) {
        learning.upsertNameContext(NameCategoryContextEntity("врач", HEALTH, LearningPlanner.SOURCE_SEED, 1))
        learning.insertNameContextKeyword(NameCategoryContextKeywordEntity("врач", doctor))
        learning.upsertNameContext(
            NameCategoryContextEntity("врач стоматолог", HEALTH, LearningPlanner.SOURCE_SEED, 1),
        )
        learning.insertNameContextKeyword(NameCategoryContextKeywordEntity("врач стоматолог", doctor))
        learning.insertNameContextKeyword(NameCategoryContextKeywordEntity("врач стоматолог", dentist))
        learning.upsertKeywordStat(KeywordCategoryStatEntity(doctor, HEALTH, CategoryVector.SOURCE_SEED, 50))
        learning.upsertKeywordStat(KeywordCategoryStatEntity(dentist, HEALTH, CategoryVector.SOURCE_SEED, 50))
    }

    private suspend fun correctDoctorDentist(learning: FakeLearningDao, keywords: FakeKeywordDao) {
        val plan = LearningPlanner.plan(
            hadExpense = false,
            previous = null,
            next = LearningFingerprint("врач стоматолог", STOMATOLOGY, null),
            source = CategoryAssignmentSource.EXPLICIT,
            interactive = true,
            proposedCategoryId = HEALTH,
        )
        writer(learning, keywords).apply(
            expenseId = "e-dentist",
            normalizedName = "врач стоматолог",
            rawName = "врач стоматолог",
            categoryId = STOMATOLOGY,
            locationId = null,
            proposedCategoryId = HEALTH,
            plan = plan,
            now = 1_000L,
            activeCategoryIds = setOf(HEALTH, STOMATOLOGY, OTHER),
        )
    }

    private suspend fun applyAccepted(
        learning: FakeLearningDao,
        keywords: FakeKeywordDao,
        source: CategoryAssignmentSource,
    ) {
        val plan = LearningPlanner.plan(
            hadExpense = false,
            previous = null,
            next = fingerprint,
            source = source,
            interactive = true,
            proposedCategoryId = 2,
        )
        writer(learning, keywords).apply(
            expenseId = "e1",
            normalizedName = "латт",
            rawName = "Латте",
            categoryId = 2,
            locationId = 10,
            proposedCategoryId = 2,
            plan = plan,
            now = 1_000L,
        )
    }

    private fun writer(learning: LearningDao, keywords: KeywordDao) = LearningWriter(learning, keywords, normalizer)

    private companion object {
        const val HEALTH = 4L
        const val STOMATOLOGY = 8L
        const val OTHER = 10L
    }
}
