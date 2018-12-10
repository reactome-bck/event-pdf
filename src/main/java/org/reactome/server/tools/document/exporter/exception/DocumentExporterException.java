package org.reactome.server.tools.document.exporter.exception;

public class DocumentExporterException extends RuntimeException {
	public DocumentExporterException(String message) {
		super(message);
	}

	public DocumentExporterException(String message, Throwable throwable) {
		super(message, throwable);
	}
}
