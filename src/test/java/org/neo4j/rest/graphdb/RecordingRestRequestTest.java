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

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;
import org.neo4j.rest.graphdb.batch.RecordingRestRequest;
import org.neo4j.rest.graphdb.batch.RestOperations;

public class RecordingRestRequestTest {
    
    private ExecutingRestRequest exeRequest;
    private RecordingRestRequest recRequest;
    private String baseUri;
    
    @Before
    public void init(){        
            this.baseUri = "www.test.net";
            this.exeRequest = new ExecutingRestRequest(baseUri);
            this.recRequest = new RecordingRestRequest(new RestOperations(), exeRequest.getUri());
    }
    
    @Test
    public void testCreate() {
        RecordingRestRequest testRequest = new RecordingRestRequest(new RestOperations(), exeRequest.getUri());
        assertEquals(this.baseUri, testRequest.getBaseUri());
        assertEquals(0, testRequest.getRecordedRequests().size());
        
    }
    
    @Test
    public void testGetWithoutData(){
       RequestResult response = recRequest.get("/node");      
       assertEquals(1, response.getBatchId());
       assertEquals(1, recRequest.getRecordedRequests().size());
    }
    
    @Test
    public void testGetWithData(){
       RequestResult response = recRequest.get("/node","Test");      
       assertEquals(1, response.getBatchId());
       assertEquals(1, recRequest.getRecordedRequests().size());
    }
    
    @Test
    public void testPost(){
       RequestResult response = recRequest.post("/node","Test");      
       assertEquals(1, response.getBatchId());
       assertEquals(1, recRequest.getRecordedRequests().size());
    }
    
    @Test
    public void testPut(){
       RequestResult response = recRequest.put("/node","Test");      
       assertEquals(1, response.getBatchId());
       assertEquals(1, recRequest.getRecordedRequests().size());
    }
    
    @Test
    public void testDelete(){
       RequestResult response = recRequest.delete("/node");      
       assertEquals(1, response.getBatchId());
       assertEquals(1, recRequest.getRecordedRequests().size());
    }
    
    @Test
    public void testMultipleEntries(){
       RequestResult response = recRequest.post("/node","Test");      
       assertEquals(1, response.getBatchId());
       assertEquals(1, recRequest.getRecordedRequests().size());     
       response = recRequest.delete("/node");      
       assertEquals(2, response.getBatchId());
       assertEquals(2, recRequest.getRecordedRequests().size());
       response = recRequest.get("/node","Test");      
       assertEquals(3, response.getBatchId());
       assertEquals(3, recRequest.getRecordedRequests().size());
    }

}
