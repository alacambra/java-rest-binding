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

import static java.util.Arrays.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.neo4j.graphdb.Node;
import org.neo4j.helpers.collection.IteratorUtil;
import org.neo4j.helpers.collection.MapUtil;
import org.neo4j.rest.graphdb.entity.RestNode;
import org.neo4j.rest.graphdb.query.RestCypherQueryEngine;
import org.neo4j.rest.graphdb.util.QueryResult;


public class RestCypherQueryEngineTest extends RestTestBase {
    private RestCypherQueryEngine queryEngine;
    private RestAPI restAPI;
    private MatrixDataGraph embeddedMatrixdata;
    private MatrixDataGraph restMatrixData;

    public RestCypherQueryEngineTest( String url )
    {
        super( url );
    }

    @Before
    public void init() throws Exception {
        embeddedMatrixdata = new MatrixDataGraph(getGraphDatabase()).createNodespace();
        restMatrixData = new MatrixDataGraph(getRestGraphDb());
        this.restAPI = ((RestGraphDatabase)getRestGraphDb()).getRestAPI();
        queryEngine = new RestCypherQueryEngine(restAPI);      
    }
    
    @Test
    public void testGetReferenceNode(){
        final String queryString = "start n=node({reference}) return n";
        final Node result = (Node) queryEngine.query(queryString, MapUtil.map("reference",0)).to(Node.class).single();
        assertEquals(embeddedMatrixdata.getGraphDatabase().getNodeById(0), result);

    }
    
    @Test
    public void testGetNeoNode(){        
        final String queryString = "start neo=node({neoname}) return neo";
        final Node result = (Node) queryEngine.query(queryString, MapUtil.map("neoname",getNeoId())).to(Node.class).single();
        assertEquals(embeddedMatrixdata.getNeoNode(), result);
    }
    
    @Test
    public void testGetNeoNodeByIndexLookup(){
        final String queryString = "start neo=node:heroes(name={neoname}) return neo";
        final Node result = (Node) queryEngine.query(queryString, MapUtil.map("neoname","Neo")).to(Node.class).single();
        assertEquals(embeddedMatrixdata.getNeoNode(), result);
    }

    @Ignore
    @Test
    public void testGetNeoNodeByIndexQuery(){        
        final String queryString = "start neo=node:heroes({neoquery}) return neo";
        final Node result = (Node) queryEngine.query(queryString, MapUtil.map("neoquery","name:Neo")).to(Node.class).single();
        assertEquals(embeddedMatrixdata.getNeoNode(), result);
    }
    @Test
    public void testGetNeoNodeSingleProperty(){       
        final String queryString = "start n=node({neo}) return n.name";
        final String result = (String) queryEngine.query(queryString, MapUtil.map("neo",getNeoId())).to(String.class).single();
        assertEquals("Thomas Anderson", result);
    }
    
    @Test
    public void testGetNeoNodeViaMorpheus(){
        final String queryString = "start morpheus=node:heroes(name={morpheusname}) match (morpheus) <-[:KNOWS]- (neo) return neo";
        final Node result = (Node) queryEngine.query(queryString, MapUtil.map("morpheusname","Morpheus")).to(Node.class).single();
        assertEquals(embeddedMatrixdata.getNeoNode(), result);
    }
    
    @Test
    public void testGetCypherNodeViaMorpheusAndFilter(){
        final String queryString = "start morpheus=node:heroes(name={morpheusname}) match (morpheus) -[:KNOWS]-> (person) where person.type = \"villain\" return person";
        final Node result = (Node) queryEngine.query(queryString, MapUtil.map("morpheusname","Morpheus")).to(Node.class).single();
        assertEquals("Cypher", result.getProperty("name"));
    }

    @Test
    public void testCollectClauseReturnsCollectionOfNodes(){
        final String queryString = "start morpheus=node:heroes(name={morpheusname}) match (morpheus) -[:KNOWS]-> (person) return collect(person) as known_people";
        final QueryResult qr= queryEngine.query(queryString, MapUtil.map("morpheusname", "Morpheus"));
        final Collection<Map<String,Object>> result = IteratorUtil.asCollection(qr);

        assertEquals(1, result.size());
        Map<String,Object> firstEntry = result.iterator().next();
        List nodes = (List)firstEntry.get("known_people");

        for (Object anEntity: nodes) {
            assertEquals(RestNode.class, anEntity.getClass());
            Node aFriend = (Node)anEntity;
            assertTrue(aFriend.getProperty("name").equals("Trinity") ||
                       aFriend.getProperty("name").equals("Cypher"));
        }

    }
    
    @Test
    public void testGetArchitectViaMorpheusAndFilter(){
        final String queryString = "start morpheus=node:heroes(name={morpheusname}) match (morpheus) -[:KNOWS]-> (person) -[:KNOWS]-> (smith) -[:CODED_BY]-> (architect) where person.type = \"villain\" return architect";
        final Node result = (Node) queryEngine.query(queryString, MapUtil.map("morpheusname","Morpheus")).to(Node.class).single();
        assertEquals("The Architect", result.getProperty("name"));
    }
    
    
    @Test
    public void testGetNeoNodeMultipleProperties(){
        final String queryString = "start neo=node({neoId}) return neo.name, neo.type, neo.age";
        final Collection<Map<String,Object>> result = IteratorUtil.asCollection(queryEngine.query(queryString, MapUtil.map("neoId",getNeoId())));
        assertEquals(asList( MapUtil.map("neo.name", "Thomas Anderson", "neo.type","hero", "neo.age", 29 )),result); 
    }
    
    @Test
    public void testGetRelationshipType(){
        final String queryString ="start n=node({reference}) match (n)-[r]->() return type(r)";
        final Collection<String> result =  IteratorUtil.asCollection(queryEngine.query(queryString, MapUtil.map("reference",0)).to(String.class)); 
        assertTrue(result.contains("NEO_NODE"));      
    }
    
    
    public long getNeoId(){
        return  embeddedMatrixdata.getNeoNode().getId();
    }    
   
}
