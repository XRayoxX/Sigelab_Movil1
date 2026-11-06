package sigelab.facial;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Matrix;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.activity.ComponentActivity;
import gt.edu.sigelab.mobile.R;
import gt.edu.sigelab.mobile.BuildConfig;
import gt.edu.sigelab.mobile.ui.theme.SigelabNativeTheme;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Pantalla de captura facial para SIGELAB móvil.
 *
 * Abre la cámara FRONTAL (CameraX), guía al usuario, toma las fotos y llama a la API.
 * Devuelve el resultado con setResult():  extras "status" (int) y "json" (String).
 *
 * Abrirla desde otra Activity:
 * <pre>
 * Intent i = new Intent(this, CapturaFacialActivity.class);
 * i.putExtra(CapturaFacialActivity.EXTRA_MODO, CapturaFacialActivity.MODO_VERIFICAR);
 * i.putExtra(CapturaFacialActivity.EXTRA_ID_USUARIO, idUsuario);
 * lanzadorFacial.launch(i);
 * </pre>
 */
public class CapturaFacialActivity extends ComponentActivity {

    public static final String EXTRA_MODO = "modo";
    public static final String EXTRA_ID_USUARIO = "idUsuario";
    public static final String EXTRA_REEMPLAZAR = "reemplazar";
    public static final String EXTRA_TOKEN = "token";
    public static final String MODO_REGISTRAR = "REGISTRAR";
    public static final String MODO_VERIFICAR = "VERIFICAR";
    public static final String MODO_IDENTIFICAR = "IDENTIFICAR";
    public static final String RESULT_STATUS = "status";
    public static final String RESULT_JSON = "json";

    /**
     * Las fotos en memoria de CameraX (ImageProxy) normalmente llegan SIN espejo, aunque la vista previa sí
     * se vea como espejo. Si la API responde "Se detectó el giro hacia el lado contrario" en su teléfono,
     * cambie este valor a true.
     */
    private static final boolean FOTOS_EN_ESPEJO = false;

    private static final String FRENTE = "Mire de frente a la cámara";
    private static final String IZQUIERDA = "Gire ligeramente la cabeza hacia SU IZQUIERDA";
    private static final String DERECHA = "Gire ligeramente la cabeza hacia SU DERECHA";

    private PreviewView vistaPrevia;
    private TextView instruccion;
    private TextView estado;
    private Button botonCerrar;

    private ImageCapture imageCapture;
    private final ExecutorService fondo = Executors.newSingleThreadExecutor();
    private volatile boolean cancelado;
    private FacialApiClient api;
    private String modo;
    private int idUsuario;
    private boolean reemplazar;
    private FacialApiClient.Respuesta resultado;

    private final ActivityResultLauncher<String> permisoCamara =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), concedido -> {
                if (concedido) {
                    iniciarCamara();
                } else {
                    mostrarError("Se necesita permiso de cámara para el reconocimiento facial.");
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_captura_facial);
        vistaPrevia = findViewById(R.id.vistaPrevia);
        instruccion = findViewById(R.id.textoInstruccion);
        estado = findViewById(R.id.textoEstado);
        botonCerrar = findViewById(R.id.botonCerrar);
        SigelabNativeTheme.applyFacial(this);
        botonCerrar.setOnClickListener(v -> terminar());

        modo = getIntent().getStringExtra(EXTRA_MODO);
        if (modo == null) {
            modo = MODO_VERIFICAR;
        }
        idUsuario = getIntent().getIntExtra(EXTRA_ID_USUARIO, 0);
        reemplazar = getIntent().getBooleanExtra(EXTRA_REEMPLAZAR, false);
        String token = getIntent().getStringExtra(EXTRA_TOKEN);
        if (token == null || token.isBlank()) {
            mostrarError("Inicie sesión nuevamente antes de usar el reconocimiento facial.");
            return;
        }
        api = new FacialApiClient(BuildConfig.API_URL, token);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            iniciarCamara();
        } else {
            permisoCamara.launch(Manifest.permission.CAMERA);
        }
    }

    private void iniciarCamara() {
        ListenableFuture<ProcessCameraProvider> futuro = ProcessCameraProvider.getInstance(this);
        futuro.addListener(() -> {
            try {
                ProcessCameraProvider proveedor = futuro.get();
                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(vistaPrevia.getSurfaceProvider());
                imageCapture = new ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                        .build();
                proveedor.unbindAll();
                proveedor.bindToLifecycle(this, CameraSelector.DEFAULT_FRONT_CAMERA, preview, imageCapture);
                fondo.execute(this::flujoDeCaptura);
            } catch (Exception e) {
                mostrarError("No se pudo abrir la cámara frontal: " + e.getMessage());
            }
        }, ContextCompat.getMainExecutor(this));
    }

    /** Se ejecuta en el hilo de fondo: instrucciones, fotos y llamada a la API. */
    private void flujoDeCaptura() {
        try {
            mostrar("Conectando con el servidor...", "");
            FacialApiClient.Desafio desafio = null;
            List<String> pasos = new ArrayList<>();
            List<Integer> fotosPorPaso = new ArrayList<>();
            if (MODO_REGISTRAR.equals(modo)) {
                agregar(pasos, fotosPorPaso, FRENTE, 3);
                agregar(pasos, fotosPorPaso, IZQUIERDA, 2);
                agregar(pasos, fotosPorPaso, DERECHA, 2);
                agregar(pasos, fotosPorPaso, FRENTE, 1);
            } else {
                desafio = api.desafio(MODO_VERIFICAR.equals(modo) ? idUsuario : null);
                agregar(pasos, fotosPorPaso, FRENTE, 2);
                agregar(pasos, fotosPorPaso, desafio.esIzquierda() ? IZQUIERDA : DERECHA, 2);
            }

            List<Bitmap> fotos = new ArrayList<>();
            for (int p = 0; p < pasos.size(); p++) {
                for (int s = 3; s >= 1; s--) {
                    if (cancelado) return;
                    mostrar(pasos.get(p), "Foto en " + s + "...");
                    Thread.sleep(800);
                }
                for (int i = 0; i < fotosPorPaso.get(p); i++) {
                    if (cancelado) return;
                    mostrar(pasos.get(p), "Capturando " + (i + 1) + "/" + fotosPorPaso.get(p));
                    fotos.add(FacialApiClient.reducir(tomarFoto()));
                    Thread.sleep(300);
                }
            }

            mostrar("Procesando...", "Enviando " + fotos.size() + " fotos al servidor");
            FacialApiClient.Respuesta r;
            if (MODO_REGISTRAR.equals(modo)) {
                r = api.registrar(idUsuario, reemplazar, fotos, FOTOS_EN_ESPEJO);
            } else if (MODO_VERIFICAR.equals(modo)) {
                r = api.verificar(idUsuario, desafio.token, fotos, FOTOS_EN_ESPEJO);
            } else {
                r = api.identificar(desafio.token, fotos, FOTOS_EN_ESPEJO);
            }
            runOnUiThread(() -> mostrarResultado(r));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            runOnUiThread(() -> mostrarError("No se pudo comunicar con el servidor: " + e.getMessage()));
        }
    }

    private static void agregar(List<String> pasos, List<Integer> fotos, String texto, int cantidad) {
        pasos.add(texto);
        fotos.add(cantidad);
    }

    /** Toma una foto en memoria (sin guardarla en el teléfono) y la endereza según la rotación. */
    private Bitmap tomarFoto() throws Exception {
        final Bitmap[] foto = new Bitmap[1];
        final Exception[] error = new Exception[1];
        CountDownLatch listo = new CountDownLatch(1);
        imageCapture.takePicture(fondoCamara, new ImageCapture.OnImageCapturedCallback() {
            @Override
            public void onCaptureSuccess(@NonNull ImageProxy image) {
                try {
                    Bitmap bitmap = image.toBitmap();
                    int rotacion = image.getImageInfo().getRotationDegrees();
                    if (rotacion != 0) {
                        Matrix m = new Matrix();
                        m.postRotate(rotacion);
                        bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), m, true);
                    }
                    foto[0] = bitmap;
                } catch (Exception e) {
                    error[0] = e;
                } finally {
                    image.close();
                    listo.countDown();
                }
            }

            @Override
            public void onError(@NonNull ImageCaptureException exception) {
                error[0] = exception;
                listo.countDown();
            }
        });
        if (!listo.await(10, TimeUnit.SECONDS)) {
            throw new Exception("La cámara no respondió.");
        }
        if (error[0] != null) {
            throw error[0];
        }
        return foto[0];
    }

    private final ExecutorService fondoCamara = Executors.newSingleThreadExecutor();

    private void mostrar(String texto, String detalle) {
        runOnUiThread(() -> {
            instruccion.setText(texto);
            estado.setText(detalle);
        });
    }

    private void mostrarResultado(FacialApiClient.Respuesta r) {
        resultado = r;
        boolean ok = MODO_REGISTRAR.equals(modo) ? r.esExitosa() : r.coincide();
        instruccion.setTextColor(SigelabNativeTheme.resultColor(this, ok));
        instruccion.setText(r.mensaje());
        List<String> motivos = r.motivosFotos();
        estado.setText(motivos.isEmpty() ? (ok ? "Listo" : String.valueOf(r.codigo())) : TextUtils.join("\n", motivos));
        botonCerrar.setText("Continuar");
    }

    private void mostrarError(String mensaje) {
        instruccion.setTextColor(SigelabNativeTheme.resultColor(this, false));
        instruccion.setText(mensaje);
        botonCerrar.setText("Cerrar");
    }

    private void terminar() {
        cancelado = true;
        if (resultado != null) {
            Intent datos = new Intent();
            datos.putExtra(RESULT_STATUS, resultado.status);
            datos.putExtra(RESULT_JSON, resultado.texto);
            setResult(RESULT_OK, datos);
        } else {
            setResult(RESULT_CANCELED);
        }
        finish();
    }

    @Override
    protected void onDestroy() {
        cancelado = true;
        fondo.shutdownNow();
        fondoCamara.shutdownNow();
        super.onDestroy();
    }
}

