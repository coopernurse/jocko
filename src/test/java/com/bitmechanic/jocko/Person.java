package com.bitmechanic.jocko;

import com.bitmechanic.jocko.Infrastructure;
import com.bitmechanic.jocko.PersistableEntity;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Created by James Cooper <james@bitmechanic.com>
 * Date: Jan 28, 2010
 */
public class Person implements PersistableEntity {

    private String id;
    private long dateCreated;
    private long dateUpdated;
    private String firstName;
    private String lastName;
    private long dateOfBirth;
    private List<String> hobbies;
    private List<Integer> favoriteNumbers;

    public Person() {
    }

    public Person(String firstName, String lastName, long dateOfBirth) {
        this.firstName = firstName;
        this.lastName  = lastName;
        this.dateOfBirth = dateOfBirth;
        this.dateCreated = System.currentTimeMillis();
        this.dateUpdated = dateCreated;
    }

    @Override
    public Collection<EtlJob> getEtlJobs() {
        return null;
    }

    @Override
    public void setInfrastructure(Infrastructure infra) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String uuid) {
        this.id = uuid;
    }

    public long getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(long dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastname) {
        this.lastName = lastname;
    }

    public long getDateCreated() {
        return dateCreated;
    }

    public void setDateCreated(long dateCreated) {
        this.dateCreated = dateCreated;
    }

    public long getDateUpdated() {
        return dateUpdated;
    }

    public void setDateUpdated(long dateUpdated) {
        this.dateUpdated = dateUpdated;
    }

    public List<Integer> getFavoriteNumbers() {
        return favoriteNumbers;
    }

    public void setFavoriteNumbers(List<Integer> favoriteNumbers) {
        this.favoriteNumbers = favoriteNumbers;
    }

    public List<String> getHobbies() {
        return hobbies;
    }

    public void setHobbies(List<String> hobbies) {
        this.hobbies = hobbies;
    }

    @Override
    public void fillMetaData(Map<String, Object> map) {
        map.put("firstName", firstName);
        map.put("lastName", lastName);
        map.put("dateOfBirth", dateOfBirth);
        map.put("dateCreated", dateCreated);
        map.put("dateUpdated", dateUpdated);
        map.put("hobbies", hobbies);
        map.put("favoriteNumbers", favoriteNumbers);
    }

    @Override
    public void onAfterGet() {

    }

    @Override
    public void onBeforePut() {
        
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Person)) return false;

        Person person = (Person) o;

        if (dateCreated != person.dateCreated) return false;
        if (dateOfBirth != person.dateOfBirth) return false;
        if (dateUpdated != person.dateUpdated) return false;
        if (favoriteNumbers != null ? !favoriteNumbers.equals(person.favoriteNumbers) : person.favoriteNumbers != null)
            return false;
        if (firstName != null ? !firstName.equals(person.firstName) : person.firstName != null) return false;
        if (hobbies != null ? !hobbies.equals(person.hobbies) : person.hobbies != null) return false;
        if (id != null ? !id.equals(person.id) : person.id != null) return false;
        if (lastName != null ? !lastName.equals(person.lastName) : person.lastName != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (int) (dateCreated ^ (dateCreated >>> 32));
        result = 31 * result + (int) (dateUpdated ^ (dateUpdated >>> 32));
        result = 31 * result + (firstName != null ? firstName.hashCode() : 0);
        result = 31 * result + (lastName != null ? lastName.hashCode() : 0);
        result = 31 * result + (int) (dateOfBirth ^ (dateOfBirth >>> 32));
        result = 31 * result + (hobbies != null ? hobbies.hashCode() : 0);
        result = 31 * result + (favoriteNumbers != null ? favoriteNumbers.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "Person{" +
                "dateOfBirth=" + dateOfBirth +
                ", id='" + id + '\'' +
                ", firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                '}';
    }
}
