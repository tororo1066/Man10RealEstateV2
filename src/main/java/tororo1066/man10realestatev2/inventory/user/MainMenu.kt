package tororo1066.man10realestatev2.inventory.user

import org.bukkit.Material
import org.bukkit.entity.Player
import tororo1066.man10realestatev2.Man10RealEstateV2
import tororo1066.man10realestatev2.data.RegionData
import tororo1066.tororopluginapi.defaultMenus.LargeSInventory
import tororo1066.tororopluginapi.sInventory.SInventory
import tororo1066.tororopluginapi.sInventory.SInventoryItem

class MainMenu: SInventory(Man10RealEstateV2.plugin,"§aMainMenu",1) {

    override fun renderMenu(): Boolean {
        setItem(2, SInventoryItem(Material.PAPER).setDisplayName("§a持っている土地を管理する").setCanClick(false).setClickEvent { e ->
            object : LargeSInventory(plugin,"§a土地一覧") {
                override fun renderMenu(): Boolean {
                    val filtered = ArrayList<RegionData>()
                    Man10RealEstateV2.cityData.values.forEach { data ->
                        filtered.addAll(data.regions.values.filter { filter -> filter.ownerUUID == e.whoClicked.uniqueId })
                    }

                    setResourceItems(ArrayList(filtered.map { map -> SInventoryItem(Material.PAPER).setDisplayName(map.includeName).setCanClick(false).setClickEvent {
                        RegionSettingMenu(map).open(it.whoClicked as Player)
                    }}))

                    return true
                }
            }.open(e.whoClicked as Player)
        })
        return true
    }
}