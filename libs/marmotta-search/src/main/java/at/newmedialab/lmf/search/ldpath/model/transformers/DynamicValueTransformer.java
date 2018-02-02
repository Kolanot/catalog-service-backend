package at.newmedialab.lmf.search.ldpath.model.transformers;

import java.util.Map;

import org.apache.marmotta.ldpath.api.backend.RDFBackend;
import org.apache.marmotta.ldpath.api.transformers.NodeTransformer;
import org.openrdf.model.Value;

import at.newmedialab.lmf.search.ldpath.model.DynKiwiLiteral;
import at.newmedialab.lmf.search.ldpath.model.DynamicValue;

public class DynamicValueTransformer implements NodeTransformer<DynamicValue, Value> {

	@Override
	public DynamicValue transform(RDFBackend<Value> backend, Value node, Map<String, String> configuration)
			throws IllegalArgumentException {
		
		if ( node instanceof DynKiwiLiteral) {
			DynKiwiLiteral lit = (DynKiwiLiteral)node;
			String name = getFieldName(lit.getName(), configuration);
			try {
				// 
				switch (getFieldType(configuration)) {
				//case "string": // string is default
				case "int":
					return new DynamicValue(name, lit.intValue());
				case "integer":
					return new DynamicValue(name, lit.integerValue());
				case "date":
					return new DynamicValue(name, lit.calendarValue());
				case "float":
					return new DynamicValue(name, lit.floatValue());
				case "double":
					return new DynamicValue(name, lit.doubleValue());
				default:
					return new DynamicValue(name, lit.stringValue());
				}
				
			} catch (Exception e) {
				return null;
			}
		}
		else {
			
			return new DynamicValue(getFieldName("_", configuration), node.stringValue());
		}
	}
	private String getFieldType(Map<String, String> configuration) {
		String type = "string";
		if ( configuration.containsKey("solrType")) {
			type = configuration.get("solrType");
		}
		return type;
	}
	/**
	 * Helper for obtaining the field name to use when indexing 
	 * @param name
	 * @param configuration
	 * @return
	 */
	private String getFieldName(String name, Map<String, String> configuration) {
		String template = "*_s"; // use string as default template if not set
		// when config present
		if ( configuration !=null ) {
			// when solr-type set but no field name
			if ( ! configuration.containsKey("dynamicField") ) {
				if ( configuration.containsKey("solrType")) {
					String solrType = configuration.get("solrType");
					template = "*_"+solrType.substring(0, 1).toLowerCase();
				}
			}
			else {
				template = configuration.get("dynamicField");
			}
			// use default
		}
		return template.replace("*", name);
	}
}
