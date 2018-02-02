/*******************************************************************************
 * Copyright 2017 SalzburgResearch Forschungsgesellschaft
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package at.newmedialab.lmf.search.ldpath.model.functions;

import java.util.Collection;
import java.util.Collections;

import org.apache.marmotta.kiwi.model.rdf.KiWiLiteral;
import org.apache.marmotta.ldpath.api.backend.RDFBackend;
import org.apache.marmotta.ldpath.api.functions.SelectorFunction;

import at.newmedialab.lmf.search.ldpath.model.backend.WildcardAwareBackend;

public class DynamicFieldFunction<Node>  extends SelectorFunction<Node> {

	@Override
    public Collection<Node> apply(RDFBackend<Node> backend, Node context, Collection<Node>... args) throws IllegalArgumentException {
    	if (args.length < 1 && args.length > 2) {
            throw new IllegalArgumentException("wrong usage: " + getSignature());
    	}
    	if (args.length < 2 && args[0].size() != 1 ) {
            throw new IllegalArgumentException("wrong usage: " + getSignature());
    	}
        if (args.length == 2 && args[1].size() != 1) {
            throw new IllegalArgumentException("wrong usage: " + getSignature());
        }
        String namespace = null;
    	String propertyPrefix = null;
    	String propertyPrefixWildcard = "*";
    	
    	for (Node node : args[0]) {
    		if (node instanceof KiWiLiteral) {
    			namespace = ((KiWiLiteral) node).stringValue();
    			if ( ! (namespace.endsWith("/") || namespace.endsWith("#"))) {
    				throw new IllegalArgumentException("Invalid namespace argument: "+ getSignature());
    			}
    			break;
    		}
    		
    	}
    	if ( args.length == 2) {
    		for (Node node : args[1]) {
    			if (node instanceof KiWiLiteral) {
    				propertyPrefix = ((KiWiLiteral) node).stringValue();
    				if ( !propertyPrefix.endsWith("*") || propertyPrefix.endsWith("%")) {
    					// required to have list objects working
    					propertyPrefixWildcard = propertyPrefix + "*";
    				}
    				else {
    					propertyPrefixWildcard = propertyPrefix;
    					propertyPrefix = propertyPrefix.replaceAll("*", "");
    					propertyPrefix = propertyPrefix.replaceAll("%", "");
    				}
    				break;
    			}
    		}
    	}
        if (propertyPrefixWildcard != null && namespace != null) {
        	if ( backend instanceof WildcardAwareBackend ) {
        		Node literal = backend.createLiteral(namespace + propertyPrefixWildcard);
        		return backend.listObjects(context, literal);
        	}
        	else {
        		return backend.listObjects(context, null);
        	}
        	
        }
        

    	return Collections.<Node>emptyList();
    }

    @Override
    public String getSignature() {
        return "fn:dynamicPrefix(namespace: String [, propertyPrefix: String]) : NodeList";
    }

    @Override
    public String getDescription() {
        return "Create a string representing label & uri ";
    }

    @Override
    public String getLocalName() {
        return "dynamicPrefix";
    }
}
