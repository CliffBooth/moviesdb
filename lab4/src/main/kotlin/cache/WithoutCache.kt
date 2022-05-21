package cache

import java.sql.Connection
import java.sql.DriverManager
import java.sql.Statement

class WithoutCache(
    jdbcUrl: String,
    userName: String,
    password: String,
): DAO() {
    override val c: Connection = DriverManager.getConnection(jdbcUrl, userName, password)

    private fun get(query: String): List<String> {
        val statement: Statement = c.createStatement()
        val result = mutableListOf<String>()
        val rs = statement.executeQuery(query)
        while (rs.next())
            result += rs.getString(1)
        statement.close()
        return result
    }

    override fun getUsersByMovieId(id: Int): List<String> {
        val query = """
            select username from movies
            join rating on movies.id = rating.movie_id
            join users on rating.user_id = users.id
            where movies.id=$id
        """.trimIndent()
        return get(query)
    }

    override fun getMoviesByActorId(id: Int): List<String> {
        val query = """
            select title from movies
            join actor_to_movie as dtm on movies.id=dtm.movie_id
            join personalities as per on per.id=dtm.personality_id
            where per.id=$id
        """.trimIndent()
        return get(query)
    }

    override fun getActorsByMovieId(id: Int): List<String> {
        val query = """
            select name from movies
            join actor_to_movie as atm on movies.id = atm.movie_id
            join personalities as p on atm.personality_id = p.id
            where movies.id=$id
        """.trimIndent()
        return get(query)
        execute(query)
    }

    override fun updateUsers(id: Int) {
        val query = """
            update users 
            set username = 'changed username'
            where id=$id
        """.trimIndent()
        execute(query)
    }

    @Synchronized override fun updateMovies(id: Int) {
        val query = """
            update movies 
            set title = 'changed title'
            where id=$id
        """.trimIndent()
        execute(query)
    }

    override fun updateActors(id: Int) {
        val query = """
            update personalities 
            set name = 'changed name'
            where id=$id
        """.trimIndent()
        execute(query)
    }

    override fun deleteUser(id: Int) {
        val query = "delete from users where id=$id"
        execute(query)
    }

    override fun deleteMovie(id: Int) {
        val query = "delete from movies where id=$id"
        execute(query)
    }

    override fun deleteActor(id: Int) {
        val query = "delete from personalities where id=$id"
        execute(query)
    }

    fun execute(query: String) {
        val statement: Statement = c.createStatement()
        statement.execute(query)
        statement.close()
    }
}