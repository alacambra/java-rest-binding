/**
 * Copyright (c) 2002-2013 "Neo Technology,"
 * Network Engine for Objects in Lund AB [http://neotechnology.com]
 *
 * This file is part of Neo4j.
 *
 * Neo4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.neo4j.rest.graphdb;


import org.junit.*;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.helpers.collection.IteratorUtil;
import org.neo4j.server.configuration.Configurator;
import org.neo4j.server.security.KeyStoreFactory;
import org.neo4j.server.security.KeyStoreInformation;
import org.neo4j.tooling.GlobalGraphOperations;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.security.KeyStore;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.Properties;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

import static org.junit.Assert.assertEquals;

@RunWith(Parameterized.class)
public class RestTestBase {

    public static final String HTTP_URI = "http://localhost:7473";
    public static final String HTTPS_URI = "https://localhost:7472";

    private GraphDatabaseService restGraphDb;

    private static LocalTestServer neoServer;
    protected static final String SERVER_ROOT_URI = HTTP_URI + "/db/data/"; // only used for tryConnect
    private static final String CONFIG = "neo4j-server.properties";
    private static Properties properties = new Properties(  );
    protected String url;

    @Parameterized.Parameters(name = "{index}: URL {0}")
    public static Collection<String[]> data() {
        return Arrays.asList(new String[][] { {HTTP_URI}, {HTTPS_URI}});
    }

    public RestTestBase(String url) {
        this.url = url;
    }

    protected static void initServer() {
        if (neoServer!=null) {
            neoServer.stop();
        }
        neoServer = new LocalTestServer().withPropertiesFile(CONFIG);
    }

    @BeforeClass
    public static void startDb() throws Exception {
        readProperties();
        setupJvmKeystore();
        initServer();
        neoServer.start();
        tryConnect(HTTP_URI + "/db/data/");
        tryConnect(HTTPS_URI + "/db/data/");
    }

    /**
     * configure keystore to be used for https clients
     */
    private static void setupJvmKeystore()
    {
        try
        {
            KeyStoreInformation keyStoreInformation = new KeyStoreFactory().createKeyStore(
                    File.createTempFile( "keystore" , "key"),
                    new File( (String) properties.get( Configurator.WEBSERVER_HTTPS_KEY_PATH_PROPERTY_KEY ) ),
                    new File( (String) properties.get( Configurator.WEBSERVER_HTTPS_CERT_PATH_PROPERTY_KEY ) ) );
            KeyStore ks = KeyStore.getInstance( "JKS" );
            ks.load(new FileInputStream(keyStoreInformation.getKeyStorePath()), keyStoreInformation.getKeyStorePassword());
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmf.init(ks);
            KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            kmf.init(ks, keyStoreInformation.getKeyPassword());
            SSLContext ctx = SSLContext.getInstance("TLS");
            ctx.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
            SSLContext.setDefault(ctx);
        }
        catch ( Exception e )
        {
            throw new RuntimeException( e );
        }
    }

    private static void readProperties()
    {
        try {
            properties.load( RestTestBase.class.getResourceAsStream( "/" + CONFIG ) );
        } catch (IOException e ) {
            throw new RuntimeException( e );
        }
    }

    private static void tryConnect(String serverRootUri) throws InterruptedException {
        int retryCount = 3;
        for (int i = 0; i < retryCount; i++) {
            try {
                RequestResult result = new ExecutingRestRequest(serverRootUri).get("");
                assertEquals(200, result.getStatus());
                System.err.println("Successful HTTP connection to "+ serverRootUri);
                return;
            } catch (Exception e) {
                System.err.println("Error retrieving ROOT URI " + e.getMessage());
                Thread.sleep(500);
            }
        }
    }

    @Before
    public void setUp() throws URISyntaxException {
        neoServer.cleanDb();
        restGraphDb = new RestGraphDatabase( getServerRootUri() );
    }

    private String getServerRootUri()
    {
        return url + "/db/data";
    }

    @After
    public void tearDown() throws Exception {
        restGraphDb.shutdown();
    }

    @AfterClass
    public static void shutdownDb() {
        neoServer.stop();

    }

    protected Relationship relationship() {
        Iterator<Relationship> it = node().getRelationships(Direction.OUTGOING).iterator();
        if (it.hasNext()) return it.next();
        return node().createRelationshipTo(restGraphDb.createNode(), Type.TEST);
    }

    protected Node node() {
//        return restGraphDb.getReferenceNode();
        return GlobalGraphOperations.at(getGraphDatabase()).getAllNodes().iterator().next();
    }

    protected GraphDatabaseService getGraphDatabase() {
    	return neoServer.getGraphDatabase();
    }

	protected GraphDatabaseService getRestGraphDb() {
		return restGraphDb;
	}

    protected int countExistingNodes() {
        return IteratorUtil.count(GlobalGraphOperations.at(getGraphDatabase()).getAllNodes());
    }

    protected Node loadRealNode(Node node) {
        return getGraphDatabase().getNodeById(node.getId());
    }
    public String getUserAgent() {
        return neoServer.getUserAgent();
    }

    @Test
    public void testNothing() throws Exception { }
}
