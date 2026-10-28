// SPDX-License-Identifier: GPL-3.0-only

package app.tellyscope.ui

import app.tellyscope.adb.InstallKind
import app.tellyscope.adb.InstallRefusal
import app.tellyscope.adb.Peripheral
import app.tellyscope.adb.TvFolder
import app.tellyscope.data.Action
import app.tellyscope.data.AppNames
import app.tellyscope.data.DeviceState
import app.tellyscope.data.ResetPart
import app.tellyscope.data.ResetProgress
import app.tellyscope.data.Snapshot
import app.tellyscope.data.TvSettings
import app.tellyscope.data.sourceName
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Date
import java.util.Locale

/**
 * Every user-facing sentence about actions and time lives here so wording is
 * reviewed in one place: every button says what it does before it runs.
 */
object Copy {
    fun button(action: Action) = when (action) {
        Action.Stop -> "停止"
        Action.Disable -> "禁用"
        Action.Enable -> "开启"
        Action.Open -> "打开"
        Action.Uninstall -> "卸载"
        Action.Change, Action.ClearCaches, Action.Home, Action.HomeBack, Action.Reset -> error("$action has its own button")
    }

    fun confirm(action: Action) = when (action) {
        Action.Stop -> "是，停止它"
        Action.Disable -> "是，禁用它"
        Action.Enable -> "是，开启它"
        Action.Open -> "是，打开它"
        Action.Uninstall -> "是，卸载它"
        Action.Home -> "是，切换桌面"
        Action.HomeBack -> "是，切换回去"
        Action.Change, Action.ClearCaches, Action.Reset -> error("$action isn't an app action")
    }

    /**
     * Names the package next to the label: the label comes from the app itself, and any app can
     * call itself "Settings". The package id is what actually gets changed.
     */
    fun explain(action: Action, app: String, pkg: String) = "$app ($pkg)".let { who ->
        when (action) {
            Action.Stop -> "停止 $who，直到你再次打开它。"
            Action.Disable -> "隐藏 $who 并停止它运行。不会删除任何东西，你可以在这里重新开启它。"
            Action.Enable -> "重新开启 $who。"
            Action.Open -> "在电视上打开 $who。"
            Action.Uninstall -> "删除 $who 及其保存的全部内容。此操作无法撤销；要恢复需重新安装。"
            Action.Change, Action.ClearCaches, Action.Home, Action.HomeBack, Action.Reset -> error("$action isn't a one-app action")
        }
    }

    fun done(action: Action, app: String) = when (action) {
        Action.Stop -> "已停止 $app"
        Action.Disable -> "已禁用 $app"
        Action.Enable -> "已开启 $app"
        Action.Open -> "已打开 $app"
        Action.Uninstall -> "已卸载 $app"
        // A setting's label is the whole sentence: "Private DNS set to …", as is a reset's.
        Action.Change, Action.Reset -> app
        Action.ClearCaches -> "已清除 $app"
        Action.Home -> "已将 $app 设为桌面"
        Action.HomeBack -> "已将桌面切换回 $app"
    }

    /** Said in the confirm before Stop, Disable or Uninstall of an app the TV's list keeps. */
    const val KEEP_WARNING = "这台电视的清单建议保留它。"

    /** Where the lists come from, for the Debloat screen and the snackbar after a change. */
    fun listSource(custom: String?) =
        if (custom == null) "清单来自 tellyscope-devices，基于 cleanyourtv 制作" else "Lists from ${sourceName(custom)}"

    /** The Home screen card at the top of Tweaks: every launcher on the TV, and which one Home opens. */
    const val HOME_SCREEN = "桌面"

    fun opensOnHome(app: String) = "按 Home 键时会打开 $app。"

    const val NO_DEFAULT = "未设置默认桌面：请在下方选择。"
    const val ADDED = "已添加"

    fun builtIn(off: Boolean) = "电视自带" + if (off) " · 已关闭" else ""

    const val CURRENT = "当前"
    const val KEEPS_HOME = "靠无障碍服务保持桌面"
    const val MAKE_HOME = "设为桌面"
    const val GET_LAUNCHER = "换个启动器"
    const val GET_LAUNCHER_TEXT = "从 Play 商店安装一个，比如 Projectivy 或 FLauncher。随时可以切换回去。"

    /** The Apps tab's launcher rows: switching lives in the card, this opens it. */
    const val HOME_SETTINGS = "桌面设置"

    fun makeHomeTitle(app: String) = "把 $app 设为桌面吗？"

    /** A launcher the user added in place of the TV's own, which is turned off: otherwise many TVs switch back by themselves. */
    fun explainReplace(app: String, current: String, currentPkg: String, service: Boolean) = buildString {
        append("Home opens $app. To stop the TV switching back by itself, Tellyscope turns off $current, the TV's built-in launcher.")
        append("\n\nThat's the only launcher it ever turns off. Launchers you added stay on, so you can switch between them any time.")
        append("\n\nUndo in History turns $current back on.")
        if (currentPkg in GOOGLE_HOMES) append("\n\nSome Google TV features, like the Apps row and the Live tab, are part of $current and go away with it.")
        if (service) append("\n\nNext, you choose whether to turn on its accessibility service.")
    }

    /**
     * Away from [current], a launcher the user added: it stays on, only its accessibility service
     * goes off when it has one on, since that would take Home straight back. [off]: [app] is the
     * TV's own launcher, turned off by an earlier switch.
     */
    fun explainLeave(app: String, off: Boolean, current: String, currentService: Boolean, service: Boolean) = buildString {
        append(if (off) "Tellyscope turns $app back on and makes it open when you press Home." else "Tellyscope makes $app open when you press Home.")
        append("\n\n")
        if (currentService) append("It also turns off $current's accessibility service, which would otherwise take Home straight back. ")
        append("$current stays installed and on, so you can make it Home again any time.")
        append("\n\nUndo in History makes $current Home again")
        append(if (currentService) " and turns its service back on." else ".")
        if (service) append("\n\nNext, you choose whether to turn on its accessibility service.")
    }

    /**
     * The home switch touches two apps, so it names both. The old launcher is disabled, not just
     * replaced: otherwise many TVs switch back to it by themselves.
     */
    fun explainHome(app: String, pkg: String, current: String, currentPkg: String, service: Boolean = false) = buildString {
        append("Makes $app ($pkg) open when you press Home, then disables $current ($currentPkg) so the TV can't switch back by itself. ")
        append("Undo in History turns $current back on and makes it Home again.")
        if (currentPkg in GOOGLE_HOMES) append("\n\nSome Google TV features, like the Apps row and the Live tab, are part of $current and go away with it.")
        if (service) append("\n\nNext, you choose whether to turn on its accessibility service.")
    }

    /** No default home and no launcher of the TV's own on: nothing is turned off, so Undo can only turn its service off. */
    fun explainFirstHome(app: String, pkg: String, service: Boolean = false) = buildString {
        append("Makes $app ($pkg) open when you press Home. The TV has no default home screen now, and nothing else is turned off.")
        if (service) append("\n\nNext, you choose whether to turn on its accessibility service.")
    }

    /** The separate yes for a launcher's accessibility service: what it does, what it can see, how it goes. */
    fun serviceTitle(app: String) = "开启 $app 的无障碍服务吗？"

    fun explainService(app: String, other: String) =
        "它能让 Home 键一直打开 $app。没有它，这台电视的 Home 键可能仍会打开 $other。\n\n" +
            "无障碍服务可以看到屏幕内容。只有信任 $app 时才开启。\n\n" +
            "在“历史记录”里撤销，或把另一个启动器设为桌面，即可关闭它。"

    const val TURN_IT_ON = "开启它"
    const val SWITCH_WITHOUT = "不开启也切换"
    const val NOT_NOW = "以后再说"
    const val TURN_ON = "开启"
    const val GOT_IT = "知道了"
    const val SERVICE_OFF = "无障碍服务已关闭"

    /** The TV page's card while the launcher on Home has its service off; [other] is the launcher Home may still open. */
    fun serviceOffTitle(other: String?) = "Home 键可能打开 ${other ?: TV_OWN_LAUNCHER}"

    const val TV_OWN_LAUNCHER = "电视自带的启动器"

    /** Install a home screen: the Apps tab's card, the picker, the wait for the Store, and its dead ends. */
    const val BETTER_HOME = "换个更好的桌面"

    fun betterHomeText(current: String?) =
        "把 ${current ?: "电视桌面"} 换成没有广告的启动器。Play 商店免费提供，随时可以切换回去。"

    const val CHOOSE_ONE = "选一个"
    const val CHOOSE_HOME = "选一个桌面"
    const val CHOOSE_HOME_TEXT = "每个都会替换电视桌面。Tellyscope 会在电视上的 Play 商店里打开它，你用遥控器安装。"
    const val RECOMMENDED = "推荐"
    const val USES_SERVICE = "使用无障碍服务"
    const val NO_SERVICE = "开源 · 不使用无障碍服务"
    const val NOT_AFFILIATED = "Tellyscope 与这些启动器及其开发者均无关联。"
    const val OPEN_IN_STORE = "在 Play 商店中打开"
    const val INSTALL_HOME = "安装一个桌面"
    const val INSTALL_IT = "在电视上安装它"

    fun installItText(name: String) =
        "电视上的 Play 商店已打开 $name 的页面。用遥控器选择安装，装好后 Tellyscope 会自动继续。"

    fun storeOn(tv: String) = "$tv 上的 Play 商店"

    const val WAITING_INSTALL = "正在等待安装…"
    const val DOWNLOADS_TAKE = "下载可能需要一分钟。Tellyscope 看不到下载过程，只能看到是否装好。"
    const val OPEN_STORE_AGAIN = "再次打开 Play 商店"
    const val MAKE_IT_HOME = "把它设为桌面吗？"

    fun isInstalled(name: String) = "$name 已安装。"

    fun launcherLater(name: String) = "$name 已安装。以后要用时，在“小工具”→“桌面”里对它选择“设为桌面”。"

    fun launcherInstalled(name: String) = "$name 已安装。按遥控器的 Home 键选择它。"

    fun launcherIsHome(name: String) = "$name 已经是你的桌面了。"

    const val NO_STORE = "Play 商店没有打开"

    fun noStoreText(tv: String, name: String) =
        "$tv 没有为 Tellyscope 打开 Play 商店。请在电视上自己打开 Play 商店并搜索 $name。" +
            "Tellyscope 会继续留意，装好后自动继续。"

    const val CHOOSE_ANOTHER = "换一个"

    fun notInstalled(name: String) = "$name 未安装"

    const val NOT_INSTALLED_TEXT =
        "Play 商店在电视上安装完成前就关掉了。如果商店说这台电视不支持，就换一个启动器。"

    /** The Tweaks row that reopens it after Not now on the card. */

    fun serviceOffText(app: String) = "$app 的无障碍服务已关闭，电视可能会把 Home 键夺回去。"

    private val GOOGLE_HOMES = setOf("com.google.android.tvlauncher", "com.google.android.apps.tv.launcherx")

    /**
     * Force-stop has no true undo, and undoing a Turn on disables the app; the button says what it
     * actually does.
     */
    fun undo(undo: Action) = when (undo) {
        Action.Open -> "再次打开"
        Action.Disable -> button(Action.Disable)
        else -> "撤销"
    }

    fun ago(at: Long, now: Long): String {
        val s = ((now - at) / 1000).coerceAtLeast(0)
        return when {
            s < 10 -> "刚刚"
            s < 60 -> "$s 秒前"
            s < 3600 -> "${s / 60} 分钟前"
            s < 86_400 -> "${s / 3600} 小时前"
            else -> "${s / 86_400} 天前"
        }
    }

    fun uptime(s: Long): String {
        val d = s / 86_400
        val h = (s % 86_400) / 3600
        val m = (s % 3600) / 60
        return when {
            d > 0 -> "${d}d ${h}h"
            h > 0 -> "${h}h ${m}m"
            else -> "${m}m"
        }
    }

    /** Uptime for people: the largest unit or two, spelled out. */
    fun upFor(s: Long): String {
        fun n(v: Long, unit: String) = "$v $unit${if (v == 1L) "" else "s"}"
        val d = s / 86_400
        val h = (s % 86_400) / 3600
        val m = (s % 3600) / 60
        return when {
            d > 0 && h > 0 -> "${n(d, "day")}, ${n(h, "hour")}"
            d > 0 -> n(d, "day")
            h > 0 -> n(h, "hour")
            else -> n(m, "minute")
        }
    }

    /** "May 2023", in the phone's language. */
    fun month(m: YearMonth): String = m.format(DateTimeFormatter.ofPattern("MMMM yyyy"))

    /** Network speed in the same units as [bytes], so a download's size and speed line up. */
    fun speed(bytesPerSec: Long) = bytes(bytesPerSec) + "/s"

    fun mb(v: Int) = if (v >= 1024) "%.1f GB".format(v / 1024.0) else "$v MB"

    fun uninstallTitle(app: String) = "卸载 $app 吗？"

    /** The confirm for the launcher on Home: Home goes back first, and a failed step stops it. */
    fun uninstallHome(app: String, back: String, service: Boolean) = listOfNotNull(
        "$app is your home screen. Before removing it, Tellyscope:",
        "1. Makes $back the home screen again, turning it back on if it's off.",
        "2. Turns off $app's accessibility service.".takeIf { service },
        "${if (service) 3 else 2}. Uninstalls it, with everything saved in it. This can't be undone.",
        "If a step fails, Tellyscope stops there, says which one, and doesn't uninstall.",
    ).joinToString("\n\n")

    /** The Apps tabs' header over the apps the TV needs, and why they're apart. */
    const val SYSTEM = "系统"
    const val SYSTEM_WHY = "电视必需 · 无法更改"

    /** Debloat with nothing left on the list: what's done, what's next, then Apps. */
    const val GATE_TITLE = "清单上的应用已全部关闭"
    const val GATE_DONE = "cleanyourtv 清单建议这台电视关闭的应用已经全部关闭了。"
    const val GATE_LAUNCHER = "想再精简，就换一个极简桌面。它会替换电视自带的、带广告和推荐的桌面。"
    const val GATE_APPS = "你仍然可以在“应用”里逐个选择保留或关闭哪些应用。"
    const val GO_TO_APPS = "去“应用”看看"
    const val DONT_SHOW_AGAIN = "不再显示"

    /** Apps, while the TV's app list is being read. */
    const val READING_APPS = "正在读取电视的应用…"
    const val READING_APPS_LINE = "需要几秒钟。"

    /** Running, with no word from the TV. */
    const val TV_OFFLINE = "电视离线"
    const val RUNNING_OFFLINE = "连接电视后，正在运行的应用会显示在这里。"

    const val ANDROID_SYSTEM = AppNames.SYSTEM

    /** The word next to the status dot, so the state never rests on colour alone. */
    fun state(s: DeviceState) = when (s) {
        DeviceState.Online -> "已连接"
        DeviceState.Offline -> "离线"
        DeviceState.NotAllowed -> "未获授权"
        DeviceState.Waiting -> "正在连接"
        DeviceState.NeedsPairing -> "需要配对码"
        DeviceState.Found -> "未连接"
    }

    /** Under 10 GB a whole number hides too much: a 1.5 GB TV isn't a 2 GB one. */
    fun gbOf(used: Double, total: Double) =
        if (total < 10) "%.1f of %.1f GB".format(used, total) else "%.1f of %.0f GB".format(used, total)

    /** Free storage in plain words, rounded down so it never promises room that isn't there; null if unknown. */
    fun free(s: Snapshot): String? = free(s.storageUsedGb, s.storageTotalGb)

    /** [free] from the two numbers, for a TV's saved facts as well as a live reading. */
    fun free(used: Double?, total: Double?): String? {
        val gb = used?.let { u -> total?.let { t -> (t - u).coerceAtLeast(0.0) } } ?: return null
        return if (gb < 10) "%.1f GB free".format(Math.floor(gb * 10) / 10) else "${gb.toInt()} GB free"
    }

    /** An offline TV that has never sent stats; a refusing TV is on, it just hasn't allowed us. */
    fun noStats(s: DeviceState) =
        if (s == DeviceState.NotAllowed) "电视允许这部手机后，状态数据就会显示。"
        else "还没有状态数据。电视重新开机后就会显示。"

    const val WRONG_CODE = "配对码不对。请核对电视上的 6 位数字再试一次。"
    const val UNREACHABLE = "电视没有回应。请确认电视已开机、与手机在同一 Wi-Fi，再试一次。"
    const val PAIR_INSTEAD = "电视还没有放行这部手机。请用电视上的配对码配对一次。"
    const val LOGGED_OUT = "你已被登出"
    const val LOGGED_OUT_WHY =
        "电视移除了这部手机的权限。Tellyscope 已断开并不再请求，直到你重新连接，电视上不会再弹出提示。"
    val LOGGED_OUT_CAUSES = listOf(
        "电视上选择了“撤销 USB 调试授权”。",
        "另一部手机（或这部手机上的另一个 Tellyscope 版本）使用了“移除其他所有访问”。",
    )
    fun loggedOutOf(name: String) = "已登出 $name"

    /** When the TV took the access back, on the logged-out page. */
    fun loggedOutAt(at: Long, now: Long): String {
        // Calendar days, so 23:00 reads "yesterday" the next morning.
        val zone = ZoneId.systemDefault()
        val days = ChronoUnit.DAYS.between(Instant.ofEpochMilli(at).atZone(zone).toLocalDate(), Instant.ofEpochMilli(now).atZone(zone).toLocalDate())
        return when {
            now - at < 5 * 60_000 -> "刚刚登出"
            days == 0L -> "今天登出，" + SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(at))
            days == 1L -> "昨天登出"
            else -> "Logged out $days days ago"
        }
    }

    data class Health(val ok: Boolean, val text: String)

    /**
     * The one line at the top of a TV's page: the most pressing problem, or that all is well.
     * [heaviest] are app names by memory use, largest first; there is no per-app storage size yet.
     */
    fun health(s: Snapshot, heaviest: List<String>): Health {
        val storage = s.storageUsedGb?.let { u -> s.storageTotalGb?.takeIf { it > 0 }?.let { u / it } }
        val ram = if (s.ramTotalMb > 0) s.ramUsedMb.toDouble() / s.ramTotalMb else null
        val most = heaviest.take(2).takeIf { it.isNotEmpty() }?.let { ": ${it.joinToString(" and ")} use the most" }.orEmpty()
        return when {
            storage != null && storage >= FULL -> Health(false, "存储空间快满了")
            ram != null && ram >= FULL -> Health(false, "内存快满了$most")
            (s.tempC ?: 0) >= HOT_C -> Health(false, "运行温度过高")
            else -> Health(true, "运行正常")
        }
    }

    const val FULL = 0.9
    private const val HOT_C = 80

    fun bytes(v: Long): String = when {
        v >= 1L shl 30 -> "%.1f GB".format(v / (1L shl 30).toDouble())
        v >= 1L shl 20 -> "%.1f MB".format(v / (1L shl 20).toDouble())
        v >= 1L shl 10 -> "${v shr 10} KB"
        else -> "$v bytes"
    }

    fun folder(f: TvFolder) = when (f) {
        TvFolder.DOWNLOAD -> "下载"
        TvFolder.MOVIES -> "影片"
    }

    // Sideload

    fun installButton(kind: InstallKind) = when (kind) {
        InstallKind.NEW -> "安装"
        InstallKind.UPDATE -> "更新"
        InstallKind.REINSTALL -> "重新安装"
        InstallKind.DOWNGRADE -> "安装旧版本"
    }

    fun installKind(kind: InstallKind, onTv: String?, picked: String?) = when (kind) {
        InstallKind.NEW -> "这台电视上还没有。点一下即可安装。"
        InstallKind.UPDATE -> "更新电视上的版本" + versions(onTv, picked) + "。设置会保留。"
        InstallKind.REINSTALL -> "电视上已经是这个版本" + (onTv?.let { " ($it)" } ?: "") + "。这会再装一次覆盖它。"
        InstallKind.DOWNGRADE -> "这个版本比电视上的旧" +
            (if (onTv != null && picked != null) " ($picked here, $onTv on the TV)" else "") +
            "。Android 通常会拒绝降级，而且应用保存的数据可能与旧版本不兼容。"
    }

    private fun versions(onTv: String?, picked: String?) =
        if (onTv != null && picked != null) " from $onTv to $picked" else ""

    const val ALLOW_DOWNGRADE = "仍要安装旧版本"
    const val INSTALL_KEEP_OPEN = "安装完成前请保持 Tellyscope 打开。"

    fun installed(kind: InstallKind, app: String) = when (kind) {
        InstallKind.UPDATE -> "已更新 $app。"
        InstallKind.REINSTALL -> "已重新安装 $app。"
        InstallKind.NEW, InstallKind.DOWNGRADE -> "已安装 $app。"
    }

    fun installRefused(reason: InstallRefusal, app: String) = when (reason) {
        InstallRefusal.SIGNATURE ->
            "The $app on the TV comes from a different developer, so Android won't replace it. If you're sure, uninstall it first: from Apps here if you installed it, or on the TV."
        InstallRefusal.DOWNGRADE -> "The TV has a newer $app and won't go back to an older one."
        InstallRefusal.NO_SPACE -> "电视空间不足。请先在电视上禁用或卸载一些东西，再试一次。"
        InstallRefusal.TV_SAID_NO ->
            "电视阻止了安装。请在电视的安全设置里查看“安装未知来源应用”或“应用验证”，再试一次。"
        InstallRefusal.TV_TOO_OLD -> "$app needs a newer version of Android than this TV has."
        InstallRefusal.WRONG_CPU ->
            "This $app is built for a different kind of processor. Look for a version made for this TV, often marked armeabi-v7a or arm64-v8a."
        InstallRefusal.APP_TOO_OLD -> "$app was made for a very old version of Android, and this TV won't install it."
        InstallRefusal.MISSING_FEATURE -> "$app needs something this TV doesn't have, like a touchscreen or a camera."
        InstallRefusal.MISSING_PARTS ->
            "$app comes in several parts and one is missing. Pick all of its .apk files together, or its .apks bundle."
        InstallRefusal.TEST_ONLY -> "This is a developer test build of $app, which the TV won't install."
        InstallRefusal.CONFLICT -> "Another app on the TV already uses something $app needs, so the TV refused it."
        InstallRefusal.BROKEN -> "这个文件不是可用的应用。可能已损坏或只下载了一半，请重新获取。"
        InstallRefusal.UNKNOWN -> "电视没有安装 $app。"
    }

    const val NOT_AN_APP = "这个文件不是 Android 应用。请选择 .apk、多个分包 .apk 文件，或 .apks / .xapk 安装包。"
    const val SIDELOAD_LOST = "中途与电视断开了。请确认电视仍开机并已连接，再试一次。"

    // Files

    fun sent(name: String, savedAs: String, f: TvFolder) =
        if (savedAs == name) "已将 $name 发送到${folder(f)}" else "Sent to ${folder(f)} as $savedAs, since $name was already there"

    fun saved(name: String) = "已将 $name 保存到手机"
    fun sendFailed(name: String) = "无法发送 $name。请确认电视仍连接且有剩余空间。"
    fun saveFailed(name: String) = "无法保存 $name。请确认电视仍连接且手机有剩余空间。"

    const val CANCELLED_SEND = "已取消。电视上没有留下任何东西。"
    const val CANCELLED_SAVE = "已取消。没有保存任何东西。"
    const val CANT_READ = "无法读取手机上的这个文件。"
    const val CANT_UNDO = "这个版本的 Tellyscope 无法撤销该设置。"
    const val FILES_LOST = "无法读取这个文件夹。请确认电视仍连接。"

    // Remote

    const val KEY_REFUSED = "电视没有接受这个按键。"
    const val TYPE_REFUSED = "电视没有接受这段文字。请先在电视上选中一个输入框，再发送。"
    const val REMOTE_LOST = "与电视断开了。请确认电视仍开机并已连接。"
    const val NO_UPDATE_SCREEN = "这台电视没有 Tellyscope 能打开的更新页面。请在电视的“设置”→“系统”或“关于”里找。"
    const val UPDATES_OPENED = "已在电视上打开更新页面。请看电视检查更新。"
    const val REBOOT_REFUSED = "电视拒绝重启。请改用它自己的遥控器或电源键。"
    const val REBOOT_IGNORED = "电视没有重启。请改用它自己的遥控器或电源键。"
    const val REMOTE_OFFLINE_WAKE = "重新连接后即可恢复"
    const val TYPE_HINT = "先在电视上选中一个输入框（比如搜索框），再在这里输入。"
    const val BACKSPACE = "删除在电视上输入的最后一个字"
    const val SCREEN_OFF = "关屏"
    const val SCREEN_ON = "开屏"
    const val SCREEN_OFF_WAKE = "按任意键唤醒"
    const val SCREEN_OFF_TIP = "关闭电视画面。应用保持连接，按任意键即可唤醒。"
    const val SCREEN_OFF_REFUSED = "这台电视无法通过 Tellyscope 关闭画面。"
    const val SCREEN_ON_REFUSED = "电视画面没有恢复。请再试一次，或重启电视。"

    fun cantType(text: String): String {
        val odd = text.firstOrNull { it !in ' '..'~' }
        return when {
            text.isEmpty() -> "请先输入内容。"
            text.length > app.tellyscope.adb.Remote.MAX_TEXT -> "That's too long to send in one go. Send up to ${app.tellyscope.adb.Remote.MAX_TEXT} characters at a time."
            odd == '\n' || odd == '\t' -> "电视一次只能输入一行。"
            odd != null -> "The TV can only type plain letters, numbers and symbols, not \u201c$odd\u201d."
            else -> "The TV reads \u201c%s\u201d as a space, so it can't type that."
        }
    }

    const val REMOTE_RESTART = "重启电视"
    const val REBOOT_TITLE = "重启电视吗？"
    const val REBOOT_TEXT = "电视会先关机再开机。回来后 Tellyscope 会自动重新连接，通常在一分钟内。"
    const val RESTARTING_TITLE = "正在重启…"
    const val RESTARTING_TEXT = "电视正在重启。Tellyscope 会自动重新连接。"
    const val RESTART_LATE_TITLE = "电视还没有回来"
    const val RESTART_LATE_STATUS = "还没回来"
    const val RESTART_LATE_TEXT = "已经过去一分钟了。有些电视启动较慢；点“重试”会再等一分钟。"
    const val RESTART_LOST_TEXT = "请用电视自己的遥控器开机，再点“重试”。"

    fun waitingFor(name: String, ms: Long): String {
        val s = (ms / 1000).coerceAtLeast(0)
        return "正在等待 $name · ${s / 60}:${(s % 60).toString().padStart(2, '0')}"
    }

    fun restartLost(name: String) = "仍然连不上 $name"
    fun backOnline(name: String) = "已恢复在线。$name 已重新连接。"

    // Dock

    const val ADD_TO_DOCK = "添加到 Dock"
    const val DOCK_EMPTY = "Dock 最多放 4 个电视应用，点一下即可打开。"
    const val DOCK_PICK_TITLE = "向 Dock 添加应用"
    const val DOCK_PICK_HINT = "点一个应用即可添加。"
    const val DOCK_ADDED = "已添加"
    const val DOCK_NO_APPS = "仍在读取电视的应用。过一会儿再打开。"
    const val DOCK_CHANGE = "更换应用"
    const val DOCK_REMOVE = "从 Dock 移除"
    const val DOCK_REORDER = "长按并移动可调整顺序"

    fun dockPickFrom(name: String) = "$name 上已安装的应用"
    fun dockSwap(label: String) = "用另一个应用替换 $label 的位置"
    fun cantOpen(label: String) = "电视无法打开 $label。如果它被关闭了，请在“应用”里重新开启。"

    fun leftDock(labels: List<String>) =
        if (labels.size == 1) "${labels[0]} was uninstalled, so it left the dock."
        else "${labels.size} apps were uninstalled, so they left the dock."

    fun restarting(minutes: Long) =
        if (minutes < 3) "Tellyscope reconnects on its own when the TV is back, usually within a couple of minutes."
        else "有点慢。有些电视重启后会关闭网络调试。如果一直回不来，请在电视的“开发者选项”里重新打开。"

    const val TWEAKS = "电视小工具"
    const val DNS_TITLE = "用 Private DNS 屏蔽广告"
    fun dnsState(host: String?) = host?.let { "On: $it" } ?: "已关闭。开启后可屏蔽电视上所有应用里的广告和追踪器。"
    const val DNS_TOO_OLD = "需要 Android 9 或更高版本。这台电视版本较旧。"
    const val DNS_DIALOG = "在整台电视上屏蔽广告吗？"
    const val DNS_TEXT = "The TV will look up every address through this ad-blocking DNS provider. AdGuard is free and needs no account. " +
        "Tellyscope then checks the TV can still look addresses up, and puts it back if not. Undo is in History."
    const val DNS_FIELD = "DNS 提供商"
    const val DNS_BAD = "请输入类似 dns.adguard-dns.com 的主机名，IP 地址在这里无效。"
    const val DNS_CONFIRM = "开启"
    const val ANIMATIONS_TITLE = "加快动画"
    fun animationsState(scale: String?) =
        if (scale == TvSettings.FAST) "已开启：菜单移动速度翻倍"
        else "已关闭。开启后菜单会感觉更跟手。"
    const val CACHES_TITLE = "清除所有应用缓存"
    const val CACHES_TEXT = "清除每个应用的临时文件来释放空间。应用数据和登录状态会保留。"
    const val CACHES_BUTTON = "清除缓存"
    fun cachesCleared(freedKb: Long?) =
        if (freedKb == null || freedKb <= 0) "已清除应用缓存" else "Cleared app caches, freed ${bytes(freedKb * 1024)}"
    const val SCREENSHOT_TITLE = "截图"
    const val SCREENSHOT_TEXT = "把电视画面保存到手机。受保护的视频（比如大多数流媒体应用）会显示为黑色。"
    const val SCREENSHOT_BUTTON = "截图"
    fun screenshotName(at: Long) =
        "TV screenshot " + SimpleDateFormat("yyyy-MM-dd HH.mm.ss", Locale.ROOT).format(Date(at)) + ".png"
    const val SCREENSHOT_SAVED = "截图已保存到手机"
    const val SCREENSHOT_FAILED = "无法截图。请确认电视仍连接。"
    const val REQUEST_TITLE = "另一台设备想控制这台电视"
    const val REQUEST_TEXT = "电视正在询问是否允许一台设备用这个密钥指纹进行调试。" +
        "只有当你自己发起、且指纹与那台设备显示的一致时，才允许。"
    const val REQUEST_ALWAYS = "始终允许这台设备"
    const val REQUEST_ALLOW = "允许"
    const val REQUEST_DENY = "拒绝"
    const val REQUEST_ALLOWED = "已允许。那台设备现在可以控制电视了。"
    const val REQUEST_DENIED = "已拒绝。那台设备无法连接。"
    const val REQUEST_GONE = "电视上的提示已关闭或变化，所以什么都没点。"

    // Who has access
    const val WARNING_TITLE = "另一台电脑已连接到这台电视"
    const val WARNING_TEXT = "它能做 Tellyscope 能做的一切：安装或删除应用、读取文件、控制屏幕。"
    const val ITS_MINE = "是我自己的"
    const val REMOVE_ACCESS = "移除其他所有访问…"
    const val CONNECTED_NOW = "当前已连接"
    const val WHO_HAS_ACCESS = "谁有访问权限"

    /** The TV menu's rows for the screens it opens. */
    const val HISTORY = "历史记录"
    const val ADVANCED_MONITOR = "高级监控"

    /** In the TV menu and in Settings: opens Android's share sheet with the repo link. */
    const val SHARE = "分享 Tellyscope"

    /** Reset TV: its row in the TV menu, its screen, and what it says before, during and after. */
    const val RESET_TV = "把电视恢复到出厂状态"
    const val RESET_KEEPS = "保留应用数据和登录状态"

    fun apps(n: Int) = if (n == 1) "1 app" else "$n apps"

    fun resetFound(n: Int) = "$n ${if (n == 1) "change" else "changes"} since this TV left the factory"
    const val RESET_FOUND_TEXT = "应用数据和登录状态会保留。目前还没有改动任何东西。"
    fun turnOnApps(n: Int) = "Turn on ${apps(n)}"
    const val TURN_ON_TEXT = "手动关闭的预装应用，来自任何手机或工具"
    fun bringBackApps(n: Int) = "Bring back ${apps(n)}"
    const val BRING_BACK_TEXT = "已被卸载的预装应用。它们仍在电视上。"
    fun resetHome(to: String) = "Make $to the home screen"
    fun resetHomeText(from: String, service: Boolean) =
        "Instead of $from" + (if (service) ", with its accessibility service turned off" else "") + ". $from stays installed."

    /** Under what Reset found: what it leaves alone. [maker] is the TV's brand, when known. */
    fun resetLeaves(shippedOff: Int, maker: String?) =
        (if (shippedOff > 0) "${apps(shippedOff)} ${maker ?: "the maker"} shipped switched off stay off. " else "") +
            "你安装的应用和启动器会保持安装状态。"

    const val NOTHING_TO_RESET = "没有需要恢复的内容"
    fun nothingToReset(ownHome: String?, maker: String?) =
        "Every preinstalled app is on" + (ownHome?.let { ", and $it is the home screen" } ?: "") + ". " +
            "Apps ${maker ?: "the maker"} shipped switched off stay off."

    fun resetting(p: ResetProgress, home: String?) = when (p.part) {
        ResetPart.TurnOn -> "Turning on apps: ${p.done} of ${p.of}."
        ResetPart.BringBack -> "Bringing back apps: ${p.done} of ${p.of}."
        ResetPart.Home -> "Making ${home ?: "电视自带的启动器"} the home screen."
    } + " Keep the TV on and connected."
    const val RESETTING = "Resetting…"
    const val RESET_APPS_STALE = "恢复已完成，但应用列表没有刷新。请重新打开电视查看。"
    const val RESET_BUTTON = "恢复电视"

    const val RESET_DONE = "电视已恢复到出厂状态"
    const val RESET_PARTLY = "恢复只完成了一部分"
    fun resetDoneText(apps: Int, home: String?) = listOfNotNull(
        when (apps) {
            0 -> null
            1 -> "1 preinstalled app is back"
            else -> "$apps preinstalled apps are back"
        },
        home?.let { "$it is the home screen" },
    ).joinToString(", and ") + ". Apps' data and sign-ins stayed."
    fun resetPartlyText(failed: Int) =
        "${if (failed == 1) "有 1 步" else "$failed 步"}没有成功。其余部分已恢复到出厂状态。没有丢失任何东西。"
    fun turnedOn(done: Int, of: Int) = if (done == of) "Turned on ${apps(of)}" else "Turned on $done of ${apps(of)}"
    fun broughtBack(done: Int, of: Int) = if (done == of) "Brought back ${apps(of)}" else "Brought back $done of ${apps(of)}"
    fun isHome(app: String) = "$app is the home screen"
    fun stillHome(app: String) = "$app is still the home screen"
    fun tryAgain(app: String?) = if (app != null) "Try $app again" else "再试一次"

    /** Debloat while it turns the ticked apps off, then its result. */
    fun disablingApps(done: Int, of: Int) = "Turning off apps: $done of $of. Keep the TV on and connected."
    const val DISABLING = "Disabling…"
    const val DISABLING_NOTE = "不会删除任何东西。可以在“历史记录”里重新开启任何一个。"
    const val DEBLOAT_DONE = "精简完成"
    const val DEBLOAT_PARTLY = "精简只完成了一部分"
    const val DEBLOAT_RESULT_TEXT = "没有删除任何东西；可以在“历史记录”里撤销其中任何一项。"
    fun turnedOff(done: Int, of: Int) = if (done == of) "Turned off ${apps(of)}" else "Turned off $done of ${apps(of)}"
    fun stillOn(app: String) = "$app is still on"

    /** Under History in the menu, and on top of the History screen: apps Restore everything turns back on. */
    fun stillOff(n: Int) = if (n == 1) "1 app still off" else "$n apps still off"
    fun stillOffTitle(n: Int) = if (n == 1) "1 app Tellyscope turned off is still off" else "$n apps Tellyscope turned off are still off"
    const val STILL_OFF_TEXT = "Restore everything turns them back on. Apps that were off before Tellyscope's first change stay off."
    fun othersConnected(n: Int) = if (n == 1) "1 other connected now" else "$n others connected now"
    fun connections(n: Int) = if (n == 1) "1 connection" else "$n connections"
    fun connectedFrom(address: String, n: Int) = "From $address · ${connections(n)}"
    const val ON_TV = "这台电视上的一个应用"
    const val ACCESS_INTRO = "每台获得 ADB 授权的电脑都可以安装或删除应用、读取文件、控制屏幕。"
    const val THIS_PHONE = "这部手机"
    const val TELLYSCOPE = "Tellyscope"
    const val NOT_THIS_PHONE = "不是这部手机"
    const val YOURS = "你的"
    const val ALLOWED_COMPUTERS = "已授权的电脑"
    const val UNNAMED_COMPUTER = "未命名的电脑"
    const val CANT_CHECK_CONNECTED = "这台电视没有说明谁已连接。"
    const val CANT_CHECK_ALLOWED = "这台电视没有列出已授权的电脑。"
    const val NOBODY_ALLOWED = "没有任何电脑获得授权。下一台连接的电脑需要在电视上获得允许。"
    const val ACCESS_FOOT_LINK = "电视没有说明某个连接背后是哪台已授权的电脑。如果你不认识某个地址，请移除其他所有访问。"
    const val ACCESS_FOOT_ALL = "Android can't remove one computer on its own: this clears every computer, then you allow Tellyscope again."
    /** Rough on purpose: the TV stamps a key when any connection uses it, not per session. */
    fun lastSeen(at: Long?, now: Long): String {
        if (at == null) return "从未出现"
        val days = ((now - at) / 86_400_000).coerceAtLeast(0)
        return when {
            now - at < 5 * 60_000 -> "刚刚出现过"
            days == 0L -> "今天出现过，" + SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(at))
            days == 1L -> "昨天出现过"
            days < 30 -> "Last seen $days days ago"
            else -> "一个多月前出现过"
        }
    }
    /** Enough of a fingerprint to tell keys apart: the first three and last two pairs. */
    fun shortFingerprint(fp: String): String {
        val parts = fp.split(':')
        return if (parts.size <= 5) fp else (parts.take(3) + "…" + parts.takeLast(2)).joinToString(":")
    }

    // Remove all other access
    const val REMOVE_TITLE = "移除其他所有访问"
    const val CONFIRM_TITLE = "移除其他所有访问吗？"
    const val CONFIRM_TEXT = "这台电视授权过的所有电脑都会失去访问权限，包括你自己的电脑：它们需要重新获得允许。"
    const val CONFIRM_REMOTE = "Tellyscope 会立即重新连接。请把电视遥控器放在手边：电视会请你允许这部手机。"
    const val CONFIRM_BUTTON = "移除访问"
    const val CANCEL = "取消"
    const val REVOKING = "正在移除访问…"
    const val ALLOW_TITLE = "在电视上允许这部手机"
    const val ALLOW_TEXT_A = "其他访问已移除。电视现在会问“是否允许 USB 调试？”。请勾选 "
    const val ALLOW_ALWAYS = "始终允许这台电脑"
    const val ALLOW_TEXT_B = "，然后选择 "
    const val ALLOW_BUTTON = "允许"
    const val FINGERPRINT_LABEL = "电视上应显示这个指纹"
    const val FINGERPRINT_HINT = "如果显示的不同，请在电视上选择“取消”。"
    const val STOP_WAITING = "停止等待"
    const val REFUSED = "电视没有允许这部手机。可能已超时，或选择了“取消”。"
    const val ASK_AGAIN = "再次询问电视"
    const val CHECKING = "正在检查电视…"
    const val DONE_TITLE = "只有这部手机有访问权限"
    const val DONE_TEXT = "Tellyscope 已再次检查电视：这部手机是唯一获得授权的电脑，且没有其他设备连接。"
    const val TRUST_WARNING = "只允许你信任的电脑和应用。任何获得允许的对象都可以安装应用、读取文件、控制电视。"
    fun backTo(tv: String) = "回到 $tv"
    const val STILL_TITLE = "仍有另一台电脑连接着"
    fun stillText(who: String, tv: String) =
        "Tellyscope still sees $who on $tv. It may have connected again straight away. Turn it off or unplug it from the network, then try again."
    fun stillOpenText(who: String, tv: String) =
        "Tellyscope still sees $who on $tv. Its access is gone, but the TV keeps a connection open until it ends. Restart the TV to end it."
    const val CHECK_AGAIN = "再次检查"
    const val RESTART_TV = "重启电视"
    const val RESTARTING_TO_CHECK = "正在重启电视…"
    fun connectionFrom(address: String) = "a connection from $address"
    const val TRY_AGAIN = "再试一次"
    const val UNCONFIRMED_TITLE = "已在电视上撤销"
    const val UNCONFIRMED_TEXT = "Tellyscope 无法在这台电视上检查清单。请在“开发者选项”里确认。"
    const val DONE = "完成"
    const val MANUAL_INTRO = "This TV didn't take Tellyscope's request, so it's two steps on the TV. Afterwards only this phone can use ADB on it; your own computers will need allowing again."
    const val MANUAL_STEP_1 = "在电视上打开“开发者选项”"
    const val MANUAL_STEP_1_TEXT = "Tellyscope opens it for you. If nothing happens on the TV, go to Settings → Device Preferences → Developer options."
    const val OPEN_ON_TV = "在电视上打开"
    const val MANUAL_STEP_2 = "Choose “Revoke USB debugging authorisations”"
    const val MANUAL_STEP_2_TEXT = "Then come back here, and Tellyscope asks the TV to allow this phone again."
    const val MANUAL_DONE = "我已经撤销了"
    const val DEV_OPTIONS_FAILED = "Couldn't open Developer options on the TV. Go to Settings → Device Preferences → Developer options."

    // Connected devices
    const val DEVICES_TITLE = "已连接的设备"
    fun showAll(n: Int) = "Show all ($n)"
    const val SHOW_LESS = "收起"
    const val NO_DEVICES = "没有接入或配对的设备。"
    fun group(g: Peripheral.Group) = when (g) {
        Peripheral.Group.Bluetooth -> "蓝牙"
        Peripheral.Group.Input -> "输入设备"
        Peripheral.Group.Storage -> "USB 存储"
        Peripheral.Group.Hdmi -> "HDMI-CEC"
        Peripheral.Group.Audio -> "音频输出"
    }
    /** The row's right-hand words. Collapsed rows have no group label, so Bluetooth says so there. */
    fun peripheralStatus(p: Peripheral, grouped: Boolean): String {
        val status = p.free?.let { "${bytes(it).replace(".0 ", " ")} free" } ?: p.status
        return if (!grouped && p.group == Peripheral.Group.Bluetooth) "Bluetooth · ${status.lowercase()}" else status
    }
}
