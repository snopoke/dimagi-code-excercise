package com.dimagi;

import fi.evident.dalesbred.Database;
import fi.evident.dalesbred.ResultTable;
import jodd.mail.ReceiveMailSessionProvider;
import org.apache.http.impl.client.DefaultHttpClient;
import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.unitils.UnitilsJUnit4;
import org.unitils.database.annotations.TestDataSource;
import org.unitils.dbunit.annotation.DataSet;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@DataSet({"/dbunit/WhereamiByMailTest.xml"})
public class WhereamiByMailTest extends UnitilsJUnit4 {

    @TestDataSource
    public DataSource datasource;

    private Database db;
    private WhereamiByMail service;
    private ReceiveMailSessionProvider sessionProvider;


    @Before
    public void setup() {
        sessionProvider = Mockito.mock(ReceiveMailSessionProvider.class);
        db = Database.forDataSource(datasource);
        service = new WhereamiByMail(null, sessionProvider, db, new DefaultHttpClient(), "demo");
    }

    @Test
    public void testGetPerson_existing(){
        //ReceiveMailSession session = Mockito.mock(ReceiveMailSession.class);
        int personId = service.getPerson("test", "test@test.com");
        Assert.assertThat(personId, CoreMatchers.is(1));
    }

    @Test
    public void testGetPerson_not_existing(){
        int personId = service.getPerson("test", "test1@test.com");
        Assert.assertTrue(personId > 0);
    }

    @Test
    public void testSaveMessages() {
        List<LocatedMessage> m = new ArrayList<LocatedMessage>();
        m.add(new LocatedMessage("subject", "from", "from@test.com", new Date(), "location", 1.0, 2.0));
        service.saveMessages(m);

        Integer person = db.findUniqueInt("SELECT id from people where email = ?", "from");
        ResultTable table = db.findTable("SELECT * from locations where person_id = ?", person);
        Assert.assertThat(table.getRowCount(), CoreMatchers.is(1));
        String location = (String) table.get(0, "location");
        Assert.assertThat(location, CoreMatchers.is("location"));
    }

    @Test
    public void testGetLocations() {
        ParsedMessage m = new ParsedMessage(new MailMessage("Cape Town", "Joe", "joe@test.com", null), "Cape Town");
        service.geoLocateMessage(m);
    }
}
