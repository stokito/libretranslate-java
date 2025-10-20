package space.dynomake.libretranslate;

import lombok.NonNull;
import lombok.Setter;
import lombok.experimental.UtilityClass;
import space.dynomake.libretranslate.exception.BadTranslatorResponseException;
import space.dynomake.libretranslate.type.TranslateResponse;
import space.dynomake.libretranslate.util.JsonUtil;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.*;
import java.nio.charset.StandardCharsets;

@UtilityClass
public class Translator {

    @Setter
    private String urlApi = "https://translate.fedilab.app/translate";

    @Setter
    private String apiKey = "unknown";

    public String translate(@NonNull String from, @NonNull String to, @NonNull String request) {
        return translateDetect(from, to, request).getTranslatedText();
    }

    public TranslateResponse translateDetect(@NonNull String from, @NonNull String to, @NonNull String request) {
        HttpURLConnection httpConn = null;
        try {
            URL url = new URL(urlApi);
            httpConn = (HttpURLConnection) url.openConnection();
            httpConn.setRequestMethod("POST");

            httpConn.setRequestProperty("Accept", "application/json");
            httpConn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            httpConn.setRequestProperty("User-Agent", "Mozilla/5.0");

            httpConn.setDoOutput(true);

            OutputStreamWriter writer = new OutputStreamWriter(httpConn.getOutputStream());

            writer.write("q=" + URLEncoder.encode(request, "UTF-8") + "&source=" + from + "&api_key=" + apiKey + "&target=" + to + "&format=text");
            writer.flush();
            writer.close();
            httpConn.getOutputStream().close();

            // Check response code before reading
            int responseCode = httpConn.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                throw new BadTranslatorResponseException(responseCode, urlApi);
            }

            InputStream responseStream = httpConn.getInputStream();

            InputStreamReader reader = new InputStreamReader(responseStream, StandardCharsets.UTF_8);
            return JsonUtil.from(reader, TranslateResponse.class);
        } catch (IOException e) {
            throw new RuntimeException("Network error during translation", e);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Translation failed", e);
        } finally {
            if (httpConn != null) {
                httpConn.disconnect();
            }
        }
    }

    public String translate(@NonNull Language from, @NonNull Language to, @NonNull String request) {
        if (to == Language.NONE || from == to) return request;
        return translate(from.getCode(), to.getCode(), request);
    }

    public TranslateResponse translateDetect(@NonNull Language to, @NonNull String request) {
        return translateDetect("auto", to.getCode(), request);
    }

    public String translate(@NonNull Language to, @NonNull String request) {
        if (to == Language.NONE) return request;
        return translate("auto", to.getCode(), request);
    }
}
