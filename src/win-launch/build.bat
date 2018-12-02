del main.jar
del /Q bin

jre\bin\javac -d bin --module-path jfx\lib src/module-info.java src/net/joshuad/test/Main.java
jre\bin\jar cfm main.jar MANIFEST.MF -C bin .

rem jre\bin\javac -d bin src/net/joshuad/test/Main.java
rem jre\bin\jar cfm main.jar MANIFEST.MF -C bin .

cl^
 /EHsc^
 /I jre\include^
 /I jre\include\win32^
 /W4 /O2 src\cpp\launch.cpp^
 /link jre\lib\jvm.lib^
 /subsystem:WINDOWS^
 /MACHINE:X64^
 /out:launch.exe 

