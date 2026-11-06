package sigelab.facial;

import android.graphics.Bitmap;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Cliente Android de la SIGELAB Facial API (Azure).
 *
 * Los métodos son BLOQUEANTES: llámelos desde un hilo de fondo (Executor), nunca desde el hilo principal.
 * Dependencia: implementation("com.squareup.okhttp3:okhttp:4.12.0")
 */
public class FacialApiClient {

    /** Lado mayor de cada foto enviada (px). */
    private static final int MAX_LADO = 960;
    private static final MediaType JPEG = MediaType.get("image/jpeg");

    private final String baseUrl;
    private final String bearerToken;
    private final OkHttpClient http;

    public FacialApiClient(String baseUrl, String bearerToken) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.bearerToken = bearerToken;
        // Tiempos largos: si Azure tenía la API apagada (escala a cero) el primer uso tarda en arrancar.
        this.http = new OkHttpClient.Builder()
                .connectTimeout(20, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .readTimeout(90, TimeUnit.SECONDS)
                .build();
    }

    // ------------------------------------------------------------ endpoints

    /** Despierta la API. Llámelo al abrir la app (en segundo plano). */
    public Respuesta salud() throws IOException {
        return enviar(peticion("/api/face/health").get().build());
    }

    public Desafio desafio(Integer idUsuario) throws IOException {
        HttpUrl.Builder url = HttpUrl.get(baseUrl + "/api/face/challenge").newBuilder();
        if (idUsuario != null) {
            url.addQueryParameter("userId", String.valueOf(idUsuario));
        }
        Respuesta r = enviar(peticion(url.build()).get().build());
        if (!r.esExitosa()) {
            throw new IOException(r.mensaje());
        }
        return new Desafio(r.json.optString("challengeToken"), r.json.optString("challenge"), r.json.optString("instruction"));
    }

    /** Registrar rostro: ~3 fotos de frente, 2 girando a la izquierda y 2 a la derecha. */
    public Respuesta registrar(int idUsuario, boolean reemplazar, List<Bitmap> fotos, boolean espejo) throws IOException {
        MultipartBody.Builder b = formulario(fotos, espejo);
        b.addFormDataPart("userId", String.valueOf(idUsuario));
        b.addFormDataPart("replace", String.valueOf(reemplazar));
        return enviar(peticion("/api/face/enroll").post(b.build()).build());
    }

    /** Verificación 1:1 (¿es este usuario?). */
    public Respuesta verificar(int idUsuario, String tokenDesafio, List<Bitmap> fotos, boolean espejo) throws IOException {
        MultipartBody.Builder b = formulario(fotos, espejo);
        b.addFormDataPart("userId", String.valueOf(idUsuario));
        if (tokenDesafio != null) {
            b.addFormDataPart("challengeToken", tokenDesafio);
        }
        return enviar(peticion("/api/face/verify").post(b.build()).build());
    }

    /** Identificación 1:N (¿quién es?). */
    public Respuesta identificar(String tokenDesafio, List<Bitmap> fotos, boolean espejo) throws IOException {
        MultipartBody.Builder b = formulario(fotos, espejo);
        if (tokenDesafio != null) {
            b.addFormDataPart("challengeToken", tokenDesafio);
        }
        return enviar(peticion("/api/face/identify").post(b.build()).build());
    }

    /** Calidad y pose de las fotos, sin guardar nada. Útil para comprobar el valor de "espejo". */
    public Respuesta analizar(List<Bitmap> fotos, boolean espejo) throws IOException {
        return enviar(peticion("/api/face/analyze").post(formulario(fotos, espejo).build()).build());
    }

    public Respuesta estado(int idUsuario) throws IOException {
        return enviar(peticion("/api/face/status/" + idUsuario).get().build());
    }

    // ------------------------------------------------------------- internos

    private Request.Builder peticion(String ruta) {
        return peticion(HttpUrl.get(baseUrl + ruta));
    }

    private Request.Builder peticion(HttpUrl url) {
        return new Request.Builder().url(url)
                .header("Authorization", "Bearer " + bearerToken)
                .header("X-Client", "sigelab-android")
                .header("Accept", "application/json");
    }

    private MultipartBody.Builder formulario(List<Bitmap> fotos, boolean espejo) {
        MultipartBody.Builder b = new MultipartBody.Builder().setType(MultipartBody.FORM);
        b.addFormDataPart("mirrored", String.valueOf(espejo));
        for (int i = 0; i < fotos.size(); i++) {
            b.addFormDataPart("images", "foto" + i + ".jpg", RequestBody.create(aJpeg(fotos.get(i)), JPEG));
        }
        return b;
    }

    private Respuesta enviar(Request request) throws IOException {
        try (Response res = http.newCall(request).execute()) {
            ResponseBody body = res.body();
            return new Respuesta(res.code(), body != null ? body.string() : "");
        }
    }

    static Bitmap reducir(Bitmap foto) {
        int lado = Math.max(foto.getWidth(), foto.getHeight());
        if (lado <= MAX_LADO) {
            return foto;
        }
        float escala = (float) MAX_LADO / lado;
        return Bitmap.createScaledBitmap(foto, Math.round(foto.getWidth() * escala), Math.round(foto.getHeight() * escala), true);
    }

    static byte[] aJpeg(Bitmap foto) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        reducir(foto).compress(Bitmap.CompressFormat.JPEG, 85, out);
        return out.toByteArray();
    }

    // ---------------------------------------------------------- resultados

    public static final class Desafio {
        public final String token;
        public final String tipo;        // "turn_left" | "turn_right"
        public final String instruccion; // texto para mostrar

        Desafio(String token, String tipo, String instruccion) {
            this.token = token;
            this.tipo = tipo;
            this.instruccion = instruccion;
        }

        public boolean esIzquierda() {
            return "turn_left".equals(tipo);
        }
    }

    public static final class Respuesta {
        public final int status;
        public final String texto;
        public final JSONObject json;

        public Respuesta(int status, String texto) {
            this.status = status;
            this.texto = texto == null ? "" : texto;
            JSONObject parsed;
            try {
                parsed = new JSONObject(this.texto);
            } catch (JSONException e) {
                parsed = new JSONObject();
            }
            this.json = parsed;
        }

        public boolean esExitosa() {
            return status >= 200 && status < 300;
        }

        /** true si verify/identify reconoció a la persona. */
        public boolean coincide() {
            return esExitosa() && json.optBoolean("matched", false);
        }

        public String codigo() {
            return json.optString("code", null);
        }

        public String mensaje() {
            Object detalle = json.opt(esExitosa() ? "message" : "error");
            if (esExitosa() && detalle == null) return "Operación completada";
            if (detalle instanceof String) {
                return (String) detalle;
            }
            return status == 422 ? "Datos inválidos enviados a la API." : "Error HTTP " + status;
        }

        /** "Foto 2: Acérquese un poco a la cámara." por cada foto rechazada. */
        public List<String> motivosFotos() {
            List<String> motivos = new ArrayList<>();
            JSONArray frames = json.optJSONArray("frames");
            if (frames != null) {
                for (int i = 0; i < frames.length(); i++) {
                    JSONObject f = frames.optJSONObject(i);
                    if (f != null && !f.optBoolean("accepted", true)) {
                        motivos.add("Foto " + (f.optInt("index") + 1) + ": " + f.optString("reason"));
                    }
                }
            }
            return motivos;
        }
    }
}

