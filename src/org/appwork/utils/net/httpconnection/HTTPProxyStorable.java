/**
 * Copyright (c) 2009 - 2011 AppWork UG(haftungsbeschränkt) <e-mail@appwork.org>
 * 
 * This file is part of org.appwork.utils.net.httpconnection
 * 
 * This software is licensed under the Artistic License 2.0,
 * see the LICENSE file or http://www.opensource.org/licenses/artistic-license-2.0.php
 * for details
 */
package org.appwork.utils.net.httpconnection;

import org.appwork.storage.Storable;

/**
 * @author daniel
 * 
 */
public class HTTPProxyStorable implements Storable {

    public static enum TYPE {
        NONE,
        DIRECT,
        SOCKS4,
        SOCKS5,
        HTTP
    }

    private String  username                   = null;
    private String  password                   = null;
    private int     port                       = -1;
    private String  address                    = null;
    private TYPE    type                       = null;
    private boolean useConnectMethod           = false;
    private boolean preferNativeImplementation = false;

    public HTTPProxyStorable(/* storable */) {
    }

    public String getAddress() {
        return this.address;
    }

    public String getPassword() {
        return this.password;
    }

    public int getPort() {
        return this.port;
    }

    public TYPE getType() {
        return this.type;
    }

    public String getUsername() {
        return this.username;
    }

    public boolean isConnectMethodPrefered() {
        return this.useConnectMethod;
    }

    public boolean isPreferNativeImplementation() {
        return this.preferNativeImplementation;
    }

    public void setAddress(final String address) {
        this.address = address;
    }

    public void setConnectMethodPrefered(final boolean value) {
        this.useConnectMethod = value;
    }

    public void setPassword(final String password) {
        this.password = password;
    }

    public void setPort(final int port) {
        this.port = port;
    }

    public void setPreferNativeImplementation(final boolean preferNativeImplementation) {
        this.preferNativeImplementation = preferNativeImplementation;
    }

    public void setType(final TYPE type) {
        this.type = type;
    }

    public void setUsername(final String username) {
        this.username = username;
    }
}
