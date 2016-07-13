package com.b2international.snowowl.snomed.api.impl.domain.browser;

import java.util.Date;
import java.util.UUID;

import com.b2international.snowowl.snomed.api.domain.browser.ISnomedBrowserBulkChangeRun;
import com.b2international.snowowl.snomed.api.domain.browser.SnomedBrowserBulkChangeStatus;

public class SnomedBrowserBulkChangeRun implements ISnomedBrowserBulkChangeRun {

	private String id;
	private Date startDate;
	private Date endDate;
	private SnomedBrowserBulkChangeStatus status;

	public SnomedBrowserBulkChangeRun() {
		this.id = UUID.randomUUID().toString();
	}
	
	public void start() {
		this.status = SnomedBrowserBulkChangeStatus.RUNNING;
		this.startDate = new Date();
	}
	
	public void end(SnomedBrowserBulkChangeStatus status) {
		this.status = status;
		this.endDate = new Date();
	}
	
	public String getId() {
		return id;
	}
	
	public SnomedBrowserBulkChangeStatus getStatus() {
		return status;
	}
	
	public void setStatus(SnomedBrowserBulkChangeStatus status) {
		this.status = status;
	}
	
	public Date getStartDate() {
		return startDate;
	}
	
	public Date getEndDate() {
		return endDate;
	}
}
