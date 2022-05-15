import org.postgresql.util.PSQLException
import java.sql.Connection
import java.sql.DriverManager
import java.sql.Statement
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.random.Random

const val jdbcUrl = "jdbc:postgresql://localhost:5432/movies_db"
const val userName = "postgres"
const val password = "1"

const val firstNameFile = "names.txt"
const val secondNameFile = "secondNames.txt"
const val titlesFile = "movie_titles.txt"

val firstNames = HashSet<String>().apply {
    Adder::class.java.getResourceAsStream(firstNameFile)!!.bufferedReader().useLines { addAll(it) }
}

val secondNames = HashSet<String>().apply {
    Adder::class.java.getResourceAsStream(secondNameFile)!!.bufferedReader().useLines { addAll(it) }
}

val titles = HashSet<String>().apply {
    Adder::class.java.getResourceAsStream(titlesFile)!!.bufferedReader().useLines { addAll(it) }
}

const val entriesNumber = 5_000

val c: Connection = DriverManager.getConnection(jdbcUrl, userName, password)
val statement: Statement = c.createStatement()

fun main() {
//    Adder.clearTable()
    Adder.addUsers(entriesNumber)
    println("users")

    Adder.addPersonalities(entriesNumber)
    println("personalities")

    Adder.addMovies(entriesNumber)
    println("movies")

    Adder.addPersonalityToMovie(entriesNumber, "director_to_movie")
    println("director")
    Adder.addPersonalityToMovie(entriesNumber, "actor_to_movie")
    println("actor")
    Adder.addPersonalityToMovie(entriesNumber, "screenwriter_to_movie")
    println("screenwriter")

    Adder.addRating(entriesNumber)
    println("rating")

    Adder.addReviews(entriesNumber)
    println("review")

    Adder.addMovieLists(entriesNumber)
    println("movieList")

    Adder.addMovieToLists(entriesNumber)
    println("movie to list")
}

object Adder {

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
        val charPool = "q,w,e,r,t,y,u,i,o,p,a,s,d,f,g,h,j,k,l,z,x,c,v,b,n,m, "
            .split(",")
            .dropLastWhile { !whiteSpace && it == " " }
        val length = (1..maxLength).random()
        return (0..length)
            .map { Random.nextInt(0, charPool.size) }.joinToString("", transform = charPool::get)
    }

    fun addUsers(number: Int) {
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

    fun addPersonalities(number: Int) {
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

    fun addMovies(number: Int) {
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

    fun addPersonalityToMovie(number: Int, table: String) {
        val existingPairs = mutableSetOf<Pair<Int, Int>>()
        val rs = statement.executeQuery("SELECT * FROM $table")
        while (rs.next()) {
            existingPairs.add(Pair(rs.getInt("personality_id"), rs.getInt("movie_id")))
        }

        val ids = getUniqueIdPairs(existingPairs,"personalities", "movies", number)
        for ((personalityId, movieId) in ids)
            statement.execute("INSERT INTO $table(personality_id, movie_id) VALUES('$personalityId','$movieId')")
    }

    fun addRating(number: Int) {
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

    fun addReviews(number: Int) {
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

    fun addMovieLists(number: Int) {
        val userIds = getIds("users")
        repeat(number) {
            val name = getRandomString(15)
            val userId = userIds.random()
            statement.execute("INSERT INTO movie_list(user_id, list_name) VALUES('$userId','$name')")
        }
    }

    fun addMovieToLists(number: Int) {
        val existingPairs = mutableSetOf<Pair<Int, Int>>()
        val rs = statement.executeQuery("SELECT * FROM movie_to_list")
        while (rs.next()) {
            existingPairs.add(Pair(rs.getInt("movie_id"), rs.getInt("list_id")))
        }

        val ids = getUniqueIdPairs(existingPairs,"movies", "movie_list", number)
        for ((movieId, listId) in ids)
            statement.execute("INSERT INTO movie_to_list(movie_id, list_id)VALUES('$movieId','$listId')")
    }


    fun clearTable() {
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