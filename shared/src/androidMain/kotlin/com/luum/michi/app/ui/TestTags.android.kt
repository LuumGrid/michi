package com.luum.michi.app.ui

import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId

internal actual fun Modifier.testTagsAsResourceIds(): Modifier =
    semantics { testTagsAsResourceId = true }
