package com.rosan.installer.ui.page.installer.dialog.inner

import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rosan.installer.ui.icons.AppIcons
import com.rosan.installer.util.help

/**
 * 一个封装了字符串资源及其格式化参数的数据类。
 * @param id 字符串资源的 ID
 * @param formatArgs 填充字符串中占位符 (如 %1$s, %2$d) 所需的参数列表.
 */
data class UiText(
    @param:StringRes val id: Int,
    val formatArgs: List<Any> = emptyList()
)

val pausingIcon: @Composable () -> Unit = {
    Icon(
        imageVector = AppIcons.Pausing, contentDescription = null
    )
}

val workingIcon: @Composable () -> Unit = {
    Icon(
        imageVector = AppIcons.Working, contentDescription = null
    )
}

val menuIcon: @Composable () -> Unit = {
    Icon(
        imageVector = AppIcons.MenuOpen, contentDescription = null, modifier = Modifier.size(32.dp)
    )
}

val permissionIcon: @Composable () -> Unit = {
    Icon(
        imageVector = AppIcons.Permission,
        contentDescription = null,
        modifier = Modifier.size(32.dp)
    )
}

val errorTextBlock: ((error: Throwable) -> (@Composable () -> Unit)) = { error ->
    {
        var expanded by remember { mutableStateOf(false) }
        val rotation by animateFloatAsState(targetValue = if (expanded) -180f else 0f, label = "errorRotation")

        CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onErrorContainer) {
            Column(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.errorContainer)
                    .fillMaxWidth()
                    .padding(12.dp),
                // verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ) {
                            expanded = !expanded
                        },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = AppIcons.ArrowDropDown,
                        contentDescription = null,
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .rotate(rotation)
                    )
                    Text(
                        // 直接使用传入的 error 对象
                        text = error.help(),
                        fontWeight = FontWeight.Bold,
                        maxLines = Int.MAX_VALUE
                    )
                }
                AnimatedVisibility(
                    visible = expanded,
                    enter = fadeIn() + expandVertically(expandFrom = Alignment.Top),
                    exit = fadeOut() + shrinkVertically(shrinkTowards = Alignment.Top)
                ) {
                    Column {
                        // The spacer that replaces Arrangement.spacedBy
                        Spacer(modifier = Modifier.height(8.dp))
                        BasicTextField(
                            // 直接使用传入的 error 对象
                            value = error.stackTraceToString().trim(),
                            onValueChange = {},
                            readOnly = true
                        )
                    }
                }
            }
        }
    }
}