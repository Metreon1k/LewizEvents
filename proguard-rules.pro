### LewizEvents — ProGuard rules
### Задача: защитить логику, но не сломать загрузку мода Fabric'ом и работу с GUI/HTTP.

-dontoptimize
-dontshrink
-dontwarn **

# Точка входа указана по полному имени в fabric.mod.json — класс,
# конструктор без аргументов и переопределённый метод нужно сохранить,
# иначе Fabric Loader не найдёт мод при запуске.
-keep class dev.lewiz.events.LewizEventsClient {
    public <init>();
    public void onInitializeClient();
}

# Все enum'ы мода (Mode, Tab, State и т.д.) используют values()/valueOf() —
# это нужно сохранить, иначе свитчи и циклы по enum сломаются.
-keepclassmembers enum dev.lewiz.events.** {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# JSON с сервера разбирается вручную через Gson JsonObject/JsonArray
# (getAsJsonObject/get("key") и т.п.), а не через reflection-биндинг на поля,
# поэтому переименовывать поля/методы модели безопасно.

-keepattributes SourceFile, LineNumberTable
-renamesourcefileattribute SourceFile
