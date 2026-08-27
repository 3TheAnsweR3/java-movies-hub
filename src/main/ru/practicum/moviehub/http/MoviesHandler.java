package ru.practicum.moviehub.http;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import ru.practicum.moviehub.model.Movie;
import ru.practicum.moviehub.store.MoviesStore;

public class MoviesHandler extends BaseHttpHandler {

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        if (method.equalsIgnoreCase("GET")) {
            List<Movie> allMovies = store.getAll();
            String responseBody = gson.toJson(allMovies);
            sendJson(exchange, 200, responseBody);
        } else if (method.equalsIgnoreCase("POST")) {
            byte[] requestBytes = exchange.getRequestBody().readAllBytes();
            String requestBody = new String(requestBytes, StandardCharsets.UTF_8);
            Movie movie = gson.fromJson(requestBody, Movie.class);
            Movie createdMovie = store.add(movie);
            String responseBody = gson.toJson(createdMovie);
            sendJson(exchange, 201, responseBody);
        }
    }

    private final MoviesStore store;
    private final Gson gson = new Gson();
    public MoviesHandler(MoviesStore store) {
        this.store = store;
    }
}