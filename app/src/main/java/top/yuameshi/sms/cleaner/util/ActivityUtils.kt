package top.yuameshi.sms.cleaner.util

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper

/**
 * Recursively unwraps a [ContextWrapper] chain to find the underlying [Activity].
 *
 * This is useful when you need an [Activity] reference from a Compose [Context],
 * which may be wrapped in one or more [ContextWrapper] instances.
 *
 * @return The [Activity] if found, or `null` if the context is not associated with an activity.
 */
fun Context.findActivity(): Activity? {
    var context = this
    while (context is ContextWrapper) {
        if (context is Activity) return context
        context = context.baseContext
    }
    return null
}
