package tororo1066.man10realestatev2.data

import org.bukkit.Location
import org.bukkit.World
import org.bukkit.entity.Player
import red.man10.man10score.ScoreDatabase
import tororo1066.man10realestatev2.Man10RealEstateV2
import tororo1066.man10realestatev2.Man10RealEstateV2.Companion.sendPrefixMsg
import tororo1066.man10realestatev2.data.enumData.TaxCycle
import tororo1066.tororopluginapi.SJavaPlugin
import tororo1066.tororopluginapi.SStr
import tororo1066.tororopluginapi.mysql.SMySQLResultSet
import java.util.*
import java.util.concurrent.Callable
import java.util.concurrent.Executors

class RegionData {

    lateinit var world: World

    var includeName = ""
    var displayName = ""
    var city = ""

    var tax: Double? = null
    var taxCycle = TaxCycle.NONE
    var lastTax = 0L

    var subUserLimit: Int? = null

    var buyScore: Int? = null
    var liveScore: Int? = null

    var defaultPrice = 0.0

    lateinit var startLoc: Triple<Int,Int,Int>
    lateinit var endLoc: Triple<Int,Int,Int>

    var ownerUUID: UUID? = null
    var price = 0.0
    var state = State.ONSALE
    var rentCycle = RentCycle.NONE
    var lastRent = 0L
    val players = HashMap<UUID,UserData>()

    var teleportLoc: Location? = null


    enum class RentCycle(val displayName: String, val amount: Long){
        NONE("無し",0),
        DAY_1("1日ごと",1440),
        DAY_7("1週間ごと",1440 * 7),
        DAY_15("15日ごと",1440 * 15),
        DAY_30("30日ごと",1440 * 30),
        MONTH_CHANGED("月の変わり目",0)
    }

    enum class State(val displayName: String){
        PROTECTED("保護"),
        ONSALE("販売中"),
        FREE("フリー"),
        DANGER("危険"),
        LOCK("ロック")
    }

    fun buyRegion(p: Player): Boolean {
        if (state != State.ONSALE && state != State.LOCK){
            p.sendPrefixMsg(SStr("&cこの土地は販売されていません！"))
            return false
        }
        if (!p.hasPermission("mrev2.buyregion.${city.lowercase()}")){
            p.sendPrefixMsg(SStr("&c土地を買う権限がありません！"))
            return false
        }
        val city = Man10RealEstateV2.cityData[city]!!
        if (city.regionLimit != null && city.regions.filter { it.value.ownerUUID == p.uniqueId }.size >= city.regionLimit!!){
            p.sendPrefixMsg(SStr("&cここの町の土地の所持数が上限に達しています！"))
            return false
        }
        val score = Executors.newCachedThreadPool().submit(Callable { ScoreDatabase.getScore(p.uniqueId) }).get()
        if ((city.buyScore != null && city.buyScore!! > score) || (buyScore != null && buyScore!! > score)){
            p.sendPrefixMsg(SStr("&cこの土地を持つためのスコアが足りません！"))
            return false
        }
        if (SJavaPlugin.vault.withdraw(p.uniqueId,price)){
            state = State.PROTECTED
            if (ownerUUID != null){
                if (players.remove(ownerUUID) != null){
                    SJavaPlugin.mysql.asyncExecute("delete from user_data where uuid = '${ownerUUID}' and region_id = '${includeName}'")
                }
            }
            if (players.remove(p.uniqueId) != null){
                SJavaPlugin.mysql.asyncExecute("delete from user_data where uuid = '${p.uniqueId}' and region_id = '${includeName}'")
            }
            if (SJavaPlugin.mysql.asyncExecute("update region_data set ownerUUID = '${p.uniqueId}', state = 'PROTECTED' where includeName = '${includeName}'")){
                ownerUUID = p.uniqueId
            }
            p.sendPrefixMsg(SStr("&a土地の購入に成功しました！"))
            return true
        } else {
            p.sendPrefixMsg(SStr("&cお金が足りません！"))
            return false
        }
    }

    companion object{
        fun loadFromSQL(result: SMySQLResultSet, world: World, city: String): RegionData {
            val data = RegionData()
            data.world = world
            data.city = city
            data.includeName = result.getString("includeName")
            data.displayName = result.getString("displayName")
            data.tax = if (result.getDouble("tax") == -1.0) null else result.getDouble("tax")
            data.subUserLimit = if (result.getInt("userLimit") == -1) null else result.getInt("userLimit")
            data.buyScore = if (result.getInt("buyScore") == -1) null else result.getInt("buyScore")
            data.liveScore = if (result.getInt("liveScore") == -1) null else result.getInt("liveScore")
            data.defaultPrice = result.getDouble("defaultPrice")
            val startLoc = result.getString("startLoc").split(",").map { it.toInt() }
            data.startLoc = Triple(startLoc[0],startLoc[1],startLoc[2])
            val endLoc = result.getString("endLoc").split(",").map { it.toInt() }
            data.endLoc = Triple(endLoc[0],endLoc[1],endLoc[2])
            data.price = result.getDouble("price")
            data.ownerUUID = if (result.getString("ownerUUID") == "none") null else UUID.fromString(result.getString("ownerUUID"))
            data.state = State.valueOf(result.getString("state"))
            data.rentCycle = RentCycle.valueOf(result.getString("rentCycle"))
            data.taxCycle = TaxCycle.valueOf(result.getString("taxCycle"))
            data.lastTax = result.getLong("lastTax")
            data.lastRent = result.getLong("lastRent")
            if (result.getString("teleportLoc") != "none"){
                val locMap = result.getString("teleportLoc").split(",").map { it.toDouble() }
                data.teleportLoc = Location(world,locMap[0],locMap[1],locMap[2],locMap[3].toFloat(),locMap[4].toFloat())
            }

            val rs = SJavaPlugin.mysql.asyncQuery("select * from user_data where region_id = '${data.includeName}'")

            rs.forEach {
                val userData = UserData.loadFromSQL(it)
                data.players[userData.uuid] = userData
            }

            return data
        }
    }
}