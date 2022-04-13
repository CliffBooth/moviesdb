DROP SCHEMA public CASCADE;
CREATE SCHEMA public;

CREATE TABLE IF NOT EXISTS movies (
	id serial NOT NULL PRIMARY KEY,
    title varchar(50) NOT NULL,
    rating real CHECK (rating BETWEEN 1 AND 10),
    date_released date NOT NULL
);

CREATE TABLE IF NOT EXISTS personalities (
	id serial NOT NULL PRIMARY KEY,
    name varchar(50) NOT NULL,
    date_of_birth date NOT NULL,
    age int NOT NULL
);

CREATE TABLE IF NOT EXISTS screenwriter_to_movie (
    id serial NOT NULL PRIMARY KEY,
    personality_id int REFERENCES personalities(id) ON DELETE CASCADE,
    movie_id int REFERENCES movies(id) ON DELETE CASCADE,
    UNIQUE (personality_id, movie_id)
);

CREATE TABLE IF NOT EXISTS actor_to_movie (
    id serial NOT NULL PRIMARY KEY,
    personality_id int REFERENCES personalities(id) ON DELETE CASCADE,
    movie_id int REFERENCES movies(id) ON DELETE CASCADE,
    UNIQUE (personality_id, movie_id)
);

CREATE TABLE IF NOT EXISTS director_to_movie (
    id serial NOT NULL PRIMARY KEY,
    personality_id int REFERENCES personalities(id) ON DELETE CASCADE,
    movie_id int REFERENCES movies(id) ON DELETE CASCADE,
    UNIQUE (personality_id, movie_id)
);

CREATE TABLE IF NOT EXISTS users (
    id serial NOT NULL PRIMARY KEY,
    email varchar(50) NOT NULL UNIQUE,
    username varchar(50) NOT NULL
);

CREATE TABLE IF NOT EXISTS movie_list (
    id serial NOT NULL PRIMARY KEY,
    user_id int REFERENCES users(id) ON DELETE CASCADE,
    list_name varchar NOT NULL
);

CREATE TABLE IF NOT EXISTS rating (
    id serial NOT NULL PRIMARY KEY,
    user_id int REFERENCES users(id) ON DELETE CASCADE,
    movie_id int REFERENCES movies(id) ON DELETE CASCADE,
    rating int NOT NULL CHECK (rating BETWEEN 1 AND 10),
    UNIQUE (user_id, movie_id)
);

CREATE TABLE IF NOT EXISTS review (
    id serial NOT NULL PRIMARY KEY,
    user_id int REFERENCES users(id) ON DELETE CASCADE,
    movie_id int REFERENCES movies(id) ON DELETE CASCADE,
    review text NOT NULL,
    UNIQUE (user_id, movie_id)
);

CREATE TABLE IF NOT EXISTS movie_to_list (
    id serial NOT NULL PRIMARY KEY,
    movie_id int REFERENCES movies(id) ON DELETE CASCADE,
    list_id int REFERENCES movie_list(id) ON DELETE CASCADE,
    UNIQUE (movie_id, list_id)
);

--insert:

insert into movies(title, rating, date_released)
values
    ('movie1',NULL,'2004.2.29'),
    ('movie2',NULL,'2004.2.29'),
    ('movie3',NULL,'2004.2.29');

insert into personalities(name, date_of_birth, age)
values
    ('Johnny Depp','1963.7.9',58),
    ('Joaquin Phoenix','1974.10.28',47),
    ('Quentin Tarantino','1963.3.27',59);

insert into screenwriter_to_movie(personality_id, movie_id)
values (3, 1);

insert into actor_to_movie(personality_id, movie_id)
values 
    (2, 1),
    (1, 1);

insert into director_to_movie(personality_id, movie_id)
values (3, 2);

insert into users(email, username)
values
    ('hello1@gmail.com','user'),
    ('hello2@gmail.com','user'),
    ('hello3@gmail.com','user2'),
    ('hello4@gmail.com','user4');


insert into movie_list(user_id, list_name) values(1,'my_list1');
insert into movie_to_list(movie_id, list_id)
values
    (1, 1),
    (2, 1),
    (3, 1);

insert into rating(user_id, movie_id, rating)
values
    (1, 1, 6),
    (1, 2, 3),
    (1, 3, 10);

insert into review(user_id, movie_id, review)
values
    (1, 1,'mediocre, 6/10'),
    (1, 3, 'one of the best movies I have ever seen!');