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

/**
 * User: KBurchardi
 * Date: 20.10.11
 * Time: 12:44
 */
public class TestHelloWorldResource extends RestTestBase {
     private RestAPI restAPI;

    @Before
    public void init(){
        this.restAPI = ((RestGraphDatabase)getRestGraphDb()).getRestAPI();
    }

    @Test
    public void testHelloWorldService() throws Exception {
        HelloWorldService hws = this.restAPI.getService(HelloWorldService.class, SERVER_ROOT+"/test");
        assertEquals("get 0", hws.get(0));
        //assertEquals("put 0:post", hws.put(0, "put"));
        //assertEquals("post 0:post", hws.post(0, "post"));
        hws.postWithoutResult(0);
        assertEquals("delete 0", hws.delete(0));
    }
}
