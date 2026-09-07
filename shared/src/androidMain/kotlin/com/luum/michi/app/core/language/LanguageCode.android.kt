package com.luum.michi.app.core.language

import java.util.Locale

actual fun currentLanguageCode(): String? = Locale.getDefault().language
