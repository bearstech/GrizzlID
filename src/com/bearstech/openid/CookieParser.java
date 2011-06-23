/* Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
//Source: http://android.git.kernel.org/?p=platform/libcore.git;a=blob;f=luni/src/main/java/java/net/HttpCookie.java
package com.bearstech.openid;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.apache.http.impl.cookie.BasicClientCookie;

public class CookieParser {
    private static final String ATTRIBUTE_NAME_TERMINATORS = ",;= \t";
    private static final String WHITESPACE = " \t";
    private final String input;
    private final String inputLowerCase;
    private int pos = 0;

    /*
     * The cookie's version is set based on an overly complex heuristic:
     * If it has an expires attribute, the version is 0.
     * Otherwise, if it has a max-age attribute, the version is 1.
     * Otherwise, if the cookie started with "Set-Cookie2", the version is 1.
     * Otherwise, if it has any explicit version attributes, use the first one.
     * Otherwise, the version is 0.
     */
    boolean hasExpires = false;
    boolean hasMaxAge = false;
    boolean hasVersion = false;

    CookieParser(String input) {
        this.input = input;
        this.inputLowerCase = input.toLowerCase(Locale.US);
    }

    public List<BasicClientCookie> parse() {
        List<BasicClientCookie> cookies = new ArrayList<BasicClientCookie>(2);

        // The RI permits input without either the "Set-Cookie:" or "Set-Cookie2" headers.
        boolean pre2965 = true;
        if (inputLowerCase.startsWith("set-cookie2:")) {
            pos += "set-cookie2:".length();
            pre2965 = false;
            hasVersion = true;
        } else if (inputLowerCase.startsWith("set-cookie:")) {
            pos += "set-cookie:".length();
        }

        /*
         * Read a comma-separated list of cookies. Note that the values may contain commas!
         *   <NAME> "=" <VALUE> ( ";" <ATTR NAME> ( "=" <ATTR VALUE> )? )*
         */
        while (true) {
            String name = readAttributeName(false);
            if (name == null) {
                if (cookies.isEmpty()) {
                    throw new IllegalArgumentException("No cookies in " + input);
                }
                return cookies;
            }

            if (!readEqualsSign()) {
                throw new IllegalArgumentException(
                        "Expected '=' after " + name + " in " + input);
            }

            String value = readAttributeValue(pre2965 ? ";" : ",;");
            BasicClientCookie cookie = new BasicClientCookie(name, value);
            cookie.setVersion((pre2965 ? 0 : 1));
            cookies.add(cookie);

            /*
             * Read the attributes of the current cookie. Each iteration of this loop should
             * enter with input either exhausted or prefixed with ';' or ',' as in ";path=/"
             * and ",COOKIE2=value2".
             */
            while (true) {
                skipWhitespace();
                if (pos == input.length()) {
                    break;
                }

                if (input.charAt(pos) == ',') {
                    pos++;
                    break; // a true comma delimiter; the current cookie is complete.
                } else if (input.charAt(pos) == ';') {
                    pos++;
                }

                String attributeName = readAttributeName(true);
                if (attributeName == null) {
                    continue; // for empty attribute as in "Set-Cookie: foo=Foo;;path=/"
                }

                /*
                 * Since expires and port attributes commonly include comma delimiters, always
                 * scan until a semicolon when parsing these attributes.
                 */
                String terminators = pre2965
                        || "expires".equals(attributeName) || "port".equals(attributeName)
                        ? ";"
                        : ";,";
                String attributeValue = null;
                if (readEqualsSign()) {
                    attributeValue = readAttributeValue(terminators);
                }
                cookie.setAttribute(attributeName, attributeValue);
            }

            if (hasExpires) {
                cookie.setVersion(0);
            } else if (hasMaxAge) {
                cookie.setVersion(1);
            }
        }
    }

    /**
     * Returns the next attribute name, or null if the input has been
     * exhausted. Returns wth the cursor on the delimiter that follows.
     */
    private String readAttributeName(boolean returnLowerCase) {
        skipWhitespace();
        int c = find(ATTRIBUTE_NAME_TERMINATORS);
        String forSubstring = returnLowerCase ? inputLowerCase : input;
        String result = pos < c ? forSubstring.substring(pos, c) : null;
        pos = c;
        return result;
    }

    /**
     * Returns true if an equals sign was read and consumed.
     */
    private boolean readEqualsSign() {
        skipWhitespace();
        if (pos < input.length() && input.charAt(pos) == '=') {
            pos++;
            return true;
        }
        return false;
    }

    /**
     * Reads an attribute value, by parsing either a quoted string or until
     * the next character in {@code terminators}. The terminator character
     * is not consumed.
     */
    private String readAttributeValue(String terminators) {
        skipWhitespace();

        /*
         * Quoted string: read 'til the close quote. The spec mentions only "double quotes"
         * but RI bug 6901170 claims that 'single quotes' are also used.
         */
        if (pos < input.length() && (input.charAt(pos) == '"' || input.charAt(pos) == '\'')) {
            char quoteCharacter = input.charAt(pos++);
            int closeQuote = input.indexOf(quoteCharacter, pos);
            if (closeQuote == -1) {
                throw new IllegalArgumentException("Unterminated string literal in " + input);
            }
            String result = input.substring(pos, closeQuote);
            pos = closeQuote + 1;
            return result;
        }

        int c = find(terminators);
        String result = input.substring(pos, c);
        pos = c;
        return result;
    }

    /**
     * Returns the index of the next character in {@code chars}, or the end
     * of the string.
     */
    private int find(String chars) {
        for (int c = pos; c < input.length(); c++) {
            if (chars.indexOf(input.charAt(c)) != -1) {
                return c;
            }
        }
        return input.length();
    }

    private void skipWhitespace() {
        for (; pos < input.length(); pos++) {
            if (WHITESPACE.indexOf(input.charAt(pos)) == -1) {
                break;
            }
        }
    }
}
