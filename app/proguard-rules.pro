# alphaTab : moteur transpilé, R8 ne doit ni renommer ni retirer son API appelée par réflexion
-keep class alphaTab.** { *; }
-dontwarn alphaTab.**
-keep class net.alphatab.** { *; }
-dontwarn net.alphatab.**

# AlphaSkia (rendu natif Skia)
-keep class net.alphaskia.** { *; }
-dontwarn net.alphaskia.**
