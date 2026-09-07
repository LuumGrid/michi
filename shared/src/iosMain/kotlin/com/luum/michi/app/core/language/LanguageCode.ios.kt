package com.luum.michi.app.core.language

import platform.Foundation.NSLocale
import platform.Foundation.preferredLanguages

actual fun currentLanguageCode(): String? {
    return NSLocale.preferredLanguages.firstOrNull() as? String
}
