.path as $p | if $p and .embedded then
  .embedded | [ keys, . ] | transpose | .[] | [ $p, .[0], .[1].content ] | @tsv 
else
  empty
end