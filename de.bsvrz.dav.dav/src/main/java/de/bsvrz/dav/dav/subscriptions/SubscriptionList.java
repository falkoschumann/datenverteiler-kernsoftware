/*
 * Copyright 2013 by Kappich Systemberatung Aachen
 * 
 * This file is part of de.bsvrz.dav.dav.
 * 
 * de.bsvrz.dav.dav is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * 
 * de.bsvrz.dav.dav is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with de.bsvrz.dav.dav; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 */

package de.bsvrz.dav.dav.subscriptions;

import de.bsvrz.sys.funclib.debug.Debug;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * @author Kappich Systemberatung
 * @version $Revision: 0000 $
 */
public class SubscriptionList {

	private static final Debug _debug = Debug.getLogger();

	@Override
	public String toString() {
		return "SubscriptionList{" +
		       "_sendingSubscriptions=" + _sendingSubscriptions +
		       ", _receivingSubscriptions=" + _receivingSubscriptions +
		       ", _drain=" + _drain +
		       ", _source=" + _source +
		       ", _dataIndexSubscriptionTime=" + _dataIndexSubscriptionTime +
		       '}';
	}

	private final Set<SendingSubscription> _sendingSubscriptions = new CopyOnWriteArraySet<SendingSubscription>();

	private final Set<ReceivingSubscription> _receivingSubscriptions = new CopyOnWriteArraySet<ReceivingSubscription>();

	private ReceivingSubscription _drain = null;

	private SendingSubscription _source = null;

	private long _dataIndexSubscriptionTime = 0;

	public boolean hasSource() {
		return getSource() != null;
	}

	public boolean hasDrain() {
		return getDrain() != null;
	}

	public ReceivingSubscription getDrain() {
		return _drain;
	}

	public SendingSubscription getSource() {
		return _source;
	}

	public void setDrain(final ReceivingSubscription drain) {
		if(drain != null && !_receivingSubscriptions.contains(drain)) throw new IllegalArgumentException("Setze Senke, die nicht angemeldet ist.");
		if(drain != null && _source != null) throw new IllegalArgumentException("Kann nicht Quelle und Senke gleichzeitig setzen.");
		if(_drain == drain) return;
		_drain = drain;
		if(drain != null){
			_dataIndexSubscriptionTime = (System.currentTimeMillis() / 1000);
		}
	}

	public void setSource(final SendingSubscription source) {
		if(source != null && !_sendingSubscriptions.contains(source)) throw new IllegalArgumentException("Setze Quelle, die nicht angemeldet ist.");
		if(source != null && _drain != null) throw new IllegalArgumentException("Kann nicht Quelle und Senke gleichzeitig setzen.");
		if(_source == source) return;
		_source = source;
		if(source != null){
			_dataIndexSubscriptionTime = (System.currentTimeMillis() / 1000);
		}
	}

	public void addReceiver(ReceivingSubscription receivingSubscription){
		if(_receivingSubscriptions.contains(receivingSubscription)) throw new IllegalArgumentException("Bereits angemeldet");
		_receivingSubscriptions.add(receivingSubscription);
	}

	public void addSender(SendingSubscription sendingSubscription){
		if(_sendingSubscriptions.contains(sendingSubscription)) throw new IllegalArgumentException("Bereits angemeldet");
		_sendingSubscriptions.add(sendingSubscription);
	}

	public void removeReceiver(ReceivingSubscription receivingSubscription){
		if(_drain == receivingSubscription) _drain = null;
		if(!_receivingSubscriptions.remove(receivingSubscription)){
			_debug.warning("Melde Empfänger ab, der nicht angemeldet ist", receivingSubscription);
		}
	}

	public void removeSender(SendingSubscription sendingSubscription){
		if(_source == sendingSubscription) _source = null;
		if(!_sendingSubscriptions.remove(sendingSubscription)) {
			_debug.warning("Melde Sender ab, der nicht angemeldet ist", sendingSubscription);
		}
	}

	public boolean canSetSource(final SendingSubscription sendingSubscription) {
		return !hasDrainOrSource() || getSource() == sendingSubscription;
	}

	public boolean canSetDrain(final ReceivingSubscription receivingSubscription) {
		return !hasDrainOrSource() || getDrain() == receivingSubscription;
	}

	public boolean hasDrainOrSource() {
		return hasDrain() || hasSource();
	}

	public Collection<SendingSubscription> getSendingSubscriptions() {
		return Collections.unmodifiableSet(_sendingSubscriptions);
	}

	public Collection<ReceivingSubscription> getReceivingSubscriptions() {
		return Collections.unmodifiableSet(_receivingSubscriptions);
	}

	public boolean isEmpty() {
		return _receivingSubscriptions.isEmpty() && _sendingSubscriptions.isEmpty();
	}

	public boolean isCentralDistributor() {
		return _source instanceof LocalSubscription || _drain instanceof LocalSubscription;
	}

	public long getDataIndex(final long runningNumber) {
		if(!isCentralDistributor()) return 0;

		// laufende Nummer geht von 1 bis  0x3ffffff, das sind 0x3fffffe verschiedene Werte
		long dataIndexRunningNumber = (runningNumber % 0x3fffffe) + 1;
		long dataIndexRunningNumberOverflow = runningNumber / 0x3fffffe;
		return (((_dataIndexSubscriptionTime + dataIndexRunningNumberOverflow) & 0xffffffffL) << 32) ^ (dataIndexRunningNumber << 2);
	}

	public long getCentralDistributorId() {
		if(_source != null){
			return _source.getCentralDistributorId();
		}
		if(_drain != null){
			return _drain.getCentralDistributorId();
		}
		return -1;
	}
}
