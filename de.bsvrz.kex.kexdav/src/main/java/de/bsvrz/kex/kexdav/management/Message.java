/*
 * Copyright 2011 by Kappich Systemberatung Aachen
 * 
 * This file is part of de.bsvrz.kex.kexdav.
 * 
 * de.bsvrz.kex.kexdav is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * 
 * de.bsvrz.kex.kexdav is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with de.bsvrz.kex.kexdav; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 */

package de.bsvrz.kex.kexdav.management;

/**
 * Benachrichtigung an die KExDaV-Verwaltung. Wird je nach den installierten Observern z.B. auf der Debug-Ausgabe oder über Betriebsmeldungen verschickt
 *
 * @author Kappich Systemberatung
 * @version $Revision: 9269 $
 */
public class Message {

	private final Throwable _throwable;

	private final String _description;

	private final ErrorLevel _errorLevel;

	private Message(final String description, final Throwable throwable, final ErrorLevel errorLevel) {
		_description = description;
		_throwable = throwable;
		_errorLevel = errorLevel;
	}

	/**
	 * Neue Informations-Meldung mit geringer Dringlichkeit. Wird nicht als Betriebsmeldung verschickt
	 *
	 * @param description Nachricht als String
	 *
	 * @return Benachrichtigung (Z.B. weiterzugeben an das {@link ManagerInterface})
	 */
	public static Message newInfo(final String description) {
		return new Message(description, null, Message.ErrorLevel.INFO);
	}

	/**
	 * Neue Informations-Meldung mit geringer Dringlichkeit. Wird nicht als Betriebsmeldung verschickt
	 *
	 * @param description Nachricht als String
	 * @param throwable   Exception. Der Stacktrace kann von den Observern abgefragt werden und die Fehlernachricht wird der description angehängt.
	 *
	 * @return Benachrichtigung (Z.B. weiterzugeben an das {@link ManagerInterface})
	 */
	public static Message newInfo(final String description, final Throwable throwable) {
		return new Message(description + ": " + throwable.getMessage(), new Exception(throwable), ErrorLevel.INFO);
	}

	/**
	 * Neue Warnung mit geringer Dringlichkeit. Wird nicht als Betriebsmeldung verschickt
	 *
	 * @param description Nachricht als String
	 *
	 * @return Benachrichtigung (Z.B. weiterzugeben an das {@link ManagerInterface})
	 */
	public static Message newMinor(final String description) {
		return new Message(description, null, Message.ErrorLevel.MINOR);
	}

	/**
	 * Neue Warnung mit geringer Dringlichkeit. Wird nicht als Betriebsmeldung verschickt
	 *
	 * @param description Nachricht als String
	 * @param throwable   Exception. Der Stacktrace kann von den Observern abgefragt werden und die Fehlernachricht wird der description angehängt.
	 *
	 * @return Benachrichtigung (Z.B. weiterzugeben an das {@link ManagerInterface})
	 */
	public static Message newMinor(final String description, final Throwable throwable) {
		return new Message(description + ": " + throwable.getMessage(), new Exception(throwable), ErrorLevel.MINOR);
	}

	/**
	 * Neue Warnung mit hoher Dringlichkeit. Wird auch als Betriebsmeldung verschickt
	 *
	 * @param description Nachricht als String
	 *
	 * @return Benachrichtigung (Z.B. weiterzugeben an das {@link ManagerInterface})
	 */
	public static Message newMajor(final String description) {
		return new Message(description, null, Message.ErrorLevel.MAJOR);
	}

	/**
	 * Neue Warnung mit hoher Dringlichkeit. Wird auch als Betriebsmeldung verschickt
	 *
	 * @param description Nachricht als String
	 * @param throwable   Exception. Der Stacktrace kann von den Observern abgefragt werden und die Fehlernachricht wird der description angehängt.
	 *
	 * @return Benachrichtigung (Z.B. weiterzugeben an das {@link ManagerInterface})
	 */
	public static Message newMajor(final String description, final Throwable throwable) {
		return new Message(description + ": " + throwable.getMessage(), new Exception(throwable), Message.ErrorLevel.MAJOR);
	}

	/**
	 * Neue Warnung mit hoher Dringlichkeit. Wird auch als Betriebsmeldung verschickt
	 *
	 * @param throwable Exception. Der Stacktrace kann von den Observern abgefragt werden und die Fehlernachricht wird als Beschreibung benutzt.
	 *
	 * @return Benachrichtigung (Z.B. weiterzugeben an das {@link ManagerInterface})
	 */
	public static Message newMajor(final Throwable throwable) {
		return new Message(throwable.getMessage(), new Exception(throwable), Message.ErrorLevel.MAJOR);
	}

	/**
	 * Neue Fehlermeldung mit hoher Dringlichkeit. Wird auch als Betriebsmeldung verschickt
	 *
	 * @param description Nachricht als String
	 *
	 * @return Benachrichtigung (Z.B. weiterzugeben an das {@link ManagerInterface})
	 */
	public static Message newError(final String description) {
		return new Message(description, null, Message.ErrorLevel.ERROR);
	}

	/**
	 * Neue Fehlermeldung mit hoher Dringlichkeit. Wird auch als Betriebsmeldung verschickt
	 *
	 * @param description Nachricht als String
	 * @param throwable   Exception. Der Stacktrace kann von den Observern abgefragt werden und die Fehlernachricht wird der description angehängt.
	 *
	 * @return Benachrichtigung (Z.B. weiterzugeben an das {@link ManagerInterface})
	 */
	public static Message newError(final String description, final Throwable throwable) {
		return new Message(description + ": " + throwable.getMessage(), new Exception(throwable), Message.ErrorLevel.ERROR);
	}

	/**
	 * Neue Fehlermeldung mit hoher Dringlichkeit. Wird auch als Betriebsmeldung verschickt
	 *
	 * @param throwable Exception. Der Stacktrace kann von den Observern abgefragt werden und die Fehlernachricht wird als beschreibung benutzt.
	 *
	 * @return Benachrichtigung (Z.B. weiterzugeben an das {@link ManagerInterface})
	 */
	public static Message newError(final Throwable throwable) {
		return new Message(throwable.getMessage(), new Exception(throwable), Message.ErrorLevel.ERROR);
	}

	/**
	 * Gibt die Fehlerbeschreibung zurück
	 *
	 * @return Fehlerbeschreibung
	 */
	public String getDescription() {
		return _description;
	}

	/**
	 * Gibt den Stacktrace zurück
	 *
	 * @return den Stacktrace oder null falls keine Exception vorliegt
	 */
	public String getStackTrace() {
		if(_throwable == null) return null;
		return throwAbleToString(_throwable);
	}

	/**
	 * Gibt die Exception zurück
	 *
	 * @return Exception
	 */
	public Throwable getException() {
		return _throwable;
	}

	/**
	 * Gibt die Dringlichkeit zurück
	 *
	 * @return die Dringlichkeit
	 */
	public ErrorLevel getErrorLevel() {
		return _errorLevel;
	}

	@Override
	public String toString() {
		return _errorLevel.toString() + ": " + _description;
	}

	/**
	 * Gibt ein Throwable als String zurück
	 *
	 * @param throwable Throwable
	 *
	 * @return String
	 */
	private static String throwAbleToString(final Throwable throwable) {
		final StringBuilder builder = new StringBuilder();
		builder.append(throwable.getCause().getMessage()).append('\n');
		final StackTraceElement[] trace = throwable.getStackTrace();
		for(int i = 1; i < trace.length; i++) {
			builder.append("\tat ").append(trace[i]).append('\n');
		}

		final Throwable ourCause = throwable.getCause();
		if(ourCause != null) printStackTraceAsCause(ourCause, builder, trace);

		return builder.toString();
	}

	private static void printStackTraceAsCause(final Throwable throwable, final StringBuilder builder, final StackTraceElement[] causedTrace) {
		final StackTraceElement[] trace = throwable.getStackTrace();
		int m = trace.length - 1, n = causedTrace.length - 1;
		while(m >= 0 && n >= 0 && trace[m].equals(causedTrace[n])) {
			m--;
			n--;
		}
		final int framesInCommon = trace.length - 1 - m;

		builder.append("Caused by: ").append(throwable).append('\n');
		for(int i = 0; i <= m; i++) {
			builder.append("\tat ").append(trace[i]).append('\n');
		}
		if(framesInCommon != 0) builder.append("\t... ").append(framesInCommon).append(" more").append('\n');

		final Throwable ourCause = throwable.getCause();
		if(ourCause != null) printStackTraceAsCause(ourCause, builder, trace);
	}



	/** Dringlichkeit einer Warnung */
	public enum ErrorLevel {
		/** Einfache Meldung über den Programmzustand */
		INFO,
		/** Geringfügige Warnung oder Hinweis, keine Betriebsmeldung notwendig */
		MINOR,
		/** Wichtige Warnung oder ein (voraussichtlich) vorübergehendes Problem, Betriebsmeldung notwendig */
		MAJOR,
		/** Fehler oder anhaltendes, schweres Problem, z.B. Parameterierungsfehler, Fehler beim Verbindungsaufbau u.ä. */
		ERROR;

		/**
		 * Ist die Warnung unwichtig? Sie wird dann z.B. nicht über die Betriebsmeldung verschickt.
		 *
		 * @return True wenn geringfügig.
		 */
		public boolean isMinor() {
			return ordinal() < MAJOR.ordinal();
		}

		@Override
		public String toString() {
			switch(this) {
				case INFO:
					return "Info";
				case MINOR:
					return "Warnung";
				case MAJOR:
					return "WARNUNG";
				case ERROR:
					return "FEHLER";
			}
			throw new IllegalStateException();
		}
	}
}
