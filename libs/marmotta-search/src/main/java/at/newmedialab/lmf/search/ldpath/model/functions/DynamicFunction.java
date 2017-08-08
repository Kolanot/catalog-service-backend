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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import org.apache.marmotta.kiwi.model.rdf.KiWiLiteral;
import org.apache.marmotta.ldpath.api.backend.RDFBackend;
import org.apache.marmotta.ldpath.api.functions.SelectorFunction;
import org.openrdf.model.Value;

import com.google.common.base.CaseFormat;

import at.newmedialab.lmf.search.ldpath.model.backend.WildcardAwareBackend;

public class DynamicFunction  extends SelectorFunction<Value> {

	@Override
    public Collection<Value> apply(RDFBackend<Value> backend, Value context, Collection<Value>... args) throws IllegalArgumentException {
//    	if (args.length < 1 && args.length > 4) {
//            throw new IllegalArgumentException("wrong usage: " + getSignature());
//    	}
    	if (args.length !=2 && args[0].size() != 1 ) {
            throw new IllegalArgumentException("wrong usage: " + getSignature());
    	}
        String fieldName = null;
        //String fieldValue = null;
    	for (Value node : args[0]) {
    		if (node instanceof KiWiLiteral) {
    			String value = ((KiWiLiteral) node).stringValue();
    			fieldName = formatNameQualifier(value);
    			break;
    		}
    		
    	}
    	if ( backend instanceof WildcardAwareBackend) {
    		Collection<String> values = processArguments(args[1]);
    		return ((WildcardAwareBackend) backend).createDynLiteralCollection(fieldName, values);
    	}
    	else {
    		return Collections.<Value>emptyList();
    	}
    }
	private Collection<String> processArguments(Collection<Value> values ) {
		Collection<String> fieldValues = new ArrayList<>();
    	for (Value node :values) {
    		if (node instanceof KiWiLiteral) {
    			String value = ((KiWiLiteral) node).stringValue();
    			fieldValues.add(value);
    		}
    	}
    	return fieldValues;
		
	}
    @Override
    public String getSignature() {
        return "fn:dynamic(name: Value, value: Value]) : DynNodeList";
    }

    @Override
    public String getDescription() {
        return "dynamically specify solr index field based on name qualifier";
    }

    @Override
    public String getLocalName() {
        return "dynamic";
    }
    private String formatNameQualifier(String input) {
		input = input.replaceAll("[^a-zA-Z0-9 ]", "");
		input = input.trim().replaceAll(" ", "_").toUpperCase();
		return CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.LOWER_CAMEL, input);
    }
}
