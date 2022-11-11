package tororo1066.man10realestatev2.listeners

import org.bukkit.Bukkit
import org.bukkit.NamespacedKey
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.persistence.PersistentDataType
import tororo1066.man10realestatev2.Man10RealEstateV2
import tororo1066.man10realestatev2.Man10RealEstateV2.Companion.sendPrefixMsg
import tororo1066.tororopluginapi.SStr
import tororo1066.tororopluginapi.annotation.SEventHandler
import tororo1066.tororopluginapi.utils.LocType
import tororo1066.tororopluginapi.utils.toLocString

class WandListener {

    @SEventHandler
    fun interact(e: PlayerInteractEvent){
        if (!e.hasItem())return
        if (e.hand == EquipmentSlot.OFF_HAND)return
        val item = e.item!!
        if (!item.itemMeta.persistentDataContainer.has(NamespacedKey(Man10RealEstateV2.plugin,"wand"),
                PersistentDataType.INTEGER))return
        e.isCancelled = true

        if (e.action.isRightClick && e.hasBlock()){
            val locStr = e.clickedBlock!!.location.toLocString(LocType.BLOCK_COMMA)
            if (item.itemMeta.lore.isNullOrEmpty()){
                val meta = item.itemMeta
                meta.persistentDataContainer.set(
                    NamespacedKey(Man10RealEstateV2.plugin,"firstloc"),
                    PersistentDataType.STRING,locStr)
                val lore = meta.lore()?: mutableListOf()
                lore.add(SStr("§d始点.${locStr}").toTextComponent())
                meta.lore(lore)
                item.itemMeta = meta
                e.player.sendPrefixMsg(SStr("&a始点を${locStr}にしたよ～"))
            } else {
                if (item.itemMeta.lore?.size == 1){
                    val meta = item.itemMeta
                    meta.persistentDataContainer.set(
                        NamespacedKey(Man10RealEstateV2.plugin,"secondloc"),
                        PersistentDataType.STRING,locStr)
                    val lore = meta.lore()?: mutableListOf()
                    lore.add(SStr("§d終点.${locStr}").toTextComponent())
                    meta.lore(lore)
                    item.itemMeta = meta
                    e.player.sendPrefixMsg(SStr("&a終点を${locStr}にしたよ～"))
                }
            }
        }
        if (e.action.isLeftClick){
            val meta = item.itemMeta
            meta.lore(null)
            meta.persistentDataContainer.remove(NamespacedKey(Man10RealEstateV2.plugin,"firstloc"))
            meta.persistentDataContainer.remove(NamespacedKey(Man10RealEstateV2.plugin,"secondloc"))
            item.itemMeta = meta
            e.player.sendPrefixMsg(SStr("&aけしたよ"))
        }

    }
}