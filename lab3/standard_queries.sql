--1
create or replace view titles_v as select id, title from movies
select * from personalities;
select * from screenwriter_to_movie;
select * from actor_to_movie;
select * from director_to_movie;
select * from users;
select * from movie_list;
select * from rating;
select * from review;
select * from movie_to_list;

--2
select * from personalities
    where name like 'Nic%'

select * from personalities
    where age between 30 and 40
    order by age

select * from movies
    where rating in (1, 3, 5, 7, 9) 
    order by rating

--3
select COUNT(*) from movies
    where rating >= 8

select AVG(rating) as avg_rating from movies
    where date_released < '1990.1.1'

--4
select * from movies
order by rating DESC, date_released ASC

--5
select 
    MAX(age) as oldest,
    MIN(age) as younges,
    AVG(age) as average
    from personalities

--6
select users.id, users.username, movie_list.list_name from users
    join movie_list on users.id=movie_list.user_id
    order by users.id

select per.id, per.name, movies.title from movies
    join director_to_movie as dtm on movies.id=dtm.movie_id
    join personalities as per on dtm.personality_id=per.id
    order by per.id

--7
select rating, count(*) as number from movies
    group by rating
    having count(*) > 450
    order by rating

--8
--select reviews where rating > 7
select * from review
    where user_id in (select user_id from rating where rating > 7)

--9
insert into movies(title) values ('movie1','2004.2.29');
insert into personalities(name, date_of_birth, age) values ('Brad Pitt','1963.12.18',58);
insert into screenwriter_to_movie(personality_id, movie_id) values (3, 1);
insert into actor_to_movie(personality_id, movie_id) values (2, 1);
insert into director_to_movie(personality_id, movie_id) values (3, 2);
insert into users(email, username) values ('hello1@gmail.com','user'),
insert into movie_list(user_id, list_name) values(1,'my_list1');
insert into movie_to_list(movie_id, list_id) values (1, 1);
insert into rating(user_id, movie_id, rating) values (1, 1, 6);
insert into review(user_id, movie_id, review) values (1, 1,'mediocre, 6/10'),

--10
update movies
    set title = 'old movie', rating = 0
    where date_released < '1950.1.1'

--11
delete from rating 
    where rating = (select MIN(rating) from rating)

--12
--delete all users who don't have a list
delete from users
    where id not in (select user_id from movie_list)