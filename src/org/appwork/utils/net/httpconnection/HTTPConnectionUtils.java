package org.appwork.utils.net.httpconnection;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.URLDecoder;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;

import org.appwork.utils.Regex;
import org.appwork.utils.StringUtils;
import org.appwork.utils.encoding.Base64;
import org.appwork.utils.logging.Log;

public class HTTPConnectionUtils {

    public final static byte R = (byte) 13;
    public final static byte N = (byte) 10;

    public static String getFileNameFromDispositionHeader(String header) {
        // http://greenbytes.de/tech/tc2231/
        if (StringUtils.isEmpty(header)) { return null; }
        final String orgheader = header;
        String contentdisposition = header;

        String filename = null;
        for (int i = 0; i < 2; i++) {
            if (contentdisposition.contains("filename*")) {
                /* Codierung default */
                /*
                 * Content-Disposition: attachment;filename==?UTF-8?B?
                 * RGF2aWQgR3VldHRhIC0gSnVzdCBBIExpdHRsZSBNb3JlIExvdmUgW2FMYnlsb3ZlciBYLUNsdXNpdiBSZW1peF0uTVAz
                 * ?=
                 */
                /* remove fallback, in case RFC 2231/5987 appear */
                contentdisposition = contentdisposition.replaceAll("filename=.*?;", "");
                contentdisposition = contentdisposition.replaceAll("filename\\*", "filename");
                final String format = new Regex(contentdisposition, ".*?=[ \"']*(.+)''").getMatch(0);
                if (format == null) {
                    Log.L.severe("Content-Disposition: invalid format: " + header);
                    filename = null;
                    return filename;
                }
                contentdisposition = contentdisposition.replaceAll(format + "''", "");
                filename = new Regex(contentdisposition, "filename.*?=[ ]*\"(.+)\"").getMatch(0);
                if (filename == null) {
                    filename = new Regex(contentdisposition, "filename.*?=[ ]*'(.+)'").getMatch(0);
                }
                if (filename == null) {
                    header = header.replaceAll("=", "=\"") + "\"";
                    header = header.replaceAll(";\"", "\"");
                    contentdisposition = header;
                } else {
                    try {
                        filename = URLDecoder.decode(filename, format);
                    } catch (final Exception e) {
                        Log.L.severe("Content-Disposition: could not decode filename: " + header);
                        filename = null;
                        return filename;
                    }
                }
            } else if (new Regex(contentdisposition, "=\\?.*?\\?.*?\\?.*?\\?=").matches()) {
                /*
                 * Codierung Encoded Words, TODO: Q-Encoding und mehrfach
                 * tokens, aber noch nicht in freier Wildbahn gesehen
                 */
                final String tokens[][] = new Regex(contentdisposition, "=\\?(.*?)\\?(.*?)\\?(.*?)\\?=").getMatches();
                if (tokens.length == 1 && tokens[0].length == 3 && tokens[0][1].trim().equalsIgnoreCase("B")) {
                    /* Base64 Encoded */
                    try {
                        filename = URLDecoder.decode(new String(Base64.decode(tokens[0][2].trim()), tokens[0][0].trim()), tokens[0][0].trim());
                    } catch (final Exception e) {
                        Log.L.severe("Content-Disposition: could not decode filename: " + header);
                        filename = null;
                        return filename;
                    }
                }
            } else if (new Regex(contentdisposition, "=\\?.*?\\?.*?\\?=").matches()) {
                /* Unicode Format wie es 4Shared nutzt */
                final String tokens[][] = new Regex(contentdisposition, "=\\?(.*?)\\?(.*?)\\?=").getMatches();
                if (tokens.length == 1 && tokens[0].length == 2) {
                    try {
                        contentdisposition = new String(tokens[0][1].trim().getBytes("ISO-8859-1"), tokens[0][0].trim());
                        continue;
                    } catch (final Exception e) {
                        Log.L.severe("Content-Disposition: could not decode filename: " + header);
                        filename = null;
                        return filename;
                    }
                }
            } else {
                /* ohne Codierung */
                filename = new Regex(contentdisposition, "filename.*?=[ ]*\"(.+)\"").getMatch(0);
                if (filename == null) {
                    filename = new Regex(contentdisposition, "filename.*?=[ ]*'(.+)'").getMatch(0);
                }
                if (filename == null) {
                    header = header.replaceAll("=", "=\"") + "\"";
                    header = header.replaceAll(";\"", "\"");
                    contentdisposition = header;
                }
            }
            if (filename != null) {
                break;
            }
        }
        if (filename != null) {
            filename = filename.trim();
            if (filename.startsWith("\"")) {
                Log.L.info("Using Workaround for broken filename header!");
                filename = filename.substring(1);
            }
        }
        if (filename == null) {
            Log.L.severe("Content-Disposition: could not parse header: " + orgheader);
        }
        return filename;
    }

    public static ByteBuffer readheader(final InputStream in, final boolean readSingleLine) throws IOException {
        ByteBuffer bigbuffer = ByteBuffer.wrap(new byte[4096]);
        final byte[] minibuffer = new byte[1];
        int position;
        while (in.read(minibuffer) >= 0) {
            if (bigbuffer.remaining() < 1) {
                final ByteBuffer newbuffer = ByteBuffer.wrap(new byte[bigbuffer.capacity() * 2]);
                bigbuffer.flip();
                newbuffer.put(bigbuffer);
                bigbuffer = newbuffer;
            }
            bigbuffer.put(minibuffer);
            if (readSingleLine) {
                if (bigbuffer.position() >= 1) {
                    /*
                     * \n only line termination, for fucking buggy non rfc
                     * servers
                     */
                    position = bigbuffer.position();
                    if (bigbuffer.get(position - 1) == HTTPConnectionUtils.N) {
                        break;
                    }
                    if (bigbuffer.position() >= 2) {
                        /* \r\n, correct line termination */
                        if (bigbuffer.get(position - 2) == HTTPConnectionUtils.R && bigbuffer.get(position - 1) == HTTPConnectionUtils.N) {
                            break;
                        }
                    }
                }
            } else {
                if (bigbuffer.position() >= 2) {
                    position = bigbuffer.position();
                    if (bigbuffer.get(position - 2) == HTTPConnectionUtils.N && bigbuffer.get(position - 1) == HTTPConnectionUtils.N) {
                        /*
                         * \n\n for header<->content divider, or fucking buggy
                         * non rfc servers
                         */
                        break;
                    }
                    if (bigbuffer.position() >= 4) {
                        /* \r\n\r\n for header<->content divider */
                        if (bigbuffer.get(position - 4) == HTTPConnectionUtils.R && bigbuffer.get(position - 3) == HTTPConnectionUtils.N && bigbuffer.get(position - 2) == HTTPConnectionUtils.R && bigbuffer.get(position - 1) == HTTPConnectionUtils.N) {
                            break;
                        }
                    }
                }
            }
        }
        bigbuffer.flip();
        return bigbuffer;
    }

    public static InetAddress[] resolvHostIP(String host) throws IOException {
        if (StringUtils.isEmpty(host)) { throw new UnknownHostException("Could not resolv: -empty host-"); }
        /* remove spaces....so literal IP's work without resolving */
        host = host.trim();
        InetAddress hosts[] = null;
        for (int resolvTry = 0; resolvTry < 2; resolvTry++) {
            try {
                /* resolv all possible ip's */
                hosts = InetAddress.getAllByName(host);
                return hosts;
            } catch (final UnknownHostException e) {
                try {
                    Thread.sleep(500);
                } catch (final InterruptedException e1) {
                    break;
                }
            }
        }
        throw new UnknownHostException("Could not resolv: -" + host + "-");
    }
}
