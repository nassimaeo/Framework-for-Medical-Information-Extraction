package evaluation;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.StringTokenizer;

/**
 * This class concept identification used by UMLS into their equivalent Snomed-ct cui
 * It uses the MRCONSO.RRF file. 
 * 
 * @author Nassim
 *
 */
public class UMLS {
	
	// UMLS File containing the mapping between SNOMED-CT_id and UMLS_CUI
	public static final String CUI_SNOMED_ID_FILE = "res/eval/MRCONSO.RRF";
	
	// SNOMED_ID to UMLS_CUI
	private HashMap<String, String> mappingSNOMEDCTidToUMLSCUI;
	
	/**
	 * Read the file MRCONSO.RRF provided by UMLS containing mapping of UMLS_CUI and SNOMED_ID
	 * Then save all the SNOMED_ID to UMLS_CUI in  a HashMap 
	 * 
	 * Eg:
	 * C0000039|ENG|P|L0012507|PF|S0033298|Y|A22817493|166113012|102735002||SNOMEDCT_US|OAP|102735002|Dipalmitoylphosphatidylcholine|9|O|256|
	 * 
	 * @return mapping SNOMED_CT_ids <-> UMLS_CUI
	 * @throws IOException
	 */	
	public UMLS() throws IOException{
		
        // Stores the SNOMED_ID: --> UMLS_CUI
		mappingSNOMEDCTidToUMLSCUI = new HashMap<>();

		//Reading the file
		BufferedReader bufferedReader = null;
	    bufferedReader = new BufferedReader(new FileReader(CUI_SNOMED_ID_FILE));
	        
	    // Get the fields
	    System.out.println("Loading the mapping SNOMEDCT_ID <-> UMLS_CUI concepts : ");
	        	        
	    int counter = 0;
	    int doubles = 0;
	    // Load the concepts.
        do {
            String line = bufferedReader.readLine();
            counter++;
            if (line == null) break;
            String[] fields = line.split("\\|");
            if (fields[6].equals("N") || fields[2].equals("P")) continue;
            String CUI = fields[0];
            String snomedID = fields[9];
            //System.out.println(line + " [" + SnomedID + " --> " + CUI + "]");
            if (CUI.length() > 0 && snomedID.length() > 0) {
            	if (mappingSNOMEDCTidToUMLSCUI.containsKey(snomedID)) {
            		if (!mappingSNOMEDCTidToUMLSCUI.get(snomedID).equals(CUI)) {
            			doubles++;
            			//System.out.println("1 - " + snomedID + " " + mappingSNOMEDidtoUMLSCUI.get(snomedID));
            			//System.out.println("2 - " + snomedID + " " + CUI);
            		}
            	}
            	mappingSNOMEDCTidToUMLSCUI.put(snomedID, CUI);
            }
        } while(true);
        bufferedReader.close();
        System.out.println("Loading of the mapping is done (mappingsFound/totalLines-ignoredDoublesMultiConceptsInUMLS)! (" +mappingSNOMEDCTidToUMLSCUI.size()+"/"+counter+"-"+doubles+")");
		
	}
	
	/**
	 * Search the SnomedID in MRCONSO.RRF and return its equivalent UMLS CUI
	 * 
	 * @param snomedId
	 * @return UMLS CUI
	 */
	public String getCUIofSnomedId(String snomedId) {
		return this.mappingSNOMEDCTidToUMLSCUI.get(snomedId);
	}
	
	
	public static void main(String[] args) {
		try {
			//Eval.allCUIinGoldTest();
			UMLS umls = new UMLS();
			System.out.println(umls.getCUIofSnomedId("25064002"));
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
