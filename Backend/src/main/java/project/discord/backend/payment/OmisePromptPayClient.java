package project.discord.backend.payment;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class OmisePromptPayClient {

    private static final Pattern JSON_STRING_PATTERN = Pattern.compile("\"%s\"\\s*:\\s*\"([^\"]*)\"");

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    private final String publicKey;
    private final String secretKey;
    private final String apiBaseUrl;
    private final long expirationMinutes;

    public OmisePromptPayClient(
            @Value("${app.payment.omise.public-key:}") String publicKey,
            @Value("${app.payment.omise.secret-key:}") String secretKey,
            @Value("${app.payment.omise.api-base-url:https://api.omise.co}") String apiBaseUrl,
            @Value("${app.payment.checkout.expiration-minutes:30}") long expirationMinutes
    ) {
        this.publicKey = publicKey == null ? "" : publicKey.trim();
        this.secretKey = secretKey == null ? "" : secretKey.trim();
        this.apiBaseUrl = apiBaseUrl.endsWith("/") ? apiBaseUrl.substring(0, apiBaseUrl.length() - 1) : apiBaseUrl;
        this.expirationMinutes = Math.max(5, expirationMinutes);
    }

    public PromptPayCharge createPromptPayCharge(Integer amountCents, String currency, String checkoutReference) {
        Instant expiresAt = Instant.now().plus(Duration.ofMinutes(expirationMinutes));
        if (!isConfigured() || amountCents < 2000) {
            String mockId = "mock_chrg_" + UUID.randomUUID().toString().replace("-", "").substring(0, 18);
            return new PromptPayCharge(
                    mockId,
                    "mock_src_" + checkoutReference,
                    "pending",
                    "https://dummyimage.com/512x512/ffffff/111827.png&text=PromptPay+Dev+QR+" + amountCents,
                    expiresAt
            );
        }

        String sourceId = createSource(amountCents, currency);
        String chargeBody = postForm(
                "/charges",
                secretKey,
                Map.of(
                        "amount", String.valueOf(amountCents),
                        "currency", currency,
                        "source", sourceId,
                        "description", "Discord bot feature checkout " + checkoutReference,
                        "metadata[checkout_reference]", checkoutReference,
                        "expires_at", expiresAt.toString()
                )
        );

        return new PromptPayCharge(
                extractString(chargeBody, "id").orElseThrow(() -> new IllegalStateException("Omise charge id missing")),
                sourceId,
                extractString(chargeBody, "status").orElse("pending"),
                extractString(chargeBody, "download_uri").orElse(null),
                expiresAt
        );
    }

    public Optional<ChargeStatus> retrieveChargeStatus(String chargeId) {
        if (!isConfigured() || chargeId == null || chargeId.startsWith("mock_")) {
            return Optional.empty();
        }

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(apiBaseUrl + "/charges/" + encodePath(chargeId)))
                    .timeout(Duration.ofSeconds(15))
                    .header("Authorization", basicAuth(secretKey))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                return Optional.empty();
            }

            return Optional.of(new ChargeStatus(
                    chargeId,
                    extractString(response.body(), "status").orElse("pending"),
                    extractString(response.body(), "failure_message").orElse(null)
            ));
        } catch (IOException exception) {
            throw new IllegalStateException("Could not retrieve Omise charge", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Could not retrieve Omise charge", exception);
        }
    }

    private String createSource(Integer amountCents, String currency) {
        String sourceBody = postForm(
                "/sources",
                publicKey,
                Map.of(
                        "amount", String.valueOf(amountCents),
                        "currency", currency,
                        "type", "promptpay"
                )
        );

        return extractString(sourceBody, "id").orElseThrow(() -> new IllegalStateException("Omise source id missing"));
    }

    private String postForm(String path, String key, Map<String, String> fields) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(apiBaseUrl + path))
                    .timeout(Duration.ofSeconds(20))
                    .header("Authorization", basicAuth(key))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(formEncode(fields)))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException("Omise API returned " + response.statusCode());
            }

            return response.body();
        } catch (IOException exception) {
            throw new IllegalStateException("Could not create Omise checkout", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Could not create Omise checkout", exception);
        }
    }

    private boolean isConfigured() {
        return publicKey.startsWith("pkey_") && secretKey.startsWith("skey_");
    }

    private String basicAuth(String key) {
        return "Basic " + Base64.getEncoder().encodeToString((key + ":").getBytes(StandardCharsets.UTF_8));
    }

    private String formEncode(Map<String, String> fields) {
        return fields.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (left, right) -> left,
                        LinkedHashMap::new
                ))
                .entrySet()
                .stream()
                .map(entry -> encode(entry.getKey()) + "=" + encode(entry.getValue()))
                .collect(Collectors.joining("&"));
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private String encodePath(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }

    private Optional<String> extractString(String json, String key) {
        Matcher matcher = Pattern.compile(String.format(JSON_STRING_PATTERN.pattern(), Pattern.quote(key))).matcher(json);
        if (!matcher.find()) {
            return Optional.empty();
        }

        return Optional.of(matcher.group(1).replace("\\/", "/"));
    }

    public record PromptPayCharge(
            String chargeId,
            String sourceId,
            String status,
            String qrCodeUrl,
            Instant expiresAt
    ) {
    }

    public record ChargeStatus(
            String chargeId,
            String status,
            String failureMessage
    ) {
    }
}
