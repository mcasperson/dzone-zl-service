package com.matthewcasperson.dzonezl.services.impl;

import com.matthewcasperson.dzonezl.services.HtmlSanitiser;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
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
        stripParentDivs(doc);
        removeLeadingWhitespace(doc);
        return doc.toString();
    }

    /**
     * Remove all leading elements with no text content.
     * @param doc
     */
    private void removeLeadingWhitespace(final Document doc) {
        while (doc.getAllElements().size() != 0 &&
                doc.getAllElements().get(0).text().trim().isEmpty()) {
            doc.getAllElements().get(0).remove();
        }
    }

    /**
     * Sometimes the imported content contains images with multiple srcs, often from a
     * srcset attribute. here we attempt to get the biggest image from that set.
     * @param doc
     */
    private void fixUpImages(final Document doc) {
        final Elements images = doc.select("img");

        for (final Element image : images) {
            final String src = image.attr("src");
            final String srcDecoded = URLDecoder.decode(src);
            if (srcDecoded.contains(" ")) {
                final String[] split = srcDecoded.split(" ");

                for (int index = split.length - 1; index >= 0; --index) {
                    final String splitSrc = split[index];
                    if (index == 0  || !splitSrc.matches("\\d+w")) {
                        image.attr("src",splitSrc);
                        break;
                    }
                }
            }
        }
    }

    /**
     * Remove all divs that contain no text that are wrapping up the document
     * @param doc
     */
    private void stripParentDivs(final Document doc) {
        while(true) {
            final Elements html = doc.getElementsByTag("body");
            if (html.size() == 1) {
                final Element div = html.get(0).child(0);
                if (div != null && div.tagName().equalsIgnoreCase("div")) {
                    for (final Node child : div.childNodes()) {
                        if (child instanceof TextNode && StringUtils.isNotBlank(child.toString())) {
                            return;
                        }
                    }

                    div.unwrap();
                } else {
                    return;
                }
            } else {
                return;
            }
        }
    }
}
