package tororo1066.man10realestatev2.convert

import com.google.gson.Gson
import com.google.gson.JsonObject
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.configuration.file.YamlConfiguration
import tororo1066.man10realestatev2.Man10RealEstateV2
import tororo1066.man10realestatev2.data.CityData
import tororo1066.man10realestatev2.data.RegionData
import tororo1066.man10realestatev2.data.enumData.TaxCycle
import tororo1066.tororopluginapi.SJavaPlugin
import tororo1066.tororopluginapi.mysql.SMySQL
import tororo1066.tororopluginapi.mysql.SMySQLResultSet
import java.io.File
import java.sql.Connection
import java.sql.DriverManager
import java.sql.ResultSet
import java.sql.SQLException
import java.sql.Statement

class MREConvert() {

    private var conn: Connection? = null
    private var stmt: Statement? = null

    fun convert(): Boolean {
        val mre = Bukkit.getPluginManager().getPlugin("Man10RealEstate")?:return false
        val conf = mre.config
        Class.forName("com.mysql.cj.jdbc.Driver")
        conn = DriverManager.getConnection("jdbc:mysql://${conf.getString("mysql.host")}:${conf.getString("mysql.port")}/${conf.getString("mysql.db")}?useSSL=false", conf.getString("mysql.user"), conf.getString("mysql.pass"))
        convert()
        val cityFolder = File(mre.dataFolder.path + "/city/")
        val gson = Gson()
        for (city in cityFolder.listFiles()?:return false){
            val json = gson.fromJson(city.readText(), JsonObject::class.java)
            val yaml = YamlConfiguration()
            yaml.set("name", json["name"].asString)
            yaml.set("world", json["world"].asString)
            yaml.set("startLoc", json["startX"].asString + "," + json["startY"].asString + "," + json["startZ"].asString)
            yaml.set("endLoc", json["endX"].asString + "," + json["endY"].asString + "," + json["endZ"].asString)
            yaml.set("tax", json["tax"].asDouble)
            yaml.set("userLimit", json["maxUser"].asInt)
            yaml.set("buyScore", json["buyScore"].asInt)
            yaml.set("liveScore", json["liveScore"].asInt)
            yaml.set("defaultPrice", json["defaultPrice"].asDouble)
            yaml.set("taxCycle", TaxCycle.MONTH_CHANGED.name)
            SJavaPlugin.sConfig.saveConfig(yaml, "city/${city.nameWithoutExtension}")
        }
        Man10RealEstateV2.plugin.reload()

        val regions = sQuery("select * from region")
        regions.forEach { rs ->
            val loc = Location(null, rs.getDouble("x"), rs.getDouble("y"), rs.getDouble("z"))
            val city = CityData.findCity(loc)?:return@forEach
            SJavaPlugin.mysql.execute("insert into region_data (city,includeName,displayName,tax,userLimit,buyScore,liveScore,defaultPrice,startLoc,endLoc,price,ownerUUID,state,rentCycle,lastTax,taxCycle,lastRent,teleportLoc) " +
                    "values('${city.includeName}','${rs.getInt("id")}','${rs.getInt("id")}',-1,-1,-1,-1,-1,'${rs.getInt("sx")},${rs.getInt("sy")},${rs.getInt("sz")}','${rs.getInt("ex")},${rs.getInt("ey")},${rs.getInt("ez")}'," +
                    "${rs.getDouble("price")},'${if (rs.getString("owner_uuid") == "null") "none" else rs.getString("owner_uuid")}','${rs.getString("status").uppercase()}','${when(rs.getInt("span")){0->RegionData.RentCycle.DAY_30;1->RegionData.RentCycle.DAY_7;2->RegionData.RentCycle.DAY_1;else->RegionData.RentCycle.NONE}}'," +
                    "${mre.config.getInt("lastTax") / 1000},'${TaxCycle.MONTH_CHANGED}',${mre.config.getInt("lastTax") / 1000},'${rs.getInt("x")},${rs.getInt("y")},${rs.getInt("z")},${rs.getInt("yaw")},${rs.getInt("pitch")}')")
        }

        return true
    }

    private fun query(query : String): ResultSet? {
        if (conn == null){
            return null
        }

        return try {
            stmt = conn!!.createStatement()
            stmt!!.executeQuery(query)
        } catch (e : SQLException) {
            Bukkit.getLogger().warning("QueryError：Error Code(${e.errorCode})\nError Message\n${e.message}")
            Bukkit.getLogger().warning(query)
            null
        }
    }

    private fun sQuery(query: String): ArrayList<SMySQLResultSet>{
        val rs = query(query)?:return arrayListOf()
        val result = ArrayList<SMySQLResultSet>()
        try {
            while (rs.next()){
                val meta = rs.metaData
                val data = HashMap<String,Any>()
                for (i in 1 until meta.columnCount + 1) {
                    val name = meta.getColumnName(i)
                    data[name] = rs.getObject(name)
                }
                result.add(SMySQLResultSet(data))
            }
            rs.close()
            stmt?.close()
            conn?.close()
            return result
        }catch (e : Exception){
            e.printStackTrace()
            return arrayListOf()
        }
    }
}