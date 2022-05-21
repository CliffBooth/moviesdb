package test

import cache.DAO

abstract class TestFunction: () -> Unit {
    open fun run() {
        this.invoke()
    }
}

abstract class TestSelect: TestFunction() {
    var time = 0L
    override fun run() {
        val start = System.nanoTime()
        this.invoke()
        time = System.nanoTime() - start
    }
}

class SelectUsers(val dao: DAO): TestSelect() {
    override fun invoke() {
        dao.getUsersByMovieId(moviesIds.random())
    }
}
class SelectActors(val dao: DAO): TestSelect() {
    override fun invoke() {
        dao.getActorsByMovieId(moviesIds.random())
    }
}
class SelectMovies(val dao: DAO): TestSelect() {
    override fun invoke() {
        dao.getMoviesByActorId(actorIds.random())
    }
}


class UpdateByMovies(val dao: DAO): TestFunction() {
    override fun invoke() {
        dao.updateMovies(moviesIds.random())
    }
}

class UpdateByUsers(val dao: DAO): TestFunction() {
    override fun invoke() {
        dao.updateUsers(userIds.random())
    }
}
class UpdateByActors(val dao: DAO): TestFunction() {
    override fun invoke() {
        dao.updateActors(actorIds.random())
    }
}
