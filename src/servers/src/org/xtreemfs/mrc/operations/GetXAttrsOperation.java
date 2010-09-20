/*  Copyright (c) 2008 Konrad-Zuse-Zentrum fuer Informationstechnik Berlin.

 This file is part of XtreemFS. XtreemFS is part of XtreemOS, a Linux-based
 Grid Operating System, see <http://www.xtreemos.eu> for more details.
 The XtreemOS project has been developed with the financial support of the
 European Commission's IST program under contract #FP6-033576.

 XtreemFS is free software: you can redistribute it and/or modify it under
 the terms of the GNU General Public License as published by the Free
 Software Foundation, either version 2 of the License, or (at your option)
 any later version.

 XtreemFS is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with XtreemFS. If not, see <http://www.gnu.org/licenses/>.
 */
/*
 * AUTHORS: Jan Stender (ZIB)
 */

package org.xtreemfs.mrc.operations;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import org.xtreemfs.interfaces.StringSet;
import org.xtreemfs.interfaces.MRCInterface.listxattrRequest;
import org.xtreemfs.interfaces.MRCInterface.listxattrResponse;
import org.xtreemfs.mrc.MRCRequest;
import org.xtreemfs.mrc.MRCRequestDispatcher;
import org.xtreemfs.mrc.ac.FileAccessManager;
import org.xtreemfs.mrc.database.StorageManager;
import org.xtreemfs.mrc.database.VolumeManager;
import org.xtreemfs.mrc.metadata.FileMetadata;
import org.xtreemfs.mrc.metadata.XAttr;
import org.xtreemfs.mrc.utils.MRCHelper;
import org.xtreemfs.mrc.utils.Path;
import org.xtreemfs.mrc.utils.PathResolver;
import org.xtreemfs.mrc.utils.MRCHelper.SysAttrs;

/**
 * 
 * @author stender
 */
public class GetXAttrsOperation extends MRCOperation {
    
    public GetXAttrsOperation(MRCRequestDispatcher master) {
        super(master);
    }
    
    @Override
    public void startRequest(MRCRequest rq) throws Throwable {
        
        final listxattrRequest rqArgs = (listxattrRequest) rq.getRequestArgs();
        
        final VolumeManager vMan = master.getVolumeManager();
        final FileAccessManager faMan = master.getFileAccessManager();
        
        validateContext(rq);
        
        final Path p = new Path(rqArgs.getPath());
        
        final StorageManager sMan = vMan.getStorageManagerByName(p.getComp(0));
        final PathResolver res = new PathResolver(sMan, p);
        
        // check whether the path prefix is searchable
        faMan.checkSearchPermission(sMan, res, rq.getDetails().userId, rq.getDetails().superUser, rq
                .getDetails().groupIds);
        
        // check whether file exists
        res.checkIfFileDoesNotExist();
        
        // retrieve and prepare the metadata to return
        FileMetadata file = res.getFile();
        
        HashSet<String> attrNames = new HashSet<String>();
        
        Iterator<XAttr> myAttrs = sMan.getXAttrs(file.getId(), rq.getDetails().userId);
        Iterator<XAttr> globalAttrs = sMan.getXAttrs(file.getId(), StorageManager.GLOBAL_ID);
        
        // include global attributes
        while (globalAttrs.hasNext())
            attrNames.add(globalAttrs.next().getKey());
        
        // include individual user attributes
        while (myAttrs.hasNext())
            attrNames.add(myAttrs.next().getKey());
        
        // include system attributes
        for (SysAttrs attr : SysAttrs.values()) {
            String key = "xtreemfs." + attr.toString();
            Object value = MRCHelper.getSysAttrValue(master.getConfig(), sMan, master.getOSDStatusManager(),
                master.getFileAccessManager(), res.toString(), file, attr.toString());
            if (!value.equals(""))
                attrNames.add(key);
        }
        
        // include policy attributes
        List<String> policyAttrNames = MRCHelper.getPolicyAttrNames(sMan, file.getId());
        for (String attr : policyAttrNames)
            attrNames.add(attr);
        
        StringSet names = new StringSet();
        Iterator<String> it = attrNames.iterator();
        while (it.hasNext())
            names.add(it.next());
        
        // set the response
        rq.setResponse(new listxattrResponse(names));
        finishRequest(rq);
        
    }
}
