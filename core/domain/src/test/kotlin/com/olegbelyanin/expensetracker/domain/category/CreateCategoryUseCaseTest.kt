package com.olegbelyanin.expensetracker.domain.category

import com.olegbelyanin.expensetracker.domain.CategoryRepository
import com.olegbelyanin.expensetracker.model.Category
import com.olegbelyanin.expensetracker.model.CategoryIcons
import com.olegbelyanin.expensetracker.model.CategoryPalette
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class CreateCategoryUseCaseTest {
    @Test
    fun rejectsBlankName() = runTest {
        val useCase = CreateCategoryUseCase(FakeCategories())
        val result = useCase("   ") as CreateCategoryResult.Invalid
        assertEquals(CategoryNameError.EMPTY, result.error)
    }

    @Test
    fun createsUserCategoryWithLetterFallback() = runTest {
        val repo = FakeCategories()
        val result = CreateCategoryUseCase(repo)("  Подписка  ") as CreateCategoryResult.Success
        assertEquals("Подписка", result.category.name)
        assertEquals(CategoryIcons.LETTER, result.category.icon)
        assertEquals(CategoryPalette.DEFAULT, result.category.color)
        assertEquals(1, repo.stored.size)
    }

    @Test
    fun suggestsIconFromNameWhenCallerOmitsIt() = runTest {
        val repo = FakeCategories()
        val useCase = CreateCategoryUseCase(repo) { name ->
            if (name.contains("Питомц", ignoreCase = true)) "pets" else CategoryIcons.LETTER
        }
        val result = useCase("Питомцы") as CreateCategoryResult.Success
        assertEquals("pets", result.category.icon)
    }

    @Test
    fun keepsExplicitIconAndColor() = runTest {
        val repo = FakeCategories()
        val result = CreateCategoryUseCase(repo)(
            rawName = "Питомцы",
            color = "#E6B84A",
            icon = "work",
        ) as CreateCategoryResult.Success
        assertEquals("work", result.category.icon)
        assertEquals("#E6B84A", result.category.color)
    }

    @Test
    fun rejectsDuplicateActiveName() = runTest {
        val repo = FakeCategories()
        CreateCategoryUseCase(repo)("Подписка")
        val duplicate = CreateCategoryUseCase(repo)("подписка") as CreateCategoryResult.Invalid
        assertEquals(CategoryNameError.DUPLICATE, duplicate.error)
    }

    @Test
    fun reactivatesArchivedCategoryWithSameName() = runTest {
        val archived = userCategory(id = 4, name = "Подписка", archived = true)
        val repo = FakeCategories(mutableListOf(archived))
        val result = CreateCategoryUseCase(repo)("Подписка") as CreateCategoryResult.Success
        assertEquals(4, result.category.id)
        assertTrue(result.category.isActive)
    }
}

class UpdateCategoryUseCaseTest {
    @Test
    fun updatesUserCategory() = runTest {
        val existing = userCategory(id = 8, name = "Подписка")
        val repo = FakeCategories(mutableListOf(existing))
        val result = UpdateCategoryUseCase(repo)(8, "Кино", color = "#A76CC1", icon = "fun")
            as UpdateCategoryResult.Success
        assertEquals("Кино", result.category.name)
        assertEquals("fun", result.category.icon)
        assertEquals("#A76CC1", result.category.color)
    }

    @Test
    fun rejectsBuiltinEdit() = runTest {
        val cafe = builtinCategory(id = 2, name = "Кафе", code = "CAFE")
        val repo = FakeCategories(mutableListOf(cafe))
        val result = UpdateCategoryUseCase(repo)(2, "Кофейня") as UpdateCategoryResult.Rejected
        assertEquals(CategoryMutationError.BUILTIN, result.error)
        assertEquals("Кафе", repo.stored.single().name)
    }

    @Test
    fun rejectsDuplicateNameOnRename() = runTest {
        val first = userCategory(id = 8, name = "Подписка")
        val second = userCategory(id = 9, name = "Кино")
        val repo = FakeCategories(mutableListOf(first, second))
        val result = UpdateCategoryUseCase(repo)(9, "подписка") as UpdateCategoryResult.InvalidName
        assertEquals(CategoryNameError.DUPLICATE, result.error)
    }
}

class ArchiveCategoryUseCaseTest {
    @Test
    fun archivesUserCategoryWithoutRemovingIt() = runTest {
        val existing = userCategory(id = 8, name = "Подписка")
        val repo = FakeCategories(mutableListOf(existing))
        val result = ArchiveCategoryUseCase(repo)(8) as ArchiveCategoryResult.Success
        assertTrue(!result.category.isActive)
        assertEquals(8, repo.stored.single().id)
        assertEquals("Подписка", repo.stored.single().name)
    }

    @Test
    fun blocksFallbackArchive() = runTest {
        val other = builtinCategory(id = 10, name = "Прочее", code = "OTHER")
        val repo = FakeCategories(mutableListOf(other))
        val result = ArchiveCategoryUseCase(repo)(10) as ArchiveCategoryResult.Rejected
        assertEquals(CategoryMutationError.FALLBACK_PROTECTED, result.error)
        assertTrue(repo.stored.single().isActive)
    }

    @Test
    fun allowsArchivingNonFallbackBuiltin() = runTest {
        val cafe = builtinCategory(id = 2, name = "Кафе", code = "CAFE")
        val repo = FakeCategories(mutableListOf(cafe))
        val result = ArchiveCategoryUseCase(repo)(2) as ArchiveCategoryResult.Success
        assertTrue(!result.category.isActive)
        assertEquals(2, result.category.id)
    }

    @Test
    fun restoreReturnsSameId() = runTest {
        val archived = userCategory(id = 8, name = "Подписка", archived = true)
        val repo = FakeCategories(mutableListOf(archived))
        val result = RestoreCategoryUseCase(repo)(8) as ArchiveCategoryResult.Success
        assertEquals(8, result.category.id)
        assertTrue(result.category.isActive)
    }
}

private fun userCategory(id: Long, name: String, archived: Boolean = false) = Category(
    id = id,
    code = null,
    name = name,
    normalizedName = name.trim().lowercase(),
    color = "#111111",
    icon = "letter",
    isBuiltin = false,
    archivedAt = if (archived) Instant.EPOCH else null,
)

private fun builtinCategory(id: Long, name: String, code: String) = Category(
    id = id,
    code = code,
    name = name,
    normalizedName = name.trim().lowercase(),
    color = "#222222",
    icon = "other",
    isBuiltin = true,
    archivedAt = null,
)

internal class FakeCategories(val stored: MutableList<Category> = mutableListOf()) : CategoryRepository {
    override suspend fun getActiveCategories(): List<Category> = stored.filter { it.isActive }

    override fun observeActiveCategories(): Flow<List<Category>> = MutableStateFlow(stored.filter { it.isActive })

    override fun observeArchivedCategories(): Flow<List<Category>> = MutableStateFlow(stored.filter { !it.isActive })

    override fun observeAll(): Flow<List<Category>> = MutableStateFlow(stored)

    override suspend fun findById(id: Long): Category? = stored.find { it.id == id }

    override suspend fun requireFallback(): Category = stored.first { it.isFallback }

    override suspend fun createUserCategory(name: String, color: String, icon: String): Category {
        val normalized = name.trim().lowercase()
        val existing = stored.find { it.normalizedName == normalized }
        if (existing != null) {
            if (existing.isActive) throw DuplicateCategoryNameException(name)
            val restored = if (existing.isBuiltin) {
                existing.copy(archivedAt = null)
            } else {
                existing.copy(archivedAt = null, name = name.trim(), color = color, icon = icon)
            }
            stored[stored.indexOf(existing)] = restored
            return restored
        }
        val created = Category(
            id = (stored.maxOfOrNull { it.id } ?: 0) + 1,
            code = null,
            name = name.trim(),
            normalizedName = normalized,
            color = color,
            icon = icon,
            isBuiltin = false,
            archivedAt = null,
        )
        stored += created
        return created
    }

    override suspend fun updateUserCategory(id: Long, name: String, color: String, icon: String): Category {
        val current = stored.find { it.id == id } ?: throw CategoryNotFoundException(id)
        if (current.isBuiltin) throw BuiltinCategoryLockedException(id)
        val normalized = name.trim().lowercase()
        val clash = stored.find { it.normalizedName == normalized && it.id != id }
        if (clash != null) throw DuplicateCategoryNameException(name)
        val updated = current.copy(name = name.trim(), normalizedName = normalized, color = color, icon = icon)
        stored[stored.indexOf(current)] = updated
        return updated
    }

    override suspend fun archive(id: Long): Category {
        val current = stored.find { it.id == id } ?: throw CategoryNotFoundException(id)
        if (current.isFallback) throw FallbackCategoryProtectedException()
        if (!current.isActive) return current
        val archived = current.copy(archivedAt = Instant.EPOCH)
        stored[stored.indexOf(current)] = archived
        return archived
    }

    override suspend fun restore(id: Long): Category {
        val current = stored.find { it.id == id } ?: throw CategoryNotFoundException(id)
        val restored = current.copy(archivedAt = null)
        stored[stored.indexOf(current)] = restored
        return restored
    }
}
