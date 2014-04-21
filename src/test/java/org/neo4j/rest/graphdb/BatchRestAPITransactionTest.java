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

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.neo4j.graphdb.*;
import org.neo4j.graphdb.index.Index;
import org.neo4j.graphdb.index.IndexHits;
import org.neo4j.graphdb.index.IndexManager;
import org.neo4j.helpers.collection.IterableWrapper;
import org.neo4j.helpers.collection.IteratorUtil;
import org.neo4j.helpers.collection.IteratorWrapper;
import org.neo4j.helpers.collection.MapUtil;
import org.neo4j.index.impl.lucene.LuceneIndexImplementation;
import org.neo4j.rest.graphdb.entity.RestNode;
import org.neo4j.rest.graphdb.entity.RestRelationship;
import org.neo4j.rest.graphdb.index.RestIndex;
import org.neo4j.rest.graphdb.transaction.NullTransaction;
import org.neo4j.rest.graphdb.util.Config;
import org.neo4j.rest.graphdb.util.TestHelper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.hamcrest.CoreMatchers.hasItems;
import static org.junit.Assert.*;
import static org.neo4j.graphdb.DynamicLabel.label;
import static org.neo4j.helpers.collection.MapUtil.map;

public class BatchRestAPITransactionTest extends RestTestBase {
    private RestAPI restAPI;

    // TODO transaction check, exception handling if an exception happened in the server

    @Before
    public void init() {
        System.setProperty(Config.CONFIG_BATCH_TRANSACTION,"true");
        this.restAPI = ((RestGraphDatabase) getRestGraphDb()).getRestAPI();
    }

    @Test
    public void testDisableBatchTransactions() throws Exception {
        System.setProperty(Config.CONFIG_BATCH_TRANSACTION,"false");
        Transaction tx = restAPI.beginTx();
        tx.failure();tx.close();
        assertTrue(tx instanceof NullTransaction);
    }
    @Test
    public void testEnableBatchTransactions() throws Exception {
        System.setProperty(Config.CONFIG_BATCH_TRANSACTION,"true");
        Transaction tx = restAPI.beginTx();
        tx.failure();tx.close();
        assertTrue(tx instanceof BatchTransaction);
    }

    @Test
    public void testCreateNode() {
        final Transaction tx = restAPI.beginTx();
        Node n1 = restAPI.createNode(map("name", "node1"));
        Node n2 = restAPI.createNode(map("name", "node2"));
        tx.success();
        tx.close();

        Transaction transaction = getGraphDatabase().beginTx();
        try {
            assertEquals("node1", n1.getProperty("name"));
            assertEquals("node1", loadRealNode(n1).getProperty("name"));
            assertEquals("node2", n2.getProperty("name"));
        } finally {
            transaction.success();transaction.close();
        }
    }


    @Test
    public void testCreateRelationshipToNodeOutsideofBatch() throws Exception {
        final Node node1 = restAPI.createNode(map());
        final Transaction tx = restAPI.beginTx();

        Node node2 = restAPI.createNode(map());
        final Relationship relationship = node1.createRelationshipTo(node2, DynamicRelationshipType.withName("foo"));

        tx.success();
        tx.close();


        Transaction transaction = getGraphDatabase().beginTx();
        try {
            assertEquals("foo", relationship.getType().name());
            assertEquals("foo", getGraphDatabase().getRelationshipById(relationship.getId()).getType().name());
        } finally {
            transaction.success();transaction.close();
        }
    }

    @Test
    public void testCreateNodeAndAddToIndex() throws Exception {
        RestIndex<Node> index = restAPI.createIndex(Node.class, "index", LuceneIndexImplementation.FULLTEXT_CONFIG);
        final Transaction tx = restAPI.beginTx();

        Node n1 = restAPI.createNode(map());
        index.add(n1,"key","value");

        tx.success();
        tx.close();
        Transaction transaction = getGraphDatabase().beginTx();
        try {
            Node node = index.get("key", "value").getSingle();
            assertEquals("created node found in index", n1, node);
        } finally {
            transaction.success();transaction.close();
        }
    }

    @Test(expected = RestResultException.class)
    public void testFailingDoubleDelete() throws Exception {
        final Transaction tx = restAPI.beginTx();

        Node n1 = restAPI.createNode(map());
        n1.delete();
        n1.delete();

        tx.success();
        tx.close();
    }

    @Test(expected = RestResultException.class)
    public void testFailingCreateNodeAndAddToIndex() throws Exception {
        RestIndex<Node> index = restAPI.createIndex(Node.class, "index", MapUtil.stringMap(IndexManager.PROVIDER, "lucene", "type", "fulltext_ _"));
        final Transaction tx = restAPI.beginTx();

        Node n1 = restAPI.createNode(map());
        index.add(n1, "key", "value");

        tx.success();
        tx.close();

        Transaction transaction = getGraphDatabase().beginTx();
        try {
            Node node = index.get("key", "value").getSingle();
            assertEquals("created node found in index", n1, node);
        } finally {
            transaction.success();transaction.close();
        }
    }

    @Test
    public void testSetNodeLabel() throws Exception {
        final Transaction tx = restAPI.beginTx();

        RestNode n1 = restAPI.createNode(map("name", "node1"));
        n1.addLabel(label("FOO"));
        n1.addLabel(label("BAR"));
        tx.success();
        tx.close();
        Transaction transaction = getGraphDatabase().beginTx();
        try {
            Collection<String> labels = IteratorUtil.asCollection(new IterableWrapper<String, Label>(n1.getLabels()) {
                protected String underlyingObjectToObject(Label label) {
                    return label.name();
                }
            });
            assertThat(labels, hasItems("FOO", "BAR"));
        } finally {
            transaction.success();transaction.close();
        }
    }
    @Test
    public void testRemoveNodeLabel() throws Exception {
        RestNode n1 = restAPI.createNode(map("name", "node1"));
        n1.addLabel(label("FOO"));
        n1.addLabel(label("BAR"));

        final Transaction tx = restAPI.beginTx();
        n1.removeLabel(label("BAR"));
        tx.success();
        tx.close();
        Transaction transaction = getGraphDatabase().beginTx();
        try {
            Collection<String> labels = IteratorUtil.asCollection(new IterableWrapper<String, Label>(n1.getLabels()) {
                protected String underlyingObjectToObject(Label label) {
                    return label.name();
                }
            });
            assertThat(labels, hasItems("FOO"));
        } finally {
            transaction.success();transaction.close();
        }
    }

    @Test
    public void testSetNodeProperties() {
        final Transaction tx = restAPI.beginTx();

        Node n1 = restAPI.createNode(map("name", "node1"));
        n1.setProperty("test", "true");
        n1.setProperty("test2", "stilltrue");
        tx.success();
        tx.close();
        Transaction transaction = getGraphDatabase().beginTx();
        try {
            assertEquals("node1", n1.getProperty("name"));
            assertEquals("true", n1.getProperty("test"));
            assertEquals("stilltrue", n1.getProperty("test2"));
            assertEquals("true", loadRealNode(n1).getProperty("test"));
            assertEquals("stilltrue", loadRealNode(n1).getProperty("test2"));
        } finally {
            transaction.success();transaction.close();
        }
    }

    @Test(expected = org.neo4j.graphdb.NotFoundException.class)
    public void testDeleteNode() {
        final Transaction tx = restAPI.beginTx();
        Node n1 = restAPI.createNode(map("name", "node1"));
        n1.delete();
        Node n2 = restAPI.createNode(map("name", "node2"));
        tx.success();
        tx.close();
        Transaction transaction = getGraphDatabase().beginTx();
        try {
            loadRealNode(n1);
        } finally {
            transaction.success();transaction.close();
        }
    }

    @Test
    public void testDeleteRelationship() {
        Transaction tx = restAPI.beginTx();
        Node n1 = restAPI.createNode(map("name", "newnode1"));
        Node n2 = restAPI.createNode(map("name", "newnode2"));
        Relationship rel = restAPI.createRelationship(n1, n2, Type.TEST, map("name", "rel"));
        rel.delete();
        tx.success();
        tx.close();
        Transaction transaction = getGraphDatabase().beginTx();
        try {
            Relationship foundRelationship = TestHelper.firstRelationshipBetween(n1.getRelationships(Type.TEST, Direction.OUTGOING), n1, n2);
            Assert.assertNull("found relationship", foundRelationship);
        } finally {
            transaction.success();transaction.close();
        }
    }

    @Test
    public void testCreateRelationship() {
        Node n1 = restAPI.createNode(map("name", "newnode1"));
        final Transaction tx = restAPI.beginTx();

        Node n2 = restAPI.createNode(map("name", "newnode2"));
        RestRelationship rel = restAPI.createRelationship(n1, n2, Type.TEST, map("name", "rel"));
        Iterable<Relationship> allRelationships = n1.getRelationships();

        tx.success();
        tx.close();

        Transaction transaction = getGraphDatabase().beginTx();
        try {
            Relationship foundRelationship = TestHelper.firstRelationshipBetween(n1.getRelationships(Type.TEST, Direction.OUTGOING), n1, n2);
            Assert.assertNotNull("found relationship", foundRelationship);
            assertEquals("same relationship", rel, foundRelationship);
            assertEquals("rel", rel.getProperty("name"));

            assertThat(n1.getRelationships(Type.TEST, Direction.OUTGOING), new IsRelationshipToNodeMatcher(n1, n2));
            assertThat(n1.getRelationships(Direction.OUTGOING), new IsRelationshipToNodeMatcher(n1, n2));
            assertThat(n1.getRelationships(Direction.BOTH), new IsRelationshipToNodeMatcher(n1, n2));
            assertThat(n1.getRelationships(Type.TEST), new IsRelationshipToNodeMatcher(n1, n2));
            assertThat(allRelationships, new IsRelationshipToNodeMatcher(n1, n2));
        } finally {
            transaction.success();transaction.close();
        }
    }

    @Test
    public void testAddToIndex() {
        final MatrixDataGraph matrixDataGraph = new MatrixDataGraph(getGraphDatabase());
        matrixDataGraph.createNodespace();
        final RestNode neoNode = restAPI.getNodeById(matrixDataGraph.getNeoNodeId());

        final Transaction tx = restAPI.beginTx();
        restAPI.index().forNodes("heroes").add(neoNode, "indexname", "Neo2");
        Node n1 = restAPI.createNode(map("name", "Apoc"));
        final Index<Node> index = restAPI.index().forNodes("heroes");
        index.add(n1, "indexname", "Apoc");

        Transaction transaction = getGraphDatabase().beginTx();
        try {
        final Node indexResult = getGraphDatabase().index().forNodes("heroes").get("indexname", "Neo2").getSingle();
        assertNull(indexResult);
        } finally { transaction.success();transaction.close(); }

        final IndexHits<Node> heroes = index.query("indexname:Apoc");
        tx.success();
        tx.close();
        Transaction transaction2 = getGraphDatabase().beginTx();
        try {
            assertEquals("1 hero", 1, heroes.size());
            IndexManager realIndex = getGraphDatabase().index();
            Index<Node> goodGuys = realIndex.forNodes("heroes");
            IndexHits<Node> hits = goodGuys.get("indexname", "Apoc");
            Node apoc = hits.getSingle();

            assertEquals("Apoc indexed", apoc, heroes.iterator().next());
        } finally {
            transaction2.success();transaction2.close();
        }
    }

    @Test
    public void testQueryIndex() {
        final MatrixDataGraph matrixDataGraph = new MatrixDataGraph(getGraphDatabase());
        matrixDataGraph.createNodespace();
        final Transaction tx = restAPI.beginTx();
        final Index<Node> index = restAPI.index().forNodes("heroes");
        final IndexHits<Node> heroes = index.query("name:Neo");
        tx.success();
        tx.close();
        Transaction transaction = getGraphDatabase().beginTx();
        try {
            assertEquals("1 hero", 1, heroes.size());
            assertEquals("Neo indexed", matrixDataGraph.getNeoNode(), heroes.iterator().next());
        } finally {
            transaction.success();transaction.close();
        }
    }

    @Test
    public void testDeleteIndex() {
        final MatrixDataGraph matrixDataGraph = new MatrixDataGraph(getGraphDatabase());
        matrixDataGraph.createNodespace();
        final Transaction tx = restAPI.beginTx();
        final Index<Node> heroes = restAPI.index().forNodes("heroes");
        heroes.delete();
        tx.success();
        tx.close();
        Transaction transaction = getGraphDatabase().beginTx();
        try {
            Assert.assertFalse(getGraphDatabase().index().existsForNodes("heroes"));
        } finally {
            transaction.success();transaction.close();
        }
    }


    @Test
    public void testRemoveEntryFromIndexWithGivenNode() {
        Node n1 = restAPI.createNode(map("name", "node1"));
        final Index<Node> index = restAPI.index().forNodes("testIndex");
        index.add(n1, "indexname", "Node1");
        final Transaction tx = restAPI.beginTx();
            index.remove(n1);
        tx.success();
        tx.close();
        Transaction transaction = getGraphDatabase().beginTx();
        try {
            assertNull(index.get("indexname", "Node1").getSingle());
        } finally {
            transaction.success();transaction.close();
        }
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testRemoveEntryFromIndexWithGivenNodeAndKey() {
        final Transaction tx = restAPI.beginTx();
        Node n1 = restAPI.createNode(map("name", "node1"));
        final Index<Node> index = restAPI.index().forNodes("testIndex");
        index.add(n1, "indexname", "Node1");
        index.remove(n1, "indexname");
        tx.success();
        tx.close();
        Transaction transaction = getGraphDatabase().beginTx();
        try {
            assertNull(index.get("indexname", "Node1").getSingle());
        } finally {
            transaction.success();transaction.close();
        }
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testRemoveEntryFromIndexWithGivenNodeAndKeyAndValue() {
        final Transaction tx = restAPI.beginTx();
        Node n1 = restAPI.createNode(map("name", "node1"));
        final Index<Node> index = restAPI.index().forNodes("testIndex");
        index.add(n1, "indexname", "Node1");
        index.remove(n1, "indexname", "Node1");
        tx.success();tx.close();
        Transaction transaction = getGraphDatabase().beginTx();
        try {
            assertNull(index.get("indexname", "Node1").getSingle());
        } finally {
            transaction.success();transaction.close();
        }
    }

    @Override
    @After
    public void tearDown() throws Exception {
        restAPI.close();
    }
}
