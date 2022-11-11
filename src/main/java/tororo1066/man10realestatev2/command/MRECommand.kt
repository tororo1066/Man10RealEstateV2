package tororo1066.man10realestatev2.command

import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.Sound
import org.bukkit.persistence.PersistentDataType
import tororo1066.man10realestatev2.Man10RealEstateV2
import tororo1066.man10realestatev2.Man10RealEstateV2.Companion.sendPrefixMsg
import tororo1066.man10realestatev2.data.CityData
import tororo1066.man10realestatev2.data.RegionData
import tororo1066.man10realestatev2.inventory.op.CityEdit
import tororo1066.man10realestatev2.inventory.op.RegionEdit
import tororo1066.man10realestatev2.inventory.user.MainMenu
import tororo1066.tororopluginapi.SJavaPlugin
import tororo1066.tororopluginapi.SStr
import tororo1066.tororopluginapi.annotation.SCommandBody
import tororo1066.tororopluginapi.sCommand.SCommand
import tororo1066.tororopluginapi.sCommand.SCommandArg
import tororo1066.tororopluginapi.sItem.SItem
import java.util.*

class MRECommand: SCommand("mrev2",Man10RealEstateV2.prefix,"mrev2.user") {

    @SCommandBody("mrev2.user")
    val menu = command().setPlayerExecutor {
        MainMenu().open(it.sender)
    }

    @SCommandBody("mrev2.user")
    val teleport = command().addArg(SCommandArg("tp")).addArg(SCommandArg(Man10RealEstateV2.cityData.values.map { it.regions.map { map -> map.key } }.stream().flatMap { it.stream() }.toList())).setPlayerExecutor {
        val region = Man10RealEstateV2.cityData.values.map { map -> map.regions.entries }.stream().flatMap { it.stream() }.toList().find { find -> find.value.includeName == it.args[1] }!!.value
        if (region.teleportLoc == null){
            it.sender.sendPrefixMsg(SStr("&cその土地にはテレポート位置が存在しません！"))
            return@setPlayerExecutor
        }
        it.sender.teleport(region.teleportLoc!!)
        it.sender.playSound(it.sender.location,Sound.ENTITY_ENDERMAN_TELEPORT,1f,1f)
        it.sender.sendPrefixMsg(SStr("&a${region.includeName}にテレポートしました"))
    }

    private fun opCommand() = command().addArg(SCommandArg("op"))

    @SCommandBody("mrev2.op")
    val giveWand = opCommand().addArg(SCommandArg("wand")).setPlayerExecutor {
        val item = SItem(Material.STICK).setDisplayName("§a範囲指定のわんど").setCustomData(Man10RealEstateV2.plugin,"wand",
            PersistentDataType.INTEGER,1)
        it.sender.inventory.setItemInMainHand(item)

        it.sender.sendPrefixMsg(SStr("&aプレゼント"))
    }

    @SCommandBody("mg.op")
    val createCity = opCommand().addArg(SCommandArg("create")).addArg(SCommandArg("city")).addArg(SCommandArg().addAlias("内部名")).setPlayerExecutor {
        val meta = it.sender.inventory.itemInMainHand.itemMeta
        val firstLoc = meta.persistentDataContainer[NamespacedKey(Man10RealEstateV2.plugin,"firstloc"), PersistentDataType.STRING]
        val secondLoc = meta.persistentDataContainer[NamespacedKey(Man10RealEstateV2.plugin,"secondloc"), PersistentDataType.STRING]

        if (firstLoc == null || secondLoc == null){
            it.sender.sendPrefixMsg(SStr("&c範囲指定してね！"))
            return@setPlayerExecutor
        }

        if (SJavaPlugin.sConfig.exists("city/${it.args[3]}")){
            it.sender.sendPrefixMsg(SStr("§c既に存在してるよー...?"))
            return@setPlayerExecutor
        }

        val firstTriple = firstLoc.split(",").map { map -> map.toInt() }.toList()
        val secondTriple = secondLoc.split(",").map { map -> map.toInt() }.toList()

        val cityData = CityData()
        cityData.includeName = it.args[3]
        cityData.displayName = it.args[3]
        cityData.startLoc = Triple(firstTriple[0],firstTriple[1],firstTriple[2])
        cityData.endLoc = Triple(secondTriple[0],secondTriple[1],secondTriple[2])
        cityData.world = it.sender.world
        CityEdit(cityData).open(it.sender)

    }

    @SCommandBody("mg.op")
    val createRegion = opCommand().addArg(SCommandArg("create")).addArg(SCommandArg("region")).addArg(SCommandArg(Man10RealEstateV2.cityData.keys).addAlias("city名")).addArg(SCommandArg().addAlias("内部名")).setPlayerExecutor {
        val meta = it.sender.inventory.itemInMainHand.itemMeta
        val firstLoc = meta.persistentDataContainer[NamespacedKey(Man10RealEstateV2.plugin,"firstloc"), PersistentDataType.STRING]
        val secondLoc = meta.persistentDataContainer[NamespacedKey(Man10RealEstateV2.plugin,"secondloc"), PersistentDataType.STRING]

        if (firstLoc == null || secondLoc == null){
            it.sender.sendPrefixMsg(SStr("&c範囲指定してね！"))
            return@setPlayerExecutor
        }

        if (Man10RealEstateV2.cityData[it.args[3]]!!.regions.containsKey(it.args[4])){
            it.sender.sendPrefixMsg(SStr("§c既に存在してるよー...?"))
            return@setPlayerExecutor
        }

        val firstTriple = firstLoc.split(",").map { map -> map.toInt() }.toList()
        val secondTriple = secondLoc.split(",").map { map -> map.toInt() }.toList()
        val city = Man10RealEstateV2.cityData[it.args[3]]!!

        val regionData = RegionData()
        regionData.includeName = it.args[4]
        regionData.displayName = it.args[4]
        regionData.tax = city.tax
        regionData.taxCycle = city.taxCycle
        regionData.defaultPrice = city.defaultPrice
        regionData.subUserLimit = city.subUserLimit
        regionData.buyScore = city.buyScore
        regionData.liveScore = city.liveScore
        regionData.startLoc = Triple(firstTriple[0],firstTriple[1],firstTriple[2])
        regionData.endLoc = Triple(secondTriple[0],secondTriple[1],secondTriple[2])
        regionData.world = it.sender.world
        val now = Calendar.getInstance()
        now.set(Calendar.MILLISECOND,0)
        now.set(Calendar.SECOND,0)
        now.set(Calendar.MINUTE,0)
        now.set(Calendar.HOUR_OF_DAY,0)
        regionData.lastRent = now.time.time
        regionData.lastTax = now.time.time

        RegionEdit(regionData,city.includeName).open(it.sender)
    }

    @SCommandBody("mrev2.op")
    val reloadCommands = opCommand().addArg(SCommandArg("reloadCommands")).setNormalExecutor {
        reloadSCommandBodies()
        it.sender.sendPrefixMsg(SStr("&aCommand Reloaded"))
    }
}