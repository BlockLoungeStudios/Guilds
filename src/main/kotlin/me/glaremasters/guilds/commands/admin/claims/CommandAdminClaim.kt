/*
 * MIT License
 *
 * Copyright (c) 2019 Glare
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package me.glaremasters.guilds.commands.admin.claims

import ch.jalu.configme.SettingsManager
import co.aikar.commands.ACFBukkitUtil
import co.aikar.commands.BaseCommand
import co.aikar.commands.annotation.*
import me.glaremasters.guilds.Guilds
import me.glaremasters.guilds.claim.ClaimPermissions
import me.glaremasters.guilds.claim.ClaimProximity
import me.glaremasters.guilds.claim.ClaimRegionHandler
import me.glaremasters.guilds.exceptions.ExpectationNotMet
import me.glaremasters.guilds.guild.Guild
import me.glaremasters.guilds.guild.GuildHandler
import me.glaremasters.guilds.messages.Messages
import me.glaremasters.guilds.claim.ClaimUtils
import me.glaremasters.guilds.utils.Constants
import org.bukkit.entity.Player
import org.codemc.worldguardwrapper.WorldGuardWrapper

@CommandAlias("%guilds")
internal class CommandAdminClaim : BaseCommand() {
    @Dependency lateinit var guilds: Guilds
    @Dependency lateinit var guildHandler: GuildHandler
    @Dependency lateinit var settingsManager: SettingsManager

    @Subcommand("admin claim")
    @Description("{@@descriptions.admin-claim}")
    @CommandPermission(Constants.ADMIN_PERM)
    @CommandCompletion("@guilds")
    @Syntax("<%syntax>")
    fun claim(player: Player, @Flags("other") @Values("@guilds") guild: Guild) {
        if (!ClaimUtils.isEnable(settingsManager)) {
            throw ExpectationNotMet(Messages.CLAIM__HOOK_DISABLED)
        }

        val wrapper = WorldGuardWrapper.getInstance()

        val claim = ClaimRegionHandler.createClaim(wrapper, guild, player)
        guild.addGuildClaim(claim)

        ClaimPermissions.addOwner(wrapper, claim, guild)
        ClaimPermissions.addMembers(wrapper, claim, guild)
        ClaimPermissions.setEnterMessage(wrapper, claim, settingsManager, guild)
        ClaimPermissions.setExitMessage(wrapper, claim, settingsManager, guild)

        currentCommandIssuer.sendInfo(Messages.CLAIM__SUCCESS,
                "{loc1}", ACFBukkitUtil.formatLocation(ClaimUtils.claimPointOne(player)),
                "{loc2}", ACFBukkitUtil.formatLocation(ClaimUtils.claimPointTwo(player)))
    }

    @Subcommand("admin unclaim")
    @Description("{@@descriptions.admin-unclaim}")
    @CommandPermission(Constants.ADMIN_PERM)
    @CommandCompletion("@guilds @claimed")
    @Syntax("<%syntax> <claim>")
    fun unclaim(player: Player, @Flags("other") @Values("@guilds") guild: Guild, @Values("@claimed") @Single option: String) {
        if (!ClaimUtils.isEnable(settingsManager)) {
            throw ExpectationNotMet(Messages.CLAIM__HOOK_DISABLED)
        }

        val wrapper = WorldGuardWrapper.getInstance()

        if (!ClaimUtils.checkIfHaveClaims(wrapper, guild)) {
            throw (ExpectationNotMet(Messages.UNCLAIM__NOT_FOUND))
        }

        when (option) {
            "all" -> {
                ClaimRegionHandler.removeAllClaims(wrapper, guild)
                guild.clearGuildClaims()
                currentCommandIssuer.sendInfo(Messages.UNCLAIM__SUCCESS)
            }
            "this" -> {
                val standingClaim = ClaimUtils.getStandingOnClaim(wrapper, player, guild)
                if (standingClaim != null) {
                    ClaimRegionHandler.removeClaim(wrapper, standingClaim)
                    guild.removeGuildClaim(standingClaim)
                    currentCommandIssuer.sendInfo(Messages.UNCLAIM__SUCCESS)
                }
                else {
                    currentCommandIssuer.sendInfo(Messages.UNCLAIM__NOT_FOUND)
                }
            }
            else -> {
                currentCommandIssuer.sendInfo(Messages.UNCLAIM__NOT_FOUND)
            }
        }
    }
}
