package com.scholarsync.backend.exception;

import java.util.List;
public class ImportValidationException extends RuntimeException {
    private final List<String> errors;

    public ImportValidationException(List<String> errors) {
        super("Import validation failed");
        this.errors = errors;
    }

    public List<String> getErrors() {
        return errors;
    }
}
