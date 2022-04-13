import java.io.BufferedReader
import java.io.FileReader
import java.io.FileWriter
import java.sql.Date
import java.sql.DriverManager

fun main_() {
    val fileName = "secondNames.txt"
    var set = mutableSetOf<String>()
    val fileReader = BufferedReader(FileReader(fileName))
    set.addAll(fileReader.readLines())

    fileReader.close()
    val fileWriter = FileWriter(fileName)
    set = HashSet(set.map { it.lowercase().capitalize() })
    set.forEach { fileWriter.write("$it\n") }
    fileWriter.close()
}

val firstNameFile = "names.txt"
val secondNameFile = "secondNames.txt"
val emailsFile = "emails.txt"

val firstNames = HashSet<String>().apply {
    val reader = BufferedReader(FileReader(firstNameFile))
    addAll(reader.readLines())
    reader.close()
}

val secondNames = HashSet<String>().apply {
    val reader = BufferedReader(FileReader(secondNameFile))
    addAll(reader.readLines())
    reader.close()
}

val emails = HashSet<String>().apply {
    val reader = BufferedReader(FileReader(emailsFile))
    addAll(reader.readLines())
    reader.close()
}

fun getUsers(number: Int): Set<User> = HashSet<User>().apply {
    repeat(number) {
        val email = emails.random()
        emails.remove(email)
        add(User(email = email, userName = firstNames.random()))
    }
}

fun main() {
    val jdbcUrl = "jdbc:postgresql://localhost:5432/movies_db"
    val userName = "postgres"
    val password = "1"
    val c = DriverManager.getConnection(jdbcUrl, userName, password)
    val statement = c.createStatement()

    val users = getUsers(5)
    for (user in users) {
        val email = user.email
        val username = user.userName
        val q = "INSERT INTO users(email, username) VALUES('$email','$username')"
        statement.execute(q)
    }

    val query = "SELECT * FROM users"
    val result = statement.executeQuery(query)
    val userSet = mutableSetOf<User>()
    while (result.next()) {
        val id = result.getInt("id")
        val email = result.getString("email")
        val name = result.getString("username")
        userSet += User(id, email, name)
    }
    userSet.forEach { println(it) }
}

data class Customer(
    val id: Int,
    val first_name: String,
    val last_name: String,
    val email: String,
)

data class Personality(
    val id: Int,
    val name: String,
    val dateOfBirth: Date,
    val age: Int
)

data class Movie(
    val id: Int,
    val title: String,
    val rating: Double,
    val dateReleased: Date
)

data class User(
    var id: Int = -1,
    val email: String,
    val userName: String
)
