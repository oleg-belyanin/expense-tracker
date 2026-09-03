package com.olegbelyanin.expensetracker

import com.olegbelyanin.expensetracker.domain.category.CategoryNameError
import com.olegbelyanin.expensetracker.model.CategoryAssignmentSource
import com.olegbelyanin.expensetracker.model.CategoryIcons
import com.olegbelyanin.expensetracker.model.Expense
import com.olegbelyanin.expensetracker.model.Money
import com.olegbelyanin.expensetracker.ui.categories.CategoryFormDraft
import com.olegbelyanin.expensetracker.ui.categories.usagesOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class CategoryFormDraftTest {
    @Test
    fun suggestsIconAndColorUntilUserPicksManually() {
        val next = CategoryFormDraft().withName("Питомцы", "pets")
        assertEquals("pets", next.icon)
        assertEquals("#A76CC1", next.color)
        val renamed = next.withName("Кофе", "cafe")
        assertEquals("cafe", renamed.icon)
        assertEquals("#E8894A", renamed.color)
        val locked = renamed.withIcon("work").withName("Питомцы", "pets")
        assertEquals("work", locked.icon)
    }

    @Test
    fun blocksEmptyAndDuplicateNames() {
        val empty = CategoryFormDraft().withName("   ", CategoryIcons.LETTER)
        assertEquals(CategoryNameError.EMPTY, empty.nameError(duplicate = false))
        assertFalse(empty.canSave(duplicate = false))
        val named = CategoryFormDraft().withName("Продукты", "groceries")
        assertNull(named.nameError(duplicate = false))
        assertEquals(CategoryNameError.DUPLICATE, named.nameError(duplicate = true))
        assertFalse(named.canSave(duplicate = true))
        assertTrue(named.canSave(duplicate = false))
    }

    @Test
    fun groupsExpenseTotalsByCategory() {
        val usages =
            usagesOf(
                listOf(
                    expense(categoryId = 1, minor = 10_000),
                    expense(categoryId = 1, minor = 428_000),
                    expense(categoryId = 2, minor = 100),
                ),
            )
        assertEquals(2, usages[1]?.count)
        assertEquals(438_000L, usages[1]?.totalMinor)
        assertEquals(1, usages[2]?.count)
    }
}

private fun expense(categoryId: Long, minor: Long) = Expense(
    id = "e-$categoryId-$minor",
    amount = Money(minor),
    spentAt = Instant.EPOCH,
    name = "Латте",
    normalizedName = "латте",
    categoryId = categoryId,
    locationId = null,
    comment = null,
    categoryAssignmentSource = CategoryAssignmentSource.FALLBACK,
    dedupKey = "e-$categoryId-$minor",
)
