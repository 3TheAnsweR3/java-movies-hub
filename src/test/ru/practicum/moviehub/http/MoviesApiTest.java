package ru.practicum.moviehub.http;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.practicum.moviehub.model.Movie;
import ru.practicum.moviehub.store.MoviesStore;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MoviesApiTest {
    private static final String BASE = "http://localhost:8080";
    private static MoviesStore store;
    private static MoviesServer server;
    private static HttpClient client;

    @BeforeAll
    static void beforeAll() {
        store = new MoviesStore();
        server = new MoviesServer(store, 8080);
        server.start();

        client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(2))
                .build();
    }

    @BeforeEach
    void beforeEach() {
        store.clear();
    }

    @AfterAll
    static void afterAll() {
        server.stop();
    }

    @Test
    @DisplayName("Должен возвращать пустой JSON-массив, если фильмы отсутствуют")
    void getMovies_whenEmpty_returnsEmptyArray() throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .GET()
                .build();

        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(200, resp.statusCode(), "GET /movies должен вернуть 200");

        String contentTypeHeaderValue =
                resp.headers().firstValue("Content-Type").orElse("");
        assertEquals("application/json; charset=UTF-8", contentTypeHeaderValue,
                "Content-Type должен содержать формат данных и кодировку");

        String body = resp.body().trim();
        assertEquals("[]", body, "Ожидается JSON-массив");
    }

    @Test
    @DisplayName("Должен создавать фильм и возвращать его с присвоенным ID")
    void postMovie_whenValid_returnsCreatedMovie() throws Exception {
        String requestBody = "{\"title\": \"Interstellar\", \"year\": 2014}";
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody, StandardCharsets.UTF_8))
                .build();
        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(201, resp.statusCode(), "POST /movies должен вернуть 201");

        String contentTypeHeaderValue =
                resp.headers().firstValue("Content-Type").orElse("");

        assertEquals("application/json; charset=UTF-8", contentTypeHeaderValue,
                "Content-Type должен содержать формат данных и кодировку");

        JsonObject createdMovie = JsonParser.parseString(resp.body()).getAsJsonObject();

        assertEquals("Interstellar", createdMovie.get("title").getAsString(),
                "Сервер должен вернуть название созданного фильма.");
        assertEquals(2014, createdMovie.get("year").getAsInt(),
                "Сервер должен вернуть год созданного фильма.");
        assertTrue(createdMovie.has("id"),
                "Созданному фильму должен быть присвоен ID");
        assertTrue(createdMovie.get("id").getAsLong() > 0,
                "ID созданного фильма должен быть положительным");
    }

    @Test
    @DisplayName("Должен возвращать массив сохранённых фильмов")
    void getMovies_whenMoviesExist_returnsMoviesArray() throws Exception {
        store.add(new Movie("Interstellar", 2014));
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .GET()
                .build();

        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(200, resp.statusCode(),
                "GET /movies должен вернуть 200");

        String contentTypeHeaderValue =
                resp.headers().firstValue("Content-Type").orElse("");

        assertEquals("application/json; charset=UTF-8",
                contentTypeHeaderValue,
                "Content-Type должен содержать формат данных и кодировку");
        JsonArray returnedMovies =
                JsonParser.parseString(resp.body()).getAsJsonArray();
        assertEquals(1, returnedMovies.size(),
                "GET /movies должен вернуть один сохранённый фильм");
        JsonObject returnedMovie =
                returnedMovies.get(0).getAsJsonObject();

        assertEquals(1L,
                returnedMovie.get("id").getAsLong(),
                "Сервер должен вернуть ID сохранённого фильма");

        assertEquals("Interstellar",
                returnedMovie.get("title").getAsString(),
                "Сервер должен вернуть название сохранённого фильма");

        assertEquals(2014,
                returnedMovie.get("year").getAsInt(),
                "Сервер должен вернуть год сохранённого фильма");
    }

    @Test
    @DisplayName("Должен возвращать ошибку валидации при пустом названии фильма")
    void postMovie_whenTitleEmpty_returnsValidationError() throws Exception {
        String requestBody = "{\"title\": \"\", \"year\": 2014}";
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(422, resp.statusCode(), "POST /movies должен вернуть 422");

        String contentTypeHeaderValue =
                resp.headers().firstValue("Content-Type").orElse("");
        assertEquals("application/json; charset=UTF-8", contentTypeHeaderValue,
                "Content-Type должен содержать формат данных и кодировку");

        JsonObject errorResponse = JsonParser.parseString(resp.body()).getAsJsonObject();
        String error = errorResponse.get("error").getAsString();

        assertEquals("Ошибка валидации", error,
                "Поле error должно содержать строку \"Ошибка валидации\"");

        JsonArray details = errorResponse.get("details").getAsJsonArray();

        assertEquals(1, details.size(),
                "Массив содержит ровно одно описание ошибки");

        String description = details.get(0).getAsString();

        assertEquals("название не должно быть пустым", description,
                "Массив details должен содержать описание ошибки");
        assertTrue(store.getAll().isEmpty(),
                "Невалидный фильм не должен сохраняться");
    }

    @Test
    @DisplayName("Должен возвращать ошибку валидации при слишком длинном названии фильма")
    void postMovie_whenTitleTooLong_returnsValidationError() throws Exception {
        String longTitle = "a".repeat(101);
        String requestBody = "{\"title\": \"" + longTitle + "\", \"year\": 2014}";

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(422, resp.statusCode(), "POST /movies должен вернуть 422");

        String contentTypeHeaderValue =
                resp.headers().firstValue("Content-Type").orElse("");
        assertEquals("application/json; charset=UTF-8", contentTypeHeaderValue,
                "Content-Type должен содержать формат данных и кодировку");

        JsonObject errorResponse = JsonParser.parseString(resp.body()).getAsJsonObject();
        String error = errorResponse.get("error").getAsString();

        assertEquals("Ошибка валидации", error,
                "Поле error должно содержать строку \"Ошибка валидации\"");

        JsonArray details = errorResponse.get("details").getAsJsonArray();

        assertEquals(1, details.size(),
                "Массив содержит ровно одно описание ошибки");

        String description = details.get(0).getAsString();

        assertEquals("название не должно быть длиннее 100 символов", description,
                "Массив details должен содержать описание ошибки");
        assertTrue(store.getAll().isEmpty(),
                "Невалидный фильм не должен сохраняться");
    }

    @Test
    @DisplayName("Должен возвращать ошибку валидации при слишком раннем годе фильма")
    void postMovie_whenYearTooEarly_returnsValidationError() throws Exception {
        int maxYear = LocalDate.now().getYear() + 1;
        String requestBody = "{\"title\": \"Interstellar\", \"year\": 1887}";

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(422, resp.statusCode(), "POST /movies должен вернуть 422");

        String contentTypeHeaderValue =
                resp.headers().firstValue("Content-Type").orElse("");
        assertEquals("application/json; charset=UTF-8", contentTypeHeaderValue,
                "Content-Type должен содержать формат данных и кодировку");

        JsonObject errorResponse = JsonParser.parseString(resp.body()).getAsJsonObject();
        String error = errorResponse.get("error").getAsString();

        assertEquals("Ошибка валидации", error,
                "Поле error должно содержать строку \"Ошибка валидации\"");

        JsonArray details = errorResponse.get("details").getAsJsonArray();

        assertEquals(1, details.size(),
                "Массив содержит ровно одно описание ошибки");

        String description = details.get(0).getAsString();

        assertEquals("год должен быть между 1888 и " + maxYear, description,
                "Массив details должен содержать описание ошибки");
        assertTrue(store.getAll().isEmpty(),
                "Невалидный фильм не должен сохраняться");
    }

    @Test
    @DisplayName("Должен возвращать ошибку валидации при слишком позднем годе фильма")
    void postMovie_whenYearTooLate_returnsValidationError() throws Exception {
        int maxYear = LocalDate.now().getYear() + 1;
        int invalidYear = maxYear + 1;

        String requestBody = "{\"title\": \"Interstellar\", \"year\": " + invalidYear + "}";

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(422, resp.statusCode(), "POST /movies должен вернуть 422");

        String contentTypeHeaderValue =
                resp.headers().firstValue("Content-Type").orElse("");
        assertEquals("application/json; charset=UTF-8", contentTypeHeaderValue,
                "Content-Type должен содержать формат данных и кодировку");

        JsonObject errorResponse = JsonParser.parseString(resp.body()).getAsJsonObject();
        String error = errorResponse.get("error").getAsString();

        assertEquals("Ошибка валидации", error,
                "Поле error должно содержать строку \"Ошибка валидации\"");

        JsonArray details = errorResponse.get("details").getAsJsonArray();

        assertEquals(1, details.size(),
                "Массив содержит ровно одно описание ошибки");

        String description = details.get(0).getAsString();

        assertEquals("год должен быть между 1888 и " + maxYear, description,
                "Массив details должен содержать описание ошибки");
        assertTrue(store.getAll().isEmpty(),
                "Невалидный фильм не должен сохраняться");
    }

    @Test
    @DisplayName("Должен возвращать 415 при неправильном Content-Type")
    void postMovie_whenContentTypeIsNotJson_returnsUnsupportedMediaType() throws Exception {
        String requestBody = "{\"title\": \"Interstellar\", \"year\": 2014}";
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "text/plain")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody, StandardCharsets.UTF_8))
                .build();
        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(415, resp.statusCode(),
                "POST /movies должен вернуть 415");

        String contentTypeHeaderValue =
                resp.headers().firstValue("Content-Type").orElse("");

        assertEquals("application/json; charset=UTF-8", contentTypeHeaderValue,
                "Content-Type должен содержать формат данных и кодировку");

        JsonObject errorResponse = JsonParser.parseString(resp.body()).getAsJsonObject();

        String error = errorResponse.get("error").getAsString();

        assertEquals("Неподдерживаемый Content-Type", error,
                "Поле error должно описывать неправильный Content-Type");
        assertTrue(store.getAll().isEmpty(),
                "Невалидный фильм не должен сохраняться");
    }

    @Test
    @DisplayName("Должен возвращать 400 при некорректном JSON")
    void postMovie_whenJsonMalformed_returnsBadRequest() throws Exception {
        String requestBody =
                "{\"title\": \"Interstellar\", \"year\": }";
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody, StandardCharsets.UTF_8))
                .build();
        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(400, resp.statusCode(),
                "POST /movies должен вернуть 400");

        String contentTypeHeaderValue =
                resp.headers().firstValue("Content-Type").orElse("");

        assertEquals("application/json; charset=UTF-8", contentTypeHeaderValue,
                "Content-Type должен содержать формат данных и кодировку");

        JsonObject errorResponse = JsonParser.parseString(resp.body()).getAsJsonObject();

        String error = errorResponse.get("error").getAsString();

        assertEquals("Некорректный JSON", error,
                "Поле error должно описывать некорректный JSON");
        assertTrue(store.getAll().isEmpty(),
                "Фильм из некорректного JSON не должен сохраняться");
    }

    @Test
    @DisplayName("Должен возвращать фильм по существующему ID")
    void getMovieById_whenExists_returnsMovie() throws Exception {
        store.add(new Movie("Interstellar", 2014));
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies/1"))
                .GET()
                .build();

        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(200, resp.statusCode(),
                "GET /movies/1 должен вернуть 200");

        String contentTypeHeaderValue =
                resp.headers().firstValue("Content-Type").orElse("");

        assertEquals("application/json; charset=UTF-8",
                contentTypeHeaderValue,
                "Content-Type должен содержать формат данных и кодировку");

        JsonObject returnedMovie =
                JsonParser.parseString(resp.body()).getAsJsonObject();

        assertEquals(1L,
                returnedMovie.get("id").getAsLong(),
                "Сервер должен вернуть ID сохранённого фильма");

        assertEquals("Interstellar",
                returnedMovie.get("title").getAsString(),
                "Сервер должен вернуть название сохранённого фильма");

        assertEquals(2014,
                returnedMovie.get("year").getAsInt(),
                "Сервер должен вернуть год сохранённого фильма");
    }

    @Test
    @DisplayName("Должен возвращать 404, если фильм не найден")
    void getMovieById_whenMissing_returnsNotFound() throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies/999"))
                .GET()
                .build();

        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(404, resp.statusCode(),
                "GET /movies/999 должен вернуть 404");

        String contentTypeHeaderValue =
                resp.headers().firstValue("Content-Type").orElse("");

        assertEquals("application/json; charset=UTF-8", contentTypeHeaderValue,
                "Content-Type должен содержать формат данных и кодировку");

        JsonObject errorResponse = JsonParser.parseString(resp.body()).getAsJsonObject();
        String error = errorResponse.get("error").getAsString();

        assertEquals("Фильм не найден", error,
                "Поле error должно сообщать, что фильм не найден");

        assertTrue(store.getAll().isEmpty(),
                "Хранилище должно оставаться пустым");
    }

    @Test
    @DisplayName("Должен возвращать 400, если ID не является числом")
    void getMovieById_whenIdIsNotNumber_returnsBadRequest() throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies/abc"))
                .GET()
                .build();

        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(400, resp.statusCode(),
                "GET /movies/abc должен вернуть 400");

        String contentTypeHeaderValue =
                resp.headers().firstValue("Content-Type").orElse("");

        assertEquals("application/json; charset=UTF-8", contentTypeHeaderValue,
                "Content-Type должен содержать формат данных и кодировку");

        JsonObject errorResponse = JsonParser.parseString(resp.body()).getAsJsonObject();
        String error = errorResponse.get("error").getAsString();

        assertEquals("Некорректный ID", error,
                "Поле error должно сообщать о некорректном ID");

        assertTrue(store.getAll().isEmpty(),
                "Хранилище должно оставаться пустым");
    }

    @Test
    @DisplayName("Должен удалять существующий фильм и возвращать 204")
    void deleteMovie_whenExists_removesMovieAndReturnsNoContent() throws Exception {
        store.add(new Movie("Interstellar", 2014));
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies/1"))
                .timeout(Duration.ofSeconds(2))
                .DELETE()
                .build();

        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(204, resp.statusCode(),
                "DELETE /movies/1 должен вернуть 204");
        String contentTypeHeaderValue =
                resp.headers().firstValue("Content-Type").orElse("");
        assertEquals("application/json; charset=UTF-8", contentTypeHeaderValue,
                "Content-Type должен содержать формат данных и кодировку");
        assertTrue(resp.body().isBlank(),
                "Ответ 204 не должен содержать тело");
        assertTrue(store.getAll().isEmpty(),
                "Фильм должен быть удалён из хранилища");
    }
}
