package com.dimagi;

import com.twitter.finagle.Service;
import com.twitter.finagle.http.Request;
import com.twitter.finagle.http.Response;
import com.twitter.util.Future;
import jodd.format.Printf;
import jodd.mail.Pop3SslServer;
import jodd.mail.ReceiveMailSession;
import jodd.mail.ReceiveMailSessionProvider;
import jodd.mail.ReceivedEmail;

import javax.inject.Inject;
import java.util.concurrent.ExecutorService;

/**
 * Hello world!
 *
 */
public class WhereamiByMail implements Runnable {


    ExecutorService executors;
    private ReceiveMailSessionProvider sessionProvider;

    @Inject
    WhereamiByMail(ExecutorService executors, ReceiveMailSessionProvider sessionProvider) {
        this.executors = executors;
        this.sessionProvider = sessionProvider;
    }

    @Override
    public void run() {
        try {

            ReceiveMailSession session = sessionProvider.createSession();
            session.open();
            System.out.println(session.getMessageCount());

            ReceivedEmail[] emails = session.receiveEmail(false);
            if (emails != null) {
                for (ReceivedEmail email : emails) {
                    System.out.println("\n\n===[" + email.getMessageNumber() + "]===");

                    // common info
                    Printf.out("%0x", email.getFlags());
                    System.out.println("FROM:" + email.getFrom());
                    System.out.println("TO:" + email.getTo()[0]);
                    System.out.println("SUBJECT:" + email.getSubject());
                    System.out.println("PRIORITY:" + email.getPriority());
                    System.out.println("SENT DATE:" + email.getSentDate());
                    System.out.println("RECEIVED DATE: " + email.getReceiveDate());

                }
            }
            session.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
