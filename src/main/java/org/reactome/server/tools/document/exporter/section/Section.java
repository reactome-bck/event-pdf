package org.reactome.server.tools.document.exporter.section;

import com.itextpdf.layout.Document;
import org.reactome.server.tools.document.exporter.DocumentContent;
import org.reactome.server.tools.document.exporter.exception.DocumentExporterException;

/**
 * Whole PDF report will be split into different sections.
 *
 * @author Chuan-Deng dengchuanbio@gmail.com
 */
public interface Section {

	void render(Document document, DocumentContent content) throws DocumentExporterException;
}
