package kuzminov;

import java.util.*;

public class Person {

    String id = null;
    String firstName = null;
    String surname = null;
    String gender = null;

    String wifeId = null;
    String husbandId = null;
    String spouseName = null; //имя

    Set<String> parentsId = new HashSet<>();
    String fatherName = null;
    String motherName = null;

    Set<String> childrenName = new HashSet<>();
    Set<String> sonId = new HashSet<>();
    Set<String> daughterId = new HashSet<>();


    Set<String> siblingsId = new HashSet<>();
    Set<String> sistersName = new HashSet<>();
    Set<String> brothersName = new HashSet<>();

    Integer childrenNumber;
    Integer siblingsNumber;

    public void union(Person another) {
        if (this.id == null) this.id = another.id;
        if (this.firstName == null) this.firstName = another.firstName;
        if (this.surname == null) this.surname = another.surname;
        if (this.gender == null) this.gender = another.gender;
        if (this.spouseName == null) this.spouseName = another.spouseName;
        if (this.wifeId == null) this.wifeId = another.wifeId;
        if (this.husbandId == null) this.husbandId = another.husbandId;
        if (this.childrenNumber == null) this.childrenNumber = another.childrenNumber;
        if (this.siblingsNumber == null) this.siblingsNumber = another.siblingsNumber;

        if (another.fatherName != null) this.fatherName = another.fatherName;
        if (another.motherName != null) this.motherName = another.motherName;

        this.parentsId.addAll(another.parentsId);
        this.sonId.addAll(another.sonId);
        this.daughterId.addAll(another.daughterId);
        this.siblingsId.addAll(another.siblingsId);
        this.sistersName.addAll(another.sistersName);
        this.brothersName.addAll(another.brothersName);

    }

    public void validate() {
        if (childrenNumber != null) {
            int totalChildren = sonId.size() + daughterId.size();
            int totalAnyChildren = childrenName.size();
            if (childrenNumber != totalChildren && childrenNumber != totalAnyChildren) {
                System.out.println("Warning: childrenNumber mismatch for " + id + " (" + firstName + " " + surname + ") " +
                        childrenNumber + " != son+daughter " + totalChildren + " и != childrenId " + totalAnyChildren);
            }
        }

        if (siblingsNumber != null) {
            int totalSibs = sistersName.size() + brothersName.size();
            int totalAnySibs = siblingsId.size();
            if (siblingsNumber != totalSibs && siblingsNumber != totalAnySibs) {
                System.out.println("Warning: siblingsNumber mismatch for " + id + " (" + firstName + " " + surname + ") " +
                        siblingsNumber + " != sisters+brothers " + totalSibs + " и != siblingsId " + totalAnySibs);
            }
        }
    }

}
