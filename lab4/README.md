# Лабораторная работа 4: Кэширование

## Цели работы
Знакомство студентов с алгоритмами кэширования.
В рамках данной работы необходимо разработать кэширующий SQL-proxy - программу, которая
принимала бы запросы к БД, отправляла эти запросы в БД, сохраняла бы результаты в хранилище. 
Если приходит повторный запрос на чтение - выдавала запросы из хранилища, если приходит 
запрос на изменение - сбрасывала бы значения всех запросов, результаты которых станут 
неактуальными после внесенных изменений.

## Ход работы
Программа была реализована на языке `kotlin`, 
работа с базой данных осуществляется через `jdbc` (без использования ORM библиотек).
### Реализация кэширования
(package ```cache```)

Вся работа с базой данных осуществляется через API определенным в абстрактном классе `DAO`
```kotlin
abstract class DAO {
    protected abstract val c: Connection
    abstract fun getUsersByMovieId(id: Int): List<String>
    abstract fun getMoviesByActorId(id: Int): List<String>
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
```
Мы можем взаимодействовать с 3 таблицами в базе данных: users, movies и personalities.
Для каждой из этих таблиц у нас есть метод, посылающий `SELECT`, `UPDATE` и `DELETE` запрос в базу данных.

У класса `DAO` 2 наследника: `WithoutCache` - взаимодействие с базой данных происходит без кэширования 
и `WithCache` - результаты `SELECT` запросов сохраняются в кэше, при вызове методов на изменение, результаты, которые
станут неактуальными после выполнения запроса удаляются из кэша.

Кэш был реализован по алгоритму LRU, с использованием `ReentrantLock` для потокобезопасности.
```kotlin
class WithCache(
    jdbcUrl: String,
    userName: String,
    password: String,
    val capacity,
): DAO() {
    override val c: Connection = DriverManager.getConnection(jdbcUrl, userName, password)
    private val map = HashMap<Key, List<String>>(capacity)
    private val queue = ArrayDeque<Key>(capacity)
    private val lock = ReentrantLock()
    
    private fun get(key: Key, query: String): List<String> {
        val result = mutableListOf<String>()
        lock.lock()
        if (queue.contains(key)) {
            result.addAll(map[key]!!)
            queue.remove(key)
            queue.addFirst(key)
        } else {
            val statement: Statement = c.createStatement()
            val rs = statement.executeQuery(query)
            while (rs.next())
                result += rs.getString(1)
            if (queue.size >= capacity) {
                val removed = queue.removeLast()
                map.remove(removed)
            }
            map[key] = result
            queue.addFirst(key)
            statement.close()
        }
        lock.unlock()
        return result
    }

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
    //other get methods...
    

    override fun updateUsers(id: Int) {
        val query = """
            update users 
            set username = 'changed username'
            where id=$id
        """.trimIndent()
        val key = Users(id)
        lock.lock()
        queue.remove(key)
        map.remove(key)
        execute(query)
        lock.unlock()
    }
    //other update methods...

    override fun deleteUser(id: Int) {
        val query = "delete from users where id=$id"
        val key = Users(id)
        lock.lock()
        queue.remove(key)
        map.remove(key)
        execute(query)
        lock.unlock()
    }
    //other delete methods... 

    private fun execute(query: String) {
        val statement: Statement = c.createStatement()
        statement.execute(query)
        statement.close()
    }

}
```
В качестве ключей для сохранения результатов `SELECT` запросов в `HashMap` я использовал обертку над id по которому
выполняется запрос. Для Запроса к каждой таблице нам нужен свой вид ключа:
```kotlin
interface Key

data class Actors(val id: Int): Key
data class Movies(val id: Int): Key
data class Users(val id: Int): Key
```

### <b><u>Реализация Тестирования</u></b>
Тестирование кэша в условиях многопоточности реализовано в файле `multiThreadedTest.kt`

В `main` методе вызывается метод `runWithRatio` - на трех разных значениях `selectRatio` - это
отношение `SELECT` запросов к количеству запросов в тестировании.
```kotlin
fun main() {
    fillIdLists()

    val ratios = listOf(.2, .5, .8)
    for ((index, r) in ratios.withIndex()) {
        println("${index+1}) select ratio = $r")
        runWithRatio(r)
        print("\n\n")
    }
}
```
Метод `runWithRatio` создает 2 объекта DAO: с использованием кэширования и без, и вмести с параметром `selectRatio` передает их по очереди в метод
`runTests`

```kotlin
fun runWithRatio(selectRatio: Double) {
    val cacheSize = (DBSize * 3)
    var cache = WithCache(jdbcUrl, userName, password, cacheSize)
    var withoutCache = WithoutCache(jdbcUrl, userName, password)

    println("===with cache===")
    runTests(cache, selectRatio)
    println("\n===without cache===")
    runTests(withoutCache, selectRatio)
}
```
В методе `runTests` создаётся 2 списка объектов-функций: 1 из случайных `SELECT` функций, другой -
 из случайных `UPDATE` функций: 

```kotlin
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
    //...

```
Классы - функции описаны в файле `testingFunctions.kt`:
```kotlin
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
//the rest of select functions...

class UpdateByMovies(val dao: DAO): TestFunction() {
    override fun invoke() {
        dao.updateMovies(moviesIds.random())
    }
}
//the rest of update functions...
}
```
Все эти классы вызывают соответствующий метод на пераданном в них `DAO` в методе `run()`. В тестировании, нас интерисует
скорость выполнения `SELECT` запросов, поэтому классы - функции, вызывающие `SELECT` запрос в `DAO`, имеют поле
`time` - в котором будет сохраняться время выполнения данного метода.

Далее, в методе `runTests()` я запускаю все эти методы параллельно с помощью корутин, после чего сохраняю значения
времени выполнения всех `SELECT` запросов в список и вывожу максималоьное, минимальное и среднее значение из этого списка
(функция `printTimes()`).
```kotlin
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
    val times = mutableListOf<Long>()
    selectFunctions.forEach {
        times += it.time
    }
    printTimes(times)
```

## Результаты исследования
Значение `coroutinesNumber` = 10_000

<b>SELECT = 20% UPDATE = 80%</b>

Attempt | With Cache | Without Cache | 
--- | --- | --- |
fastest | <b>17 401 ns</b> | 290 200 ns
slowest | 34 994 001 ns | <b>8 118 300 ns</b>
average |1 943 617 ns| <b>974 614 ns</b>

<b>SELECT = 50% UPDATE = 50%</b>

Attempt | With Cache | Without Cache | 
--- | --- | --- |
fastest | <b>9 900 ns</b> | 286 300 ns
slowest | 78 887 801 ns | <b>21 877 399 ns</b>
average |1 715 506 ns| <b>1 541 201 ns</b>

<b>SELECT = 80% UPDATE = 20%</b>

Attempt | With Cache | Without Cache | 
--- | --- | --- |
fastest | <b>6 700 ns</b> | 287 401 ns
slowest | 50 617 700 ns | <b>16 735 600 ns</b>
average |<b>1 324 949 ns</b>| 1 696 467 ns

Из результатов тестирования видно, что:

При преимущественно `UPDATE` запросах - реализация обращения в базу данных без кэширования работает быстрее.
(Причем самый быстрый запрос в реализации с кэшированием, но и самый медленный тоже в реализации с кэшированием)

При преимущественно `SELCT` запросах - наоборот реализация с кэшированием работает быстрее (при этом самый быстрый
и самый медленный запрос также в реализации с кэшированием).

При преимущественно `SELCT` запросах кэширование выигрывает потому, что обращение в кэш гораздо быстрее обращения в базу данных.

При преимущественно `UPDATE` запросах кэширование проигрывает потому, что при получении `UPDATE` запроса доступ в
кэш для всех потоков блокируется (В том числе и для `SELECT`), пока запрос не будет выполнен и кэш изменен. В результате
чего получается, что при большом количестве `UPDATE` запросов, у нас будет маленькая вероятность кэш-хита (то есть все равно
придется обращаться в базу данных), так еще и потоки будут часто ждать и ничего не делать.