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
}