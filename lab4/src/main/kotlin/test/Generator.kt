package test

import org.postgresql.util.PSQLException
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.random.Random

object Generator {
    private const val firstNameFile = "names.txt"
    private const val secondNameFile = "secondNames.txt"
    private const val titlesFile = "movie_titles.txt"

    private val firstNames = HashSet<String>().apply {
        Generator.javaClass.classLoader.getResourceAsStream(firstNameFile)!!.bufferedReader().useLines { addAll(it) }
    }

    private val secondNames = HashSet<String>().apply {
        Generator.javaClass.classLoader.getResourceAsStream(secondNameFile)!!.bufferedReader().useLines { addAll(it) }
    }

    private val titles = HashSet<String>().apply {
        Generator.javaClass.classLoader.getResourceAsStream(titlesFile)!!.bufferedReader().useLines { addAll(it) }
    }

    fun generate() {
        clearTable()
        val sb = StringBuilder("generating database... ")
        val l = sb.length + 10
        print(sb.toString().padEnd(l) + "|\r")
        addUsers(DBSize)
        print(sb.append("#").toString().padEnd(l) + "|\r")
        addPersonalities(DBSize)
        print(sb.append("#").toString().padEnd(l) + "|\r")
        addMovies(DBSize)
        print(sb.append("#").toString().padEnd(l) + "|\r")
        addPersonalityToMovie(DBSize, "director_to_movie")
        print(sb.append("#").toString().padEnd(l) + "|\r")
        addPersonalityToMovie(DBSize, "actor_to_movie")
        print(sb.append("#").toString().padEnd(l) + "|\r")
        addPersonalityToMovie(DBSize, "screenwriter_to_movie")
        print(sb.append("#").toString().padEnd(l) + "|\r")
        addRating(DBSize)
        print(sb.append("#").toString().padEnd(l) + "|\r")
        addReviews(DBSize)
        print(sb.append("#").toString().padEnd(l) + "|\r")
        addMovieLists(DBSize)
        print(sb.append("#").toString().padEnd(l) + "|\r")
        addMovieToLists(DBSize)
        println(sb.append("#|").toString())
    }

    private fun getIds(table: String) = HashSet<Int>().apply {
        val rs = statement.executeQuery("SELECT id FROM $table")
        while (rs.next()) {
            add(rs.getInt("id"))
        }
    }

    private fun getUniqueIdPairs(existingPairs: Set<Pair<Int, Int>>, table1: String, table2: String, number: Int) =
        mutableSetOf<Pair<Int, Int>>().apply {
            val ids1 = getIds(table1)
            val ids2 = getIds(table2)
            repeat(number) {
                var id1: Int?
                var id2: Int?
                do {
                    id1 = ids1.random()
                    id2 = ids2.random()
                } while (contains(Pair(id1!!, id2!!)) || existingPairs.contains(Pair(id1, id2)))
                add(Pair(id1, id2))
            }
        }

    private fun getRandomString(maxLength: Int, whiteSpace: Boolean = true): String {
        val charPool = "q,w,e,r,t,y,u,i,o,p,a,s,d,f,g,h,j,k,l,z,x,test.getC,v,b,n,m, "
            .split(",")
            .dropLastWhile { !whiteSpace && it == " " }
        val length = (1..maxLength).random()
        return (0..length)
            .map { Random.nextInt(0, charPool.size) }.joinToString("", transform = charPool::get)
    }

    private fun addUsers(number: Int) {
        var i = 0
        while (i < number) {
            i++
            val email = "${getRandomString(30, false)}@mail.com"
            try {
                statement.execute("INSERT INTO users(email, username) VALUES('$email','${firstNames.random()}')")
            } catch (e: PSQLException) {
                i--
                continue
            }
        }
    }

    private fun addPersonalities(number: Int) {
        repeat(number) {
            val name = "${firstNames.random()} ${secondNames.random()}"
            val dayOffset = (0..ChronoUnit.DAYS.between(
                LocalDate.of(1940, 1, 1),
                LocalDate.of(2004, 1, 1)
            )).random()
            val date = LocalDate.of(1940, 1, 1).plusDays(dayOffset)
            val age = ChronoUnit.YEARS.between(date, LocalDate.now()).toInt()
            statement.execute("INSERT INTO personalities(name, date_of_birth, age) VALUES('$name','$date','$age')")
        }
    }

    private fun addMovies(number: Int) {
        repeat(number) {
            val dayOffset = (0..ChronoUnit.DAYS.between(
                LocalDate.of(1940, 1, 1),
                LocalDate.of(2022, 4, 21)
            )).random()
            val date = LocalDate.of(1940, 1, 1).plusDays(dayOffset)
            val title = titles.random().replace("'", "''")
            val rating = (0..10).random()
            statement.execute("INSERT INTO movies(title, rating, date_released) VALUES('$title',$rating,'$date')")
        }
    }

    private fun addPersonalityToMovie(number: Int, table: String) {
        val existingPairs = mutableSetOf<Pair<Int, Int>>()
        val rs = statement.executeQuery("SELECT * FROM $table")
        while (rs.next()) {
            existingPairs.add(Pair(rs.getInt("personality_id"), rs.getInt("movie_id")))
        }

        val ids = getUniqueIdPairs(existingPairs,"personalities", "movies", number)
        for ((personalityId, movieId) in ids)
            statement.execute("INSERT INTO $table(personality_id, movie_id) VALUES('$personalityId','$movieId')")
    }

    private fun addRating(number: Int) {
        val existingPairs = mutableSetOf<Pair<Int, Int>>()
        val rs = statement.executeQuery("SELECT * FROM rating")
        while (rs.next()) {
            existingPairs.add(Pair(rs.getInt("user_id"), rs.getInt("movie_id")))
        }

        val ids = getUniqueIdPairs(existingPairs, "users", "movies", number)
        for ((userId, movieId) in ids) {
            val rating = (1..10).random()
            statement.execute("INSERT INTO rating(user_id, movie_id, rating) VALUES('$userId','$movieId','$rating')")
        }
    }

    private fun addReviews(number: Int) {
        val existingPairs = mutableSetOf<Pair<Int, Int>>()
        val rs = statement.executeQuery("SELECT * FROM review")
        while (rs.next()) {
            existingPairs.add(Pair(rs.getInt("user_id"), rs.getInt("movie_id")))
        }

        val ids = getUniqueIdPairs(existingPairs, "users", "movies", number)
        for ((userId, movieId) in ids) {
            val review = getRandomString(200)
            statement.execute("INSERT INTO review(user_id, movie_id, review) VALUES('$userId','$movieId','$review')")
        }
    }

    private fun addMovieLists(number: Int) {
        val userIds = getIds("users")
        repeat(number) {
            val name = getRandomString(15)
            val userId = userIds.random()
            statement.execute("INSERT INTO movie_list(user_id, list_name) VALUES('$userId','$name')")
        }
    }

    private fun addMovieToLists(number: Int) {
        val existingPairs = mutableSetOf<Pair<Int, Int>>()
        val rs = statement.executeQuery("SELECT * FROM movie_to_list")
        while (rs.next()) {
            existingPairs.add(Pair(rs.getInt("movie_id"), rs.getInt("list_id")))
        }

        val ids = getUniqueIdPairs(existingPairs,"movies", "movie_list", number)
        for ((movieId, listId) in ids)
            statement.execute("INSERT INTO movie_to_list(movie_id, list_id)VALUES('$movieId','$listId')")
    }


    private fun clearTable() {
        val tables = listOf(
            "movies",
            "personalities",
            "screenwriter_to_movie",
            "actor_to_movie",
            "director_to_movie",
            "users",
            "movie_list",
            "rating",
            "review",
            "movie_to_list"
        )
        tables.forEach { statement.execute("DELETE FROM $it") }
    }
}