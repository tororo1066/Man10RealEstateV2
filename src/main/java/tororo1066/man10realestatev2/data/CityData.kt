package tororo1066.man10realestatev2.data

import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.World
import org.bukkit.configuration.file.YamlConfiguration
import tororo1066.man10realestatev2.Man10RealEstateV2
import tororo1066.man10realestatev2.data.enumData.TaxCycle
import tororo1066.tororopluginapi.SJavaPlugin
import java.io.File

class CityData {

    lateinit var world: World

    var includeName = ""
    var displayName = ""

    var tax = 0.0
    var taxCycle = TaxCycle.NONE

    var subUserLimit: Int? = null

    var buyScore: Int? = null
    var liveScore: Int? = null

    var defaultPrice = 0.0

    var regionLimit: Int? = null

    lateinit var startLoc: Triple<Int,Int,Int>
    lateinit var endLoc: Triple<Int,Int,Int>

    val regions = HashMap<String,RegionData>()

    var loaded = false

    companion object{
        fun loadFromYml(file: File): CityData {
            val yaml = YamlConfiguration.loadConfiguration(file)
            val data = CityData()
            data.includeName = file.nameWithoutExtension
            data.displayName = yaml.getString("name")?:file.nameWithoutExtension
            data.world = Bukkit.getWorld(yaml.getString("world","world")!!)?:return data
            val startLoc = (yaml.getString("startLoc")?:return data).split(",").map { it.toInt() }
            data.startLoc = Triple(startLoc[0],startLoc[1],startLoc[2])
            val endLoc = (yaml.getString("endLoc")?:return data).split(",").map { it.toInt() }
            data.endLoc = Triple(endLoc[0],endLoc[1],endLoc[2])
            data.tax = yaml.getDouble("tax",0.0)
            val subUserLimit = yaml.getInt("userLimit",-1)
            data.subUserLimit = if (subUserLimit != -1) subUserLimit else null
            val buyScore = yaml.getInt("buyScore",-1)
            data.buyScore = if (buyScore != -1) buyScore else null
            val liveScore = yaml.getInt("liveScore",-1)
            data.liveScore = if (liveScore != -1) liveScore else null
            data.defaultPrice = yaml.getDouble("defaultPrice",0.0)
            data.taxCycle = TaxCycle.valueOf(yaml.getString("taxCycle","NONE")!!)

            val rs = SJavaPlugin.mysql.asyncQuery("select * from region_data where city = '${data.includeName}'")

            rs.forEach {
                val region = RegionData.loadFromSQL(it,data.world,data.includeName)
                data.regions[region.includeName] = region
            }

            data.loaded = true
            return data
        }

        fun findCity(loc: Location): CityData? {
            Man10RealEstateV2.cityData.forEach { (_, data) ->
                if (Man10RealEstateV2.inRegion(loc,data.startLoc,data.endLoc))return data
            }
            return null
        }
    }
}