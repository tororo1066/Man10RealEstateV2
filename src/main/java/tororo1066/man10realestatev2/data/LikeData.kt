package tororo1066.man10realestatev2.data

import tororo1066.tororopluginapi.mysql.SMySQLResultSet
import java.util.*
import kotlin.collections.ArrayList

class LikeData {
    lateinit var uuid: UUID
    var mcid = ""

    val likes = ArrayList<String>()

    companion object{
        fun fromDB(result: List<SMySQLResultSet>): LikeData? {
            val data = LikeData()
            result.forEachIndexed { i, rs ->
                if (i == 0){
                    data.uuid = UUID.fromString(rs.getString("uuid"))
                    data.mcid = rs.getString("mcid")
                }
                data.likes.add(rs.getString("region_id"))
            }

            if (data.mcid == "")return null

            return data
        }
    }
}