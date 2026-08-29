package ru.practicum.moviehub.http;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

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
        } else if (method.equalsIgnoreCase("DELETE")) {
            handleDelete(exchange);
        }
    }

    private void handleGet(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        if (path.equals("/movies")) {
            handleGetAll(exchange);
        } else {
            handleGetById(exchange, path);
        }
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
        Movie movie;
        try {
            movie = gson.fromJson(requestBody, Movie.class);
        } catch (JsonParseException e) {
            ErrorResponse errorResponse = new ErrorResponse("Некорректный JSON");
            sendError(exchange, 400, errorResponse);
            return;
        }
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

    private void handleGetAll(HttpExchange exchange) throws IOException {
        List<Movie> allMovies = store.getAll();
        String responseBody = gson.toJson(allMovies);
        sendJson(exchange, 200, responseBody);
    }

    private void handleGetById(HttpExchange exchange,
                               String path) throws IOException {
        Optional<Long> idOptional = parseIdOrSendError(exchange, path);
        if (idOptional.isEmpty()) {
            return;
        }
        long id = idOptional.get();
        Optional<Movie> movieOptional = store.getById(id);
        if (movieOptional.isEmpty()) {
            ErrorResponse errorResponse = new ErrorResponse("Фильм не найден");
            sendError(exchange, 404, errorResponse);
            return;
        }
        Movie movie = movieOptional.get();
        String responseBody = gson.toJson(movie);
        sendJson(exchange, 200, responseBody);
    }

    private void handleDelete(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        Optional<Long> idOptional = parseIdOrSendError(exchange, path);
        if (idOptional.isEmpty()) {
            return;
        }
        long id = idOptional.get();
        boolean deleted = store.delete(id);
        if (!deleted) {
            ErrorResponse errorResponse = new ErrorResponse("Фильм не найден");
            sendError(exchange, 404, errorResponse);
            return;
        }
        sendNoContent(exchange);
    }

    private Optional<Long> parseIdOrSendError(HttpExchange exchange,
                                              String path) throws IOException {
        String[] pathParts = path.split("/");
        try {
            long id = Long.parseLong(pathParts[2]);
            return Optional.of(id);
        } catch (NumberFormatException e) {
            ErrorResponse errorResponse = new ErrorResponse("Некорректный ID");
            sendError(exchange, 400, errorResponse);
            return Optional.empty();
        }
    }
}
