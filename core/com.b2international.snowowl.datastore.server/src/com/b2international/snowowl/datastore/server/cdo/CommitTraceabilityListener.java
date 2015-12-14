package com.b2international.snowowl.datastore.server.cdo;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface CommitTraceabilityListener {

	void transactionCommitted(String branchPath, String commitComment, Map<String, List<String>> newObjectCdoIds, Map<String, List<String>> updatedObjectCdoIds, Set<String> detachedObjectCdoIds);

}
