package cache

import java.sql.Connection
import java.sql.DriverManager
import java.sql.Statement

/**
 * Implements LRU caching algorithm
 */

class WithCache(
    jdbcUrl: String,
    userName: String,
    password: String,
    val capacity: Int = 100,
): DAO() {

    override val c: Connection = DriverManager.getConnection(jdbcUrl, userName, password)
    private val statement: Statement = c.createStatement()

    private val map = HashMap<Key, List<String>>(capacity)
    private val queue = ArrayDeque<Key>(capacity)

    private fun get(key: Key, query: String): List<String> {
        val result = mutableListOf<String>()
        if (queue.contains(key)) {
            queue.remove(key)
            queue.addFirst(key)
        } else {
            val rs = statement.executeQuery(query)
            while (rs.next())
                result += rs.getString(1)

            if (queue.size >= capacity) {
                val removed = queue.removeLast()
                map.remove(removed)
            }
            map[key] = result
            queue.addFirst(key)
        }
        return map[key]!!
    }

    //select queries

    override fun getUsersByMovieId(id: Int): List<String> {
        val query = """
            select username from movies
            join rating on movies.id = rating.movie_id
            join users on rating.user_id = users.id
            where movies.id=$id
        """.trimIndent()
        val key = Users(id)
        return get(key, query)
    }

    override fun getMoviesByActorId(id: Int): List<String> {
        val query = """
            select title from movies
            join actor_to_movie as dtm on movies.id=dtm.movie_id
            join personalities as per on per.id=dtm.personality_id
            where per.id=$id
        """.trimIndent()
        val key = Movies(id)
        return get(key, query)
    }

    override fun getActorsByMovieId(id: Int): List<String> {
        val query = """
            select name from movies
            join actor_to_movie as atm on movies.id = atm.movie_id
            join personalities as p on atm.personality_id = p.id
            where movies.id=$id
        """.trimIndent()
        val key = Actors(id)
        return get(key, query)
    }

    //update queries

    override fun updateUsers(id: Int) {
        val query = """
            update users 
            set username = 'changed username'
            where id=$id
        """.trimIndent()
        val key = Users(id)
        queue.remove(key)
        map.remove(key)
        statement.execute(query)
    }

    override fun updateMovies(id: Int) {
        val query = """
            update movies 
            set title = 'changed title'
            where id=$id
        """.trimIndent()
        val key = Movies(id)
        queue.remove(key)
        map.remove(key)
        statement.execute(query)
    }

    override fun updateActors(id: Int) {
        val query = """
            update personalities 
            set name = 'changed name'
            where id=$id
        """.trimIndent()
        val key = Actors(id)
        queue.remove(key)
        map.remove(key)
        statement.execute(query)
    }

    override fun deleteUser(id: Int) {
        val query = "delete from users where id=$id"
        val key = Users(id)
        queue.remove(key)
        map.remove(key)
        statement.execute(query)
    }

    override fun deleteMovie(id: Int) {
        val query = "delete from movies where id=$id"
        val key = Movies(id)
        queue.remove(key)
        map.remove(key)
        statement.executeUpdate(query)
    }

    override fun deleteActor(id: Int) {
        val query = "delete from personalities where id=$id"
        val key = Actors(id)
        queue.remove(key)
        map.remove(key)
        statement.execute(query)

    }


}