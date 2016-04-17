package com.matthewcasperson.dzonezl.services.impl;

import com.matthewcasperson.dzonezl.services.HtmlSanitiser;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.DocumentType;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import javax.validation.constraints.NotNull;
import java.net.URLDecoder;

/**
 * An implementation of the HTML sanitiser service
 */
@Component
public class HtmlSanitiseImpl implements HtmlSanitiser {
    @Override
    public String sanitiseHtml(@NotNull final String input) {
        final Document doc = Jsoup.parse(input);
        fixUpImages(doc);
        return doc.toString();
    }

    /**
     * Sometimes the imported content contains images with multiple srcs, which fails
     * @param doc
     */
    private void fixUpImages(final Document doc) {
        final Elements images = doc.select("img");

        for (final Element image : images) {
            final String src = image.attr("src");
            final String srcDecoded = URLDecoder.decode(src);
            if (srcDecoded.contains(" ")) {
                image.attr("src", srcDecoded.split(" ")[0]);
            }
        }
    }
}
