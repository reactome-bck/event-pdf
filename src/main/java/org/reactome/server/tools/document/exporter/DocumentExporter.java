package org.reactome.server.tools.document.exporter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.itextpdf.kernel.events.PdfDocumentEvent;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import org.reactome.server.analysis.core.result.AnalysisStoredResult;
import org.reactome.server.analysis.core.result.model.ResourceSummary;
import org.reactome.server.graph.domain.model.Event;
import org.reactome.server.graph.service.*;
import org.reactome.server.tools.document.exporter.exception.DocumentExporterException;
import org.reactome.server.tools.document.exporter.profile.PdfProfile;
import org.reactome.server.tools.document.exporter.section.*;
import org.reactome.server.tools.document.exporter.util.Diagrams;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.*;

public class DocumentExporter {

	private static final ObjectMapper MAPPER = new ObjectMapper();
	private DatabaseObjectService databaseObjectService;
	private GeneralService generalService;
	private ParticipantService participantService;


	public DocumentExporter(String diagramPath, String ehldPath, String analysisPath, String fireworksPath, String svgSummary, DiagramService diagramService, DatabaseObjectService databaseObjectService, GeneralService generalService, AdvancedDatabaseObjectService advancedDatabaseObjectService, ParticipantService participantService) {
		this.databaseObjectService = databaseObjectService;
		this.generalService = generalService;
		this.participantService = participantService;
		Diagrams.setPaths(diagramPath, ehldPath, analysisPath, fireworksPath, svgSummary);
		Locale.setDefault(Locale.ENGLISH);
		Diagrams.setDiagramService(diagramService);
		Diagrams.setDatabaseObjectService(databaseObjectService);
		Diagrams.setAdvancedDatabaseObjectService(advancedDatabaseObjectService);
		AnalysisData.setDatabaseObjectService(databaseObjectService);
	}

	public void export(DocumentArgs args, AnalysisStoredResult result, OutputStream destination) throws DocumentExporterException {
		final Event event = databaseObjectService.findById(args.getStId());
		if (event == null) throw new DocumentExporterException(args.getStId() + " is not an event");

		final PdfProfile pdfProfile = loadProfile("breathe");

		final AnalysisData analysisData;
		if (result != null) {
			// if the analysis result not contains the given resource, use the first resource in this analysis.
			if (!result.getResourceSummary().contains(new ResourceSummary(args.getResource(), null)))
				args.setResource(getDefaultResource(result));

			analysisData = new AnalysisData(result, args.getResource(), args.getSpecies(), Integer.MAX_VALUE);
		} else analysisData = null;
		final DocumentContent content = new DocumentContent(analysisData, pdfProfile, event, args);
		try (Document document = new Document(new PdfDocument(new PdfWriter(destination)))) {
			document.getPdfDocument().getDocumentInfo().setAuthor(String.format("Reactome (%s)", content.getServer()));
			document.getPdfDocument().getDocumentInfo().setCreator(String.format("Reactome (%s)", content.getServer()));
			document.getPdfDocument().getDocumentInfo().setTitle(String.format("Reactome | %s (%s)", event.getDisplayName(), args.getStId()));
			document.getPdfDocument().getDocumentInfo().setSubject(String.format("Reactome | %s (%s)", event.getDisplayName(), args.getStId()));
			document.getPdfDocument().getDocumentInfo().setKeywords("pathway,reactome,reaction");
			document.setFont(pdfProfile.getRegularFont());
			document.setMargins(pdfProfile.getMargin().getTop(),
					pdfProfile.getMargin().getRight(),
					pdfProfile.getMargin().getBottom(),
					pdfProfile.getMargin().getLeft());
			document.getPdfDocument().addEventHandler(PdfDocumentEvent.START_PAGE, new FooterEventHandler(document, pdfProfile, content.getServer()));
			final Map<Long, Integer> pages = new HashMap<>();
			final List<Section> SECTIONS = Arrays.asList(
					new CoverPage(),
					new Introduction(generalService),
					new PropertiesSection(),
					new PathwaysDetails(participantService, pages),
					new TableOfContent(pages)
			);
			for (Section section : SECTIONS)
				section.render(document, content);
		}
	}

	private PdfProfile loadProfile(String profile) throws DocumentExporterException {
		try {
			final InputStream resource = getClass().getResourceAsStream("/profiles/" + profile.toLowerCase() + ".json");
			return MAPPER.readValue(resource, PdfProfile.class);
		} catch (IOException e) {
			throw new DocumentExporterException("Couldn't load profile " + profile, e);
		}
	}

	private String getDefaultResource(AnalysisStoredResult result) {
		final List<ResourceSummary> summary = result.getResourceSummary();
		// Select the second one since first one always "TOTAL" .
		return summary.size() == 2
				? summary.get(1).getResource()
				: summary.get(0).getResource();
	}


}
