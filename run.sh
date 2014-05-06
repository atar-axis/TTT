#!/bin/bash
PRG=`type $0`
PRG=${PRG##* }
# If PRG is a symlink, trace it to the real home directory
while [ -L "$PRG" ]
do
    newprg=$(ls -l ${PRG})
    newprg=${newprg##*-> }
    [ ${newprg} = ${newprg#/} ] && newprg=${PRG%/*}/${newprg}
    PRG="$newprg"
done
PRG=${PRG%/*}
echo Changing to application folder is ${PRG}
cd ${PRG}
MACHINE_TYPE=`uname -m`
if [ ${MACHINE_TYPE} == 'x86_64' ]; then
    echo Executing 64 bit version
    LD_LIBRARY_PATH=linux64 java -Dsun.java2d.opengl=true -Xmx4096M -jar ttt.jar
else
    echo Executing 32 bit version
    LD_LIBRARY_PATH=linux32 java -Dsun.java2d.opengl=true -Xmx1024M -jar ttt.jar
fi
cd -
