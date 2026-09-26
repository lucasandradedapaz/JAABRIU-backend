package jaabriu.jaabriu_backend.util;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.safety.Safelist;

import java.util.regex.Pattern;

/**
 * Sanitiza o HTML produzido pelo editor de solução antes de persistir/
 * exibir.
 *
 * Regra (item 3.2 do pedido): nunca armazenar/renderizar <script>,
 * atributos "on..." (onclick etc.), links "javascript:" ou qualquer marcação
 * fora do que o próprio editor permite (negrito, itálico, sublinhado,
 * alinhamento, listas, link, títulos, citação).
 *
 * Estratégia: allowlist (Safelist.none() + só o que é explicitamente
 * liberado) em vez de blocklist — é a forma segura de sanitizar HTML,
 * porque qualquer tag/atributo não listado é removido por padrão.
 */
public final class HtmlSanitizer {

    // Único uso de "style" que o editor gera é alinhamento de parágrafo.
    private static final Pattern ESTILO_PERMITIDO =
            Pattern.compile("^text-align:\\s*(left|center|right|justify)\\s*;?$", Pattern.CASE_INSENSITIVE);

    private static final Safelist SAFELIST = new Safelist()
            .addTags(
                    "p", "br", "b", "strong", "i", "em", "u",
                    "ul", "ol", "li",
                    "a",
                    "h3", "h4",
                    "blockquote",
                    "span", "div"
            )
            .addAttributes("a", "href", "target", "rel")
            .addAttributes("p", "style")
            .addAttributes("div", "style")
            .addAttributes("span", "style")
            .addAttributes("li", "style")
            .addProtocols("a", "href", "http", "https", "mailto")
            .addEnforcedAttribute("a", "target", "_blank")
            .addEnforcedAttribute("a", "rel", "noopener noreferrer");

    private HtmlSanitizer() {
    }

    /**
     * Limpa o HTML recebido do editor. Remove completamente qualquer tag ou
     * atributo fora da allowlist acima (inclui <script>, on*, javascript:).
     */
    public static String sanitizar(String htmlOriginal) {
        if (htmlOriginal == null) {
            return null;
        }

        String limpo = Jsoup.clean(htmlOriginal, SAFELIST);

        // O Safelist libera o atributo "style" nos elementos acima, mas não
        // restringe QUAIS propriedades CSS — então filtramos manualmente
        // pra só sobrar alinhamento de texto, nada mais.
        Document doc = Jsoup.parseBodyFragment(limpo);
        for (Element el : doc.body().getAllElements()) {
            if (el.hasAttr("style")) {
                String estilo = el.attr("style").trim();
                if (!ESTILO_PERMITIDO.matcher(estilo).matches()) {
                    el.removeAttr("style");
                }
            }
        }

        return doc.body().html().trim();
    }

    /**
     * Considera "vazio" um HTML que, sem as tags, não tem nenhum texto
     * visível (ex: "<p></p>" ou "<p><br></p>" que o editor gera quando o
     * usuário apaga tudo).
     */
    public static boolean isBlank(String html) {
        if (html == null) {
            return true;
        }
        return Jsoup.parse(html).text().trim().isEmpty();
    }
}
