!include "MUI.nsh"

Icon nsis/ttt.ico
Name "TeleTeaching Tool"
OutFile "installer.exe"
InstallDir $PROGRAMFILES\TeleTeachingTool
BrandingText " "

!insertmacro MUI_PAGE_DIRECTORY
!insertmacro MUI_PAGE_INSTFILES

!insertmacro MUI_LANGUAGE "English"
!insertmacro MUI_LANGUAGE "German"




;Page directory
;Page instfiles

!define JRE_VERSION "1.6"
!define JRE_URL "http://javadl.sun.com/webapps/download/AutoDL?BundleId=18714&/jre-6u5-windows-i586-p.exe"

Function .onInit

  !insertmacro MUI_LANGDLL_DISPLAY

FunctionEnd
Function DetectJRE
  ReadRegStr $2 HKLM "SOFTWARE\JavaSoft\Java Runtime Environment" \
             "CurrentVersion"
  StrCmp $2 ${JRE_VERSION} done
 
  Call DownloadJRE
 
  done:
FunctionEnd
Function DownloadJRE
        MessageBox MB_OK "TTT uses Java ${JRE_VERSION}, it will now \
                         be downloaded and installed"
 
        StrCpy $2 "$TEMP\Java Runtime Environment.exe"
        nsisdl::download /TIMEOUT=30000 ${JRE_URL} $2
        Pop $R0 ;Get the return value
                StrCmp $R0 "success" +3
                MessageBox MB_OK "Download failed: $R0"
                Quit
        ExecWait $2
        Delete $2
FunctionEnd

Function GetJRE
;  Find JRE (javaw.exe)
;  1 - in .\jre directory (JRE Installed with application)
;  2 - in JAVA_HOME environment variable
;  3 - in the registry
;  4 - assume javaw.exe in current dir or PATH
 
  Push $R0
  Push $R1
 
  ClearErrors
  StrCpy $R0 "$EXEDIR\jre\bin\javaw.exe"
  IfFileExists $R0 JreFound
  StrCpy $R0 ""
 
  ClearErrors
  ReadEnvStr $R0 "JAVA_HOME"
  StrCpy $R0 "$R0\bin\javaw.exe"
  IfErrors 0 JreFound
 
  ClearErrors
  ReadRegStr $R1 HKLM "SOFTWARE\JavaSoft\Java Runtime Environment" "CurrentVersion"
  ReadRegStr $R0 HKLM "SOFTWARE\JavaSoft\Java Runtime Environment\$R1" "JavaHome"
  StrCpy $R0 "$R0\bin\javaw.exe"
 
  IfErrors GoodLuck JreFound
  StrCpy $R0 "javaw.exe"
 
 GoodLuck:
  MessageBox MB_ICONSTOP "Cannot find appropriate Java Runtime Environment."
  Abort

 JreFound:
  Pop $R1
  Exch $R0
FunctionEnd

Section ""

  SetOutPath $INSTDIR

  Call DetectJRE

  Call GetJRE
  Pop $R0

  File dist/ttt.jar
  File dist/jmf-2.1.1e.jar
  File dist/mp3plugin.jar
  File dist/swing-layout-1.0.2.jar
  File dist/jsch-0.1.32-patched.jar
  File dist/itext-1.4.8.jar
  File nsis/lame.exe
  File nsis/MP4Box.exe
  File nsis/js32.dll
  File nsis/ffmpeg.exe
  File nsis/ttt.ico

 
  SetOutPath $INSTDIR


  ; change for your purpose (-jar etc.)
  StrCpy $0 '"$R0" -jar "$INSTDIR"/ttt.jar'
 
  Exec $0

  CreateDirectory "$SMPROGRAMS\TU-Muenchen"
  CreateShortCut "$SMPROGRAMS\TU-MUenchen\TeleTeachingTool.lnk" "$R0"  '-jar "$INSTDIR"/ttt.jar' "$INSTDIR\ttt.ico" 1 SW_SHOWNORMAL 

SectionEnd 
