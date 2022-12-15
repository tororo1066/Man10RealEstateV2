package tororo1066.man10realestatev2.listeners

import org.bukkit.Bukkit
import org.bukkit.DyeColor
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.block.Container
import org.bukkit.block.Sign
import org.bukkit.entity.Player
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.player.PlayerBucketEmptyEvent
import org.bukkit.event.player.PlayerFishEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.material.Colorable
import tororo1066.man10realestatev2.Man10RealEstateV2
import tororo1066.man10realestatev2.Man10RealEstateV2.Companion.sendPrefixMsg
import tororo1066.man10realestatev2.data.CityData
import tororo1066.man10realestatev2.data.RegionData
import tororo1066.man10realestatev2.data.UserData
import tororo1066.tororopluginapi.SStr
import tororo1066.tororopluginapi.annotation.SEventHandler

class RegionListener {

    @SEventHandler
    fun onBreak(e: BlockBreakEvent){
        val p = e.player
        if (!hasPermission(p,e.block.location,UserData.Perm.BLOCK)){
            p.sendPrefixMsg(SStr("&cこのブロックを壊す権限がありません！"))
            e.isCancelled = true
        }
    }

    @SEventHandler
    fun onPlace(e: BlockPlaceEvent){
        val p = e.player
        if (!hasPermission(p,e.block.location,UserData.Perm.BLOCK)){
            p.sendPrefixMsg(SStr("&cブロックを置く権限がありません！"))
            e.isCancelled = true
        }
    }

    @SEventHandler
    fun onBucket(e: PlayerBucketEmptyEvent){
        val p = e.player
        if (!hasPermission(p,e.block.location,UserData.Perm.BLOCK)){
            p.sendPrefixMsg(SStr("&cブロックを置く権限がありません！"))
            e.isCancelled = true
        }
    }

    @SEventHandler
    fun onInteract(e: PlayerInteractEvent){
        if (!e.hasBlock())return
        val block = e.clickedBlock!!
        if (block.state is Sign){
            if (!e.hasItem())return
            val item = e.item!!
            if (item.itemMeta !is Colorable && item.type != Material.GLOW_INK_SAC)return
            if (!hasPermission(e.player,block.location,UserData.Perm.BLOCK)){
                e.player.sendPrefixMsg(SStr("&c看板を染める権限がありません！"))
                e.isCancelled = true
            }
            return
        }
        val p = e.player
        if (!hasPermission(p,block.location,UserData.Perm.INTERACT)){
            p.sendPrefixMsg(SStr("&cブロックに触る権限がありません！"))
            e.isCancelled = true
            return
        }
        if (block.state is Container && !hasPermission(p,block.location,UserData.Perm.INVENTORY)){
            p.sendPrefixMsg(SStr("&cブロックに触る権限がありません！"))
            e.isCancelled = true
            return
        }
    }

    @SEventHandler
    fun onDamageEntity(e: EntityDamageByEntityEvent){

        val p = e.damager

        if (p !is Player || e.entity is Player)return

        if (!hasPermission(p, e.entity.location,UserData.Perm.INTERACT)){
            p.sendPrefixMsg(SStr("&cブロックに触る権限がありません！"))
            e.isCancelled = true
        }
    }

//    @SEventHandler
//    fun onFish(e: PlayerFishEvent){
//        if (e.state == PlayerFishEvent.State.FISHING){
//            if (CityData.findCity(e.player.location) != null && !e.player.hasPermission("mrev2.op")){
//                e.isCancelled = true
//            }
//        }
//    }



    private fun hasPermission(p: Player, loc: Location, perm: UserData.Perm): Boolean {
        if (p.hasPermission("mrev2.op"))return true
        val city = CityData.findCity(loc)?:return true
        city.regions.forEach { (_, data) ->
            if (Man10RealEstateV2.inRegion(loc,data.startLoc,data.endLoc)){
                if (data.state == RegionData.State.DANGER)return true
                if (data.state == RegionData.State.FREE && perm != UserData.Perm.BLOCK)return true
                if (data.ownerUUID == p.uniqueId)return true
                val userData = data.players[p.uniqueId]?:return false
                if (userData.perms.contains(UserData.Perm.ALL))return true
                return userData.perms.contains(perm)
            }
        }
        return false
    }
}