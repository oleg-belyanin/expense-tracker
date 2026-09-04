package com.olegbelyanin.expensetracker

import com.olegbelyanin.expensetracker.categorization.CategoryIconSuggester
import com.olegbelyanin.expensetracker.categorization.TextNormalizer
import com.olegbelyanin.expensetracker.domain.CategoryRepository
import com.olegbelyanin.expensetracker.model.Category
import com.olegbelyanin.expensetracker.ui.categories.CategoryFormNotice
import com.olegbelyanin.expensetracker.ui.categories.CategoryFormViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CategoryFormViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setMainDispatcher() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun resetMainDispatcher() {
        Dispatchers.resetMain()
    }

    @Test
    fun saveFailureShowsHumanNoticeAndUnlocksButton() = runTest(dispatcher) {
        var saved = false
        val viewModel =
            CategoryFormViewModel(
                categoryId = null,
                categories = FakeCategories(),
                createCategory = { _, _, _ -> error("constraint SQLITE_CONSTRAINT") },
                updateCategory = { _, _, _, _ -> error("unused") },
                iconSuggester = CategoryIconSuggester(TextNormalizer()),
                normalizer = TextNormalizer(),
            )
        runCurrent()

        viewModel.onNameChange("Питомцы")
        viewModel.onSave { saved = true }
        runCurrent()

        assertFalse(saved)
        assertFalse(viewModel.state.value.saving)
        assertEquals(CategoryFormNotice.SaveFailed, viewModel.state.value.notice)
        assertTrue(viewModel.canSave())
    }

    private class FakeCategories : CategoryRepository {
        override suspend fun getActiveCategories() = emptyList<Category>()

        override fun observeActiveCategories() = MutableStateFlow(emptyList<Category>())

        override fun observeArchivedCategories() = MutableStateFlow(emptyList<Category>())

        override fun observeAll() = MutableStateFlow(emptyList<Category>())

        override suspend fun findById(id: Long) = null

        override suspend fun requireFallback() = error("unused")

        override suspend fun createUserCategory(name: String, color: String, icon: String) = error("unused")

        override suspend fun updateUserCategory(id: Long, name: String, color: String, icon: String) = error("unused")

        override suspend fun archive(id: Long) = error("unused")

        override suspend fun restore(id: Long) = error("unused")
    }
}
