import java.sql.Connection

interface DAO {
    val c: Connection

    //select from Users
    //get all users who rated this movie
    fun getUsersByMovieId(id: Int): List<String>

    //select from Movies
    fun getMoviesByActorId(id: Int): List<String>

    //select from personalities
    fun getActorsByMovieId(id: Int): List<String>

    fun updateUsers(id: Int)
    fun updateMovies(id: Int)
    fun updateActors(id: Int)

    fun close() {
        c.close()
    }

}

