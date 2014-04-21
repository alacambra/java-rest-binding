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

import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;
import static org.neo4j.helpers.collection.MapUtil.map;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.neo4j.graphdb.*;
import org.neo4j.graphdb.index.Index;
import org.neo4j.graphdb.index.IndexHits;
import org.neo4j.graphdb.index.IndexManager;
import org.neo4j.helpers.collection.IteratorUtil;
import org.neo4j.rest.graphdb.batch.BatchCallback;
import org.neo4j.rest.graphdb.entity.RestNode;
import org.neo4j.rest.graphdb.entity.RestRelationship;
import org.neo4j.rest.graphdb.util.TestHelper;

public class BatchRestAPITest extends RestTestBase {
    public static final DynamicRelationshipType RELATIONSHIP_TYPE = DynamicRelationshipType.withName("foo");
    private RestAPI restAPI;
  
    // TODO transaction check, exception handling if an exception happened in the server

    @Before
    public void init(){
        this.restAPI = ((RestGraphDatabase)getRestGraphDb()).getRestAPI();
    }
    
    @Test
    public void testCreateNode(){
        TestBatchResult response =this.restAPI.executeBatch(new BatchCallback<TestBatchResult>() {
            
            @Override
            public TestBatchResult recordBatch(RestAPI batchRestApi) {
                TestBatchResult result=new TestBatchResult();
                result.n1 = batchRestApi.createNode(map("name", "node1"));              
                result.n2 = batchRestApi.createNode(map("name", "node2"));
                return result;
            }
        });
        Transaction transaction = getGraphDatabase().beginTx();
        try {
            assertEquals("node1", response.n1.getProperty("name"));
            assertEquals("node2", response.n2.getProperty("name"));
        } finally {
            transaction.success();transaction.close();
        }
    }

    @Test
    public void testCreateLargeSetOfNodes(){
        TestBatchResult response =this.restAPI.executeBatch(new BatchCallback<TestBatchResult>() {
            @Override
            public TestBatchResult recordBatch(RestAPI batchRestApi) {
                TestBatchResult result=new TestBatchResult();
                result.n1=batchRestApi.createNode(map("name", "node0"));
                for (int i=1;i<=1000;i++) {
                    result.n2=batchRestApi.createNode(map("name", "node"+i));
                }
                return result;
            }
        });
        Transaction transaction = getGraphDatabase().beginTx();
        try {
            assertEquals("node0", response.n1.getProperty("name"));
            assertEquals("node1000", response.n2.getProperty("name"));
        } finally {
            transaction.success();transaction.close();
        }
    }
   
    @Test
    public void testRestApiWorksRegardlessOfSource() {
        RestAPI leaked =this.restAPI.executeBatch(new BatchCallback<RestAPI>() {
            @Override
            public RestAPI recordBatch(RestAPI batchRestApi) {
                return batchRestApi;
            }
        });
        leaked.createNode(map());
        Transaction transaction = getGraphDatabase().beginTx();
        try {
            assertEquals(2, countExistingNodes());
        } finally {
            transaction.success();transaction.close();
        }
    }


    @Test
    public void testCreateRelationshipToNodeOutsideofBatch() throws Exception {
        final Node node1 = restAPI.createNode(map());

        Relationship relationship = restAPI.executeBatch(new BatchCallback<Relationship>() {

            @Override
            public Relationship recordBatch(RestAPI batchRestApi) {
                RestNode node2 = batchRestApi.createNode(map("name","node2"));
                return node1.createRelationshipTo(node2, RELATIONSHIP_TYPE);
            }

        });
        Transaction transaction = getGraphDatabase().beginTx();
        try {
            assertNotNull(relationship);
            assertEquals(RELATIONSHIP_TYPE.name(),relationship.getType().name());
            assertEquals(node1,relationship.getStartNode());
            assertEquals("node2", relationship.getEndNode().getProperty("name"));
        } finally {
            transaction.success();transaction.close();
        }
    }

    @Test(expected = IllegalStateException.class)
    public void testAccessRelationshipsInBatch() throws Exception {
        final Node node1 = restAPI.createNode(map("name","node1"));
        restAPI.executeBatch(new BatchCallback<Integer>() {
            @Override
            public Integer recordBatch(RestAPI batchRestApi) {
                return IteratorUtil.count(node1.getRelationships());
            }

        });

    }

    @Test
    public void testDeleteNodeAndRelationshipsInBatch() throws Exception {
        final Node node1 = restAPI.createNode(map("name","node1"));
        node1.createRelationshipTo(restAPI.createNode(map("name","node2")),RELATIONSHIP_TYPE);
        node1.createRelationshipTo(restAPI.createNode(map("name","node3")),RELATIONSHIP_TYPE);
        final Iterable<Relationship> rels = node1.getRelationships();
        Integer deleted = restAPI.executeBatch(new BatchCallback<Integer>() {

            @Override
            public Integer recordBatch(RestAPI batchRestApi) {
                int count = 0;
                for (Relationship rel : rels) {
                    rel.delete();
                    count++;
                }
                node1.delete();
                count++;
                return count;
            }

        });
        Transaction transaction = getGraphDatabase().beginTx();
        try {
            assertEquals(3, deleted.intValue());
        } finally {
            transaction.success();transaction.close();
        }
    }
    @Test
    public void testCreateRelationshipBetweenNodesOutsideofBatch() throws Exception {
        final Node node1 = restAPI.createNode(map());
        final Node node2 = restAPI.createNode(map());

        Relationship relationship = restAPI.executeBatch(new BatchCallback<Relationship>() {

            @Override
            public Relationship recordBatch(RestAPI batchRestApi) {
                return node1.createRelationshipTo(node2, RELATIONSHIP_TYPE);
            }

        });
        Transaction transaction = getGraphDatabase().beginTx();
        try {
            assertNotNull(relationship);
            assertEquals(RELATIONSHIP_TYPE.name(), relationship.getType().name());
            assertEquals(node1, relationship.getStartNode());
            assertEquals(node2, relationship.getEndNode());
        } finally {
            transaction.success();transaction.close();
        }
    }

    @Test
    public void testSetNodeProperties(){
        TestBatchResult response =this.restAPI.executeBatch(new BatchCallback<TestBatchResult>() {
            
            @Override
            public TestBatchResult recordBatch(RestAPI batchRestApi) {
                TestBatchResult result=new TestBatchResult();
                result.n1 = batchRestApi.createNode(map("name", "node1"));
                result.n1.setProperty("test", "true");
                result.n1.setProperty("test2", "stilltrue");
                
                return result;
            }
        });

        Transaction transaction = getGraphDatabase().beginTx();
        try {
            assertEquals("node1", response.n1.getProperty("name"));
            assertEquals("true", response.n1.getProperty("test"));
            assertEquals("stilltrue",response.n1.getProperty("test2"));
            assertEquals("true", loadRealNode(response.n1).getProperty("test"));
            assertEquals("stilltrue", loadRealNode(response.n1).getProperty("test2"));
        } finally {
            transaction.success();transaction.close();
        }
    }
    
    @Test  (expected = org.neo4j.graphdb.NotFoundException.class)
    public void testDeleteNode(){
        TestBatchResult response =this.restAPI.executeBatch(new BatchCallback<TestBatchResult>() {
            
            @Override
            public TestBatchResult recordBatch(RestAPI batchRestApi) {
                TestBatchResult result=new TestBatchResult();
                result.n1 = batchRestApi.createNode(map("name", "node1"));
                result.n1.delete();
                result.n2 = batchRestApi.createNode(map("name", "node2"));                
                return result;
            }
        });
        Transaction transaction = getGraphDatabase().beginTx();
        try {
            loadRealNode(response.n1);
        } finally {
            transaction.success();transaction.close();
        }
    }
    
    @Test  
    public void testDeleteRelationship(){
        TestBatchResult r =this.restAPI.executeBatch(new BatchCallback<TestBatchResult>() {
            
            @Override
            public TestBatchResult recordBatch(RestAPI batchRestApi) {
                TestBatchResult result=new TestBatchResult();
                result.n1 = batchRestApi.createNode(map("name", "newnode1"));
                result.n2 = batchRestApi.createNode(map("name", "newnode2"));
                result.rel = batchRestApi.createRelationship(result.n1, result.n2, Type.TEST, map("name", "rel") );
                result.rel.delete();
                
                return result;
            }
        });
        Transaction transaction = getGraphDatabase().beginTx();
        try {
            Relationship foundRelationship = TestHelper.firstRelationshipBetween( r.n1.getRelationships(Type.TEST, Direction.OUTGOING), r.n1, r.n2);
            Assert.assertNull("found relationship", foundRelationship);
        } finally {
            transaction.success();transaction.close();
        }
    }

    @Test
    public void testCreateRelationship(){
        TestBatchResult r = this.restAPI.executeBatch(new BatchCallback<TestBatchResult>() {
            @Override
            public TestBatchResult recordBatch(RestAPI batchRestApi) {
                TestBatchResult result=new TestBatchResult();
                result.n1 = batchRestApi.createNode(map("name", "newnode1"));
                result.n2 = batchRestApi.createNode(map("name", "newnode2"));
                result.rel = batchRestApi.createRelationship(result.n1, result.n2, Type.TEST, map("name", "rel"));
                result.allRelationships = result.n1.getRelationships();
                return result;
            }
        });

        Relationship foundRelationship = TestHelper.firstRelationshipBetween( r.n1.getRelationships(Type.TEST, Direction.OUTGOING), r.n1, r.n2);
        Assert.assertNotNull("found relationship", foundRelationship);
        assertEquals("same relationship", r.rel, foundRelationship);
        assertEquals("rel", r.rel.getProperty("name"));

        Transaction transaction = getGraphDatabase().beginTx();
        try {
            assertThat(r.n1.getRelationships(Type.TEST, Direction.OUTGOING), new IsRelationshipToNodeMatcher(r.n1, r.n2));
            assertThat(r.n1.getRelationships(Direction.OUTGOING), new IsRelationshipToNodeMatcher(r.n1, r.n2));
            assertThat(r.n1.getRelationships(Direction.BOTH), new IsRelationshipToNodeMatcher(r.n1, r.n2));
            assertThat(r.n1.getRelationships(Type.TEST), new IsRelationshipToNodeMatcher(r.n1, r.n2));
            assertThat(r.allRelationships, new IsRelationshipToNodeMatcher(r.n1, r.n2));
        } finally {
            transaction.success();transaction.close();
        }
    }

    @Test
    public void testAddToIndex() {
        final MatrixDataGraph matrixDataGraph = new MatrixDataGraph(getGraphDatabase());
        matrixDataGraph.createNodespace();
        final RestNode neoNode = restAPI.getNodeById(matrixDataGraph.getNeoNodeId());
        final IndexHits<Node> heroes = restAPI.executeBatch(new BatchCallback<IndexHits<Node>>() {
            @Override
            public IndexHits<Node> recordBatch(RestAPI batchRestApi) {
                restAPI.index().forNodes("heroes").add(neoNode,"indexname","Neo2");
                Node n1 = batchRestApi.createNode(map("name", "Apoc"));
                final Index<Node> index = batchRestApi.index().forNodes("heroes");
                index.add(n1, "indexname", "Apoc");

                Transaction transaction = getGraphDatabase().beginTx();
                try {
                    final Node indexResult = getGraphDatabase().index().forNodes("heroes").get("indexname", "Neo2").getSingle();
                    assertNull(indexResult);
                } finally {
                    transaction.success();transaction.close();
                }

                return index.query("indexname:Apoc");
            }
        });
        Transaction transaction = getGraphDatabase().beginTx();
        try {
        assertEquals("1 hero",1,heroes.size());        
        IndexManager index = getGraphDatabase().index();             
        Index<Node> goodGuys = index.forNodes("heroes");
        IndexHits<Node> hits = goodGuys.get( "indexname", "Apoc" );
        Node apoc = hits.getSingle();
        
        assertEquals("Apoc indexed",apoc,heroes.iterator().next());

        } finally {
            transaction.success();transaction.close();
        }
    }
    
    @Test
    public void testQueryIndex() {
        final MatrixDataGraph matrixDataGraph = new MatrixDataGraph(getGraphDatabase());
        matrixDataGraph.createNodespace();
        final IndexHits<Node> heroes = restAPI.executeBatch(new BatchCallback<IndexHits<Node>>() {
            @Override
            public IndexHits<Node> recordBatch(RestAPI batchRestApi) {
                final Index<Node> index = batchRestApi.index().forNodes("heroes");
                return index.query("name:Neo");
            }
        });
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
            restAPI.executeBatch(new BatchCallback<Void>() {
            @Override
            public Void recordBatch(RestAPI batchRestApi) {
                final Index<Node> index = batchRestApi.index().forNodes("heroes");
                index.delete();
                return null;
            }
        });
        Transaction transaction = getGraphDatabase().beginTx();
        try {
            IndexManager index = matrixDataGraph.getGraphDatabase().index();
            Assert.assertFalse(index.existsForNodes("heroes"));
        } finally {
            transaction.success();transaction.close();
        }
    }
    
    
    @Test  (expected = UnsupportedOperationException.class)
    public void testRemoveEntryFromIndexWithGivenNode(){
        TestBatchResult response =this.restAPI.executeBatch(new BatchCallback<TestBatchResult>() {
            
            @Override
            public TestBatchResult recordBatch(RestAPI batchRestApi) {
                TestBatchResult result=new TestBatchResult();
                result.n1 = batchRestApi.createNode(map("name", "node1"));                
                final Index<Node> index = batchRestApi.index().forNodes("testIndex");
                index.add( result.n1, "indexname", "Node1");
                index.remove(result.n1);
                return result;
            }
        });         
    }
    
    @Test  (expected = UnsupportedOperationException.class)
    public void testRemoveEntryFromIndexWithGivenNodeAndKey(){
        TestBatchResult response =this.restAPI.executeBatch(new BatchCallback<TestBatchResult>() {
            
            @Override
            public TestBatchResult recordBatch(RestAPI batchRestApi) {
                TestBatchResult result=new TestBatchResult();
                result.n1 = batchRestApi.createNode(map("name", "node1"));                
                final Index<Node> index = batchRestApi.index().forNodes("testIndex");
                index.add( result.n1, "indexname", "Node1");
                index.remove(result.n1,"indexname");
                return result;
            }
        });         
    }
    
    @Test  (expected = UnsupportedOperationException.class)
    public void testRemoveEntryFromIndexWithGivenNodeAndKeyAndValue(){
        TestBatchResult response =this.restAPI.executeBatch(new BatchCallback<TestBatchResult>() {
            
            @Override
            public TestBatchResult recordBatch(RestAPI batchRestApi) {
                TestBatchResult result=new TestBatchResult();
                result.n1 = batchRestApi.createNode(map("name", "node1"));                
                final Index<Node> index = batchRestApi.index().forNodes("testIndex");
                index.add( result.n1, "indexname", "Node1");
                index.remove(result.n1,"indexname", "Node1");
                return result;
            }
        });         
    }
     
    static class TestBatchResult {
        Node n1;
        Node n2;
        RestRelationship rel;
        Iterable<Relationship> allRelationships;
    }
}
