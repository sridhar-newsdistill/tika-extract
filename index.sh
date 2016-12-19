# run under screen to maintain parent proc when ssh session terminated
#
# Usage: index.sh
# before running copy json files to todo, script will create inprogress done
 
concurrentProcs=30
 
mkdir -p todo inprogress done
# process a todo/bunch-*.json file until there are no more
function onethread() {
  local todoFile inprogFile doneFile
  while true; do
    todoFile=$( ls -1 todo/bunch-*.json 2> /dev/null | head -1 )
    [[ -z "$todoFile" ]] && return 0   # no more
    inprogFile="inprogress${todoFile#todo}"
    doneFile="done${todoFile#todo}"
    if mv $todoFile $inprogFile 2> /dev/null; then
      # if file successfully moved from todo to inprogress then we process it
      # otherwise some other subprocess beat us to it
      jq -cf /data/tika-extract/jq/addBulkMeta.jq $inprogFile | curl -s -X POST localhost:9200/_bulk --data-binary "@-" >& ${doneFile%json}err
      # move from inprogress to done to indicate completion
      mv ${inprogFile} ${doneFile}
    fi
  done
}
 
 
i=0
while [[ $i -lt ${concurrentProcs} ]]; do
  echo "start subproc $i"
  onethread &
  i=$(( $i + 1 ))
done
wait
echo "all done"
