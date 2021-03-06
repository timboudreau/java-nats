/*
 *  Copyright (c) 2015-2016 Apcera Inc. All rights reserved. This program and the accompanying
 *  materials are made available under the terms of the MIT License (MIT) which accompanies this
 *  distribution, and is available at http://opensource.org/licenses/MIT
 */

package io.nats.client;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@Category(UnitTest.class)
public class AsyncSubscriptionImplTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Rule
    public TestCasePrinterRule pr = new TestCasePrinterRule(System.out);

    @Mock
    public ConnectionImpl connMock;

    @Mock
    public MessageHandler mcbMock;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
    }

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testClose() {
        // Make sure the connection opts aren't null
        when(connMock.getOptions()).thenReturn(Nats.defaultOptions());

        AsyncSubscriptionImpl sub = new AsyncSubscriptionImpl(connMock, "foo", "bar", mcbMock);
        sub.close();
    }

    // @Test
    // public void testAsyncSubscriptionImpl() {
    // fail("Not yet implemented"); // TODO
    // }
    //
    @Test
    public void testStart() {
        // Make sure the connection opts aren't null
        when(connMock.getOptions()).thenReturn(Nats.defaultOptions());

        AsyncSubscriptionImpl sub = new AsyncSubscriptionImpl(connMock, "foo", "bar", mcbMock);
        // Just for the sake of coverage, even though it's a NOOP
        sub.start();
    }


    @Test
    public void testSetMessageHandler() {
        ConnectionImpl nc = mock(ConnectionImpl.class);

        // Make sure the connection opts aren't null
        when(nc.getOptions()).thenReturn(Nats.defaultOptions());

        MessageHandler mh = new MessageHandler() {
            @Override
            public void onMessage(Message msg) {
            }
        };

        try (AsyncSubscriptionImpl s = new AsyncSubscriptionImpl(nc, "foo", "bar", null)) {
            assertTrue(s.isValid());
            s.setMessageHandler(mh);

            assertEquals(mh, s.getMessageHandler());
        }
    }

}
