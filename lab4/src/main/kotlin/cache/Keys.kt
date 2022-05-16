package cache

interface Key

data class Actors(val id: Int): Key
data class Movies(val id: Int): Key
data class Users(val id: Int): Key