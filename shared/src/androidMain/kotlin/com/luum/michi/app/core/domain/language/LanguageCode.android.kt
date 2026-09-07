package com.luum.michi.app.core.domain.language

import java.util.Locale

actual fun currentLanguageCode(): String? = Locale.getDefault().language
