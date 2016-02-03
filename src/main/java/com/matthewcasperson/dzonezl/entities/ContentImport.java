package com.matthewcasperson.dzonezl.entities;

/**
 * Represents the extracted content of a web page
 */
public class ContentImport {
    private String content;
    private String title;

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public ContentImport() {

    }

    public ContentImport(final String content, final String title) {
        this.content = content;
        this.title = title;
    }
}
