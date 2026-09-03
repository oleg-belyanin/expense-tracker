package com.olegbelyanin.expensetracker.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.olegbelyanin.expensetracker.R
import com.olegbelyanin.expensetracker.ui.theme.ExpenseTheme

enum class CategoryGlyphKey(val storageKey: String, @DrawableRes val iconRes: Int?) {
    Groceries("groceries", R.drawable.ic_category_groceries),
    Cafe("cafe", R.drawable.ic_category_cafe),
    Transport("transport", R.drawable.ic_category_transport),
    Health("health", R.drawable.ic_category_health),
    Housing("housing", R.drawable.ic_category_housing),
    Comms("comms", R.drawable.ic_category_comms),
    Fun("fun", R.drawable.ic_category_fun),
    Clothes("clothes", R.drawable.ic_category_clothes),
    Home("home", R.drawable.ic_category_home),
    Other("other", R.drawable.ic_category_other),
    Pets("pets", R.drawable.ic_category_pets),
    Education("education", R.drawable.ic_category_education),
    Shopping("shopping", R.drawable.ic_category_shopping),
    Sports("sports", R.drawable.ic_category_sports),
    Travel("travel", R.drawable.ic_category_travel),
    Work("work", R.drawable.ic_category_work),
    Letter("letter", null),
    ;

    companion object {
        fun fromStorage(key: String): CategoryGlyphKey = when (key) {
            "communication" -> Comms
            "entertainment" -> Fun
            "clothing" -> Clothes
            else -> entries.firstOrNull { it.storageKey == key } ?: Letter
        }
    }
}

@Composable
fun CategoryGlyph(key: CategoryGlyphKey, modifier: Modifier = Modifier, size: Dp = 24.dp, letter: String = "") {
    val colors = ExpenseTheme.colors
    val typography = ExpenseTheme.typography
    val iconRes = key.iconRes
    if (iconRes != null) {
        Image(
            painter = painterResource(iconRes),
            contentDescription = null,
            modifier = modifier.size(size),
            contentScale = ContentScale.Fit,
            colorFilter = ColorFilter.tint(colors.onCategory),
        )
    } else {
        Box(
            modifier = modifier.size(size),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = letter.take(1).uppercase(),
                style = typography.titleSection,
                color = colors.onCategory,
            )
        }
    }
}
