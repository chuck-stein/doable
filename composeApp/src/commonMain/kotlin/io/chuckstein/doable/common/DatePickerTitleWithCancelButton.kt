package io.chuckstein.doable.common

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import doable.composeapp.generated.resources.Res
import doable.composeapp.generated.resources.action_cancel
import io.telereso.kmp.core.icons.resources.Close
import org.jetbrains.compose.resources.stringResource

/**
 * Helper for putting a cancel/close button in the top right corner of a date picker dialog by using its title slot,
 * so that the dismiss button slot can be used for a secondary action instead of cancel.
 */
@Composable
fun DatePickerTitleWithCancelButton(
    title: TextModel,
    onCancelClick: () -> Unit
) {
    Row(verticalAlignment = Alignment.Top) {
        Text(
            text = title.resolveText(),
            modifier = Modifier
                .padding(vertical = 16.dp)
                .padding(start = 24.dp, end = 12.dp)
                .weight(1f)
        )
        DoableIconButton(
            icon = Icons.Close,
            contentDescription = stringResource(Res.string.action_cancel),
            modifier = Modifier.padding(4.dp),
            onClick = onCancelClick
        )
    }
}