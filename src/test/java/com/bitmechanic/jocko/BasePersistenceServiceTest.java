package com.bitmechanic.jocko;

import com.bitmechanic.jocko.local.LocalBlobService;
import junit.framework.TestCase;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

/**
 * Created by James Cooper <james@bitmechanic.com>
 * Date: Jan 28, 2010
 */
public abstract class BasePersistenceServiceTest extends TestCase {

    PersistenceService service;

    protected abstract PersistenceService getPersistenceService();

    @Override
    public void setUp() throws Exception {
        super.setUp();

        service = getPersistenceService();
        InfrastructureContainer infra = new InfrastructureContainer();
        infra.setPersistenceService(service);
        infra.setBlobService(new LocalBlobService());
        service.setInfrastructure(infra);
    }

    public void testPutGetDelete() throws Exception {
        Person person = new Person("John", "Doe", System.currentTimeMillis());

        String uuid = service.put(person);
        assertNotNull(uuid);
        assertEquals(person.getId(), uuid);
        assertNotNull(uuid);

        Person person2 = service.get(uuid);
        assertEquals(person, person2);

        service.delete(uuid);

        Person person3 = service.get(uuid);
        assertNull(person3);

        service.delete(uuid); // should no-op
    }

    public void testSearchEquals() throws Exception {
        Person person1 = new Person("John", "Doe", System.currentTimeMillis());
        Person person2 = new Person("Jack", "Doe", System.currentTimeMillis());

        service.put(person1);
        service.put(person2);

        Query<Person> query = Query.Builder(Person.class).equals("firstName", "John");
        List<Person> result = service.query(query);
        assertEquals(1, result.size());
        assertEquals(person1, result.get(0));
    }

    public void testSearchNotEquals() throws Exception {
        Person person1 = new Person("John", "Doe", System.currentTimeMillis());
        Person person2 = new Person("Jack", "Doe", System.currentTimeMillis());

        service.put(person1);
        service.put(person2);

        Query<Person> query = Query.Builder(Person.class).notEquals("firstName", "John");
        List<Person> result = service.query(query);
        assertEquals(1, result.size());
        assertEquals(person2, result.get(0));
    }

    public void testSorting() throws Exception {
        Person person1 = new Person("John", "Doe", System.currentTimeMillis());
        Person person2 = new Person("Jack", "Doe", System.currentTimeMillis());

        service.put(person1);
        service.put(person2);

        Query<Person> query = Query.Builder(Person.class).sortAscending("firstName");
        List<Person> result = service.query(query);
        assertEquals(2, result.size());
        assertEquals(person2, result.get(0));

        query = Query.Builder(Person.class).sortDescending("firstName");
        result = service.query(query);
        assertEquals(2, result.size());
        assertEquals(person1, result.get(0));
    }

    public void testMultipleSearchTerms() throws Exception {
        Person person1 = new Person("John", "Doe", 100);
        Person person2 = new Person("Jack", "Doe", 200);

        service.put(person1);
        service.put(person2);

        Query<Person> query = Query.Builder(Person.class)
                .equals("firstName", "John")
                .lessThan("dateOfBirth", 150L);
        List<Person> result = service.query(query);
        assertEquals(1, result.size());
        assertEquals(person1, result.get(0));

        query = Query.Builder(Person.class)
                .equals("lastName", "Doe")
                .greaterThanOrEquals("dateOfBirth", 200L);
        result = service.query(query);
        assertEquals(1, result.size());
        assertEquals(person2, result.get(0));
    }

    public void testSetBasedOperations() throws Exception {
        Person person1 = new Person("John", "Doe", 100);
        Person person2 = new Person("Jack", "Doe", 200);

        Set<String> ids = service.put(Arrays.asList(person1, person2));
        assertEquals(2, ids.size());

        List<Person> list = service.get(ids);
        assertEquals(2, list.size());
        assertEquals(person1, list.get(0));
        assertEquals(person2, list.get(1));

        service.delete(ids);

        list = service.get(ids);
        assertEquals(0, list.size());
    }

    public void testQueryForIds() throws Exception {
        Person person1 = new Person("John", "Doe", 100);
        Person person2 = new Person("Jack", "Doe", 200);

        service.put(person1);
        service.put(person2);

        Query<Person> query = Query.Builder(Person.class).sortDescending("firstName");
        List<String> list = service.queryForIds(query);
        assertEquals(2, list.size());
        assertEquals(person1.getId(), list.get(0));
        assertEquals(person2.getId(), list.get(1));
    }

    public void testQueryCount() throws Exception {
        Person person1 = new Person("John", "Doe", 100);
        Person person2 = new Person("Jack", "Doe", 200);

        service.put(person1);
        service.put(person2);

        Query<Person> query = Query.Builder(Person.class);
        assertEquals(2, service.queryCount(query));

        query = Query.Builder(Person.class).equals("firstName", "Jack");
        assertEquals(1, service.queryCount(query));

        service.delete(person1.getId());
        query = Query.Builder(Person.class);
        assertEquals(1, service.queryCount(query));
    }

    // test generation of next/back tokens

    public void testPaginationTokens() throws Exception {
        Person person1 = new Person("John", "Doe", 100);
        Person person2 = new Person("Jack", "Doe", 200);

        service.put(person1);
        service.put(person2);

        Query<Person> query = Query.Builder(Person.class).maxResults(1).sortAscending("dateOfBirth");
        PaginatedResult<Person> result = service.queryPaginated(query);
        assertEquals(1, result.getCurrentPage().size());
        assertEquals(person1, result.getCurrentPage().get(0));
        assertTrue(result.getPaginationState().hasNext());
        assertFalse(result.getPaginationState().hasPrevious());

        query = Query.Builder(Person.class).maxResults(1).sortAscending("dateOfBirth").paginationState(result.getPaginationState().getNextPage());
        result = service.queryPaginated(query);
        assertEquals(1, result.getCurrentPage().size());
        assertEquals(person2, result.getCurrentPage().get(0));
        assertTrue(result.getPaginationState().hasPrevious());
        assertFalse(result.getPaginationState().hasNext());

        query = Query.Builder(Person.class).maxResults(1).sortAscending("dateOfBirth").paginationState(result.getPaginationState().getPreviousPage());
        result = service.queryPaginated(query);
        assertEquals(1, result.getCurrentPage().size());
        assertEquals(person1, result.getCurrentPage().get(0));
        assertTrue(result.getPaginationState().hasNext());
        assertFalse(result.getPaginationState().hasPrevious());
    }

    public void testIdPagination() throws Exception {
        Person person1 = new Person("John", "Doe", 100);
        Person person2 = new Person("Jack", "Doe", 200);

        service.put(person1);
        service.put(person2);

        Query<Person> query = Query.Builder(Person.class).maxResults(1).sortAscending("dateOfBirth");
        PaginatedResult<String> result = service.queryForIdsPaginated(query);
        assertEquals(1, result.getCurrentPage().size());
        assertEquals(person1.getId(), result.getCurrentPage().get(0));
        assertTrue(result.getPaginationState().hasNext());
        assertFalse(result.getPaginationState().hasPrevious());

        query = Query.Builder(Person.class).maxResults(1).sortAscending("dateOfBirth").paginationState(result.getPaginationState().getNextPage());
        result = service.queryForIdsPaginated(query);
        assertEquals(1, result.getCurrentPage().size());
        assertEquals(person2.getId(), result.getCurrentPage().get(0));
        assertTrue(result.getPaginationState().hasPrevious());
        assertFalse(result.getPaginationState().hasNext());

        query = Query.Builder(Person.class).maxResults(1).sortAscending("dateOfBirth").paginationState(result.getPaginationState().getPreviousPage());
        result = service.queryForIdsPaginated(query);
        assertEquals(1, result.getCurrentPage().size());
        assertEquals(person1.getId(), result.getCurrentPage().get(0));
        assertTrue(result.getPaginationState().hasNext());
        assertFalse(result.getPaginationState().hasPrevious());
    }

    public void testMetaDataIsClassSpecific() throws Exception {
        Person person1 = new Person("John", "Doe", 100);
        Thread.sleep(1);
        Person person2 = new Person("Jack", "Doe", 200);
        Thread.sleep(1);

        Invoice invoice1 = new Invoice(10, 20);
        Thread.sleep(1);
        Invoice invoice2 = new Invoice(30, 40);

        service.put(person1);
        service.put(person2);

        service.put(invoice1);
        service.put(invoice2);

        Query<Invoice> query = Query.Builder(Invoice.class).maxResults(5).sortAscending("dateUpdated");
        List<Invoice> list = service.query(query);
        assertEquals(2, list.size());
        assertEquals(invoice1, list.get(0));
        assertEquals(invoice2, list.get(1));
    }

    public void testSetPersistence() throws Exception {
        Person person1 = new Person("John", "Doe", 100);
        person1.setHobbies(Arrays.asList("golf", "table tennis"));
        person1.setFavoriteNumbers(Arrays.asList(10, 300, -3));

        Person person2 = new Person("John", "Doe", 200);
        person2.setHobbies(Arrays.asList("disco dance", "table tennis"));

        service.put(person1);
        service.put(person2);

        Query<Person> query = Query.Builder(Person.class).maxResults(5).sortAscending("dateOfBirth").equals("hobbies", "golf");
        List<Person> list = service.query(query);
        assertEquals(1, list.size());
        assertEquals(person1, list.get(0));

        query = Query.Builder(Person.class).maxResults(5).sortAscending("dateOfBirth").equals("hobbies", "table tennis");
        list = service.query(query);
        assertEquals(2, list.size());
        assertEquals(person1, list.get(0));
        assertEquals(person2, list.get(1));

        query = Query.Builder(Person.class).equals("favoriteNumbers", -3);
        list = service.query(query);
        assertEquals(1, list.size());
        assertEquals(person1, list.get(0));

        person1.setFavoriteNumbers(Arrays.asList(39));
        service.put(person1);

        query = Query.Builder(Person.class).equals("favoriteNumbers", -3);
        list = service.query(query);
        assertEquals(0, list.size());

        query = Query.Builder(Person.class).equals("favoriteNumbers", 39);
        list = service.query(query);
        assertEquals(1, list.size());
    }

}
