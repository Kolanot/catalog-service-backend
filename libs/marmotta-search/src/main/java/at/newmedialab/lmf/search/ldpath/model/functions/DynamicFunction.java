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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.marmotta.kiwi.model.rdf.KiWiLiteral;
import org.apache.marmotta.ldpath.api.backend.RDFBackend;
import org.apache.marmotta.ldpath.api.functions.SelectorFunction;
import org.openrdf.model.Value;

import com.google.common.base.CaseFormat;

import at.newmedialab.lmf.search.ldpath.model.backend.WildcardAwareBackend;

public class DynamicFunction extends SelectorFunction<Value> {

	@Override
	public Collection<Value> apply(RDFBackend<Value> backend, Value context, Collection<Value>... args)
			throws IllegalArgumentException {
		// if (args.length < 1 && args.length > 4) {
		// throw new IllegalArgumentException("wrong usage: " + getSignature());
		// }
		
		if ( args.length < 2 ) {
			throw new IllegalArgumentException("wrong usage: " + getSignature());
		}
		if ( args.length > 3 ) {
			throw new IllegalArgumentException("wrong usage: " + getSignature());
		}
//		if ( args.length == 3 && args[1].size()!= args[2].size() && args[2].size() != 1) {
//			throw new IllegalArgumentException("wrong usage: " + getSignature());
//		}
		if ( args.length == 3 && args[2].size() == 0 ) {
		    throw new IllegalArgumentException("wrong usage: " + getSignature());
		}
		String fieldName = null;
		// String fieldValue = null;
		for (Value node : args[0]) {
			if (node instanceof KiWiLiteral) {
				String value = ((KiWiLiteral) node).stringValue();
				fieldName = formatNameQualifier(value);
				break;
			}

		}
		if (fieldName != null && backend instanceof WildcardAwareBackend) {
			if ( args.length == 2) { // name and values provided
				Collection<String> values = processArguments(args[1]);
				return ((WildcardAwareBackend) backend).createDynLiteralCollection(fieldName, values);
			}
			else if ( args.length == 3) { // name values and suffix_extension provided
				Map<String, Collection<String>> valueMap = processArgumentsSuffix(fieldName, args[2], args[1]);
				return ((WildcardAwareBackend) backend).createDynLiteralCollection(valueMap);
			}
		} else {
			// not applicable since only WildcardAwareBackend create DynLiteral Collections
		}
		return Collections.<Value>emptyList();
	}

	private Collection<String> processArguments(Collection<Value> values) {
		Collection<String> fieldValues = new ArrayList<>();
		for (Value node : values) {
			if (node instanceof KiWiLiteral) {
				String value = ((KiWiLiteral) node).stringValue();
				fieldValues.add(value);
			}
		}
		return fieldValues;

	}
	private Map<String, Collection<String>> processArgumentsSuffix(String name, Collection<Value> qualifiers, Collection<Value> values) {
		HashMap<String, Collection<String>> map = new HashMap<>();
		// qualifier & values must be of the same length
		Value[] qArray = new Value[qualifiers.size()];
		qualifiers.toArray(qArray);
		Value[] vArray = new Value[values.size()];
		values.toArray(vArray);
		// handle unitMapping - when only one unit provided (qArray.length is 1) 
		boolean mapUnit = false;
		if ( qArray.length == 1 && vArray.length > 1) {
		    mapUnit = true;
		}
		for ( int i = 0; i < vArray.length; i++) {
			// access the extension element
			Value qualifier = (mapUnit ? qArray[0] : qArray[i]);
			// access the value element
			Value value = vArray[i];
			String extension = "";
			String strval = "";
			if ( qualifier instanceof KiWiLiteral) {
				extension = ((KiWiLiteral) qualifier).stringValue();
			}
			if ( value instanceof KiWiLiteral ) {
				strval = ((KiWiLiteral) value).stringValue();
			}
			// combine the string with the extension
			String qualifiedName = formatNameQualifier(name.trim(), extension.trim());
			// now add to the list
			if ( strval.length() > 0 ) {
				if ( map.get(qualifiedName) == null ) {
					map.put(qualifiedName, new ArrayList<String>());
				}
				map.get(qualifiedName).add(strval);
			}
		}
		return map;
	}

	@Override
	public String getSignature() {
		return "fn:dynamic(name: Value, value: Value[, suffix: Value]) : DynNodeList";
	}

	@Override
	public String getDescription() {
		return "dynamically specify solr index field based on name qualifier and values, optionally provide name suffix extension";
	}

	@Override
	public String getLocalName() {
		return "dynamic";
	}

	private String formatNameQualifier(String ...inputs) {
		String input = String.join(" ", inputs);
		input = CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_UNDERSCORE, input);
		input = input.replaceAll("[^a-zA-Z0-9_ ]", "");
		input = input.trim().replaceAll(" ", "_").toUpperCase();
		return CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.LOWER_CAMEL, input);
	}
}
