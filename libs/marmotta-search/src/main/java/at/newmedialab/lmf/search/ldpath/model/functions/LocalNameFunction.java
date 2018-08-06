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

import org.apache.marmotta.kiwi.model.rdf.KiWiStringLiteral;
import org.apache.marmotta.kiwi.model.rdf.KiWiUriResource;
import org.apache.marmotta.ldpath.api.backend.RDFBackend;
import org.apache.marmotta.ldpath.api.functions.SelectorFunction;
import org.openrdf.model.Value;

public class LocalNameFunction extends SelectorFunction<Value> {

	@Override
	public Collection<Value> apply(RDFBackend<Value> backend, Value context, Collection<Value>... args)
			throws IllegalArgumentException {
		// if (args.length < 1 && args.length > 4) {
		// throw new IllegalArgumentException("wrong usage: " + getSignature());
		// }
		
		if ( args.length != 1 ) {
			throw new IllegalArgumentException("wrong usage: " + getSignature());
		}
		Collection<Value> values = new ArrayList<Value>();
		for (Value node : args[0]) {
		    if ( node instanceof KiWiUriResource) {
		        KiWiUriResource res = (KiWiUriResource) node;
		        values.add(new KiWiStringLiteral(res.getLocalName()));
		    }

		}
		return values;
	}

	@Override
	public String getSignature() {
		return "fn:localName(uri: Value) : StringLiteral";
	}

	@Override
	public String getDescription() {
		return "extract the localName from a provided uri - stripping the namespace";
	}

	@Override
	public String getLocalName() {
		return "localName";
	}
}
