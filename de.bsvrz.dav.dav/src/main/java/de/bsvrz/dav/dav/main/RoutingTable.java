/*
 * Copyright 2010 by Kappich Systemberatung, Aachen
 * Copyright 2007 by Kappich Systemberatung, Aachen
 * Copyright 2004 by Kappich+Kniß Systemberatung, Aachen
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

package de.bsvrz.dav.dav.main;

import de.bsvrz.dav.daf.communication.lowLevel.telegrams.RoutingUpdate;
import de.bsvrz.dav.daf.communication.lowLevel.telegrams.TransmitterBestWayUpdate;

import java.util.*;

/**
 * Die Klasse ist für die Weginformationsverwaltung zuständig. Sie stellt die Tabelle der Weginformationen dar.
 *
 * @author Kappich Systemberatung
 * @version $Revision: 11561 $
 */
public class RoutingTable {

	/** zu einem Knoten gehörige Reihe aus der Tabelle */
	private final List<NodeInfo> _rowToNodeInfo;

	/** zu einem Knoten gehörige Spalte aus der Tabelle */
	private final List<NodeInfo> _columnToNodeInfo;

	/** Id des Knotens */
	private final Map<Long, NodeInfo> _idToNodeInfo;

	/** Tabelle der Weginformationen */
	private final RoutingInfoMatrix _routingInfos;

	/** Anzahl der Spalten */
	private int _columns = 0;

	/** Anzahl der Reihen */
	private int _rows = 0;

	/** Die über Änderungen zu benachrichtigende Komponente */
	private final DistributionInterface _distribution;

	/** ID des lokalen Datenverteilers */
	private long _localNodeId = 0;

	/**
	 * Erzeugt ein neues Objekt mit den gegebenen Parametern.
	 *
	 * @param distribution Die über Änderungen zu benachrichtigende Komponente.
	 * @param localNodeId  ID des lokalen Datenverteilers
	 */
	public RoutingTable(DistributionInterface distribution, long localNodeId) {
		_distribution = distribution;
		_localNodeId = localNodeId;
		_idToNodeInfo = new HashMap<Long, NodeInfo>();
		_rowToNodeInfo = new ArrayList<NodeInfo>();
		_columnToNodeInfo = new ArrayList<NodeInfo>();
		_routingInfos = new RoutingInfoMatrix();
	}

	/**
	 * Diese Methode wird von der Wegverwaltung aufgerufen. Gibt es zur angegebenen Ziel-ID (Knoten) einen Eintrag, wird die Verbindunginformation zurückgegeben,
	 * über die dieses Ziel am Besten erreicht werden kann.
	 *
	 * @param destinationNodeId ID des Zielknotens
	 *
	 * @return Routing-Repräsentant der Datenverteilerverbindung, oder <code>null</code>, falls es keine Verbindung zum Zielknoten gibt.
	 */
	public RoutingConnectionInterface findBestConnection(long destinationNodeId) {
		NodeInfo nodeInfo = _idToNodeInfo.get(new Long(destinationNodeId));
		if((nodeInfo == null) || (nodeInfo.getBestRoutingNode() == null)) {
			return null;
		}
		return nodeInfo.getBestRoutingNode().getDirectConnection();
	}

	/**
	 * Diese Methode wird von der Wegverwaltung aufgerufen und es wird überprüft, ob ein Eintrag mit dem spezifizierten Repräsentanten der Datenverteilerverbindung
	 * vorhanden ist. Ist dies nicht der Fall, so wird ein neuer Eintrag erzeugt und in die Tabelle aufgenommen. Wenn der Eintrag vorhanden ist und dessen
	 * Repräsentant der Datenverteilerverbindung nicht vorhanden ist, wird dieser Eintrag durch den als übergebenen Repräsentanten aktualisiert. Hat sich durch
	 * diese Änderung der beste Weg zu einem Datenverteiler geändert, so wird das {@link DistributionInterface} darüber benachrichtigt. Haben sich die Gewichte der
	 * einzelnen Wege über einen bestimmten Datenverteiler geändert, so werden die Änderungen in ein {@link TransmitterBestWayUpdate}-Telegramm verpackt und über
	 * die Datenverteiler gesendet.
	 *
	 * @param connection Repräsentant der Datenverteilerverbindung
	 */
	public void addConnection(RoutingConnectionInterface connection) {
		Long remoteNodeId = new Long(connection.getRemoteNodeId());
		NodeInfo remoteNode = (NodeInfo)_idToNodeInfo.get(remoteNodeId);
		if(remoteNode == null) {
			remoteNode = new NodeInfo(remoteNodeId.longValue());
			_idToNodeInfo.put(remoteNodeId, remoteNode);
		}
		if(remoteNode.getDirectConnection() != null) {
			return;
		}
		remoteNode.setDirectConnection(connection);
		if(remoteNode.getRow() < 0) {
			remoteNode.setRow(_rows++);
			_rowToNodeInfo.add(remoteNode);
		}
		if(remoteNode.getColumn() < 0) {
			remoteNode.setColumn(_columns++);
			_columnToNodeInfo.add(remoteNode);
		}
		RoutingInfo routingInfo = new RoutingInfo(connection.getThroughputResistance());
		_routingInfos.init(remoteNode.getRow(), remoteNode.getColumn(), routingInfo);
		updateAll();
	}

	/**
	 * Diese Methode wird für automatisierte Tests benötigt und prüft, ob ein <code>RoutingConnectionInterface</code> Objekt in die entsprechenden
	 * Datenstruktur eingetragen wurde.
	 *
	 * @param connection Verbindung, die in der Datenstruktur <code>_idToNodeInfo</code> eingetragen sein muss.
	 * @return <code>true</code>, wenn das Objekte <code>connection</code> in der oben genannten Datenstruktur eingetragen ist.
	 */
	boolean containsIdToNodeInfo(RoutingConnectionInterface connection){
		return _idToNodeInfo.containsKey(new Long(connection.getRemoteNodeId()));
	}

	/**
	 * Diese Methode wird von der Wegverwaltung aufgerufen und es wird überprüft, ob ein Eintrag mit dem spezifizierten Repräsentanten der Datenverteilerverbindung
	 * connection vorhanden ist. Ist dies der Fall, so wird dessen Repräsentant der Datenverteilerverbindung gelöscht. Alle Weginformationen über die nicht mehr
	 * vorhandene Verbindung werden zurückgesetzt. Hat sich durch diese Änderung der beste Weg zu einem Datenverteiler geändert, so wird {@link
	 * DistributionInterface} darüber benachrichtigt. Haben sich die Gewichte der einzelnen Wege über einen bestimmten Datenverteiler geändert, so werden die
	 * Änderungen in ein {@link TransmitterBestWayUpdate}-Telegramm verpackt und über die Protokollsteuerung DaV-DaV zu diesem Datenverteiler gesendet.
	 *
	 * @param connection Repräsentant der Datenverteilerverbindung
	 */
	public void removeConnection(RoutingConnectionInterface connection) {
		Long remoteNodeId = new Long(connection.getRemoteNodeId());
		NodeInfo remoteNode = _idToNodeInfo.get(remoteNodeId);
		if((remoteNode == null) || (remoteNode.getDirectConnection() == null)) {
			return;
		}
		remoteNode.setDirectConnection(null);
		int row = remoteNode.getRow();
		for(int column = 0; column < _columns; ++column) {
			RoutingInfo routingInfo = _routingInfos.get(row, column);
			routingInfo.setRoutingInfo(-1, null);
			routingInfo.setBestRestRoutingInfo(-1, null);
		}
		updateAll();
	}

	/**
	 * Diese Methode wird von der Wegverwaltung aufgerufen und es wird überprüft, ob ein Eintrag mit der als Parameter spezifizierten <code>fromNodeId</code>
	 * vorhanden ist. Wenn der Eintrag vorhanden ist, werden die Gewichte der Wege zwischen dem aktuellen Datenverteiler und den Datenverteilern, die in
	 * RoutingUpdate spezifiziert sind, aktualisiert. Dabei muß der Datenverteiler mit der ID <code>fromNodeId</code> auf diesem Weg liegen. Die Aktualisierung
	 * erfolgt durch das Addieren des Gewichts der Verbindung zwischen aktuellem Datenverteiler und <code>fromNodeId</code> und die jeweiligen
	 * RoutingUpdate-Gewichte. Hat sich durch diese Änderung der beste Weg zu einem Datenverteiler geändert, so wird {@link DistributionInterface} darüber
	 * benachrichtigt. Haben sich die Gewichte der einzelnen Wege zu einem bestimmten Datenverteiler geändert, so werden die Änderungen in ein {@link
	 * de.bsvrz.dav.daf.communication.lowLevel.telegrams.TransmitterBestWayUpdate}-Telegramm verpackt und über die Protokollsteuerung DaV-DaV zu den Nachbardatenverteilern gesendet.
	 *
	 * @param fromNodeId     ID eines Eintrags in routingTable
	 * @param routingUpdates Gewichte der Wege zwischen dem aktuellen Datenverteiler und den Datenverteilern
	 */
	public void update(long fromNodeId, RoutingUpdate[] routingUpdates) {
		Long remoteNodeId = new Long(fromNodeId);
		NodeInfo remoteNode = (NodeInfo)_idToNodeInfo.get(remoteNodeId);
		if((remoteNode == null) || (remoteNode.getDirectConnection() == null)) {
			return;
		}

		int row = remoteNode.getRow();
		for(int nodeIndex = 0; nodeIndex < routingUpdates.length; ++nodeIndex) {
			RoutingUpdate routingUpdate = routingUpdates[nodeIndex];
			Long destinationNodeId = new Long(routingUpdate.getTransmitterId());
			NodeInfo destinationNode = _idToNodeInfo.get(destinationNodeId);
			if(destinationNode == null) {
				destinationNode = new NodeInfo(destinationNodeId.longValue());
				destinationNode.setColumn(_columns++);
				_columnToNodeInfo.add(destinationNode);
				_routingInfos.init(destinationNode.getColumn());
				_idToNodeInfo.put(destinationNodeId, destinationNode);
			}
			int throughputResistance = routingUpdate.getThroughputResistance();
			if(throughputResistance >= 0) {
				throughputResistance += remoteNode.getDirectConnection().getThroughputResistance();
			}
			long[] involvedTransmitterIds = routingUpdate.getInvolvedTransmitterIds();
			int visitedNodesCount = 0;
			if(involvedTransmitterIds != null) {
				visitedNodesCount = involvedTransmitterIds.length;
			}
			long[] visitedNodes = new long[visitedNodesCount + 1];
			for(int vi = 0; vi < visitedNodesCount; ++vi) {
				visitedNodes[vi] = involvedTransmitterIds[vi];
			}
			visitedNodes[visitedNodesCount] = fromNodeId;
			RoutingInfo routingInfo = _routingInfos.get(row, destinationNode.getColumn());
			if(routingInfo != null) {
				routingInfo.setRoutingInfo(throughputResistance, visitedNodes);
			}
			updateColumn(destinationNode);
		}

		for(row = 0; row < _rows; ++row) {
			NodeInfo destinationNode = _rowToNodeInfo.get(row);
			if(destinationNode != null) {
				updateRow(destinationNode);
			}
		}
	}

	/**
	 * Diese Methode schreibt alle Ziele, die von dem Ausgangsknoten(Ausgangs-DAV) zu erreichen sind in einen String, und gibt diesen zurück.
	 *
	 * @return Matrix der erreichbaren Ziele in einem String
	 */
	public final String toString() {
		final boolean showAll = false;
		StringBuffer result = new StringBuffer();
		result.append("[").append(_localNodeId).append("]");

		for(int column = 0; column < _columns; ++column) {
			NodeInfo destinationNode = _columnToNodeInfo.get(column);
			if(!showAll && (destinationNode.getBestRoutingNode() == null)) {
				continue;
			}
			result.append("	(").append(destinationNode.getNodeId()).append(")");
		}
		result.append("\n");
		for(int row = 0; row < _rows; ++row) {
			NodeInfo routingNode = _rowToNodeInfo.get(row);
			if(!showAll && (routingNode.getDirectConnection() == null)) {
				continue;
			}
			result.append("(").append(routingNode.getNodeId()).append("):");
			for(int column = 0; column < _columns; ++column) {
				if(!showAll && _columnToNodeInfo.get(column).getBestRoutingNode() == null) {
					continue;
				}
				RoutingInfo routingInfo = _routingInfos.get(row, column);
				long[] visitedNodes = routingInfo.getVisitedNodes();
				String hops = "-";
				int hopCount = 0;
				if(visitedNodes != null) {
					hopCount = visitedNodes.length;
					hops = String.valueOf(hopCount);
				}
				result.append(routingInfo.getThroughputResistance()).append("/").append(routingInfo.getBestRestThroughputResistance());
				result.append("	");
			}
			result.append("\n");
		}
		for(int column = 0; column < _columns; ++column) {
			NodeInfo destinationNode = _columnToNodeInfo.get(column);
			NodeInfo bestRoutingNode = destinationNode.getBestRoutingNode();
			if(bestRoutingNode == null) {
				if(showAll) {
					result.append("	-");
				}
			}
			else {
				RoutingInfo routingInfo = _routingInfos.get(bestRoutingNode.getRow(), column);
				result.append("	").append(routingInfo.getThroughputResistance());
			}
		}
		result.append("\n");
		for(int column = 0; column < _columns; ++column) {
			NodeInfo destinationNode = _columnToNodeInfo.get(column);
			NodeInfo bestRoutingNode = destinationNode.getBestRoutingNode();
			if(bestRoutingNode == null) {
				if(showAll) {
					result.append("	(-)");
				}
			}
			else {
				result.append("	(").append(bestRoutingNode.getNodeId()).append(")");
			}
		}
		return result.toString();
	}
	/**
	 * Diese Methode schreibt alle Ziele, die von dem Ausgangsknoten(Ausgangs-DAV) zu erreichen sind in einen String, und gibt diesen zurück.
	 *
	 * @return Matrix der erreichbaren Ziele in einem String
	 */
	public final String toString(boolean showAll) {
		StringBuffer result = new StringBuffer();
		result.append(String.format("[%20d]",_localNodeId));

		for(int column = 0; column < _columns; ++column) {
			NodeInfo destinationNode = _columnToNodeInfo.get(column);
			if(!showAll && (destinationNode.getBestRoutingNode() == null)) {
				continue;
			}
			result.append(String.format("\t(%20d)", destinationNode.getNodeId()));
		}
		result.append("\n");
		for(int row = 0; row < _rows; ++row) {
			NodeInfo routingNode = _rowToNodeInfo.get(row);
			if(!showAll && (routingNode.getDirectConnection() == null)) {
				continue;
			}
			result.append(String.format("(%20d)", routingNode.getNodeId()));
			for(int column = 0; column < _columns; ++column) {
				if(!showAll && _columnToNodeInfo.get(column).getBestRoutingNode() == null) {
					continue;
				}
				RoutingInfo routingInfo = _routingInfos.get(row, column);
				long[] visitedNodes = routingInfo.getVisitedNodes();
				String hops = "-";
				int hopCount = 0;
				if(visitedNodes != null) {
					hopCount = visitedNodes.length;
					hops = String.valueOf(hopCount);
				}
				final String properties = String.format( "R:%d, BR:%d", routingInfo.getThroughputResistance(), routingInfo.getBestRestThroughputResistance());
				result.append(String.format("\t  %20s", properties));
			}
			result.append("\n");
		}
		result.append("bestResistance:       ");
		for(int column = 0; column < _columns; ++column) {
			NodeInfo destinationNode = _columnToNodeInfo.get(column);
			NodeInfo bestRoutingNode = destinationNode.getBestRoutingNode();
			if(bestRoutingNode == null) {
				if(showAll) {
					result.append("\t(--------------------)");
				}
			}
			else {
				RoutingInfo routingInfo = _routingInfos.get(bestRoutingNode.getRow(), column);
				result.append(String.format("\t(%20d)", routingInfo.getThroughputResistance()));
			}
		}
		result.append("\n");
		result.append("bestRoutingNode:      ");
		for(int column = 0; column < _columns; ++column) {
			NodeInfo destinationNode = _columnToNodeInfo.get(column);
			NodeInfo bestRoutingNode = destinationNode.getBestRoutingNode();
			if(bestRoutingNode == null) {
				if(showAll) {
					result.append("\t(--------------------)");
				}
			}
			else {
				result.append(String.format("\t(%20d)", bestRoutingNode.getNodeId()));
			}
		}
		return result.toString();
	}

	/** Aktualisiert die komplette Tabelle. */
	private void updateAll() {
		for(int column = 0; column < _columns; ++column) {
			NodeInfo destinationNode = _columnToNodeInfo.get(column);
			if(destinationNode != null) {
				updateColumn(destinationNode);
			}
		}
		for(int row = 0; row < _rows; ++row) {
			NodeInfo routingNode = _rowToNodeInfo.get(row);
			if(routingNode != null) {
				updateRow(routingNode);
			}
		}
	}

	/**
	 * Aktualisiert die Spalte zu einem Zielknoten.
	 *
	 * @param destinationNode Zielknoten
	 */
	private void updateColumn(NodeInfo destinationNode) {
		int column = destinationNode.getColumn();
		int bestNeighborRow = -1;
		int bestNeighborVal = Integer.MAX_VALUE;

		for(int row = 0; row < _rows; ++row) {
			RoutingInfo neighborInfo = _routingInfos.get(row, column);
			long[] bestValVisitedNodes = null;
			int bestVal = Integer.MAX_VALUE;


			for(int testRow = 0; testRow < _rows; ++testRow) {
				if(testRow == row) {
					continue;
				}
				RoutingInfo testInfo = _routingInfos.get(testRow, column);
				int testVal = testInfo.getThroughputResistance();
				if(testVal == -1) {
					testVal = Integer.MAX_VALUE;
				}
				if(testVal < bestVal) {
					boolean testValHasVisitedNeighbor = false;
					long[] visitedNodes = testInfo.getVisitedNodes();
					if(visitedNodes != null) {
						long neighborId = _rowToNodeInfo.get(row).getNodeId();
						for(int vi = 0; vi < visitedNodes.length; ++vi) {
							if(neighborId == visitedNodes[vi]) {
								testValHasVisitedNeighbor = true;
								break;
							}
						}
						if(testValHasVisitedNeighbor) {
							continue;
						}
					}
					bestValVisitedNodes = visitedNodes;
					bestVal = testVal;
				}
			}
			if(bestVal == Integer.MAX_VALUE) {
				bestVal = -1;
			}
			neighborInfo.setBestRestRoutingInfo(bestVal, bestValVisitedNodes);

			int neighborVal = neighborInfo.getThroughputResistance();
			if(neighborVal == -1) {
				neighborVal = Integer.MAX_VALUE;
			}
			if(neighborVal < bestNeighborVal) {
				bestNeighborRow = row;
				bestNeighborVal = neighborVal;
			}
		}

		if(bestNeighborVal == Integer.MAX_VALUE) {
			bestNeighborVal = -1;
		}
		if(destinationNode.getBestRoutingRow() != bestNeighborRow) {
			RoutingConnectionInterface oldBestConnection = null;
			if(destinationNode.getBestRoutingNode() != null) {
				oldBestConnection = destinationNode.getBestRoutingNode().getDirectConnection();
			}

			destinationNode.setBestRoutingRow(bestNeighborRow);
			if(bestNeighborRow < 0) {
				destinationNode.setBestRoutingNode(null);
			}
			else {
				destinationNode.setBestRoutingNode(_rowToNodeInfo.get(bestNeighborRow));
			}
			RoutingConnectionInterface newBestConnection = null;
			if(destinationNode.getBestRoutingNode() != null) {
				newBestConnection = destinationNode.getBestRoutingNode().getDirectConnection();
			}
			_distribution.updateDestinationRoute(destinationNode.getNodeId(), oldBestConnection, newBestConnection);
		}
	}

	/**
	 * Aktualisiert die Zeile zu einem Zwischenknoten.
	 *
	 * @param routingNode Zwischenknoten
	 */
	private void updateRow(NodeInfo routingNode) {
		if(routingNode.getDirectConnection() == null) {
			return;
		}
		List<NodeInfo> list = new ArrayList<NodeInfo>();
		for(int column = 0; column < _columns; ++column) {
			NodeInfo destinationNode = _columnToNodeInfo.get(column);
			if(destinationNode == routingNode) {
				continue;
			}
			RoutingInfo routingInfo = _routingInfos.get(routingNode.getRow(), column);
			if(routingInfo != null) {
				if(routingInfo.isChanged()) {
					list.add(destinationNode);
				}
			}
		}
		int size = list.size();
		if(size == 0) {
			return;
		}

		RoutingUpdate[] routingUpdates = new RoutingUpdate[size];
		for(int i = 0; i < size; ++i) {
			NodeInfo nodeInfo = (NodeInfo)list.get(i);
			if(nodeInfo != null) {
				RoutingInfo routingInfo = _routingInfos.get(routingNode.getRow(), nodeInfo.getColumn());
				routingUpdates[i] = new RoutingUpdate(
						nodeInfo.getNodeId(), (short)routingInfo.getBestRestThroughputResistance(), routingInfo.getBestRestVisitedNodes()
				);
			}
		}
		routingNode.getDirectConnection().sendRoutingUpdate(routingUpdates);
	}

	public void dumpRoutingTable() {
		System.out.println("RoutingTable.dumpRoutingTable############################################################Anfang");
		System.out.println("_localNodeId = " + _localNodeId);
		System.out.println("_rows = " + _rows);
		System.out.println("_columns = " + _columns);
		System.out.println("matrix:\n" + this.toString(true));
		System.out.println("RoutingTable.dumpRoutingTable############################################################Ende");
}

	/**
	 * Repräsentiert einen Knoten. Ist der Knoten ein Zwischenknoten, so repräsentiert diese Klasse ein Zeile. Ist der Knoten ein zielknoten, so repräsentiert
	 * diese Klasse eine Spalte.
	 */
	class NodeInfo {

		/** Id des Knotens */
		long _nodeId;

		/** Zeile */
		int _row;

		/** Spalte */
		int _column;

		/**
		 *
		 */
		RoutingConnectionInterface _directConnection;

		int _bestRoutingRow;

		NodeInfo _bestRoutingNode;

		/**
		 * Erzeugt ein neues Objekt mit den gegebenen Parametern.
		 *
		 * @param nodeId Id des Knotens
		 */
		NodeInfo(long nodeId) {
			_nodeId = nodeId;
			_row = -1;
			_column = -1;
			_directConnection = null;
			_bestRoutingRow = -1;
			_bestRoutingNode = null;
		}

		public long getNodeId() {
			return _nodeId;
		}

		public int getRow() {
			return _row;
		}

		public void setRow(final int row) {
			_row = row;
		}

		public int getColumn() {
			return _column;
		}

		public void setColumn(final int column) {
			_column = column;
		}

		public NodeInfo getBestRoutingNode() {
			return _bestRoutingNode;
		}

		public void setBestRoutingNode(final NodeInfo bestRoutingNode) {
			_bestRoutingNode = bestRoutingNode;
		}

		public int getBestRoutingRow() {
			return _bestRoutingRow;
		}

		public void setBestRoutingRow(final int bestRoutingRow) {
			_bestRoutingRow = bestRoutingRow;
		}

		public RoutingConnectionInterface getDirectConnection() {
			return _directConnection;
		}

		public void setDirectConnection(final RoutingConnectionInterface directConnection) {
			_directConnection = directConnection;
		}
	}

	/** Repräsentiert eine Zelle in der Tabelle */
	class RoutingInfo {

		private int _throughputResistance = -1;

		private int _bestRestThroughputResistance = -1;

		private boolean changed = false;

		private long[] _visitedNodes = null;

		private long[] _bestRestVisitedNodes = null;

		RoutingInfo() {
		}

		RoutingInfo(int throughputResistance) {
			_throughputResistance = throughputResistance;
		}

		final void setRoutingInfo(int throughputResistance, long[] visitedNodes) {
			_throughputResistance = throughputResistance;
			_visitedNodes = visitedNodes;
		}

		final int getThroughputResistance() {
			return _throughputResistance;
		}

		final long[] getVisitedNodes() {
			return _visitedNodes;
		}

		final void setBestRestRoutingInfo(int bestRestThroughputResistance, long[] bestRestVisitedNodes) {
			if((_bestRestThroughputResistance != bestRestThroughputResistance) || !Arrays.equals(bestRestVisitedNodes, _bestRestVisitedNodes)) {
				_bestRestThroughputResistance = bestRestThroughputResistance;
				_bestRestVisitedNodes = null;
				if(bestRestVisitedNodes != null) {
					_bestRestVisitedNodes = (long[])(bestRestVisitedNodes.clone());
				}
				changed = true;
			}
		}

		final int getBestRestThroughputResistance() {
			return _bestRestThroughputResistance;
		}

		final long[] getBestRestVisitedNodes() {
			return _bestRestVisitedNodes;
		}

		final boolean isChanged() {
			if(changed) {
				changed = false;
				return true;
			}
			return false;
		}
	}

	/** Die Tabelle der Weginformationen */
	class RoutingInfoMatrix {

		private final int ROW_INCREASE = 10;

		private final int COLUMN_INCREASE = 10;

		private int _rows = 0;

		private int _columns = 0;

		private RoutingInfo[][] _routingInfoMatrix = null;

		RoutingInfoMatrix() {
		}

		final synchronized void init(int initRow, int initColumn, RoutingInfo routingInfo) {
			if((initRow >= _rows) || (initColumn >= _columns)) {
				int newRows = _rows;
				int newColumns = _columns;
				if(initRow >= _rows) {
					newRows += ROW_INCREASE;
				}
				if(initColumn >= _columns) {
					newColumns += COLUMN_INCREASE;
				}
				RoutingInfo[][] newRoutingInfoMatrix = new RoutingInfo[newRows][newColumns];
				for(int row = 0; row < newRows; ++row) {
					for(int column = 0; column < newColumns; ++column) {
						if((row < _rows) && (column < _columns)) {
							newRoutingInfoMatrix[row][column] = _routingInfoMatrix[row][column];
						}
						else if((row != initRow) || (column != initColumn)) {
							newRoutingInfoMatrix[row][column] = new RoutingInfo();
						}
					}
				}
				_routingInfoMatrix = newRoutingInfoMatrix;
				_rows = newRows;
				_columns = newColumns;
			}
			if(initRow != -1) {
				_routingInfoMatrix[initRow][initColumn] = routingInfo;
			}
		}

		final synchronized void init(int initColumn) {
			init(-1, initColumn, null);
		}

		final synchronized RoutingInfo get(int row, int column) {
			return _routingInfoMatrix[row][column];
		}

		final synchronized int getColumnCount() {
			return _columns;
		}

		final synchronized int getRowCount() {
			return _rows;
		}

		public final synchronized String toString() {
			StringBuffer result = new StringBuffer();
			for(int row = 0; row < _rows; ++row) {
				for(int column = 0; column < _columns; ++column) {
					result.append(_routingInfoMatrix[row][column].getThroughputResistance());
					result.append(":");
					result.append(_routingInfoMatrix[row][column].getBestRestThroughputResistance());
					result.append(", ");
				}
				result.append("\n");
			}
			return result.toString();
		}
	}
}
