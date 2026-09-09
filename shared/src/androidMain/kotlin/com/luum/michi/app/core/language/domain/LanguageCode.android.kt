package com.luum.michi.app.core.language.domain

import java.util.Locale

actual fun currentLanguageCode(): String? = Locale.getDefault().language
