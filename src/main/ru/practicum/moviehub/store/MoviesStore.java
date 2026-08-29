package ru.practicum.moviehub.store;

import ru.practicum.moviehub.model.Movie;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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

    public List<Movie> getAll() {
        return List.copyOf(movies.values());
    }

    public Optional<Movie> getById(long id) {
        return Optional.ofNullable(movies.get(id));
    }

    public boolean delete(long id) {
        return movies.remove(id) != null;
    }
}
