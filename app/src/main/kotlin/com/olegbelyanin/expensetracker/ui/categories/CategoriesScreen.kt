package com.olegbelyanin.expensetracker.ui.categories

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.olegbelyanin.expensetracker.R
import com.olegbelyanin.expensetracker.model.Category
import com.olegbelyanin.expensetracker.ui.components.BackAction
import com.olegbelyanin.expensetracker.ui.components.BottomSheetSize
import com.olegbelyanin.expensetracker.ui.components.ButtonTone
import com.olegbelyanin.expensetracker.ui.components.CategoryAvatar
import com.olegbelyanin.expensetracker.ui.components.ExpenseBottomSheet
import com.olegbelyanin.expensetracker.ui.components.PrimaryButton
import com.olegbelyanin.expensetracker.ui.components.toVisual
import com.olegbelyanin.expensetracker.ui.format.ExpenseFormat
import com.olegbelyanin.expensetracker.ui.navigation.AppTab
import com.olegbelyanin.expensetracker.ui.navigation.TabScaffold
import com.olegbelyanin.expensetracker.ui.theme.ExpenseTheme
import com.olegbelyanin.expensetracker.ui.theme.MinTapTarget
import java.time.LocalDate
import java.time.ZoneId

@Composable
fun CategoriesScreen(
    viewModel: CategoriesViewModel,
    onOpenSettings: () -> Unit,
    onTabSelected: (AppTab) -> Unit,
    onCreateCategory: () -> Unit,
    onEditCategory: (Long) -> Unit,
    modifier: Modifier = Modifier,
    today: LocalDate = viewModel.today,
    zoneId: ZoneId = ZoneId.systemDefault(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val colors = ExpenseTheme.colors
    val typography = ExpenseTheme.typography
    val spacing = ExpenseTheme.spacing
    val onArchive = state.page == CategoriesPage.Archive
    TabScaffold(
        title =
        if (onArchive) {
            stringResource(R.string.category_archive_title)
        } else {
            stringResource(R.string.tab_categories)
        },
        selectedTab = AppTab.Categories,
        onTabSelected = onTabSelected,
        onOpenSettings = onOpenSettings,
        modifier = modifier,
        leading = if (onArchive) {
            { BackAction(onClick = viewModel::onCloseArchive) }
        } else {
            null
        },
        showSettings = !onArchive,
        subtitle = {
            Text(
                text =
                if (onArchive) {
                    stringResource(
                        R.string.category_archive_subtitle,
                        ExpenseFormat.categoryCount(state.archived.size),
                    )
                } else {
                    stringResource(R.string.categories_subtitle, state.active.size)
                },
                style = if (onArchive) typography.labelSmall else typography.bodySecondary,
                color = colors.textSecondary,
            )
        },
    ) {
        if (onArchive) {
            ArchivePage(
                categories = state.archived,
                usages = state.usages,
                today = today,
                zoneId = zoneId,
                onRestore = viewModel::onRestore,
            )
        } else {
            CategoryListPage(
                categories = state.active,
                archivedCount = state.archived.size,
                onOpenArchive = viewModel::onOpenArchive,
                onEdit = onEditCategory,
                onMenu = viewModel::onOpenMenu,
                onCreate = onCreateCategory,
            )
        }
    }
    when (val dialog = state.dialog) {
        CategoriesDialog.None -> Unit

        is CategoriesDialog.Actions ->
            CategoryActionsSheet(
                category = dialog.category,
                onEdit = {
                    viewModel.onDismissDialog()
                    onEditCategory(dialog.category.id)
                },
                onArchive = { viewModel.onArchiveFromMenu(dialog.category) },
                onDismiss = viewModel::onDismissDialog,
            )

        is CategoriesDialog.ArchiveConfirm ->
            ArchiveCategorySheet(
                category = dialog.category,
                usage = state.usages[dialog.category.id] ?: CategoryUsage(),
                onConfirm = viewModel::onConfirmArchive,
                onDismiss = viewModel::onDismissDialog,
            )
    }
}

@Composable
private fun CategoryListPage(
    categories: List<Category>,
    archivedCount: Int,
    onOpenArchive: () -> Unit,
    onEdit: (Long) -> Unit,
    onMenu: (Category) -> Unit,
    onCreate: () -> Unit,
) {
    val colors = ExpenseTheme.colors
    val typography = ExpenseTheme.typography
    val spacing = ExpenseTheme.spacing
    Column(modifier = Modifier.fillMaxWidth()) {
        ArchiveEntry(
            subtitle = stringResource(R.string.category_archive_entry, ExpenseFormat.categoryCount(archivedCount)),
            onClick = onOpenArchive,
        )
        Text(
            text = stringResource(R.string.categories_active_section),
            style = typography.titleSection,
            color = colors.textPrimary,
            modifier = Modifier.padding(top = spacing.sm, bottom = spacing.xs),
        )
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(spacing.xxs),
        ) {
            items(categories, key = { it.id }) { category ->
                ActiveCategoryRow(
                    category = category,
                    onClick =
                    if (category.isBuiltin) {
                        null
                    } else {
                        { onEdit(category.id) }
                    },
                    onMenu = if (category.isFallback) null else ({ onMenu(category) }),
                )
            }
        }
        PrimaryButton(
            label = stringResource(R.string.category_new),
            onClick = onCreate,
            modifier = Modifier.padding(vertical = spacing.xs),
        )
    }
}

@Composable
private fun ArchivePage(
    categories: List<Category>,
    usages: Map<Long, CategoryUsage>,
    today: LocalDate,
    zoneId: ZoneId,
    onRestore: (Long) -> Unit,
) {
    val colors = ExpenseTheme.colors
    val typography = ExpenseTheme.typography
    val spacing = ExpenseTheme.spacing
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = spacing.xs),
        verticalArrangement = Arrangement.spacedBy(spacing.sm),
    ) {
        Column(
            modifier =
            Modifier
                .fillMaxWidth()
                .clip(ExpenseTheme.radii.md)
                .background(colors.surfaceSubtle)
                .padding(horizontal = spacing.sm, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = stringResource(R.string.category_archive_info_title),
                style = typography.bodyPrimary,
                color = colors.textPrimary,
            )
            Text(
                text = stringResource(R.string.category_archive_info_body),
                style = typography.labelSmall,
                color = colors.textSecondary,
            )
        }
        Text(
            text = stringResource(R.string.category_archive_section),
            style = typography.titleSection,
            color = colors.textPrimary,
        )
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(spacing.xs),
        ) {
            items(categories, key = { it.id }) { category ->
                ArchivedCategoryCard(
                    category = category,
                    usage = usages[category.id] ?: CategoryUsage(),
                    today = today,
                    zoneId = zoneId,
                    onRestore = { onRestore(category.id) },
                )
            }
        }
        Text(
            text = stringResource(R.string.category_archive_footnote),
            style = typography.bodySecondary,
            color = colors.textSecondary,
            modifier = Modifier.padding(bottom = spacing.sm),
        )
    }
}

@Composable
private fun ArchiveEntry(subtitle: String, onClick: () -> Unit) {
    val colors = ExpenseTheme.colors
    val typography = ExpenseTheme.typography
    val spacing = ExpenseTheme.spacing
    Row(
        modifier =
        Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(ExpenseTheme.radii.md)
            .background(colors.surfaceSubtle)
            .clickable(onClick = onClick)
            .padding(horizontal = spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.sm),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.category_archive_title),
                style = typography.bodyPrimary,
                color = colors.textPrimary,
            )
            Text(
                text = subtitle,
                style = typography.labelSmall,
                color = colors.textSecondary,
            )
        }
        Text(
            text = "›",
            style = typography.titleSection,
            color = colors.textPrimary,
        )
    }
}

@Composable
private fun ActiveCategoryRow(category: Category, onClick: (() -> Unit)?, onMenu: (() -> Unit)?) {
    val colors = ExpenseTheme.colors
    val visual = category.toVisual(colors)
    Row(
        modifier =
        Modifier
            .fillMaxWidth()
            .heightIn(min = MinTapTarget)
            .clip(ExpenseTheme.radii.md)
            .background(colors.surface)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = ExpenseTheme.spacing.xs, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(ExpenseTheme.spacing.xs),
    ) {
        CategoryAvatar(
            glyph = visual.glyph,
            containerColor = visual.containerColor,
            size = 32.dp,
            letter = visual.letter,
        )
        Text(
            text = category.name,
            style = ExpenseTheme.typography.bodyPrimary,
            color = colors.textPrimary,
            modifier = Modifier.weight(1f),
        )
        CategoryKindBadge(builtin = category.isBuiltin)
        if (onMenu != null) {
            Box(
                modifier =
                Modifier
                    .size(MinTapTarget)
                    .clip(ExpenseTheme.radii.md)
                    .clickable(onClick = onMenu),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "⋮",
                    style = ExpenseTheme.typography.bodyPrimary,
                    color = colors.textSecondary,
                )
            }
        }
    }
}

@Composable
private fun ArchivedCategoryCard(
    category: Category,
    usage: CategoryUsage,
    today: LocalDate,
    zoneId: ZoneId,
    onRestore: () -> Unit,
) {
    val colors = ExpenseTheme.colors
    val spacing = ExpenseTheme.spacing
    val visual = category.toVisual(colors)
    val archivedAt = category.archivedAt?.atZone(zoneId)?.toLocalDate()
    val archivedLabel =
        if (archivedAt != null) {
            stringResource(R.string.category_archived_on, ExpenseFormat.archivedDay(archivedAt, today))
        } else {
            stringResource(R.string.category_archived_on, ExpenseFormat.archivedDay(today, today))
        }
    Column(
        modifier =
        Modifier
            .fillMaxWidth()
            .clip(ExpenseTheme.radii.md)
            .background(colors.surface)
            .border(1.dp, colors.border, ExpenseTheme.radii.md)
            .padding(horizontal = spacing.sm, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(spacing.xs),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing.xs),
        ) {
            CategoryAvatar(
                glyph = visual.glyph,
                containerColor = visual.containerColor,
                size = 40.dp,
                letter = visual.letter,
            )
            Text(
                text = category.name,
                style = ExpenseTheme.typography.bodyPrimary,
                color = colors.textPrimary,
                modifier = Modifier.weight(1f),
            )
            CategoryKindBadge(builtin = category.isBuiltin)
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing.xs),
        ) {
            Text(
                text =
                stringResource(
                    R.string.category_archive_meta,
                    ExpenseFormat.expenseCount(usage.count),
                    archivedLabel,
                ),
                style = ExpenseTheme.typography.labelSmall,
                color = colors.textSecondary,
                modifier = Modifier.weight(1f),
            )
            Box(
                modifier =
                Modifier
                    .heightIn(min = MinTapTarget)
                    .clip(ExpenseTheme.radii.full)
                    .border(1.dp, colors.action, ExpenseTheme.radii.full)
                    .clickable(onClick = onRestore)
                    .padding(horizontal = spacing.md),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(R.string.category_restore),
                    style = ExpenseTheme.typography.labelControl,
                    color = colors.action,
                )
            }
        }
    }
}

@Composable
private fun CategoryKindBadge(builtin: Boolean) {
    val colors = ExpenseTheme.colors
    val label =
        if (builtin) {
            stringResource(R.string.category_badge_builtin)
        } else {
            stringResource(R.string.category_badge_custom)
        }
    val textColor = if (builtin) colors.textSecondary else colors.action
    Box(
        modifier =
        Modifier
            .clip(ExpenseTheme.radii.full)
            .then(
                if (builtin) {
                    Modifier.background(colors.surfaceSubtle)
                } else {
                    Modifier.border(1.dp, colors.action, ExpenseTheme.radii.full)
                },
            )
            .padding(horizontal = ExpenseTheme.spacing.xs, vertical = ExpenseTheme.spacing.xxs),
    ) {
        Text(
            text = label,
            style = ExpenseTheme.typography.labelSmall,
            color = textColor,
        )
    }
}

@Composable
private fun CategoryActionsSheet(
    category: Category,
    onEdit: () -> Unit,
    onArchive: () -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = ExpenseTheme.colors
    ExpenseBottomSheet(onDismiss = onDismiss, size = BottomSheetSize.Compact) {
        Text(
            text = category.name,
            style = ExpenseTheme.typography.titleSection,
            color = colors.textPrimary,
        )
        SheetAction(label = stringResource(R.string.category_edit), onClick = onEdit)
        SheetAction(label = stringResource(R.string.category_archive_action), onClick = onArchive)
    }
}

@Composable
private fun SheetAction(label: String, onClick: () -> Unit) {
    Box(
        modifier =
        Modifier
            .fillMaxWidth()
            .heightIn(min = MinTapTarget)
            .clip(ExpenseTheme.radii.md)
            .clickable(onClick = onClick)
            .padding(horizontal = ExpenseTheme.spacing.xs),
        contentAlignment = Alignment.CenterStart,
    ) {
        Text(
            text = label,
            style = ExpenseTheme.typography.bodyPrimary,
            color = ExpenseTheme.colors.textPrimary,
        )
    }
}

@Composable
private fun ArchiveCategorySheet(
    category: Category,
    usage: CategoryUsage,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = ExpenseTheme.colors
    val typography = ExpenseTheme.typography
    val spacing = ExpenseTheme.spacing
    val kind =
        if (category.isBuiltin) {
            stringResource(R.string.category_kind_builtin)
        } else {
            stringResource(R.string.category_kind_custom)
        }
    val description =
        if (usage.count > 0) {
            stringResource(
                R.string.category_archive_description,
                ExpenseFormat.expenseCount(usage.count),
                ExpenseFormat.money(usage.totalMinor),
                kind,
            )
        } else {
            stringResource(R.string.category_archive_description_empty, kind)
        }
    ExpenseBottomSheet(onDismiss = onDismiss, size = BottomSheetSize.Medium) {
        Text(
            text = stringResource(R.string.category_archive_confirm_title, category.name),
            style = typography.titleSection,
            color = colors.textPrimary,
        )
        Text(
            text = description,
            style = typography.bodySecondary,
            color = colors.textSecondary,
        )
        Column(
            modifier =
            Modifier
                .fillMaxWidth()
                .clip(ExpenseTheme.radii.md)
                .background(colors.surfaceSubtle)
                .padding(horizontal = spacing.sm, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(spacing.xxs),
        ) {
            Text(
                text = stringResource(R.string.category_archive_notice_title),
                style = typography.bodyPrimary,
                color = colors.textPrimary,
            )
            Text(
                text = stringResource(R.string.category_archive_notice_body),
                style = typography.bodySecondary,
                color = colors.textSecondary,
            )
        }
        PrimaryButton(
            label = stringResource(R.string.category_archive_action),
            onClick = onConfirm,
            tone = ButtonTone.Destructive,
        )
        PrimaryButton(
            label = stringResource(R.string.cancel),
            onClick = onDismiss,
            tone = ButtonTone.Subtle,
        )
    }
}
