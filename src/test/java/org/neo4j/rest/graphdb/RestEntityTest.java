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

import java.util.Arrays;

import org.junit.Assert;
import org.junit.Test;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.rest.graphdb.util.TestHelper;

public class RestEntityTest extends RestTestBase  {

    public RestEntityTest( String url )
    {
        super( url );
    }

    @Test
    public void testSetProperty() {
        getRestGraphDb().getNodeById(0).setProperty( "name", "test" );
        Node node = getRestGraphDb().getNodeById(0);
        Assert.assertEquals( "test", node.getProperty( "name" ) );
    }

    @Test
    public void testSetStringArrayProperty() {
        getRestGraphDb().getNodeById(0).setProperty( "name", new String[]{"test"} );
        Node node = getRestGraphDb().getNodeById(0);
        Assert.assertArrayEquals( new String[]{"test"}, (String[])node.getProperty( "name" ) );
    }
    @Test
    public void testSetDoubleArrayProperty() {
        double[] data = {0, 1, 2};
        getRestGraphDb().getNodeById(0).setProperty( "data", data );
        Node node = getRestGraphDb().getNodeById(0);
        Assert.assertTrue("same double array",Arrays.equals( data, (double[])node.getProperty( "data" ) ));
    }

    @Test
    public void testRemoveProperty() {
        Node node = getRestGraphDb().getNodeById(0);
        node.setProperty( "name", "test" );
        Assert.assertEquals( "test", node.getProperty( "name" ) );
        node.removeProperty( "name" );
        Assert.assertEquals( false, node.hasProperty( "name" ) );
    }


    @Test
    public void testSetPropertyOnRelationship() {
        Node refNode = getRestGraphDb().getNodeById(0);
        Node node = getRestGraphDb().createNode();
        Relationship rel = refNode.createRelationshipTo( node, Type.TEST );
        rel.setProperty( "name", "test" );
        Assert.assertEquals( "test", rel.getProperty( "name" ) );
        Relationship foundRelationship = TestHelper.firstRelationshipBetween( refNode.getRelationships( Type.TEST, Direction.OUTGOING ), refNode, node );
        Assert.assertEquals( "test", foundRelationship.getProperty( "name" ) );
    }

    @Test
    public void testRemovePropertyOnRelationship() {
        Node refNode = getRestGraphDb().getNodeById(0);
        Node node = getRestGraphDb().createNode();
        Relationship rel = refNode.createRelationshipTo( node, Type.TEST );
        rel.setProperty( "name", "test" );
        Assert.assertEquals( "test", rel.getProperty( "name" ) );
        Relationship foundRelationship = TestHelper.firstRelationshipBetween( refNode.getRelationships( Type.TEST, Direction.OUTGOING ), refNode, node );
        Assert.assertEquals( "test", foundRelationship.getProperty( "name" ) );
        rel.removeProperty( "name" );
        Assert.assertEquals( false, rel.hasProperty( "name" ) );
        Relationship foundRelationship2 = TestHelper.firstRelationshipBetween( refNode.getRelationships( Type.TEST, Direction.OUTGOING ), refNode, node );
        Assert.assertEquals( false, foundRelationship2.hasProperty( "name" ) );
    }

}
