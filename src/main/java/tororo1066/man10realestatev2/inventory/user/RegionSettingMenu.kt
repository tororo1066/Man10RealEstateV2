package tororo1066.man10realestatev2.inventory.user

import org.bukkit.Material
import org.bukkit.entity.HumanEntity
import org.bukkit.entity.Player
import red.man10.man10score.ScoreDatabase
import tororo1066.man10realestatev2.Man10RealEstateV2
import tororo1066.man10realestatev2.Man10RealEstateV2.Companion.sendPrefixMsg
import tororo1066.man10realestatev2.data.RegionData
import tororo1066.tororopluginapi.SJavaPlugin
import tororo1066.tororopluginapi.SStr
import tororo1066.tororopluginapi.otherUtils.UsefulUtility
import tororo1066.tororopluginapi.sInventory.SInventory
import tororo1066.tororopluginapi.sInventory.SInventoryItem
import tororo1066.tororopluginapi.sItem.SItem
import tororo1066.tororopluginapi.utils.LocType
import tororo1066.tororopluginapi.utils.toLocString
import java.util.Calendar
import java.util.concurrent.Callable
import java.util.concurrent.Executors

class RegionSettingMenu(val region: RegionData): SInventory(Man10RealEstateV2.plugin,"§a${region.includeName}",1) {
    override fun renderMenu(): Boolean {
        setItem(2, SInventoryItem(Material.EMERALD).setDisplayName("§a詳細設定")
            .setLore(listOf("§b土地の状態:${region.state.displayName}","§e金額:${UsefulUtility.doubleToFormatString(region.price)}円"))
            .setCanClick(false).setClickEvent { e ->
                val newInv = object : SInventory(Man10RealEstateV2.plugin,"§a${region.includeName}",3){
                    override fun renderMenu(): Boolean {
                        setItem(1, SInventoryItem(Material.COMPASS).setDisplayName("§a土地の状態を変更する").addLore("§a現在の状態:${region.state.displayName}").setCanClick(false).setClickEvent {
                            val newInv = object : SInventory(Man10RealEstateV2.plugin,"§a状態",1){
                                fun action(p: HumanEntity, state: RegionData.State){
                                    p.closeInventory()
                                    if (SJavaPlugin.mysql.asyncExecute("update region_data set state = '${state.name}' where includeName = '${region.includeName}'")){
                                        region.state = state
                                    }
                                }

                                override fun renderMenu(): Boolean {

                                    setItem(0, SInventoryItem(Material.RED_TERRACOTTA).setDisplayName("§c危険").setCanClick(false).setClickEvent {
                                        action(it.whoClicked,RegionData.State.DANGER)
                                    })
                                    setItem(1, SInventoryItem(Material.LIME_TERRACOTTA).setDisplayName("§6フリー").setCanClick(false).setClickEvent {
                                        action(it.whoClicked,RegionData.State.FREE)
                                    })
                                    setItem(2, SInventoryItem(Material.EMERALD_BLOCK).setDisplayName("§d販売中").setCanClick(false).setClickEvent {
                                        action(it.whoClicked,RegionData.State.ONSALE)
                                    })
                                    setItem(3, SInventoryItem(Material.IRON_BLOCK).setDisplayName("§a保護").setCanClick(false).setClickEvent {
                                        action(it.whoClicked,RegionData.State.PROTECTED)
                                    })
                                    return true
                                }
                            }
                            moveChildInventory(newInv,e.whoClicked as Player)
                        })

                        setItem(4,createInputItem(SItem(Material.EMERALD).setDisplayName("§a土地の金額を設定する").addLore("§a現在の金額:${UsefulUtility.doubleToFormatString(region.price)}円"),Double::class.java,"§a/<金額>(例:/10000)") { double, _ ->
                            if (double < 0.0){
                                e.whoClicked.sendPrefixMsg(SStr("&c値段を0円未満にすることはできません！"))
                                return@createInputItem
                            }
                            if (SJavaPlugin.mysql.asyncExecute("update region_data set price = $double where includeName = '${region.includeName}'")){
                                region.price = double
                            }
                        })

                        setItem(7,SInventoryItem(Material.ENDER_PEARL).setDisplayName("§aテレポートの位置を設定する").addLore("§a立っている位置をテレポート位置にする").setCanClick(false).setClickEvent {
                            val locString = it.whoClicked.location.toLocString(LocType.DIR_COMMA)
                            if (SJavaPlugin.mysql.asyncExecute("update region_data set teleportLoc = '${locString}' where includeName = '${region.includeName}'")){
                                region.teleportLoc = it.whoClicked.location
                            }
                            (it.whoClicked as Player).sendPrefixMsg(SStr("&a設定しました"))
                        })

                        setItem(20, SInventoryItem(Material.CLOCK).setDisplayName("§a賃料のスパンを変更する").addLore("§a現在のスパン:${region.rentCycle.displayName}").setCanClick(false).setClickEvent {
                            val newInv = object : SInventory(Man10RealEstateV2.plugin,"§aスパン",1){
                                override fun renderMenu(): Boolean {

                                    fun action(p: HumanEntity, cycle: RegionData.RentCycle){
                                        p.closeInventory()
                                        if (SJavaPlugin.mysql.asyncExecute("update region_data set rentCycle = '${cycle.name}' where includeName = '${region.includeName}'")){
                                            region.rentCycle = cycle
                                            val now = Calendar.getInstance()
                                            now.set(Calendar.MILLISECOND,0)
                                            now.set(Calendar.SECOND,0)
                                            now.set(Calendar.MINUTE,0)
                                            now.set(Calendar.HOUR_OF_DAY,0)

                                            if (SJavaPlugin.mysql.asyncExecute("update region_data set lastRent = ${now.time.time} where includeName = '${region.includeName}'")){
                                                region.lastRent = now.time.time
                                            }
                                        }
                                    }

                                    setItem(0, SInventoryItem(Material.RED_TERRACOTTA).setDisplayName("§a1日ごと").setCanClick(false).setClickEvent {
                                        action(it.whoClicked,RegionData.RentCycle.DAY_1)
                                    })
                                    setItem(1, SInventoryItem(Material.ORANGE_TERRACOTTA).setDisplayName("§a1週間ごと").setCanClick(false).setClickEvent {
                                        action(it.whoClicked,RegionData.RentCycle.DAY_7)
                                    })
                                    setItem(2, SInventoryItem(Material.YELLOW_TERRACOTTA).setDisplayName("§a15日ごと").setCanClick(false).setClickEvent {
                                        action(it.whoClicked,RegionData.RentCycle.DAY_15)
                                    })
                                    setItem(3, SInventoryItem(Material.LIME_TERRACOTTA).setDisplayName("§a30日ごと").setCanClick(false).setClickEvent {
                                        action(it.whoClicked,RegionData.RentCycle.DAY_30)
                                    })
                                    setItem(4, SInventoryItem(Material.GREEN_TERRACOTTA).setDisplayName("§a月の変わり目").setCanClick(false).setClickEvent {
                                        action(it.whoClicked,RegionData.RentCycle.MONTH_CHANGED)
                                    })
                                    setItem(5, SInventoryItem(Material.BARRIER).setDisplayName("§a無し").setCanClick(false).setClickEvent {
                                        action(it.whoClicked,RegionData.RentCycle.NONE)
                                    })
                                    return true
                                }
                            }
                            moveChildInventory(newInv,e.whoClicked as Player)
                        })

                        setItem(24, createInputItem(SItem(Material.PLAYER_HEAD).setDisplayName("§aオーナーを変更する"),Player::class.java,"/<プレイヤー名> /cancelでキャンセル",false) { player, _ ->
                            val data = Man10RealEstateV2.cityData[region.city]!!
                            val score = Executors.newCachedThreadPool().submit(Callable { ScoreDatabase.getScore(player.uniqueId) }).get()
                            if ((data.buyScore != null && data.buyScore!! > score) || (region.buyScore != null && region.buyScore!! > score)){
                                e.whoClicked.sendPrefixMsg(SStr("&cこのプレイヤーはこの土地を持つためのスコアが足りません！"))
                                return@createInputItem
                            }
                            if (data.regionLimit != null && data.regionLimit!! >= data.regions.filter { it.value.ownerUUID == player.uniqueId }.size){
                                e.whoClicked.sendPrefixMsg(SStr("&cこのプレイヤーはこれ以上この町の土地を持てません！"))
                                return@createInputItem
                            }
                            player.sendPrefixMsg(SStr("&e${e.whoClicked.name}&aからオーナー権を受け取ろうとしています"))
                            Man10RealEstateV2.sInput.clickAccept(player,"§a§l[受け取るにはここをクリック！]",{
                                if (SJavaPlugin.mysql.asyncExecute("update region_data set ownerUUID = '${player.uniqueId}' where includeName = '${region.includeName}'")){
                                    if (region.ownerUUID != null){
                                        if (region.players.remove(region.ownerUUID) != null){
                                            SJavaPlugin.mysql.asyncExecute("delete from user_data where uuid = '${region.ownerUUID}' and region_id = '${region.includeName}'")
                                        }
                                    }
                                    if (region.players.remove(e.whoClicked.uniqueId) != null){
                                        SJavaPlugin.mysql.asyncExecute("delete from user_data where uuid = '${e.whoClicked.uniqueId}' and region_id = '${region.includeName}'")
                                    }
                                    region.ownerUUID = player.uniqueId
                                    e.whoClicked.sendPrefixMsg(SStr("&a土地のオーナー権を移しました！"))
                                    player.sendPrefixMsg(SStr("&a土地を受け取りました！"))
                                } else {
                                    e.whoClicked.sendPrefixMsg(SStr("&c失敗しました"))
                                    player.sendPrefixMsg(SStr("&c失敗しました"))
                                }

                            },{e.whoClicked.sendPrefixMsg(SStr("&c${player.name}は土地を受け取りませんでした"))},60)
                        })
                        return true
                    }
                }
                moveChildInventory(newInv,e.whoClicked as Player)
            })
        return true
    }
}