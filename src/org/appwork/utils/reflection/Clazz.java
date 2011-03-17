/**
 * Copyright (c) 2009 - 2011 AppWork UG(haftungsbeschränkt) <e-mail@appwork.org>
 * 
 * This file is part of org.appwork.utils.reflection
 * 
 * This software is licensed under the Artistic License 2.0,
 * see the LICENSE file or http://www.opensource.org/licenses/artistic-license-2.0.php
 * for details
 */
package org.appwork.utils.reflection;

/**
 * @author thomas
 * 
 */
public class Clazz {
    /**
     * returns true if type is a boolean. No Matter if primitive or it's object
     * wrapper
     * 
     * @param type
     * @return
     */
    public static boolean isBoolean(final Class<?> type) {
        return type == Boolean.class || type == boolean.class;
    }

    /**
     * returns true if type is a byte. No Matter if primitive or it's object
     * wrapper
     * 
     * @param type
     * @return
     */
    public static boolean isByte(final Class<?> type) {
        return type == Byte.class || type == byte.class;
    }

    /**
     * returns true if type is a char. No Matter if primitive or it's object
     * wrapper
     * 
     * @param type
     * @return
     */
    public static boolean isCharacter(final Class<?> type) {
        return type == Character.class || type == char.class;
    }

    /**
     * returns true if type is a double. No Matter if primitive or it's object
     * wrapper
     * 
     * @param type
     * @return
     */
    public static boolean isDouble(final Class<?> type) {
        return type == Double.class || type == double.class;
    }

    /**
     * returns true if type is a float. No Matter if primitive or it's object
     * wrapper
     * 
     * @param type
     * @return
     */
    public static boolean isFloat(final Class<?> type) {
        return type == Float.class || type == float.class;
    }

    /**
     * returns true if type is a int. No Matter if primitive or it's object
     * wrapper
     * 
     * @param type
     * @return
     */
    public static boolean isInteger(final Class<?> type) {
        return type == Integer.class || type == int.class;
    }

    /**
     * returns true if type is a long. No Matter if primitive or it's object
     * wrapper
     * 
     * @param type
     * @return
     */
    public static boolean isLong(final Class<?> type) {
        return type == Long.class || type == long.class;
    }

    /**
     * returns true if type is a primitive or a priomitive object wrapper
     * 
     * @param type
     * @return
     */
    public static boolean isPrimitive(final Class<?> type) {
        return type.isPrimitive() || Clazz.isPrimitiveWrapper(type);
    }

    /**
     * returns true if type os a primitive object wrapper
     * 
     * @param type
     * @return
     */
    public static boolean isPrimitiveWrapper(final Class<?> type) {
        return type == Boolean.class || type == Integer.class || type == Long.class || type == Byte.class || type == Short.class || type == Float.class || type == Double.class || type == Character.class || type == Void.class;

    }

    /**
     * returns true if type is a short. No Matter if primitive or it's object
     * wrapper
     * 
     * @param type
     * @return
     */
    public static boolean isShort(final Class<?> type) {
        return type == Short.class || type == short.class;
    }

    /**
     * returns true if type is a void. No Matter if primitive or it's object
     * wrapper
     * 
     * @param type
     * @return
     */
    public static boolean isVoid(final Class<?> type) {
        return type == Void.class || type == void.class;
    }

}
