package de.word_light.document_builder.exception;


/**
 * General format any exception thrown in this api should have so the frontend can rely
 * on this object's fields. <p>
 * 
 * Don't change this class for above reason!
 * 
 * @since 0.0.1
 */
public record ApiExceptionFormat(
    int status,

    String error,

    String message,

    String path
) { }