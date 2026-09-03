package com.olegbelyanin.expensetracker.ui.components

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.olegbelyanin.expensetracker.R
import com.olegbelyanin.expensetracker.ui.navigation.AppTab
import com.olegbelyanin.expensetracker.ui.theme.BottomNavHeight
import com.olegbelyanin.expensetracker.ui.theme.ExpenseTheme

@Composable
fun BottomNav(selected: AppTab, onSelect: (AppTab) -> Unit, modifier: Modifier = Modifier) {
    val colors = ExpenseTheme.colors
    val spacing = ExpenseTheme.spacing
    Column(
        modifier =
        modifier
            .fillMaxWidth()
            .background(colors.surface),
    ) {
        HorizontalDivider(color = colors.border, thickness = 1.dp)
        Row(
            modifier =
            Modifier
                .fillMaxWidth()
                .height(BottomNavHeight)
                .padding(horizontal = spacing.md, vertical = spacing.xs),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AppTab.entries.forEach { tab ->
                val active = tab == selected
                val tint = if (active) colors.action else colors.textSecondary
                Column(
                    modifier =
                    Modifier
                        .width(96.dp)
                        .height(56.dp)
                        .clip(ExpenseTheme.radii.md)
                        .clickable { onSelect(tab) },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement =
                    Arrangement.spacedBy(4.dp, Alignment.CenterVertically),
                ) {
                    Icon(
                        painter = painterResource(tab.iconRes),
                        contentDescription = stringResource(tab.labelRes),
                        modifier = Modifier.size(22.dp),
                        tint = tint,
                    )
                    Text(
                        text = stringResource(tab.labelRes),
                        style = ExpenseTheme.typography.labelSmall,
                        color = tint,
                    )
                }
            }
        }
    }
}

private val AppTab.iconRes: Int
    @DrawableRes get() =
        when (this) {
            AppTab.Expenses -> R.drawable.ic_nav_expenses
            AppTab.Analytics -> R.drawable.ic_nav_analytics
            AppTab.Categories -> R.drawable.ic_nav_categories
        }

private val AppTab.labelRes: Int
    @StringRes get() =
        when (this) {
            AppTab.Expenses -> R.string.tab_expenses
            AppTab.Analytics -> R.string.tab_analytics
            AppTab.Categories -> R.string.tab_categories
        }
