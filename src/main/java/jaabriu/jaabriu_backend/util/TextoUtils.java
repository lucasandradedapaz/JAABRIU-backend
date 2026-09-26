package jaabriu.jaabriu_backend.util;

import java.text.Normalizer;
import java.util.regex.Pattern;

/**
 * Pequeno utilitário de texto usado pelos autocompletes (setor e técnicos):
 * normaliza removendo acentos e caixa, para que "tec" encontre "Tecnologia"
 * e "geas" encontre "GEAS" independente de maiúsculas/acentuação.
 */
public final class TextoUtils {

    private static final Pattern MARCAS_DIACRITICAS = Pattern.compile("\\p{M}");

    private TextoUtils() {
    }

    public static String normalizar(String texto) {
        if (texto == null) {
            return "";
        }
        String semAcento = Normalizer.normalize(texto, Normalizer.Form.NFD);
        semAcento = MARCAS_DIACRITICAS.matcher(semAcento).replaceAll("");
        return semAcento.trim().toLowerCase();
    }
}
