package com.luum.michi.app.notifications

import com.luum.michi.app.core.language.domain.EnglishStrings
import com.luum.michi.app.core.language.domain.SpanishStrings
import com.luum.michi.app.core.media.domain.toLocalMediaReleaseDateTime
import com.luum.michi.app.notifications.domain.model.AppNotification
import com.luum.michi.app.notifications.domain.model.NotificationKind
import com.luum.michi.app.notifications.domain.model.NotificationTarget
import com.luum.michi.app.notifications.ui.model.dispatch
import com.luum.michi.app.notifications.ui.model.notificationTimeLabel
import com.luum.michi.app.notifications.ui.model.toRowItem
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private const val Now = 1_800_000_000L

private fun notification(
    kind: NotificationKind,
    userName: String? = null,
    mediaTitle: String? = null,
    threadTitle: String? = null,
    subjectName: String? = null,
    status: String? = null,
    episode: Int? = null,
    deletedMediaTitles: List<String> = emptyList(),
): AppNotification = AppNotification(
    id = 1,
    kind = kind,
    image = null,
    createdAtEpochSeconds = Now,
    target = NotificationTarget.None,
    episode = episode,
    mediaTitle = mediaTitle,
    threadTitle = threadTitle,
    userName = userName,
    subjectName = subjectName,
    status = status,
    deletedMediaTitles = deletedMediaTitles,
)

private fun text(n: AppNotification, strings: com.luum.michi.app.core.language.domain.LanguageStrings) =
    n.toRowItem(strings, Now).text

/** The substring has the readable date plus, when present, the past countdown. */
private fun expectedTime(ageSeconds: Long, relative: String?): String {
    val created = Now - ageSeconds
    val date = EnglishStrings.notificationDateLabel(created.toLocalMediaReleaseDateTime())
    return if (relative != null) "$date ($relative)" else date
}

/** The notifications row's presentation: one text per kind, and the timestamp. */
class NotificationsPresentationTest {

    @Test
    fun everyKindHasANonEmptyTextInBothLanguages() {
        NotificationKind.entries.forEach { kind ->
            val bare = notification(kind)
            assertTrue(text(bare, EnglishStrings).isNotBlank(), "EN $kind")
            assertTrue(text(bare, SpanishStrings).isNotBlank(), "ES $kind")
        }
        assertEquals(EnglishStrings.otherLabel, text(notification(NotificationKind.UNKNOWN), EnglishStrings))
        assertEquals(SpanishStrings.otherLabel, text(notification(NotificationKind.UNKNOWN), SpanishStrings))
    }

    @Test
    fun actorTextsUseTheNameAndFallBackPerLanguage() {
        assertEquals(
            "bob started following you",
            text(notification(NotificationKind.FOLLOWING, userName = "bob"), EnglishStrings),
        )
        assertEquals(
            "bob empezó a seguirte",
            text(notification(NotificationKind.FOLLOWING, userName = "bob"), SpanishStrings),
        )
        assertEquals(
            "Someone liked your activity",
            text(notification(NotificationKind.ACTIVITY_LIKE), EnglishStrings),
        )
        assertEquals(
            "A alguien le gustó tu actividad",
            text(notification(NotificationKind.ACTIVITY_LIKE), SpanishStrings),
        )
        assertEquals(
            "bob sent you a message",
            text(notification(NotificationKind.ACTIVITY_MESSAGE, userName = "bob"), EnglishStrings),
        )
        assertEquals(
            "bob te envió un mensaje",
            text(notification(NotificationKind.ACTIVITY_MESSAGE, userName = "bob"), SpanishStrings),
        )
    }

    @Test
    fun threadTextsUseTheThreadTitleAndFallBackPerLanguage() {
        val withThread = notification(
            NotificationKind.THREAD_COMMENT_REPLY,
            userName = "bob",
            threadTitle = "Best of 2026",
        )
        assertEquals("bob replied to your comment in Best of 2026", text(withThread, EnglishStrings))
        assertEquals("bob respondió a tu comentario en Best of 2026", text(withThread, SpanishStrings))

        val bare = notification(NotificationKind.THREAD_LIKE, userName = "bob")
        assertEquals("bob liked your thread", text(bare, EnglishStrings))
        assertEquals("A bob le gustó tu hilo", text(bare, SpanishStrings))

        val titled = notification(
            NotificationKind.THREAD_LIKE,
            userName = "bob",
            threadTitle = "Best of 2026",
        )
        assertEquals("bob liked your thread Best of 2026", text(titled, EnglishStrings))
        assertEquals("A bob le gustó tu hilo Best of 2026", text(titled, SpanishStrings))
    }

    @Test
    fun mediaTextsUseTheTitleAndFallBackPerLanguage() {
        assertEquals(
            "Se agregó Naruto a AniList",
            text(notification(NotificationKind.RELATED_MEDIA_ADDITION, mediaTitle = "Naruto"), SpanishStrings),
        )
        assertEquals(
            "Naruto was added to AniList",
            text(notification(NotificationKind.RELATED_MEDIA_ADDITION, mediaTitle = "Naruto"), EnglishStrings),
        )
        assertEquals(
            "Se eliminó Naruto de AniList",
            text(notification(NotificationKind.MEDIA_DELETION, mediaTitle = "Naruto"), SpanishStrings),
        )
        assertEquals(
            "Un título recibió cambios",
            text(notification(NotificationKind.MEDIA_DATA_CHANGE), SpanishStrings),
        )
    }

    @Test
    fun mergeListsTheDeletedTitlesOnlyWhenThereAreAny() {
        val merged = notification(NotificationKind.MEDIA_MERGE, mediaTitle = "Naruto", deletedMediaTitles = listOf("A", "B"))
        assertEquals("Fusionado en Naruto: A, B", text(merged, SpanishStrings))
        assertEquals("Merged into Naruto: A, B", text(merged, EnglishStrings))

        val alone = notification(NotificationKind.MEDIA_MERGE, mediaTitle = "Naruto")
        assertEquals("Fusionado en Naruto", text(alone, SpanishStrings))
        assertEquals("Merged into Naruto", text(alone, EnglishStrings))
    }

    @Test
    fun submissionTextsUseTheirSubjectAndStatus() {
        val media = notification(NotificationKind.MEDIA_SUBMISSION_UPDATE, subjectName = "My show", status = "PENDING")
        assertEquals("Tu envío de My show: PENDING", text(media, SpanishStrings))
        assertEquals("Your submission of My show: PENDING", text(media, EnglishStrings))

        val staff = notification(NotificationKind.STAFF_SUBMISSION_UPDATE, subjectName = "Anno")
        assertEquals("Tu envío de Anno: actualizado", text(staff, SpanishStrings))
        assertEquals("Your submission of Anno: updated", text(staff, EnglishStrings))
    }

    @Test
    fun aZeroSecondUnitIsOmitted() {
        assertEquals("hace 1 día", SpanishStrings.timeAgoLabel(1, 0, 0))
        assertEquals("1 day ago", EnglishStrings.timeAgoLabel(1, 0, 0))
        assertEquals("hace 2 horas", SpanishStrings.timeAgoLabel(0, 2, 0))
        assertEquals("2 hours ago", EnglishStrings.timeAgoLabel(0, 2, 0))
        assertEquals("hace 5 minutos", SpanishStrings.timeAgoLabel(0, 0, 5))

        assertEquals("en 2 horas", SpanishStrings.airingCountdownLabel(0, 2, 0))
        assertEquals("in 2 hours", EnglishStrings.airingCountdownLabel(0, 2, 0))
        assertEquals("en 3 días", SpanishStrings.airingCountdownLabel(3, 0, 0))
        assertEquals("in 3 days", EnglishStrings.airingCountdownLabel(3, 0, 0))
    }

    // ────────────── timestamp ──────────────

    @Test
    fun timestampIsTheDatePlusThePastCountdown() {
        assertEquals(expectedTime(30, "now"), notificationTimeLabel(Now, Now - 30, EnglishStrings))
        assertEquals(
            expectedTime(60, "1 minute ago"),
            notificationTimeLabel(Now, Now - 60, EnglishStrings),
        )
        assertEquals(
            expectedTime(5 * 60, "5 minutes ago"),
            notificationTimeLabel(Now, Now - 5 * 60, EnglishStrings),
        )
        assertEquals(
            expectedTime(2 * 3_600 + 5 * 60, "2 hours 5 minutes ago"),
            notificationTimeLabel(Now, Now - (2 * 3_600 + 5 * 60), EnglishStrings),
        )
        assertEquals(
            expectedTime(86_400 + 4 * 3_600, "1 day 4 hours ago"),
            notificationTimeLabel(Now, Now - (86_400 + 4 * 3_600), EnglishStrings),
        )
        assertEquals(
            expectedTime(3 * 86_400 + 4 * 3_600, "3 days 4 hours ago"),
            notificationTimeLabel(Now, Now - (3 * 86_400 + 4 * 3_600), EnglishStrings),
        )
    }

    @Test
    fun timestampInSpanishUsesTheWordedPastForm() {
        val created = Now - (3 * 86_400 + 4 * 3_600)
        val date = SpanishStrings.notificationDateLabel(created.toLocalMediaReleaseDateTime())
        assertEquals("$date (hace 3 días 4 horas)", notificationTimeLabel(Now, created, SpanishStrings))
    }

    @Test
    fun fromAWeekOnItIsJustTheDate() {
        val exactlyAWeek = notificationTimeLabel(Now, Now - 7 * 86_400, EnglishStrings)
        val older = notificationTimeLabel(Now, Now - 10 * 86_400, EnglishStrings)

        assertEquals(expectedTime(7 * 86_400, null), exactlyAWeek)
        assertEquals(expectedTime(10 * 86_400, null), older)
        assertTrue("(" !in exactlyAWeek, "no parenthesis from a week on")
    }

    // ────────────── dispatch ──────────────

    @Test
    fun dispatchRoutesEveryTargetAndNoneDoesNothing() {
        var mediaId: Int? = null
        var characterId: Int? = null
        var staffId: Int? = null
        var url: String? = null

        NotificationTarget.Media(1).dispatch({ mediaId = it }, { characterId = it }, { staffId = it }, { url = it })
        assertEquals(1, mediaId)
        NotificationTarget.Character(2).dispatch({ mediaId = it }, { characterId = it }, { staffId = it }, { url = it })
        assertEquals(2, characterId)
        NotificationTarget.Staff(3).dispatch({ mediaId = it }, { characterId = it }, { staffId = it }, { url = it })
        assertEquals(3, staffId)
        NotificationTarget.Web("https://anilist.co/activity/7")
            .dispatch({ mediaId = it }, { characterId = it }, { staffId = it }, { url = it })
        assertEquals("https://anilist.co/activity/7", url)

        NotificationTarget.None.dispatch({ mediaId = it }, { characterId = it }, { staffId = it }, { url = it })
        assertEquals(1, mediaId, "None touches nothing")
        assertEquals(2, characterId)
        assertEquals(3, staffId)
        assertEquals("https://anilist.co/activity/7", url)
    }
}
