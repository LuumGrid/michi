package com.luum.michi.app.core.language

import java.util.Locale

actual fun currentPlatformLanguageCode(): String? = Locale.getDefault().language
