package com.olegbelyanin.expensetracker.ui.startup

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.olegbelyanin.expensetracker.R
import com.olegbelyanin.expensetracker.ui.components.ScreenStatePanel
import com.olegbelyanin.expensetracker.ui.components.StatePanelType

@Composable
fun StartupScreen(failed: Boolean, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    if (failed) {
        ScreenStatePanel(
            type = StatePanelType.NoResults,
            title = stringResource(R.string.startup_failed_title),
            description = stringResource(R.string.startup_failed_description),
            actionLabel = stringResource(R.string.startup_retry),
            onAction = onRetry,
            modifier = modifier,
        )
    } else {
        ScreenStatePanel(
            type = StatePanelType.Loading,
            title = stringResource(R.string.startup_loading_title),
            description = stringResource(R.string.startup_loading_description),
            modifier = modifier,
        )
    }
}
