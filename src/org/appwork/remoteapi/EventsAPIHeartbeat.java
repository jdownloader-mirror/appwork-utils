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
public class EventsAPIHeartbeat extends RemoteAPIException {

    /**
     * 
     */
    private static final long serialVersionUID = 9088648283338861592L;

    public EventsAPIHeartbeat() {
        super(new Throwable());
    }

    @Override
    public HashMap<String, Object> getRemoteAPIExceptionResponse() {
        final HashMap<String, Object> ret = new HashMap<String, Object>();
        ret.put("type", "system");
        ret.put("message", "heartbeart");
        return ret;
    }
}