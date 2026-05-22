package com.luum.michi.app.core.platform

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
import com.luum.michi.app.resources.Res
import com.luum.michi.app.resources.account_circle
import com.luum.michi.app.resources.add
import com.luum.michi.app.resources.admin_panel_settings
import com.luum.michi.app.resources.animated_images
import com.luum.michi.app.resources.arrow_back
import com.luum.michi.app.resources.auto_awesome_mosaic
import com.luum.michi.app.resources.auto_stories
import com.luum.michi.app.resources.calendar_month
import com.luum.michi.app.resources.chats
import com.luum.michi.app.resources.close
import com.luum.michi.app.resources.creator_tools
import com.luum.michi.app.resources.download
import com.luum.michi.app.resources.dynamic_feed
import com.luum.michi.app.resources.explore
import com.luum.michi.app.resources.favorite
import com.luum.michi.app.resources.filter_list
import com.luum.michi.app.resources.forum
import com.luum.michi.app.resources.forward
import com.luum.michi.app.resources.heart_broken
import com.luum.michi.app.resources.help_support
import com.luum.michi.app.resources.history
import com.luum.michi.app.resources.info
import com.luum.michi.app.resources.keyboard_arrow_down
import com.luum.michi.app.resources.keyboard_arrow_left
import com.luum.michi.app.resources.keyboard_arrow_right
import com.luum.michi.app.resources.keyboard_arrow_up
import com.luum.michi.app.resources.language
import com.luum.michi.app.resources.lock
import com.luum.michi.app.resources.logout
import com.luum.michi.app.resources.mood
import com.luum.michi.app.resources.more_vert
import com.luum.michi.app.resources.notifications
import com.luum.michi.app.resources.perm_data_setting
import com.luum.michi.app.resources.qr_code_scanner
import com.luum.michi.app.resources.room_preferences
import com.luum.michi.app.resources.search
import com.luum.michi.app.resources.season_view
import com.luum.michi.app.resources.settings
import com.luum.michi.app.resources.settings_accessibility
import com.luum.michi.app.resources.switch_account
import com.luum.michi.app.resources.user_activity
import org.jetbrains.compose.resources.painterResource

object PlatformIcons {
    val Home: Painter @Composable get() = painterResource(Res.drawable.auto_awesome_mosaic)
    val Create: Painter @Composable get() = painterResource(Res.drawable.add)
    val Inbox: Painter @Composable get() = painterResource(Res.drawable.chats)
    val Search: Painter @Composable get() = painterResource(Res.drawable.search)
    val Account: Painter @Composable get() = painterResource(Res.drawable.account_circle)

    val Animation: Painter @Composable get() = painterResource(Res.drawable.animated_images)
    val Reading: Painter @Composable get() = painterResource(Res.drawable.auto_stories)
    val FilterList: Painter @Composable get() = painterResource(Res.drawable.filter_list)
    val Mood: Painter @Composable get() = painterResource(Res.drawable.mood)
    val Settings: Painter @Composable get() = painterResource(Res.drawable.settings)
    val Close: Painter @Composable get() = painterResource(Res.drawable.close)
    val ChevronUp: Painter @Composable get() = painterResource(Res.drawable.keyboard_arrow_up)
    val ChevronDown: Painter @Composable get() = painterResource(Res.drawable.keyboard_arrow_down)
    val ArrowBack: Painter @Composable get() = painterResource(Res.drawable.arrow_back)

    val SwitchAccount: Painter @Composable get() = painterResource(Res.drawable.switch_account)
    val Like: Painter @Composable get() = painterResource(Res.drawable.favorite)
    val DisLike: Painter @Composable get() = painterResource(Res.drawable.heart_broken)
    val Comments: Painter @Composable get() = painterResource(Res.drawable.forum)
    val Share: Painter @Composable get() = painterResource(Res.drawable.forward)
    val Download: Painter @Composable get() = painterResource(Res.drawable.download)
    val MoreVert: Painter @Composable get() = painterResource(Res.drawable.more_vert)
    val Season: Painter @Composable get() = painterResource(Res.drawable.season_view)
    val Explore: Painter @Composable get() = painterResource(Res.drawable.explore)
    val Add: Painter @Composable get() = painterResource(Res.drawable.add)
    val ChevronLeft: Painter @Composable get() = painterResource(Res.drawable.keyboard_arrow_left)
    val ChevronRight: Painter @Composable get() = painterResource(Res.drawable.keyboard_arrow_right)
    val Calendar: Painter @Composable get() = painterResource(Res.drawable.calendar_month)
    val QrScanner: Painter @Composable get() = painterResource(Res.drawable.qr_code_scanner)

    val Posts: Painter @Composable get() = painterResource(Res.drawable.dynamic_feed)

    val Logout: Painter @Composable get() = painterResource(Res.drawable.logout)
    val ManageAccount: Painter @Composable get() = painterResource(Res.drawable.admin_panel_settings)
    val Privacy: Painter @Composable get() = painterResource(Res.drawable.lock)
    val ContentPreferences: Painter @Composable get() = painterResource(Res.drawable.room_preferences)
    val History: Painter @Composable get() = painterResource(Res.drawable.history)
    val UserActivity: Painter @Composable get() = painterResource(Res.drawable.user_activity)
    val Notifications: Painter @Composable get() = painterResource(Res.drawable.notifications)
    val Language: Painter @Composable get() = painterResource(Res.drawable.language)
    val Accessibility: Painter @Composable get() = painterResource(Res.drawable.settings_accessibility)
    val DataPlayback: Painter @Composable get() = painterResource(Res.drawable.perm_data_setting)
    val CreatorTools: Painter @Composable get() = painterResource(Res.drawable.creator_tools)
    val HelpSupport: Painter @Composable get() = painterResource(Res.drawable.help_support)
    val Information: Painter @Composable get() = painterResource(Res.drawable.info)
}
