package snomed;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.StringTokenizer;

/**
 * 
 * This class represent a description as defined by SNOMED-CT
 * 
 * ========================
 * Loading the descriptions
 * ========================
 * id (descriptionId)
 * effectiveTime
 * active
 * moduleId
 * conceptId
 * languageCode
 * typeId
 * term
 * caseSignificanceId
 * ========================
 * 
 * @author Nassim
 * @version 2016 04
 */




class Description{
    
    private long descriptionId;
    private String effectiveTime;
    private boolean active;
    private long moduleId;
    private long conceptId;
    private String languageCode;
    private long typeId; //  [Synonym: 900000000000013009, FullySpecifiedName: 900000000000003001]
    private String term;
    private long caseSignificanceId; // [Case sensitive: 900000000000017005, 1st car insensitive: 900000000000020002]
    
    public Description(
            String descriptionId,
            String effectiveTime,
            String active,
            String moduleId,
            String conceptId,
            String languageCode,
            String typeId,
            String term,
            String caseSignificanceId            
            ) {
        this.descriptionId = Long.parseLong(descriptionId);
        this.effectiveTime = effectiveTime;
        if (active =="1") this.active = true;
        else this.active = false;
        this.moduleId = Long.parseLong(moduleId);
        this.conceptId = Long.parseLong(conceptId);
        this.languageCode = languageCode;
        this.typeId = Long.parseLong(typeId);
        this.term = term;
        this.caseSignificanceId = Long.parseLong(caseSignificanceId);
    }

    public boolean isFSN(){
        return this.typeId == 900000000000003001L;
    }
    public boolean isSynonym(){
        return this.typeId == 900000000000013009L;
    }
    public long getDescriptionId() {
        return descriptionId;
    }

    public String getEffectiveTime() {
        return effectiveTime;
    }

    public boolean isActive() {
        return active;
    }

    public long getModuleId() {
        return moduleId;
    }

    public long getConceptId() {
        return conceptId;
    }

    public String getLanguageCode() {
        return languageCode;
    }

    public long getTypeId() {
        return typeId;
    }

    public String getTerm() {
        return term;
    }

    public long getCaseSignificanceId() {
        return caseSignificanceId;
    }
    
}


public class DescriptionsTable implements Iterable<Description>{
    
    HashMap<Integer, Description> descriptions;
    
    class getDescriptions implements Iterator<Description>{
        private int i = 0;
        @Override
        public boolean hasNext() {
            return i < descriptions.size();
        }

        @Override
        public Description next() {
            return descriptions.get(i++);
        }
        
    }
    
    public DescriptionsTable(String descriptionsFile) throws IOException {
        /* ========================
         * Loading the descriptions
         * ========================
         * id (descriptionId)
         * effectiveTime
         * active
         * moduleId
         * conceptId
         * languageCode
         * typeId
         * term
         * caseSignificanceId
         */
        BufferedReader bufferedReader = null;
        StringTokenizer st = null;
        
        // the list of descriptions id --> descriptions
        this.descriptions = new HashMap<Integer, Description>();

        // read the file containing the descriptions
        bufferedReader = new BufferedReader(new FileReader(descriptionsFile));

        // getting the fields
        st = new StringTokenizer(bufferedReader.readLine());
        
        // print the fields
        System.out.println("Loading descriptions : ");
        while (st.hasMoreTokens()) System.out.println("\t" + st.nextToken());
        int counter = 0;
        do{
            String line = bufferedReader.readLine();
            if (line == null) break;
            String[] s = line.split("\t");
            if (s[2].equals("0")) continue; // ignore inactive terms.
            // When active = 0 the Description is not a valid and the associated Term should no longer 
            // be regarded as being associated with the Concept referred to by conceptId
            Description description = new Description(s[0],s[1],s[2],s[3],s[4],s[5],s[6],s[7],s[8]);
            descriptions.put(counter++, description);
        } while(true);
        bufferedReader.close();
        System.out.println("Loading descriptions done!");
    }
    
    public int size(){
        return this.descriptions.size();
    }
    
    
    @Override
    public Iterator<Description> iterator() {
        return new getDescriptions();
    }
    
    public static void main(String[] args) throws IOException {
        // Used for debugging
        String descriptionsFile = "sct2_Description_Snapshot-en_INT_20150731.txt";
        DescriptionsTable d = new DescriptionsTable(descriptionsFile);
        System.out.println(d.size());
    }


}
