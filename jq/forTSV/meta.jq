.path as $p | if $p then
  .meta[] | [ $p, .key, .val ] | @tsv 
else
  empty
end