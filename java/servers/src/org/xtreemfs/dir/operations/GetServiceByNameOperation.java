/*
 * Copyright (c) 2009-2011 by Bjoern Kolbeck,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.dir.operations;

import java.util.Map.Entry;

import org.xtreemfs.babudb.api.database.Database;
import org.xtreemfs.babudb.api.database.ResultSet;
import org.xtreemfs.common.stage.BabuDBComponent;
import org.xtreemfs.common.stage.BabuDBPostprocessing;
import org.xtreemfs.common.stage.BabuDBComponent.BabuDBRequest;
import org.xtreemfs.common.stage.RPCRequestCallback;
import org.xtreemfs.dir.DIRRequest;
import org.xtreemfs.dir.DIRRequestDispatcher;
import org.xtreemfs.dir.data.ServiceRecord;
import org.xtreemfs.foundation.buffer.ReusableBuffer;
import org.xtreemfs.pbrpc.generatedinterfaces.DIRServiceConstants;
import org.xtreemfs.pbrpc.generatedinterfaces.DIR.ServiceSet;
import org.xtreemfs.pbrpc.generatedinterfaces.DIR.serviceGetByNameRequest;

import com.google.protobuf.Message;

/**
 * 
 * @author bjko
 */
public class GetServiceByNameOperation extends DIROperation {
    
    private final BabuDBComponent component;
    private final Database database;
    
    public GetServiceByNameOperation(DIRRequestDispatcher master) {
        super(master);
        component = master.getBabuDBComponent();
        database = master.getDirDatabase();
    }
    
    @Override
    public int getProcedureId() {
        return DIRServiceConstants.PROC_ID_XTREEMFS_SERVICE_GET_BY_NAME;
    }
    
    @Override
    public void startRequest(DIRRequest rq, RPCRequestCallback callback) throws Exception {
        
        final serviceGetByNameRequest request = (serviceGetByNameRequest) rq.getRequestMessage();
        
        component.prefixLookup(database, callback, DIRRequestDispatcher.INDEX_ID_SERVREG, new byte[0], rq.getMetadata(), 
                new BabuDBPostprocessing<ResultSet<byte[], byte[]>>() {
            
            @Override
            public Message execute(ResultSet<byte[], byte[]> result, BabuDBRequest rq) 
                    throws Exception {
                
                ServiceSet.Builder services = ServiceSet.newBuilder();
                long now = System.currentTimeMillis() / 1000l;
                
                while (result.hasNext()) {
                    Entry<byte[], byte[]> e = result.next();
                    ServiceRecord servEntry = new ServiceRecord(ReusableBuffer.wrap(e.getValue()));
                    if (servEntry.getName().equals(request.getName()))
                        services.addServices(servEntry.getService());
                    
                    long secondsSinceLastUpdate = now - servEntry.getLast_updated_s();
                    servEntry.getData().put("seconds_since_last_update",
                        Long.toString(secondsSinceLastUpdate));
                }
                return services.build();
            }
        });
    }
}
