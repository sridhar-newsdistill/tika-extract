#! /bin/bash
# run under screen to maintain parent proc when ssh session terminated
#
# Usage: find ... | ./proc.sh firstrun
# file paths to process on stdin and "firstrun" parameter indicates its ok to clobber any existing dirs: todo inprogress done
#
# to restart a partially completed run after something goes wrong
# ./proc.sh
# inprogress files are moved back to todo and then whatever is in todo is processed
# (you can first manually move done/bunch-???????? files to todo to reprocess them).

url="http://localhost:9998/rmeta/text"
bunchSize=4
concurrentProcs=4

if [[ "$1" == "firstrun" ]]; then
  rm -rf todo inprogress done
  mkdir -p todo inprogress done

  # file paths to process on stdin
  # write (at most) bunchSize file paths to each todo/bunch-* file
  cd todo
  split --suffix-length=3 --numeric-suffixes --lines=${bunchSize} - bunch-
  cd ..
else
  mv inprogress/bunch-* todo || {
    echo "$0: can't restart with no inprogress files" >&2
    exit 1
  }
fi

# process a todo/bunch-* file until there are no more
function onethread() {
  while true; do
    todoFile=$( ls -1 todo/bunch-* 2> /dev/null | head -1 )
    [[ -z "$todoFile" ]] && return 0   # no more
    inprogFile="inprogress${todoFile#todo}"
    doneFile="done${todoFile#todo}"
    if mv $todoFile $inprogFile 2> /dev/null; then
      # if file successfully moved from todo to inprogress then we process the file paths in it
      # otherwise some other subprocess beat us to it
      while read filepath; do
        echo "$filepath ..." >&2
        curl --silent --show-error --upload-file "$filepath" $url | node transform.js "$filepath"
        # echo "testing $filepath"
      done < $inprogFile > ${doneFile}.json 2> ${doneFile}.err
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
