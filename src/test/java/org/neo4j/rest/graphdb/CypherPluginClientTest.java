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

import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.neo4j.graphdb.Node;
import org.neo4j.helpers.collection.MapUtil;
import org.neo4j.rest.graphdb.query.RestCypherQueryEngine;
import org.neo4j.rest.graphdb.util.QueryResultBuilder;


/**
 * User: KBurchardi
 * Date: 13.10.11
 * Time: 14:26
 */
@Ignore("No CypherPlugin anymore in 2.0")
// todo replace with a simple example plugin instead
public class CypherPluginClientTest extends RestTestBase {
    private RestCypherQueryEngine queryEngine;
    private RestAPI restAPI;
    private MatrixDataGraph embeddedMatrixdata;
    private MatrixDataGraph restMatrixData;


    @Before
    public void init(){
        embeddedMatrixdata = new MatrixDataGraph(getGraphDatabase()).createNodespace();
        restMatrixData = new MatrixDataGraph(getRestGraphDb());
        this.restAPI = ((RestGraphDatabase)getRestGraphDb()).getRestAPI();
        queryEngine = new RestCypherQueryEngine(restAPI);
    }

    @Test
    public void  testPluginClient(){
        CypherPlugin proxy = this.restAPI.getPlugin(CypherPlugin.class);
        Iterable<Object> result = proxy.execute_query("start n=node({reference}) return n", MapUtil.map("reference", 0), null);
        Node resultNode = (Node)new QueryResultBuilder(result).to(Node.class).single();
        final String queryString = "start n=node({reference}) return n";
        final Node resultNodeCypher = (Node) queryEngine.query(queryString, MapUtil.map("reference",0)).to(Node.class).single();
        Assert.assertEquals(resultNodeCypher, resultNode);

    }
}
