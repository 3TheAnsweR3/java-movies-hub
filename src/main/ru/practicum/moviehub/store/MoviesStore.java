package ru.practicum.moviehub.store;

import ru.practicum.moviehub.model.Movie;

import java.util.HashMap;
import java.util.Map;

public class MoviesStore {
    private final Map<Long, Movie> movies = new HashMap<>();
    private long nextId = 1L;

    public Movie add(Movie movie) {
        long assignedId = nextId;
        movie.setId(assignedId);
        movies.put(assignedId, movie);
        nextId++;
        return movie;
    }

    public void clear() {
        movies.clear();
        nextId = 1L;
    }
}