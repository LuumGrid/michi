package com.luum.michi.app.notifications

import com.luum.michi.app.notifications.domain.model.AppNotification
import com.luum.michi.app.notifications.domain.model.NotificationKind
import com.luum.michi.app.notifications.domain.model.NotificationTarget
import com.luum.michi.app.notifications.domain.model.notificationDetails
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class NotificationDetailsTest {

    private fun notification(
        kind: NotificationKind,
        reason: String? = null,
        notes: String? = null,
        status: String? = null,
        deletedMediaTitles: List<String> = emptyList(),
    ) = AppNotification(
        id = 1,
        kind = kind,
        image = null,
        createdAtEpochSeconds = 0,
        target = NotificationTarget.None,
        reason = reason,
        notes = notes,
        status = status,
        deletedMediaTitles = deletedMediaTitles,
    )

    private val detailKinds = listOf(
        NotificationKind.MEDIA_DATA_CHANGE,
        NotificationKind.MEDIA_MERGE,
        NotificationKind.MEDIA_DELETION,
        NotificationKind.MEDIA_SUBMISSION_UPDATE,
        NotificationKind.STAFF_SUBMISSION_UPDATE,
        NotificationKind.CHARACTER_SUBMISSION_UPDATE,
    )

    @Test
    fun eachDetailKindReturnsItsFieldsAndBareOnesReturnNull() {
        detailKinds.forEach { kind ->
            assertNull(notification(kind).notificationDetails(), "$kind without fields")

            val details = notification(
                kind = kind,
                reason = "<b>Reason</b>",
                notes = "Some notes",
                status = "ACCEPTED",
                deletedMediaTitles = listOf("Old title"),
            ).notificationDetails()

            assertEquals("Reason", details?.reason, "$kind reason")
            assertEquals("Some notes", details?.notes, "$kind notes")
            assertEquals("ACCEPTED", details?.status, "$kind raw status")
            assertEquals(listOf("Old title"), details?.affectedTitles, "$kind titles")
        }
    }

    @Test
    fun socialAndUnrelatedKindsAlwaysReturnNull() {
        listOf(
            NotificationKind.AIRING,
            NotificationKind.FOLLOWING,
            NotificationKind.ACTIVITY_LIKE,
            NotificationKind.ACTIVITY_REPLY,
            NotificationKind.THREAD_LIKE,
            NotificationKind.RELATED_MEDIA_ADDITION,
            NotificationKind.UNKNOWN,
        ).forEach { kind ->
            assertNull(
                notification(
                    kind = kind,
                    reason = "Reason",
                    notes = "Notes",
                    status = "ACCEPTED",
                    deletedMediaTitles = listOf("Old"),
                ).notificationDetails(),
                "$kind must never carry details",
            )
        }
    }

    @Test
    fun blankFieldsAreDroppedAndAnAllBlankBlockIsNull() {
        assertNull(
            notification(
                kind = NotificationKind.MEDIA_DATA_CHANGE,
                reason = "   ",
                notes = "<br>",
                status = " ",
                deletedMediaTitles = listOf("", "  "),
            ).notificationDetails(),
        )

        val details = notification(
            kind = NotificationKind.MEDIA_MERGE,
            reason = " ",
            notes = "Only notes",
        ).notificationDetails()

        assertNull(details?.reason)
        assertNull(details?.status)
        assertTrue(details?.affectedTitles.orEmpty().isEmpty())
        assertEquals("Only notes", details?.notes)
    }

    @Test
    fun fieldsAreCleanedOfHtmlAndTitlesSkipBlanks() {
        val details = notification(
            kind = NotificationKind.MEDIA_MERGE,
            reason = "<b>Duplicated</b> entry",
            notes = "A &amp; B",
            deletedMediaTitles = listOf("<i>Old</i>", " ", "New"),
        ).notificationDetails()

        assertEquals("Duplicated entry", details?.reason)
        assertEquals("A & B", details?.notes)
        assertEquals(listOf("Old", "New"), details?.affectedTitles)
    }
}
