package snomed;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.StringTokenizer;

/**
 * 
 * This class represent a relationship as defined by SNOMED-CT
 *
 * =========================
 * Loading the relationships
 * =========================
 * id (relationshipId)
 * effectiveTime
 * active
 * moduleId
 * sourceId
 * destinationId
 * relationshipGroup
 * typeId
 * characteristicTypeId
 * moduleId
 * =========================
 * 
 * @author Nassim
 * @version 2016 04
 */

class Relationship {
    private long id;
    private String effectiveTime;
    private boolean active;
    private long moduleId;
    private long sourceId; // refers to the Concept to which a defining characteristic (attribute) applies
    private long destinationId; // refers to the Concept that represents the value of that attribute.
    private long relationshipGroup; 
    private long typeId;// indicates the nature of the defining attribute.
    private long characteristicTypeId;
    private long modifierId;
    
    public Relationship(String relationshipId, String effectiveTime, String active, String moduleId, String sourceId, String destinationId, String relationshipGroup,String typeId, String characteristicTypeId, String modifierId) {
        this.id = Long.parseLong(relationshipId);
        this.effectiveTime = effectiveTime;
        if (active =="1") this.active = true;
        else this.active = false;
        this.moduleId = Long.parseLong(moduleId);
        this.sourceId = Long.parseLong(sourceId);
        this.destinationId = Long.parseLong(destinationId);
        this.relationshipGroup = Long.parseLong(relationshipGroup);
        this.typeId = Long.parseLong(typeId);
        this.characteristicTypeId = Long.parseLong(characteristicTypeId);
        this.modifierId = Long.parseLong(modifierId);
    }

    public long getId() {
        return id;
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

    public long getSourceId() {
        return sourceId;
    }

    public long getDestinationId() {
        return destinationId;
    }

    public long getRelationshipGroup() {
        return relationshipGroup;
    }

    public long getTypeId() {
        return typeId;
    }

    public long getCharacteristicTypeId() {
        return characteristicTypeId;
    }

    public long getModifierId() {
        return modifierId;
    }
    
}

public class RelationshipsTable implements Iterable<Relationship> {
    
    private HashMap<Integer, Relationship> relationships;
    
    private class getRelationships implements Iterator<Relationship>{
        int i = 0;

        @Override
        public boolean hasNext() {
            return i < relationships.size();
        }

        @Override
        public Relationship next() {
            return relationships.get(i++);
        }
    }
    
    public RelationshipsTable(String relationshipsFile) throws IOException {
        /* =========================
         * Loading the relationships
         * =========================
         * id (relationshipId)
         * effectiveTime
         * active
         * moduleId
         * sourceId
         * destinationId
         * relationshipGroup
         * typeId
         * characteristicTypeId
         * moduleId
         */
        BufferedReader bufferedReader = null;
        StringTokenizer st = null;
        
        // read the file
        bufferedReader = new BufferedReader(new FileReader(relationshipsFile));
        
        // get the fields
        st = new StringTokenizer(bufferedReader.readLine());
        
        // print the fields
        System.out.println("Loading relationships : ");
        while (st.hasMoreTokens()) System.out.println("\t" + st.nextToken());
        
        // Load the relationships id --> relationship
        relationships = new HashMap<Integer, Relationship>();
        int counter = 0;
        
        do{
            String line = bufferedReader.readLine();
            if (line == null) break;
            String[] s = line.split("\t");            
            if (s[2].equals("0")) continue;
            Relationship relationship = new Relationship(s[0],s[1],s[2],s[3],s[4],s[5],s[6],s[7],s[8],s[9]);
            relationships.put(counter++, relationship);
        } while(true);
        bufferedReader.close();
        System.out.println("Loading relationships done! ");
    }
    
    public int size(){
        return this.relationships.size();
    }
    
    @Override
    public Iterator<Relationship> iterator() {
        return new getRelationships();
    }
    
    public static void main(String[] args) throws IOException {
        String relationshipsFile = "sct2_Relationship_Snapshot_INT_20150731.txt";
        RelationshipsTable r = new RelationshipsTable(relationshipsFile);
        System.out.println(r.size());
    }
}
