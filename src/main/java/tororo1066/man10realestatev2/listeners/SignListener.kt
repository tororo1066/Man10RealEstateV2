package tororo1066.man10realestatev2.listeners

import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.block.BlockState
import org.bukkit.block.Chest
import org.bukkit.block.Sign
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.block.SignChangeEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.material.Colorable
import org.bukkit.persistence.PersistentDataType
import tororo1066.man10realestatev2.Man10RealEstateV2
import tororo1066.man10realestatev2.Man10RealEstateV2.Companion.sendPrefixMsg
import tororo1066.man10realestatev2.data.CityData
import tororo1066.man10realestatev2.data.RegionData
import tororo1066.tororopluginapi.SStr
import tororo1066.tororopluginapi.SStr.Companion.toSStr
import tororo1066.tororopluginapi.annotation.SEventHandler
import tororo1066.tororopluginapi.otherUtils.UsefulUtility

class SignListener {

    @SEventHandler
    fun clickSign(e: PlayerInteractEvent){
        if (!e.hasBlock())return
        val block = e.clickedBlock?:return
        val state = block.state
        if (state !is Sign)return
        if (!state.persistentDataContainer.has(NamespacedKey(Man10RealEstateV2.plugin,"region"), PersistentDataType.STRING))return
        val data = state.persistentDataContainer[NamespacedKey(Man10RealEstateV2.plugin,"region"), PersistentDataType.STRING]!!
        val city = CityData.findCity(block.location)?:return
        val region = city.regions[data]?:return

        updateSign(state,region)

        e.player.sendPrefixMsg(SStr("&a==========${region.displayName}&a&lの情報=========="))
        e.player.sendPrefixMsg(SStr("&aID:$data"))
        e.player.sendPrefixMsg(SStr("&aステータス:${region.state.displayName}"))
        e.player.sendPrefixMsg(SStr("&aオーナー:${if (region.ownerUUID != null) Bukkit.getOfflinePlayer(region.ownerUUID!!).name else "Admin"}"))
        e.player.sendPrefixMsg(SStr("&a値段:${UsefulUtility.doubleToFormatString(region.price)}"))
        e.player.sendPrefixMsg(SStr("&a=========================================="))

        if (region.state == RegionData.State.ONSALE){
            Man10RealEstateV2.sInput.clickAccept(e.player,"§a§l[土地を買う！]",{
                region.buyRegion(e.player)
            },{},60)
        }
    }

    @SEventHandler
    fun changeSign(e: SignChangeEvent){
        if (e.line(0) != null){
            val line = e.line(0)!!.toSStr().toString()
            if (line.contains("mrev2:")){
                val id = line.replace("mrev2:","")
                val city = CityData.findCity(e.block.location)?:return
                val region = city.regions[id]
                if (region == null){
                    e.player.sendPrefixMsg(SStr("&c土地のIDが存在しません！"))
                    return
                }
                if (!Man10RealEstateV2.inRegion(e.block.location,region.startLoc,region.endLoc)){
                    e.player.sendPrefixMsg(SStr("&c土地の中に看板を置いてください！"))
                    return
                }
                e.line(0,SStr("&eID:${region.includeName}").toTextComponent())
                e.line(1,SStr(region.displayName).toTextComponent())
                e.line(2,SStr("&d&l${if (region.ownerUUID != null) Bukkit.getOfflinePlayer(region.ownerUUID!!).name else "Admin"}").toTextComponent())
                e.line(3,SStr("&a&l${region.state.displayName}").toTextComponent())
                val state = e.block.state as Sign
                state.persistentDataContainer.set(NamespacedKey(Man10RealEstateV2.plugin,"region"), PersistentDataType.STRING,region.includeName)
                state.update()
            }
        }
    }

    private fun updateSign(state: Sign, region: RegionData){
        state.line(0,SStr("&eID:${region.includeName}").toTextComponent())
        state.line(1,SStr(region.displayName).toTextComponent())
        state.line(2,SStr("&d&l${if (region.ownerUUID != null) Bukkit.getOfflinePlayer(region.ownerUUID!!).name else "Admin"}").toTextComponent())
        state.line(3,SStr("&a&l${region.state.displayName}").toTextComponent())
        state.update()
    }
}