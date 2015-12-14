package traceability;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.b2international.snowowl.core.ApplicationContext;
import com.b2international.snowowl.datastore.server.cdo.CommitTraceabilityListener;
import com.b2international.snowowl.datastore.server.domain.ComponentRef;
import com.b2international.snowowl.snomed.datastore.SnomedTerminologyBrowser;

public class SnomedCommitTraceabilityListener implements CommitTraceabilityListener {

	@Override
	public void transactionCommitted(String branchPath,
			String commitComment,
			Map<String, List<String>> newObjectCdoIds,
			Map<String, List<String>> updatedObjectCdoIds,
			Set<String> detachedObjectCdoIds) {
		
		final SnomedTerminologyBrowser terminologyBrowser = ApplicationContext.getInstance().getService(SnomedTerminologyBrowser.class);
//		terminologyBrowser.getConcept(new ComponentRef("SNOMEDCT", branchPath, ))
	}
	
}
