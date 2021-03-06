package edu.isi.bmkeg.uimaBioC.uima.readers;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.stream.XMLStreamException;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.collection.CollectionException;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.Progress;
import org.apache.uima.util.ProgressImpl;
import org.uimafit.component.JCasCollectionReader_ImplBase;
import org.uimafit.descriptor.ConfigurationParameter;
import org.uimafit.factory.ConfigurationParameterFactory;

import com.google.gson.Gson;

import bioc.BioCDocument;
import bioc.io.BioCDocumentReader;
import bioc.io.BioCFactory;
import edu.isi.bmkeg.uimaBioC.UimaBioCUtils;

/**
 * We want to optimize this interaction for speed, so we run a
 * manual query over the underlying database involving a minimal subset of
 * tables.
 * 
 * @author burns
 * 
 */
public class BioCCollectionReader extends JCasCollectionReader_ImplBase {
	
	private Iterator<File> bioCFileIt; 
	private File bioCFile; 
	private BioCDocument bioD;	
	
	private int pos = 0;
	private int count = 0;
	
	private static Logger logger = Logger.getLogger(BioCCollectionReader.class);
	
	public static final String INPUT_DIRECTORY = ConfigurationParameterFactory
			.createConfigurationParameterName(BioCCollectionReader.class,
					"inputDirectory");
	@ConfigurationParameter(mandatory = true, description = "Input Directory for BioC Files")
	protected String inputDirectory;

	/*
	 * If this is set, then we expect to output BioC files to this directory.
	 * Also, if we detect an appropriately named file in this directory, we'll
	 * skip to the next JCas.
	 */
	public static final String OUTPUT_DIRECTORY = ConfigurationParameterFactory
			.createConfigurationParameterName(BioCCollectionReader.class,
					"outputDirectory");
	@ConfigurationParameter(mandatory = false, description = "Output Directory for BioC Files")
	protected String outputDirectory;
	protected Set<String> existingFiles;
	
	public static String XML = "xml";
	public static String JSON = "json";
	public final static String PARAM_FORMAT = ConfigurationParameterFactory
			.createConfigurationParameterName(BioCCollectionReader.class,
					"inFileFormat");
	@ConfigurationParameter(mandatory = true, description = "The format of the BioC input files.")
	String inFileFormat;
	
	Pattern patt = Pattern.compile("^(.*?)[_\\.]");
	
	@Override
	public void initialize(UimaContext context) throws ResourceInitializationException {

		try {
			
			String[] fileTypes = {"xml", "txt", "json", "tsv"};
			Collection<File> l = (Collection<File>) FileUtils.listFiles(
					new File(inputDirectory), fileTypes, true);
			
			this.existingFiles = new HashSet<String>();
			if( outputDirectory != null ) {
				File outDir = new File(outputDirectory);
				if(!outDir.exists())
					outDir.mkdirs();
				for(Object o : FileUtils.listFiles(
						new File(outputDirectory), fileTypes, true)) {
					String fName = ((File) o).getName();
					Matcher m = patt.matcher(fName);
					if( m.find() ) {
						this.existingFiles.add(m.group(1));
					}
				}
			}
			
			this.bioCFileIt = l.iterator();
			this.count = l.size();
			
		} catch (Exception e) {

			e.printStackTrace();
			throw new ResourceInitializationException(e);

		}

	}

	/**
	 * @see com.ibm.uima.collection.CollectionReader#getNext(com.ibm.uima.cas.CAS)
	 */
	public void getNext(JCas jcas) throws IOException, CollectionException {

		try {
						
			UimaBioCUtils.addBioCDocumentToUimaCas(this.bioD, jcas);
						
			logger.debug("Processing " + bioCFile.getName() + "." );
		    
		} catch (Exception e) {
			
			System.err.print(this.count);
			throw new CollectionException(e);

		}

	}
		
	protected void error(String message) {
		logger.error(message);
	}

	@SuppressWarnings("unused")
	protected void warn(String message) {
		logger.warn(message);
	}

	@SuppressWarnings("unused")
	protected void debug(String message) {
		logger.error(message);
	}

	public Progress[] getProgress() {		
		Progress progress = new ProgressImpl(
				this.pos, 
				this.count, 
				Progress.ENTITIES);
		
        return new Progress[] { progress };
	}

	@Override
	public boolean hasNext() throws IOException, CollectionException {

		try {

			if( !bioCFileIt.hasNext() )
				return false;

			this.bioCFile = bioCFileIt.next();
			this.bioD = UimaBioCUtils.readBioCFile(bioCFile);
			
			while( this.existingFiles.contains(bioD.getID())) {
				logger.debug("output file for " + bioCFile.getName() + " exists, skipping." );
				
				if( !bioCFileIt.hasNext() )
					return false;
				
				bioCFile = bioCFileIt.next();
				bioD = UimaBioCUtils.readBioCFile(bioCFile);
			}
			
			return true;
		
		} catch (Exception e) {
			throw new CollectionException(e);
		} 

	}

}
