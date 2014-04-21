The Java binding for the Neo4j Server REST API wraps the REST calls behind the well known
[GraphDatabaseService](http://api.neo4j.org/1.8.M07/org/neo4j/graphdb/GraphDatabaseService.html) API.

**Please note**, that the performance semantics are not the same, as most of these operations will cause a network
request to the server. Make sure to minimize the fine grained operations and rely more on cypher and traversals to get
your data and on batching and cypher to update the graph.

You can also use the `RestAPIFacade` directly to interact with your Neo4j-Server, without the GraphDatabaseService Wrapper.

Note
----
The behavior of "transactions" changed in 1.8, in 1.7 they were no-op, i.e. just ignored. So you could just leave them off to not confuse people.

In 1.8 it tries to collect all operations within a tx as a batch-operation which will then be executed on the server.

This has the implication that the results retrieved within that "tx" are not immediately available but only after you called tx.success and tx.close



Currently supports:
___________________
 * all the node and relationship operations
 * [cypher operations](http://docs.neo4j.org/chunked/milestone/rest-api-cypher.html)
 * [REST-batch operations](http://docs.neo4j.org/chunked/milestone/rest-api-batch-ops.html)
 * Basic Http Auth (Digest) (Important for using [Neo4j on Heroku](https://devcenter.heroku.com/articles/neo4j)
 * index creation and index operations (add, get, query, delete)
 * (Auto Index configuration)[http://docs.neo4j.org/chunked/milestone/rest-api-configurable-auto-indexes.html]
 * limited [traversal API](http://docs.neo4j.org/chunked/milestone/rest-api-traverse.html) but with dynamic language support
 * preliminary support for arbitrary server plugins and extensions
 * gremlin support
 
Usage:
------

Build it locally. Then use the maven / ivy dependency or copy the jar into your app.

    <dependency>
		<groupId>org.neo4j</groupId>
		<artifactId>neo4j-rest-graphdb</artifactId>
		<version>2.0.0</version>
    </dependency>
    
    and in the repositories section add:
    
    <repository>
       <id>neo4j-contrib-releases</id>
       <url>https://raw.github.com/neo4j-contrib/m2/master/releases</url>
    </repository>

    GraphDatabaseService gds = new RestGraphDatabase("http://localhost:7474/db/data");
    GraphDatabaseService gds = new RestGraphDatabase("http://localhost:7474/db/data",username,password);

    // or using the RestAPI directly
    RestAPI restAPI = new RestAPIFacade("http://localhost:7474/db/data",username,password);

    // as a Spring Bean, e.g. in [Spring Data Neo4j](http://www.springsource.org/spring-data/neo4j)
    <bean id="graphDbService" class="org.neo4j.rest.graphdb.RestGraphDatabase" destroy-method="shutdown">
        <constructor-arg index="0" value="http://localhost:7474/db/data" />
    </bean>
    </pre>

References / Community:
-----------------------

 * [Neo4j community site](http://neo4j.org)
 * [Neo4j REST API](http://docs.neo4j.org/chunked/milestone/rest-api.html)
 * [Neo4j Docs](http://docs.neo4j.org)
 * [Neo4j Mailing List](http://neo4j.org/forums)


Configuration (System-Properties)
-------------

_timeouts in seconds_

* org.neo4j.rest.read_timeout=30
* org.neo4j.rest.connect_timeout=30
* org.neo4j.rest.driver="neo4j-rest-graphdb/1.8.RC1"
* org.neo4j.rest.stream=true
* org.neo4j.rest.batch_transaction=false (convert transaction scope into batch-rest-operations)
* org.neo4j.rest.logging_filter=false (set to true if verbose request/response logging should be enabled)
