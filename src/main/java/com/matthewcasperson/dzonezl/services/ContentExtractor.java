package com.matthewcasperson.dzonezl.services;

import com.matthewcasperson.dzonezl.entities.ContentImport;

import java.util.Map;
import java.util.Optional;

/**
 * A service that can extract content from a web page
 */
public interface ContentExtractor {
    /**
     *
     * @param url The URL to be imported
     * @param data General data used by the importing service
     * @return The imported content
     */
    Optional<ContentImport> extractContent(final String url, final Map<String, String> data);
}
