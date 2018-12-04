!define APPNAME "Hypnos Music Player"
!define APP_DIR_NAME "Hypnos"
!define COMPANYNAME "JoshuaD.net"
!define MENU_DIR_NAME "Hypnos Music Player"
!define MENU_SHORTCUT_NAME "Hypnos"
# These three must be integers?
# !define VERSIONMAJOR Beta5
# !define VERSIONBUILD 1
# These will be displayed by the "Click here for support information" link in "Add/Remove Programs"
# It is possible to use "mailto:" links in here to open the email client
!define HELPURL "http://hypnosplayer.org" # "Support Information" link
!define UPDATEURL "http://hypnosplayer.org" # "Product Updates" link
!define ABOUTURL "http://hypnosplayer.org" # "Publisher" link
# This is the size (in kB) of all the files copied into "Program Files"
!define INSTALLSIZE 188600
 
RequestExecutionLevel admin ;Require admin rights on NT6+ (When UAC is turned on)
 
InstallDir "$PROGRAMFILES64\${APP_DIR_NAME}"
 
# rtf or txt file - remember if it is txt, it must be in the DOS text format (\r\n)
LicenseData "..\..\LICENSE"
# This will be in the installer/uninstaller's title bar
Name "${APPNAME}"
Icon "..\..\stage\resources\icon.ico"
outFile "..\..\distribution\Hypnos-Installer.exe"
 
!include LogicLib.nsh
 
# Just three pages - license agreement, install location, and installation
page license
page directory
Page instfiles
 
!macro VerifyUserIsAdmin
UserInfo::GetAccountType
pop $0
${If} $0 != "admin" ;Require admin rights on NT4+
        messageBox mb_iconstop "Administrator rights required!"
        setErrorLevel 740 ;ERROR_ELEVATION_REQUIRED
        quit
${EndIf}
!macroend
 
function .onInit
	setShellVarContext all
	!insertmacro VerifyUserIsAdmin
functionEnd
 
section "install"
	# Files for the install directory - to build the installer, these should be in the same directory as the install script (this file)
	setOutPath $INSTDIR

	# Files added here should be removed by the uninstaller (see section "uninstall")
	file /r "../../temp/Hypnos/*"
 
	# Uninstaller - See function un.onInit and section "uninstall" for configuration
	writeUninstaller "$INSTDIR\uninstall.exe"
 
	# Start Menu
	createDirectory "$SMPROGRAMS\${MENU_DIR_NAME}"
	createShortCut "$SMPROGRAMS\${MENU_DIR_NAME}\${MENU_SHORTCUT_NAME}.lnk" "$INSTDIR\Hypnos.exe" "" "$INSTDIR\resources\icon.ico"
 
	# Registry information for add/remove programs
	WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\${APPNAME}" "DisplayName" "${APPNAME}"
	WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\${APPNAME}" "UninstallString" "$\"$INSTDIR\uninstall.exe$\""
	WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\${APPNAME}" "QuietUninstallString" "$\"$INSTDIR\uninstall.exe$\" /S"
	WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\${APPNAME}" "InstallLocation" "$\"$INSTDIR$\""
	WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\${APPNAME}" "DisplayIcon""$\"$INSTDIR\resources\icon.ico$\""
	WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\${APPNAME}" "Publisher" "${COMPANYNAME}"
	WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\${APPNAME}" "HelpLink" "$\"${HELPURL}$\""
	WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\${APPNAME}" "URLUpdateInfo" "$\"${UPDATEURL}$\""
	WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\${APPNAME}" "URLInfoAbout" "$\"${ABOUTURL}$\""
	# WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\${APPNAME}" "DisplayVersion""${VERSIONMAJOR} b${VERSIONBUILD}"
	# WriteRegDWORD HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\${APPNAME}" "VersionMajor" ${VERSIONMAJOR}
	# There is no option for modifying or repairing the install
	WriteRegDWORD HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\${APPNAME}" "NoModify" 1
	WriteRegDWORD HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\${APPNAME}" "NoRepair" 1
	# Set the INSTALLSIZE constant (!defined at the top of this script) so Add/Remove Programs can accurately report the size
	WriteRegDWORD HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\${APPNAME}" "EstimatedSize" ${INSTALLSIZE}
	
	#Registry Info for context menus
	#WriteRegStr HKCR ".flac\shell\Hypnos\command" "" "$INSTDIR\Hypnos.exe $\"%1$\""
	#WriteRegStr HKCR "Directory\shell\Open with Hypnos" "" "$(SHELLEXT_ADD)"
	#WriteRegStr HKCR "Directory\shell\Open with Hypnos\command" "" '$INSTDIR\${APPNAME}.exe "%1"'
	#WriteRegStr HKCR "Directory\shell\Open with Hypnos\${APPNAME}.exe"

sectionEnd
 
# Uninstaller
 
function un.onInit
	SetShellVarContext all
 
	#Verify the uninstaller - last chance to back out
	MessageBox MB_OKCANCEL "Permanantly remove ${APPNAME}?" IDOK next
		Abort
	next:
	!insertmacro VerifyUserIsAdmin
functionEnd
 
section "uninstall"
 
	# Remove Start Menu launcher
	delete "$SMPROGRAMS\${MENU_DIR_NAME}\${MENU_SHORTCUT_NAME}.lnk"
	# Try to remove the Start Menu folder - this will only happen if it is empty
	rmDir "$SMPROGRAMS\${MENU_DIR_NAME}"
 
	# Remove files
	rmDir /r /REBOOTOK $INSTDIR\jre
	rmDir /r /REBOOTOK $INSTDIR\lib
	rmDir /r /REBOOTOK $INSTDIR\resources
   delete $INSTDIR\Hypnos.exe
 
	# Always delete uninstaller as the last action
	delete $INSTDIR\uninstall.exe
 
	# Try to remove the install directory - this will only happen if it is empty
	rmDir $INSTDIR
 
	# Remove uninstaller information from the registry
	DeleteRegKey HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\${APPNAME}"
sectionEnd
