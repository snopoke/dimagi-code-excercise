package com.dimagi;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.node.ArrayNode;
import fi.evident.dalesbred.Database;
import jodd.mail.ReceiveMailSession;
import jodd.mail.ReceiveMailSessionProvider;
import jodd.mail.ReceivedEmail;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WhereamiByMail implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(WhereamiByMail.class);

    private Pattern email_pattern = Pattern.compile("(.*)\\s<(.*)>");
    ExecutorService executors;
    private ReceiveMailSessionProvider sessionProvider;
    private Database db;
    private HttpClient httpClient;
    private String username;

    @Inject
    WhereamiByMail(ExecutorService executors,
                   ReceiveMailSessionProvider sessionProvider,
                   Database db,
                   HttpClient httpClient,
                   @Named("geo.username") String geoUsername) {
        this.executors = executors;
        this.sessionProvider = sessionProvider;
        this.db = db;
        this.httpClient = httpClient;
        this.username = geoUsername;
    }

    @Override
    public void run() {
        try {
            List<MailMessage> messages = getMailMessages(getSession());
            if (!messages.isEmpty()) {
                List<ParsedMessage> parsedMessages = parseMessages(messages);
                List<LocatedMessage> locatedMessages = geoLocationMessages(parsedMessages);
                saveMessages(locatedMessages);
                logger.info("Run complete");
            }
        } catch (Exception e) {
            logger.error("Error during run", e);
        }
    }

    /**
     * Parse the message subject to get the location
     */
    protected List<ParsedMessage> parseMessages(List<MailMessage> messages) {
        logger.debug("Parsing messages");
        List<ParsedMessage> parsedMessages = new ArrayList<ParsedMessage>(messages.size());
        for (MailMessage m : messages) {
            // assume subject is just the location
            parsedMessages.add(new ParsedMessage(m, m.getSubject()));
        }
        return parsedMessages;
    }

    /**
     * Given a list of messages get the geo locations and return a list of Geo Located messages
     */
    protected List<LocatedMessage> geoLocationMessages(final List<ParsedMessage> messages) throws IOException {
        List<LocatedMessage> locatedMessages = new ArrayList<LocatedMessage>(messages.size());
        for (ParsedMessage m : messages) {
            List<GeoName> geoNameList = getGeoNames(m);
            GeoName gn = getBestGeoName(m, geoNameList);
            locatedMessages.add(new LocatedMessage(m, gn.getLat(), gn.getLng()));
        }
        return locatedMessages;
    }

    /**
     * Save the messages to the database
     */
    protected void saveMessages(List<LocatedMessage> messages) {
        logger.debug("Saving messages to database");
        for (final LocatedMessage m : messages) {
            final int person = getPerson(m.getName(), m.getEmail());
            db.update("INSERT INTO locations (person_id,date,location,lat,lng) VALUES (?,?,?,?,?)",
                    person, m.getSentDate(), m.getLocation(), m.getLat(), m.getLng());
        }
    }

    /**
     * Given a list of GeoNames get the best one for this message.
     *
     * TODO: future: find the GeoName closest to the person's previous location
     */
    private GeoName getBestGeoName(ParsedMessage m, List<GeoName> geoNameList) {
        if (geoNameList.isEmpty()) {
            return null;
        }
        // for now just get the first one
        return geoNameList.get(0);
    }

    /**
     * Query the GeoName service and return a list of GeoNames for the
     * message.
     */
    protected List<GeoName> getGeoNames(ParsedMessage m) {
        logger.debug("Extracting geonames from JSON data");
        List<GeoName> geoNameList = new ArrayList<GeoName>();
        try {
            String responseBody = getGeoResponse(m.getLocation());


            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(responseBody);
            JsonNode geonames = root.get("geonames");
            if (geonames.isArray()) {
                Iterator<JsonNode> elements = ((ArrayNode) geonames).elements();
                ObjectReader geonameReader = mapper.reader(GeoName.class);
                while (elements.hasNext()) {
                    GeoName gn = geonameReader.readValue(elements.next());
                    logger.debug("Read geoname: {}", gn.getName());
                    geoNameList.add(gn);
                }
            }
        } catch (IOException e) {
            logger.error("Error performing geo location", e);
        }
        return geoNameList;
    }

    /**
     * Perform the request to GeoNames service and return the JSON response
     */
    private String getGeoResponse(String search) throws IOException {
        logger.debug("Fetching geoname response for search: {}", search);

        String location = URLEncoder.encode(search, "UTF-8");
        HttpGet httpget = new HttpGet("http://api.geonames.org/searchJSON?username="+username+"&q=" + location);

        System.out.println("executing request " + httpget.getURI());

        // Create a response handler
        ResponseHandler<String> responseHandler = new BasicResponseHandler();
        String responseBody = httpClient.execute(httpget, responseHandler);
        logger.trace(responseBody);
        return responseBody;
    }

    /**
     * Get a person related to the given email address from the database
     */
    protected int getPerson(String name, String email) {
        Integer person = selectPerson(email);
        if (person == null) {
            createPerson(name, email);
            person = selectPerson(email);
        }
        return person;
    }

    private Integer selectPerson(String email) {
         return db.findUniqueOrNull(Integer.class, "SELECT id from people where email = ?", email);
    }

    private void createPerson(String name, String email) {
        db.update("INSERT INTO people (name, email) VALUES (?,?)", name, email);
    }

    /**
     * Get the list of mail messages in the inbox
     */
    protected List<MailMessage> getMailMessages(ReceiveMailSession session) {
        logger.info("Polling mail for new messages");
        try {
            session.open();

            int messageCount = session.getMessageCount();
            logger.info("{} new messages found", messageCount);
            List<MailMessage> messages = new ArrayList<MailMessage>(messageCount);

            ReceivedEmail[] emails = session.receiveEmail(false);
            if (emails != null) {
                for (ReceivedEmail email : emails) {
                    String from = email.getFrom();
                    String name = from;
                    Matcher matcher = email_pattern.matcher(from);
                    if (matcher.find()) {
                        name = matcher.group(1);
                        from = matcher.group(2);
                    }
                    messages.add(new MailMessage(email.getSubject(), name, from, email.getSentDate()));
                }
            }
            return messages;
        } finally {
            if (session != null) {
                session.close();
            }
        }
    }

    public ReceiveMailSession getSession() {
        return sessionProvider.createSession();
    }

}
