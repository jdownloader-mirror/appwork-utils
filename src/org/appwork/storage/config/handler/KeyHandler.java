/**
 * Copyright (c) 2009 - 2011 AppWork UG(haftungsbeschränkt) <e-mail@appwork.org>
 * 
 * This file is part of org.appwork.storage.config
 * 
 * This software is licensed under the Artistic License 2.0,
 * see the LICENSE file or http://www.opensource.org/licenses/artistic-license-2.0.php
 * for details
 */
package org.appwork.storage.config.handler;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.Storage;
import org.appwork.storage.TypeRef;
import org.appwork.storage.config.ConfigInterface;
import org.appwork.storage.config.InterfaceParseException;
import org.appwork.storage.config.MethodHandler;
import org.appwork.storage.config.ValidationException;
import org.appwork.storage.config.annotations.AboutConfig;
import org.appwork.storage.config.annotations.AbstractCustomValueGetter;
import org.appwork.storage.config.annotations.AbstractValidator;
import org.appwork.storage.config.annotations.AllowStorage;
import org.appwork.storage.config.annotations.CryptedStorage;
import org.appwork.storage.config.annotations.CustomValueGetter;
import org.appwork.storage.config.annotations.DefaultFactory;
import org.appwork.storage.config.annotations.DefaultJsonObject;
import org.appwork.storage.config.annotations.DescriptionForConfigEntry;
import org.appwork.storage.config.annotations.HexColorString;
import org.appwork.storage.config.annotations.LookUpKeys;
import org.appwork.storage.config.annotations.PlainStorage;
import org.appwork.storage.config.annotations.RequiresRestart;
import org.appwork.storage.config.annotations.ValidatorFactory;
import org.appwork.storage.config.events.ConfigEvent;
import org.appwork.storage.config.events.ConfigEvent.Types;
import org.appwork.storage.config.events.ConfigEventSender;
import org.appwork.utils.reflection.Clazz;

/**
 * @author thomas
 * 
 */
public abstract class KeyHandler<RawClass> {

    private static final String                 ANNOTATION_PACKAGE_NAME = CryptedStorage.class.getPackage().getName();
    private static final String                 PACKAGE_NAME            = PlainStorage.class.getPackage().getName();
    private final String                        key;
    private MethodHandler                       getter;

    protected MethodHandler                     setter;
    protected final StorageHandler<?>           storageHandler;
    private boolean                             primitive;
    protected RawClass                          defaultValue;

    private ConfigEventSender<RawClass>         eventSender;
    private AbstractValidator<RawClass>         validatorFactory;
    private AbstractCustomValueGetter<RawClass> customValueGetter;
    protected String[]                          backwardsCompatibilityLookupKeys;

    /**
     * @param storageHandler
     * @param key2
     */
    protected KeyHandler(final StorageHandler<?> storageHandler, final String key) {
        this.storageHandler = storageHandler;
        this.key = key;
    }

    protected void checkBadAnnotations(final Class<? extends Annotation>... class1) {
        int checker = 0;
        if (this.getAnnotation(this.getDefaultAnnotation()) != null) {
            checker++;
        }
        if (this.getAnnotation(DefaultJsonObject.class) != null) {
            checker++;
        }
        if (this.getAnnotation(DefaultFactory.class) != null) {
            checker++;
        }
        if (checker > 1) { throw new InterfaceParseException("Make sure that you use only one  of getDefaultAnnotation,DefaultObjectValue or DefaultValue "); }

        this.checkBadAnnotations(this.getter.getMethod(), class1);
        if (this.setter != null) {
            this.checkBadAnnotations(this.setter.getMethod(), class1);
        }

    }

    /**
     * @param m
     * @param class1
     */
    private void checkBadAnnotations(final Method m, final Class<? extends Annotation>... classes) {

        final Class<?>[] okForAll = new Class<?>[] { HexColorString.class, CustomValueGetter.class, ValidatorFactory.class, DefaultJsonObject.class, DefaultFactory.class, AboutConfig.class, RequiresRestart.class, AllowStorage.class, DescriptionForConfigEntry.class, CryptedStorage.class, PlainStorage.class };
        final Class<?>[] clazzes = new Class<?>[classes.length + okForAll.length];
        System.arraycopy(classes, 0, clazzes, 0, classes.length);
        System.arraycopy(okForAll, 0, clazzes, classes.length, okForAll.length);
        /**
         * This main mark is important!!
         */
        main: for (final Annotation a : m.getAnnotations()) {
            // all other Annotations are ok anyway
            if (a == null) {
                continue;
            }
            final String aName = a.annotationType().getName();
            if (!aName.startsWith(KeyHandler.PACKAGE_NAME)) {
                continue;
            }
            if (this.getDefaultAnnotation() != null && this.getDefaultAnnotation().isAssignableFrom(a.getClass())) {
                continue;
            }
            for (final Class<?> ok : clazzes) {
                if (ok.isAssignableFrom(a.getClass())) {
                    continue main;
                }
            }
            throw new InterfaceParseException("Bad Annotation: " + a + " for " + m);
        }

    }

    /**
     * @param valueUpdated
     * @param keyHandler
     * @param object
     */
    protected void fireEvent(final Types type, final KeyHandler<?> keyHandler, final Object parameter) {
        this.storageHandler.fireEvent(type, keyHandler, parameter);
        if (this.hasEventListener()) {
            this.getEventSender().fireEvent(new ConfigEvent(type, this, parameter));
        }
    }

    @SuppressWarnings("unchecked")
    protected Class<? extends Annotation>[] getAllowedAnnotations() {
        return (Class<? extends Annotation>[]) new Class<?>[] { LookUpKeys.class };
    }

    /**
     * @param <T>
     * @param class1
     * @return
     */
    public <T extends Annotation> T getAnnotation(final Class<T> class1) {
        if (class1 == null) { return null; }
        T ret = this.getter.getMethod().getAnnotation(class1);
        if (ret == null && this.setter != null) {
            ret = this.setter.getMethod().getAnnotation(class1);
        } else if (this.setter != null && this.setter.getMethod().getAnnotation(class1) != null) {

            if (KeyHandler.ANNOTATION_PACKAGE_NAME.equals(class1.getPackage().getName())) {

                //

                throw new InterfaceParseException("Dupe Annotation in  " + this + " (" + class1 + ") " + this.setter.getMethod());
            }
        }
        return ret;
    }

    /**
     * @return
     */
    public Class<?> getDeclaringClass() {
        if (this.getter != null) {
            return this.getter.getMethod().getDeclaringClass();
        } else {
            return this.setter.getMethod().getDeclaringClass();
        }

    }

    protected Class<? extends Annotation> getDefaultAnnotation() {
        return null;
    }

    @SuppressWarnings("unchecked")
    public RawClass getDefaultValue() {
        try {
            final DefaultFactory df = this.getAnnotation(DefaultFactory.class);
            if (df != null) { return (RawClass) df.value().newInstance().getDefaultValue(); }
            final DefaultJsonObject defaultJson = this.getAnnotation(DefaultJsonObject.class);
            if (defaultJson != null) { return (RawClass) JSonStorage.restoreFromString(defaultJson.value(), new TypeRef<Object>(this.getRawClass()) {
            }, null); }
            final Annotation ann = this.getAnnotation(this.getDefaultAnnotation());
            if (ann != null) { return (RawClass) ann.annotationType().getMethod("value", new Class[] {}).invoke(ann, new Object[] {}); }
            return this.defaultValue;
        } catch (final Throwable e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Lazy initialiser of the eventsender. we do not wnat to create an
     * eventsender if nowbody uses it
     * 
     * @return
     */
    public synchronized ConfigEventSender<RawClass> getEventSender() {
        if (this.eventSender == null) {
            this.eventSender = new ConfigEventSender<RawClass>();
        }
        return this.eventSender;
    }

    public MethodHandler getGetter() {
        return this.getter;
    }

    public String getKey() {
        return this.key;
    }

    /**
     * @return
     */
    @SuppressWarnings("unchecked")
    public Class<RawClass> getRawClass() {
        return (Class<RawClass>) this.getter.getRawClass();
    }

    /**
     * @return
     */
    public Type getRawType() {
        if (this.getter != null) { return this.getter.getRawType(); }
        return this.setter.getRawType();

    }

    public MethodHandler getSetter() {
        return this.setter;
    }

    public StorageHandler<?> getStorageHandler() {
        return this.storageHandler;
    }

    public static enum AbstractTypeDefinition {
        BOOLEAN,
        INT,
        LONG,
        STRING,
        OBJECT,
        OBJECT_LIST,
        STRING_LIST,
        ENUM,
        BYTE,
        CHAR,
        DOUBLE,
        FLOAT,
        SHORT,
        BOOLEAN_LIST,
        BYTE_LIST,
        SHORT_LIST,
        LONG_LIST,
        INT_LIST,
        FLOAT_LIST,
        ENUM_LIST,
        DOUBLE_LIST,
        CHAR_LIST,
        UNKNOWN,
        HEX_COLOR,
        HEX_COLOR_LIST;
    }

    public AbstractTypeDefinition getAbstractType() {
        Type ret = null;
        if (this.getter != null) {
            ret = this.getter.getMethod().getGenericReturnType();
        } else {
            ret = this.setter.getMethod().getGenericParameterTypes()[0];
        }

        if (ret instanceof Class) {
            final Class<?> clazz = (Class<?>) ret;

            if (Clazz.isBoolean(ret)) { return AbstractTypeDefinition.BOOLEAN; }
            if (Clazz.isByte(ret)) { return AbstractTypeDefinition.BYTE; }
            if (Clazz.isCharacter(ret)) { return AbstractTypeDefinition.CHAR; }
            if (Clazz.isDouble(ret)) { return AbstractTypeDefinition.DOUBLE; }
            if (Clazz.isEnum(ret)) { return AbstractTypeDefinition.ENUM; }
            if (Clazz.isFloat(ret)) { return AbstractTypeDefinition.FLOAT; }
            if (Clazz.isInteger(ret)) { return AbstractTypeDefinition.INT; }
            if (Clazz.isLong(ret)) { return AbstractTypeDefinition.LONG; }
            if (Clazz.isShort(ret)) { return AbstractTypeDefinition.SHORT; }
            if (Clazz.isString(ret)) {

                if (getAnnotation(HexColorString.class) != null) { return AbstractTypeDefinition.HEX_COLOR; }
                return AbstractTypeDefinition.STRING;
            }

            if (clazz.isArray()) {

                final Class aType = ((Class) ret).getComponentType();

                if (Clazz.isBoolean(aType)) { return AbstractTypeDefinition.BOOLEAN_LIST; }
                if (Clazz.isByte(aType)) { return AbstractTypeDefinition.BYTE_LIST; }
                if (Clazz.isCharacter(aType)) { return AbstractTypeDefinition.CHAR_LIST; }
                if (Clazz.isDouble(aType)) { return AbstractTypeDefinition.DOUBLE_LIST; }
                if (Clazz.isEnum(aType)) { return AbstractTypeDefinition.ENUM_LIST; }
                if (Clazz.isFloat(aType)) { return AbstractTypeDefinition.FLOAT_LIST; }
                if (Clazz.isInteger(aType)) { return AbstractTypeDefinition.INT_LIST; }
                if (Clazz.isLong(aType)) { return AbstractTypeDefinition.LONG_LIST; }
                if (Clazz.isShort(aType)) { return AbstractTypeDefinition.SHORT_LIST; }
                if (Clazz.isString(aType)) {
                    if (getAnnotation(HexColorString.class) != null) { return AbstractTypeDefinition.HEX_COLOR_LIST; }
                    return AbstractTypeDefinition.STRING_LIST;
                }

                return AbstractTypeDefinition.OBJECT_LIST;

            }

            // if(ret instanceof List){
            // return AbstractType.OBJECT_LIST;
            // }
        } else {

            if (ret instanceof ParameterizedType) {
                final Type raw = ((ParameterizedType) ret).getRawType();
                final Type[] acutal = ((ParameterizedType) ret).getActualTypeArguments();
                if (raw instanceof Class) {
                    final Class<?> rawClazz = (Class<?>) raw;
                    if (List.class.isAssignableFrom(rawClazz)) {
                        if (Clazz.isBoolean(acutal[0])) { return AbstractTypeDefinition.BOOLEAN_LIST; }

                        if (Clazz.isByte(acutal[0])) { return AbstractTypeDefinition.BYTE_LIST; }
                        if (Clazz.isCharacter(acutal[0])) { return AbstractTypeDefinition.CHAR_LIST; }
                        if (Clazz.isDouble(acutal[0])) { return AbstractTypeDefinition.DOUBLE_LIST; }
                        if (Clazz.isEnum(acutal[0])) { return AbstractTypeDefinition.ENUM_LIST; }
                        if (Clazz.isFloat(acutal[0])) { return AbstractTypeDefinition.FLOAT_LIST; }
                        if (Clazz.isInteger(acutal[0])) { return AbstractTypeDefinition.INT_LIST; }
                        if (Clazz.isLong(acutal[0])) { return AbstractTypeDefinition.LONG_LIST; }
                        if (Clazz.isShort(acutal[0])) { return AbstractTypeDefinition.SHORT_LIST; }
                        if (Clazz.isString(acutal[0])) {
                            if (getAnnotation(HexColorString.class) != null) { return AbstractTypeDefinition.HEX_COLOR_LIST; }

                            return AbstractTypeDefinition.STRING_LIST;
                        }
                        return AbstractTypeDefinition.OBJECT_LIST;

                    }
                } else {
                    return AbstractTypeDefinition.UNKNOWN;
                }

            } else {
                return AbstractTypeDefinition.UNKNOWN;
            }
        }

        return AbstractTypeDefinition.OBJECT;
    }

    public String getTypeString() {
        Type ret = null;
        if (this.getter != null) {
            ret = this.getter.getMethod().getGenericReturnType();
        } else {
            ret = this.setter.getMethod().getGenericParameterTypes()[0];
        }
        if (ret instanceof Class) {
            return ((Class) ret).getName();
        } else {
            return ret.toString();
        }
    }

    public RawClass getValue() {
        final RawClass value = this.getValueStorage();
        if (this.customValueGetter != null) { return this.customValueGetter.getValue(value); }
        return value;
    }

    public RawClass getValueStorage() {
        final Storage storage = this.getStorageHandler().getPrimitiveStorage();
        if (storage.hasProperty(this.getKey())) {
            // primitiveSTorage contains a value. we do not need to calculate
            // the defaultvalue.
            return storage.get(this.getKey(), this.defaultValue);
        }

        // we have no value yet. call the getDefaultMethod to calculate the
        // default value

        if (this.backwardsCompatibilityLookupKeys != null) {
            for (final String key : this.backwardsCompatibilityLookupKeys) {
                if (storage.hasProperty(key)) {
                    final boolean apv = storage.isAutoPutValues();
                    try {
                        if (!apv) {
                            storage.setAutoPutValues(true);
                        }
                        return storage.get(this.getKey(), storage.get(key, this.defaultValue));
                    } finally {
                        if (!apv) {
                            storage.setAutoPutValues(apv);
                        }
                    }
                }
            }
        }
        return storage.get(this.getKey(), this.getDefaultValue());
    }

    public synchronized boolean hasEventListener() {
        return this.eventSender != null && this.eventSender.hasListener();
    }

    /**
     * @throws Throwable
     * 
     */
    @SuppressWarnings("unchecked")
    protected void init() throws Throwable {
        if (this.getter == null) { throw new InterfaceParseException("Getter Method is Missing for " + this.setter.getMethod()); }

        // read local cryptinfos
        this.primitive = JSonStorage.canStorePrimitive(this.getter.getMethod().getReturnType());
        final CryptedStorage cryptedStorage = this.getAnnotation(CryptedStorage.class);
        if (cryptedStorage != null) {
            if (this.storageHandler.getPrimitiveStorage().getCryptKey() != null) {
                //
                throw new InterfaceParseException("No reason to mark " + this + " as @CryptedStorage. Parent is already CryptedStorage");
            } else if (!(this instanceof ListHandler)) {
                //
                throw new InterfaceParseException(this + " Cannot set @CryptedStorage on primitive fields. Use an object, or an extra plain config interface");
            }
            this.validateEncryptionKey(cryptedStorage.key());
        }
        final PlainStorage plainStorage = this.getAnnotation(PlainStorage.class);
        if (plainStorage != null) {
            if (cryptedStorage != null) {
                //
                throw new InterfaceParseException("Cannot Set CryptStorage and PlainStorage Annotation in " + this);
            }
            if (this.storageHandler.getPrimitiveStorage().getCryptKey() == null) {
                //
                throw new InterfaceParseException("No reason to mark " + this + " as @PlainStorage. Parent is already Plain");
            } else if (!(this instanceof ListHandler)) {
                //
                throw new InterfaceParseException(this + " Cannot set @PlainStorage on primitive fields. Use an object, or an extra plain config interface");
                // primitive storage. cannot set single plain values in a en
                // crypted
                // primitive storage
            }
            // parent crypted, but plain for this single entry
        }

        try {
            final ValidatorFactory anno = this.getAnnotation(ValidatorFactory.class);
            if (anno != null) {
                this.validatorFactory = (AbstractValidator<RawClass>) anno.value().newInstance();
            }
        } catch (final Throwable e) {
        }
        try {
            final CustomValueGetter anno = this.getAnnotation(CustomValueGetter.class);
            if (anno != null) {
                this.customValueGetter = (AbstractCustomValueGetter<RawClass>) anno.value().newInstance();
            }
        } catch (final Throwable e) {
        }
        this.checkBadAnnotations(this.getAllowedAnnotations());
        this.initDefaults();
        this.initHandler();

        final String kk = "CFG:" + this.storageHandler.getConfigInterface().getName() + "." + this.key;
        final String sys = System.getProperty(kk);
        if (sys != null) {
            // Set configvalud because of JVM Parameter
            System.out.println(kk + "=" + sys);
            this.setValue((RawClass) JSonStorage.restoreFromString(sys, new TypeRef<Object>(this.getRawClass()) {
            }, null));
        }

        final LookUpKeys lookups = this.getAnnotation(LookUpKeys.class);
        if (lookups != null) {
            this.backwardsCompatibilityLookupKeys = lookups.value();
        }

    }

    protected void initDefaults() throws Throwable {
        this.defaultValue = null;
    }

    /**
     * @throws Throwable
     * 
     */
    protected abstract void initHandler() throws Throwable;

    /**
     * returns true of this keyhandler belongs to ConfigInterface
     * 
     * @param settings
     * @return
     */
    public boolean isChildOf(final ConfigInterface settings) {
        return settings._getStorageHandler() == this.getStorageHandler();
    }

    /**
     * @param m
     * @return
     */
    protected boolean isGetter(final Method m) {
        return m.equals(this.getter.getMethod());
    }

    protected boolean isPrimitive() {
        return this.primitive;
    }

    /**
     * @param object
     */
    protected abstract void putValue(RawClass object);

    public void setDefaultValue(final RawClass c) {
        this.defaultValue = c;
    }

    /**
     * @param h
     */
    protected void setGetter(final MethodHandler h) {
        this.getter = h;

    }

    /**
     * @param h
     */
    protected void setSetter(final MethodHandler h) {
        this.setter = h;

    }

    /**
     * @param newValue
     */
    public void setValue(final RawClass newValue) throws ValidationException {
        try {
            final RawClass oldValue = this.getValue();
            if (oldValue == null && newValue == null) {
                /* everything is null */
                return;
            }
            boolean changed = false;
            if (newValue != null && oldValue == null) {
                /* old is null, but new is not */
                changed = true;
            } else if (oldValue != null && newValue == null) {
                /* new is null, but old is not */
                changed = true;

            } else if (!Clazz.isPrimitive(this.getRawClass()) && !Clazz.isEnum(this.getRawClass()) && this.getRawClass() != String.class) {
                /* no primitive, we cannot detect changes 100% */
                changed = true;
            } else if (!newValue.equals(oldValue)) {
                /* does not equal */
                changed = true;
            }
            if (changed == false) { return; }
            if (this.validatorFactory != null) {
                this.validatorFactory.validate(newValue);
            }
            this.validateValue(newValue);
            this.putValue(newValue);

            this.fireEvent(ConfigEvent.Types.VALUE_UPDATED, this, newValue);

        } catch (final ValidationException e) {
            e.setValue(newValue);
            this.fireEvent(ConfigEvent.Types.VALIDATOR_ERROR, this, e);

            throw e;
        } catch (final Throwable t) {
            final ValidationException e = new ValidationException(t);
            e.setValue(newValue);
            this.fireEvent(ConfigEvent.Types.VALIDATOR_ERROR, this, e);

            throw e;
        }
    }

    @Override
    public String toString() {
        final RawClass ret = this.getValue();
        return ret == null ? null : ret.toString();
    }

    public void validateEncryptionKey(final byte[] key2) {
        if (key2 == null) { throw new InterfaceParseException("Key missing in " + this); }
        if (key2.length != JSonStorage.KEY.length) { throw new InterfaceParseException("Crypt key for " + this + " is invalid. required length: " + JSonStorage.KEY.length); }
    }

    /**
     * @param object
     */
    protected abstract void validateValue(RawClass object) throws Throwable;

}
