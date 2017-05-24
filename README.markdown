# Stygian

Build simple workflows using a fluent API, pretty-print them and execute them with flexible execution strategies.

## Creating and sequencing flows

A `Flow<I, O>` is a workflow that receives an input of type `I`, and asynchronously returns a response of type `O`:

```java
interface WeatherService {
    CompletableFuture<Weather> getWeatherFor(Postcode postcode);
}

Flow<Postcode, Weather> fetchWeather = flow("Fetch weather", postcode -> weatherService.getWeatherFor(postcode));
```

We can sequence two flows together:

```
Flow<Weather, Unit> printToConsole = flow("Print weather to console", weather -> {
    System.out.println(weather);
    return Unit.INSTANCE;
});

Flow<Postcode, Unit> fetchAndPrintWeather = fetchWeather.then(printToConsole);
```

We can ask a `FlowDescriber` to get us a pretty-printed representation of this flow:

```java
System.out.println(describe(fetchAndPrintWeather));
```

which will look like this:

```
Sequence
		1: Fetch weather
		2: Print weather to console
```

Let's introduce a formatting step:

```java
Flow<Weather, String> formatWeather = flow("Format weather", weather -> weatherFormatter.format(weather));
```

We can now modify `printToConsole` to print any string, and compose it with `formatWeather`:

```java
Flow<String, Unit> printToConsole = flow("Print to console", string -> {
    System.out.println(string);
    return Unit.INSTANCE;
});

Flow<Weather, Unit> printWeatherToConsole = formatWeather.then(printToConsole);
Flow<Postcode, Unit> fetchAndPrintWeather = fetchWeather.then(printWeatherToConsole);
```

The pretty-printed representation of this flow looks like this:

```
Sequence
		1: Fetch weather
		2: Sequence
			2.1: Format weather
			2.2: Print to console
```

## Branching

Suppose we only want to fetch the weather for users with proper credentials. We might define a flow like this, using the `or` operator:

```java
Condition<WeatherRequest> hasValidCredentials = asyncCondition("Check credentials", weatherRequest -> credentialsChecker.check(weatherRequest));
Flow<WeatherRequest, Postcode> extractPostcode = flow("Extract postcode", weatherRequest -> weatherRequest.getPostcode());
Flow<WeatherRequest, String> extractError = flow("Extract error message", weatherRequest -> "Sorry, " + weatherRequest.getUsername() + ", your credentials are not valid");

Flow<WeatherRequest, Unit> printErrorToConsole = extractError.then(printToConsole);

Flow<WeatherRequest, Unit> fetchAndPrintWeatherIfValid = printErrorToConsole
    .or(hasValidCredentials, extractPostcode.then(fetchAndPrintWeather));
```

Here's how that pretty-prints:

```
Branch
		If credentials ok: Sequence
			1: Extract postcode
			2: Sequence
				2.1: Fetch weather
				2.2: Sequence
					2.2.1: Format weather
					2.2.2: Print to console
		Otherwise: Sequence
			1: Extract error message
			2: Print to console
```

We can simplify this a bit by stitching the flows in the "happy path" together more directly:

```java
Flow<WeatherRequest, Unit> fetchAndPrintWeatherIfValid = printErrorToConsole
    .or(
        hasValidCredentials,
        extractPostcode
            .then(fetchWeather)
            .then(formatWeather)
            .then(printToConsole));
```

which comes out like so:

```
Branch
		If credentials ok: Sequence
			1: Extract postcode
			2: Fetch weather
			3: Format weather
			4: Print to console
		Otherwise: Sequence
			1: Extract error message
			2: Print to console
```

## Varying execution strategies

We can plug different execution strategies in, to control things like the thread pools that are used to execute individual flows, and wire in behaviours such as logging and security checking.

For example,

```java
runLogging(fetchAndPrintWeatherIfValid, myRequest, LOGGER::info);
```

will output the log messages:

```
INFO: Invoking Condition 'credentials ok' with <A request from Arthur Putey for the weather at Postcode 'VB6 5UX'>
INFO: Condition 'credentials ok' completed with <true>
INFO: Invoking Action 'Extract postcode' with <A request from Arthur Putey for the weather at Postcode 'VB6 5UX'>
INFO: Action 'Extract postcode' completed with <Postcode 'VB6 5UX'>
INFO: Invoking Action 'Fetch weather' with <Postcode 'VB6 5UX'>
INFO: Action 'Fetch weather' completed with <Temperature: 26F>
INFO: Invoking Action 'Format weather' with <Temperature: 26F>
INFO: Action 'Format weather' completed with <It's going to be a fine day today>
INFO: Invoking Action 'Print to console' with <It's going to be a fine day today>
INFO: Action 'Print to console' completed with <kotlin.Unit>
```