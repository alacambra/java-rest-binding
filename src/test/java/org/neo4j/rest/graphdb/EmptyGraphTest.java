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
import org.junit.Ignore;
import org.junit.Test;
import org.neo4j.graphdb.NotFoundException;
import org.neo4j.graphdb.Transaction;

public class EmptyGraphTest extends RestTestBase {

    public EmptyGraphTest( String url )
    {
        super( url );
    }

    /*
	 * @TODO hoe to test that with indexes?
	 *  Deprecated. The reference node concept is obsolete - indexes are the canonical way of getting hold of entry points in the graph.
	 *  @link http://api.neo4j.org/1.9.5/org/neo4j/graphdb/GraphDatabaseService.html
	 */
    @Ignore
    @Test(expected = NotFoundException.class)
    public void testGetReferenceNodeOnEmptyDbFails() {
//        Transaction tx = getGraphDatabase().beginTx();
//        try {
//            getGraphDatabase().getReferenceNode().delete();
//            tx.success();
//        } finally
//        {
//            tx.finish();
//        }
//        getRestGraphDb().getReferenceNode();
    }

    @Override
    @After
    public void tearDown() throws Exception {
        super.tearDown();
        initServer();
        startDb();
    }
}
