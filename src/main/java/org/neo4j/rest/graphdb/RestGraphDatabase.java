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


import org.neo4j.graphdb.*;
import org.neo4j.graphdb.schema.Schema;
import org.neo4j.graphdb.traversal.BidirectionalTraversalDescription;
import org.neo4j.kernel.impl.nioneo.store.StoreId;
import org.neo4j.rest.graphdb.entity.RestNode;
import org.neo4j.rest.graphdb.index.RestIndexManager;
import org.neo4j.rest.graphdb.query.RestCypherQueryEngine;
import org.neo4j.rest.graphdb.traversal.RestTraversal;
import org.neo4j.rest.graphdb.traversal.RestTraversalDescription;
import org.neo4j.rest.graphdb.util.ResourceIterableWrapper;
import org.neo4j.rest.graphdb.util.ResultConverter;

import javax.transaction.TransactionManager;
import java.util.Collection;
import java.util.Map;


public class RestGraphDatabase extends AbstractRemoteDatabase {
    private RestAPI restAPI;
    private final RestCypherQueryEngine cypherQueryEngine;


    public RestGraphDatabase( RestAPI api){
    	this.restAPI = api;
        cypherQueryEngine = new RestCypherQueryEngine(restAPI);
    }
    
    public RestGraphDatabase( String uri ) {
        this( new RestAPIFacade( uri ));
    }

    public RestGraphDatabase( String uri, String user, String password ) {
        this(new RestAPIFacade( uri, user, password ));
    }

    public RestAPI getRestAPI(){
    	return this.restAPI;
    }

    public RestIndexManager index() {
       return this.restAPI.index();
    }

    public Node createNode() {
    	return this.restAPI.createNode(null);
    }
  
    public Node getNodeById( long id ) {
    	return this.restAPI.getNodeById(id);
    }

    public Node getReferenceNode(boolean create) {
        return this.restAPI.getReferenceNode();
    }

    @Override
    public Iterable<Node> getAllNodes() {
        return cypherQueryEngine.query("start n=node(*) return n", null).to(Node.class);
    }

    @Override
    public Iterable<RelationshipType> getRelationshipTypes() {
        return cypherQueryEngine.query("start n=node(*) match n-[r]->() return distinct type(r) as rel_type", null).to(RelationshipType.class, new ResultConverter<Map<String, Object>, RelationshipType>() {
            @Override
            public RelationshipType convert(Map<String, Object> row, Class<RelationshipType> type) {
                return DynamicRelationshipType.withName((String)row.get("rel_type"));
            }
        });
    }

    public Relationship getRelationshipById( long id ) {
    	return this.restAPI.getRelationshipById(id);
    }    
    @Override
    public String getStoreDir() {
        return restAPI.getBaseUri();
    }

    @Override
    public StoreId storeId() {
        return null;
    }

    @Override
    public boolean isAvailable(long timeout) {
        return restAPI!=null;
    }

    public TransactionManager getTxManager() {
        return new BatchTransactionManager(restAPI); //new NullTransactionManager();
    }

    @Override
    public DependencyResolver getDependencyResolver() {
        return new DependencyResolver.Adapter() {
            @Override
            public <T> T resolveDependency(Class<T> type, SelectionStrategy selector) throws IllegalArgumentException {
                if (TransactionManager.class.isAssignableFrom(type)) return (T)getTxManager();
                return null;
            }
        };
    }

    @Override
    public Transaction beginTx() {
        return restAPI.beginTx();
    }

    @Override
    public void shutdown() {
        restAPI.close();
    }

    @Override
    public Node createNode(Label... labels) {
        RestNode node = restAPI.createNode(null);
        String[] labelNames = new String[labels.length];
        for (int i = 0; i < labels.length; i++) labelNames[i]=labels[i].name();
        restAPI.addLabels(node,labelNames);
        return node;
    }

    @Override
    public ResourceIterable<Node> findNodesByLabelAndProperty(Label label, String property, Object value) {
        Iterable<RestNode> nodes = restAPI.getNodesByLabelAndProperty(label.name(), property, value);
        return new ResourceIterableWrapper<Node,RestNode>(nodes) {
            protected Node underlyingObjectToObject(RestNode node) {
                return node;
            }
        };
    }

    @Override
    public Schema schema() {
        throw new UnsupportedOperationException();
    }

    @Override
    public RestTraversalDescription traversalDescription() {
        return RestTraversal.description();
    }

    @Override
    public BidirectionalTraversalDescription bidirectionalTraversalDescription() {
        throw new UnsupportedOperationException();
    }

    public Collection<String> getAllLabelNames() {
        return restAPI.getAllLabelNames();
    }
}

