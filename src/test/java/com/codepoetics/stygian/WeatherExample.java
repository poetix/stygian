package com.codepoetics.stygian;

import kotlin.Unit;
import org.junit.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.logging.Logger;

import static com.codepoetics.stygian.java.JavaFlow.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class WeatherExample {

    private static final Logger LOGGER = Logger.getLogger(WeatherExample.class.getName());

    private static final class WeatherRequest {
        private final Postcode postcode;
        private final String user;

        private WeatherRequest(Postcode postcode, String user) {
            this.postcode = postcode;
            this.user = user;
        }

        public Postcode getPostcode() {
            return postcode;
        }

        public String getUser() {
            return user;
        }

        @Override
        public String toString() {
            return "A request from " + user + " for the weather at " + postcode;
        }
    }

    private static final class Postcode {
        private final String postcode;

        public Postcode(String postcode) {
            this.postcode = postcode;
        }

        @Override
        public String toString() {
            return "Postcode '" + postcode + "'";
        }
    }

    private static final class Weather {
        private final String temperature;

        private Weather(String temperature) {
            this.temperature = temperature;
        }

        @Override
        public String toString() {
            return "Temperature: " + temperature;
        }
    }

    private interface WeatherService {
        CompletableFuture<Weather> getWeatherFor(Postcode postcode);
    }

    private interface WeatherFormatter {
        String format(Weather weather);
    }

    private interface CredentialsChecker {
        CompletableFuture<Boolean> check(WeatherRequest request);
    }

    private final WeatherService weatherService = mock(WeatherService.class);
    private final WeatherFormatter weatherFormatter = mock(WeatherFormatter.class);
    private final CredentialsChecker credentialsChecker = mock(CredentialsChecker.class);

    @Test
    public void weatherExample() throws ExecutionException, InterruptedException {

        when(weatherService.getWeatherFor(any(Postcode.class))).thenReturn(CompletableFuture.completedFuture(new Weather("26F")));
        when(weatherFormatter.format(any(Weather.class))).thenReturn("It's going to be a fine day today");
        when(credentialsChecker.check(any(WeatherRequest.class))).thenReturn(CompletableFuture.completedFuture(true));

        Flow<Postcode, Weather> fetchWeather = asyncFlow("Fetch weather", postcode -> weatherService.getWeatherFor(postcode));

        Flow<Weather, String> formatWeather = flow("Format weather", weather -> weatherFormatter.format(weather));
        Flow<String, Unit> printToConsole = flow("Print to console", string -> {
            System.out.println(string);
            return Unit.INSTANCE;
        });

        Flow<Weather, Unit> printWeatherToConsole = formatWeather.then(printToConsole);
        Flow<Postcode, Unit> fetchAndPrintWeather = fetchWeather.then(printWeatherToConsole);

        WeatherRequest myRequest = new WeatherRequest(new Postcode("VB6 5UX"), "Arthur Putey");
        Condition<WeatherRequest> hasValidCredentials = asyncCondition("credentials ok", weatherRequest -> credentialsChecker.check(weatherRequest));
        Flow<WeatherRequest, Postcode> extractPostcode = flow("Extract postcode", weatherRequest -> weatherRequest.getPostcode());
        Flow<WeatherRequest, String> extractError = flow("Extract error message", weatherRequest -> "Sorry, " + weatherRequest.getUser() + ", your credentials are not valid");

        Flow<WeatherRequest, Unit> printErrorToConsole = extractError.then(printToConsole);

        Flow<WeatherRequest, Unit> fetchAndPrintWeatherIfValid = printErrorToConsole
                .orIf(
                        hasValidCredentials,
                        extractPostcode.
                                then(fetchWeather)
                                .then(formatWeather)
                                .then(printToConsole));

        System.out.println(prettyPrint(fetchAndPrintWeatherIfValid));

        runLogging(fetchAndPrintWeatherIfValid, myRequest, LOGGER::info);
    }
}
