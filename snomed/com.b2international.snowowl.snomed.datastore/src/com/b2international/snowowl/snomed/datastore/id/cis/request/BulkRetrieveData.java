package com.b2international.snowowl.snomed.datastore.id.cis.request;

import java.util.ArrayList;
import java.util.Collection;

import org.eclipse.xtext.util.Strings;

public class BulkRetrieveData extends RequestData {

	private Collection<String> sctids;
	
	public BulkRetrieveData(Collection<String> sctids) {
		this.sctids = sctids;
	}
	
	public String getSctids() {
		return Strings.concat(",", new ArrayList<String>(sctids));
	}

}
