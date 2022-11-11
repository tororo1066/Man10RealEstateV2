package tororo1066.man10realestatev2.inventory.op

import org.bukkit.Material
import org.bukkit.World
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import tororo1066.man10realestatev2.Man10RealEstateV2
import tororo1066.man10realestatev2.Man10RealEstateV2.Companion.sendPrefixMsg
import tororo1066.man10realestatev2.data.CityData
import tororo1066.man10realestatev2.data.enumData.TaxCycle
import tororo1066.tororopluginapi.SJavaPlugin
import tororo1066.tororopluginapi.SStr
import tororo1066.tororopluginapi.defaultMenus.LargeSInventory
import tororo1066.tororopluginapi.sInventory.SInventory
import tororo1066.tororopluginapi.sInventory.SInventoryItem
import tororo1066.tororopluginapi.sItem.SItem

class CityEdit(val cityData: CityData): LargeSInventory(Man10RealEstateV2.plugin,"CityEdit: ${cityData.includeName}") {

    override fun renderMenu(): Boolean {
        val items = arrayListOf(
            createInputItem(SItem(Material.GRASS_BLOCK).setDisplayName("§aワールド").setLore(listOf("§e現在の値:${cityData.world.name}")),World::class.java) { world, _ ->
                cityData.world = world
            },
            createInputItem(SItem(Material.GOLD_BLOCK).setDisplayName("§a表示名").setLore(listOf("§e現在の値:${cityData.displayName}")),String::class.java){ str, _ ->
                cityData.displayName = str.replace("&","§")
            },
            createInputItem(SItem(Material.DIAMOND_BLOCK).setDisplayName("§a税金").setLore(listOf("§e現在の値:${cityData.tax}円")),Double::class.java) { double, _ ->
                cityData.tax = double
            },
            createInputItem(SItem(Material.IRON_BLOCK).setDisplayName("§a住民の数(-1でnull)").setLore(listOf("§e現在の値:${cityData.subUserLimit}")),Int::class.java) { int, _ ->
                if (int == -1) cityData.subUserLimit = null else cityData.subUserLimit = int
            },
            createInputItem(SItem(Material.REDSTONE_BLOCK).setDisplayName("§a土地を買うのに必要なスコア(-1でnull)").setLore(listOf("§e現在の値:${cityData.buyScore}")),Int::class.java) { int, _ ->
                if (int == -1) cityData.buyScore = null else cityData.buyScore = int
            },
            createInputItem(SItem(Material.LAPIS_BLOCK).setDisplayName("§a土地に住まわせるのに必要なスコア(-1でnull)").setLore(listOf("§e現在の値:${cityData.liveScore}")),Int::class.java) { int, _ ->
                if (int == -1) cityData.liveScore = null else cityData.liveScore = int
            },
            createInputItem(SItem(Material.COAL_BLOCK).setDisplayName("§aデフォルトの土地の価格").setLore(listOf("§e現在の値:${cityData.defaultPrice}")),Double::class.java) { double, _ ->
                cityData.defaultPrice = double
            },
            SInventoryItem(Material.SANDSTONE).setDisplayName("§a税金の周期").setLore(listOf("§e現在の値:${cityData.taxCycle.name}")).setClickEvent { e ->
                val newInv = object : SInventory(Man10RealEstateV2.plugin,"§a周期",3){
                    override fun renderMenu(): Boolean {

                        setItem(10, SInventoryItem(Material.RED_TERRACOTTA).setDisplayName("NONE").setCanClick(false).setClickEvent {
                            cityData.taxCycle = TaxCycle.NONE
                            it.whoClicked.closeInventory()
                        })
                        setItem(11, SInventoryItem(Material.ORANGE_TERRACOTTA).setDisplayName("DAY_1").setCanClick(false).setClickEvent {
                            cityData.taxCycle = TaxCycle.DAY_1
                            it.whoClicked.closeInventory()
                        })
                        setItem(12, SInventoryItem(Material.YELLOW_TERRACOTTA).setDisplayName("DAY_7").setCanClick(false).setClickEvent {
                            cityData.taxCycle = TaxCycle.DAY_7
                            it.whoClicked.closeInventory()
                        })
                        setItem(13, SInventoryItem(Material.LIME_TERRACOTTA).setDisplayName("DAY_15").setCanClick(false).setClickEvent {
                            cityData.taxCycle = TaxCycle.DAY_15
                            it.whoClicked.closeInventory()
                        })
                        setItem(14, SInventoryItem(Material.GREEN_TERRACOTTA).setDisplayName("DAY_30").setCanClick(false).setClickEvent {
                            cityData.taxCycle = TaxCycle.DAY_30
                            it.whoClicked.closeInventory()
                        })
                        setItem(15, SInventoryItem(Material.BLUE_TERRACOTTA).setDisplayName("MONTH_CHANGED").setCanClick(false).setClickEvent {
                            cityData.taxCycle = TaxCycle.MONTH_CHANGED
                            it.whoClicked.closeInventory()
                        })
                        return true
                    }
                }
                moveChildInventory(newInv,e.whoClicked as Player)
            }
            ,
            SInventoryItem(Material.EMERALD_BLOCK).setDisplayName("§a保存").setCanClick(false).setClickEvent {
                val yaml = YamlConfiguration()
                yaml.set("name",cityData.displayName)
                yaml.set("world",cityData.world.name)
                yaml.set("startLoc",cityData.startLoc.toList().joinToString(","))
                yaml.set("endLoc",cityData.endLoc.toList().joinToString(","))
                yaml.set("tax",cityData.tax)
                yaml.set("userLimit",cityData.subUserLimit)
                yaml.set("buyScore",cityData.buyScore)
                yaml.set("liveScore",cityData.liveScore)
                yaml.set("defaultPrice",cityData.defaultPrice)
                yaml.set("taxCycle",cityData.taxCycle.name)
                SJavaPlugin.sConfig.saveConfig(yaml,"city/${cityData.includeName}")
                Man10RealEstateV2.cityData[cityData.includeName] = cityData
                (it.whoClicked as Player).sendPrefixMsg(SStr("&a保存したよ～"))
                it.whoClicked.closeInventory()
            }
        )
        setResourceItems(items)
        return true
    }
}