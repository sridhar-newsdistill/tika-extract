if .path then
  [ .path , .content ] | @tsv 
else
  empty
end