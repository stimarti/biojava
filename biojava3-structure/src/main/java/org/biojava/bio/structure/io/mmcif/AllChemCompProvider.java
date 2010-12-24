package org.biojava.bio.structure.io.mmcif;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.concurrent.atomic.AtomicBoolean;


import org.biojava.bio.structure.align.ce.AbstractUserArgumentProcessor;
import org.biojava.bio.structure.io.mmcif.model.ChemComp;
import org.biojava3.core.util.InputStreamProvider;

/** A ChemComp provider that downloads and caches the components.cif file from the wwPDB site. It then loads
 * all chemical components at startup and keeps them in memory. This provider is not used as a default
 * since it is slower at startup and requires more memory than the {@link DownloadChemCompProvider} that is used by default.
 * 
 * @author Andreas Prlic
 *
 */
public class AllChemCompProvider implements ChemCompProvider, Runnable{

	static String path; 

	private static final String lineSplit = System.getProperty("file.separator");

	private static String serverLocation = "ftp://ftp.wwpdb.org/pub/pdb/data/monomers/";

	// there will be only one copy of the dictionary across all instances
	// to reduce memory impact
	static ChemicalComponentDictionary dict;

	// flags to make sure there is only one thread running that is loading the dictionary
	static AtomicBoolean loading       = new AtomicBoolean(false);
	static AtomicBoolean isInitialized = new AtomicBoolean(false);

	public AllChemCompProvider(){

		if ( loading.get()) {
			System.err.println("other thread is already loading all chemcomps, no need to init twice");
			return;
		}
		if ( isInitialized.get())
			return;

		loading.set(true);

		Thread t = new Thread(this);
		t.start();

	}


	/** make sure all paths are initialized correctly
	 * 
	 */
	public static void checkPath(){

		if ((path == null) || (path.equals("")) || path.equals("null")) {

			String syspath = System.getProperty(AbstractUserArgumentProcessor.PDB_DIR);

			if ((syspath != null) && (! syspath.equals("")) && (! syspath.equals("null"))){

				path = syspath;
				return;
			}

			// accessing temp. OS directory:         
			String property = "java.io.tmpdir";

			String tempdir = System.getProperty(property);

			if ( !(tempdir.endsWith(lineSplit) ) )
				tempdir = tempdir + lineSplit;

			System.err.println("you did not set the path in PDBFileReader, don't know where to write the downloaded file to");
			System.err.println("assuming default location is temp directory: " + tempdir);
			path = tempdir;
		}
	}

	private void ensureFileExists() {


		String fileName = getLocalFileName();
		File f = new File(fileName);

		if ( ! f.exists()) {
			try {
			downloadFile();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}



	}

	/** Downloads the components.cif.gz file from the wwPDB site.
	 * 
	 */
	public static void downloadFile() throws IOException{


		String localName = getLocalFileName();

		String u = serverLocation +  "components.cif.gz";

		System.out.println("downloading " + u);

		try {

			URL url = new URL(u);

			URLConnection uconn = url.openConnection();

			InputStream in = uconn.getInputStream();			
			
			FileOutputStream out = new FileOutputStream(localName);
			int c;
            while ((c = in.read()) != -1) {
                out.write(c);
            }

			in.close();
			out.flush();
			out.close();
			

		} catch (Exception e){
			e.printStackTrace();
		}


	}



	private static String getLocalFileName(){

		String dir = path + DownloadChemCompProvider.CHEM_COMP_CACHE_DIRECTORY + lineSplit;

		File f = new File(dir);
		if (! f.exists()){
			System.out.println("creating directory " + f);
			f.mkdir();
		}

		String fileName = path + DownloadChemCompProvider.CHEM_COMP_CACHE_DIRECTORY + lineSplit + "components.cif.gz";

		return fileName;
	}

	/** Load all {@link ChemComp} definitions into memory.
	 * 
	 */
	private void loadAllChemComps() {
		String fileName = getLocalFileName();
		InputStreamProvider isp = new InputStreamProvider();

		try {
			InputStream inStream = isp.getInputStream(fileName);

			MMcifParser parser = new SimpleMMcifParser();

			ChemCompConsumer consumer = new ChemCompConsumer();

			// The Consumer builds up the BioJava - structure object.
			// you could also hook in your own and build up you own data model.
			parser.addMMcifConsumer(consumer);

			parser.parse(new BufferedReader(new InputStreamReader(inStream)));

			dict = consumer.getDictionary();


		} catch (Exception e){
			e.printStackTrace();
		}
	}


	/** {@inheritDoc}
	 * 
	 */
	public ChemComp getChemComp(String recordName) {

		while ( loading.get()) {

			// another thread is still initializing the definitions
			try {
				// wait half a second

				Thread.sleep(500);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		return dict.getChemComp(recordName);
	}


	/** Do the actual loading of the dictionary in a thread.
	 * 
	 */
	public void run() {
		long timeS = System.currentTimeMillis();

		try {
			checkPath();

			ensureFileExists();

			loadAllChemComps();
		} catch (Exception e) {
			e.printStackTrace();
		}
		long timeE = System.currentTimeMillis();
		System.out.println("time to init chem comp dictionary: " + (timeE - timeS) / 1000 + " sec.");

		loading.set(false);
		isInitialized.set(true);		
	}

}