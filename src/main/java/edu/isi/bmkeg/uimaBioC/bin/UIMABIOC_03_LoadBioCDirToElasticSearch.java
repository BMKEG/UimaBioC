package edu.isi.bmkeg.uimaBioC.bin;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collection;

import javax.xml.stream.XMLStreamException;

import org.apache.commons.io.FileUtils;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.stereotype.Component;

import com.google.gson.Gson;

import bioc.BioCDocument;
import bioc.esViews.BioCDocument.BioCDocument__BioCAnnotation;
import bioc.esViews.BioCDocument.BioCDocument__BioCDocument;
import bioc.esViews.BioCDocument.BioCDocument__BioCLocation;
import bioc.esViews.BioCDocument.BioCDocument__BioCPassage;
import bioc.io.BioCDocumentReader;
import bioc.io.BioCFactory;
import edu.isi.bmkeg.uimaBioC.elasticSearch.BioCDocumentRepository;

@Component
public class UIMABIOC_03_LoadBioCDirToElasticSearch {

	private static final Logger logger = LoggerFactory.getLogger(UIMABIOC_03_LoadBioCDirToElasticSearch.class);

	@Autowired
	BioCDocumentRepository biocRepo;

	public static class Options {

		@Option(name = "-biocDirectory", usage = "BioC Director", required = true, metaVar = "BIOC_DIRECTORY")
		public String biocDirectory = "";

	}

	public static void main(String[] args) throws IOException, XMLStreamException, CmdLineException {

		Options options = new Options();

		CmdLineParser parser = new CmdLineParser(options);

		try {

			parser.parseArgument(args);

		} catch (CmdLineException e) {

			System.err.print("Arguments: ");
			parser.printSingleLineUsage(System.err);
			System.err.println("\n\n Options: \n");
			parser.printUsage(System.err);
			throw e;

		}

		// Use annotated beans from the specified package
		ApplicationContext ctx = new AnnotationConfigApplicationContext("edu.isi.bmkeg.uimaBioC");

		UIMABIOC_03_LoadBioCDirToElasticSearch main = ctx.getBean(UIMABIOC_03_LoadBioCDirToElasticSearch.class);

		logger.info("BioC File Location: " + options.biocDirectory);

		String[] fileTypes = { "json", "xml" };
		Collection<File> fileList = (Collection<File>) FileUtils.listFiles(new File(options.biocDirectory), fileTypes,
				true);
		for (File f : fileList) {

			BioCDocument bioD = null;
			if (f.getName().endsWith(".json")) {

				Gson gson = new Gson();
				bioD = gson.fromJson(new FileReader(f), BioCDocument.class);

			} else {

				BioCDocumentReader reader = BioCFactory.newFactory(BioCFactory.STANDARD)
						.createBioCDocumentReader(new FileReader(f));

				bioD = reader.readDocument();

				reader.close();
			}

			BioCDocument__BioCDocument esBioD = main.convertToES(bioD);

			main.biocRepo.index(esBioD);

		}

	}

	public BioCDocument__BioCDocument convertToES(bioc.BioCDocument d) {
		
		BioCDocument__BioCDocument esBioD = new BioCDocument__BioCDocument();
		esBioD.setId( d.getID() );
		esBioD.setInfons(d.getInfons());
	
		for( bioc.BioCPassage p : d.getPassages() ) {
			BioCDocument__BioCPassage pp = new BioCDocument__BioCPassage();
			esBioD.getPassages().add(pp);
			
			pp.setOffset(p.getOffset());
			pp.setText(p.getText());
			pp.setInfons(p.getInfons());
	
			for( bioc.BioCAnnotation a : p.getAnnotations() ) {
				BioCDocument__BioCAnnotation aa = new BioCDocument__BioCAnnotation();
				pp.getAnnotations().add(aa);
				
				aa.setId(a.getID());
				aa.setInfons(a.getInfons());
				aa.setText(a.getText());
	
				for( bioc.BioCLocation l : a.getLocations() ) {
					BioCDocument__BioCLocation ll = new BioCDocument__BioCLocation();
					aa.getLocations().add(ll);
					
					ll.setLength(l.getLength());
					ll.setOffset(l.getOffset());
					
				}
	
			}
	
		}
		
		return esBioD;
	
	}
	
}
