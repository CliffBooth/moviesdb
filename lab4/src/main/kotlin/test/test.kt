package test

import cache.*
import java.sql.Connection
import java.sql.DriverManager
import java.sql.Statement
import java.text.DecimalFormat

const val jdbcUrl = "jdbc:postgresql://localhost:5432/movies_db"
const val userName = "postgres"
const val password = "1"
const val DBSize = 2_000

var iterations = 50_000

val c: Connection = DriverManager.getConnection(jdbcUrl, userName, password)
val statement: Statement = c.createStatement()

val moviesIds = mutableListOf<Int>()
val actorIds = mutableListOf<Int>()
val userIds = mutableListOf<Int>()

fun main() {
    Generator.generate()

    fillIdLists()

    val cacheSize = DBSize * 2
    val smallCacheSize = (DBSize * .2).toInt()

    //primarily select
    var bigCache = WithCache(jdbcUrl, userName, password, cacheSize)
    var smallCache = WithCache(jdbcUrl, userName, password, smallCacheSize)
    var withoutCache = WithoutCache(jdbcUrl, userName, password)
    println("1) Primarily Select")
    println("===without cache===")
    test(withoutCache, 100, 0, 0)
    println("\n===cache = ${smallCache.capacity}===")
    test(smallCache, 100, 0, 0)
    println("\n===cache = ${bigCache.capacity}===")
    test(bigCache, 100, 0, 0)
    println("-------------------------------")
    bigCache.close()
    smallCache.close()
    withoutCache.close()

    //primarily update
    bigCache = WithCache(jdbcUrl, userName, password, cacheSize)
    smallCache = WithCache(jdbcUrl, userName, password, smallCacheSize)
    withoutCache = WithoutCache(jdbcUrl, userName, password)
    println("\n2) Primarily Update")
    println("===without cache===")
    test(withoutCache, 0, 100, 0)
    println("\n===cache = ${smallCache.capacity}===")
    test(smallCache, 0, 100, 0)
    println("\n===cache = ${bigCache.capacity}===")
    test(bigCache, 0, 100, 0)
    println("-------------------------------")
    bigCache.close()
    smallCache.close()
    withoutCache.close()

    //primarily delete
    iterations = DBSize / 2
    bigCache = WithCache(jdbcUrl, userName, password, cacheSize)
    smallCache = WithCache(jdbcUrl, userName, password, smallCacheSize)
    withoutCache = WithoutCache(jdbcUrl, userName, password)
    println("\n2) Primarily Delete")
    Generator.generate()
    fillIdLists()
    println("===without cache===")
    test(withoutCache, 0, 0, 100)

    Generator.generate()
    fillIdLists()
    println("\n===cache = ${smallCache.capacity}===")
    test(smallCache, 0, 0, 100)

    Generator.generate()
    fillIdLists()
    println("\n===cache = ${bigCache.capacity}===")
    test(bigCache, 0, 0, 100)
    println("-------------------------------")
    bigCache.close()
    smallCache.close()
    withoutCache.close()

}

fun test(dao: DAO, select: Int, update: Int, delete: Int) {
    val list = listOf(select to 's', update to 'u', delete to 'd').sortedBy { it.first }

    val times = mutableListOf<Long>()

    fun addTime(action: Char) {
        when (action) {
            's' -> times += select(dao)
            'u' -> times += update(dao)
            'd' -> times += delete(dao)
        }
    }

    for (i in 1..iterations) {
        val operation = Math.random() * list.sumOf { it.first }
        if (operation < list[0].first) {
            addTime(list[0].second)
        } else if (operation < list[0].first + list[1].first) {
            addTime(list[1].second)
        } else {
            addTime(list[2].second)
        }
    }

    printTimes(times)

}

fun select(dao: DAO): Long {
    val start = System.nanoTime()
    when (listOf(1, 2, 3).random()){
        1 -> dao.getUsersByMovieId(moviesIds.random())
        2 -> dao.getActorsByMovieId(moviesIds.random())
        3 -> dao.getMoviesByActorId(actorIds.random())
    }
    return (System.nanoTime() - start)
}

fun update(dao: DAO): Long {
    val start = System.nanoTime()
    when (listOf(1, 2, 3).random()){
        1 -> dao.updateMovies(moviesIds.random())
        2 -> dao.updateUsers(userIds.random())
        3 -> dao.updateActors(actorIds.random())
    }
    return (System.nanoTime() - start)
}

fun delete(dao: DAO): Long {
    val start = System.nanoTime()
    when (listOf(1, 2, 3).random()){
        1 -> {
            val id = moviesIds.random()
            moviesIds.remove(id)
            dao.deleteMovie(id)
        }
        2 -> {
            val id = userIds.random()
            userIds.remove(id)
            dao.deleteUser(id)
        }
        3 -> {
            val id = actorIds.random()
            actorIds.remove(id)
            dao.deleteActor(id)
        }
    }
    printTableSizes()
    return (System.nanoTime() - start)

}

fun fillIdLists() {
    moviesIds.apply {
        clear()
        val rs = statement.executeQuery("select id from movies")
        while (rs.next())
            add(rs.getInt("id"))
    }

    actorIds.apply {
        clear()
        val rs = statement.executeQuery("select id from personalities")
        while (rs.next())
            add(rs.getInt("id"))
    }

    userIds.apply {
        clear()
        val rs = statement.executeQuery("select id from users")
        while (rs.next())
            add(rs.getInt("id"))
    }
}


fun printTimes(times: List<Long>) {
    val format = DecimalFormat("###,###")
    println("fastest: ${format.format(times.minOrNull())} ns")
    println("slowest: ${format.format(times.maxOrNull())} ns")
    println("average: ${format.format(times.sum() / times.size)} ns")
}


fun printTableSizes() {
    print("actors: ${actorIds.size}, movies: ${moviesIds.size}, users: ${userIds.size}\r")
}
