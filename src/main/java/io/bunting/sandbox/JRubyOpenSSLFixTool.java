package io.bunting.sandbox;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

/**
 * Invoke from jruby like:
 *
 * <pre>
 *   require 'openssl'
 *   require 'java'
 *
 *   utilsClass = org.jruby.ext.openssl.x509store.X509Utils.java_class
 *   Java::IoBuntingSandbox::JRubyOpenSSLFixTool.modifyTrustStore(utilsClass)
 * </pre>
 */
public class JRubyOpenSSLFixTool {
  public static final String UTILS_CLASS_NAME = "org.jruby.ext.openssl.x509store.X509Utils";

  /**
   * The X509Utils class has to be passed in from ruby, b/c we don't have access to it in the pure JVM scope.
   * I'm assuming that we could probably find the right classloader, but this seems like a simpler and quicker
   * solution -- which should be fine for our current goals.
   * @param x509UtilsClass
   */
  public static void modifyTrustStore(Class<?> x509UtilsClass) throws NoSuchFieldException, IllegalAccessException {
    if (!UTILS_CLASS_NAME.equals(x509UtilsClass.getName())) {
      throw new IllegalArgumentException("Expected class " + UTILS_CLASS_NAME + " but got " + x509UtilsClass);
    }

    // get the field -- will throw exception if not found for some reason
    Field field = x509UtilsClass.getDeclaredField("X509_CERT_FILE");

    // make the Field object non-final -- this is terrible!  generally not recommended, but I think it'll get us
    // what we want
    Field modifiersField = Field.class.getDeclaredField("modifiers");
    modifiersField.setAccessible(true);
    modifiersField.setInt(field, field.getModifiers() & ~Modifier.FINAL);

    // get the trust store property that we want to use
    String trustStoreLocation = System.getProperty("javax.net.ssl.trustStore");
    if (trustStoreLocation != null) {
      field.set(null, trustStoreLocation);
    } else {
      System.err.println("Not setting truststore for JRuby since a specific one is not define.");
    }
  }
}
