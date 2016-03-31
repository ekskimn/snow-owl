/**
 * Logback: the reliable, generic, fast and flexible logging framework.
 * Copyright (C) 1999-2011, QOS.ch. All rights reserved.
 *
 * This program and the accompanying materials are dual-licensed under
 * either the terms of the Eclipse Public License v1.0 as published by
 * the Eclipse Foundation
 *
 *   or (per the licensee's choosing)
 *
 * under the terms of the GNU Lesser General Public License version 2.1
 * as published by the Free Software Foundation.
 */
package com.b2international.snowowl.logging.amq;

import java.io.Serializable;
import java.util.Objects;

import ch.qos.logback.classic.spi.ILoggingEvent;

/**
 * A read-only and serializable summary of {@link ILoggingEvent} that only includes the event's message.
 */
public class MessageVO implements Serializable {

	private static final long serialVersionUID = 2833757990703910898L;

	private String message;

	public static MessageVO build(ILoggingEvent le) {
		MessageVO vo = new MessageVO();
		vo.message = le.getMessage();
		return vo;
	}

	public String getMessage() {
		return message;
	}

	@Override
	public int hashCode() {
		return Objects.hash(message);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;

		final MessageVO other = (MessageVO) obj;
		return Objects.equals(message, other.message);
	}
}
