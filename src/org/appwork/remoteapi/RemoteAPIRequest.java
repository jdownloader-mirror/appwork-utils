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

/**
 * @author daniel
 * 
 */
public class RemoteAPIRequest {

    private final InterfaceHandler<?> iface;
    private final String              methodName;

    public RemoteAPIRequest(final InterfaceHandler<?> iface, final String methodName) {
        this.iface = iface;
        this.methodName = methodName;
    }

    public InterfaceHandler<?> getIface() {
        return this.iface;
    }

    public String getMethodName() {
        return this.methodName;
    }

}
