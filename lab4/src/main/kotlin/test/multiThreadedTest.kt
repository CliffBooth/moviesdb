package test

import cache.*
import kotlinx.coroutines.*
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit

const val coroutinesNumber = 10_000


fun main() {
//    Generator.generate()
    fillIdLists()

    val ratios = listOf(.2, .5, .8)
    for ((index, r) in ratios.withIndex()) {
        println("${index+1}) select ratio = $r")
        runWithRatio(r)
        print("\n\n")
    }
}

fun runWithRatio(selectRatio: Double) {
    val cacheSize = (DBSize * 3)
    var cache = WithCache(jdbcUrl, userName, password, cacheSize)
    var withoutCache = WithoutCache(jdbcUrl, userName, password)

    println("===with cache===")
    runTests(cache, selectRatio)
    println("\n===without cache===")
    runTests(withoutCache, selectRatio)
}

fun runTests(dao: DAO, selectRatio: Double) {
    val updateFunctions = MutableList(((coroutinesNumber * (1 - selectRatio)).toInt())) {
        listOf(
            UpdateByMovies(dao),
            UpdateByUsers(dao),
            UpdateByActors(dao)
        ).random()
    }
    val selectFunctions = MutableList(((coroutinesNumber * selectRatio).toInt())) {
        listOf(
            SelectActors(dao),
            SelectMovies(dao),
            SelectUsers(dao)
        ).random()
    }
    val functions = mutableListOf<TestFunction>().apply {
        addAll(updateFunctions)
        addAll(selectFunctions)
    }

    functions.shuffle()

    //coroutines
    val jobs = mutableListOf<Job>()
    functions.forEach {
        jobs.add(GlobalScope.launch {
            it.run()
        })
    }
    jobs.forEach {
        runBlocking {
            it.join()
        }
    }
    //executor
//    val executorService = Executors.newCachedThreadPool()
//    val futures = mutableListOf<Future<out Any>>()
//    for (f in functions) {
//        futures += executorService.submit { f.run() }
//    }
//    executorService.shutdown()
//    executorService.awaitTermination(24, TimeUnit.HOURS)

    val times = mutableListOf<Long>()
    selectFunctions.forEach {
        times += it.time
    }
    printTimes(times)
}
