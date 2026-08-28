package ru.practicum.moviehub.http;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

import ru.practicum.moviehub.api.ErrorResponse;
import ru.practicum.moviehub.model.Movie;
import ru.practicum.moviehub.store.MoviesStore;

public class MoviesHandler extends BaseHttpHandler {
    private static final int MAX_TITLE_LENGTH = 100;
    private static final int MIN_RELEASE_YEAR = 1888;

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
        String contentType = exchange.getRequestHeaders().getFirst("Content-Type");
        boolean isJsonContentType = contentType != null
                && contentType.split(";", 2)[0]
                        .trim()
                        .equalsIgnoreCase("application/json");
        if (!isJsonContentType) {
            ErrorResponse errorResponse =
                    new ErrorResponse("Неподдерживаемый Content-Type");
            sendError(exchange, 415, errorResponse);
            return;
        }
        byte[] requestBytes = exchange.getRequestBody().readAllBytes();
        String requestBody = new String(requestBytes, StandardCharsets.UTF_8);
        Movie movie = gson.fromJson(requestBody, Movie.class);
        String title = movie.getTitle();
        if (title == null || title.isBlank()) {
            sendValidationError(exchange,
                    "название не должно быть пустым");
            return;
        }
        if (title.length() > MAX_TITLE_LENGTH) {
            sendValidationError(exchange,
                    "название не должно быть длиннее "
                            + MAX_TITLE_LENGTH + " символов");
            return;
        }
        int year = movie.getYear();
        int maxYear = LocalDate.now().getYear() + 1;
        if (year < MIN_RELEASE_YEAR || year > maxYear) {
            sendValidationError(exchange,
                    "год должен быть между "
                            + MIN_RELEASE_YEAR + " и " + maxYear);
            return;
        }
        Movie createdMovie = store.add(movie);
        String responseBody = gson.toJson(createdMovie);
        sendJson(exchange, 201, responseBody);
    }

    private void sendValidationError(HttpExchange exchange,
                                     String detail) throws IOException {
        ErrorResponse errorResponse = new ErrorResponse("Ошибка валидации",
                List.of(detail));
        sendError(exchange, 422, errorResponse);
    }

    private void sendError(HttpExchange exchange, int status,
                           ErrorResponse errorResponse) throws IOException {
        String responseBody = gson.toJson(errorResponse);
        sendJson(exchange, status, responseBody);
    }
}
