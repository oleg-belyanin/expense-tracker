package com.olegbelyanin.expensetracker.ui.categories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.olegbelyanin.expensetracker.domain.CategoryRepository
import com.olegbelyanin.expensetracker.domain.ExpenseRepository
import com.olegbelyanin.expensetracker.domain.category.ArchiveCategoryUseCase
import com.olegbelyanin.expensetracker.domain.category.RestoreCategoryUseCase
import com.olegbelyanin.expensetracker.model.Category
import com.olegbelyanin.expensetracker.model.Expense
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId

enum class CategoriesPage {
    List,
    Archive,
}

sealed interface CategoriesDialog {
    data object None : CategoriesDialog

    data class Actions(val category: Category) : CategoriesDialog

    data class ArchiveConfirm(val category: Category) : CategoriesDialog
}

data class CategoryUsage(val count: Int = 0, val totalMinor: Long = 0)

enum class CategoriesNotice {
    ArchiveFailed,
    RestoreFailed,
}

data class CategoriesUiState(
    val active: List<Category> = emptyList(),
    val archived: List<Category> = emptyList(),
    val usages: Map<Long, CategoryUsage> = emptyMap(),
    val page: CategoriesPage = CategoriesPage.List,
    val dialog: CategoriesDialog = CategoriesDialog.None,
)

class CategoriesViewModel(
    categories: CategoryRepository,
    expenses: ExpenseRepository,
    private val archiveCategory: ArchiveCategoryUseCase,
    private val restoreCategory: RestoreCategoryUseCase,
    private val clock: Clock,
    private val zoneId: ZoneId,
) : ViewModel() {
    private val page = MutableStateFlow(CategoriesPage.List)
    private val dialog = MutableStateFlow<CategoriesDialog>(CategoriesDialog.None)
    private val notice = MutableStateFlow<CategoriesNotice?>(null)
    private var noticeJob: Job? = null

    val today: LocalDate = LocalDate.now(clock.withZone(zoneId))

    val state: StateFlow<CategoriesUiState> =
        combine(
            categories.observeActiveCategories(),
            categories.observeArchivedCategories(),
            expenses.observeAll(),
            page,
            dialog,
        ) { active, archived, rows, currentPage, currentDialog ->
            CategoriesUiState(
                active = active.sortedBy { it.name.lowercase() },
                archived = archived,
                usages = usagesOf(rows),
                page = currentPage,
                dialog = currentDialog,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CategoriesUiState())

    val noticeState: StateFlow<CategoriesNotice?> = notice.asStateFlow()

    fun onOpenArchive() {
        page.value = CategoriesPage.Archive
        dialog.value = CategoriesDialog.None
    }

    fun onCloseArchive() {
        page.value = CategoriesPage.List
    }

    fun onOpenMenu(category: Category) {
        if (category.isFallback) return
        dialog.value =
            if (category.isBuiltin) {
                CategoriesDialog.ArchiveConfirm(category)
            } else {
                CategoriesDialog.Actions(category)
            }
    }

    fun onArchiveFromMenu(category: Category) {
        dialog.value = CategoriesDialog.ArchiveConfirm(category)
    }

    fun onDismissDialog() {
        dialog.value = CategoriesDialog.None
    }

    fun onConfirmArchive() {
        val target = (dialog.value as? CategoriesDialog.ArchiveConfirm)?.category ?: return
        viewModelScope.launch {
            try {
                archiveCategory(target.id)
                dialog.value = CategoriesDialog.None
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                showNotice(CategoriesNotice.ArchiveFailed)
            }
        }
    }

    fun onRestore(id: Long) {
        viewModelScope.launch {
            try {
                restoreCategory(id)
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                showNotice(CategoriesNotice.RestoreFailed)
            }
        }
    }

    private fun showNotice(value: CategoriesNotice) {
        notice.value = value
        noticeJob?.cancel()
        noticeJob =
            viewModelScope.launch {
                delay(TOAST_MS)
                if (notice.value == value) {
                    notice.value = null
                }
            }
    }

    companion object {
        private const val TOAST_MS = 4_000L

        fun factory(
            categories: CategoryRepository,
            expenses: ExpenseRepository,
            archiveCategory: ArchiveCategoryUseCase,
            restoreCategory: RestoreCategoryUseCase,
            clock: Clock,
            zoneId: ZoneId,
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return CategoriesViewModel(
                    categories = categories,
                    expenses = expenses,
                    archiveCategory = archiveCategory,
                    restoreCategory = restoreCategory,
                    clock = clock,
                    zoneId = zoneId,
                ) as T
            }
        }
    }
}

internal fun usagesOf(expenses: List<Expense>): Map<Long, CategoryUsage> {
    val result = mutableMapOf<Long, CategoryUsage>()
    expenses.forEach { expense ->
        val current = result[expense.categoryId] ?: CategoryUsage()
        result[expense.categoryId] =
            CategoryUsage(count = current.count + 1, totalMinor = current.totalMinor + expense.amount.minor)
    }
    return result
}
