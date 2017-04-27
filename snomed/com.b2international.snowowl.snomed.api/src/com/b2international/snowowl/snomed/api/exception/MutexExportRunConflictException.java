package com.b2international.snowowl.snomed.api.exception;

import com.b2international.snowowl.core.exceptions.ConflictException;

/**
 * Thrown when an export request can not be processed because an export process is already executing on the system. The client
 * has to send the request again, after the previous export has finished.
 * @since 5.0.9 
 */
public class MutexExportRunConflictException extends ConflictException {

	private static final long serialVersionUID = 1L;

	public MutexExportRunConflictException(String message) {
		super(message);
	}
}
