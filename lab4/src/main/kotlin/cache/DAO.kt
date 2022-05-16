package cache

import java.sql.Connection

abstract class DAO {
    protected abstract val c: Connection

    //select from cache.Users
    //get all users who rated this movie
    abstract fun getUsersByMovieId(id: Int): List<String>

    //select from cache.Movies
    abstract fun getMoviesByActorId(id: Int): List<String>

    //select from personalities
    abstract fun getActorsByMovieId(id: Int): List<String>

    abstract fun updateUsers(id: Int)
    abstract fun updateMovies(id: Int)
    abstract fun updateActors(id: Int)

    abstract fun deleteUser(id: Int)
    abstract fun deleteMovie(id: Int)
    abstract fun deleteActor(id: Int)

    fun close() {
        c.close()
    }

}

