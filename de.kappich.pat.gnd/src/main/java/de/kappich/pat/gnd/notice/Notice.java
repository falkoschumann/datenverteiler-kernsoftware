/*
 * Copyright 2011 by Kappich Systemberatung Aachen
 * 
 * This file is part of de.kappich.pat.gnd.
 * 
 * de.kappich.pat.gnd is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * 
 * de.kappich.pat.gnd is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with de.kappich.pat.gnd; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 */

package de.kappich.pat.gnd.notice;

/**
 * @author Kappich Systemberatung
 * @version $Revision: 9139 $
 */
public class Notice {

	private final String _message;

	private final long _creationTime;

	private final long _changeTime;

	public Notice(String message){
		this(message, System.currentTimeMillis());
	}

	public Notice(final String message, final long creationTime) {
		this(message, creationTime,  creationTime);
	}

	public Notice(final String message, final long creationTime, final long changeTime) {
		_message = message;
		_creationTime = creationTime;
		_changeTime = changeTime;
	}

	public String getMessage() {
		return _message;
	}

	public long getCreationTime() {
		return _creationTime;
	}

	public long getChangeTime() {
		return _changeTime;
	}

	@Override
	public boolean equals(final Object o) {
		if(this == o) return true;
		if(o == null || getClass() != o.getClass()) return false;

		final Notice notice = (Notice)o;

		if(_changeTime != notice._changeTime) return false;
		if(_creationTime != notice._creationTime) return false;
		if(_message != null ? !_message.equals(notice._message) : notice._message != null) return false;

		return true;
	}

	@Override
	public int hashCode() {
		int result = _message != null ? _message.hashCode() : 0;
		result = 31 * result + (int)(_creationTime ^ (_creationTime >>> 32));
		result = 31 * result + (int)(_changeTime ^ (_changeTime >>> 32));
		return result;
	}
}
