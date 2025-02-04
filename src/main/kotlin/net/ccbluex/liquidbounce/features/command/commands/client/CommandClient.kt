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
 */
package net.ccbluex.liquidbounce.features.command.commands.client

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.config.ConfigSystem
import net.ccbluex.liquidbounce.features.command.builder.CommandBuilder
import net.ccbluex.liquidbounce.features.command.builder.ParameterBuilder
import net.ccbluex.liquidbounce.lang.LanguageManager
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.client.regular
import net.ccbluex.liquidbounce.utils.client.variable
import net.ccbluex.liquidbounce.web.integration.BrowserScreen
import net.ccbluex.liquidbounce.web.integration.IntegrationHandler
import net.ccbluex.liquidbounce.web.integration.IntegrationHandler.clientJcef
import net.ccbluex.liquidbounce.web.integration.VirtualScreenType
import net.ccbluex.liquidbounce.web.theme.Theme
import net.ccbluex.liquidbounce.web.theme.ThemeManager
import net.ccbluex.liquidbounce.web.theme.component.ComponentOverlay
import net.ccbluex.liquidbounce.web.theme.component.components
import net.ccbluex.liquidbounce.web.theme.component.types.FrameComponent
import net.ccbluex.liquidbounce.web.theme.component.types.HtmlComponent
import net.ccbluex.liquidbounce.web.theme.component.types.ImageComponent
import net.ccbluex.liquidbounce.web.theme.component.types.TextComponent
import net.minecraft.text.ClickEvent
import net.minecraft.text.HoverEvent

/**
 * Client Command
 *
 * Provides subcommands for client management.
 */
object CommandClient {

    /**
     * Creates client command with a variety of subcommands.
     */
    fun createCommand() = CommandBuilder.begin("client")
        .hub()
        .subcommand(infoCommand())
        .subcommand(browserCommand())
        .subcommand(integrationCommand())
        .subcommand(languageCommand())
        .subcommand(themeCommand())
        .subcommand(componentCommand())
        .build()

    private fun infoCommand() = CommandBuilder
        .begin("info")
        .handler { command, _ ->
            chat(regular(command.result("clientName", variable(LiquidBounce.CLIENT_NAME))),
                prefix = false)
            chat(regular(command.result("clientVersion", variable(LiquidBounce.clientVersion))),
                prefix = false)
            chat(regular(command.result("clientAuthor", variable(LiquidBounce.CLIENT_AUTHOR))),
                prefix = false)
        }.build()

    private fun browserCommand() = CommandBuilder.begin("browser")
        .hub()
        .subcommand(
            CommandBuilder.begin("open")
                .parameter(
                    ParameterBuilder.begin<String>("name")
                        .verifiedBy(ParameterBuilder.STRING_VALIDATOR).required()
                        .build()
                ).handler { command, args ->
                    chat(regular("Opening browser..."))
                    mc.setScreen(BrowserScreen(args[0] as String))
                }.build()
        )
        .build()

    private fun integrationCommand() = CommandBuilder.begin("integration")
        .hub()
        .subcommand(CommandBuilder.begin("menu")
            .alias("url")
            .handler { command, args ->
                chat(variable("Client Integration"))
                val baseUrl = ThemeManager.route().url

                chat(
                    regular("Base URL: ")
                        .append(variable(baseUrl).styled {
                            it.withUnderline(true)
                                .withClickEvent(ClickEvent(ClickEvent.Action.OPEN_URL, baseUrl))
                                .withHoverEvent(
                                    HoverEvent(
                                        HoverEvent.Action.SHOW_TEXT,
                                        regular("Click to open the integration URL in your browser.")
                                    )
                                )
                        }),
                    prefix = false
                )

                chat(prefix = false)
                chat(regular("Integration Menu:"))
                for (screenType in VirtualScreenType.entries) {
                    val url = runCatching {
                        ThemeManager.route(screenType, true)
                    }.getOrNull()?.url ?: continue
                    val upperFirstName = screenType.routeName.replaceFirstChar { it.uppercase() }

                    chat(
                        regular("-> $upperFirstName (")
                            .append(variable("Browser").styled {
                                it.withUnderline(true)
                                    .withClickEvent(ClickEvent(ClickEvent.Action.OPEN_URL, url))
                                    .withHoverEvent(
                                        HoverEvent(
                                            HoverEvent.Action.SHOW_TEXT,
                                            regular("Click to open the URL in your browser.")
                                        )
                                    )
                            })
                            .append(regular(", "))
                            .append(variable("Clipboard").styled {
                                it.withUnderline(true)
                                    .withClickEvent(ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, url))
                                    .withHoverEvent(
                                        HoverEvent(
                                            HoverEvent.Action.SHOW_TEXT,
                                            regular("Click to copy the URL to your clipboard.")
                                        )
                                    )
                            })
                            .append(regular(")")),
                        prefix = false
                    )
                }

                chat(variable("Hint: You can also access the integration from another device.")
                    .styled { it.withItalic(true) })
            }.build()
        )
        .subcommand(CommandBuilder.begin("override")
            .parameter(
                ParameterBuilder.begin<String>("name")
                    .verifiedBy(ParameterBuilder.STRING_VALIDATOR).required()
                    .build()
            ).handler { command, args ->
                chat(regular("Overrides client JCEF browser..."))
                clientJcef?.loadUrl(args[0] as String)
            }.build()
        ).subcommand(CommandBuilder.begin("reset")
            .handler { command, args ->
                chat(regular("Resetting client JCEF browser..."))
                IntegrationHandler.updateIntegrationBrowser()
            }.build()
        )
        .build()

    private fun languageCommand() = CommandBuilder.begin("language")
        .hub()
        .subcommand(CommandBuilder.begin("list")
            .handler { command, args ->
                chat(regular("Available languages:"))
                for (language in LanguageManager.knownLanguages) {
                    chat(regular("-> $language"))
                }
            }.build()
        )
        .subcommand(CommandBuilder.begin("set")
            .parameter(
                ParameterBuilder.begin<String>("language")
                    .verifiedBy(ParameterBuilder.STRING_VALIDATOR).required()
                    .build()
            ).handler { command, args ->
                val language = LanguageManager.knownLanguages.find { it.equals(args[0] as String, true) }
                if (language == null) {
                    chat(regular("Language not found."))
                    return@handler
                }

                chat(regular("Setting language to ${language}..."))
                LanguageManager.overrideLanguage = language

                ConfigSystem.storeConfigurable(LanguageManager)
            }.build()
        )
        .subcommand(CommandBuilder.begin("unset")
            .handler { command, args ->
                chat(regular("Unset override language..."))
                LanguageManager.overrideLanguage = ""
                ConfigSystem.storeConfigurable(LanguageManager)
            }.build()
        )
        .build()

    private fun themeCommand() = CommandBuilder.begin("theme")
        .hub()
        .subcommand(CommandBuilder.begin("list")
            .handler { command, args ->
                chat(regular("Available themes:"))
                for (theme in ThemeManager.themesFolder.listFiles()!!) {
                    chat(regular("-> ${theme.name}"))
                }
            }.build()
        )
        .subcommand(CommandBuilder.begin("set")
            .parameter(
                ParameterBuilder.begin<String>("theme")
                    .verifiedBy(ParameterBuilder.STRING_VALIDATOR).required()
                    .build()
            ).handler { command, args ->
                val theme = ThemeManager.themesFolder.listFiles()?.find {
                    it.name.equals(args[0] as String, true)
                }

                if (theme == null) {
                    chat(regular("Theme not found."))
                    return@handler
                }

                chat(regular("Setting theme to ${theme.name}..."))
                ThemeManager.activeTheme = Theme(theme.name)
            }.build()
        )
        .subcommand(CommandBuilder.begin("unset")
            .handler { command, args ->
                chat(regular("Unset active theme..."))
                ThemeManager.activeTheme = ThemeManager.defaultTheme
            }.build()
        )
        .build()

    fun componentCommand() = CommandBuilder.begin("component")
        .hub()
        .subcommand(CommandBuilder.begin("list")
            .handler { command, args ->
                chat(regular("Components:"))
                for (component in components) {
                    chat(regular("-> ${component.name}"))
                }
            }.build()
        )
        .subcommand(CommandBuilder.begin("add")
            .hub()
            .subcommand(CommandBuilder.begin("text")
                .parameter(
                    ParameterBuilder.begin<String>("text")
                        .vararg()
                        .verifiedBy(ParameterBuilder.STRING_VALIDATOR).required()
                        .build()
                ).handler { command, args ->
                    val arg = (args[0] as Array<*>).joinToString(" ") { it as String }
                    components += TextComponent(arg)
                    ComponentOverlay.fireComponentsUpdate()

                    chat("Successfully added text component.")
                }.build()
            )
            .subcommand(CommandBuilder.begin("frame")
                .parameter(
                    ParameterBuilder.begin<String>("url")
                        .vararg()
                        .verifiedBy(ParameterBuilder.STRING_VALIDATOR).required()
                        .build()
                ).handler { command, args ->
                    val arg = (args[0] as Array<*>).joinToString(" ") { it as String }
                    components += FrameComponent(arg)
                    ComponentOverlay.fireComponentsUpdate()

                    chat("Successfully added frame component.")
                }.build()
            )
            .subcommand(CommandBuilder.begin("image")
                .parameter(
                    ParameterBuilder.begin<String>("url")
                        .vararg()
                        .verifiedBy(ParameterBuilder.STRING_VALIDATOR).required()
                        .build()
                ).handler { command, args ->
                    val arg = (args[0] as Array<*>).joinToString(" ") { it as String }
                    components += ImageComponent(arg)
                    ComponentOverlay.fireComponentsUpdate()

                    chat("Successfully added image component.")
                }.build()
            )
            .subcommand(CommandBuilder.begin("html")
                .parameter(
                    ParameterBuilder.begin<String>("code")
                        .vararg()
                        .verifiedBy(ParameterBuilder.STRING_VALIDATOR).required()
                        .build()
                ).handler { command, args ->
                    val arg = (args[0] as Array<*>).joinToString(" ") { it as String }
                    components += HtmlComponent(arg)
                    ComponentOverlay.fireComponentsUpdate()

                    chat("Successfully added html component.")
                }.build()
            ).build()
        )
        .subcommand(CommandBuilder.begin("remove")
            .parameter(
                ParameterBuilder.begin<String>("name")
                    .verifiedBy(ParameterBuilder.STRING_VALIDATOR).required()
                    .build()
            ).handler { command, args ->
                val name = args[0] as String
                val component = components.find { it.name.equals(name, true) }

                if (component == null) {
                    chat(regular("Component not found."))
                    return@handler
                }

                components -= component
                ComponentOverlay.fireComponentsUpdate()

                chat("Successfully removed component.")
            }.build()
        )
        .subcommand(CommandBuilder.begin("clear")
            .handler { command, args ->
                components.clear()
                ComponentOverlay.fireComponentsUpdate()

                chat("Successfully cleared components.")
            }.build()
        )
        .subcommand(CommandBuilder.begin("update")
            .handler { command, args ->
                ComponentOverlay.fireComponentsUpdate()

                chat("Successfully updated components.")
            }.build()
        )
        .build()

}
