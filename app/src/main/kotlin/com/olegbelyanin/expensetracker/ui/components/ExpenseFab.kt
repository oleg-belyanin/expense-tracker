package com.olegbelyanin.expensetracker.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.olegbelyanin.expensetracker.R
import com.olegbelyanin.expensetracker.ui.theme.ExpenseTheme
import com.olegbelyanin.expensetracker.ui.theme.FabSize

@Composable
fun ExpenseFab(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = ExpenseTheme.colors
    FloatingActionButton(
        onClick = onClick,
        modifier = modifier.size(FabSize),
        shape = ExpenseTheme.radii.full,
        containerColor = colors.action,
        contentColor = colors.onAction,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_add),
            contentDescription = stringResource(R.string.add_expense),
        )
    }
}
