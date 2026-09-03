package com.olegbelyanin.expensetracker.ui.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.olegbelyanin.expensetracker.domain.expense.AnalyticsSlice
import com.olegbelyanin.expensetracker.domain.expense.ExpensePeriodPreset
import com.olegbelyanin.expensetracker.domain.expense.ExpensePeriodResolver
import com.olegbelyanin.expensetracker.domain.expense.ObserveAnalyticsUseCase
import com.olegbelyanin.expensetracker.model.Period
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId

enum class AnalyticsChart {
    Donut,
    Bars,
}

enum class AnalyticsDialog {
    None,
    Periods,
    CustomStart,
    CustomEnd,
}

class AnalyticsViewModel(observe: ObserveAnalyticsUseCase, clock: Clock, zoneId: ZoneId) : ViewModel() {
    val today: LocalDate = LocalDate.now(clock.withZone(zoneId))

    private val selection = MutableStateFlow(AnalyticsSelection())
    private val chart = MutableStateFlow(AnalyticsChart.Donut)
    private val dialog = MutableStateFlow(AnalyticsDialog.None)
    private val draftPreset = MutableStateFlow(ExpensePeriodPreset.CURRENT_MONTH)
    private val draftCustom = MutableStateFlow<Period?>(null)

    val selectionState: StateFlow<AnalyticsSelection> = selection.asStateFlow()
    val chartState: StateFlow<AnalyticsChart> = chart.asStateFlow()
    val dialogState: StateFlow<AnalyticsDialog> = dialog.asStateFlow()
    val draftPresetState: StateFlow<ExpensePeriodPreset> = draftPreset.asStateFlow()
    val draftCustomState: StateFlow<Period?> = draftCustom.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val slice: StateFlow<AnalyticsSlice?> =
        selection.flatMapLatest { observe.observe(it.toFilter()) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun onChart(value: AnalyticsChart) {
        chart.value = value
    }

    fun onPreviousMonth() {
        selection.value = selection.value.previous(today)
    }

    fun onNextMonth() {
        selection.value = selection.value.next(today)
    }

    fun onOpenPeriods() {
        draftPreset.value = selection.value.preset
        draftCustom.value = selection.value.customPeriod
            ?: ExpensePeriodResolver.defaultCustom(today)
        dialog.value = AnalyticsDialog.Periods
    }

    fun onDismissDialog() {
        dialog.value = AnalyticsDialog.None
    }

    fun onDraftPreset(value: ExpensePeriodPreset) {
        draftPreset.value = value
        if (value == ExpensePeriodPreset.CUSTOM) {
            if (draftCustom.value == null) {
                draftCustom.value = ExpensePeriodResolver.defaultCustom(today)
            }
            dialog.value = AnalyticsDialog.CustomStart
        }
    }

    fun onCustomStart(date: LocalDate) {
        val end = draftCustom.value?.endInclusive ?: today
        draftCustom.value = Period.of(date, end)
        dialog.value = AnalyticsDialog.CustomEnd
    }

    fun onCustomEnd(date: LocalDate) {
        val start = draftCustom.value?.startInclusive ?: today
        draftCustom.value = Period.of(start, date)
        dialog.value = AnalyticsDialog.Periods
    }

    fun onApplyPeriod() {
        val preset = draftPreset.value
        selection.value = AnalyticsSelection(
            preset = preset,
            customPeriod = if (preset == ExpensePeriodPreset.CUSTOM) draftCustom.value else null,
        )
        dialog.value = AnalyticsDialog.None
    }

    companion object {
        fun factory(observe: ObserveAnalyticsUseCase, clock: Clock, zoneId: ZoneId): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    @Suppress("UNCHECKED_CAST")
                    return AnalyticsViewModel(observe, clock, zoneId) as T
                }
            }
    }
}
