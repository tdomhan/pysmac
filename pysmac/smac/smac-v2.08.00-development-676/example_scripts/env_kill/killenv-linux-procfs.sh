#!/bin/bash
#Uses procfs to match environment variable settings
#This may be somewhat brittle, I'm not sure
#On other systems without procfs "ps ewww" may be able to help you

#Without this we may kill every process
if [ "$#" -ne 2 ]; then
    echo "Must supply 2 arguments"
    exit 2
fi

fgrep -l "$1=$2" /proc/*/environ -s | grep -v self | grep -v $$ | wc -l  #Outputs the number matches
fgrep -l "$1=$2" /proc/*/environ -s | sed 's|/proc/||' | sed 's|/environ||' | grep -v self | grep -v "$$" | xargs -L 1 kill -KILL 2> /dev/null 

