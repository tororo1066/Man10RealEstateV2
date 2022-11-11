package tororo1066.man10realestatev2.data

import org.bukkit.Bukkit
import tororo1066.man10realestatev2.Man10RealEstateV2
import tororo1066.tororopluginapi.SJavaPlugin
import tororo1066.tororopluginapi.mysql.SMySQLResultSet
import java.util.*

class UserData {
    lateinit var uuid: UUID
    var mcid = ""

    var perms = ArrayList<Perm>()
    var rent = 0.0

    enum class Perm{
        ALL,
        BLOCK,
        INVENTORY,
        INTERACT
    }

    companion object{
        fun loadFromSQL(result: SMySQLResultSet): UserData {
            val data = UserData()
            data.uuid = UUID.fromString(result.getString("uuid"))
            data.mcid = result.getString("mcid")
            if (Man10RealEstateV2.plugin.config.getBoolean("checkDatabasePlayerName")){
                val name = Bukkit.getOfflinePlayer(data.uuid).name
                if (name != null && name != data.mcid){
                    data.mcid = name
                    SJavaPlugin.mysql.execute("update user_data set mcid = '${name}' where uuid = '${data.uuid}'")
                }
            }
            data.perms = ArrayList(result.getString("perms").split(",").map { Perm.valueOf(it) })
            data.rent = result.getDouble("rent")
            return data
        }
    }
}