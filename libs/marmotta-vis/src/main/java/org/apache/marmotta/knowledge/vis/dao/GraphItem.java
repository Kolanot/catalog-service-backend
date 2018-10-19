package org.apache.marmotta.knowledge.vis.dao;

public class GraphItem extends BaseItem {
	
	public Integer getNetworkCount() {
		return getCount();
	}

    public String getGraph() {
        return super.getId();
    }
	    
}
