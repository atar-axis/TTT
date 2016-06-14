#!/bin/bash
vncserver -kill :1
vncserver -geometry 960x720
command -v kdialog || {
    command -v zenity || { echo 'VNC started'; exit 1 ; }
    zenity --info --text="VNC started"
    exit 1
}
kdialog -msgbox 'VNC started'
