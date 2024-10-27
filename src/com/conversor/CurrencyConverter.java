// CurrencyConverter.java

package com.conversor;

import com.google.gson.Gson;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Properties;
import java.util.Scanner;

public class CurrencyConverter {
    private static final String API_KEY;
    private static final String BASE_URL;

    static {
        String apiKey = null;
        try {
            // Intenta primero obtener la API_KEY de las variables de entorno
            apiKey = System.getenv("EXCHANGE_API_KEY");

            // Si no encuentra la variable de entorno, busca en el archivo properties
            if (apiKey == null || apiKey.isEmpty()) {
                Properties props = new Properties();
                InputStream input = CurrencyConverter.class.getClassLoader()
                        .getResourceAsStream("config.properties");

                if (input != null) {
                    props.load(input);
                    apiKey = props.getProperty("api.key");
                    input.close();
                }
            }

            // Si aún no encuentra la API_KEY, lanza una excepción
            if (apiKey == null || apiKey.isEmpty()) {
                throw new RuntimeException("API Key no encontrada. " +
                        "Configura la variable de entorno EXCHANGE_API_KEY o el archivo config.properties");
            }

        } catch (IOException e) {
            throw new RuntimeException("Error al cargar la configuración: " + e.getMessage());
        }

        API_KEY = apiKey;
        BASE_URL = "https://v6.exchangerate-api.com/v6/" + API_KEY + "/latest/";
    }

    private static ConversionRates getExchangeRates(String baseCurrency) throws IOException, InterruptedException {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + baseCurrency))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        ConversionRates rates = new Gson().fromJson(response.body(), ConversionRates.class);

        if (rates == null || rates.getConversion_rates() == null ||
                !rates.getConversion_rates().containsKey(baseCurrency)) {
            return null;
        }

        return rates;
    }

    private static boolean continuarPrograma(Scanner scanner) {
        while (true) {
            System.out.print("¿Deseas seguir cambiando? (si/no, s/n): ");
            String respuesta = scanner.nextLine().toLowerCase().trim();

            if (respuesta.equals("s") || respuesta.equals("si") ||
                    respuesta.equals("y") || respuesta.equals("yes")) {
                return true;
            } else if (respuesta.equals("n") || respuesta.equals("no")) {
                return false;
            } else {
                System.out.println("Por favor, responde 'si/s' o 'no/n'");
            }
        }
    }

    private static String obtenerMonedaValida(Scanner scanner, String mensaje, ConversionRates rates) {
        while (true) {
            System.out.print(mensaje);
            String currency = scanner.nextLine().toUpperCase().trim();

            if (currency.matches("\\d+")) {
                System.out.println("Ingresa el código de la moneda de tres letras correctamente:");
                continue;
            }

            if (rates.getConversion_rates().containsKey(currency)) {
                return currency;
            } else {
                System.out.println("La moneda no existe, ingresa el código de la moneda correctamente:");
            }
        }
    }

    private static ConversionRates obtenerMonedaBaseValida(Scanner scanner) {
        while (true) {
            try {
                System.out.print("Ingresa el código de tu moneda: ");
                String baseCurrency = scanner.nextLine().toUpperCase().trim();

                // Verificar si la entrada es un número
                if (baseCurrency.matches("\\d+")) {
                    System.out.println("Ingresa el código de la moneda de tres letras correctamente:");
                    continue;
                }

                ConversionRates rates = getExchangeRates(baseCurrency);
                if (rates != null) {
                    return rates;
                } else {
                    System.out.println("La moneda no existe, ingresa el código de la moneda correctamente:");
                }
            } catch (Exception e) {
                System.out.println("Error al obtener las tasas de cambio. Por favor, intenta de nuevo.");
            }
        }
    }

    private static double obtenerCantidadValida(Scanner scanner, String baseCurrency) {
        while (true) {
            System.out.printf("Ingresa una cantidad válida para la moneda %s: ", baseCurrency);
            try {
                String input = scanner.nextLine().trim();
                double amount = Double.parseDouble(input);
                if (amount <= 0) {
                    System.out.println("Por favor, ingresa una cantidad mayor que cero.");
                    continue;
                }
                return amount;
            } catch (NumberFormatException e) {
                System.out.println("Por favor, ingresa un número válido.");
            }
        }
    }

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        boolean continuar = true;

        while (continuar) {
            try {
                // Obtener y validar moneda base
                ConversionRates rates = obtenerMonedaBaseValida(scanner);
                String baseCurrency = rates.getBase_code();

                // Obtener cantidad válida
                double amount = obtenerCantidadValida(scanner, baseCurrency);

                // Solicitar y validar moneda objetivo
                String mensaje = String.format("Ingresa el código de la moneda para cambiar tus %.2f %s: ",
                        amount, baseCurrency);
                String targetCurrency = obtenerMonedaValida(scanner, mensaje, rates);

                // Calcular conversión
                double exchangeRate = rates.getConversion_rates().get(targetCurrency);
                double result = amount * exchangeRate;

                // Mostrar resultado
                System.out.printf("Son %.2f %s%n", result, targetCurrency);

            } catch (Exception e) {
                System.out.println("Ocurrió un error inesperado. Por favor, intenta de nuevo.");
                continue;
            }

            // Preguntar si desea continuar
            continuar = continuarPrograma(scanner);
        }

        System.out.println("¡Gracias por usar el conversor de monedas!");
        scanner.close();
    }
}