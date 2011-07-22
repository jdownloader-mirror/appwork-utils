/**
 * Copyright (c) 2009 - 2011 AppWork UG(haftungsbeschränkt) <e-mail@appwork.org>
 * 
 * This file is part of org.appwork.remoteapi
 * 
 * This software is licensed under the Artistic License 2.0,
 * see the LICENSE file or http://www.opensource.org/licenses/artistic-license-2.0.php
 * for details
 */
package org.appwork.remoteapi;

import java.util.HashMap;

/**
 * @author daniel
 * 
 */
public class RemoteAPIUnauthorizedException extends RemoteAPIException {
    /**
     * 
     */
    private static final long serialVersionUID = 2895933316406807059L;

    public RemoteAPIUnauthorizedException() {
        super(new Throwable());
    }

    @Override
    public HashMap<String, Object> getRemoteAPIExceptionResponse() {
        final HashMap<String, Object> ret = new HashMap<String, Object>();
        ret.put("type", "system");
        ret.put("message", "error");
        final HashMap<String, Object> content = new HashMap<String, Object>();
        content.put("type", "unauthorized");
        ret.put("data", content);
        return ret;
    }
}
