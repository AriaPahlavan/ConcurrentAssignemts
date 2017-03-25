#! /bin/bash

oneStatus="alive"
twoStatus="alive"
loop="true"
root="server/"

javac ${root}Server.java
java ${root}Server < ${root}server1.cfg&
first=`echo $!`

java ${root}Server < ${root}server2.cfg&
second=`echo $!`

sleep 1


while [[ $loop == "true" ]]; do

  echo "Press Enter, 1, or 2 to kill all, first, or second server, respectively..."
  read command

  # exit?
  [[ $command == "" ]] && loop="false" && echo "loop is over"

  # kill server 1?
  [[ $command == "1" ]] && [[ $oneStatus == "alive" ]] \
  && kill -9 $first && oneStatus="dead" && echo "killing one"

  # kill server 2?
  [[ $command == "2" ]] && [[ $twoStatus == "alive" ]] \
  && kill -9 $second && twoStatus="dead" && echo "killing one"

  [[ $oneStatus == "dead" ]] && [[ $twoStatus == "dead" ]] && loop="false"

done


[[ $oneStatus == "alive" ]] && kill -9 $first && echo "finally killing one"
[[ $twoStatus == "alive" ]] && kill -9 $second && echo "finally killing two"

rm -rf ./*/*.class
