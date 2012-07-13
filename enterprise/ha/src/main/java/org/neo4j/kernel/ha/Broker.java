/**
 * Copyright (c) 2002-2012 "Neo Technology,"
 * Network Engine for Objects in Lund AB [http://neotechnology.com]
 *
 * This file is part of Neo4j.
 *
 * Neo4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.neo4j.kernel.ha;

import org.neo4j.helpers.Pair;
import org.neo4j.kernel.GraphDatabaseAPI;
import org.neo4j.kernel.KernelData;
import org.neo4j.kernel.ha.zookeeper.Machine;
import org.neo4j.kernel.impl.nioneo.store.StoreId;
import org.neo4j.kernel.impl.util.StringLogger;

public interface Broker
{
    Pair<Master, Machine> getMaster();

    Pair<Master, Machine> getMasterReally( boolean allowChange );

    Machine getMasterExceptMyself();

    void setLastCommittedTxId( long txId );

    boolean iAmMaster();

    int getMyMachineId();

    // I know... this isn't supposed to be here
    Object instantiateMasterServer( GraphDatabaseAPI graphDb );
    
    Object instantiateSlaveServer( GraphDatabaseAPI graphDb, SlaveDatabaseOperations ops );
    
    Slave[] getSlaves();

    void rebindMaster();

    void notifyMasterChange( Machine newMaster );

    void shutdown();

    void restart();

    void start();

    void setConnectionInformation( KernelData kernel );

    ConnectionInformation getConnectionInformation( int machineId );

    ConnectionInformation[] getConnectionInformation();

    /**
     * Asks the cluster for and returns the storeId currently shared by the
     * cluster members or a new one if none is there.
     *
     * @param firstTime False if it should timeout after a session interval
     *            passes -
     *            expected to be true in the same scenarios where
     *            {@link #bootstrap()} is called instead of
     *            {@link #getMasterReally(boolean)}
     * @return The storeId existing in the cluster or a new one if none is found
     */
    StoreId getClusterStoreId( boolean firstTime );

    void logStatus( StringLogger msgLog );

    Pair<Master, Machine> bootstrap();
}
