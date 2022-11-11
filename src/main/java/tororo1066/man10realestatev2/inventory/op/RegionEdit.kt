package tororo1066.man10realestatev2.inventory.op

import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.World
import org.bukkit.entity.Player
import tororo1066.man10realestatev2.Man10RealEstateV2
import tororo1066.man10realestatev2.Man10RealEstateV2.Companion.sendPrefixMsg
import tororo1066.man10realestatev2.data.RegionData
import tororo1066.man10realestatev2.data.enumData.TaxCycle
import tororo1066.tororopluginapi.SJavaPlugin
import tororo1066.tororopluginapi.SStr
import tororo1066.tororopluginapi.defaultMenus.LargeSInventory
import tororo1066.tororopluginapi.sInventory.SInventory
import tororo1066.tororopluginapi.sInventory.SInventoryItem
import tororo1066.tororopluginapi.sItem.SItem
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.SimpleTimeZone

class RegionEdit(val data: RegionData, val city: String): LargeSInventory(Man10RealEstateV2.plugin,"RegionEdit: ${data.includeName}") {


    override fun renderMenu(): Boolean {
        val items = arrayListOf(
            createInputItem(SItem(Material.GOLD_BLOCK).setDisplayName("§a表示名").setLore(listOf("§e現在の値:${data.displayName}")),String::class.java){ str, _ ->
                data.displayName = str.replace("&","§")
            },
            createInputItem(SItem(Material.DIAMOND_BLOCK).setDisplayName("§a税金").setLore(listOf("§e現在の値:${data.tax}円")),Double::class.java) { double, _ ->
                data.tax = double
            },
            createInputItem(SItem(Material.IRON_BLOCK).setDisplayName("§a住民の数(-1でnull)").setLore(listOf("§e現在の値:${data.subUserLimit}")),Int::class.java) { int, _ ->
                if (int == -1) data.subUserLimit = null else data.subUserLimit = int
            },
            createInputItem(SItem(Material.REDSTONE_BLOCK).setDisplayName("§a土地を買うのに必要なスコア(-1でnull)").setLore(listOf("§e現在の値:${data.buyScore}")),Int::class.java) { int, _ ->
                if (int == -1) data.buyScore = null else data.buyScore = int
            },
            createInputItem(SItem(Material.LAPIS_BLOCK).setDisplayName("§a土地に住まわせるのに必要なスコア(-1でnull)").setLore(listOf("§e現在の値:${data.liveScore}")),Int::class.java) { int, _ ->
                if (int == -1) data.liveScore = null else data.liveScore = int
            },
            createInputItem(SItem(Material.COAL_BLOCK).setDisplayName("§aデフォルトの土地の価格").setLore(listOf("§e現在の値:${data.defaultPrice}")),Double::class.java) { double, _ ->
                data.defaultPrice = double
                data.price = double
            },
            createInputItem(SItem(Material.NETHERITE_BLOCK).setDisplayName("§a最後に税金を払った日を設定する")
                .setLore(listOf("§e現在の値:${SimpleDateFormat("yyyy/MM/dd kk:mm:ss").format(Date(data.lastTax))}")),Date::class.java){ date, _ ->
                data.lastTax = date.time / 1000
            },
            SInventoryItem(Material.GLASS).setDisplayName("§a土地の状態").setLore(listOf("§e現在の値:${data.state.name}")).setClickEvent { e ->
                val newInv = object : SInventory(Man10RealEstateV2.plugin,"§a状態",3){
                    override fun renderMenu(): Boolean {

                        setItem(10, SInventoryItem(Material.RED_TERRACOTTA).setDisplayName("DANGER").setCanClick(false).setClickEvent {
                            data.state = RegionData.State.DANGER
                            it.whoClicked.closeInventory()
                        })
                        setItem(11, SInventoryItem(Material.ORANGE_TERRACOTTA).setDisplayName("FREE").setCanClick(false).setClickEvent {
                            data.state = RegionData.State.FREE
                            it.whoClicked.closeInventory()
                        })
                        setItem(12, SInventoryItem(Material.YELLOW_TERRACOTTA).setDisplayName("ONSALE").setCanClick(false).setClickEvent {
                            data.state = RegionData.State.ONSALE
                            it.whoClicked.closeInventory()
                        })
                        setItem(13, SInventoryItem(Material.LIME_TERRACOTTA).setDisplayName("PROTECTED").setCanClick(false).setClickEvent {
                            data.state = RegionData.State.PROTECTED
                            it.whoClicked.closeInventory()
                        })
                        return true
                    }
                }
                moveChildInventory(newInv,e.whoClicked as Player)
            },
            SInventoryItem(Material.SANDSTONE).setDisplayName("§a税金の周期").setLore(listOf("§e現在の値:${data.taxCycle.name}")).setClickEvent { e ->
                val newInv = object : SInventory(Man10RealEstateV2.plugin,"§a周期",3){
                    override fun renderMenu(): Boolean {

                        setItem(10, SInventoryItem(Material.RED_TERRACOTTA).setDisplayName("NONE").setCanClick(false).setClickEvent {
                            data.taxCycle = TaxCycle.NONE
                            it.whoClicked.closeInventory()
                        })
                        setItem(11, SInventoryItem(Material.ORANGE_TERRACOTTA).setDisplayName("DAY_1").setCanClick(false).setClickEvent {
                            data.taxCycle = TaxCycle.DAY_1
                            it.whoClicked.closeInventory()
                        })
                        setItem(12, SInventoryItem(Material.YELLOW_TERRACOTTA).setDisplayName("DAY_7").setCanClick(false).setClickEvent {
                            data.taxCycle = TaxCycle.DAY_7
                            it.whoClicked.closeInventory()
                        })
                        setItem(13, SInventoryItem(Material.LIME_TERRACOTTA).setDisplayName("DAY_15").setCanClick(false).setClickEvent {
                            data.taxCycle = TaxCycle.DAY_15
                            it.whoClicked.closeInventory()
                        })
                        setItem(14, SInventoryItem(Material.GREEN_TERRACOTTA).setDisplayName("DAY_30").setCanClick(false).setClickEvent {
                            data.taxCycle = TaxCycle.DAY_30
                            it.whoClicked.closeInventory()
                        })
                        setItem(15, SInventoryItem(Material.BLUE_TERRACOTTA).setDisplayName("MONTH_CHANGED").setCanClick(false).setClickEvent {
                            data.taxCycle = TaxCycle.MONTH_CHANGED
                            it.whoClicked.closeInventory()
                        })
                        return true
                    }
                }
                moveChildInventory(newInv,e.whoClicked as Player)
            },
            SInventoryItem(Material.EMERALD_BLOCK).setDisplayName("§a保存").setCanClick(false).setClickEvent {
                SJavaPlugin.mysql.asyncExecute("insert into region_data (city,includeName,displayName,tax,userLimit,buyScore,liveScore,defaultPrice,startLoc,endLoc,price,ownerUUID,state,rentCycle,lastTax,taxCycle,lastRent,teleportLoc) values" +
                        "('${city}','${data.includeName}','${data.displayName}',${data.tax},${if (data.subUserLimit == null) -1 else data.subUserLimit},${if (data.buyScore == null) -1 else data.buyScore},${if (data.liveScore == null) -1 else data.liveScore}," +
                        "${data.defaultPrice},'${data.startLoc.first},${data.startLoc.second},${data.startLoc.third}','${data.endLoc.first},${data.endLoc.second},${data.endLoc.third}'," +
                        "${data.defaultPrice},'none','${data.state.name}','${data.rentCycle.name}',${data.lastTax},'${data.taxCycle.name}',${data.lastTax},'none')")
                Man10RealEstateV2.cityData[city]!!.regions[data.includeName] = data
                (it.whoClicked as Player).sendPrefixMsg(SStr("&a保存したよ～"))
                it.whoClicked.closeInventory()
            }
        )
        setResourceItems(items)
        return true
    }
}