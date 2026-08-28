package ru.practicum.moviehub.http;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import ru.practicum.moviehub.api.ErrorResponse;
import ru.practicum.moviehub.model.Movie;
import ru.practicum.moviehub.store.MoviesStore;

public class MoviesHandler extends BaseHttpHandler {

    private final MoviesStore store;
    private final Gson gson = new Gson();

    public MoviesHandler(MoviesStore store) {
        this.store = store;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        if (method.equalsIgnoreCase("GET")) {
            handleGet(exchange);
        } else if (method.equalsIgnoreCase("POST")) {
            handlePost(exchange);
        }
    }

    private void handleGet(HttpExchange exchange) throws IOException {
        List<Movie> allMovies = store.getAll();
        String responseBody = gson.toJson(allMovies);
        sendJson(exchange, 200, responseBody);
    }

    private void handlePost(HttpExchange exchange) throws IOException {
        byte[] requestBytes = exchange.getRequestBody().readAllBytes();
        String requestBody = new String(requestBytes, StandardCharsets.UTF_8);
        Movie movie = gson.fromJson(requestBody, Movie.class);
        if (movie.getTitle() == null || movie.getTitle().isBlank()) {
            ErrorResponse resp = new ErrorResponse("Ошибка валидации",
                    List.of("название не должно быть пустым"));
            String responseBody = gson.toJson(resp);
            sendJson(exchange, 422, responseBody);
            return;
        }
        Movie createdMovie = store.add(movie);
        String responseBody = gson.toJson(createdMovie);
        sendJson(exchange, 201, responseBody);
    }
}