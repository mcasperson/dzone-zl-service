package com.matthewcasperson.dzonezl.services.impl;

import com.matthewcasperson.dzonezl.entities.ContentImport;
import com.matthewcasperson.dzonezl.services.ContentExtractor;
import de.l3s.boilerpipe.BoilerpipeExtractor;
import de.l3s.boilerpipe.extractors.CommonExtractors;
import de.l3s.boilerpipe.sax.HTMLHighlighter;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.PrintWriter;
import java.net.URL;
import java.util.Map;
import java.util.Optional;

/**
 * A content scraper that uses the boilerpipe engine.
 */
@Component(value="boilerpipeContentExtractor")
public class BoilerpipeContentExtractor implements ContentExtractor {
    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(BoilerpipeContentExtractor.class);

    @Override
    public Optional<ContentImport> extractContent(final String sourceUrl, final Map<String, String> data) {
        try {
            final URL url = new URL(sourceUrl);

            // choose from a set of useful BoilerpipeExtractors...
            final BoilerpipeExtractor extractor = CommonExtractors.ARTICLE_EXTRACTOR;
            // final BoilerpipeExtractor extractor = CommonExtractors.DEFAULT_EXTRACTOR;
            // final BoilerpipeExtractor extractor = CommonExtractors.CANOLA_EXTRACTOR;
            // final BoilerpipeExtractor extractor = CommonExtractors.LARGEST_CONTENT_EXTRACTOR;

            // choose the operation mode (i.e., highlighting or extraction)
            //final HTMLHighlighter hh = HTMLHighlighter.newHighlightingInstance();
            final HTMLHighlighter hh = HTMLHighlighter.newExtractingInstance();

            final String content = hh.process(url, extractor);
            if (StringUtils.isNotBlank(content)) {
                return Optional.of(new ContentImport(content, ""));
            }

        } catch (final Exception ex) {
            LOGGER.error("Exception raised runnung Boilerpipe", ex);
        }

        return Optional.empty();
    }
}
