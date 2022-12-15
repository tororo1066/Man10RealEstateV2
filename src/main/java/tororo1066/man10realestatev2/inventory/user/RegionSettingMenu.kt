package tororo1066.man10realestatev2.inventory.user

import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.HumanEntity
import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import red.man10.man10score.ScoreDatabase
import tororo1066.man10realestatev2.Man10RealEstateV2
import tororo1066.man10realestatev2.Man10RealEstateV2.Companion.sendPrefixMsg
import tororo1066.man10realestatev2.data.RegionData
import tororo1066.man10realestatev2.data.UserData
import tororo1066.man10realestatev2.data.enumData.TaxCycle
import tororo1066.tororopluginapi.SJavaPlugin
import tororo1066.tororopluginapi.SStr
import tororo1066.tororopluginapi.defaultMenus.LargeSInventory
import tororo1066.tororopluginapi.integer.PlusInt
import tororo1066.tororopluginapi.otherUtils.UsefulUtility
import tororo1066.tororopluginapi.sInventory.SInventory
import tororo1066.tororopluginapi.sInventory.SInventoryItem
import tororo1066.tororopluginapi.sItem.SItem
import tororo1066.tororopluginapi.utils.LocType
import tororo1066.tororopluginapi.utils.toLocString
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import kotlin.collections.ArrayList

class RegionSettingMenu(val region: RegionData): SInventory(Man10RealEstateV2.plugin,"§a${region.includeName}",1) {
    override fun renderMenu(): Boolean {
        setItem(1, SInventoryItem(Material.EMERALD).setDisplayName("§a詳細設定")
            .setLore(listOf("§b土地の状態:${region.state.displayName}","§e金額:${UsefulUtility.doubleToFormatString(region.price)}円"))
            .setCanClick(false).setClickEvent { e ->
                val newInv = object : SInventory(Man10RealEstateV2.plugin,"§a${region.includeName}",3){
                    override fun renderMenu(): Boolean {
                        if (region.ownerUUID == e.whoClicked.uniqueId){
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
                        }

                        if (region.ownerUUID == e.whoClicked.uniqueId){
                            setItem(4,createInputItem(SItem(Material.EMERALD).setDisplayName("§a土地の金額を設定する").addLore("§a現在の金額:${UsefulUtility.doubleToFormatString(region.price)}円"),Double::class.java,"§a/<金額>(例:/10000)") { double, _ ->
                                if (double < 0.0){
                                    e.whoClicked.sendPrefixMsg(SStr("&c値段を0円未満にすることはできません！"))
                                    return@createInputItem
                                }
                                if (Man10RealEstateV2.regionPriceLimit != -1.0 && Man10RealEstateV2.regionPriceLimit < double){
                                    e.whoClicked.sendPrefixMsg(SStr("&c値段は${UsefulUtility.doubleToFormatString(Man10RealEstateV2.regionPriceLimit)}円以上にできません！"))
                                    return@createInputItem
                                }
                                if (SJavaPlugin.mysql.asyncExecute("update region_data set price = $double where includeName = '${region.includeName}'")){
                                    region.price = double
                                }
                            })
                        }

                        setItem(7,SInventoryItem(Material.ENDER_PEARL).setDisplayName("§aテレポートの位置を設定する").addLore("§a立っている位置をテレポート位置にする").setCanClick(false).setClickEvent second@ {
                            if (!Man10RealEstateV2.inRegion(it.whoClicked.location,region.startLoc,region.endLoc)){
                                it.whoClicked.sendPrefixMsg(SStr("&c土地の中で実行してください！"))
                                return@second
                            }
                            val locString = it.whoClicked.location.toLocString(LocType.DIR_COMMA)
                            if (SJavaPlugin.mysql.asyncExecute("update region_data set teleportLoc = '${locString}' where includeName = '${region.includeName}'")){
                                region.teleportLoc = it.whoClicked.location
                            }
                            it.whoClicked.sendPrefixMsg(SStr("&a設定しました"))
                        })

                        setItem(19, SInventoryItem(Material.CLOCK).setDisplayName("§a賃料のスパンを変更する").addLore("§a現在のスパン:${region.rentCycle.displayName}").setCanClick(false).setClickEvent {
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

                        if (region.ownerUUID == e.whoClicked.uniqueId){
                            setItem(22, createInputItem(SItem(Material.PLAYER_HEAD).setDisplayName("§aオーナーを変更する"),Player::class.java,"/<プレイヤー名> /cancelでキャンセル",false) { player, _ ->
                                val data = Man10RealEstateV2.cityData[region.city]!!

                                player.sendPrefixMsg(SStr("&e${e.whoClicked.name}&aからオーナー権を受け取ろうとしています"))
                                Man10RealEstateV2.sInput.clickAccept(player,"§a§l[受け取るにはここをクリック！]",{
                                    val score = Executors.newCachedThreadPool().submit(Callable { ScoreDatabase.getScore(player.uniqueId) }).get()
                                    if ((data.buyScore != null && data.buyScore!! > score) || (region.buyScore != null && region.buyScore!! > score)){
                                        e.whoClicked.sendPrefixMsg(SStr("&cこのプレイヤーはこの土地を持つためのスコアが足りません！"))
                                        player.sendPrefixMsg(SStr("&cこの土地を持つためのスコアが足りません！"))
                                        return@clickAccept
                                    }
                                    if (data.regionLimit != null && data.regionLimit!! >= data.regions.filter { it.value.ownerUUID == player.uniqueId }.size){
                                        e.whoClicked.sendPrefixMsg(SStr("&cこのプレイヤーはこれ以上この町の土地を持てません！"))
                                        player.sendPrefixMsg(SStr("&cこれ以上この町の土地を持てません！"))
                                        return@clickAccept
                                    }

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
                        }

                        setItem(25, SInventoryItem(Material.GOLD_BLOCK).setDisplayName("§e次の税金を確認する").setCanClick(false).setClickEvent {
                            it.whoClicked.closeInventory()
                            it.whoClicked.sendPrefixMsg(SStr("&e次の税金:" +
                                    if (region.taxCycle == TaxCycle.NONE) "無し" else SimpleDateFormat("yyyy/MM/dd kk:mm:ss").format(Date(region.lastTax * 1000 + region.taxCycle.amount))
                            ))
                            it.whoClicked.sendPrefixMsg(SStr("&e税金:${UsefulUtility.doubleToFormatString(if (region.tax == null) Man10RealEstateV2.cityData[region.city]!!.tax else region.tax!!)}円"))
                        })
                        return true
                    }
                }
                moveChildInventory(newInv,e.whoClicked as Player)
            })

        setItem(4, SInventoryItem(Material.PLAYER_HEAD).setDisplayName("§b住人の管理").setCanClick(false).setClickEvent { e ->
            val newInv = object : LargeSInventory(plugin,"§b住人の管理"){
                override fun renderMenu(): Boolean {
                    val items = ArrayList<SInventoryItem>()
                    region.players.values.forEach {
                        if (it.uuid == e.whoClicked.uniqueId)return@forEach
                        val offlinePlayer = Bukkit.getOfflinePlayer(it.uuid)
                        items.add(SInventoryItem(Material.PLAYER_HEAD).setSkullOwner(it.uuid).setDisplayName("§6${it.mcid}")
                            .addLore(if (offlinePlayer.isOnline) "§aオンライン" else "§cオフライン").setCanClick(false).setClickEvent { _ ->
                                val newInv = object : SInventory(plugin,offlinePlayer.name!!,1){
                                    override fun renderMenu(): Boolean {
                                        fun updatePerm(){
                                            SJavaPlugin.mysql.asyncExecute("update user_data set perms = '${it.perms.map { map -> map.name }.joinToString(",")}' where region_id = '${region.includeName}' and uuid = '${it.uuid}'")
                                            renderMenu()
                                        }
                                        setItem(0,SInventoryItem(Material.PLAYER_HEAD).setSkullOwner(it.uuid).setDisplayName("§6${it.mcid}")
                                            .addLore(if (offlinePlayer.isOnline) "§aオンライン" else "§cオフライン").setCanClick(false))
                                        val hasAll = it.perms.contains(UserData.Perm.ALL)
                                        val hasInteract = it.perms.contains(UserData.Perm.INTERACT)
                                        val hasBlock = it.perms.contains(UserData.Perm.BLOCK)
                                        val hasInventory = it.perms.contains(UserData.Perm.INVENTORY)
                                        setItem(2, SInventoryItem(if (hasAll) Material.LIME_STAINED_GLASS_PANE else Material.RED_STAINED_GLASS_PANE)
                                            .setDisplayName(if (hasAll) "§a権限:全て" else "§c権限:全て").setCanClick(false).setClickEvent { _ ->
                                                if (hasAll){
                                                    it.perms.remove(UserData.Perm.ALL)
                                                } else {
                                                    it.perms.add(UserData.Perm.ALL)
                                                }
                                                updatePerm()
                                        })

                                        setItem(3, SInventoryItem(if (hasInteract) Material.LIME_STAINED_GLASS_PANE else Material.RED_STAINED_GLASS_PANE)
                                            .setDisplayName(if (hasInteract) "§a権限:触る" else "§c権限:触る").setCanClick(false).setClickEvent { _ ->
                                                if (hasInteract){
                                                    it.perms.remove(UserData.Perm.INTERACT)
                                                } else {
                                                    it.perms.add(UserData.Perm.INTERACT)
                                                }
                                                updatePerm()
                                            })

                                        setItem(4, SInventoryItem(if (hasBlock) Material.LIME_STAINED_GLASS_PANE else Material.RED_STAINED_GLASS_PANE)
                                            .setDisplayName(if (hasBlock) "§a権限:ブロック" else "§c権限:ブロック").setCanClick(false).setClickEvent { _ ->
                                                if (hasBlock){
                                                    it.perms.remove(UserData.Perm.BLOCK)
                                                } else {
                                                    it.perms.add(UserData.Perm.BLOCK)
                                                }
                                                updatePerm()
                                            })

                                        setItem(5, SInventoryItem(if (hasInventory) Material.LIME_STAINED_GLASS_PANE else Material.RED_STAINED_GLASS_PANE)
                                            .setDisplayName(if (hasInventory) "§a権限:インベントリ" else "§c権限:インベントリ").setCanClick(false).setClickEvent { _ ->
                                                if (hasInventory){
                                                    it.perms.remove(UserData.Perm.INVENTORY)
                                                } else {
                                                    it.perms.add(UserData.Perm.INVENTORY)
                                                }
                                                updatePerm()
                                            })

                                        setItem(7,createInputItem(SItem(Material.EMERALD).setDisplayName("§a賃料を設定する").addLore("§a現在:${it.rent}円"),PlusInt::class.java,"/<賃料(/cancelでキャンセル)>",{ int, _ ->
                                            if (SJavaPlugin.mysql.asyncExecute("update user_data set rent = ${int.get().toDouble()} where region_id = '${region.includeName}' and uuid = '${it.uuid}' ")){
                                                it.rent = int.get().toDouble()
                                            }
                                        },{ "§c0以上で設定してください！" },ClickType.values().toList(),false))
                                        return true
                                    }
                                }
                                moveChildInventory(newInv,e.whoClicked as Player)
                            })
                    }
                    setResourceItems(items)
                    return true
                }
            }
            moveChildInventory(newInv,e.whoClicked as Player)
        })

        setItem(7, createInputItem(SItem(Material.EMERALD_BLOCK).setDisplayName("§a住人の追加"),Player::class.java,"/<プレイヤー名(/cancelでキャンセル)>") { player, p ->


            p.sendPrefixMsg(SStr("&a申請を送りました"))
            player.sendPrefixMsg(SStr("&a${player.name}から入居の勧誘が来ています！"))
            player.sendPrefixMsg(SStr("&aID:${region.includeName}"))
            Man10RealEstateV2.sInput.clickAccept(player,"§a§l[ここをクリックで入居！]",{
                if (region.players.containsKey(player.uniqueId)){
                    p.sendPrefixMsg(SStr("&cすでにそのプレイヤーは住民です！"))
                    player.sendPrefixMsg(SStr("&cすでに住民です！"))
                    return@clickAccept
                }
                if (region.subUserLimit != null && region.subUserLimit!! <= region.players.size){
                    p.sendPrefixMsg(SStr("&cこの土地にこれ以上プレイヤーは住めません！"))
                    player.sendPrefixMsg(SStr("&cこの土地にこれ以上プレイヤーは住めません！"))
                    return@clickAccept
                } else {
                    val cityLimit = Man10RealEstateV2.cityData[region.city]!!.subUserLimit
                    if (cityLimit != null && cityLimit <= region.players.size){
                        p.sendPrefixMsg(SStr("&cこの土地にこれ以上プレイヤーは住めません！"))
                        player.sendPrefixMsg(SStr("&cこの土地にこれ以上プレイヤーは住めません！"))
                        return@clickAccept
                    }
                }

                val score = Executors.newCachedThreadPool().submit(Callable { ScoreDatabase.getScore(player.uniqueId) }).get()
                if (region.liveScore != null && region.liveScore!! > score){
                    p.sendPrefixMsg(SStr("&cプレイヤーの住むために必要なスコアが足りません！"))
                    player.sendPrefixMsg(SStr("&c住むために必要なスコアが足りません！"))
                    return@clickAccept
                } else {
                    val cityScore = Man10RealEstateV2.cityData[region.city]!!.liveScore
                    if (cityScore != null && cityScore > score){
                        p.sendPrefixMsg(SStr("&cプレイヤーの住むために必要なスコアが足りません！"))
                        player.sendPrefixMsg(SStr("&c住むために必要なスコアが足りません！"))
                        return@clickAccept
                    }
                }

                if (SJavaPlugin.mysql.asyncExecute("insert into user_data (region_id,uuid,mcid,perms,rent) values ('${region.includeName}','${player.uniqueId}','${player.name}','',0.0)")){
                    val userData = UserData()
                    userData.uuid = player.uniqueId
                    userData.mcid = player.name
                    region.players[player.uniqueId] = userData
                    player.sendPrefixMsg(SStr("&a入居が完了しました！"))
                    p.sendPrefixMsg(SStr("&a入居が完了しました！"))
                }
            },{p.sendPrefixMsg(SStr("&c${player.name}は申請を承諾しませんでした"))},60)


        })
        return true
    }
}