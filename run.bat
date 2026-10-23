@echo off
if exist "newsroom-2.0.0.jar" (
    echo Launching NEWSROOM Phase 2 from JAR...
    java "-Dawt.useSystemAAFontSettings=on" "-Dswing.aatext=true" -jar newsroom-2.0.0.jar
) else if exist "newsroom-1.0.0.jar" (
    echo Launching NEWSROOM from JAR...
    java "-Dawt.useSystemAAFontSettings=on" "-Dswing.aatext=true" -jar newsroom-1.0.0.jar
) else (
    echo Launching NEWSROOM from bin directory...
    java "-Dawt.useSystemAAFontSettings=on" "-Dswing.aatext=true" -cp "bin;lib/*" com.newsroom.NewsReaderApplication
)
