# Règles R8 spécifiques à Finoria.
# Room, Hilt et Compose apportent leurs propres règles (consumer rules) — rien à ajouter.

# Stack traces lisibles dans la console Play (les numéros de ligne sont conservés,
# le nom de fichier source est masqué).
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# ── kotlinx.serialization ────────────────────────────────────────────────
# Utilisé par StorageService pour lire l'ancienne persistance JSON (migration
# one-shot vers Room). Les serializers générés des modèles @Serializable sont
# résolus par réflexion sur le Companion → ils doivent survivre à R8, sinon la
# migration échoue silencieusement et les données legacy seraient perdues.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

-keep,includedescriptorclasses class com.finoria.app.**$$serializer { *; }
-keepclassmembers class com.finoria.app.** {
    *** Companion;
}
-keepclasseswithmembers class com.finoria.app.** {
    kotlinx.serialization.KSerializer serializer(...);
}
