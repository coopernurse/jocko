package com.bitmechanic.jocko.local;

import com.bitmechanic.jocko.BasePersistenceServiceTest;
import com.bitmechanic.jocko.PersistenceService;

import java.io.File;

/**
 * Created by James Cooper <james@bitmechanic.com>
 * Date: May 9, 2010
 */
public class HsqlPersistenceServiceTest extends BasePersistenceServiceTest {

    private File dbFile;
    private HsqlPersistenceService service;

    @Override
    public void setUp() throws Exception {

        dbFile  = File.createTempFile("hsql-unittest", ".db");
        service = new HsqlPersistenceService(dbFile.getAbsolutePath());

        super.setUp();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();

        dbFile.delete();
    }

    @Override
    protected PersistenceService getPersistenceService() {
        return service;
    }
}
