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

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.util.Map;
import java.util.regex.Pattern;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation.Builder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;




import javax.ws.rs.core.Response;




//import com.sun.jersey.api.client.filter.LoggingFilter; 
import org.neo4j.helpers.collection.MapUtil;
import org.neo4j.rest.graphdb.util.Config;
import org.neo4j.rest.graphdb.util.JsonHelper;

//import com.sun.jersey.api.client.Client;
//import com.sun.jersey.api.client.ClientResponse;
//import com.sun.jersey.api.client.WebResource;
//import com.sun.jersey.api.client.WebResource.Builder;
//import com.sun.jersey.api.client.filter.HTTPBasicAuthFilter;








import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;

public class ExecutingRestRequest implements RestRequest {

	public static final MediaType STREAMING_JSON_TYPE = new MediaType(APPLICATION_JSON_TYPE.getType(),APPLICATION_JSON_TYPE.getSubtype(), MapUtil.stringMap("stream","true"));
	private final String baseUri;
	private final UserAgent userAgent = new UserAgent();
	private final Client client;

	private static final Pattern pattern = Pattern.compile("^https?://.*");

	public ExecutingRestRequest( String baseUri ) {
		this( baseUri, null, null );
	}

	public ExecutingRestRequest( String baseUri, String username, String password ) {
		this.baseUri = uriWithoutSlash( baseUri );
		client = createClient();
		addAuthFilter(username, password);

	}

	protected void addAuthFilter(String username, String password) {
		if (username == null) return;
		//        client.addFilter( new HTTPBasicAuthFilter( username, password ) );
	}

	protected Client createClient() {
		Client client = ClientBuilder.newClient();
		//        client.setConnectTimeout(Config.getConnectTimeout());
		//        client.setReadTimeout(Config.getReadTimeout());
		//        client.setChunkedEncodingSize(8*1024);
		userAgent.install(client);
		if (Config.useLoggingFilter()) {
			//            client.addFilter(new LoggingFilter());
		}
		return client;
	}

	private ExecutingRestRequest( String uri, Client client ) {
		this.baseUri = uriWithoutSlash( uri );
		this.client = client;
	}

	protected String uriWithoutSlash( String uri ) {
		return  (uri.endsWith("/") ?  uri.substring(0, uri.length() - 1)  : uri);
	}

	public static String encode( Object value ) {
		if ( value == null ) return "";
		try {
			return URLEncoder.encode( value.toString(), "utf-8" ).replaceAll( "\\+", "%20" );
		} catch ( UnsupportedEncodingException e ) {
			throw new RuntimeException( e );
		}
	}


	private Builder builder( String path ) {

		WebTarget target = client.target(uri( pathOrAbsolute( path ) ) );

		if (Config.streamingIsEnabled()){
			return target.request().accept(STREAMING_JSON_TYPE).header("X-Stream","true");
		} 
		
		return target.request().accept(APPLICATION_JSON_TYPE);
	}

	private String pathOrAbsolute( String path ) {
		if (pattern.matcher(path).matches()) {
			return path;
		}
		return baseUri + "/" + path;
	}


	@Override
	public RequestResult get( String path ) {
		return RequestResult.extractFrom(builder(path).get(Response.class));
	}


	@Override
	public RequestResult get( String path, Object data ) {
		Builder builder = builder(path);
		
		/*
		 * œtodo: whz a get method couls need an entity?
		 */
		if ( data != null ) {
			throw new RuntimeException("a getter call has no entity body");
//			builder = builder.entity( JsonHelper.createJsonFrom( data ), APPLICATION_JSON_TYPE );
		}
		return RequestResult.extractFrom(builder.get(Response.class));
	}


	@Override
	public RequestResult delete(String path) {
		return RequestResult.extractFrom(builder(path).delete(Response.class));
	}


	@Override
	public RequestResult post( String path, Object data ) {
		Builder builder = builder( path );
		if ( data != null ) {
			Object payload = data instanceof InputStream ? data : JsonHelper.createJsonFrom(data);
			return RequestResult.extractFrom(builder.post(Entity.json(payload) , Response.class));
		} else {
			return RequestResult.extractFrom(builder.post(null, Response.class));
		}
		
	}

	@Override
	public RequestResult put( String path, Object data ) {
		Builder builder = builder( path );
		if ( data != null ) {
			return RequestResult.extractFrom(builder.put( Entity.json(JsonHelper.createJsonFrom( data )), Response.class ));
		}
		return RequestResult.extractFrom(builder.put(null,Response.class));
	}

	@Override
	public RestRequest with( String uri ) {
		return new ExecutingRestRequest(uri, client);
	}

	private URI uri( String uri ) {
		try {
			return new URI( uri );
		} catch ( URISyntaxException e ) {
			throw new RuntimeException( e );
		}
	}


	@Override
	public String getUri() {
		return baseUri;
	}

	@Override
	public Map<?, ?> toMap(RequestResult requestResult) {	
		return requestResult.toMap();
	}

	public void close() {
		client.close();
	}
}
