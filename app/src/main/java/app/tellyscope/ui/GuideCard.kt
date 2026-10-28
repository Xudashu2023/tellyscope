// SPDX-License-Identifier: GPL-3.0-only

package app.tellyscope.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp

/**
 * How to switch on debugging, per brand, because the menu names differ. [pair] is where the
 * pairing code lives on TVs with Wireless debugging; null where the brand only has the older switch.
 * `**…**` marks the words to find on the TV.
 */
class TvBrand(val name: String, val unlock: String, val debugTitle: String, val debug: String, val pair: String?)

private const val ANDROID_TV_PAIR = "**设备偏好设置 → 开发者选项 → 无线调试 → 使用配对码配对设备**"

val brands = listOf(
    TvBrand(
        "Google TV",
        unlock = "在电视上：**设置 → 系统 → 关于**。在 **Android TV OS 版本** 上按 OK 7 次，直到提示你已成为开发者。",
        debugTitle = "开启无线调试",
        debug = "回到 **系统 → 开发者选项**。开启 **无线调试**。较旧电视叫 **USB 调试**。",
        pair = "**系统 → 开发者选项 → 无线调试 → 使用配对码配对设备**",
    ),
    TvBrand(
        "Android TV",
        unlock = "在电视上：**设置 → 设备偏好设置 → 关于**。在 **版本** 上按 OK 7 次。",
        debugTitle = "开启无线调试",
        debug = "回到 **设备偏好设置 → 开发者选项**。开启 **无线调试**。较旧电视改用 **USB 调试** 和 **网络调试**：两个都开启。",
        pair = ANDROID_TV_PAIR,
    ),
    TvBrand(
        "Fire TV",
        unlock = "在电视上：**设置 → 我的 Fire TV → 关于**。在第一行（你的 Fire TV 名字）上按 OK 7 次。",
        debugTitle = "开启 ADB 调试",
        debug = "回到 **我的 Fire TV → 开发者选项**。开启 **ADB 调试**。",
        pair = null,
    ),
    TvBrand(
        "Xiaomi",
        unlock = "在电视上：**设置 → 设备偏好设置 → 关于**。在 **版本** 上按 OK 7 次。",
        debugTitle = "开启无线调试",
        debug = "回到 **设备偏好设置 → 开发者选项**。开启 **无线调试**。较旧电视叫 **USB 调试**。",
        pair = ANDROID_TV_PAIR,
    ),
    TvBrand(
        "海信 VIDDA",
        unlock = "在电视上：**设置 → 关于**。连点 **版本号** 7 次。（不同固件菜单名略有出入）",
        debugTitle = "开启无线调试",
        debug = "回到 **开发者选项**。开启 **无线调试**。较旧机型叫 **USB 调试** 和 **网络调试**：两个都开启。",
        pair = "**开发者选项 → 无线调试 → 使用配对码配对设备**",
    ),
    TvBrand(
        "XGIMI",
        unlock = "在电视上：**设置 → 设备偏好设置 → 关于**。在 **版本** 上按 OK 7 次。",
        debugTitle = "开启无线调试",
        debug = "回到 **设备偏好设置 → 开发者选项**。开启 **无线调试**。较旧投影仪有 **USB 调试**，如有 **网络调试** 也一并开启。",
        pair = ANDROID_TV_PAIR,
    ),
    TvBrand(
        "其他",
        unlock = "在电视上打开 **设置**。有 **系统**？那是 Google TV：去 **系统 → 关于**，在 **Android TV OS 版本** 上按 OK 7 次。有 **设备偏好设置**？那是 Android TV：去 **设备偏好设置 → 关于**，在 **版本** 上按 OK 7 次。",
        debugTitle = "开启无线调试",
        debug = "退回上一级，打开 **开发者选项**。开启 **无线调试**。较旧电视有 **USB 调试**，有些还有 **网络调试**：有的都开启。",
        pair = "**开发者选项 → 无线调试 → 使用配对码配对设备**",
    ),
)

/** The brand chips: every brand stays visible, so nobody has to guess what a menu hides. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BrandChips(brand: TvBrand, onPick: (TvBrand) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("选择你的电视", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            brands.forEach { b ->
                FilterChip(
                    selected = b == brand,
                    onClick = { onPick(b) },
                    label = { Text(b.name) },
                    leadingIcon = if (b == brand) ({ Icon(Icons.Default.Check, null, Modifier.size(18.dp)) }) else null,
                )
            }
        }
    }
}

/**
 * One step open at a time: "完成，下一步" ticks it off and opens the next, "返回" reopens the one
 * before. The last step holds [findTvs], which is the live scan and whatever it found.
 */
@Composable
fun GuideSteps(brand: TvBrand, step: Int, onStep: (Int) -> Unit, findTvs: @Composable () -> Unit) {
    val steps = listOf(
        Triple("解锁开发者选项", brand.unlock, "开发者选项已解锁"),
        Triple(brand.debugTitle, brand.debug, brand.debugTitle.removePrefix("Turn on ") + " on"),
        Triple("找到你的电视", "", ""),
    )
    Column(Modifier.animateContentSize(MaterialTheme.motionScheme.defaultSpatialSpec())) {
        steps.forEachIndexed { i, (title, body, done) ->
            val last = i == steps.lastIndex
            Row(Modifier.height(IntrinsicSize.Min)) {
                Column(Modifier.width(32.dp).fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally) {
                    StepDot(i + 1, done = i < step, current = i == step)
                    if (!last) Box(Modifier.padding(vertical = 4.dp).width(2.dp).weight(1f).background(MaterialTheme.colorScheme.outlineVariant))
                }
                Column(
                    Modifier.padding(start = 16.dp, top = 4.dp, bottom = if (last) 8.dp else 24.dp).weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        title,
                        style = MaterialTheme.typography.titleMedium,
                        color = if (i > step) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurface,
                    )
                    when {
                        i < step -> Text(done, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        i == step && last -> findTvs()
                        i == step -> {
                            Text(marked(body), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(onClick = { onStep(i + 1) }) { Text("完成，下一步") }
                                if (i > 0) TextButton(onClick = { onStep(i - 1) }) { Text("返回") }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StepDot(n: Int, done: Boolean, current: Boolean) {
    val c = MaterialTheme.colorScheme
    val filled = done || current
    Box(
        Modifier.size(32.dp)
            .background(if (filled) c.primary else c.surface, CircleShape)
            .border(2.dp, if (filled) c.primary else c.outlineVariant, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        if (done) Icon(Icons.Default.Check, contentDescription = "已完成", Modifier.size(18.dp), tint = c.onPrimary)
        else Text("$n", style = MaterialTheme.typography.labelLarge, color = if (current) c.onPrimary else c.outline)
    }
}

/** "找不到电视？": the fixes that used to sit under the guide as small print. */
@Composable
fun HelpDialog(brand: TvBrand, onAddByIp: () -> Unit, onDismiss: () -> Unit) {
    val tips = buildList {
        add("让手机和电视保持在同一 Wi-Fi。访客网络常常会把设备隔开。")
        add("调试自己关掉了？有些电视重启后会重置。请重新打开，本屏会再次找到电视。")
        if (brand.name == "Fire TV") add("按 7 次后还是没有开发者选项？较新的 Fire TV 运行 Vega OS，不是 Android，Tellyscope 连不上它们。")
        add("还是没有？请从电视的网络设置里用 IP 地址添加电视。")
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("找不到电视？") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                tips.forEach { Text(it, style = MaterialTheme.typography.bodyMedium) }
            }
        },
        confirmButton = { Button(onClick = { onDismiss(); onAddByIp() }) { Text("用 IP 地址添加") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("关闭") } },
    )
}

/** Turns `**words**` into medium-weight spans: the menu names to look for on the TV. */
@Composable
fun marked(text: String): AnnotatedString {
    val strong = SpanStyle(fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
    return buildAnnotatedString {
        text.split("**").forEachIndexed { i, part -> if (i % 2 == 1) withStyle(strong) { append(part) } else append(part) }
    }
}
