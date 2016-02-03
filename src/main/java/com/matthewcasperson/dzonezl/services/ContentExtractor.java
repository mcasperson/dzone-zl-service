package com.matthewcasperson.dzonezl.services;

import com.matthewcasperson.dzonezl.entities.ContentImport;

import java.util.Map;
import java.util.Optional;

/**
 * A service that can extract content from a web page
 */
public interface ContentExtractor {
    Optional<ContentImport> extractContent(final String url, final Map<String, String> data);
}
