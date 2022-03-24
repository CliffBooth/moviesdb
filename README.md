# MoviesDB

## Описание
Сервис, предоставляющий информацию о фильмах по типу Кинопоиска или IMDb. На нем могут регистрироваться пользователи, ставить оценки фильмам, писать рецензии на фильмы и составлять подборки фильмов.
## Структура базы данных
1. Movies (id, title, genre, rating, date_released) - информация о фильме
2. Personalities (id, name, date_of_birth, age) - информация о человеке (это может быть актер, режиссер, сценарист или всё сразу)
3. Director_to_movie (personality_id, movie_id) - отношение many-to-many между Режиссером фильмом
4. Actor_to_movie (personality_id, movie_id) - отношение many-to-many между Актером фильмом
5. Screenwriter_to_movie (personality_id, movie_id) - отношение many-to-many между Сценаристом фильмом
6. User (id, email, nickname) - пользователь сайта, может ставить оценку фильмам и составлять подборки фильмов.
7. Rating (id, user_id, movie_id, rating) - оценка пользователя фильму
8. Review (id, user_id, movie_id, content) - рецензия пользователя на фильмам
9. Movie_list (id, user_id, list_name) - подборка фильмов, которую может создать пользователь
10. Movie_to_list (list_id, movie_id) - отношение many-to-many между подборкой фильмов и фильмом

# Схема базы данных

![schema](./bd_schema.png)

