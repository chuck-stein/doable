package io.chuckstein.doable.common

import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.PluralStringResource
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

/**
 * Presentation-layer representation of text that will be displayed on the UI.
 * Call [resolveText] to convert to a string.
 */
sealed interface TextModel {

    /** Text derived from a string resource */
    data class Resource(val resource: StringResource, val formatArgs: List<Any> = emptyList()) : TextModel {
        constructor(resource: StringResource, vararg formatArg: Any) : this(resource, formatArg.asList())
    }

    /** Text derived from a specific string */
    data class Value(val text: String) : TextModel

    /** Text derived from a plural resource ID */
    data class Plural(
        val resource: PluralStringResource,
        val quantity: Int,
        val formatArgs: List<Any> = emptyList()
    ) : TextModel {

        constructor(
            resource: PluralStringResource,
            quantity: Int,
            vararg formatArg: Any
        ) : this(resource, quantity, formatArg.asList())
    }

    companion object {
        val empty = Value("")
    }
}

fun StringResource.toTextModel() = TextModel.Resource(this)
fun String.toTextModel() = TextModel.Value(this)

@Composable
fun TextModel.resolveText(): String = when (this) {
    is TextModel.Resource -> stringResource(resource, formatArgs)
    is TextModel.Value -> text
    is TextModel.Plural -> pluralStringResource(resource, quantity, formatArgs)
}

fun TextModel.isEmpty() = this is TextModel.Value && text.isEmpty()
fun TextModel.isBlank() = this is TextModel.Value && text.isBlank()