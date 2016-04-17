package com.matthewcasperson.dzonezl.services;

import javax.validation.constraints.NotNull;

/**
 * A service that cleans up HTML before returning it to the client
 */
public interface HtmlSanitiser {
    String sanitiseHtml(@NotNull final String input);
}
