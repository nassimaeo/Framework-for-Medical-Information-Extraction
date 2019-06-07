package snomed;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.StringTokenizer;

/**
 * 
 * This class represent a concept as defined by SNOMED-CT
 * 
 * @author Nassim
 * @version 2016 04
 */


class Concept {
    
    private long conceptId;
    private String effectiveTime; // yyyymmdd
    private boolean active; // 1 : true, 0 : false
    private long moduleId;
    private long definitionStatusId;
    
    public Concept(String conceptId, String effectiveTime, String active, String moduleId, String definitionStatusId) {
        this.conceptId = Long.parseLong(conceptId);
        this.effectiveTime = effectiveTime;
        if (active =="1") this.active = true;
        else this.active = false;
        this.moduleId = Long.parseLong(moduleId);
        this.definitionStatusId = Long.parseLong(definitionStatusId);
    }
    
    public boolean isActive(){
        return this.active;
    }
    
    public long getConceptId() {
        return conceptId;
    }
    
    public String getEffectiveTime() {
        return effectiveTime;
    }
    
    public long getModuleId() {
        return moduleId;
    }
    
    public long getDefinitionStatusId() {
        return definitionStatusId;
    }
}

public class ConceptsTable implements Iterable<Concept> {
    // Stores the concepts: id --> concept
    HashMap<Integer, Concept> concepts;   
    
    public class getConcepts implements Iterator<Concept>{
        
        private int i = 0;
        
        @Override
        public boolean hasNext() {
            return i < concepts.size();
        }

        @Override
        public Concept next() {
            return concepts.get(i++);
        }
        
    }

    
    public ConceptsTable(String conceptFile) throws IOException {
        /* ====================
         * Loading the concepts
         * ====================
         * id (conceptId)
         * effectiveTime
         * active
         * moduleId [900000000000012004, 900000000000207008]
         * definitionStatusId ["Primitive Type":900000000000074008 or "Fully Specified":900000000000073002]
         */

        BufferedReader bufferedReader = null;
        StringTokenizer st = null;
        
        // Stores the concepts: id --> concept
        this.concepts = new HashMap<Integer, Concept>();
        
        // File reader
        bufferedReader = new BufferedReader(new FileReader(conceptFile));
        
        // Get the fields
        st = new StringTokenizer(bufferedReader.readLine());
        System.out.println("Loading concepts : ");
        
        // Print the fields
        while (st.hasMoreTokens()) System.out.println("\t" + st.nextToken());
        
        int counter = 0;
        // Load the concepts.
        do {
            String line = bufferedReader.readLine();
            if (line == null) break;
            String[] s = line.split("\t");
            if (s[2].equals("0")) continue; // Load only where active = 1
            Concept concept = new Concept(s[0],s[1],s[2],s[3],s[4]);
            concepts.put(counter++, concept);
        } while(true);
        bufferedReader.close();
        System.out.println("Loading concepts done! ");
    }
    
    public int size(){
        return this.concepts.size();
    }

    @Override
    public Iterator<Concept> iterator() {
        return new getConcepts();
    }
    
    
    public static void main(String[] args) throws IOException {
        // Used for tests and debuggings
        String conceptFile ="res/snomed/sct2_Concept_Snapshot_INT_20150731.txt";
        ConceptsTable c = new ConceptsTable(conceptFile);
        System.out.println(c.size());
    }
}