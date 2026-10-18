package com.luum.michi.app.notifications

import com.luum.michi.app.core.language.domain.EnglishStrings
import com.luum.michi.app.core.media.domain.toLocalMediaReleaseDateTime
import com.luum.michi.app.notifications.domain.model.NotificationTarget
import com.luum.michi.app.notifications.ui.model.dispatch
import com.luum.michi.app.notifications.ui.model.notificationTimeLabel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

private const val Now = 1_800_000_000L

/** The notifications row's presentation: relative time and tap routing. */
class NotificationsPresentationTest {

    @Test
    fun relativeTimeRunsToSevenDaysThenTheReadableDate() {
        assertEquals("now", notificationTimeLabel(Now, Now - 30, EnglishStrings))
        assertEquals("2min ago", notificationTimeLabel(Now, Now - 120, EnglishStrings))
        assertEquals("3h ago", notificationTimeLabel(Now, Now - 3 * 3_600, EnglishStrings))
        assertEquals("2d ago", notificationTimeLabel(Now, Now - 2 * 86_400, EnglishStrings))

        val old = Now - 10 * 86_400
        assertEquals(
            EnglishStrings.notificationDateLabel(old.toLocalMediaReleaseDateTime()),
            notificationTimeLabel(Now, old, EnglishStrings),
        )
    }

    @Test
    fun aMediaTargetOpensTheDetailAndEverythingElseTheBrowser() {
        var mediaId: Int? = null
        var url: String? = null
        val onMedia: (Int) -> Unit = { mediaId = it }
        val onUrl: (String) -> Unit = { url = it }

        NotificationTarget.Media(42).dispatch(onMedia, onUrl)
        assertEquals(42, mediaId)
        assertNull(url)

        NotificationTarget.Web("https://anilist.co/activity/7").dispatch(onMedia, onUrl)
        assertEquals("https://anilist.co/activity/7", url)
        assertEquals(42, mediaId)
    }
}
