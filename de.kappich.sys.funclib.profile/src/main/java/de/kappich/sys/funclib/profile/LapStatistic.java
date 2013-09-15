/*
 * Copyright 2007 by Kappich Systemberatung, Aachen
 * Copyright 2004 by Kappich+Kniß Systemberatung Aachen (K2S)
 * 
 * This file is part of de.kappich.sys.funclib.profile.
 * 
 * de.kappich.sys.funclib.profile is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * 
 * de.kappich.sys.funclib.profile is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with de.kappich.sys.funclib.profile; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 */

package de.kappich.sys.funclib.profile;

/**
 * Messung und Ausgabe von Zeit- und Speicherverbrauch.  
 * @author Kappich Systemberatung
 * @version $Revision: 5009 $
 */
public class LapStatistic {
	private final int _alignNumberColumn;
	private final int _alignNameColumn;
	private final int _alignLapTimeColumn;
	private final int _alignLapMemColumn;
	private final int _alignTotalMemColumn;

	public LapStatistic() {
		this(-10, -20, 13, 17, 17);
	}

	public LapStatistic(int alignNumberColumn, int alignNameColumn, int alignLapTimeColumn, int alignLapMemColumn, int alignTotalMemColumn) {
		_alignNumberColumn = alignNumberColumn;
		_alignNameColumn = alignNameColumn;
		_alignLapTimeColumn = alignLapTimeColumn;
		_alignLapMemColumn = alignLapMemColumn;
		_alignTotalMemColumn = alignTotalMemColumn;
		getLapResult();
	}

	public Result getLapResult() {
		long oldStartTime = _startTime;
		long oldStartMem = _startMem;
		_startTime = System.currentTimeMillis();
		_startMem = getUsedMemory();
		return new Result(_lapCounter++, _startTime - oldStartTime, _startMem - oldStartMem, _startMem);
	}

	public void printLapResult(String title) {
		Result result = getLapResult();
		System.out.println(printColumns(result, title));
	}

	public void printLapResultWithGc(String title) {
		Result result = getLapResult();
		System.gc();
		Result gcResult = getLapResult();
		printColumns(result, title);
		printColumns(gcResult, "gc");
		long bothTime = result.getLapTime() + gcResult.getLapTime();
		long bothMem = result.getLapMem() + gcResult.getLapMem();
		System.out.println(printColumns(result.getLapCounter() + "+" + gcResult.getLapCounter(), title + "+gc", bothTime, bothMem, gcResult.getTotalMem()));
	}

	private String printColumns(Result result, String title) {
		return printColumns(String.valueOf(result._lapCounter), title, result._lapTime, result._lapMem, result._totalMem);
	}

	private String printColumns(String number, String name, long lapTime, long lapMem, long totalMem) {
		String result = "";
		result += alignString("(" + number + "):", _alignNumberColumn);
		result += alignString(name + ":", _alignNameColumn);
		result += alignString(String.valueOf(lapTime) + " ms,", _alignLapTimeColumn);
		result += alignString((lapMem > 0 ? "+" : "") + String.valueOf(lapMem) + " Bytes,", _alignLapMemColumn);
		result += " Gesamt";
		result += alignString(String.valueOf(totalMem) + " Bytes", _alignTotalMemColumn);

		return result;
	}

	/**
	 * Der Rückgabewert entspricht der Ausgabe der Methode {@link #printLapResult(String)}.
	 *
	 * @param title Titel der Ausgabezeile
	 * @return Ergebnis der Messung
	 */
	public String toString(String title) {
		Result result = getLapResult();
		return printColumns(result, title);
	}

	/**
	 * Der Rückgabewert entspricht der Ausgabe der Methode {@link #printLapResultWithGc(String)}.
	 *
	 * @param title Titel der Ausgabezeile
	 * @return Ergebnis der Messung
	 */
	public String toStringWithGc(String title) {
		Result result = getLapResult();
		System.gc();
		Result gcResult = getLapResult();
		long bothTime = result.getLapTime() + gcResult.getLapTime();
		long bothMem = result.getLapMem() + gcResult.getLapMem();
		return printColumns(String.valueOf(result._lapCounter), title + "+gc", bothTime, bothMem, gcResult.getTotalMem());
	}

	private String alignString(final String text, final int alignment) {
		final boolean left = alignment < 0;
		final int width = Math.abs(alignment);
		final int textLength = text.length();
		final int missingLength = width - textLength;
		if (missingLength > 0) {
			StringBuffer alignedText = new StringBuffer();
			if (left) alignedText.append(text);
			for (int i = 0; i < missingLength; ++i) {
				alignedText.append(' ');
			}
			if (!left) alignedText.append(text);
			return alignedText.toString();
		} else {
			return text.substring(0, width);
		}
	}

	private static long getUsedMemory() {
		return _runtime.totalMemory() - _runtime.freeMemory();
	}

	public static final class Result {
		private final long _lapCounter;
		private final long _lapTime;
		private final long _lapMem;
		private long _totalMem;

		public Result(long lapCounter, long lapTime, long lapMem, long totalMem) {
			_lapCounter = lapCounter;
			_lapTime = lapTime;
			_lapMem = lapMem;
			_totalMem = totalMem;
		}

		public long getLapCounter() {
			return _lapCounter;
		}

		public long getLapTime() {
			return _lapTime;
		}

		public long getLapMem() {
			return _lapMem;
		}

		public long getTotalMem() {
			return _totalMem;
		}
	}

	private int _lapCounter;
	private long _startTime;
	private long _startMem;
	static final Runtime _runtime = Runtime.getRuntime();
}
