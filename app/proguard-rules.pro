# Preserve native methods
-keepclasseswithmembernames class * {
    native <methods>;
}

# keep sniffer class and its members for rust JNI bindings
-keep class com.dest4590.sigint.sniffer.Sniffer { *; }

# btleplug resources
-keep class com.nonpolynomial.** { *; }
-keep class io.github.gedgygedgy.** { *; }
