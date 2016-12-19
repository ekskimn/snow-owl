package com.b2international.snowowl.snomed.datastore.id.cis.request;

import java.util.Collection;

public class BulkRetrieveData extends RequestData {

	private Collection<String> sctids;
	
	public BulkRetrieveData(Collection<String> sctids) {
		this.sctids = sctids;
	}
	
	public Collection<String> getSctids() {
		return sctids;
	}

}
