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

package de.kappich.pat.gnd.viewManagement;

import de.kappich.pat.gnd.layerManagement.Layer;
import de.kappich.pat.gnd.needlePlugin.DOTNeedle;

/**
 * @author Kappich Systemberatung
 * @version $Revision: 9145 $
 */
public class NoticeViewEntry extends ViewEntry {

	private final ViewEntry _viewEntry;

	private NoticeViewEntry(final ViewEntry viewEntry, Layer layer, int zoomIn, int zoomOut, boolean visible) {
		super(layer, zoomIn, zoomOut, false, visible);
		_viewEntry = viewEntry;
	}

	public static NoticeViewEntry create(final ViewEntry viewEntry) {
		return new NoticeViewEntry(
				viewEntry, makeNoticeLayer(viewEntry.getLayer().getGeoReferenceType()), viewEntry.getZoomIn(), viewEntry.getZoomOut(), viewEntry.isVisible()
		);
	}

	@Override
	public boolean isVisible() {
		return _viewEntry.isVisible();
	}

	@Override
	public boolean isVisible(final int scale) {
		return _viewEntry.isVisible(scale);
	}

	private static Layer makeNoticeLayer(final String geoReferenceType) {
		Layer noticeLayer = new Layer("Notizen", "", geoReferenceType);
		DOTNeedle type = new DOTNeedle("Notiz", "Stellt Notizen dar.");
		noticeLayer.addDisplayObjectType(type, Integer.MAX_VALUE, 1);
		return noticeLayer;
	}
}
