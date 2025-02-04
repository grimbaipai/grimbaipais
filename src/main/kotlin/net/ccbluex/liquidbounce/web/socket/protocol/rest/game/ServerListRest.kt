/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2024 CCBlueX
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
 *
 */

package net.ccbluex.liquidbounce.web.socket.protocol.rest.game

import com.google.common.util.concurrent.ThreadFactoryBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.mojang.blaze3d.systems.RenderSystem
import net.ccbluex.liquidbounce.config.util.decode
import net.ccbluex.liquidbounce.event.Listenable
import net.ccbluex.liquidbounce.event.events.GameTickEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.utils.client.logger
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.web.socket.netty.httpInternalServerError
import net.ccbluex.liquidbounce.web.socket.netty.httpOk
import net.ccbluex.liquidbounce.web.socket.netty.rest.RestNode
import net.ccbluex.liquidbounce.web.socket.protocol.protocolGson
import net.minecraft.client.gui.screen.TitleScreen
import net.minecraft.client.gui.screen.multiplayer.ConnectScreen
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen
import net.minecraft.client.network.MultiplayerServerListPinger
import net.minecraft.client.network.ServerAddress
import net.minecraft.client.network.ServerInfo
import net.minecraft.client.option.ServerList
import net.minecraft.screen.ScreenTexts
import net.minecraft.text.Text
import net.minecraft.util.Colors
import java.net.UnknownHostException
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.ThreadPoolExecutor

object ServerListRest : Listenable {

    private var serverList = ServerList(mc).apply { loadFile() }

    private val serverListPinger = MultiplayerServerListPinger()
    private val serverPingerThreadPool: ThreadPoolExecutor = ScheduledThreadPoolExecutor(
        10,
        ThreadFactoryBuilder().setNameFormat("Server Pinger #%d")
            .setDaemon(true)
            .build()
    )
    private val cannotConnectText = Text.translatable("multiplayer.status.cannot_connect")
        .withColor(Colors.RED)

    private fun pingThemAll() {
        serverList.toList()
            .distinctBy { it.address } // We do not want to ping the same server multiple times
            .forEach(this::ping)
    }

    fun ping(serverEntry: ServerInfo) {
        if (!serverEntry.online) {
            serverEntry.online = true
            serverEntry.ping = -2L
            serverEntry.label = ScreenTexts.EMPTY
            serverEntry.playerCountLabel = ScreenTexts.EMPTY

            serverPingerThreadPool.submit {
                try {
                    serverListPinger.add(serverEntry) {
                        mc.execute(serverList::saveFile)
                    }
                } catch (unknownHostException: UnknownHostException) {
                    serverEntry.ping = -1L
                    serverEntry.label = cannotConnectText
                    logger.error("Failed to ping server ${serverEntry.name} due to ${unknownHostException.message}")
                } catch (exception: Exception) {
                    serverEntry.ping = -1L
                    serverEntry.label = cannotConnectText
                    logger.error("Failed to ping server ${serverEntry.name}", exception)
                }
            }
        }
    }

    internal fun RestNode.serverListRest() {
        get("/servers") {
            serverList = ServerList(mc).apply { loadFile() }

            // We either hit Refresh or entered the page for the first time
            pingThemAll()

            val servers = JsonArray()
            runCatching {
                serverList.toList().forEachIndexed { index, serverInfo ->
                    val json = protocolGson.toJsonTree(serverInfo)

                    if (!json.isJsonObject) {
                        logger.warn("Failed to convert serverInfo to json")
                        return@forEachIndexed
                    }

                    val jsonObject = json.asJsonObject
                    jsonObject.addProperty("index", index)
                    servers.add(jsonObject)
                }

                httpOk(servers)
            }.getOrElse { httpInternalServerError("Failed to get servers due to ${it.message}") }
        }.apply {
            post("/connect") {
                data class ServerConnectRequest(val address: String)
                val serverConnectRequest = decode<ServerConnectRequest>(it.content)

                val serverInfo = serverList.getByAddress(serverConnectRequest.address)
                    ?: ServerInfo("Unknown Server", serverConnectRequest.address, ServerInfo.ServerType.OTHER)

                val serverAddress = ServerAddress.parse(serverInfo.address)

                RenderSystem.recordRenderCall {
                    ConnectScreen.connect(MultiplayerScreen(TitleScreen()), mc, serverAddress, serverInfo,
                        false)
                }
                httpOk(JsonObject())
            }

            put("/add") {
                data class ServerAddRequest(val name: String, val address: String)
                val serverAddRequest = decode<ServerAddRequest>(it.content)

                val serverInfo = ServerInfo(serverAddRequest.name, serverAddRequest.address,
                    ServerInfo.ServerType.OTHER)
                serverList.add(serverInfo, false)
                serverList.saveFile()

                httpOk(JsonObject())
            }

            delete("/remove") {
                data class ServerRemoveRequest(val index: Int)
                val serverRemoveRequest = decode<ServerRemoveRequest>(it.content)
                val serverInfo = serverList.get(serverRemoveRequest.index)

                serverList.remove(serverInfo)
                serverList.saveFile()

                httpOk(JsonObject())
            }

            put("/edit") {
                data class ServerEditRequest(val index: Int, val name: String, val address: String)
                val serverEditRequest = decode<ServerEditRequest>(it.content)
                val serverInfo = serverList.get(serverEditRequest.index)

                serverInfo.name = serverEditRequest.name
                serverInfo.address = serverEditRequest.address
                serverList.saveFile()

                httpOk(JsonObject())
            }

            post("/swap") {
                data class ServerSwapRequest(val from: Int, val to: Int)
                val serverSwapRequest = decode<ServerSwapRequest>(it.content)

                serverList.swapEntries(serverSwapRequest.from, serverSwapRequest.to)
                serverList.saveFile()
                httpOk(JsonObject())
            }

            post("/order") {
                data class ServerOrderRequest(val order: List<Int>)
                val serverOrderRequest = decode<ServerOrderRequest>(it.content)

                serverOrderRequest.order.map { serverList.get(it) }
                    .forEachIndexed { index, serverInfo ->
                        serverList.set(index, serverInfo)
                    }
                serverList.saveFile()

                httpOk(JsonObject())
            }
        }
    }

    val tickHandler = handler<GameTickEvent> {
        serverListPinger.tick()
    }

    override fun handleEvents() = true

}

fun ServerList.toList() = (0 until size()).map { get(it) }

fun ServerList.getByAddress(address: String) = toList().firstOrNull { it.address == address }
