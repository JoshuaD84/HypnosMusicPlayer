cl^
 /EHsc^
 /I ..\packaging\jres\win-full\include^
 /I ..\packaging\jres\win-full\include\win32^
 /W4 /O2 ..\src\win-launch\win-launch.cpp^
 /link ..\packaging\jres\win-full\lib\jvm.lib^
 /subsystem:Windows^
 /MACHINE:X64^
 /out:..\bin-native\win-launch\Hypnos.exe 

del win-launch.obj
