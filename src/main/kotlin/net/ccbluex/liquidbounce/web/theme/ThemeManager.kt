/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2024 CCBlueX
 *
 * LiquidBounce is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * LiquidBounce is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LiquidBounce. If not, see <https://www.gnu.org/licenses/>.
 *
 */
package net.ccbluex.liquidbounce.web.theme

import com.google.gson.JsonArray
import com.google.gson.annotations.SerializedName
import net.ccbluex.liquidbounce.config.ConfigSystem
import net.ccbluex.liquidbounce.config.util.decode
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleHud
import net.ccbluex.liquidbounce.utils.client.logger
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.io.extractZip
import net.ccbluex.liquidbounce.utils.io.resource
import net.ccbluex.liquidbounce.web.browser.BrowserManager
import net.ccbluex.liquidbounce.web.browser.supports.tab.ITab
import net.ccbluex.liquidbounce.web.integration.IntegrationHandler
import net.ccbluex.liquidbounce.web.integration.VirtualScreenType
import net.ccbluex.liquidbounce.web.socket.netty.NettyServer.Companion.NETTY_ROOT
import net.ccbluex.liquidbounce.web.theme.component.Component
import net.ccbluex.liquidbounce.web.theme.component.ComponentOverlay
import net.ccbluex.liquidbounce.web.theme.component.ComponentType
import net.minecraft.client.gui.screen.ChatScreen
import java.io.File

object ThemeManager {

    internal val themesFolder = File(ConfigSystem.rootFolder, "themes")
    internal val defaultTheme = Theme.defaults()

    var activeTheme = defaultTheme
        set(value) {
            if (!value.exists) {
                logger.warn("Unable to set theme to ${value.name}, theme does not exist")
                return
            }

            field = value

            // Update components
            ComponentOverlay.insertComponents()

            // Update integration browser
            IntegrationHandler.updateIntegrationBrowser()
            ModuleHud.refresh()
        }

    private val takesInputHandler: () -> Boolean
        get() = { mc.currentScreen != null && mc.currentScreen !is ChatScreen }

    fun openImmediate(virtualScreenType: VirtualScreenType? = null, markAsStatic: Boolean = false): ITab =
        BrowserManager.browser?.createTab(route(virtualScreenType, markAsStatic).url)
            ?: error("Browser is not initialized")

    fun openInputAwareImmediate(virtualScreenType: VirtualScreenType? = null, markAsStatic: Boolean = false): ITab =
        BrowserManager.browser?.createInputAwareTab(route(virtualScreenType, markAsStatic).url, takesInputHandler)
            ?: error("Browser is not initialized")

    fun updateImmediate(tab: ITab?, virtualScreenType: VirtualScreenType? = null, markAsStatic: Boolean = false) =
        tab?.loadUrl(route(virtualScreenType, markAsStatic).url)

    fun route(virtualScreenType: VirtualScreenType? = null, markAsStatic: Boolean = false): Route {
        val theme = if (virtualScreenType == null || activeTheme.doesAccept(virtualScreenType.routeName)) {
            activeTheme
        } else if (defaultTheme.doesAccept(virtualScreenType.routeName)) {
            defaultTheme
        } else {
            error("No theme supports the route ${virtualScreenType.routeName}")
        }

        return Route(
            theme,
            theme.getUrl(virtualScreenType?.routeName, markAsStatic)
        )
    }

    data class Route(val theme: Theme, val url: String)

}

class Theme(val name: String) {

    val folder = File(ThemeManager.themesFolder, name)
    val metadata: ThemeMetadata = run {
        val metadataFile = File(folder, "metadata.json")
        if (!metadataFile.exists()) {
            error("Theme $name does not contain a metadata file")
        }

        decode<ThemeMetadata>(metadataFile.readText())
    }

    val exists: Boolean
        get() = folder.exists()

    private val url: String
        get() = "$NETTY_ROOT/$name/#/"



    /**
     * Get the URL to the given page name in the theme.
     */
    fun getUrl(name: String? = null, markAsStatic: Boolean = false) = "$url${name.orEmpty()}".let {
        if (markAsStatic) {
            "$it?static"
        } else {
            it
        }
    }

    fun doesAccept(name: String?) = doesSupport(name) || doesOverlay(name)

    fun doesSupport(name: String?) = name != null && metadata.supports.contains(name)

    fun doesOverlay(name: String?) = name != null && metadata.overlays.contains(name)

    fun parseComponents(): MutableList<Component> {
        val themeComponent = metadata.rawComponents
            .map { it.asJsonObject }
            .associateBy { it["name"].asString!! }

        val componentList = mutableListOf<Component>()

        for ((name, obj) in themeComponent) {
            runCatching {
                val componentType = ComponentType.byName(name) ?: error("Unknown component type: $name")
                val component = componentType.createComponent()

                runCatching {
                    ConfigSystem.deserializeConfigurable(component, obj)
                }.onFailure {
                    logger.error("Failed to deserialize component $name", it)
                }

                componentList.add(component)
            }.onFailure {
                logger.error("Failed to create component $name", it)
            }
        }

        return componentList
    }

    companion object {

        fun defaults() = runCatching {
            val folder = ThemeManager.themesFolder.resolve("default")
            val stream = resource("/assets/liquidbounce/default_theme.zip")

            if (folder.exists()) {
                folder.deleteRecursively()
            }

            extractZip(stream, folder)
            folder.deleteOnExit()

            Theme("default")
        }.onFailure {
            logger.error("Unable to extract default theme", it)
        }.onSuccess {
            logger.info("Successfully extracted default theme")
        }.getOrThrow()

    }

}

data class ThemeMetadata(
    val name: String,
    val author: String,
    val version: String,
    val supports: List<String>,
    val overlays: List<String>,
    @SerializedName("components")
    val rawComponents: JsonArray
) {



}
