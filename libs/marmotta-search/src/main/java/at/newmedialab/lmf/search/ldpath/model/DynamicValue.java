package at.newmedialab.lmf.search.ldpath.model;

public class DynamicValue {
	final String name;
	final Object value;
	public DynamicValue(String name, Object value) {
		this.name = name;
		this.value = value;
	}
	public String toString() {
		return this.value.toString();
	}
	public String fieldName() {
		return this.name;
	}
//	public String fieldName(String nameTemplate) {
//		return nameTemplate.replace("*", name);
//	}
	

}
