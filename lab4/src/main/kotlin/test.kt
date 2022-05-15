import java.sql.Connection
import java.sql.DriverManager
import java.sql.Statement

//database should be full

//create DAO with big cache
//with small cache (maybe add middle-sized cache)
//and without cache (different class)
const val jdbcUrl = "jdbc:postgresql://localhost:5432/movies_db"
const val userName = "postgres"
const val password = "1"

const val iterations = 1_000
val c: Connection = DriverManager.getConnection(jdbcUrl, userName, password)
private val statement: Statement = c.createStatement()

fun main() {
    val withCache = WithCache(jdbcUrl, userName, password)
    val withoutCache = WithoutCache(jdbcUrl, userName, password)

    //GET THE DIFFERENCE!!!
//    println("1) Select")
//    println("===with cache===")
//    testSelect(withCache)
//    println("===without cache===")
//    testSelect(withoutCache)

//    println("2) Update")
//    println("===with cache===")
//    testSelect(withCache)
//    println("===without cache===")
//    testSelect(withoutCache)

    testSelect(withCache)
}

fun testSelect(dao: DAO) {
    //I need a method to start from the same db (or do I actually?)
//    generateDatabase()
    val moviesIds = mutableListOf<Int>().apply {
        val rs = statement.executeQuery("select id from movies")
        while (rs.next())
            add(rs.getInt("id"))
    }

    val times = listOf<Int>()

    for (iteration in 1..iterations) {
//        val start =
    }

}

fun testUpdate(dao: DAO) {

}