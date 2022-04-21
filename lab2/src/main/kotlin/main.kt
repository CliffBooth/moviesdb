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

const val entriesNumber = 5000

val firstNames = HashSet<String>().apply {
    User::class.java.getResourceAsStream(firstNameFile)!!.bufferedReader().useLines { addAll(it) }
}

val secondNames = HashSet<String>().apply {
    User::class.java.getResourceAsStream(secondNameFile)!!.bufferedReader().useLines { addAll(it) }
}

fun main() {
    val c = DriverManager.getConnection(jdbcUrl, userName, password)
    val statement = c.createStatement()

    clearTable(statement)

    getUsers(entriesNumber).forEach {
        statement.execute("INSERT INTO users(email, username) VALUES('${it.email}','${it.userName}')")
    }
    println("users")

    getPersonalities(entriesNumber).forEach {
        val q = """
            INSERT INTO personalities(name, date_of_birth, age)
            VALUES('${it.name}','${it.dateOfBirth}','${it.age}')
        """.trimIndent()
        statement.execute(q)
    }
    println("personalities")

    getMovies(entriesNumber).forEach {
        statement.execute("INSERT INTO movies(title, date_released) VALUES('${it.title}','${it.dateReleased}')")
    }
    println("movies")

    getPersonalityToMovie(statement, entriesNumber).forEach {
        statement.execute("INSERT INTO screenwriter_to_movie(personality_id, movie_id) " +
                "VALUES('${it.personalityId}','${it.movieId}')")
    }
    println("screenwriter")

    getPersonalityToMovie(statement, entriesNumber).forEach {
        statement.execute("INSERT INTO director_to_movie(personality_id, movie_id) " +
                "VALUES('${it.personalityId}','${it.movieId}')")
    }
    println("director")

    getPersonalityToMovie(statement, entriesNumber).forEach {
        statement.execute("INSERT INTO actor_to_movie(personality_id, movie_id) " +
                "VALUES('${it.personalityId}','${it.movieId}')")
    }
    println("actor")

    getRating(statement, entriesNumber).forEach {
        statement.execute("INSERT INTO rating(user_id, movie_id, rating) " +
                "VALUES('${it.userId}','${it.movieId}','${it.rating}')")
    }
    println("rating")

    getReviews(statement, entriesNumber).forEach {
        statement.execute("INSERT INTO review(user_id, movie_id, review) " +
                "VALUES('${it.userId}','${it.movieId}','${it.review}')")
    }
    println("review")

    getMovieLists(statement, entriesNumber).forEach {
        statement.execute("INSERT INTO movie_list(user_id, list_name) VALUES('${it.userId}','${it.name}')")
    }
    println("movieList")

    getMovieToLists(statement, entriesNumber).forEach {
        statement.execute("INSERT INTO movie_to_list(movie_id, list_id) " +
                "VALUES('${it.movieId}','${it.listId}')")
    }
    println("movie to list")
}

fun getUsers(number: Int) = HashSet<User>().apply {
    var emailCounter: Long = 1
    repeat(number) {
        val email = "email${emailCounter++}@mail.com"
        add(User(email = email, userName = firstNames.random()))
    }
}

fun getPersonalities(number: Int) = HashSet<Personality>().apply {
    repeat(number) {
        val name = "${firstNames.random()} ${secondNames.random()}"
        val dayOffset = (0..ChronoUnit.DAYS.between(
            LocalDate.of(1940, 1, 1),
            LocalDate.of(2004, 1, 1)
        )).random()
        val date = LocalDate.of(1940, 1, 1).plusDays(dayOffset)
        val age = ChronoUnit.YEARS.between(date, LocalDate.now()).toInt()
        add(Personality(name = name, dateOfBirth = date.toString(), age = age))
    }
}

fun getMovies(number: Int) = HashSet<Movie>().apply {
    val titles = HashSet<String>().apply {
        User::class.java.getResourceAsStream(titlesFile)!!.bufferedReader().useLines { addAll(it) }
    }
    repeat(number) {
        val dayOffset = (0..ChronoUnit.DAYS.between(
            LocalDate.of(1940, 1, 1),
            LocalDate.of(2022, 4, 21)
        )).random()
        val date = LocalDate.of(1940, 1, 1).plusDays(dayOffset)
        val title = titles.random().replace("'", "''")
        add(Movie(title = title, dateReleased = date.toString()))
    }
}

private fun getIds(statement: Statement, table: String) = HashSet<Int>().apply {
    val rs = statement.executeQuery("SELECT id FROM $table")
    while (rs.next()) {
        add(rs.getInt("id"))
    }
}

private fun getRandomString(maxLength: Int): String {
    val charPool = "q,w,e,r,t,y,u,i,o,p,a,s,d,f,g,h,j,k,l,z,x,c,v,b,n,m, ".split(",")
    val length = (1..maxLength).random()
    return (0..length)
        .map { Random.nextInt(0, charPool.size) }.joinToString("", transform = charPool::get)
}


fun getPersonalityToMovie(statement: Statement, number: Int): Set<PersonalityToMovie> {
    val personalityIds = getIds(statement, "personalities")
    val movieIds = getIds(statement, "movies")
    val inserted = mutableSetOf<PersonalityToMovie>()
    repeat(number) {
        var ptm: PersonalityToMovie?
        do {
            ptm = PersonalityToMovie(personalityIds.random(), movieIds.random())
        } while (inserted.contains(ptm))
        inserted.add(ptm!!)
    }
    return inserted
}

fun getRating(statement: Statement, number: Int): Set<Rating> {
    val movieIds = getIds(statement, "movies")
    val userIds = getIds(statement, "users")
    val inserted = mutableSetOf<Pair<Int, Int>>()
    val result = mutableSetOf<Rating>()
    repeat(number) {
        var userId: Int?
        var movieId: Int?
        do {
            userId = userIds.random()
            movieId = movieIds.random()
        } while (inserted.contains(Pair(userId!!, movieId!!)))
        inserted.add(Pair(userId, movieId))
        result.add(Rating(userId, movieId, (1..10).random()))
    }
    return result
}

fun getReviews(statement: Statement, number: Int): Set<Review> {
    val movieIds = getIds(statement, "movies")
    val userIds = getIds(statement, "users")
    val result = mutableSetOf<Review>()
    val inserted = mutableSetOf<Pair<Int, Int>>()
    repeat(number) {
        var userId: Int?
        var movieId: Int?
        do {
            userId = userIds.random()
            movieId = movieIds.random()
        } while (inserted.contains(Pair(userId!!, movieId!!)))
        inserted.add(Pair(userId, movieId))
        val review = getRandomString(200)
        result.add(Review(userId, movieId, review))
    }
    return result
}

fun getMovieLists(statement: Statement, number: Int) = HashSet<MovieList>().apply {
    val userIds = getIds(statement, "users")
    repeat(number) {
        add(MovieList(userIds.random(), getRandomString(15)))
    }
}

fun getMovieToLists(statement: Statement, number: Int): Set<MovieToList> {
    val movieIds = getIds(statement, "movies")
    val listIds = getIds(statement, "movie_list")
    val inserted = mutableSetOf<MovieToList>()
    repeat(number) {
        var mtl: MovieToList?
        do {
            mtl = MovieToList(movieIds.random(), listIds.random())
        } while (inserted.contains(mtl))
        inserted.add(mtl!!)
    }
    return inserted
}


fun clearTable(statement: Statement) {
    val tables = listOf(
        "personalities",
        "screenwriter_to_movie",
        "movies",
        "actor_to_movie",
        "director_to_movie",
        "screenwriter_to_movie",
        "users",
        "movie_list",
        "rating",
        "review",
    )
    tables.forEach { statement.execute("DELETE FROM $it") }
}

data class Personality(val name: String, val dateOfBirth: String, val age: Int)

data class Movie(val title: String, val dateReleased: String)

data class User(val email: String, val userName: String)

data class PersonalityToMovie(val personalityId: Int, val movieId: Int)

data class Rating(val userId: Int, val movieId: Int, val rating: Int)

data class Review(val userId: Int, val movieId: Int, val review: String)

data class MovieList(val userId: Int, val name: String)

data class MovieToList(val movieId: Int, val listId: Int)
