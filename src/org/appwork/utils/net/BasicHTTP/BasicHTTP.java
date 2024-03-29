package org.appwork.utils.net.BasicHTTP;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;

import org.appwork.net.protocol.http.HTTPConstants;
import org.appwork.txtresource.TranslationFactory;
import org.appwork.utils.Application;
import org.appwork.utils.logging2.LogInterface;
import org.appwork.utils.net.DownloadProgress;
import org.appwork.utils.net.UploadProgress;
import org.appwork.utils.net.httpconnection.HTTPConnection;
import org.appwork.utils.net.httpconnection.HTTPConnection.RequestMethod;
import org.appwork.utils.net.httpconnection.HTTPConnectionFactory;
import org.appwork.utils.net.httpconnection.HTTPProxy;

public class BasicHTTP {

    public static void main(final String[] args) throws MalformedURLException, IOException, InterruptedException {

        final BasicHTTP client = new BasicHTTP();
        System.out.println(client.getPage(new URL("http://ipcheck0.jdownloader.org")));
    }

    private HashSet<Integer>              allowedResponseCodes;

    private final HashMap<String, String> requestHeader;

    protected HTTPConnection              connection;

    private int                           connectTimeout = 15000;

    private int                           readTimeout    = 30000;

    private HTTPProxy                     proxy          = HTTPProxy.NONE;
    protected LogInterface                logger         = null;

    private final Object                  lock           = new Object();

    public BasicHTTP() {
        this.requestHeader = new HashMap<String, String>();
    }

    /**
     * @throws IOException
     * 
     */
    protected void checkResponseCode() throws InvalidResponseCode {
        if (this.allowedResponseCodes != null && !this.allowedResponseCodes.contains(this.connection.getResponseCode())) {

        throw this.createInvalidResponseCodeException(); }
    }

    public void clearRequestHeader() {
        this.requestHeader.clear();
    }

    /**
     * @return
     */
    protected InvalidResponseCode createInvalidResponseCodeException() {
        return new InvalidResponseCode(this.connection);
    }

    /**
     * @param url
     * @param progress
     * @param file
     * @throws InterruptedException
     * @throws IOException
     */
    public void download(final URL url, final DownloadProgress progress, final File file) throws BasicHTTPException, InterruptedException {
        FileOutputStream fos = null;
        try {

            fos = new FileOutputStream(file, true);
            try {
                this.download(url, progress, 0, fos, file.length());
            } catch (final BasicHTTPException e) {
                throw e;
            } catch (final InterruptedException e) {
                throw e;
            } catch (final Exception e) {
                // we cannot say if read or write
                throw new BasicHTTPException(this.connection, e);

            }
        } catch (final FileNotFoundException e) {
            throw new BasicHTTPException(this.connection, new WriteIOException(e));
        } finally {
            try {
                fos.close();
            } catch (final Throwable t) {
            }
        }
    }

    public byte[] download(final URL url, final DownloadProgress progress, final long maxSize) throws BasicHTTPException, InterruptedException {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {

            this.download(url, progress, maxSize, baos, -1);
            // if(getProxy()==null||getProxy().equals(HTTPProxy.NONE)) {
            // throw new IOException("Direct is pfui!");
            // }
        } catch (final BasicHTTPException e) {
            throw e;
        } catch (final InterruptedException e) {
            throw e;
        } catch (final Exception e) {
            if (baos.size() > 0) {
                throw new BasicHTTPException(this.connection, new ReadIOException(e));
            } else {
                throw new BasicHTTPException(this.connection, new WriteIOException(e));
            }
        } finally {
            try {
                baos.close();
            } catch (final Throwable t) {
            }
        }
        return baos.toByteArray();
    }

    /**
     * 
     * Please do not forget to close the output stream.
     * 
     * @param url
     * @param progress
     * @param maxSize
     * @param baos
     * @throws IOException
     * @throws InterruptedException
     */
    public void download(final URL url, final DownloadProgress progress, final long maxSize, final OutputStream baos, final long resumePosition) throws BasicHTTPException, InterruptedException {

        synchronized (this.lock) {
            InputStream input = null;
            int ioExceptionWhere = 0;
            try {

                this.connection = HTTPConnectionFactory.createHTTPConnection(url, this.proxy);
                this.setAllowedResponseCodes(this.connection);
                this.connection.setConnectTimeout(this.getConnectTimeout());
                this.connection.setReadTimeout(this.getReadTimeout());
                this.connection.setRequestProperty("Accept-Language", TranslationFactory.getDesiredLanguage());
                this.connection.setRequestProperty("User-Agent", "AppWork " + Application.getApplication());
                for (final Entry<String, String> next : this.requestHeader.entrySet()) {
                    this.connection.setRequestProperty(next.getKey(), next.getValue());
                }

                if (resumePosition > 0) {
                    this.connection.setRequestProperty("Range", "bytes=" + resumePosition + "-");
                }
                this.connection.setRequestProperty("Connection", "Close");

                this.connection.connect();
                final boolean ranged = this.connection.getRequestProperty("Range") != null;
                if (ranged && this.connection.getResponseCode() == 200) {
                    //
                    throw new BadRangeResponse(this.connection);
                }
                if (this.connection.getResponseCode() == 302) {
                    final String red = this.connection.getHeaderField("Location");
                    if (red != null) {
                        try {
                            this.connection.disconnect();
                        } catch (final Throwable e) {
                        }
                        this.download(new URL(red), progress, maxSize, baos, resumePosition);
                        return;
                    }
                    throw new IOException("302 without locationHeader!");
                }
                this.checkResponseCode();
                input = this.connection.getInputStream();

                if (this.connection.getCompleteContentLength() >= 0) {
                    /* contentLength is known */
                    if (maxSize > 0 && this.connection.getCompleteContentLength() > maxSize) { throw new IOException("Max size exeeded!"); }
                    if (progress != null) {
                        progress.setTotal(this.connection.getCompleteContentLength());
                    }
                } else {
                    /* no contentLength is known */
                }
                final byte[] b = new byte[512 * 1024];
                int len = 0;
                long loaded = Math.max(0, resumePosition);
                if (progress != null) {
                    progress.setLoaded(loaded);
                }
                while (true) {
                    ioExceptionWhere = 1;
                    if ((len = input.read(b)) == -1) {
                        break;
                    }
                    if (Thread.interrupted()) {

                    throw new InterruptedException();

                    }
                    if (len > 0) {
                        if (progress != null) {
                            ioExceptionWhere = 0;
                            progress.onBytesLoaded(b, len);
                        }
                        ioExceptionWhere = 2;
                        baos.write(b, 0, len);

                        loaded += len;
                        if (maxSize > 0 && loaded > maxSize) { throw new IOException("Max size exeeded!"); }
                        if (progress != null) {
                            progress.increaseLoaded(len);
                        }
                    }
                }
                ioExceptionWhere = 0;
                if (this.connection.getCompleteContentLength() >= 0) {
                    if (loaded != this.connection.getCompleteContentLength()) { throw new IOException("Incomplete download! " + loaded + " from " + this.connection.getCompleteContentLength()); }
                }
            } catch (final BasicHTTPException e) {
                throw e;
            } catch (final InterruptedException e) {
                throw e;
            } catch (final Exception e) {
                if (ioExceptionWhere == 1) { throw new BasicHTTPException(this.connection, new ReadIOException(e)); }
                if (ioExceptionWhere == 2) { throw new BasicHTTPException(this.connection, new WriteIOException(e)); }
                throw new BasicHTTPException(this.connection, e);
            } finally {
                try {
                    input.close();
                } catch (final Exception e) {
                }
                try {
                    if (this.logger != null) {
                        this.logger.info(this.connection.toString());
                    }
                } catch (final Throwable e) {
                    e.printStackTrace();
                }
                try {
                    this.connection.disconnect();
                } catch (final Throwable e) {
                }
            }
        }
    }

    public HashSet<Integer> getAllowedResponseCodes() {
        return this.allowedResponseCodes;
    }

    public HTTPConnection getConnection() {
        return this.connection;
    }

    public int getConnectTimeout() {
        return this.connectTimeout;
    }

    public LogInterface getLogger() {
        return this.logger;
    }

    public String getPage(final URL url) throws IOException, InterruptedException {
        synchronized (this.lock) {
            BufferedReader in = null;
            InputStreamReader isr = null;
            try {

                this.connection = HTTPConnectionFactory.createHTTPConnection(url, this.proxy);
                this.setAllowedResponseCodes(this.connection);
                this.connection.setConnectTimeout(this.getConnectTimeout());
                this.connection.setReadTimeout(this.getReadTimeout());
                this.connection.setRequestProperty("Accept-Language", TranslationFactory.getDesiredLanguage());
                this.connection.setRequestProperty("User-Agent", "AppWork " + Application.getApplication());
                this.connection.setRequestProperty("Accept-Charset", "UTF-8");
                for (final Entry<String, String> next : this.requestHeader.entrySet()) {
                    this.connection.setRequestProperty(next.getKey(), next.getValue());
                }
                this.connection.setRequestProperty("Connection", "Close");
                int lookupTry = 0;
                while (true) {
                    try {
                        this.connection.connect();
                        break;
                    } catch (final UnknownHostException e) {
                        if (++lookupTry > 3) { throw e; }
                        /* dns lookup failed, short wait and try again */
                        Thread.sleep(200);
                    }
                }
                this.checkResponseCode();
                in = new BufferedReader(isr = new InputStreamReader(this.connection.getInputStream(), "UTF-8"));

                String str;
                final StringBuilder sb = new StringBuilder();
                while ((str = in.readLine()) != null) {
                    if (sb.length() > 0) {
                        sb.append("\r\n");
                    }
                    sb.append(str);

                }

                return sb.toString();
            } catch (final IOException e) {
                throw new BasicHTTPException(this.connection, new ReadIOException(e));
            } finally {
                try {
                    in.close();
                } catch (final Throwable e) {
                }
                try {
                    isr.close();
                } catch (final Throwable e) {
                }
                try {
                    if (this.logger != null) {
                        this.logger.info(this.connection.toString());
                    }
                } catch (final Throwable e) {
                }
                try {
                    this.connection.disconnect();
                } catch (final Throwable e) {
                }

            }
        }
    }

    public HTTPProxy getProxy() {
        return this.proxy;
    }

    public int getReadTimeout() {
        return this.readTimeout;
    }

    /**
     * @return
     */
    public HashMap<String, String> getRequestHeader() {
        return this.requestHeader;
    }

    public String getRequestHeader(final String key) {
        return this.requestHeader.get(key);
    }

    public String getResponseHeader(final String string) {
        synchronized (this.lock) {
            if (this.connection == null) { return null; }
            return this.connection.getHeaderField(string);

        }
    }

    public HTTPConnection openGetConnection(final URL url) throws IOException, InterruptedException {
        return this.openGetConnection(url, this.readTimeout);
    }

    public HTTPConnection openGetConnection(final URL url, final int readTimeout) throws BasicHTTPException, InterruptedException {
        boolean close = true;
        synchronized (this.lock) {
            try {
                this.connection = HTTPConnectionFactory.createHTTPConnection(url, this.proxy);
                this.setAllowedResponseCodes(this.connection);
                this.connection.setConnectTimeout(this.getConnectTimeout());
                this.connection.setReadTimeout(readTimeout < 0 ? this.readTimeout : readTimeout);
                this.connection.setRequestProperty("Accept-Language", TranslationFactory.getDesiredLanguage());
                this.connection.setRequestProperty("User-Agent", "AppWork " + Application.getApplication());
                this.connection.setRequestProperty("Accept-Charset", "UTF-8");
                for (final Entry<String, String> next : this.requestHeader.entrySet()) {
                    this.connection.setRequestProperty(next.getKey(), next.getValue());
                }
                this.connection.setRequestProperty("Connection", "Close");
                int lookupTry = 0;
                try {
                    while (true) {
                        try {
                            this.connection.connect();
                            break;
                        } catch (final UnknownHostException e) {
                            if (++lookupTry > 3) { throw e; }
                            /* dns lookup failed, short wait and try again */
                            Thread.sleep(200);
                        }
                    }
                } catch (final InterruptedException e) {
                    throw e;
                } catch (final Exception e) {
                    throw new BasicHTTPException(this.connection, new ReadIOException(e));
                }
                close = false;
                this.checkResponseCode();
                return this.connection;
            } catch (final InvalidResponseCode e) {
                throw new BasicHTTPException(this.connection, new ReadIOException(e));
            } finally {
                try {
                    if (this.logger != null) {
                        this.logger.info(this.connection.toString());
                    }
                } catch (final Throwable e) {
                }
                try {
                    if (close) {
                        this.connection.disconnect();
                    }
                } catch (final Throwable e2) {
                }
            }
        }
    }

    public HTTPConnection openPostConnection(final URL url, final UploadProgress progress, final InputStream is, final HashMap<String, String> header) throws BasicHTTPException, InterruptedException {
        boolean close = true;
        synchronized (this.lock) {
            OutputStream outputStream = null;
            final byte[] buffer = new byte[64000];
            try {
                this.connection = HTTPConnectionFactory.createHTTPConnection(url, this.proxy);
                this.setAllowedResponseCodes(this.connection);
                this.connection.setConnectTimeout(this.getConnectTimeout());
                this.connection.setReadTimeout(this.getReadTimeout());
                this.connection.setRequestMethod(RequestMethod.POST);
                this.connection.setRequestProperty("Accept-Language", TranslationFactory.getDesiredLanguage());
                this.connection.setRequestProperty("User-Agent", "AppWork " + Application.getApplication());
                this.connection.setRequestProperty("Connection", "Close");
                /* connection specific headers */
                if (header != null) {
                    for (final Entry<String, String> next : header.entrySet()) {
                        this.connection.setRequestProperty(next.getKey(), next.getValue());
                    }
                }
                for (final Entry<String, String> next : this.requestHeader.entrySet()) {
                    this.connection.setRequestProperty(next.getKey(), next.getValue());
                }

                int lookupTry = 0;
                try {
                    while (true) {
                        try {
                            this.connection.connect();
                            break;
                        } catch (final UnknownHostException e) {
                            if (++lookupTry > 3) { throw e; }
                            /* dns lookup failed, short wait and try again */
                            Thread.sleep(200);
                        }
                    }
                } catch (final IOException e) {
                    throw new BasicHTTPException(this.connection, new ReadIOException(e));
                }
                outputStream = this.connection.getOutputStream();
                int read = 0;
                while ((read = is.read(buffer)) != -1) {
                    try {
                        outputStream.write(buffer, 0, read);
                    } catch (final IOException e) {
                        throw new BasicHTTPException(this.connection, new WriteIOException(e));
                    }
                    if (progress != null) {
                        progress.onBytesUploaded(buffer, read);
                        progress.increaseUploaded(read);
                    }
                    if (Thread.interrupted()) { throw new InterruptedException(); }
                }
                try {
                    outputStream.flush();
                } catch (final IOException e) {
                    throw new BasicHTTPException(this.connection, new WriteIOException(e));
                }
                this.connection.finalizeConnect();
                this.checkResponseCode();
                close = false;
                return this.connection;

            } catch (final IOException e) {
                throw new BasicHTTPException(this.connection, new ReadIOException(e));
            } finally {
                try {
                    if (this.logger != null) {
                        this.logger.info(this.connection.toString());
                    }
                } catch (final Throwable e) {
                }
                try {
                    if (close) {
                        this.connection.disconnect();
                    }
                } catch (final Throwable e2) {
                }
            }
        }
    }

    public byte[] postPage(final URL url, final byte[] byteData) throws BasicHTTPException, InterruptedException {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        this.postPage(url, byteData, baos, null, null);
        return baos.toByteArray();

    }

    /**
     * @param url
     * @param byteData
     * @param baos
     * @return
     * @throws InterruptedException
     * @throws BasicHTTPException
     */
    public void postPage(final URL url, byte[] byteData, final OutputStream baos, final DownloadProgress uploadProgress, final DownloadProgress downloadProgress) throws InterruptedException, BasicHTTPException {

        synchronized (this.lock) {

            final OutputStreamWriter writer = null;
            final BufferedReader reader = null;
            OutputStream outputStream = null;
            final InputStreamReader isr = null;
            try {
                this.connection = HTTPConnectionFactory.createHTTPConnection(url, this.proxy);
                this.setAllowedResponseCodes(this.connection);
                this.connection.setConnectTimeout(this.getConnectTimeout());
                this.connection.setReadTimeout(this.getReadTimeout());
                this.connection.setRequestMethod(RequestMethod.POST);
                this.connection.setRequestProperty("Accept-Language", TranslationFactory.getDesiredLanguage());
                this.connection.setRequestProperty("User-Agent", "AppWork " + Application.getApplication());
                if (byteData == null) {
                    byteData = new byte[0];
                }
                this.connection.setRequestProperty(HTTPConstants.HEADER_REQUEST_CONTENT_LENGTH, byteData.length + "");
                for (final Entry<String, String> next : this.requestHeader.entrySet()) {
                    this.connection.setRequestProperty(next.getKey(), next.getValue());
                }
                this.connection.setRequestProperty("Connection", "Close");
                int lookupTry = 0;
                while (true) {
                    try {
                        this.connection.connect();
                        break;
                    } catch (final UnknownHostException e) {
                        if (++lookupTry > 3) { throw e; }
                        /* dns lookup failed, short wait and try again */
                        Thread.sleep(200);
                    }
                }
                outputStream = this.connection.getOutputStream();
                // writer = new OutputStream(outputStream);
                if (uploadProgress != null) {
                    uploadProgress.setTotal(byteData.length);
                }

                if (this.connection.getCompleteContentLength() >= 0) {
                    /* contentLength is known */
                    if (downloadProgress != null) {
                        downloadProgress.setTotal(this.connection.getCompleteContentLength());
                    }
                } else {
                    /* no contentLength is known */
                }

                // write upload in 50*1024 steps
                if (byteData.length > 0) {
                    int offset = 0;
                    while (true) {
                        final int part = Math.min(50 * 1024, byteData.length - offset);
                        if (part == 0) {
                            if (uploadProgress != null) {
                                uploadProgress.setLoaded(byteData.length);
                            }
                            break;
                        }
                        outputStream.write(byteData, offset, part);
                        outputStream.flush();
                        offset += part;
                        if (uploadProgress != null) {
                            uploadProgress.increaseLoaded(part);
                        }
                    }
                }

                outputStream.flush();
                this.connection.finalizeConnect();
                this.checkResponseCode();
                final byte[] b = new byte[32767];
                int len = 0;
                long loaded = 0;
                final InputStream input = this.connection.getInputStream();
                while (true) {
                    try {
                        if ((len = input.read(b)) == -1) {
                            break;
                        }
                    } catch (final IOException e) {
                        throw new ReadIOException(e);
                    }
                    if (Thread.interrupted()) { throw new InterruptedException(); }
                    if (len > 0) {
                        try {
                            baos.write(b, 0, len);
                        } catch (final IOException e) {
                            throw new WriteIOException(e);
                        }
                        loaded += len;

                        if (downloadProgress != null) {
                            downloadProgress.increaseLoaded(len);
                        }
                    }
                }
                if (this.connection.getCompleteContentLength() >= 0) {
                    if (loaded != this.connection.getCompleteContentLength()) { throw new IOException("Incomplete download! " + loaded + " from " + this.connection.getCompleteContentLength()); }
                }
                return;
            } catch (final IOException e) {
                throw new BasicHTTPException(this.connection, new ReadIOException(e));
            } finally {
                try {
                    reader.close();
                } catch (final Throwable e) {
                }
                try {
                    isr.close();
                } catch (final Throwable e) {
                }
                try {
                    writer.close();
                } catch (final Throwable e) {
                }
                try {
                    outputStream.close();
                } catch (final Throwable e) {
                }
                try {
                    if (this.logger != null) {
                        this.logger.info(this.connection.toString());
                    }
                } catch (final Throwable e) {
                }
                try {
                    this.connection.disconnect();
                } catch (final Throwable e) {
                }

            }
        }
    }

    public String postPage(final URL url, final String data) throws BasicHTTPException, InterruptedException {
        byte[] byteData;
        try {
            byteData = data.getBytes("UTF-8");

            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            this.postPage(url, byteData, baos, null, null);
            return new String(baos.toByteArray(), "UTF-8");

        } catch (final UnsupportedEncodingException e) {
            throw new BasicHTTPException(this.connection, e);
        }
    }

    public void putRequestHeader(final String key, final String value) {
        this.requestHeader.put(key, value);
    }

    protected void setAllowedResponseCodes(final HTTPConnection connection) {
        final HashSet<Integer> loc = this.getAllowedResponseCodes();
        if (loc != null) {
            final ArrayList<Integer> allowed = new ArrayList<Integer>(loc);

            final int[] ret = new int[allowed.size()];
            for (int i = 0; i < ret.length; i++) {
                ret[i] = allowed.get(i);
            }
            connection.setAllowedResponseCodes(ret);
        }
    }

    public void setAllowedResponseCodes(final int... codes) {
        this.allowedResponseCodes = new HashSet<Integer>();
        for (final int i : codes) {
            this.allowedResponseCodes.add(i);
        }
    }

    public void setConnectTimeout(final int connectTimeout) {
        this.connectTimeout = Math.max(1000, connectTimeout);
    }

    public void setLogger(final LogInterface logger) {
        this.logger = logger;
    }

    public void setProxy(final HTTPProxy proxy) {
        this.proxy = proxy;
    }

    public void setReadTimeout(final int readTimeout) {
        this.readTimeout = Math.max(1000, readTimeout);
    }

}
