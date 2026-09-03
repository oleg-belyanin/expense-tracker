package com.olegbelyanin.expensetracker.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.olegbelyanin.expensetracker.R
import com.olegbelyanin.expensetracker.ui.theme.ExpenseTheme
import com.olegbelyanin.expensetracker.ui.theme.MinTapTarget

@Composable
fun SettingsAction(onClick: () -> Unit, modifier: Modifier = Modifier) {
    AppBarIconButton(
        iconRes = R.drawable.ic_settings,
        contentDescription = stringResource(R.string.settings),
        onClick = onClick,
        modifier = modifier,
    )
}

@Composable
fun BackAction(onClick: () -> Unit, modifier: Modifier = Modifier) {
    AppBarIconButton(
        iconRes = R.drawable.ic_back,
        contentDescription = stringResource(R.string.back),
        onClick = onClick,
        modifier = modifier,
    )
}

@Composable
private fun AppBarIconButton(
    iconRes: Int,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
        modifier
            .size(MinTapTarget)
            .clip(ExpenseTheme.radii.full)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = contentDescription,
            modifier = Modifier.size(MinTapTarget),
            tint = ExpenseTheme.colors.icon,
        )
    }
}
