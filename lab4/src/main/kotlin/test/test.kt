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

    moviesIds.apply {
        val rs = statement.executeQuery("select id from movies")
        while (rs.next())
            add(rs.getInt("id"))
    }

    actorIds.apply {
        val rs = statement.executeQuery("select id from personalities")
        while (rs.next())
            add(rs.getInt("id"))
    }

    userIds.apply {
        val rs = statement.executeQuery("select id from users")
        while (rs.next())
            add(rs.getInt("id"))
    }

    val cacheSize = DBSize * 2
    val smallCacheSize = DBSize * 1

    //primarily select
    var bigCache = WithCache(jdbcUrl, userName, password, cacheSize)
    var smallCache = WithCache(jdbcUrl, userName, password, smallCacheSize)
    var withoutCache = WithoutCache(jdbcUrl, userName, password)
    println("1) Primarily Select")
    println("===without cache===")
    test(withoutCache, 100, 2, 0)
    println("\n===cache = ${smallCache.capacity}===")
    test(smallCache, 100, 2, 0)
    println("\n===cache = ${bigCache.capacity}===")
    test(bigCache, 100, 2, 0)
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
    test(withoutCache, 2, 100, 0)
    println("\n===cache = ${smallCache.capacity}===")
    test(smallCache, 2, 100, 0)
    println("\n===cache = ${bigCache.capacity}===")
    test(bigCache, 2, 100, 0)
    println("-------------------------------")
    bigCache.close()
    smallCache.close()
    withoutCache.close()

    //primarily delete
    iterations = DBSize
    bigCache = WithCache(jdbcUrl, userName, password, cacheSize)
    smallCache = WithCache(jdbcUrl, userName, password, smallCacheSize)
    withoutCache = WithoutCache(jdbcUrl, userName, password)
    println("\n2) Primarily Delete")
    Generator.generate()
    println("===without cache===")
    test(withoutCache, 1, 2, 100)

    Generator.generate()
    println("\n===cache = ${smallCache.capacity}===")
    test(smallCache, 1, 2, 100)

    Generator.generate()
    println("\n===cache = ${bigCache.capacity}===")
    test(bigCache, 1, 2, 100)
    println("-------------------------------")
    bigCache.close()
    smallCache.close()
    withoutCache.close()

}

fun test(dao: DAO, select: Int, update: Int, delete: Int) {
    val list = listOf(select, update, delete).sorted()

    val times = mutableListOf<Long>()

    fun addTime(action: Int) {
        when (action) {
            select -> times += select(dao)
            update -> times += update(dao)
            delete -> times += delete(dao)
        }
    }

    for (i in 1..iterations) {
        val operation = Math.random() * list.sum()
        if (operation < list[0]) {
            addTime(list[0])
        } else if (operation < list[0] + list[1]) {
            addTime(list[1])
        } else {
            addTime(list[2])
        }
    }

    printTimes(times)

}

fun select(dao: DAO): Long {
    val r = Math.random()

    val start = System.nanoTime()
    if (r < 1.0 / 3) {
        dao.getUsersByMovieId(moviesIds.random())
    } else if (r < 2.0 / 3) {
        dao.getActorsByMovieId(moviesIds.random())
    } else {
        dao.getMoviesByActorId(actorIds.random())
    }

    return (System.nanoTime() - start)
}

fun update(dao: DAO): Long {
    val r = Math.random()

    val start = System.nanoTime()
    if (r < 1.0 / 3) {
        dao.updateMovies(moviesIds.random())
    } else if (r < 2.0 / 3) {
        dao.updateUsers(userIds.random())
    } else {
        dao.updateActors(actorIds.random())
    }

    return (System.nanoTime() - start)
}

fun delete(dao: DAO): Long {
    val r = Math.random()

    val start = System.nanoTime()
    if (r < 1.0 / 3) {
        val id = moviesIds.random()
        moviesIds.remove(id)
        dao.deleteMovie(id)
    } else if (r < 2.0 / 3) {
        val id = userIds.random()
        userIds.remove(id)
        dao.deleteUser(id)
    } else {
        val id = actorIds.random()
        actorIds.remove(id)
        dao.deleteActor(id)
    }

    printTableSizes()
    return (System.nanoTime() - start)

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
