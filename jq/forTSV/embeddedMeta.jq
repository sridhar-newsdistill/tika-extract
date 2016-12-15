.path as $p | if $p and .embedded then
  .embedded | [ keys, . ] | transpose | .[] | . as [ $embIdx, $emb ] | $emb.meta[] | [ $p, $embIdx, .key, .val ] | @tsv 
else
  empty
end